package jfx.layout

import jfx.core.component.Box
import jfx.dsl.DslRuntime

class HorizontalLine extends Box("hr")

object HorizontalLine {
  def horizontalLine(init: HorizontalLine ?=> Unit = {}): HorizontalLine = {
    DslRuntime.build(new HorizontalLine())(init)
  }
}
