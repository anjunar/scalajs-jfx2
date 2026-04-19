package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.dsl.DslRuntime

class Viewport extends Box("div") {
  override def compose(): Unit = {
    classes = Seq("jfx-viewport")
  }
}

object Viewport {
  def viewport(init: Viewport ?=> Unit): Viewport = {
    DslRuntime.build(new Viewport())(init)
  }
  
  def mount(component: Component)(using v: Viewport): Unit = {
  }
}
