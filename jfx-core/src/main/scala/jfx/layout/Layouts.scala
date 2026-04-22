package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.dsl.DslRuntime

object Div {
  def div(init: Box ?=> Unit): Box = {
    Box.box("div")(init)
  }
}

object VBox {
  def vbox(init: Box ?=> Unit): Box = {
    DslRuntime.build(new Box("div")) {
      addClass("vbox")
      init
    }
  }
}

object HBox {
  def hbox(init: Box ?=> Unit): Box = {
    DslRuntime.build(new Box("div")) {
      addClass("hbox")
      init
    }
  }
}
