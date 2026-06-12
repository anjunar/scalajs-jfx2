package app

import jfx.router.{Language, LocalizedRoute}
import org.scalajs.dom

object DemoRoutes {
  private val publishedBasePath = "/scalajs-jfx2"

  val homePath: String = "/"
  val buttonPath: String = "/button"
  val inputPath: String = "/input"
  val comboBoxPath: String = "/combo-box"
  val carouselPath: String = "/carousel"
  val tableViewPath: String = "/table-view"
  val dataGridPath: String = "/data-grid"
  val virtualListPath: String = "/virtual-list"
  val layoutPath: String = "/layout"
  val windowPath: String = "/window"
  val domainPath: String = "/domain"
  val imagePath: String = "/image"
  val imageCropperPath: String = "/image-cropper"
  val editorPath: String = "/editor"
  val hydrationReproPath: String = "/hydration-repro"
  val memoryLeakTestPath: String = "/memory-leak-test"

  def localizedPath(path: String, language: Language): String =
    LocalizedRoute.path(language, path)

  def languageFromUrl(url: String): Option[Language] =
    routeSegments(pathOnly(url)).headOption.flatMap(Language.fromCode)

  def currentLanguage: Language =
    languageFromUrl(currentUrl()).getOrElse(Language.default)

  def switchLanguage(): Unit = {
    val nextLanguage = currentLanguage match {
      case Language.German => Language.English
      case _ => Language.German
    }

    val target = rewriteLanguage(currentUrl(), nextLanguage)
    dom.window.history.pushState(null, "", target)
    dom.window.dispatchEvent(new dom.Event("popstate"))
  }

  def rewriteLanguage(url: String, language: Language): String = {
    val (path, suffix) = splitUrl(url)
    val (basePath, routePath) = splitBasePath(path)
    s"$basePath${localizedPath(stripLanguagePrefix(routePath), language)}$suffix"
  }

  private def currentUrl(): String =
    s"${dom.window.location.pathname}${dom.window.location.search}"

  private def pathOnly(url: String): String =
    splitUrl(url)._1

  private def splitUrl(url: String): (String, String) = {
    val raw = Option(url).getOrElse("")
    val queryIndex = raw.indexOf('?')
    val hashIndex = raw.indexOf('#')
    val splitIndex =
      Seq(queryIndex, hashIndex).filter(_ >= 0).sorted.headOption.getOrElse(raw.length)

    raw.take(splitIndex) -> raw.drop(splitIndex)
  }

  private def stripLanguagePrefix(path: String): String = {
    val currentSegments = routeSegments(path)
    val strippedSegments =
      currentSegments.headOption.flatMap(Language.fromCode) match {
        case Some(_) => currentSegments.drop(1)
        case None => currentSegments
      }

    if (strippedSegments.isEmpty) "/"
    else s"/${strippedSegments.mkString("/")}"
  }

  private def segments(path: String): Vector[String] =
    Option(path)
      .getOrElse("")
      .split("/")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .toVector

  private def routeSegments(path: String): Vector[String] =
    segments(splitBasePath(path)._2)

  private def splitBasePath(path: String): (String, String) = {
    val normalizedPath =
      Option(path).filter(_.nonEmpty).getOrElse("/")

    if (normalizedPath == publishedBasePath) {
      publishedBasePath -> "/"
    } else if (normalizedPath.startsWith(s"$publishedBasePath/")) {
      publishedBasePath -> normalizedPath.drop(publishedBasePath.length)
    } else {
      "" -> normalizedPath
    }
  }
}
