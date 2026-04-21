package jfx.form

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Input(override val name: String, override val standalone: Boolean = false) extends Component with Control[String] {
  override def tagName: String = "input"

  override val valueProperty: Property[String] = Property("")
  def stringValueProperty: Property[String] = valueProperty

  override def compose(): Unit = {
    given Component = this

    attribute("name", name)
    
    onInput { _ =>
      setDirty(true)
      valueProperty.set(nativeValue)
    }

    onFocus(_ => setFocused(true))
    onBlur { _ =>
      setFocused(false)
      validate()
    }

    bindNativeValue()
    bindNativePlaceholder()

    addDisposable(validators.observe(_ => validate()))
    addDisposable(dirtyProperty.observe(_ => validate()))

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

  private def nativeInput: Option[dom.html.Input] =
    host.domNode.collect { case input: dom.html.Input => input }

  private def nativeValue: String =
    nativeInput.map(_.value).getOrElse("")

  private def bindNativeValue(): Unit = {
    addDisposable(valueProperty.observe { value =>
      nativeInput.foreach(_.value = value)
      validate()
    })
  }

  private def bindNativePlaceholder(): Unit = {
    addDisposable(placeholderProperty.observe { placeholder =>
      nativeInput.foreach(_.placeholder = if (placeholder == null) "" else placeholder)
    })
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
