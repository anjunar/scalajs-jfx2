package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.ObserveRender.observeRender
import org.scalajs.dom
import org.scalajs.dom.window

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class Router(val routes: Seq[Route], initialUrl: String) extends Component {
  override def tagName: String = "" 

  private sealed trait RenderState
  private case object RoutePending extends RenderState
  private case object RouteLoading extends RenderState
  private final case class RouteReady(context: RouteContext, render: RouteContext => Unit) extends RenderState
  private final case class RouteFailure(error: Throwable) extends RenderState
  private final case class RouteNotFound(path: String) extends RenderState

  private given ExecutionContext = ExecutionContext.global
  private var renderToken = 0

  val stateProperty = Property(resolve(initialUrl))
  private val renderStateProperty: Property[RenderState] = Property(RoutePending)
  val loadingProperty: Property[Boolean] = Property(false)
  val errorProperty: Property[Option[Throwable]] = Property(None)

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
    if (!jfx.core.render.RenderBackend.current.isServer) {
      if (replace) window.history.replaceState(null, "", nextState.url)
      else window.history.pushState(null, "", nextState.url)
    }
    stateProperty.set(nextState)
  }

  private def resolveRenderState(): Unit = {
    renderToken += 1
    val token = renderToken
    val state = stateProperty.get
    errorProperty.set(None)
    loadingProperty.set(false)

    state.currentMatchOption match {
      case Some(m) =>
        val ctx = RouteContext(state.path, state.url, m.fullPath, m.params, state.queryParams, state, m)
        m.route.asyncFactory match {
          case Some(asyncFactory) =>
            loadingProperty.set(true)
            renderStateProperty.set(RouteLoading)
            val future = asyncFactory(ctx).toFuture
            future.foreach { factory =>
              if (token == renderToken) {
                loadingProperty.set(false)
                renderStateProperty.set(RouteReady(ctx, currentContext => factory(using currentContext)))
              }
            }
            future.failed.foreach { error =>
              if (token == renderToken) {
                loadingProperty.set(false)
                errorProperty.set(Some(error))
                renderStateProperty.set(RouteFailure(error))
              }
            }
          case None =>
            renderStateProperty.set(RouteReady(ctx, m.route.factory))
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
      if (jfx.core.render.RenderBackend.current.isServer) "/"
      else s"${window.location.pathname}${window.location.search}"
    }
    DslRuntime.build(new Router(routes, startUrl)) {}
  }
  def navigate(path: String)(using r: Router): Unit = r.navigate(path)
}
