package jfx.core.component

import jfx.core.render.{Cursor, HostNode}
import jfx.core.state.ReadOnlyProperty
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

class ReactiveTextComponent(val property: ReadOnlyProperty[String]) extends Component {
  override def tagName: String = "#text" 
  
  override def bind(cursor: Cursor): Unit = {
    _host = cursor.claimText(property.get)
  }

  override def compose(): Unit = {
    addDisposable(property.observe { v =>
      if (_host != null && !jfx.core.render.RenderBackend.current.isServer) {
         _host.domNode.foreach(_.textContent = v)
      }
    })
  }
}

object Text {
  def text(value: String): TextComponent = {
    DslRuntime.build(new TextComponent(value)) {
    }
  }

  def text(value: ReadOnlyProperty[String]): ReactiveTextComponent = {
    DslRuntime.build(new ReactiveTextComponent(value)) {
    }
  }
}
