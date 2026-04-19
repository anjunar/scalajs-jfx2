package jfx.core.component

import jfx.dsl.DslRuntime
import org.scalajs.dom

class Box(val tagName: String = "div") extends Component {
  def classes: Seq[String] = host.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
  def classes_=(names: Seq[String]): Unit = host.setClassNames(names)
}

object Box {
  def box(tagName: String = "div")(init: Box ?=> Unit): Box = {
    DslRuntime.build(new Box(tagName))(init)
  }
}
