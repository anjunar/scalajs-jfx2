package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom

class Condition(val property: ReadOnlyProperty[Boolean]) extends Component {
  override def tagName: String = "" // Virtual fragment

  private var thenBuilder: Option[() => Unit] = None
  private var elseBuilder: Option[() => Unit] = None
  private var activeBackend: jfx.core.render.RenderBackend = _

  def registerThen(builder: => Unit): Unit = {
    thenBuilder = Some(() => builder)
  }

  def registerElse(builder: => Unit): Unit = {
    elseBuilder = Some(() => builder)
  }

  override def compose(): Unit = {
    activeBackend = jfx.core.render.RenderBackend.current
    addDisposable(property.observe { _ =>
      render()
    })
  }

  private def render(): Unit = {
    jfx.core.render.RenderBackend.withBackend(activeBackend) {
      // 1. Dispose and remove old children
      children.toSeq.foreach { c =>
        removeChild(c)
        c.dispose()
      }

      // 2. Build new branch
      val builder = if (property.get) thenBuilder else elseBuilder
      builder.foreach { b =>
        val cursor = parent match {
          case Some(_) => 
            val offset = calculateDomOffset
            jfx.core.render.RenderBackend.current.insertionCursor(host, offset)
          case None => 
            // Re-use binding cursor for root level
            bindCursor
        }

        DslRuntime.withCursor(cursor) {
          DslRuntime.withContext(ComponentContext(Some(this))) {
            b()
          }
        }
      }
    }
  }
}

object Condition {
  def condition(property: ReadOnlyProperty[Boolean])(init: Condition ?=> Unit): Condition = {
    val c = DslRuntime.build(new Condition(property)) { c ?=>
      init
    }
    c.render() // Trigger initial render after init block has registered branches
    c
  }

  def thenDo(builder: => Unit)(using c: Condition): Unit = {
    c.registerThen(builder)
  }

  def elseDo(builder: => Unit)(using c: Condition): Unit = {
    c.registerElse(builder)
  }
}
