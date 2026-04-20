package jfx.action

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Button extends Component {
  override def tagName: String = "button"
}

object Button {
  def button(textValue: String = "")(init: Button ?=> Unit): Button = {
    DslRuntime.build(new Button()) { b ?=>
      if (textValue.nonEmpty) {
        text = textValue
      }
      init
    }
  }

  def buttonType(using b: Button): String = b.host.attribute("type").getOrElse("")
  def buttonType_=(value: String)(using b: Button): Unit = {
    b.host.setAttribute("type", value)
  }
}
