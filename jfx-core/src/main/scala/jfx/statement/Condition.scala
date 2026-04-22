package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime
import org.scalajs.dom

class Condition(val property: ReadOnlyProperty[Boolean]) extends Component {
  override def tagName: String = "" 
  private var thenBuilder: Option[() => Unit] = None
  private var elseBuilder: Option[() => Unit] = None

  def registerThen(builder: => Unit): Unit = thenBuilder = Some(() => builder)
  def registerElse(builder: => Unit): Unit = elseBuilder = Some(() => builder)

  override def compose(): Unit = {
    addDisposable(property.observe(_ => render()))
  }

  private[jfx] def render(): Unit = {
    DslRuntime.updateBranch(this) {
      renderInternal()
    }
  }

  private[jfx] def renderInternal(): Unit = {
    children.toSeq.foreach { c => removeChild(c); c.dispose() }
    if (property.get) thenBuilder.foreach(_())
    else elseBuilder.foreach(_())
  }
}

object Condition {
  def condition(property: ReadOnlyProperty[Boolean])(init: Condition ?=> Unit): Condition = {
    val condition = new Condition(property)
    DslRuntime.build(condition) {
      init(using condition)
      condition.renderInternal()
    }
  }
  def thenDo(builder: => Unit)(using c: Condition): Unit = c.registerThen(builder)
  def elseDo(builder: => Unit)(using c: Condition): Unit = c.registerElse(builder)
}
