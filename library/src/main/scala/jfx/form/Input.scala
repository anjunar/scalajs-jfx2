package jfx.form

import jfx.core.component.Component
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Input(override val name: String, override val standalone: Boolean = false) extends Component with Control[String] {
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
    addDisposable(host.addEventListener("blur", _ => {
      setFocused(false)
      validate()
    }))

    addDisposable(valueProperty.observe { v =>
       host.domNode.foreach(_.asInstanceOf[dom.html.Input].value = v)
       if (dirtyProperty.get) validate()
    })

    addDisposable(placeholderProperty.observe { p =>
       host.domNode.foreach(_.asInstanceOf[dom.html.Input].placeholder = if (p == null) "" else p)
    })

    addDisposable(validators.observe(_ => validate()))

    // Register with FormContext if available and not standalone
    if (!standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case e: Exception => 
          dom.console.warn(s"Input $name could not resolve FormContext", e.getMessage)
      }
    }
  }
}

object Input {
  def input(name: String)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name))(init)
  }

  def standaloneInput(name: String)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name, true))(init)
  }

  def placeholder(using i: Input): String = i.placeholder
  def placeholder_=(value: String)(using i: Input): Unit = i.placeholder = value
  def stringValueProperty(using i: Input): Property[String] = i.stringValueProperty
  def validators(using i: Input): jfx.core.state.ListProperty[jfx.form.validators.Validator[String]] = i.validators
  def errorsProperty(using i: Input): jfx.core.state.ListProperty[String] = i.errorsProperty
}
