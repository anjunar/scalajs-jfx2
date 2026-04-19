package jfx.core.component

import jfx.core.render.{Cursor, HostElement, HostNode}
import jfx.core.state.Disposable
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.compiletime.uninitialized

trait Component extends Disposable {
  private var _host: HostElement = uninitialized
  def host: HostElement = _host

  private[jfx] def bind(cursor: Cursor): Unit = {
    _host = cursor.claimElement(tagName)
    val sub = cursor.subCursor(_host)
    DslRuntime.withCursor(sub) {
      compose()
    }
  }

  def tagName: String
  def compose(): Unit = {}
  
  override def dispose(): Unit = {}
}
