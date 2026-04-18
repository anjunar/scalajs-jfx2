package jfx.dsl

import jfx.core.component.NodeComponent
import org.scalajs.dom

import scala.collection.mutable
import scala.compiletime.summonFrom

private[jfx] final case class ComponentContext(
  parent: Option[NodeComponent[? <: dom.Node]],
  attachOverride: Option[NodeComponent[? <: dom.Node] => Unit] = None
)

private[jfx] object ComponentContext {
  val root: ComponentContext = ComponentContext(None)
}

object DslRuntime {

  private val componentContextStack =
    mutable.ArrayBuffer(ComponentContext.root)
  private val scopeStack =
    mutable.ArrayBuffer.empty[Scope]

  inline def currentScope[A](block: Scope => A): A =
    summonFrom {
      case given Scope =>
        block(summon[Scope])
      case _ =>
        if (scopeStack.nonEmpty) block(scopeStack.last)
        else block(Scope.root())
    }

  def activeScopeOption(): Option[Scope] =
    scopeStack.lastOption

  private[jfx] def currentComponentContext(): ComponentContext =
    componentContextStack.last

  def withScope[A](scope: Scope)(block: => A): A = {
    scopeStack += scope
    try block
    finally scopeStack.remove(scopeStack.length - 1)
  }

  private[jfx] def attach(component: NodeComponent[? <: dom.Node], context: ComponentContext): Unit =
    context.attachOverride match {
      case Some(attachOverride) =>
        attachOverride(component)
      case None =>
        context.parent.foreach(_.attachChild(component))
    }

  private[jfx] def withComponentContext[A](context: ComponentContext)(block: => A): A = {
    componentContextStack += context
    try block
    finally componentContextStack.remove(componentContextStack.length - 1)
  }

}
