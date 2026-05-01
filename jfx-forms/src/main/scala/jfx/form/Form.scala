package jfx.form

import jfx.core.component.Box
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime

class Form[M](val $name: String = "default") extends Box("form") with Formular[M] {

  override def compose(): Unit = {
    given jfx.core.component.Component = this
    super.compose()
    onSubmit { event =>
      event.preventDefault()
    }
  }

  def setModel(model: M): Unit = {
    $valueProperty.asInstanceOf[Property[M]].set(model)
  }
}

object Form {
  def form[M](model: M)(init: Form[M] ?=> Unit): Form[M] = {
    val f = new Form[M]()
    f.setModel(model)
    DslRuntime.build(f) {
      DslRuntime.provide[FormContext](f) {
        init(using f)
      }
    }
  }

  def form(init: Form[Any] ?=> Unit): Form[Any] = {
    val f = new Form[Any]()
    DslRuntime.build(f) {
      DslRuntime.provide[FormContext](f) {
        init(using f)
      }
    }
  }

  def controls[M](using f: Form[M]): jfx.core.state.ListProperty[Control[?]] = f.$controls

  def editable[M](using f: Form[M]): Boolean = f.$editable
  def editable_=[M](using f: Form[M])(value: Boolean): Unit = f.$editable = value
  def editableProperty[M](using f: Form[M]): Property[Boolean] = f.$editableProperty
}
