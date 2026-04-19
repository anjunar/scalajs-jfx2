package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom

class Condition(val property: ReadOnlyProperty[Boolean]) extends Component {
  override def tagName: String = "" // Virtual fragment

  private var thenBuilder: Option[() => Unit] = None
  private var elseBuilder: Option[() => Unit] = None

  def registerThen(builder: => Unit): Unit = {
    thenBuilder = Some(() => builder)
  }

  def registerElse(builder: => Unit): Unit = {
    elseBuilder = Some(() => builder)
  }

  override def compose(): Unit = {
    render()
    addDisposable(property.observe { _ =>
      render()
    })
  }

  private def render(): Unit = {
    // 1. Dispose and remove old children
    children.toSeq.foreach { c =>
      removeChild(c)
      c.dispose()
    }

    // 2. Build new branch
    val builder = if (property.get) thenBuilder else elseBuilder
    builder.foreach { b =>
      // Start at our own calculated offset in the physical host
      val offset = calculateDomOffset
      val cursor = jfx.core.render.RenderBackend.current.insertionCursor(host, offset)
      DslRuntime.withCursor(cursor) {
        DslRuntime.withContext(ComponentContext(Some(this))) {
          b()
        }
      }
    }
  }
}

object Condition {
  def condition(property: ReadOnlyProperty[Boolean])(init: Condition ?=> Unit): Condition = {
    DslRuntime.build(new Condition(property)) { c ?=>
      init
    }
  }

  def thenDo(builder: => Unit)(using c: Condition): Unit = {
    c.registerThen(builder)
  }

  def elseDo(builder: => Unit)(using c: Condition): Unit = {
    c.registerElse(builder)
  }
}
