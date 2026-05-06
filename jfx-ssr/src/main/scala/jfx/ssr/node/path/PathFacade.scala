package jfx.ssr.node.path

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("node:path", JSImport.Namespace)
object nodePath extends js.Object {
  def join(parts: String*): String = js.native
}

