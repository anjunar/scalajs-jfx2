package jfx.core.component

import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom

object ClientOnly {

  def clientOnly(
    fallback: Scope ?=> NodeComponent[? <: dom.Node]
  )(
    client: Scope ?=> NodeComponent[? <: dom.Node]
  ): ClientOnlyBoundary =
    clientOnly("client-only")(fallback)(client)

  def clientOnly(
    name: String
  )(
    fallback: Scope ?=> NodeComponent[? <: dom.Node]
  )(
    client: Scope ?=> NodeComponent[? <: dom.Node]
  ): ClientOnlyBoundary =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new ClientOnlyBoundary(name, fallback, client)(using currentScope)
      DslRuntime.attach(component, currentContext)
      component
    }

}

final class ClientOnlyBoundary(
  name: String,
  fallbackFactory: Scope ?=> NodeComponent[? <: dom.Node],
  clientFactory: Scope ?=> NodeComponent[? <: dom.Node]
)(using Scope) extends ClientOnlyComponent[dom.HTMLDivElement]("div", name, fallbackFactory) {

  override protected def mountClient(element: dom.HTMLDivElement): Unit =
    renderClientComponent(clientFactory)

}
