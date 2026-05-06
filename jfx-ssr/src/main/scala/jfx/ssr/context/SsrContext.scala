package jfx.ssr.context

import scala.scalajs.js

/**
 * Scala.js-side request context (no globalThis usage).
 *
 * NOTE: This is safe and deterministic as long as the host ensures a request-scoped execution
 * boundary (e.g. render queue, or ALS). Our prod SSR uses a render queue, and the dev server
 * inlines SSR behind a queue as well.
 */
object SsrContext {

  private var active: js.UndefOr[SsrRequestContext] = js.undefined

  def withContext[A](ctx: js.UndefOr[SsrRequestContext])(thunk: => A): A = {
    val previous = active
    active = ctx
    try thunk
    finally active = previous
  }

  def withAsyncContext[A](ctx: js.UndefOr[SsrRequestContext])(thunk: => js.Promise[A]): js.Promise[A] = {
    val previous = active
    active = ctx

    try {
      thunk.`then`[A]({ value =>
        active = previous
        value
      }).`catch` { error =>
        active = previous
        js.Promise.reject(error).asInstanceOf[js.Promise[A]]
      }
    } catch {
      case error: Throwable =>
        active = previous
        throw error
    }
  }

  def currentOrigin: Option[String] =
    active.toOption.flatMap(_.origin.toOption).filter(_.nonEmpty)

  def currentPath: Option[String] =
    active.toOption.flatMap(_.path.toOption).filter(_.nonEmpty)

  def currentCookie: Option[String] =
    active.toOption.flatMap(_.cookie.toOption).filter(_.nonEmpty)
}

