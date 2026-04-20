package jfx.form

import jfx.core.component.Component
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import org.scalajs.dom.HTMLElement

trait Control[V] extends Component with Editable {
  val name: String
  val standalone: Boolean = false

  val placeholderProperty: Property[String] = Property("")
  val focusedProperty: Property[Boolean] = Property(false)
  val dirtyProperty: Property[Boolean] = Property(false)

  val errorsProperty: ListProperty[String] = new ListProperty[String]()

  val invalidProperty: ReadOnlyProperty[Boolean] =
    errorsProperty.map(_.length > 0)

  val valueProperty: Property[V]

  def placeholder: String = placeholderProperty.get
  def placeholder_=(value: String): Unit = placeholderProperty.set(value)

  def setFocused(value: Boolean): Unit = focusedProperty.set(value)
  def setDirty(value: Boolean): Unit = dirtyProperty.set(value)
  def setErrors(values: IterableOnce[String]): Unit = errorsProperty.setAll(values)
  
  // Validation could be added here
}
