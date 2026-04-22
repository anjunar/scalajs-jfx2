package jfx.layout

import jfx.core.component.Box
import jfx.dsl.DslRuntime

class Span extends Box("span")

object Span {
  def span(init: Span ?=> Unit): Span = {
    DslRuntime.build(new Span())(init)
  }
}
