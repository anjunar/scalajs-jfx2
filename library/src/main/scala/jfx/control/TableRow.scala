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
    }
  }

  private[control] def bind(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]]
  ): Unit = {
    given Component = this
    indexProperty.set(rowIndex)
    itemProperty.set(rowValue)

    // In JFX2, we follow the "Truth" principle. 
    // If the number of columns changed, we rebuild.
    // If not, we could update, but for now, simple rebuild is safer for hydration truth.
    if (children.length != columns.length) {
      children.toSeq.foreach { c => removeChild(c); c.dispose() }

      columns.foreach { col =>
        val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
        Box.box("div") {
          addClass("jfx-table-cell")
          typedColumn.cellRenderer.foreach(r => r(rowValue))
        }
      }
    } else {
       // Optional: more fine-grained update logic
       // For now, if we want to be truly reactive inside the row, 
       // we'd need to re-render the cells or use properties.
    }
  }
}
