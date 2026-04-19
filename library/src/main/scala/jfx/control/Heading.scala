package jfx.control

import jfx.core.component.Box
import jfx.dsl.DslRuntime

class Heading(level: Int) extends Box(s"h${math.max(1, math.min(6, level))}")

object Heading {
  def heading(level: Int)(init: Heading ?=> Unit): Heading = {
    DslRuntime.build(new Heading(level))(init)
  }
}
