package jfx.statement

import jfx.core.component.{NativeComponent, NodeComponent}
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom

import scala.collection.mutable

private[statement] object StatementRuntime {

  def currentNativeParent(statementName: String): NativeComponent[? <: dom.Element] =
    DslRuntime.currentComponentContext().parent match {
      case Some(native: NativeComponent[?]) =>
        native

      case Some(other) =>
        throw IllegalStateException(
          s"$statementName can only be used inside a NativeComponent, but current parent is ${other.getClass.getSimpleName}"
        )

      case None =>
        throw IllegalStateException(s"$statementName can only be used inside a NativeComponent")
    }

  def collectChildren(
    owner: NativeComponent[? <: dom.Element],
    scope: Scope
  )(render: Scope ?=> Unit): Vector[NodeComponent[? <: dom.Node]] = {
    val buffer = mutable.ArrayBuffer.empty[NodeComponent[? <: dom.Node]]

    DslRuntime.withScope(scope) {
      DslRuntime.withComponentContext(
        ComponentContext(
          parent = Some(owner),
          attachOverride = Some(child => buffer += child)
        )
      ) {
        render(using scope)
      }
    }

    buffer.toVector
  }

  def collectOne(
    owner: NativeComponent[? <: dom.Element],
    scope: Scope
  )(render: Scope ?=> NodeComponent[? <: dom.Node]): Vector[NodeComponent[? <: dom.Node]] = {
    val buffer = mutable.ArrayBuffer.empty[NodeComponent[? <: dom.Node]]

    val returned =
      DslRuntime.withScope(scope) {
        DslRuntime.withComponentContext(
          ComponentContext(
            parent = Some(owner),
            attachOverride = Some(child => buffer += child)
          )
        ) {
          render(using scope)
        }
      }

    if (!buffer.exists(_ eq returned)) {
      buffer += returned
    }

    buffer.toVector
  }

}
