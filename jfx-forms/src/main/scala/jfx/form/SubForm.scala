package jfx.form

import jfx.core.component.Box
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime

class SubForm[M](val $name: String) extends Box("fieldset") with Formular[M] with Control[M] {

  override val tagName: String = "fieldset"

  override val $valueProperty: Property[M] = Property(null.asInstanceOf[M])
  private var _factory: (() => M) | Null = null

  def $factory: (() => M) | Null = _factory
  def $factory_=(value: () => M): Unit =
    _factory = value

  override def registerControl(control: Control[?]): Unit = {
    super.registerControl(control)
    control.$editableProperty.set($editableProperty.get)
  }

  override def compose(): Unit = {
    given jfx.core.component.Component = this
    super.compose()

    addDisposable($editableProperty.observe { editable =>
      $controls.foreach(_. $editableProperty.set(editable))
    })

    if (!$standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case _: Exception => 
      }
    }
  }

  def clearForm(): Unit = {
    $valueProperty.set(null.asInstanceOf[M])
    clearErrors()
    resetInteractionState()
  }

  def newInstance(): Unit = {
    Option(_factory).foreach { factory =>
      $valueProperty.set(factory())
      clearErrors()
      resetInteractionState()
    }
  }
}

object SubForm {
  def subForm[M](name: String)(init: SubForm[M] ?=> Unit): SubForm[M] = {
    val f = new SubForm[M](name)
    DslRuntime.build(f) {
      DslRuntime.provide[FormContext](f) {
        init(using f)
      }
    }
  }

  def editable[M](using f: SubForm[M]): Boolean = f.$editable
  def editable_=[M](using f: SubForm[M])(value: Boolean): Unit = f.$editable = value
  def editableProperty[M](using f: SubForm[M]): Property[Boolean] = f.$editableProperty
  def factory[M](using f: SubForm[M]): (() => M) | Null = f.$factory
  def factory_=[M](using f: SubForm[M])(value: () => M): Unit = f.$factory = value
  def clearForm[M]()(using f: SubForm[M]): Unit = f.clearForm()
  def newInstance[M]()(using f: SubForm[M]): Unit = f.newInstance()
}
