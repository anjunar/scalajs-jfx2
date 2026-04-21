package jfx.form

import jfx.core.component.Box
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime

class Form extends Box("form") with FormContext {
  val controls: ListProperty[Control[?]] = new ListProperty[Control[?]]()

  override def registerControl(control: Control[?]): Unit = {
    if (!controls.contains(control)) {
      controls += control
    }
  }

  override def unregisterControl(control: Control[?]): Unit = {
    val idx = controls.indexOf(control)
    if (idx >= 0) controls.remove(idx)
  }

  override def clearErrors(): Unit = {
    controls.foreach { control =>
      control.setErrors(Nil)
    }
  }

  override def resetInteractionState(): Unit = {
    controls.foreach { control =>
      control.setDirty(false)
      control.setErrors(Nil)
    }
  }

  override def compose(): Unit = {
    given jfx.core.component.Component = this
    super.compose()
    onSubmit { event =>
      event.preventDefault()
    }
  }
}

object Form {
  def form(init: Form ?=> Unit): Form = {
    val f = new Form()
    DslRuntime.build(f) {
      DslRuntime.provide[FormContext](f) {
        init(using f)
      }
    }
  }

  def controls(using f: Form): ListProperty[Control[?]] = f.controls

  def clearErrors()(using f: Form): Unit = f.clearErrors()
}
