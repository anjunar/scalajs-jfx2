package jfx.ssr.node.vite

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("vite", JSImport.Namespace)
object vite extends js.Object {
  def createServer(inlineConfig: js.Object): js.Promise[ViteDevServer] = js.native
}

@js.native
trait ViteDevServer extends js.Object {
  val middlewares: js.Any = js.native
  def transformIndexHtml(url: String, html: String): js.Promise[String] = js.native
  def ssrLoadModule(path: String): js.Promise[js.Dynamic] = js.native
  def ssrFixStacktrace(error: js.Any): Unit = js.native
}

