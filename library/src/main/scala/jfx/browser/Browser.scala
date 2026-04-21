package jfx.browser

import jfx.core.component.{ClientSideComponent, Component}
import jfx.core.render.{BrowserRenderBackend, DomHostElement, RenderBackend}
import jfx.dsl.DslRuntime
import org.scalajs.dom

object Browser {
  def mount(element: dom.HTMLElement)(factory: => Component): Component = {
    val backend = BrowserRenderBackend
    val cursor = backend.nextCursor(Some(new DomHostElement("div", element)))
    
    val root = RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(cursor) {
        factory
      }
    }

    ClientSideComponent.activateTree(root)
    root
  }
}
