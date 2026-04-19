package jfx.statement

import jfx.core.component.Component
import jfx.core.render.Cursor
import jfx.core.state.ListProperty
import jfx.dsl.{ComponentContext, DslRuntime}
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
    // Initial render
    items.foreach { item =>
      renderItem(item)
    }

    // React to changes
    addDisposable(items.observeChanges {
      case ListProperty.Add(item, _) =>
        appendItem(item)
      case ListProperty.Insert(index, item, _) =>
        insertItem(index, item)
      case ListProperty.RemoveAt(index, _, _) =>
        removeItem(index)
      case ListProperty.Reset(_) =>
        resetItems()
      case _ => // Handle other cases if needed
    })
  }

  private def appendItem(item: T): Unit = {
    insertItem(children.length, item)
  }

  private def insertItem(index: Int, item: T): Unit = {
    val cursor = jfx.core.render.RenderBackend.current.insertionCursor(host, index)
    DslRuntime.withCursor(cursor) {
      DslRuntime.withContext(ComponentContext(Some(this))) {
        renderItem(item)
      }
    }
  }

  private def removeItem(index: Int): Unit = {
    val child = children(index)
    removeChild(child)
    child.dispose()
  }

  private def resetItems(): Unit = {
    children.foreach(_.dispose())
    host.clearChildren()
    items.foreach(appendItem)
  }
}

object ForEach {
  def forEach[T](items: ListProperty[T])(renderItem: T => Unit): ForEach[T] = {
    DslRuntime.build(new ForEach(items, renderItem)) {
    }
  }
}
