package jfx.form

import jfx.core.state.Property

trait Editable {
  val editableProperty: Property[Boolean] = Property(true)

  def editable: Boolean = editableProperty.get

  def editable_=(value: Boolean): Unit = editableProperty.set(value)
}
