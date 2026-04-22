package app.domain

import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*
import scala.annotation.meta.field

class Address(
  @(NotBlank @field)(message = "Street must not be empty")
  var street: Property[String] = Property(""),
  @(NotBlank @field)(message = "City must not be empty")
  var city: Property[String] = Property("")
)

class Email(
  @(EmailConstraint @field)()
  var value: Property[String] = Property("")
)

class User(
  @(Size @field)(min = 3, message = "Name must be at least 3 characters long")
  @(NotBlank @field)()
  var name: Property[String] = Property(""),
  @(EmailConstraint @field)()
  var email: Property[String] = Property(""),
  var address: Property[Address] = Property(new Address()),
  var emails: ListProperty[Email] = new ListProperty[Email]()
)
