package jfx.statement

import jfx.core.component.Component
import jfx.core.render.{Cursor, HostElement}
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom

class Condition(
  val property: ReadOnlyProperty[Boolean],
  val thenBranch: () => Unit,
  val elseBranch: () => Unit
) extends Component {
  override def tagName: String = "jfx-condition"

  override def compose(): Unit = {
    render()

    addDisposable(property.observe { _ =>
      render()
    })
  }

  private def render(): Unit = {
    // Clear old children
    children.toSeq.foreach { c =>
      removeChild(c)
      c.dispose()
    }
    host.clearChildren()

    val cursor = jfx.core.render.RenderBackend.current.insertionCursor(host, 0)
    DslRuntime.withCursor(cursor) {
      DslRuntime.withContext(ComponentContext(Some(this))) {
        if (property.get) thenBranch()
        else elseBranch()
      }
    }
  }
}

object Condition {
  def condition(property: ReadOnlyProperty[Boolean])(thenBranch: => Unit)(using dummy: DummyImplicit): Condition = {
     DslRuntime.build(new Condition(property, () => thenBranch, () => ())) {
     }
  }

  def condition(property: ReadOnlyProperty[Boolean])(thenBranch: => Unit)(elseBranch: => Unit): Condition = {
     DslRuntime.build(new Condition(property, () => thenBranch, () => elseBranch)) {
     }
  }
}
