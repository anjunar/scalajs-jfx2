package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom
import org.scalajs.dom.window

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class Router(val routes: Seq[Route], initialUrl: String) extends Component {
  override def tagName: String = "" 

  private given ExecutionContext = ExecutionContext.global
  private var renderToken = 0

  val stateProperty = Property(resolve(initialUrl))

  override def compose(): Unit = {
    if (!jfx.core.render.RenderBackend.current.isServer) {
      addDisposable({
        val listener: dom.Event => Unit = _ => navigate(currentBrowserUrl(), replace = true)
        window.addEventListener("popstate", listener)
        () => window.removeEventListener("popstate", listener)
      })
    }

    addDisposable(stateProperty.observe(_ => render()))
  }

  // ... (navigate remains same)
  def navigate(path: String, replace: Boolean = false): Unit = {
    val nextState = resolve(path)
    if (!jfx.core.render.RenderBackend.current.isServer) {
      if (replace) window.history.replaceState(null, "", nextState.url)
      else window.history.pushState(null, "", nextState.url)
    }
    stateProperty.set(nextState)
  }

  private def render(): Unit = {
    renderToken += 1
    val token = renderToken
    val state = stateProperty.get

    state.currentMatchOption match {
      case Some(m) =>
        val ctx = RouteContext(state.path, state.url, m.fullPath, m.params, state.queryParams, state, m)
        m.route.asyncFactory match {
          case Some(asyncFactory) =>
            renderLoading()
            val future = asyncFactory(ctx).toFuture
            future.foreach { factory =>
              if (token == renderToken) {
                renderFactory(ctx, factory)
              }
            }
            future.failed.foreach { error =>
              if (token == renderToken) {
                renderError(error)
              }
            }
          case None =>
            replaceContent {
              DslRuntime.provide(ctx) {
                m.route.factory(ctx)
              }
            }
        }
      case None =>
        replaceContent {
          jfx.core.component.Box.box("div") { text = s"No route matched for: ${state.path}" }
        }
    }
  }

  private def renderFactory(ctx: RouteContext, factory: Route.Factory): Unit =
    replaceContent {
      DslRuntime.provide(ctx) {
        factory(using ctx)
      }
    }

  private def renderLoading(): Unit =
    replaceContent {
      jfx.core.component.Box.box("div") {
        classes = "jfx-router-loading"
        text = "Loading..."
      }
    }

  private def renderError(error: Throwable): Unit =
    replaceContent {
      jfx.core.component.Box.box("div") {
        classes = "jfx-router-error"
        text = Option(error.getMessage).filter(_.nonEmpty).getOrElse("Route could not be loaded")
      }
    }

  private def replaceContent(renderContent: => Unit): Unit = {
    DslRuntime.updateBranch(this) {
      children.toSeq.foreach { c => removeChild(c); c.dispose() }
      renderContent
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
