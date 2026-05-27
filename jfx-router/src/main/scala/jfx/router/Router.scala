package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.render.{AsyncRenderPending, HydrationRenderBackend, RenderBackend}
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.ObserveRender.observeRender
import org.scalajs.dom
import org.scalajs.dom.window

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success}

class Router(val routes: Seq[Route], initialUrl: String) extends Component with AsyncRenderPending {
  override def tagName: String = "" 

  private final case class LoadedRoute(component: Component, context: RouteContext)

  private sealed trait RenderState
  private case object RoutePending extends RenderState
  private case object RouteLoading extends RenderState
  private case object RouteReady extends RenderState
  private final case class RouteFailure(error: Throwable) extends RenderState
  private final case class RouteNotFound(path: String) extends RenderState

  private given ExecutionContext = ExecutionContext.global
  private var renderToken = 0
  private var pendingRouteLoad: Option[js.Promise[Unit]] = None
  private var loadingRenderer: Router ?=> Unit = Router.defaultLoadingRenderer
  private val statefulComponents = mutable.LinkedHashMap.empty[String, Component]

  val $stateProperty = Property(resolve(initialUrl))
  private val renderStateProperty: Property[RenderState] = Property(RoutePending)
  private val currentRouteProperty: Property[LoadedRoute | Null] = Property(null)
  val $loadingProperty: Property[Boolean] = Property(false)
  val $errorProperty: Property[Option[Throwable]] = Property(None)

  override def pendingRenderPromises: Seq[js.Promise[Unit]] =
    pendingRouteLoad.toSeq

  override def initialize(): Unit = {
    given Component = this
    onWindowPopState(_ => navigate(currentBrowserUrl(), replace = true))
    addDisposable($stateProperty.observe(_ => resolveRenderState()))
  }

  override def compose(): Unit = {
    DslRuntime.build(new RouteOutlet(currentRouteProperty)) {}

    observeRender(renderStateProperty) {
      case RoutePending =>
        ()
      case RouteLoading =>
        renderLoadingView()
      case RouteReady =>
        ()
      case RouteFailure(error) =>
        div {
          $classes = Seq("jfx-router-error")
          text = Option(error.getMessage).filter(_.nonEmpty).getOrElse("Route could not be loaded")
        }
      case RouteNotFound(path) =>
        div {
          text = s"No route matched for: $path"
        }
    }
  }

  def navigate(path: String, replace: Boolean = false): Unit = {
    val nextState = resolve(path)
    if (!RenderBackend.current.isServer) {
      if (replace) window.history.replaceState(null, "", nextState.url)
      else window.history.pushState(null, "", nextState.url)
    }
    $stateProperty.set(nextState)
  }

  private def resolveRenderState(): Unit = {
    renderToken += 1
    val token = renderToken
    val state = $stateProperty.get
    clearDetachedCurrentComponent()
    pendingRouteLoad = None
    $errorProperty.set(None)
    $loadingProperty.set(false)

    state.currentMatchOption match {
      case Some(m) =>
        val ctx = RouteContext(state.path, state.url, m.fullPath, m.params, state.queryParams, state, m)
        val backend = RenderBackend.current
        val componentKey = state.url

        try {
          statefulComponents.get(componentKey).filter(_ => m.route.stateful) match {
            case Some(component) =>
              currentRouteProperty.set(LoadedRoute(component, ctx))
              renderStateProperty.set(RouteReady)

            case None =>
              val loaded = m.route.load(ctx)
              loaded.value match {
                case Some(Success(component)) =>
                  if (m.route.stateful) {
                    statefulComponents.update(componentKey, component)
                  }
                  currentRouteProperty.set(LoadedRoute(component, ctx))
                  renderStateProperty.set(RouteReady)

                case Some(Failure(error)) =>
                  $errorProperty.set(Some(error))
                  renderStateProperty.set(RouteFailure(error))

                case None =>
                  currentRouteProperty.set(null)
                  $loadingProperty.set(true)
                  renderStateProperty.set(RouteLoading)

                  val rendered = loaded.transform { result =>
                    RenderBackend.withBackend(backend) {
                      if (token == renderToken) {
                        pendingRouteLoad = None
                        $loadingProperty.set(false)
                        result match {
                          case Success(component) =>
                            if (m.route.stateful) {
                              statefulComponents.update(componentKey, component)
                            }
                            currentRouteProperty.set(LoadedRoute(component, ctx))
                            renderStateProperty.set(RouteReady)
                          case Failure(error) =>
                            $errorProperty.set(Some(error))
                            renderStateProperty.set(RouteFailure(error))
                        }
                      }
                    }
                    Success(())
                  }

                  pendingRouteLoad = Some(rendered.toJSPromise)
              }
          }
        } catch {
          case error: Throwable =>
            $errorProperty.set(Some(error))
            renderStateProperty.set(RouteFailure(error))
        }
      case None =>
        currentRouteProperty.set(null)
        renderStateProperty.set(RouteNotFound(state.path))
    }
  }

