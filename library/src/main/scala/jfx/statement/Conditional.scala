package jfx.statement

import jfx.core.component.{ChildSlot, NativeComponent, NodeComponent}
import jfx.core.state.{CompositeDisposable, Disposable, ReadOnlyProperty}
import jfx.dsl.{DslRuntime, Scope}
import org.scalajs.dom

final class Conditional private[statement] (
  val condition: ReadOnlyProperty[Boolean],
  private val owner: NativeComponent[? <: dom.Element],
  private val slot: ChildSlot
) extends Disposable {

  private val disposable = new CompositeDisposable()
  private var disposed = false
  private var renderingEnabled = false
  private var thenBuilder: Option[() => Vector[NodeComponent[? <: dom.Node]]] = None
  private var elseBuilder: Option[() => Vector[NodeComponent[? <: dom.Node]]] = None

  disposable.add(
    condition.observeWithoutInitial { _ =>
      render()
    }
  )

  def registerThenBuilder(builder: () => Vector[NodeComponent[? <: dom.Node]]): Unit = {
    thenBuilder = Some(builder)
    if (renderingEnabled) render()
  }

  def registerElseBuilder(builder: () => Vector[NodeComponent[? <: dom.Node]]): Unit = {
    elseBuilder = Some(builder)
    if (renderingEnabled) render()
  }

  def startRendering(): Unit = {
    renderingEnabled = true
    render()
  }

  def render(): Unit =
    if (!disposed) {
      val children =
        if (condition.get) thenBuilder.map(_()).getOrElse(Vector.empty)
        else elseBuilder.map(_()).getOrElse(Vector.empty)

      slot.replace(children)
    }

  override def dispose(): Unit =
    if (!disposed) {
      disposed = true
      disposable.dispose()
      slot.dispose()
    }

}

object Conditional {

  def conditional(condition: ReadOnlyProperty[Boolean])(init: Conditional ?=> Unit): Conditional =
    DslRuntime.currentScope { currentScope =>
      val owner = StatementRuntime.currentNativeParent("conditional")
      val conditional = new Conditional(condition, owner, owner.reserveChildSlot())

      owner.addDisposable(conditional)

      given Scope = currentScope
      given Conditional = conditional
      init

      conditional.startRendering()
      conditional
    }

  def thenDo(init: Conditional ?=> Unit)(using conditional: Conditional): Conditional =
    DslRuntime.currentScope { currentScope =>
      conditional.registerThenBuilder { () =>
        StatementRuntime.collectChildren(conditional.owner, currentScope) {
          given Conditional = conditional
          init
        }
      }

      conditional
    }

  def elseDo(init: Conditional ?=> Unit)(using conditional: Conditional): Conditional =
    DslRuntime.currentScope { currentScope =>
      conditional.registerElseBuilder { () =>
        StatementRuntime.collectChildren(conditional.owner, currentScope) {
          given Conditional = conditional
          init
        }
      }

      conditional
    }

}
