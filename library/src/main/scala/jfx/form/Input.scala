package jfx.form

import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.{Event, HTMLInputElement}

class Input(override val name: String, override val standalone: Boolean = false)
    extends Control[String | Boolean | Double, HTMLInputElement]("input", name, standalone) {

  override val valueProperty: Property[String | Boolean | Double] =
    Property(null.asInstanceOf[String | Boolean | Double])

  private var currentInputType = "text"
  private var currentDisabled = false
  private var currentReadOnly = false

  setAttribute("name", name)

  private val updateFromDom: Event => Unit = _ => {
    dirtyProperty.set(true)
    valueProperty.set(readElementValue())
  }

  addEventListener("input", updateFromDom)
  addEventListener("change", updateFromDom)
  addEventListener("focus", _ => focusedProperty.set(true))
  addEventListener("blur", _ => focusedProperty.set(false))

  private val valueObserver =
    valueProperty.observe(applyElementValue)
  addDisposable(valueObserver)

  private val editableObserver =
    editableProperty.observe(editable => readOnly = !editable)
  addDisposable(editableObserver)

  private val placeholderObserver =
    placeholderProperty.observe { value =>
      if (value == null || value.isEmpty) removeAttribute("placeholder")
      else setAttribute("placeholder", value)
    }
  addDisposable(placeholderObserver)

  def stringValueProperty: Property[String] =
    valueProperty.asInstanceOf[Property[String]]

  def booleanValueProperty: Property[Boolean] =
    valueProperty.asInstanceOf[Property[Boolean]]

  def numberValueProperty: Property[Double] =
    valueProperty.asInstanceOf[Property[Double]]

  def disabled: Boolean =
    currentDisabled

  def disabled_=(value: Boolean): Unit = {
    currentDisabled = value
    hostElement.setBooleanAttribute("disabled", value)
    hostElement.domElementOption.collect { case input: HTMLInputElement => input.disabled = value }
  }

  def readOnly: Boolean =
    currentReadOnly

  def readOnly_=(value: Boolean): Unit = {
    currentReadOnly = value
    hostElement.setBooleanAttribute("readonly", value)
    hostElement.domElementOption.collect { case input: HTMLInputElement => input.readOnly = value }
  }

  def inputType: String =
    currentInputType

  def inputType_=(value: String): Unit = {
    currentInputType =
      if (value == null || value.isBlank) "text"
      else value

    if (currentInputType == "text") removeAttribute("type")
    else setAttribute("type", currentInputType)

    hostElement.domElementOption.collect {
      case input: HTMLInputElement => input.`type` = currentInputType
    }
    applyElementValue(valueProperty.get)
  }

  private def applyElementValue(value: String | Boolean | Double): Unit =
    currentInputType match {
      case "checkbox" =>
        val checked =
          value match {
            case bool: Boolean => bool
            case _ => false
          }

        hostElement.setBooleanAttribute("checked", checked)
        hostElement.domElementOption.collect {
          case input: HTMLInputElement => input.checked = checked
        }

      case "number" =>
        val rendered =
          value match {
            case number: Double if !number.isNaN => Some(number.toString)
            case _ => None
          }

        rendered match {
          case Some(number) => setAttribute("value", number)
          case None => removeAttribute("value")
        }

        hostElement.domElementOption.collect {
          case input: HTMLInputElement =>
            rendered match {
              case Some(_) => input.valueAsNumber = value.asInstanceOf[Double]
              case None => input.value = ""
            }
        }

      case _ =>
        val rendered =
          if (value == null) ""
          else value.toString

        if (rendered.isEmpty) removeAttribute("value")
        else setAttribute("value", rendered)

        hostElement.domElementOption.collect {
          case input: HTMLInputElement => input.value = rendered
        }
    }

  private def readElementValue(): String | Boolean | Double =
    hostElement.domElementOption.collect { case input: HTMLInputElement =>
      currentInputType match {
        case "checkbox" =>
          input.checked
        case "number" =>
          if (input.value.trim.isEmpty) null.asInstanceOf[String | Boolean | Double]
          else input.valueAsNumber
        case _ =>
          input.value
      }
    }.getOrElse(valueProperty.get)

  override def toString: String =
    s"Input($valueProperty, $name)"

}

object Input {

  def input(name: String): Input =
    input(name)({})

  def input(name: String)(init: Input ?=> Unit): Input =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new Input(name)

      DslRuntime.withComponentContext(ComponentContext(None)) {
        given Scope = currentScope
        given Input = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def standaloneInput(name: String)(init: Input ?=> Unit): Input =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new Input(name, standalone = true)

      DslRuntime.withComponentContext(ComponentContext(None)) {
        given Scope = currentScope
        given Input = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def placeholder(using component: Input): String =
    component.placeholder

  def placeholder_=(value: String)(using component: Input): Unit =
    component.placeholder = value

  def value(using input: Input): String | Boolean | Double =
    input.valueProperty.get

  def value_=(nextValue: String | Boolean | Double)(using input: Input): Unit =
    input.valueProperty.set(nextValue)

  def inputType(using input: Input): String =
    input.inputType

  def inputType_=(value: String)(using input: Input): Unit =
    input.inputType = value

  def stringValueProperty(using input: Input): Property[String] =
    input.stringValueProperty

  def booleanValueProperty(using input: Input): Property[Boolean] =
    input.booleanValueProperty

  def numberValueProperty(using input: Input): Property[Double] =
    input.numberValueProperty

  def disabled(using input: Input): Boolean =
    input.disabled

  def disabled_=(value: Boolean)(using input: Input): Unit =
    input.disabled = value

  def readOnly(using input: Input): Boolean =
    input.readOnly

  def readOnly_=(value: Boolean)(using input: Input): Unit =
    input.readOnly = value

}
