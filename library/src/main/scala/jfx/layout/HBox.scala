package jfx.layout

import jfx.core.component.NativeComponent
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.HTMLDivElement

class HBox extends NativeComponent[HTMLDivElement]("div") {
  classProperty += "jfx-hbox"
}

object HBox {
  def hbox(init: HBox ?=> Unit): HBox =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new HBox()

      DslRuntime.withComponentContext(ComponentContext(Some(component))) {
        given Scope = currentScope
        given HBox = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
