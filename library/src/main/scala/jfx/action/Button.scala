package jfx.action

import jfx.core.component.Component
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Button extends Component {
  override def tagName: String = "button"

  def onClick(handler: dom.MouseEvent => Unit): Unit = {
    addDisposable(host.addEventListener("click", e => handler(e.asInstanceOf[dom.MouseEvent])))
  }

  override def compose(): Unit = {
  }
}

object Button {
  def button(textValue: String = "")(init: Button ?=> Unit): Button = {
    DslRuntime.build(new Button()) { b ?=>
      if (textValue.nonEmpty) {
        jfx.core.component.Component.text_=(textValue)
      }
      init
    }
  }

  def onClick(handler: dom.MouseEvent => Unit)(using b: Button): Unit = b.onClick(handler)
}
