package jfx.core.component

import jfx.core.render.RenderBackend
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom

abstract class ClientOnlyComponent[E <: dom.Element](
  tagName: String,
  val clientOnlyName: String,
  fallbackFactory: Scope ?=> NodeComponent[? <: dom.Node]
)(using Scope) extends NativeComponent[E](tagName) {

  private var clientMounted = false

  setAttribute("data-jfx-client-only", clientOnlyName)

  if (RenderBackend.current.isServer) {
    setAttribute("data-jfx-client-only-state", "fallback")
    renderFallback(fallbackFactory)
  }

  protected def mountClient(element: E): Unit

  override protected def afterMount(): Unit =
    if (!RenderBackend.current.isServer && !clientMounted) {
      clientMounted = true

      clearChildren()
      hostElement.clearChildren()
      removeAttribute("data-jfx-client-only-state")
      setAttribute("data-jfx-client-only-mounted", "true")
      mountClient(element)
    }

  private def renderFallback(
    factory: Scope ?=> NodeComponent[? <: dom.Node]
  ): Unit =
    DslRuntime.currentScope { currentScope =>
      DslRuntime.withComponentContext(ComponentContext(Some(this))) {
        given Scope = currentScope
        ensureAttached(factory)
      }
    }

  protected final def renderClientComponent(
    factory: Scope ?=> NodeComponent[? <: dom.Node]
  ): NodeComponent[? <: dom.Node] =
    DslRuntime.currentScope { currentScope =>
      DslRuntime.withComponentContext(ComponentContext(Some(this))) {
        given Scope = currentScope
        val child = factory
        ensureAttached(child)
        child
      }
    }

  private def ensureAttached(child: NodeComponent[? <: dom.Node]): Unit =
    if (child.parent.isEmpty) {
      attachChild(child)
    }

}
