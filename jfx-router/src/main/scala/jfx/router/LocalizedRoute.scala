package jfx.router

object LocalizedRoute {
  val languageParam: String = "lang"

  def path(language: Language, path: String): String = {
    val suffix = normalize(path)
    if (suffix == "/") s"/${language.code}"
    else s"/${language.code}$suffix"
  }

  def routePath(path: String): String = {
    val suffix = normalize(path)
    if (suffix == "/") s"/:$languageParam"
    else s"/:$languageParam$suffix"
  }

  private def normalize(path: String): String = {
    val trimmed = Option(path).getOrElse("")
      .takeWhile(_ != '?')
      .takeWhile(_ != '#')

    val segments = trimmed.split("/").iterator.filter(_.nonEmpty).toVector

    if (segments.isEmpty) "/"
    else s"/${segments.mkString("/")}"
  }
}
