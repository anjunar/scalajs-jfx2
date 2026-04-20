package jfx.form

import jfx.core.component.Component
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Input(override val name: String) extends Component with Control[String] {
  override def tagName: String = "input"

  override val valueProperty: Property[String] = Property("")
  def stringValueProperty: Property[String] = valueProperty

  override def bind(cursor: jfx.core.render.Cursor): Unit = {
    super.bind(cursor)
    host.setAttribute("name", name)
    
    addDisposable(host.addEventListener("input", _ => {
      val value = host.domNode.get.asInstanceOf[dom.html.Input].value
      setDirty(true)
      valueProperty.set(value)
    }))

    addDisposable(host.addEventListener("focus", _ => setFocused(true)))
    addDisposable(host.addEventListener("blur", _ => setFocused(false)))

    addDisposable(valueProperty.observe { v =>
       host.domNode.foreach(_.asInstanceOf[dom.html.Input].value = v)
    })

    // Register with FormContext if available
    try {
      val formContext = DslRuntime.service[FormContext]
      formContext.registerControl(this)
      addDisposable(() => formContext.unregisterControl(this))
    } catch {
      case _: Exception => // Not inside a FormContext, standalone
    }
  }
}

object Input {
  def input(name: String)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name))(init)
  }

  def placeholder(using i: Input): String = i.placeholder
  def placeholder_=(value: String)(using i: Input): Unit = i.placeholder = value
  def stringValueProperty(using i: Input): Property[String] = i.stringValueProperty
}
