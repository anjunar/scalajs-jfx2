package jfx.control

import jfx.core.component.Component
import jfx.dsl.DslRuntime

class TableColumn[S, T](val text: String, val cellFactory: S => Unit) extends Component {
  override def tagName: String = "jfx-column"
  override def compose(): Unit = {
    // Columns might render their header text or stay as metadata
  }
}

object TableColumn {
  def column[S, T](text: String)(cellFactory: S => Unit): TableColumn[S, T] = {
    DslRuntime.build(new TableColumn[S, T](text, cellFactory)) {
    }
  }
}
