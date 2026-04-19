package jfx.core.component

import jfx.dsl.DslRuntime
import org.scalajs.dom

class Box(val tagName: String = "div") extends Component {
  def classes: Seq[String] = host.attribute("class").getOrElse("").split(" ").toSeq
  def classes_=(names: Seq[String]): Unit = host.setClassNames(names)
}

object Box {
  def box(tagName: String = "div")(init: Box ?=> Unit): Box = {
    DslRuntime.build(new Box(tagName))(init)
  }
  
  def text(value: String): TextComponent = {
    Text.text(value)
  }

  def classes(using b: Box): Seq[String] = b.classes
  def classes_=(names: Seq[String])(using b: Box): Unit = b.classes = names

  def text_=(value: String)(using b: Box): Unit = {
    Text.text(value)
  }

  def addDisposable(d: jfx.core.state.Disposable)(using c: Component): Unit = c.addDisposable(d)
}
