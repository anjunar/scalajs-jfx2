package jfx.statement

import jfx.core.component.Component
import jfx.core.render.Cursor
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.collection.mutable

/**
 * A structural branch that renders items from a ListProperty.
 */
class ForEach[T](
  val items: ListProperty[T],
  val renderItem: T => Unit
) extends Component {
  override def tagName: String = "jfx-foreach" 

  override def compose(): Unit = {
    items.foreach { item =>
      renderItem(item)
    }
  }
}

object ForEach {
  def forEach[T](items: ListProperty[T])(renderItem: T => Unit): ForEach[T] = {
    DslRuntime.build(new ForEach(items, renderItem)) { (f: ForEach[T]) => }
  }
}
