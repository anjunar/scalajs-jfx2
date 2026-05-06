package jfx.ssr.server

import jfx.ssr.context.SsrRequestContext

import scala.scalajs.js

/**
 * Renderer contract used by the Node host.
 *
 * Expected to be backed by an application-provided renderer, e.g. exported from Scala.js bundle:
 * `renderSsrWithTheme(path, theme, ctx)`.
 */
trait JfxSsrRenderer extends js.Object {
  def renderSsrWithTheme(
    path: String,
    theme: String,
    ctx: js.UndefOr[SsrRequestContext]
  ): js.Promise[String]
}

