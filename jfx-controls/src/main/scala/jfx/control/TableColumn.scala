package jfx.control

import jfx.core.component.Component
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import scala.annotation.targetName

class TableColumn[S, T](initialText: String = "") extends Component {

  override def tagName: String = ""

  val textProperty: Property[String] = Property(initialText)
  val prefWidthProperty: Property[Double] = Property(160.0)
  val cellRenderer: Property[Option[S => Unit]] = Property(None)
  
  val sortableProperty = Property(false)
  val sortKeyProperty = Property[Option[String]](None)

  def text: String = textProperty.get
  def text_=(value: String): Unit = textProperty.set(value)

  def prefWidth: Double = prefWidthProperty.get
  def prefWidth_=(value: Double): Unit = prefWidthProperty.set(value)
  
  def setCellRenderer(renderer: S => Unit): Unit = {
    cellRenderer.set(Some(renderer))
  }

  override def compose(): Unit = {
  }
}

object TableColumn {
  def tableColumn[S, T](text: String)(init: TableColumn[S, T] ?=> Unit): TableColumn[S, T] = {
    val col = new TableColumn[S, T](text)
    DslRuntime.build(col)(init)
    
    DslRuntime.currentContext.parent match {
      case Some(t: TableView[S]) => t.columns += col
      case _ => 
    }
    col
  }

  def column[S, T](text: String)(init: TableColumn[S, T] ?=> Unit): TableColumn[S, T] = {
    tableColumn(text)(init)
  }

  @targetName("columnWithRenderer")
  def column[S, T](text: String)(renderer: S => Unit): TableColumn[S, T] = {
    val col = new TableColumn[S, T](text)
    col.setCellRenderer(renderer)
    DslRuntime.currentContext.parent match {
      case Some(t: TableView[S]) => t.columns += col
      case _ => 
    }
    col
  }

  def prefWidth[S, T](using c: TableColumn[S, T]): Double = c.prefWidthProperty.get
  def prefWidth_=[S, T](value: Double)(using c: TableColumn[S, T]): Unit = c.prefWidthProperty.set(value)
  def prefWidth_=[S, T](value: jfx.core.state.ReadOnlyProperty[Double])(using c: TableColumn[S, T]): Unit =
    c.addDisposable(value.observe(c.prefWidthProperty.set))

  def cellRenderer_=[S](renderer: S => Unit)(using c: TableColumn[S, ?]): Unit = c.setCellRenderer(renderer)

  def cellValueFactory[S, T](using tableColumn: TableColumn[S, T]): TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null =
    throw new UnsupportedOperationException("Not implemented in JFX2 yet")
  
  def sortable[S, T](using c: TableColumn[S, T]): Boolean = c.sortableProperty.get
  def sortable_=[S, T](v: Boolean)(using c: TableColumn[S, T]): Unit = c.sortableProperty.set(v)
  
  def sortKey[S, T](using c: TableColumn[S, T]): Option[String] = c.sortKeyProperty.get
  def sortKey_=[S, T](v: String)(using c: TableColumn[S, T]): Unit = c.sortKeyProperty.set(Some(v))

  final case class CellDataFeatures[S, T](
    tableView: TableView[S],
    tableColumn: TableColumn[S, T],
    value: S,
    index: Int
  )

  private[control] def withEnclosingTableView[S, A](tableView: TableView[S])(block: => A): A = {
    block
  }
}
