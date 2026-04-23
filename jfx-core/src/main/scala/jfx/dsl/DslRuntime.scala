package jfx.dsl

import jfx.core.component.{ClientSideComponent, Component}
import jfx.core.render.{BrowserRenderBackend, Cursor, HostElement, HydrationCursor, HydrationRenderBackend, RenderBackend}
import jfx.di.{ServiceRegistry, HierarchicalRegistry}
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
  private var clientSideActivationSuspensions = 0

  def currentCursor: Cursor = cursorStack.lastOption.getOrElse(
    throw IllegalStateException(
      s"No active DSL cursor found. This usually happens when trying to build a component (context: ${currentContext.parent.map(_.tagName).getOrElse("root")}) outside of a build/compose block or in an asynchronous callback without providing a component scope."
    )
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

  def withClientSideActivationSuspended[A](block: => A): A = {
    clientSideActivationSuspensions += 1
    try block finally clientSideActivationSuspensions -= 1
  }

  def attach[C <: Component](component: C): C = {
    component.bind(currentCursor)
    component
  }

  def withComponentScope[A](component: Component)(block: => A): A = {
    val cursor = jfx.core.render.RenderBackend.current.nextCursor(Some(component.host))
    withCursor(cursor) {
      withContext(ComponentContext(Some(component), registry = component.registry)) {
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
      val cursor = if (component.parent.isEmpty || nearestPhysicalParent(component).isEmpty) {
        component.bindCursor
      } else {
        val baseOffset = component.calculateDomOffset
        val offset = index.map { i =>
           baseOffset + component.children.take(i).map(_.domNodeCount).sum
        }.getOrElse(baseOffset)
        
        RenderBackend.current.insertionCursor(component.host, offset)
      }

      withCursor(cursor) {
        withContext(ComponentContext(Some(component), index, component.registry)) {
          block
        }
      }
    }
  }

  private def nearestPhysicalParent(component: Component): Option[Component] =
    component.parent match {
      case Some(parent) if parent.isVirtual => nearestPhysicalParent(parent)
      case Some(parent)                    => Some(parent)
      case None                            => None
    }

  /**
   * Recursively binds an existing component tree to a new cursor.
   * Used for async hydration after the dry-run is complete.
   */
  def rehydrate(component: Component, cursor: Cursor): Unit = {
    component.bind(cursor)
    
    if (component.isVirtual) {
      rehydrateChildren(component, cursor)
    } else {
      component.hostNode match {
        case h: HostElement =>
          val childCursor = cursor.subCursor(h)
          rehydrateChildren(component, childCursor)
          pruneHydratedRemainder(childCursor)
        case _ =>
          // Non-element hosts (like text nodes) cannot have children in the DOM
      }
    }

    if (component.parent.isEmpty) {
      pruneHydratedRemainder(cursor)
    }

    component.afterRehydrate()
  }

  private def rehydrateChildren(component: Component, cursor: Cursor): Unit = {
    component.children.foreach(child => rehydrate(child, cursor))
  }

  private def pruneHydratedRemainder(cursor: Cursor): Unit = {
    cursor match {
      case h: HydrationCursor => h.pruneRemaining()
      case _                  =>
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
    component.setRegistry(context.registry)
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
        withContext(ComponentContext(Some(component), registry = context.registry)) {
          given c: C = component
          component.initialize()
          component.compose()
          init
          component.afterCompose()
        }
      }
    } else {
      component.hostNode match {
        case h: jfx.core.render.HostElement =>
          val sub = cursor.subCursor(h)
          withCursor(sub) {
            withContext(ComponentContext(Some(component), registry = context.registry)) {
              given c: C = component
              component.initialize()
              component.compose()
              init
              component.afterCompose()
            }
          }
        case _ =>
          withContext(ComponentContext(Some(component), registry = context.registry)) {
            given c: C = component
            component.initialize()
            component.compose()
            init
            component.afterCompose()
          }
      }
    }
    
    activateClientSideIfNeeded(component)
    component
  }

  private def activateClientSideIfNeeded(component: Component): Unit =
    component match {
      case clientSide: ClientSideComponent
          if clientSideActivationSuspensions == 0 &&
            !RenderBackend.current.isServer &&
            !RenderBackend.current.isInstanceOf[HydrationRenderBackend] =>
        clientSide.activateClientSide()
      case _ =>
    }
}
