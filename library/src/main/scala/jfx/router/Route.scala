package jfx.router

import jfx.core.component.NodeComponent
import jfx.dsl.Scope
import org.scalajs.dom

import scala.scalajs.js

final case class RouteContext(
  path: String,
  url: String,
  fullPath: String,
  pathParams: js.Map[String, String],
  queryParams: js.Map[String, String],
  state: RouterState,
  routeMatch: RouteMatch
)

object RouteContext {
  def routeContext(using context: RouteContext): RouteContext =
    context
}

final case class Route(
  path: String,
  factory: Route.Factory,
  children: js.Array[Route] = js.Array()
)

object Route {

  type Component = NodeComponent[? <: dom.Node] | Null
  type Factory = (RouteContext, Scope) => Component
  type ScopedFactory = RouteContext ?=> Scope ?=> Component

  def scoped(path: String)(factory: ScopedFactory): Route =
    scoped(path = path, factory = factory, children = js.Array())

  def scoped(path: String, children: js.Array[Route])(factory: ScopedFactory): Route =
    scoped(path = path, factory = factory, children = children)

  def scoped(
    path: String,
    factory: ScopedFactory,
    children: js.Array[Route] = js.Array()
  ): Route =
    Route(
      path,
      (context, scope) => factory(using context)(using scope),
      children
    )

}
