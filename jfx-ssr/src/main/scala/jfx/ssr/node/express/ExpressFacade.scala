package jfx.ssr.node.express

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

// Minimal facade: only what we need for SSR server hosting.

@js.native
@JSImport("express", JSImport.Default)
object express extends js.Object {
  def apply(): ExpressApp = js.native
  def json(): RequestHandler = js.native
}

@js.native
trait ExpressApp extends js.Object {
  def disable(setting: String): Unit = js.native
  def use(handler: RequestHandler): Unit = js.native
  def use(path: String, handler: RequestHandler): Unit = js.native
  def get(path: String, handler: RequestHandler): Unit = js.native
  def post(path: String, handler: RequestHandler): Unit = js.native
  def listen(port: Int, callback: js.Function0[Unit] = js.native): js.Object = js.native
}

@js.native
trait Request extends js.Object {
  val method: String = js.native
  val originalUrl: String = js.native
  val headers: js.Dynamic = js.native
  val query: js.Dynamic = js.native
  val body: js.UndefOr[js.Any] = js.native
}

@js.native
trait Response extends js.Object {
  def status(code: Int): Response = js.native
  def set(header: String, value: String): Response = js.native
  def `type`(value: String): Response = js.native
  def send(body: String): Unit = js.native
  def end(body: String = js.native): Unit = js.native
}

@js.native
trait NextFunction extends js.Function1[js.UndefOr[js.Any], Unit]

trait RequestHandler extends js.Function3[Request, Response, NextFunction, Unit]