  override def dispose(): Unit = {
    currentRouteProperty.set(null)
    statefulComponents.valuesIterator
      .filter(_.parent.isEmpty)
      .foreach(_.dispose())
    statefulComponents.clear()
    super.dispose()
  }

  private[router] def setLoadingRenderer(renderer: Router ?=> Unit): Unit =
    loadingRenderer = renderer

  private[router] def renderLoadingView(): Unit =
    loadingRenderer(using this)

  private def resolve(url: String): RouterState = {
    val pathname = url.takeWhile(_ != '?')
    val search = url.drop(pathname.length)
    val matches = RouteMatcher.resolve(routes, pathname)
    RouterState(pathname, matches, parseQueryParams(search), search)
  }

  private def currentBrowserUrl(): String = s"${window.location.pathname}${window.location.search}"

  private def parseQueryParams(search: String): Map[String, String] = {
    if (search.startsWith("?")) {
      search.drop(1).split("&").filter(_.nonEmpty).map { p =>
        val parts = p.split("=")
        val key = js.URIUtils.decodeURIComponent(parts(0))
        val value = if (parts.length > 1) js.URIUtils.decodeURIComponent(parts(1)) else ""
        key -> value
      }.toMap
    } else Map.empty
  }

  private def clearDetachedCurrentComponent(): Unit = {
    val current = currentRouteProperty.get
    if (current != null) {
      currentRouteProperty.set(null)
      if (!statefulComponents.valuesIterator.exists(_ eq current.component)) {
        current.component.dispose()
      }
    }
  }

  private final class RouteOutlet(content: Property[LoadedRoute | Null]) extends Component {
    override def tagName: String = ""

    private var mounted: LoadedRoute | Null = null

    override def compose(): Unit =
      addDisposable(content.observe(_ => reconcile()))

    override def dispose(): Unit = {
      detachMounted()
      super.dispose()
    }

    private def reconcile(): Unit = {
      DslRuntime.updateBranch(this) {
        val next = content.get

        if (mounted != next) {
          detachMounted()
          mounted = null

          if (next != null) {
            attachMounted(next)
            mounted = next
          }
        }
      }
    }

    private def attachMounted(next: LoadedRoute): Unit = {
      val child = next.component
      child.parent.foreach { oldParent =>
        if (oldParent != this) {
          oldParent.removeChild(child)
        }
      }

      if (!children.contains(child)) {
        if (child.parent.contains(this) || child.isBound) {
          child.setParent(Some(this))
          addChild(child)

          if (!RenderBackend.current.isInstanceOf[HydrationRenderBackend]) {
            syncChildAddition(child)
          }
        } else {
          DslRuntime.provide(next.context) {
            DslRuntime.mount(child) {}
          }
        }
      }
    }

    private def detachMounted(): Unit = {
      val current = mounted
      if (current != null && children.contains(current.component)) {
        removeChild(current.component)
        current.component.setParent(None)
      }
    }
  }
}

object Router {
  private[router] def defaultLoadingRenderer(using Router): Unit =
    div {
      classes = Seq("jfx-router-loading")
      text = "Loading..."
    }

  def router(routes: Seq[Route], initial: String = null): Router =
    router(routes, initial)({})

  def router(routes: Seq[Route], initial: String)(init: Router ?=> Unit): Router = {
    val startUrl = if (initial != null) initial else {
      if (RenderBackend.current.isServer) "/"
      else s"${window.location.pathname}${window.location.search}"
    }
    DslRuntime.build(new Router(routes, startUrl))(init)
  }

  def loading(render: Router ?=> Unit)(using router: Router): Unit =
    router.setLoadingRenderer(render)

  def navigate(path: String)(using r: Router): Unit = r.navigate(path)
}
