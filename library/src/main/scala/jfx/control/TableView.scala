package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime
import jfx.statement.ForEach.forEach

class TableView[S] extends Component {
  override def tagName: String = "div"
  
  val items = new ListProperty[S]()
  
  def columns: Seq[TableColumn[S, ?]] = children.collect { case c: TableColumn[S, ?] => c }

  override def compose(): Unit = {
    host.setClassNames(Seq("jfx-table-view"))

    // 1. Header Area
    Box.box("div") {
      classes = Seq("jfx-table-header")
      columns.foreach { col =>
        Box.box("div") {
          classes = Seq("jfx-table-header-cell")
          text = col.text
        }
      }
    }
    
    // 2. Body Area
    Box.box("div") {
      classes = Seq("jfx-table-body")
      forEach(items) { item =>
        Box.box("div") {
          classes = Seq("jfx-table-row")
          columns.foreach { col =>
            Box.box("div") {
              classes = Seq("jfx-table-cell")
              col.cellFactory(item)
            }
          }
        }
      }
    }
  }
}

object TableView {
  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] = {
    DslRuntime.build(new TableView[S])(init)
  }
}
