package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime

class ObserveRender[T](
  val source: ReadOnlyProperty[T],
  private val renderBlock: ObserveRender[T] ?=> T => Any
) extends Component {

  override def tagName: String = ""

  override def compose(): Unit = {
    addDisposable(source.observe(render))
  }

  private def render(value: T): Unit = {
    DslRuntime.updateBranch(this) {
      given ObserveRender[T] = this
      clearRenderedChildren()
      renderBlock(value)
    }
  }

  private def clearRenderedChildren(): Unit = {
    children.toSeq.foreach { child =>
      removeChild(child)
      child.dispose()
    }
  }
}

object ObserveRender {

  def apply[T](source: ReadOnlyProperty[T])(render: ObserveRender[T] ?=> T => Any): ObserveRender[T] =
    observeRender(source)(render)

  def observeRender[T](source: ReadOnlyProperty[T])(render: ObserveRender[T] ?=> T => Any): ObserveRender[T] =
    DslRuntime.build(new ObserveRender(source, render)) {}

  def observeRenderValue[T](using observeRender: ObserveRender[T]): T =
    observeRender.source.get
}
