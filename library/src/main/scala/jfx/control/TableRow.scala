package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{Disposable, Property}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import org.scalajs.dom

class TableRow[S] extends Box("div") {
  val itemProperty = Property[S | Null](null)
  val indexProperty = Property(-1)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-row")
    style {
      display = "flex"
      width = "100%"
      position = "absolute"
      left = "0"
      top = "0"
    }
  }

  private[control] def bind(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    given Component = this
    removeBaseClass("jfx-table-row-empty")
    removeBaseClass("jfx-table-row-placeholder")
    indexProperty.set(rowIndex)
    itemProperty.set(rowValue)
    
    val isSelected = tableView.selectedIndexProperty.map(_ == rowIndex)
    classIf("jfx-table-row-selected", isSelected)
    classIf("jfx-table-row-even", Property(rowIndex % 2 == 0))
    classIf("jfx-table-row-odd", Property(rowIndex % 2 != 0))
    
    attribute("aria-selected", isSelected.map(_.toString))

    onClick { _ =>
      tableView.select(rowIndex)
    }

    children.toSeq.foreach { c => removeChild(c); c.dispose() }

    columns.zipWithIndex.foreach { case (col, colIndex) =>
      val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
      Box.box("div") {
        addClass("jfx-table-cell")
        style {
          val w = tableView.renderedWidthsProperty.map(ws => s"${ws.lift(colIndex).getOrElse(typedColumn.prefWidth)}px")
          width_=(w)
          minWidth_=(w)
          flex = "0 0 auto"
        }
        typedColumn.cellRenderer.get.foreach(r => r(rowValue))
      }
    }
  }

  private[control] def bindPlaceholder(
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    given Component = this
    indexProperty.set(rowIndex)
    itemProperty.set(null)

    addBaseClass("jfx-table-row-empty")
    addBaseClass("jfx-table-row-placeholder")
    if (rowIndex % 2 == 0) {
      addBaseClass("jfx-table-row-even")
      removeBaseClass("jfx-table-row-odd")
    } else {
      addBaseClass("jfx-table-row-odd")
      removeBaseClass("jfx-table-row-even")
    }

    attribute("aria-selected", "false")

    children.toSeq.foreach { c => removeChild(c); c.dispose() }

    columns.zipWithIndex.foreach { case (col, colIndex) =>
      val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
      Box.box("div") {
        addClass("jfx-table-cell")
        addClass("jfx-table-cell-empty")
        addClass("jfx-table-cell-loading-placeholder")
        style {
          val w = tableView.renderedWidthsProperty.map(ws => s"${ws.lift(colIndex).getOrElse(typedColumn.prefWidth)}px")
          width_=(w)
          minWidth_=(w)
          flex = "0 0 auto"
        }
      }
    }
  }
}
