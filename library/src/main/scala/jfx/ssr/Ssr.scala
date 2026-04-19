package jfx.ssr

import jfx.core.render.{RenderBackend, SsrRenderBackend, SsrCursor}
import jfx.core.component.Component
import jfx.dsl.DslRuntime

object Ssr {
  def renderToString(init: => Component): String = {
    val backend = new SsrRenderBackend()
    val cursor = backend.nextCursor(None).asInstanceOf[SsrCursor]
    
    RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(cursor) {
        val root = init
        cursor.resultHtml
      }
    }
  }
}
