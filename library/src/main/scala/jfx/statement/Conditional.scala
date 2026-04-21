package jfx.statement

import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime

class Conditional(condition: ReadOnlyProperty[Boolean]) extends Condition(condition)

object Conditional {

  def conditional(condition: ReadOnlyProperty[Boolean])(init: Conditional ?=> Unit): Conditional = {
    DslRuntime.build(new Conditional(condition)) {
      init
      val c = summon[Conditional]
      c.renderInternal()
    }
  }

  def thenDo(builder: Conditional ?=> Unit)(using c: Conditional): Unit =
    c.registerThen {
      builder(using c)
    }

  def elseDo(builder: Conditional ?=> Unit)(using c: Conditional): Unit =
    c.registerElse {
      builder(using c)
    }
}
