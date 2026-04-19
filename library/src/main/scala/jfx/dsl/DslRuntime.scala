package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.Cursor
import org.scalajs.dom
import scala.collection.mutable

/**
 * Context for DSL composition.
 */
final case class ComponentContext(
  parent: Option[jfx.core.component.Component],
  insertionIndex: Option[Int] = None
)

object ComponentContext {
  val root = ComponentContext(None)
}

/**
 * Placeholder for Scope.
 */
trait Scope
object Scope {
  val root = new Scope {}
}

object DslRuntime {
  private val cursorStack = mutable.ArrayBuffer.empty[Cursor]
  private val contextStack = mutable.ArrayBuffer(ComponentContext.root)

  def currentCursor: Cursor = cursorStack.lastOption.getOrElse(
    throw IllegalStateException("No active DSL cursor found")
  )

  def currentContext: ComponentContext = contextStack.last

  def withCursor[A](cursor: Cursor)(block: => A): A = {
    cursorStack += cursor
    try block finally cursorStack.remove(cursorStack.length - 1)
  }

  def withContext[A](context: ComponentContext)(block: => A): A = {
    contextStack += context
    try block finally contextStack.remove(contextStack.length - 1)
  }

  def attach[C <: Component](component: C): C = {
    component.bind(currentCursor)
    component
  }

  def withComponentScope[A](component: Component)(block: => A): A = {
    val cursor = jfx.core.render.RenderBackend.current.nextCursor(Some(component.host))
    withCursor(cursor) {
      withContext(ComponentContext(Some(component))) {
        block
      }
    }
  }

  /**
   * Main entry point for DSL: Creates a component and binds it immediately.
   */
  def build[C <: Component](factory: => C)(init: C ?=> Unit): C = {
    val component = factory
    val cursor = currentCursor
    val context = currentContext
    
    // 1. Establish logical parent immediately
    component.setParent(context.parent)
    context.parent.foreach { p =>
      context.insertionIndex match {
        case Some(idx) => p._children.insert(idx, component)
        case None => p._children += component
      }
    }

    // 2. Physical bind (obtains _host)
    component.bind(cursor)

    // 3. Physical sync with parent host (only for non-virtual children)
    if (!component.isVirtual) {
       context.parent.foreach { p =>
         // Only sync if we are not in hydration mode (or if it's a dynamic addition)
         if (!jfx.core.render.RenderBackend.current.isInstanceOf[jfx.core.render.HydrationRenderBackend]) {
           p.syncChildAddition(component)
         }
       }
    }
    
    // 4. COMPOSITION Logic: Set as parent for children
    if (component.isVirtual) {
      // For virtual components, we STAY in the same cursor context
      // but we need to ensure the cursor is available for compose()
      withCursor(cursor) {
        withContext(ComponentContext(Some(component))) {
          given c: C = component
          component.compose()
          init
        }
      }
    } else {
      component.hostNode match {
        case h: jfx.core.render.HostElement =>
          val sub = cursor.subCursor(h)
          withCursor(sub) {
            withContext(ComponentContext(Some(component))) {
              given c: C = component
              component.compose()
              init
            }
          }
        case _ =>
          withContext(ComponentContext(Some(component))) {
            given c: C = component
            component.compose()
            init
          }
      }
    }
    
    component
  }
}
