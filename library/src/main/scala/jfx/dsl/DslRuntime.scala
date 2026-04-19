package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.Cursor
import org.scalajs.dom
import scala.collection.mutable

/**
 * Context for DSL composition.
 */
final case class ComponentContext(
  parent: Option[jfx.core.component.Component]
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

  /**
   * Main entry point for DSL: Creates a component and binds it immediately.
   */
  def build[C <: Component](factory: => C)(init: C ?=> Unit): C = {
    val component = factory
    
    // ATTACHMENT Logic: Always through Cursor
    component.bind(currentCursor)
    
    // COMPOSITION Logic: Set as parent for children
    // The children created in `init` must be attached to this component's host
    val sub = currentCursor.subCursor(component.host)
    withCursor(sub) {
      withContext(ComponentContext(Some(component))) {
        given c: C = component
        init
      }
    }
    
    component
  }
}
