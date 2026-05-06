package jfx.ssr.node.fs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("node:fs/promises", JSImport.Namespace)
object fsPromises extends js.Object {
  def readFile(path: String, encoding: String): js.Promise[String] = js.native
}
