package jfx.ssr.context

import jfx.core.render.RenderBackend

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

  def currentOrigin: Option[String] =
    if (!RenderBackend.current.isServer) None
    else active.toOption.flatMap(_.origin.toOption).filter(_.nonEmpty)

  def currentPath: Option[String] =
    if (!RenderBackend.current.isServer) None
    else active.toOption.flatMap(_.path.toOption).filter(_.nonEmpty)

  def currentCookie: Option[String] =
    if (!RenderBackend.current.isServer) None
    else active.toOption.flatMap(_.cookie.toOption).filter(_.nonEmpty)
}

