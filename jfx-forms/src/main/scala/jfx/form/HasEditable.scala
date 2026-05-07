package jfx.form

import jfx.core.state.{Property, ReadOnlyProperty}

trait HasEditable[-C <: Editable] {
  def editable(using c: C): Boolean =
    c.$editableProperty.get

  def editable_=(using c: C)(value: Boolean): Unit =
    c.$editableProperty.set(value)

  def editable_=(using c: C)(value: ReadOnlyProperty[Boolean]): Unit =
    c.$editable = value

  def editableProperty(using c: C): Property[Boolean] =
    c.$editableProperty
}

object HasEditable extends HasEditable[Editable]
