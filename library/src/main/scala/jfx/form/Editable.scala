package jfx.form

import jfx.core.state.Property

trait Editable {

  val editableProperty: Property[Boolean] = Property(true)

  def editable: Boolean =
    editableProperty.get

  def editable_=(value: Boolean): Unit =
    editableProperty.set(value)

}

object Editable {

  def editableProperty(using editable: Editable): Property[Boolean] =
    editable.editableProperty

  def editable(using editable: Editable): Boolean =
    editable.editable

  def editable_=(value: Boolean)(using editable: Editable): Unit =
    editable.editable = value

}
