package jfx.form

import jfx.core.state.{ListProperty, Property}

trait FormContext {
  def registerControl(control: Control[?]): Unit
  def unregisterControl(control: Control[?]): Unit

  def clearErrors(): Unit
  def resetInteractionState(): Unit
}
