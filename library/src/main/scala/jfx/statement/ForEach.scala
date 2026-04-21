package jfx.statement

import jfx.core.component.Component
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime

class ForEach[T](val items: ListProperty[T], val renderItem: (T, Int) => Any) extends Component {
  override def tagName: String = "" 

  override def compose(): Unit = {
    items.zipWithIndex.foreach { (item, index) => 
      insertItem(index, item)
    }

    addDisposable(items.observeChanges {
      case ListProperty.Add(item, _) => appendItem(item)
      case ListProperty.Insert(index, _, _) => rebuildFrom(index)
      case ListProperty.InsertAll(index, _, _) => rebuildFrom(index)
      case ListProperty.RemoveAt(index, _, _) => rebuildFrom(index)
      case ListProperty.RemoveRange(index, _, _) => rebuildFrom(index)
      case ListProperty.UpdateAt(index, _, newItem, _) => replaceItem(index, newItem)
      case ListProperty.Patch(from, _, _, _) => rebuildFrom(from)
      case ListProperty.Clear(_, _) => resetItems()
      case ListProperty.Reset(_) => resetItems()
    })
  }

  private def appendItem(item: T): Unit = insertItem(children.length, item)

  private def insertItem(index: Int, item: T): Unit = {
    DslRuntime.updateBranch(this, Some(index)) {
      renderItem(item, index)
    }
  }

  private def removeItem(index: Int): Unit = {
    if (index >= 0 && index < _children.length) {
      val child = _children(index)
      removeChild(child)
      child.dispose()
    }
  }

  private def replaceItem(index: Int, item: T): Unit = {
    if (index >= 0 && index < _children.length) {
      removeItem(index)
      insertItem(index, item)
    } else {
      rebuildFrom(index)
    }
  }

  private def rebuildFrom(index: Int): Unit = {
    val start = math.max(0, math.min(index, children.length))
    while (children.length > start) removeItem(start)

    var currentIndex = start
    while (currentIndex < items.length) {
      insertItem(currentIndex, items(currentIndex))
      currentIndex += 1
    }
  }

  private def resetItems(): Unit = {
    while (children.nonEmpty) removeItem(0)
    items.zipWithIndex.foreach { (item, index) =>
      insertItem(index, item)
    }
  }
}

object ForEach {
  def apply[T](items: ListProperty[T])(renderItem: (T, Int) => Any): ForEach[T] =
    forEach(items)(renderItem)

  def apply[T](items: ListProperty[T])(renderItem: T => Any): ForEach[T] =
    forEach(items)(renderItem)

  def forEach[T](items: ListProperty[T])(renderItem: (T, Int) => Any): ForEach[T] = {
    DslRuntime.build(new ForEach(items, renderItem)) {}
  }

  def forEach[T](items: ListProperty[T])(renderItem: T => Any): ForEach[T] = {
    forEach(items) { (item, _) =>
      renderItem(item)
    }
  }
}
