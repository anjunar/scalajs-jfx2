package jfx.form

import jfx.core.component.NativeComponent
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.HTMLDivElement

class InputContainer extends NativeComponent[HTMLDivElement]("div") {
  classProperty += "jfx-input-container"
}

object InputContainer {
  def inputContainer(init: InputContainer ?=> Unit): InputContainer =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new InputContainer()

      DslRuntime.withComponentContext(ComponentContext(Some(component))) {
        given Scope = currentScope
        given InputContainer = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
