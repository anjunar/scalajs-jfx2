package jfx.form

import jfx.core.component.Box
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime

class SubForm[M](val $name: String) extends Box("fieldset") with Formular[M] with Control[M] {

  override val tagName: String = "fieldset"

  override val $valueProperty: Property[M] = Property(null.asInstanceOf[M])

  override def compose(): Unit = {
    given jfx.core.component.Component = this
    super.compose()

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
}
