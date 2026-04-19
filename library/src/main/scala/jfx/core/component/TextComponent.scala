package jfx.core.component

import jfx.core.render.{Cursor, HostNode}
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.compiletime.uninitialized

class TextComponent(val initialText: String) extends Component {
  override def tagName: String = "#text" // Special tagName for text nodes
  
  override def bind(cursor: Cursor): Unit = {
    _host = cursor.claimText(initialText)
  }

  override def compose(): Unit = {}
}

object Text {
  def text(value: String): TextComponent = {
    DslRuntime.build(new TextComponent(value)) {
    }
  }
}
