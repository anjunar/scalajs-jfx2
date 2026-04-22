package jfx.form.validators

import scala.annotation.StaticAnnotation

final case class NotNull(message: String = "Must not be null") extends StaticAnnotation

final case class NotEmpty(message: String = "Must not be empty") extends StaticAnnotation

final case class NotBlank(message: String = "Must not be blank") extends StaticAnnotation

final case class Size(
  min: Int = 0,
  max: Int = Int.MaxValue,
  message: String = ""
) extends StaticAnnotation

final case class Min(value: Long, message: String = "") extends StaticAnnotation

final case class Max(value: Long, message: String = "") extends StaticAnnotation

final case class Digits(
  integer: Int = 0,
  fraction: Int = 0,
  message: String = ""
) extends StaticAnnotation

final case class Pattern(
  regex: String,
  message: String = "Has an invalid format"
) extends StaticAnnotation

final case class EmailConstraint(message: String = "Must be a valid email address") extends StaticAnnotation
