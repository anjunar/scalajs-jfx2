package jfx.form

import jfx.core.component.Component
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.validators.Validator
import org.scalajs.dom.HTMLElement

trait Control[V] extends Component with Editable {
  val name: String
  val standalone: Boolean = false

  val placeholderProperty: Property[String] = Property("")
  val focusedProperty: Property[Boolean] = Property(false)
  val dirtyProperty: Property[Boolean] = Property(false)

  val validators: ListProperty[Validator[V]] = new ListProperty[Validator[V]]()
  val errorsProperty: ListProperty[String] = new ListProperty[String]()

  val invalidProperty: ReadOnlyProperty[Boolean] =
    errorsProperty.map(_.length > 0)

  val valueProperty: ReadOnlyProperty[V]

  def placeholder: String = placeholderProperty.get
  def placeholder_=(value: String): Unit = placeholderProperty.set(value)

  def setFocused(value: Boolean): Unit = focusedProperty.set(value)
  def setDirty(value: Boolean): Unit = dirtyProperty.set(value)
  def setErrors(values: IterableOnce[String]): Unit = errorsProperty.setAll(values)
  
  def validate(forceVisible: Boolean = false): Seq[String] = {
    val errors =
      if (!editableProperty.get) Seq.empty
      else validators.iterator.flatMap(_.validate(valueProperty.get)).toSeq

    if (forceVisible || dirtyProperty.get) {
      if (forceVisible) setDirty(true)
      if (errors.nonEmpty) {
        org.scalajs.dom.console.log(s"Control '$name' invalid: ${errors.mkString(", ")}")
      }
      errorsProperty.setAll(errors)
    } else {
      errorsProperty.setAll(Nil)
    }
    
    errors
  }
}
