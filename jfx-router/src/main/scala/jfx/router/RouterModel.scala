package jfx.router

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
  load: RouteContext => Route.Load,
  children: Seq[Route] = Nil
)

object Route {
  type LoaderResult = Load | js.Promise[Factory]

  final class Factory(render: RouteContext ?=> Unit) {
    def apply(context: RouteContext): Unit =
      render(using context)
  }

  final case class Load(promise: js.Promise[Factory], immediate: Option[Factory] = None)

  def asyncRoute(path: String)(factory: RouteContext ?=> LoaderResult): Route =
    Route(
      path = path,
      load = ctx => normalize(factory(using ctx))
    )

  def page(render: RouteContext ?=> Unit): Load = {
    val routeFactory: Factory = new Factory(render)
    Load(js.Promise.resolve(routeFactory), immediate = Some(routeFactory))
  }

  def factory(render: RouteContext ?=> Unit): Factory =
    new Factory(render)

  def load(promise: js.Promise[Factory]): Load =
    Load(promise)

  private def normalize(result: LoaderResult): Load =
    result match {
      case routeLoad: Load => routeLoad
      case promise         => Load(promise.asInstanceOf[js.Promise[Factory]])
    }
}

object RouteMatcher {
  def resolve(routes: Seq[Route], path: String): List[RouteMatch] = {
    val normalized = normalize(path)
    
    def tryMatch(p: String): Option[List[RouteMatch]] =
      routes
        .iterator
        .map(route => route -> matches(route.path, p))
        .collectFirst {
          case (route, Some(params)) => List(RouteMatch(route, normalized, params))
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

  private def matches(routePath: String, requestPath: String): Option[Map[String, String]] = {
    val routeSegments = segments(normalize(routePath))
    val requestSegments = segments(normalize(requestPath))

    if (routeSegments.length != requestSegments.length) {
      None
    } else {
      routeSegments.zip(requestSegments).foldLeft(Option(Map.empty[String, String])) {
        case (None, _) => None
        case (Some(params), (routeSegment, requestSegment)) =>
          if (routeSegment.startsWith(":") && routeSegment.length > 1) {
            Some(params.updated(routeSegment.drop(1), decode(requestSegment)))
          } else if (routeSegment == requestSegment) {
            Some(params)
          } else {
            None
          }
      }
    }
  }

  private def segments(path: String): Vector[String] =
    if (path == "/") Vector.empty
    else path.stripPrefix("/").split("/").iterator.filter(_.nonEmpty).toVector

  private def decode(value: String): String =
    try js.URIUtils.decodeURIComponent(value)
    catch {
      case _: Throwable => value
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
