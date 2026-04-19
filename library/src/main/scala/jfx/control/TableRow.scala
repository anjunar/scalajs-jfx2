package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{Disposable, Property}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*

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
    indexProperty.set(rowIndex)
    itemProperty.set(rowValue)
    
    // Update position
    style {
      top = s"${rowIndex * rowHeight}px"
      height = s"${rowHeight}px"
    }

    // For Virtualization, we MUST rebuild or update the cell content.
    // Since cellRenderer is a function S => Unit, we currently rebuild the cell's children
    // to ensure the renderer runs with the NEW rowValue.
    
    // Safety check: if we have children (cells), we need to clear them to re-render with new data
    // because cellRenderer might have created components that are now stale.
    children.toSeq.foreach { c => removeChild(c); c.dispose() }

    columns.foreach { col =>
      val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
      Box.box("div") {
        addClass("jfx-table-cell")
        // Apply column width if set
        style {
          width = s"${typedColumn.prefWidth}px"
          minWidth = s"${typedColumn.prefWidth}px"
          flex = "0 0 auto"
        }
        typedColumn.cellRenderer.foreach(r => r(rowValue))
      }
    }
  }
}
