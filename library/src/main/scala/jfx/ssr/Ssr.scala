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

      awaitPending(root).map(_ => cursor.resultHtml(root)).toJSPromise
    } catch {
      case error: Throwable => js.Promise.reject(error).asInstanceOf[js.Promise[String]]
    }
  }

  private def awaitPending(root: Component): Future[Unit] = {
    val pending = collectPending(root)

    if (pending.isEmpty) Future.successful(())
    else Future.sequence(pending.map(_.toFuture)).flatMap(_ => awaitPending(root)).map(_ => ())
  }

  private def collectPending(component: Component): Seq[js.Promise[Unit]] = {
    val own = component match {
      case pending: AsyncRenderPending => pending.pendingRenderPromises
      case _                           => Nil
    }

    own ++ component.children.flatMap(collectPending)
  }
}
