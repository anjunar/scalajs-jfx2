package jfx.action

import jfx.core.component.ElementComponent
import jfx.core.state.Disposable
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.{Event, HTMLButtonElement}

class Button extends ElementComponent[HTMLButtonElement]("button") {

  def buttonType: String =
    getAttribute("type").getOrElse("submit")

  def buttonType_=(value: String): Unit =
    setAttribute("type", value)

  def addClick(listener: Event => Unit): Disposable =
    addEventListener("click", listener)

}

object Button {

  def button(label: String): Button =
    button(label)({})

  def button(label: String)(init: Button ?=> Unit): Button =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new Button()
      component.textContent = label
      component.buttonType = "button"

      DslRuntime.withComponentContext(ComponentContext(None)) {
        given Scope = currentScope
        given Button = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def buttonType(using button: Button): String =
    button.buttonType

  def buttonType_=(value: String)(using button: Button): Unit =
    button.buttonType = value

}
