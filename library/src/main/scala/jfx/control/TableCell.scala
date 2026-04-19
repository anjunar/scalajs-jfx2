package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*

class TableCell[S, T] extends Box("div") {
  val itemProperty = Property[T | Null](null)
  val emptyProperty = Property(true)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-cell")
    addDisposable(itemProperty.observe(item => {
      updateItem(item, emptyProperty.get)
    }))
    addDisposable(emptyProperty.observe(empty => {
      updateItem(itemProperty.get, empty)
    }))
  }

  protected def updateItem(item: T | Null, empty: Boolean): Unit = {
    given Component = this
    // Clear existing children (usually text)
    children.toSeq.foreach { c => removeChild(c); c.dispose() }
    
    if (!empty && item != null) {
      removeBaseClass("jfx-table-cell-empty")
      text = item.toString
    } else {
      addBaseClass("jfx-table-cell-empty")
    }
  }

  private[control] def applyRenderedItem(item: T | Null, empty: Boolean): Unit = {
    emptyProperty.set(empty)
    itemProperty.set(item)
  }
}

object TableCell {
  def cell[S, T](init: TableCell[S, T] ?=> Unit): TableCell[S, T] = {
    DslRuntime.build(new TableCell[S, T]())(init)
  }
}
