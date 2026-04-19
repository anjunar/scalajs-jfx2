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
    
    // 1. Try exact match
    routes.collectFirst {
      case r if normalize(r.path) == normalized => List(RouteMatch(r, normalized, Map.empty))
    }.orElse {
      // 2. Try matching without base path if it doesn't match directly
      // This is a bit hacky but works for many dev setups.
      // Better would be to know the base path explicitly.
      val withoutBase = if (normalized.startsWith("/scalajs-jfx2")) normalized.stripPrefix("/scalajs-jfx2") else normalized
      val normalizedWithoutBase = if (withoutBase.isEmpty) "/" else withoutBase
      
      routes.collectFirst {
        case r if normalize(r.path) == normalizedWithoutBase => List(RouteMatch(r, normalized, Map.empty))
      }
    }.getOrElse(Nil)
  }

  private def normalize(path: String): String = {
    val p = path.takeWhile(_ != '?').takeWhile(_ != '#')
    if (p == "/") p
    else p.stripSuffix("/")
  }
}
