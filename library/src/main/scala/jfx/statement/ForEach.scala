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
  override def tagName: String = "" 
  private var activeBackend: jfx.core.render.RenderBackend = _

  override def compose(): Unit = {
    activeBackend = jfx.core.render.RenderBackend.current
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
    jfx.core.render.RenderBackend.withBackend(activeBackend) {
      // 1. Physical target
      val baseOffset = calculateDomOffset
      val itemOffset = children.take(index).map(_.domNodeCount).sum
      val cursor = jfx.core.render.RenderBackend.current.insertionCursor(host, baseOffset + itemOffset)
      
      // 2. Build with logical index
      DslRuntime.withCursor(cursor) {
        DslRuntime.withContext(ComponentContext(Some(this), Some(index))) {
          renderItem(item)
        }
      }
    }
  }

  private def removeItem(index: Int): Unit = {
    jfx.core.render.RenderBackend.withBackend(activeBackend) {
      val child = children(index)
      removeChild(child)
      child.dispose()
    }
  }

  private def resetItems(): Unit = {
    jfx.core.render.RenderBackend.withBackend(activeBackend) {
      children.toSeq.foreach { c =>
        removeChild(c)
        c.dispose()
      }
      items.foreach(appendItem)
    }
  }
}

object ForEach {
  def forEach[T](items: ListProperty[T])(renderItem: T => Unit): ForEach[T] = {
    DslRuntime.build(new ForEach(items, renderItem)) {
    }
  }
}
