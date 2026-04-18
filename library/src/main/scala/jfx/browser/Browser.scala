package jfx.browser

import jfx.core.component.NodeComponent
import jfx.core.render.{BrowserRenderBackend, RenderBackend}
import jfx.dsl.Scope
import org.scalajs.dom

object Browser {

  def mount[C <: NodeComponent[? <: dom.Node]](
    container: dom.Element
  )(factory: Scope ?=> C): C = {
    container.textContent = ""

    val component =
      RenderBackend.withBackend(BrowserRenderBackend) {
        Scope.scope {
          factory
        }
      }

    container.appendChild(component.element)
    component.onMount()
    component
  }

  def renderInto[C <: NodeComponent[? <: dom.Node]](
    container: dom.Element
  )(factory: Scope ?=> C): C =
    mount(container)(factory)

}
