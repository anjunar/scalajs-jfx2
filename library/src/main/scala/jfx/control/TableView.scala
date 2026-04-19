package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import jfx.statement.ForEach.forEach

class TableView[S] extends Box("div") {
  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-view")
    
    // 1. Header
    jfx.statement.Condition.condition(showHeaderProperty) {
      jfx.statement.Condition.thenDo {
        Box.box("div") {
          addClass("jfx-table-header")
          forEach(columns) { col =>
            Box.box("div") {
              addClass("jfx-table-header-cell")
              text = col.text
            }
          }
        }
      }
    }

    // 2. Body
    Box.box("div") {
      addClass("jfx-table-body")
      forEach(items) { (item: S) =>
        val row = new TableRow[S]()
        DslRuntime.build(row) {
          row.bind(0, item, this, columns.get.toSeq)
        }
      }
    }
  }
}

object TableView {
  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] = {
    DslRuntime.build(new TableView[S])(init)
  }

  def items[S](using t: TableView[S]): ListProperty[S] = t.items
}
