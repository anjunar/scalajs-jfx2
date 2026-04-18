package jfx.statement

import jfx.core.component.{ChildSlot, NativeComponent, NodeComponent}
import jfx.core.state.{CompositeDisposable, Disposable, ReadOnlyProperty}
import jfx.dsl.{DslRuntime, Scope}
import org.scalajs.dom

final class ObserveRender[T] private[statement] (
  val source: ReadOnlyProperty[T],
  private val owner: NativeComponent[? <: dom.Element],
  private val slot: ChildSlot,
  private val renderBlock: T => Unit,
  private val renderScope: Scope
) extends Disposable {

  private val disposable = new CompositeDisposable()
  private var disposed = false

  disposable.add(
    source.observeWithoutInitial { value =>
      rebuild(value)
    }
  )

  def rebuild(value: T): Unit =
    if (!disposed) {
      slot.replace(
        StatementRuntime.collectChildren(owner, renderScope) {
          renderBlock(value)
        }
      )
    }

  override def dispose(): Unit =
    if (!disposed) {
      disposed = true
      disposable.dispose()
      slot.dispose()
    }

}

object ObserveRender {

  def apply[T](source: ReadOnlyProperty[T])(render: T => Unit)(using Scope): ObserveRender[T] =
    observeRender(source)(render)

  def observeRender[T](source: ReadOnlyProperty[T])(render: T => Unit): ObserveRender[T] =
    DslRuntime.currentScope { currentScope =>
      val owner = StatementRuntime.currentNativeParent("observeRender")
      val component = new ObserveRender(source, owner, owner.reserveChildSlot(), render, currentScope)

      owner.addDisposable(component)
      component.rebuild(source.get)
      component
    }

  def observeRenderValue[T](using observeRender: ObserveRender[T]): T =
    observeRender.source.get

}
