package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Condition(val property: ReadOnlyProperty[Boolean]) extends Component {
  override def tagName: String = "" 
  private var thenBuilder: Option[() => Unit] = None
  private var elseBuilder: Option[() => Unit] = None
  private var activeBackend: jfx.core.render.RenderBackend = _

  def registerThen(builder: => Unit): Unit = thenBuilder = Some(() => builder)
  def registerElse(builder: => Unit): Unit = elseBuilder = Some(() => builder)

  override def compose(): Unit = {
    activeBackend = jfx.core.render.RenderBackend.current
    addDisposable(property.observe(_ => render()))
  }

  private[jfx] def render(): Unit = {
    DslRuntime.updateBranch(this, activeBackend) {
      children.toSeq.foreach { c => removeChild(c); c.dispose() }
      if (property.get) thenBuilder.foreach(_())
      else elseBuilder.foreach(_())
    }
  }
}

object Condition {
  def condition(property: ReadOnlyProperty[Boolean])(init: Condition ?=> Unit): Condition = {
    val c = DslRuntime.build(new Condition(property))(init)
    c.render()
    c
  }
  def thenDo(builder: => Unit)(using c: Condition): Unit = c.registerThen(builder)
  def elseDo(builder: => Unit)(using c: Condition): Unit = c.registerElse(builder)
}
