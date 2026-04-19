package jfx.hydration

import jfx.core.component.Component
import jfx.core.render.{HydrationRenderBackend, RenderBackend}
import jfx.dsl.DslRuntime
import org.scalajs.dom

object Hydration {
  /**
   * Main entry point for Hydration.
   * Takes a root component factory and a DOM element where SSR content is located.
   */
  def hydrate(rootElement: dom.Element)(factory: => Component): Component = {
    val backend = HydrationRenderBackend.root(rootElement)
    val cursor = backend.nextCursor(None)
    
    RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(cursor) {
        val root = factory
        // The tree is now bound to existing nodes
        root
      }
    }
  }
}
