package jfx.router

import org.scalajs.dom
import jfx.core.render.RenderBackend

object RouterConfig {
  private var _basePath: String = ""

  def basePath: String = {
    if (_basePath.isEmpty && !RenderBackend.current.isServer) {
      val baseElements = dom.document.getElementsByTagName("base")
      if (baseElements.length > 0) {
        val href = baseElements.item(0).asInstanceOf[dom.html.Base].href
        // URL constructor parses absolute URL from base href
        val path = try {
          new dom.URL(href).pathname
        } catch {
          case _: Throwable => href
        }
        _basePath = normalize(path)
      }
    }
    _basePath
  }

  def basePath_=(v: String): Unit = {
    _basePath = normalize(v)
  }

  private def normalize(v: String): String = {
    if (v == null || v == "/" || v.isEmpty) ""
    else {
      val n = if (v.startsWith("/")) v else s"/$v"
      if (n.endsWith("/")) n.dropRight(1) else n
    }
  }

  def resolve(path: String): String = {
    if (path == null) ""
    else if (path.startsWith("http") || path.startsWith("//") || path.startsWith("mailto:") || path.startsWith("tel:")) path
    else {
      val p = if (path.startsWith("/")) path else s"/$path"
      val base = basePath
      if (base.isEmpty || p.startsWith(base + "/") || p == base) p
      else s"$base$p"
    }
  }
}
