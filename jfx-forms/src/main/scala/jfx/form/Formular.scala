package jfx.form

import jfx.core.component.Component
import jfx.core.state.{CompositeDisposable, Disposable, ListProperty, Property, ReadOnlyProperty}
import jfx.form.validators.{Validator, ValidatorFactory}
import org.scalajs.dom.console
import reflect.{ClassDescriptor, PropertyAccessor, PropertyDescriptor}

import scala.collection.mutable

trait Formular[M] extends FormContext with Editable {
  self: Component =>

  val name: String

  val valueProperty: ReadOnlyProperty[M] = Property(null.asInstanceOf[M])

  val controls: ListProperty[Control[?]] = new ListProperty[Control[?]]()

  private val bindingsByControl: mutable.Map[Control[?], CompositeDisposable] =
    mutable.Map.empty

  private val boundValidatorsByControl: mutable.Map[Control[?], Vector[Validator[Any]]] =
    mutable.Map.empty

  def registerControl(control: Control[?]): Unit = {
    if (!controls.contains(control)) {
      controls += control

      val binding = initBinding(control)
      bindOrDefer(control, binding)
    }
  }

  def unregisterControl(control: Control[?]): Unit = {
    disposeBinding(control)
    val idx = controls.indexOf(control)
    if (idx >= 0) controls.remove(idx)
  }

  def setErrorResponses(errors: Seq[ErrorResponse]): Unit = {
    errors.groupBy(error => error.path.apply(0).toString)
      .foreach((key, errors) => {
        controls.foreach {
          case subForm: SubForm[?] if subForm.name == key => subForm.setErrorResponses(errors.map(error => new ErrorResponse(error.message, error.path.tail)))
          case control: Control[?] if control.name == key => control.setErrors(errors.map(error => error.message))
          case _ => ()
        }
      })
  }

  def clearErrors(): Unit = {
    controls.foreach { control =>
      control.setErrors(Nil)
      control match {
        case subForm: Formular[?] => subForm.clearErrors()
        case _ => ()
      }
    }
  }

  def resetInteractionState(): Unit = {
    controls.foreach { control =>
      control.setDirty(false)
      control.setErrors(Nil)
      control match {
        case nestedForm: Formular[?] => nestedForm.resetInteractionState()
        case _ => ()
      }
    }
  }

  private def initBinding(control: Control[?]): CompositeDisposable = {
    bindingsByControl.remove(control).foreach(_.dispose())
    val composite = new CompositeDisposable()
    bindingsByControl.put(control, composite)
    composite
  }

  private def disposeBinding(control: Control[?]): Unit =
    bindingsByControl.remove(control).foreach(_.dispose())

  private def clearBoundValidators(control: Control[?]): Unit = {
    val previousValidators = boundValidatorsByControl.remove(control).getOrElse(Vector.empty)
    if (previousValidators.nonEmpty) {
      previousValidators.foreach(v => removeValidator(control, v))
    }
  }

  private def bindOrDefer(control: Control[?], binding: CompositeDisposable): Unit = {
    val currentModel = valueProperty.get
    if (currentModel != null) {
      binding.add(bindNow(control))
      return
    }

    val observer = valueProperty.observe { model =>
      if (model != null) {
        binding.add(bindNow(control))
      } else {
        control.valueProperty match {
          case property: Property[Any @unchecked] => property.set(null.asInstanceOf[Any])
          case listProperty: ListProperty[?] => listProperty.clear()
        }
      }
    }
    binding.add(observer)
  }

  private def bindNow(control: Control[?]): Disposable = {
    val controlName = control.name
    val model = valueProperty.get

    if (model == null) return () => ()

    val (modelPropertyOption, accessValidators) = control match {
      case subForm: SubForm[?] =>
        val accessOption = findPropertyAccessOption(model, controlName)
        val descriptorOption = findPropertyDescriptorOption(model, controlName)
        (
          accessOption.map(_.get(model)),
          descriptorOption.map(d => ValidatorFactory.createValidators(d.annotations)).getOrElse(Vector.empty)
        )
      case _ =>
        val accessOption = findPropertyAccessOption(model, controlName)
        val descriptorOption = findPropertyDescriptorOption(model, controlName)
        (
          accessOption.map(_.get(model)),
          descriptorOption.map(d => ValidatorFactory.createValidators(d.annotations)).getOrElse(Vector.empty)
        )
    }

    if (modelPropertyOption.isEmpty) {
      console.warn(s"Skipping form binding for control '$controlName' because no matching model property was found on ${model.getClass.getName}.")
      return () => ()
    }

    if (accessValidators.nonEmpty) {
//      console.log(s"Binding control '$controlName' with ${accessValidators.size} validators: ${accessValidators.map(_.getClass.getSimpleName).mkString(", ")}")
    }

    syncControlValidators(control, accessValidators)

    val modelProperty: Any = modelPropertyOption.get
    val controlProperty: Any = control.valueProperty

    (modelProperty, controlProperty) match {
      case (m: ListProperty[Any @unchecked], c: ListProperty[Any @unchecked]) =>
        ListProperty.subscribeBidirectional(m, c)
      case (m: Property[Any @unchecked], c: Property[Any @unchecked]) =>
        Property.subscribeBidirectional(m, c)
      case _ =>
        console.warn(s"Property type mismatch for control '$controlName'")
        () => ()
    }
  }

  private def syncControlValidators(control: Control[?], validators: Vector[Validator[Any]]): Unit = {
    clearBoundValidators(control)
    if (validators.nonEmpty) {
      validators.foreach(v => addValidatorIfMissing(control, v))
      boundValidatorsByControl.put(control, validators)
    }
  }

  private def addValidatorIfMissing(control: Control[?], validator: Validator[Any]): Unit = {
    val raw = control.validators.asInstanceOf[ListProperty[Validator[Any]]]
    if (!raw.exists(_ == validator)) {
      raw += validator
    }
  }

  private def removeValidator(control: Control[?], validator: Validator[Any]): Unit = {
    val raw = control.validators.asInstanceOf[ListProperty[Validator[Any]]]
    val idx = raw.indexWhere(_ == validator)
    if (idx >= 0) raw.remove(idx)
  }

  private def findPropertyAccessOption(model: Any, propertyName: String): Option[PropertyAccessor[Any, Any]] =
    findPropertyDescriptorOption(model, propertyName).flatMap(_.accessor).map(_.asInstanceOf[PropertyAccessor[Any, Any]])

  private def findPropertyDescriptorOption(model: Any, propertyName: String): Option[PropertyDescriptor] =
    modelDescriptor(model).flatMap(_.getProperty(propertyName))

  private def modelDescriptor(model: Any): Option[ClassDescriptor] =
    if (model == null) None
    else ClassDescriptor.maybeForName(model.getClass.getName)
      .orElse(ClassDescriptor.maybeForName(model.getClass.getSimpleName))

}
