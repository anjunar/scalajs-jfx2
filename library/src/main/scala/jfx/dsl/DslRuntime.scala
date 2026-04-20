package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.{BrowserRenderBackend, Cursor, HydrationRenderBackend, RenderBackend}
import jfx.di.{ServiceRegistry, HierarchicalRegistry}
import org.scalajs.dom
import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Context for DSL composition.
 */
final case class ComponentContext(
  parent: Option[jfx.core.component.Component],
  insertionIndex: Option[Int] = None,
  registry: ServiceRegistry = new HierarchicalRegistry(None)
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

  def service[T](using manifest: ClassTag[T]): T = {
    currentContext.registry.get[T]
  }

  def provide[T](service: T)(init: => Unit)(using manifest: ClassTag[T]): Unit = {
    val currentCtx = currentContext
    val newRegistry = new HierarchicalRegistry(Some(currentCtx.registry))
    newRegistry.register(service)
    
    withContext(currentCtx.copy(registry = newRegistry)) {
      init
    }
  }

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
      withContext(ComponentContext(Some(component), registry = currentContext.registry)) {
        block
      }
    }
  }

  /**
   * Helper for components that render branches dynamically (Router, Condition, ForEach).
   * Ensures the correct backend and cursor context are maintained.
   */
  def updateBranch[C <: Component](component: C, index: Option[Int] = None)(block: => Unit): Unit = {
    val currentBackend = RenderBackend.current
    
    // Logic: If we are in the browser and currently in a hydration pass, 
    // we continue with it. But for ALL subsequent reactive updates, 
    // we MUST use the BrowserRenderBackend to actually modify the DOM.
    val effectiveBackend = currentBackend match {
      case _: HydrationRenderBackend if component.bindCursor != null && !component.bindCursor.isInstanceOf[jfx.core.render.HydrationCursor] =>
        // We were bound with a normal cursor but someone started a global hydration? 
        // This shouldn't happen, but safety first.
        BrowserRenderBackend
      case h: HydrationRenderBackend => 
        // We are currently in the initial hydration pass.
        h
      case other => 
        // Normal live mode or SSR.
        other
    }

    RenderBackend.withBackend(effectiveBackend) {
      val cursor = if (component.parent.isEmpty) {
        component.bindCursor
      } else {
        val baseOffset = component.calculateDomOffset
        val offset = index.map { i =>
           baseOffset + component.children.take(i).map(_.domNodeCount).sum
        }.getOrElse(baseOffset)
        
        RenderBackend.current.insertionCursor(component.host, offset)
      }

      withCursor(cursor) {
        withContext(ComponentContext(Some(component), index, currentContext.registry)) {
          block
        }
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
         if (!RenderBackend.current.isInstanceOf[HydrationRenderBackend]) {
           p.syncChildAddition(component)
         }
       }
    }
    
    // 4. COMPOSITION Logic: Set as parent for children
    if (component.isVirtual) {
      withCursor(cursor) {
        withContext(ComponentContext(Some(component), registry = currentContext.registry)) {
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
            withContext(ComponentContext(Some(component), registry = currentContext.registry)) {
              given c: C = component
              component.compose()
              init
            }
          }
        case _ =>
          withContext(ComponentContext(Some(component), registry = currentContext.registry)) {
            given c: C = component
            component.compose()
            init
          }
      }
    }
    
    component
  }
}
