package jfx.action

import jfx.core.component.Component
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Button(val text: String) extends Component {
  override def tagName: String = "button"

  def onClick(handler: dom.MouseEvent => Unit): Unit = {
    addDisposable(host.addEventListener("click", e => handler(e.asInstanceOf[dom.MouseEvent])))
  }

  override def compose(): Unit = {
    jfx.core.component.Text.text(text)
  }
}

object Button {
  def button(text: String)(init: Button ?=> Unit): Button = {
    DslRuntime.build(new Button(text))(init)
  }

  def onClick(handler: dom.MouseEvent => Unit)(using b: Button): Unit = b.onClick(handler)
}
