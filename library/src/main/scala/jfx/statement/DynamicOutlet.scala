package jfx.statement

import jfx.core.component.{ChildSlot, NativeComponent, NodeComponent}
import jfx.core.state.{CompositeDisposable, Disposable, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import org.scalajs.dom

final class DynamicOutlet private[statement] (
  val content: ReadOnlyProperty[? <: NodeComponent[? <: dom.Node] | Null],
  private val owner: NativeComponent[? <: dom.Element],
  private val slot: ChildSlot
) extends Disposable {

  private val disposable = new CompositeDisposable()
  private var disposed = false

  disposable.add(
    content.observeWithoutInitial { _ =>
      reconcile()
    }
  )

  def reconcile(): Unit =
    if (!disposed) {
      val child = content.get.asInstanceOf[NodeComponent[? <: dom.Node] | Null]
      slot.replace(Option(child).toVector)
    }

  override def dispose(): Unit =
    if (!disposed) {
      disposed = true
      disposable.dispose()
      slot.dispose()
    }

}

object DynamicOutlet {

  def apply(content: ReadOnlyProperty[? <: NodeComponent[? <: dom.Node] | Null]): DynamicOutlet =
    outlet(content)

  def outlet(content: ReadOnlyProperty[? <: NodeComponent[? <: dom.Node] | Null]): DynamicOutlet =
    DslRuntime.currentScope { _ =>
      val owner = StatementRuntime.currentNativeParent("dynamicOutlet")
      val component = new DynamicOutlet(content, owner, owner.reserveChildSlot())

      owner.addDisposable(component)
      component.reconcile()
      component
    }

  def dynamicOutlet(content: ReadOnlyProperty[? <: NodeComponent[? <: dom.Node] | Null]): DynamicOutlet =
    outlet(content)

  def outletContent(using outlet: DynamicOutlet): ReadOnlyProperty[? <: NodeComponent[? <: dom.Node] | Null] =
    outlet.content

}
