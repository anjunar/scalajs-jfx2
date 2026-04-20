package jfx.control

import jfx.core.component.Component
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import scala.annotation.targetName

class TableColumn[S, T](initialText: String = "") extends Component {
  override def tagName: String = "" 

  val textProperty = Property(initialText)
  val prefWidthProperty = Property(160.0)
  
  // Cell Content logic
  private var _cellRenderer: Option[S => Unit] = None
  def setCellRenderer(renderer: S => Unit): Unit = _cellRenderer = Some(renderer)
  def cellRenderer: Option[S => Unit] = _cellRenderer

  def text: String = textProperty.get
  def text_=(value: String): Unit = textProperty.set(value)

  def prefWidth: Double = prefWidthProperty.get
  def prefWidth_=(value: Double): Unit = prefWidthProperty.set(value)
}

object TableColumn {
  def column[S, T](text: String)(init: TableColumn[S, T] ?=> Unit): TableColumn[S, T] = {
    val col = DslRuntime.build(new TableColumn[S, T](text))(init)
    DslRuntime.currentContext.parent match {
      case Some(t: TableView[S] @unchecked) => t.columns += col
      case _ => 
    }
    col
  }

  // JFX2 simplified column API: column("Title") { item => text = item.title }
  @targetName("columnWithRenderer")
  def column[S, T](text: String)(renderer: S => Unit): TableColumn[S, T] = {
    val col = new TableColumn[S, T](text)
    col.setCellRenderer(renderer)
    DslRuntime.build(col) {}
    DslRuntime.currentContext.parent match {
      case Some(t: TableView[S] @unchecked) => t.columns += col
      case _ => 
    }
    col
  }

  def prefWidth[S, T](using c: TableColumn[S, T]): Double = c.prefWidthProperty.get
  def prefWidth_=[S, T](value: Double)(using c: TableColumn[S, T]): Unit = c.prefWidthProperty.set(value)
  def prefWidth_=[S, T](value: jfx.core.state.ReadOnlyProperty[Double])(using c: TableColumn[S, T]): Unit =
    c.addDisposable(value.observe(c.prefWidthProperty.set))

  def cellRenderer_=[S](renderer: S => Unit)(using c: TableColumn[S, ?]): Unit = c.setCellRenderer(renderer)

  def cellValueFactory[S, T](using c: TableColumn[S, T]): TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null =
    throw new UnsupportedOperationException("Not implemented in JFX2 yet")
  
  final case class CellDataFeatures[S, T](
    tableView: TableView[S],
    tableColumn: TableColumn[S, T],
    value: S,
    index: Int
  )
}
