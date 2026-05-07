package jfx.form

import jfx.core.component.Component
import jfx.core.state.{Property, ReadOnlyProperty}

trait Editable {
  self: Component =>

  val $editableProperty: Property[Boolean] = Property(true)

  def $editable: Boolean = $editableProperty.get
  def $editable_=(value: Boolean): Unit = $editableProperty.set(value)
  def $editable_=(value: ReadOnlyProperty[Boolean]): Unit =
    addDisposable(value.observe(next => $editableProperty.set(next)))
}
