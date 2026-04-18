package jfx.ssr

import jfx.core.component.NodeComponent
import jfx.core.render.{RenderBackend, SsrRenderBackend}
import jfx.dsl.Scope
import org.scalajs.dom

object Ssr {

  final case class Request(
    path: String = "/",
    method: String = "GET",
    headers: Map[String, String] = Map.empty,
    attributes: Map[String, Any] = Map.empty
  )

  final case class Result(
    html: String
  )

  def renderToString[C <: NodeComponent[? <: dom.Node]](
    factory: Scope ?=> C
  ): String =
    render(factory).html

  def renderToStringFor[C <: NodeComponent[? <: dom.Node]](
    request: Request
  )(factory: Scope ?=> C): String =
    renderFor(request)(factory).html

  def render[C <: NodeComponent[? <: dom.Node]](
    factory: Scope ?=> C
  ): Result =
    renderFor(Request())(factory)

  def renderFor[C <: NodeComponent[? <: dom.Node]](
    request: Request
  )(factory: Scope ?=> C): Result = {
    val backend = new SsrRenderBackend()

    val component =
      RenderBackend.withBackend(backend) {
        Scope.scope {
          Scope.singleton[Request] {
            request
          }

          factory
        }
      }

    try Result(component.renderHtml)
    finally component.dispose()
  }

}
