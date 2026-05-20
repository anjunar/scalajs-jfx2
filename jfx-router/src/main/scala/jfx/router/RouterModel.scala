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

    resolvePath(routes, normalized).orElse {
      val withoutBase = normalized
        .stripPrefix("/scalajs-jfx2")
        .stripPrefix("/scalajs-jfx")

      val p = if (withoutBase.isEmpty) "/" else withoutBase
      resolvePath(routes, p)
    }.getOrElse(Nil)
  }

  private def resolvePath(routes: Seq[Route], path: String): Option[List[RouteMatch]] =
    routes.iterator
      .map(route => resolveRoute("/", route, path))
      .collectFirst {
        case Some(matches) => matches
      }

  private def resolveRoute(parentPath: String, route: Route, targetPath: String): Option[List[RouteMatch]] = {
    val routePath = join(parentPath, route.path)

    if (!prefixMatches(routePath, targetPath)) {
      None
    } else {
      matches(routePath, targetPath) match {
        case Some(params) =>
          Some(List(RouteMatch(route, routePath, params)))

        case None =>
          route.children.iterator
            .map(child => resolveRoute(routePath, child, targetPath))
            .collectFirst {
              case Some(childMatches) =>
                RouteMatch(route, routePath, Map.empty) :: childMatches
            }
      }
    }
  }

  private def matches(routePath: String, requestPath: String): Option[Map[String, String]] = {
    val routeSegments = segments(normalize(routePath))
    val requestSegments = segments(normalize(requestPath))

    val wildcardIndex = routeSegments.indexOf("*")
    if (wildcardIndex >= 0) {
      matchSegments(routeSegments.take(wildcardIndex), requestSegments.take(wildcardIndex))
        .filter(_ => requestSegments.length >= wildcardIndex)
        .map(_ + ("*" -> requestSegments.drop(wildcardIndex).map(decode).mkString("/")))
    } else if (routeSegments.length != requestSegments.length) {
      None
    } else {
      matchSegments(routeSegments, requestSegments)
    }
  }

  private def matchSegments(routeSegments: Vector[String], requestSegments: Vector[String]): Option[Map[String, String]] =
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

  private def prefixMatches(routePath: String, requestPath: String): Boolean = {
    val routeSegments = segments(normalize(routePath))
    val requestSegments = segments(normalize(requestPath))

    routePath == "/" ||
      routeSegments.indexOf("*") >= 0 ||
      (requestSegments.length >= routeSegments.length &&
        routeSegments.zip(requestSegments).forall {
          case (routeSegment, requestSegment) =>
            routeSegment.startsWith(":") || routeSegment == requestSegment
        })
  }

  private def join(parentPath: String, childPath: String): String = {
    val parent = normalize(parentPath)
    val child = Option(childPath).getOrElse("").trim

    if (child.isEmpty || child == "/") {
      parent
    } else if (parent == "/") {
      normalize(s"/${child.stripPrefix("/")}")
    } else {
      normalize(s"$parent/${child.stripPrefix("/")}")
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
