package jfx.core.text

import jfx.core.state.{Property, ReadOnlyProperty}

trait TextValue[-T] {
  def asReadOnlyProperty(value: T): ReadOnlyProperty[String]
}

object TextValue {
  given stringTextValue: TextValue[String] with
    override def asReadOnlyProperty(value: String): ReadOnlyProperty[String] =
      Property(Option(value).getOrElse(""))

  given propertyTextValue: TextValue[ReadOnlyProperty[String]] with
    override def asReadOnlyProperty(value: ReadOnlyProperty[String]): ReadOnlyProperty[String] =
      value
}
