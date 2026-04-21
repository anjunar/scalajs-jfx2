package jfx.hydration

import jfx.core.component.Component
import jfx.core.render.{AsyncRenderPending, BrowserRenderBackend, HydrationRenderBackend, RenderBackend}
import jfx.dsl.DslRuntime
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.concurrent.ExecutionContext.Implicits.global

object Hydration {
  /**
   * Main entry point for Hydration.
   * Takes a root component factory and a DOM element where SSR content is located.
   * Returns a promise that resolves when the hydration is complete.
   */
  def hydrate(rootElement: dom.Element)(factory: => Component): js.Promise[Component] = {
    // 1. Phase: Dry-Run
    // We build the tree using the standard BrowserRenderBackend.
    // This triggers all asynchronous loading (like Router route loading).
    // We do this "silently" on a detached DOM node.
    val dryBackend = BrowserRenderBackend 
    
    val root = RenderBackend.withBackend(dryBackend) {
      val cursor = dryBackend.nextCursor(None)
      DslRuntime.withCursor(cursor) {
        factory
      }
    }

    // 2. Phase: Await Stability
    // We wait until all AsyncRenderPending components in the tree are ready.
    AsyncRenderPending.awaitPending(root).map { _ =>
      // 3. Phase: Rehydration
      // Now that the tree is in its final stable state, we bind it to the real DOM.
      val hydrationBackend = HydrationRenderBackend.root(rootElement)
      val cursor = hydrationBackend.nextCursor(None)
      
      RenderBackend.withBackend(hydrationBackend) {
        DslRuntime.rehydrate(root, cursor)
      }
      
      root
    }.toJSPromise
  }
}
