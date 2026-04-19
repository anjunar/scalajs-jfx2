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
    // Simple implementation for now
    routes.collectFirst {
      case r if r.path == path => List(RouteMatch(r, path, Map.empty))
    }.getOrElse(Nil)
  }
}
