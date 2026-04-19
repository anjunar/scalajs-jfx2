package jfx.router

import jfx.core.component.Component
import scala.scalajs.js

case class RouteContext(
  path: String,
  url: String,
  fullPath: String,
  pathParams: Map[String, String],
  queryParams: Map[String, String],
  state: RouterState,
  routeMatch: RouteMatch
)

case class RouteMatch(
  route: Route,
  fullPath: String,
  params: Map[String, String]
)

case class RouterState(
  path: String,
  matches: List[RouteMatch],
  queryParams: Map[String, String],
  search: String
) {
  def url: String = s"$path$search"
  def currentMatchOption: Option[RouteMatch] = matches.lastOption
}

case class Route(
  path: String,
  factory: RouteContext => Unit,
  children: Seq[Route] = Nil
)

object Route {
  def route(path: String)(factory: RouteContext ?=> Unit): Route = {
    Route(path, ctx => factory(using ctx))
  }
}

object RouteMatcher {
  def resolve(routes: Seq[Route], path: String): List[RouteMatch] = {
    val normalized = normalize(path)
    
    def tryMatch(p: String): Option[List[RouteMatch]] = {
      routes.collectFirst {
        case r if normalize(r.path) == p => List(RouteMatch(r, normalized, Map.empty))
      }
    }

    // 1. Try direct match
    tryMatch(normalized).orElse {
      // 2. Try matching without base paths
      val withoutBase = normalized
        .stripPrefix("/scalajs-jfx2")
        .stripPrefix("/scalajs-jfx")
      
      val p = if (withoutBase.isEmpty) "/" else withoutBase
      tryMatch(p)
    }.getOrElse(Nil)
  }

  private def normalize(path: String): String = {
    if (path == null || path.isEmpty || path == "/") "/"
    else {
      val p = path.takeWhile(_ != '?').takeWhile(_ != '#')
      val s = if (p.endsWith("/")) p.dropRight(1) else p
      if (s.isEmpty) "/" else s
    }
  }
}
