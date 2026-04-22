package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.render.{AsyncRenderPending, RenderBackend}
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.ObserveRender.observeRender
import org.scalajs.dom
import org.scalajs.dom.window

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success}

class Router(val routes: Seq[Route], initialUrl: String) extends Component with AsyncRenderPending {
  override def tagName: String = "" 

  private sealed trait RenderState
  private case object RoutePending extends RenderState
  private case object RouteLoading extends RenderState
  private final case class RouteReady(context: RouteContext, render: Route.Factory) extends RenderState
  private final case class RouteFailure(error: Throwable) extends RenderState
  private final case class RouteNotFound(path: String) extends RenderState

  private given ExecutionContext = ExecutionContext.global
  private var renderToken = 0
  private var pendingRouteLoad: Option[js.Promise[Unit]] = None

  val stateProperty = Property(resolve(initialUrl))
  private val renderStateProperty: Property[RenderState] = Property(RoutePending)
  val loadingProperty: Property[Boolean] = Property(false)
  val errorProperty: Property[Option[Throwable]] = Property(None)

  override def pendingRenderPromises: Seq[js.Promise[Unit]] =
    pendingRouteLoad.toSeq

  override def initialize(): Unit = {
    given Component = this
    onWindowPopState(_ => navigate(currentBrowserUrl(), replace = true))
    addDisposable(stateProperty.observe(_ => resolveRenderState()))
  }

  override def compose(): Unit = {
    observeRender(renderStateProperty) {
      case RoutePending =>
        ()
      case RouteLoading =>
        div {
          classes = "jfx-router-loading"
          text = "Loading..."
        }
      case RouteReady(context, factory) =>
        DslRuntime.provide(context) {
          factory(context)
        }
      case RouteFailure(error) =>
        div {
          classes = "jfx-router-error"
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
    stateProperty.set(nextState)
  }

  private def resolveRenderState(): Unit = {
    renderToken += 1
    val token = renderToken
    val state = stateProperty.get
    pendingRouteLoad = None
    errorProperty.set(None)
    loadingProperty.set(false)

    state.currentMatchOption match {
      case Some(m) =>
        val ctx = RouteContext(state.path, state.url, m.fullPath, m.params, state.queryParams, state, m)
        val backend = RenderBackend.current

        try {
          val routeLoad = m.route.load(ctx)
          routeLoad.immediate match {
            case Some(factory) =>
              renderStateProperty.set(RouteReady(ctx, factory))

            case None =>
              loadingProperty.set(true)
              renderStateProperty.set(RouteLoading)

              val loaded = routeLoad.promise.toFuture
              val rendered = loaded.transform { result =>
                RenderBackend.withBackend(backend) {
                  if (token == renderToken) {
                    pendingRouteLoad = None
                    loadingProperty.set(false)
                    result match {
                      case Success(factory) =>
                        renderStateProperty.set(RouteReady(ctx, factory))
                      case Failure(error) =>
                        errorProperty.set(Some(error))
                        renderStateProperty.set(RouteFailure(error))
                    }
                  }
                }
                Success(())
              }

              pendingRouteLoad = Some(rendered.toJSPromise)
          }
        } catch {
          case error: Throwable =>
            errorProperty.set(Some(error))
            renderStateProperty.set(RouteFailure(error))
        }
      case None =>
        renderStateProperty.set(RouteNotFound(state.path))
    }
  }

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
}

object Router {
  def router(routes: Seq[Route], initial: String = null): Router = {
    val startUrl = if (initial != null) initial else {
      if (RenderBackend.current.isServer) "/"
      else s"${window.location.pathname}${window.location.search}"
    }
    DslRuntime.build(new Router(routes, startUrl)) {}
  }
  def navigate(path: String)(using r: Router): Unit = r.navigate(path)
}
