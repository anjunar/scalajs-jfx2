package jfx.router

enum Language(val code: String) {
  case German extends Language("de")
  case English extends Language("en")
  case French extends Language("fr")
}

object Language {
  val default: Language = Language.German
  val all: Seq[Language] = Seq(Language.German, Language.English, Language.French)

  def fromCode(code: String): Option[Language] =
    all.find(_.code == code)

  def isSupported(code: String): Boolean =
    fromCode(code).nonEmpty
}
