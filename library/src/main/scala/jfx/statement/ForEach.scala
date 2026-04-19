package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime

class ForEach[T](val items: ListProperty[T], val renderItem: T => Unit) extends Component {
  override def tagName: String = "" 

  override def compose(): Unit = {
    items.foreach(renderItem)

    addDisposable(items.observeChanges {
      case ListProperty.Add(item, _) => appendItem(item)
      case ListProperty.Insert(index, item, _) => insertItem(index, item)
      case ListProperty.RemoveAt(index, _, _) => removeItem(index)
      case ListProperty.Reset(_) => resetItems()
      case _ => 
    })
  }

  private def appendItem(item: T): Unit = insertItem(children.length, item)

  private def insertItem(index: Int, item: T): Unit = {
    DslRuntime.updateBranch(this, Some(index)) {
      renderItem(item)
    }
  }

  private def removeItem(index: Int): Unit = {
    val child = children(index)
    removeChild(child)
    child.dispose()
  }

  private def resetItems(): Unit = {
    while (children.nonEmpty) removeItem(0)
    items.foreach(appendItem)
  }
}

object ForEach {
  def forEach[T](items: ListProperty[T])(renderItem: T => Unit): ForEach[T] = {
    DslRuntime.build(new ForEach(items, renderItem)) {}
  }
}
