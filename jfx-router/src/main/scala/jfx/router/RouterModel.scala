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
