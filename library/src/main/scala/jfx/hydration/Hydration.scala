package jfx.hydration

import jfx.core.component.NodeComponent
import jfx.core.render.{HydrationRenderBackend, RenderBackend}
import jfx.dsl.Scope
import org.scalajs.dom

object Hydration {

  def hydrateInto[C <: NodeComponent[? <: dom.Node]](
    container: dom.Element
  )(factory: Scope ?=> C): C = {
    val component =
      RenderBackend.withBackend(HydrationRenderBackend.into(container)) {
        Scope.scope {
          factory
        }
      }

    attachIfMissing(container, component)
    component.onMount()
    component
  }

  def hydrateRoot[C <: NodeComponent[? <: dom.Node]](
    root: dom.Element
  )(factory: Scope ?=> C): C = {
    val component =
      RenderBackend.withBackend(HydrationRenderBackend.root(root)) {
        Scope.scope {
          factory
        }
      }

    component.onMount()
    component
  }

  private def attachIfMissing(container: dom.Element, component: NodeComponent[? <: dom.Node]): Unit = {
    val node = component.element
    if (node.parentNode != container) {
      container.appendChild(node)
    }
  }

}
