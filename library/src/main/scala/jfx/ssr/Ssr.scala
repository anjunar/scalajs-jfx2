package jfx.ssr

import jfx.core.render.{AsyncRenderPending, RenderBackend, SsrRenderBackend, SsrCursor}
import jfx.core.component.Component
import jfx.dsl.DslRuntime

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object Ssr {
  private given ExecutionContext = ExecutionContext.global

  def renderToString(init: => Component): String = {
    val backend = new SsrRenderBackend()
    val cursor = backend.nextCursor(None).asInstanceOf[SsrCursor]
    
    RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(cursor) {
        val root = init
        cursor.resultHtml(root)
      }
    }
  }

  def renderToStringAsync(init: => Component): js.Promise[String] = {
    val backend = new SsrRenderBackend()
    val cursor = backend.nextCursor(None).asInstanceOf[SsrCursor]

    try {
      val root = RenderBackend.withBackend(backend) {
        DslRuntime.withCursor(cursor) {
          init
        }
      }

      AsyncRenderPending.awaitPending(root).map(_ => cursor.resultHtml(root)).toJSPromise
    } catch {
      case error: Throwable => js.Promise.reject(error).asInstanceOf[js.Promise[String]]
    }
  }
}
