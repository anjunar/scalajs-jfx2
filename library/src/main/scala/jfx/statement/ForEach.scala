package jfx.statement

import jfx.core.component.{ChildSlot, NativeComponent, NodeComponent}
import jfx.core.state.{CompositeDisposable, Disposable, ListProperty}
import jfx.dsl.{DslRuntime, Scope}
import org.scalajs.dom

final class ForEach[T] private[statement] (
  val items: ListProperty[T],
  private val owner: NativeComponent[? <: dom.Element],
  private val slot: ChildSlot,
  private val renderItem: (T, Int) => NodeComponent[? <: dom.Node],
  private val renderScope: Scope
) extends Disposable {

  private val disposable = new CompositeDisposable()
  private var disposed = false

  disposable.add(
    items.observeChanges { _ =>
      rebuild()
    }
  )

  def rebuild(): Unit =
    if (!disposed) {
      val children =
        items.iterator.zipWithIndex.toVector.flatMap { case (item, index) =>
          StatementRuntime.collectOne(owner, renderScope) {
            renderItem(item, index)
          }
        }

      slot.replace(children)
    }

  override def dispose(): Unit =
    if (!disposed) {
      disposed = true
      disposable.dispose()
      slot.dispose()
    }

}

object ForEach {

  def apply[T](items: ListProperty[T])(renderItem: (T, Int) => NodeComponent[? <: dom.Node]): ForEach[T] =
    forEach(items)(renderItem)

  def forEach[T](items: ListProperty[T])(renderItem: (T, Int) => NodeComponent[? <: dom.Node]): ForEach[T] =
    DslRuntime.currentScope { currentScope =>
      val owner = StatementRuntime.currentNativeParent("forEach")
      val component = new ForEach(items, owner, owner.reserveChildSlot(), renderItem, currentScope)

      owner.addDisposable(component)
      component.rebuild()
      component
    }

  def forEach[T](items: ListProperty[T])(renderItem: T => NodeComponent[? <: dom.Node]): ForEach[T] =
    forEach(items) { (item, _) => renderItem(item) }

}
