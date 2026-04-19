package jfx.form

import jfx.core.component.Component
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Input(val name: String) extends Component {
  override def tagName: String = "input"

  val stringValueProperty = Property("")

  def placeholder: String = host.attribute("placeholder").getOrElse("")
  def placeholder_=(value: String): Unit = host.setAttribute("placeholder", value)

  override def bind(cursor: jfx.core.render.Cursor): Unit = {
    super.bind(cursor)
    host.setAttribute("name", name)
    
    // Sync DOM to Property
    addDisposable(host.addEventListener("input", _ => {
      val value = host.domNode.get.asInstanceOf[dom.html.Input].value
      stringValueProperty.set(value)
    }))

    // Sync Property to DOM
    addDisposable(stringValueProperty.observe { v =>
       host.domNode.foreach(_.asInstanceOf[dom.html.Input].value = v)
    })
  }
}

object Input {
  def input(name: String)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name))(init)
  }

  def classes_=(names: Seq[String])(using i: Input): Unit = i.host.setClassNames(names)
  def placeholder(using i: Input): String = i.placeholder
  def placeholder_=(value: String)(using i: Input): Unit = i.placeholder = value
  def stringValueProperty(using i: Input): Property[String] = i.stringValueProperty
}
