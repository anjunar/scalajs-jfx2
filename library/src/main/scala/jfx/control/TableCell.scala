package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import jfx.statement.ObserveRender.observeRender

class TableCell[S, T] extends Box("div") {
  val itemProperty = Property[T | Null](null)
  val emptyProperty = Property(true)
  private val revisionProperty = Property(0)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-cell")
    addDisposable(itemProperty.observe(_ => bumpRevision()))
    addDisposable(emptyProperty.observe(_ => bumpRevision()))

    observeRender(revisionProperty) { _ =>
      renderItem(itemProperty.get, emptyProperty.get)
    }
  }

  private def renderItem(item: T | Null, empty: Boolean): Unit = {
    given Component = this

    if (!empty && item != null) {
      removeBaseClass("jfx-table-cell-empty")
      text = item.toString
    } else {
      addBaseClass("jfx-table-cell-empty")
    }
  }

  private def bumpRevision(): Unit =
    revisionProperty.set(revisionProperty.get + 1)

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
