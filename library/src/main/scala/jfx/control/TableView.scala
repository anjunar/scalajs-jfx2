package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime

class TableView[S] extends Component {
  override def tagName: String = "div"
  
  val items = new ListProperty[S]()

  // INTERNE STRUKTUR: Wird über compose() definiert
  override def compose(): Unit = {
    import Box.*
    
    // Header Branch
    box("div") { b ?=>
       b.classes = Seq("jfx-table-header")
       text("I am the header")
    }
    
    // Body Branch
    box("div") { b ?=>
       b.classes = Seq("jfx-table-body")
       import jfx.statement.ForEach.forEach
       forEach(items) { item =>
          box("div") { row ?=>
             row.classes = Seq("jfx-table-row")
             text(item.toString)
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
