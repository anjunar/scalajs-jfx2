package jfx.form

import jfx.core.text.TextValue

trait HasPlaceholder[-C <: Control[?]] {
  def placeholder(using c: C): String =
    c.$placeholder

  def placeholder_=[T](using c: C)(value: T)(using textValue: TextValue[T]): Unit =
    c.$placeholder = textValue.asReadOnlyProperty(value)
}

object HasPlaceholder extends HasPlaceholder[Control[?]]
