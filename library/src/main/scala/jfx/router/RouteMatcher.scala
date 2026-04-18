package jfx.router

import scala.scalajs.js

object RouteMatcher {

  def normalize(path: String): String = {
    if (path == null || path.trim.isEmpty) return "/"

    var normalized = path.trim
    if (!normalized.startsWith("/")) normalized = "/" + normalized

    if (normalized.length > 1 && normalized.endsWith("/")) normalized = normalized.dropRight(1)

    normalized
  }

  def resolveRoutes(routes: js.Array[Route], path: String): RouterState =
    resolveRoutes(routes.toList, path)

  def resolveRoutes(routes: List[Route], path: String): RouterState = {
    val target = normalize(path)

    def dfs(parentFull: String, route: Route): Option[List[RouteMatch]] = {
      val routeFull = normalize(join(parentFull, route.path))

      if (!prefixMatches(routeFull, target)) None
      else {
        val exactMatch = matchPattern(routeFull, target)

        if (exactMatch != null) {
          Some(List(RouteMatch(route, routeFull, exactMatch)))
        } else {
          route.children.iterator
            .map(child => dfs(routeFull, child))
            .collectFirst {
              case Some(childChain) =>
                RouteMatch(route, routeFull, js.Map.empty[String, String]) :: childChain
            }
        }
      }
    }

    val chains =
      routes.iterator
        .flatMap(route => dfs("/", route).iterator)
        .toList

    RouterState(
      path = target,
      matches = if (chains.isEmpty) Nil else chains.maxBy(_.size)
    )
  }

  private def join(parent: String, child: String): String = {
    val normalizedParent = normalize(parent)
    val normalizedChild = Option(child).getOrElse("").trim

    if (normalizedChild.isEmpty || normalizedChild == "/") return normalizedParent

    val effectiveChild =
      if (normalizedParent != "/" && normalizedChild.startsWith("/")) normalizedChild.drop(1)
      else normalizedChild

    normalize {
      if (normalizedParent == "/") ("/" + effectiveChild).replace("//", "/")
      else s"$normalizedParent/$effectiveChild"
    }
  }

  private def splitSegments(path: String): List[String] = {
    val normalized = normalize(path).stripPrefix("/").stripSuffix("/")
    if (normalized.isBlank) Nil else normalized.split("/").toList
  }

  private def matchPattern(pattern: String, actual: String): js.Map[String, String] | Null = {
    val patternSegments = splitSegments(pattern)
    val actualSegments = splitSegments(actual)

    val wildcardIndex = patternSegments.indexOf("*")
    if (wildcardIndex >= 0) {
      matchWildcard(patternSegments, actualSegments, wildcardIndex)
    } else if (patternSegments.size != actualSegments.size) {
      null
    } else {
      matchSegments(patternSegments, actualSegments)
    }
  }

  private def matchWildcard(
    patternSegments: List[String],
    actualSegments: List[String],
    wildcardIndex: Int
  ): js.Map[String, String] | Null = {
    val prefix = patternSegments.take(wildcardIndex)
    if (actualSegments.size < prefix.size) return null

    matchSegments(prefix, actualSegments.take(prefix.size)) match {
      case null =>
        null
      case params =>
        params += "*" -> actualSegments.drop(prefix.size).mkString("/")
        params
    }
  }

  private def matchSegments(
    patternSegments: List[String],
    actualSegments: List[String]
  ): js.Map[String, String] | Null = {
    val params = js.Map.empty[String, String]
    var matched = true
    var index = 0

    while (index < patternSegments.length && matched) {
      val patternSegment = patternSegments(index)
      val actualSegment = actualSegments(index)

      if (patternSegment.startsWith(":")) {
        params += patternSegment.drop(1) -> actualSegment
      } else if (patternSegment != actualSegment) {
        matched = false
      }

      index += 1
    }

    if (matched) params else null
  }

  private def prefixMatches(routeFull: String, target: String): Boolean = {
    val routeSegments = splitSegments(routeFull)
    val targetSegments = splitSegments(target)

    if (routeFull == "/") true
    else if (targetSegments.size < routeSegments.size) false
    else {
      var matched = true
      var index = 0

      while (index < routeSegments.length && matched) {
        val routeSegment = routeSegments(index)
        val targetSegment = targetSegments(index)

        if (routeSegment == "*") {
          index = routeSegments.length
        } else if (routeSegment.startsWith(":")) {
          index += 1
        } else if (routeSegment != targetSegment) {
          matched = false
        } else {
          index += 1
        }
      }

      matched
    }
  }

}
