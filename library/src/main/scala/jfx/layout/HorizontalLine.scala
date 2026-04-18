package jfx.layout

import jfx.core.component.ElementComponent
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.HTMLHRElement

class HorizontalLine extends ElementComponent[HTMLHRElement]("hr") {
  classProperty += "jfx-horizontal-line"
}

object HorizontalLine {
  def horizontalLine(init: HorizontalLine ?=> Unit = {}): HorizontalLine =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new HorizontalLine()

      DslRuntime.withComponentContext(ComponentContext(None)) {
        given Scope = currentScope
        given HorizontalLine = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
