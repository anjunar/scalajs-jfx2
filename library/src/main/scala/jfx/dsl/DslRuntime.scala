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
    val cursor = currentCursor
    val context = currentContext
    
    component.setParent(context.parent)

    // Capture position BEFORE claim
    val insertPos = cursor.position

    // 1. Physical bind (obtains _host)
    component.bind(cursor)

    // 2. Logical link via parent
    context.parent.foreach { p =>
      insertPos match {
        case Some(idx) => p.insertChild(idx, component)
        case None => p.addChild(component)
      }
    }
    
    // COMPOSITION Logic: Set as parent for children
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
    
    component
  }
}
