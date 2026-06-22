package jfx.ssr.context

import scala.scalajs.js

/**
 * Request-scoped SSR context passed from Node host to Scala.js renderer.
 *
 * - origin: backend origin used to resolve relative API URLs
 * - path: current rendered path (no hash)
 * - cookie: original browser cookie header for backend fetch forwarding
 */
trait SsrRequestContext extends js.Object {
  val origin: js.UndefOr[String]
  val path: js.UndefOr[String]
  val cookie: js.UndefOr[String]
}

object SsrRequestContext {
  def apply(
    origin: js.UndefOr[String] = js.undefined,
    path: js.UndefOr[String] = js.undefined,
    cookie: js.UndefOr[String] = js.undefined
  ): SsrRequestContext =
    js.Dynamic.literal(
      origin = origin,
      path = path,
      cookie = cookie
    ).asInstanceOf[SsrRequestContext]
}
