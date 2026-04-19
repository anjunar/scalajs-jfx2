package jfx.router

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom
import org.scalajs.dom.window
import scala.scalajs.js

class Router(val routes: Seq[Route], initialUrl: String) extends Component {
  override def tagName: String = "" // Virtual fragment

  val stateProperty = Property(resolve(initialUrl))

  override def compose(): Unit = {
    // 1. Listen to browser navigation
    if (!jfx.core.render.RenderBackend.current.isServer) {
      addDisposable({
        val listener: dom.Event => Unit = _ => {
          navigate(currentBrowserUrl(), replace = true)
        }
        window.addEventListener("popstate", listener)
        () => window.removeEventListener("popstate", listener)
      })
    }

    // 2. React to state changes
    addDisposable(stateProperty.observe { _ =>
      render()
    })
  }

  def navigate(path: String, replace: Boolean = false): Unit = {
    val nextState = resolve(path)
    
    if (!jfx.core.render.RenderBackend.current.isServer) {
      val fullUrl = nextState.url
      if (replace) window.history.replaceState(null, "", fullUrl)
      else window.history.pushState(null, "", fullUrl)
    }

    stateProperty.set(nextState)
  }

  private def render(): Unit = {
    // Clear old children
    children.toSeq.foreach { c =>
      removeChild(c)
      c.dispose()
    }

    // Match and render
    val state = stateProperty.get
    state.currentMatchOption match {
      case Some(m) =>
        val context = RouteContext(
          path = state.path,
          url = state.url,
          fullPath = m.fullPath,
          pathParams = m.params,
          queryParams = state.queryParams,
          state = state,
          routeMatch = m
        )

        // Use existing cursor if available (composition/hydration)
        val cursor = try {
          jfx.dsl.DslRuntime.currentCursor
        } catch {
          case _: IllegalStateException =>
            val offset = calculateDomOffset
            jfx.core.render.RenderBackend.current.insertionCursor(host, offset)
        }

        DslRuntime.withCursor(cursor) {
          DslRuntime.withContext(ComponentContext(Some(this))) {
            m.route.factory(context)
          }
        }
      case None =>
        val cursor = try {
          jfx.dsl.DslRuntime.currentCursor
        } catch {
          case _: IllegalStateException =>
            val offset = calculateDomOffset
            jfx.core.render.RenderBackend.current.insertionCursor(host, offset)
        }
        DslRuntime.withCursor(cursor) {
          DslRuntime.withContext(ComponentContext(Some(this))) {
            jfx.core.component.Box.box("div") {
              text = s"No route matched for: ${state.path}"
            }
          }
        }
    }
  }

  private def resolve(url: String): RouterState = {
    val pathname = url.takeWhile(_ != '?')
    val search = url.drop(pathname.length)
    val queryParams = parseQueryParams(search)
    
    val matches = RouteMatcher.resolve(routes, pathname)
    RouterState(pathname, matches, queryParams, search)
  }

  private def currentBrowserUrl(): String = {
    s"${window.location.pathname}${window.location.search}"
  }

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
    DslRuntime.build(new Router(routes, startUrl)) {
    }
  }
  
  def navigate(path: String)(using r: Router): Unit = r.navigate(path)
}
