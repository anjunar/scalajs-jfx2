package jfx.form.validators

import scala.util.matching.Regex

final case class NotNullValidator[V](message: String = "Darf nicht null sein") extends Validator[V] {
  override def validate(value: V): Option[String] =
    if (value == null) Some(message)
    else None
}

final case class NotEmptyValidator[V](message: String = "Darf nicht leer sein") extends Validator[V] {
  override def validate(value: V): Option[String] =
    if (value == null) Some(message)
    else ValidatorSupport.sizeOf(value) match {
      case Some(size) if size == 0 => Some(message)
      case _ => None
    }
}

final case class NotBlankValidator(message: String = "Darf nicht leer oder nur Leerzeichen sein") extends Validator[String] {
  override def validate(value: String): Option[String] =
    if (value == null || value.trim.isEmpty) Some(message)
    else None
}

final case class SizeValidator[V](
  min: Int = 0,
  max: Int = Int.MaxValue,
  message: String | Null = null
) extends Validator[V] {

  require(min >= 0, s"min must be >= 0 but was $min")
  require(max >= min, s"max must be >= min but was $max < $min")

  override def validate(value: V): Option[String] = {
    if (value == null) {
      None
    } else {
      ValidatorSupport.sizeOf(value) match {
        case Some(size) if size < min || size > max =>
          Some(resolveMessage())
        case _ =>
          None
      }
    }
  }

  private def resolveMessage(): String =
    if (message != null && message.trim.nonEmpty) message
    else if (min == max) s"Muss genau $min Zeichen/Eintraege haben"
    else if (max == Int.MaxValue) s"Muss mindestens $min Zeichen/Eintraege haben"
    else if (min == 0) s"Darf hoechstens $max Zeichen/Eintraege haben"
    else s"Muss zwischen $min und $max Zeichen/Eintraege haben"
}

final case class PatternValidator(
  regex: Regex,
  message: String = "Hat ein ungültiges Format"
) extends Validator[String] {
  override def validate(value: String): Option[String] =
    if (value == null) None
    else if (regex.matches(value)) None
    else Some(message)
}

final case class EmailValidator(
  message: String = "Muss eine gültige E-Mail-Adresse sein",
  regex: Regex = ValidatorSupport.defaultEmailRegex
) extends Validator[String] {
  override def validate(value: String): Option[String] =
    if (value == null || value.trim.isEmpty) None
    else if (regex.matches(value.trim)) None
    else Some(message)
}

private object ValidatorSupport {
  val defaultEmailRegex: Regex =
    "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$".r

  def sizeOf(value: Any): Option[Int] =
    value match {
      case text: String => Some(text.length)
      case array: scala.scalajs.js.Array[?] => Some(array.length)
      case array: Array[?] => Some(array.length)
      case iterable: Iterable[?] => Some(iterable.size)
      case collection: IterableOnce[?] => Some(collection.iterator.length)
      case _ => None
    }
}
