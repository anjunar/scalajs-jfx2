package jfx.core.component

import jfx.dsl.DslRuntime
import org.scalajs.dom

class Box(val tagName: String = "div") extends Component

object Box {
  def box(tagName: String = "div")(init: Box ?=> Unit): Box = {
    DslRuntime.build(new Box(tagName))(init)
  }
}
