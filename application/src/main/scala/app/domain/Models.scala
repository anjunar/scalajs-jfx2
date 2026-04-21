package app.domain

import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*
import scala.annotation.meta.field

class Address(
  @(NotBlank @field)(message = "Straße darf nicht leer sein")
  var street: Property[String] = Property(""),
  @(NotBlank @field)(message = "Stadt darf nicht leer sein")
  var city: Property[String] = Property("")
)

class Email(
  @(EmailConstraint @field)()
  var value: Property[String] = Property("")
)

class User(
  @(Size @field)(min = 3, message = "Name muss mindestens 3 Zeichen lang sein")
  @(NotBlank @field)()
  var name: Property[String] = Property(""),
  @(EmailConstraint @field)()
  var email: Property[String] = Property(""),
  var address: Property[Address] = Property(new Address()),
  var emails: ListProperty[Email] = new ListProperty[Email]()
)
