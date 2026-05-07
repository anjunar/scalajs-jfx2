package jfx.form

import jfx.core.state.ReadOnlyProperty

trait HasPlaceholder[-C <: Control[?]] {
  def placeholder(using c: C): String =
    c.$placeholder

  def placeholder_=(using c: C)(value: String): Unit =
    c.$placeholder = value

  def placeholder_=(using c: C)(value: ReadOnlyProperty[String]): Unit =
    c.$placeholder = value
}

object HasPlaceholder extends HasPlaceholder[Control[?]]
