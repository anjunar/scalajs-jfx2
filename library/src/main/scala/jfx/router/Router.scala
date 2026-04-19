package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom
import org.scalajs.dom.window
import scala.scalajs.js

class Router(val routes: Seq[Route], initialUrl: String) extends Component {
  override def tagName: String = "" 
  private var activeBackend: jfx.core.render.RenderBackend = _

  val stateProperty = Property(resolve(initialUrl))

  override def compose(): Unit = {
    activeBackend = jfx.core.render.RenderBackend.current
    if (!jfx.core.render.RenderBackend.current.isServer) {
      addDisposable({
        val listener: dom.Event => Unit = _ => navigate(currentBrowserUrl(), replace = true)
        window.addEventListener("popstate", listener)
        () => window.removeEventListener("popstate", listener)
      })
    }

    addDisposable(stateProperty.observe(_ => render()))
  }

  def navigate(path: String, replace: Boolean = false): Unit = {
    val nextState = resolve(path)
    if (!jfx.core.render.RenderBackend.current.isServer) {
      if (replace) window.history.replaceState(null, "", nextState.url)
      else window.history.pushState(null, "", nextState.url)
    }
    stateProperty.set(nextState)
  }

  private def render(): Unit = {
    DslRuntime.updateBranch(this, activeBackend) {
      children.toSeq.foreach { c => removeChild(c); c.dispose() }
      
      val state = stateProperty.get
      state.currentMatchOption match {
        case Some(m) =>
          val ctx = RouteContext(state.path, state.url, m.fullPath, m.params, state.queryParams, state, m)
          m.route.factory(ctx)
        case None =>
          jfx.core.component.Box.box("div") { text = s"No route matched for: ${state.path}" }
      }
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
