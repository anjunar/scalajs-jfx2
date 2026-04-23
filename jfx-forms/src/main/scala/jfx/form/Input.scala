package jfx.form

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
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
      if (editableProperty.get) {
//        org.scalajs.dom.console.log(s"Input '$name' changed to: $nativeValue")
        setDirty(true)
        valueProperty.set(nativeValue)
      } else {
        syncNativeValue(valueProperty.get)
      }
    }

    onFocus(_ => setFocused(true))
    onBlur { _ =>
      setFocused(false)
      validate()
    }

    bindNativeValue()
    bindNativePlaceholder()
    bindNativeEditable()

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

  private def nativeValue: String =
    host.property[String]("value").getOrElse("")

  private def bindNativeValue(): Unit = {
    addDisposable(valueProperty.observe { value =>
      syncNativeValue(value)
      validate()
    })
  }

  private def bindNativePlaceholder(): Unit = {
    addDisposable(placeholderProperty.observe { placeholder =>
      host.setAttribute("placeholder", if (placeholder == null) "" else placeholder)
    })
  }

  private def bindNativeEditable(): Unit = {
    addDisposable(editableProperty.observe { editable =>
      syncNativeEditable(editable)
    })
  }

  private def syncNativeValue(value: String): Unit =
    host.setProperty("value", Option(value).getOrElse(""))

  private def syncNativeEditable(editable: Boolean): Unit = {
    host.setProperty("readOnly", !editable)
    if (!editable) {
      syncNativeValue(valueProperty.get)
    }
  }
}

object Input {
  def input(name: String, standalone: Boolean = false)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name, standalone))(init)
  }

  @deprecated("Use input(name, standalone = true) instead.", "2.0.0")
  def standaloneInput(name: String)(init: Input ?=> Unit): Input = {
    DslRuntime.build(new Input(name, true))(init)
  }

  def placeholder(using i: Input): String = i.placeholder
  def placeholder_=(value: String)(using i: Input): Unit = i.placeholder = value
  def placeholder_=(value: ReadOnlyProperty[String])(using i: Input): Unit = i.placeholder = value
  def editable(using i: Input): Boolean = i.editableProperty.get
  def editable_=(value: Boolean)(using i: Input): Unit = i.editableProperty.set(value)
  def editableProperty(using i: Input): Property[Boolean] = i.editableProperty
  def stringValueProperty(using i: Input): Property[String] = i.stringValueProperty
  def validators(using i: Input): jfx.core.state.ListProperty[jfx.form.validators.Validator[String]] = i.validators
  def errorsProperty(using i: Input): jfx.core.state.ListProperty[String] = i.errorsProperty
}
