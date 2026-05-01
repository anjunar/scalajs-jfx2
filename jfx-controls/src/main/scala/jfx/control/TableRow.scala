package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{Disposable, Property}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import org.scalajs.dom
import jfx.statement.ObserveRender.observeRender

class TableRow[S] extends Box("div") {
  val $itemProperty = Property[S | Null](null)
  val $indexProperty = Property(-1)

  private sealed trait RowState
  private case object Unbound extends RowState
  private final case class Bound(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ) extends RowState
  private final case class Placeholder(
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ) extends RowState

  private val rowStateProperty = Property[RowState](Unbound)

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

    observeRender(rowStateProperty) {
      case Unbound =>
        ()
      case Bound(rowIndex, rowValue, tableView, columns, rowHeight) =>
        renderBound(rowIndex, rowValue, tableView, columns, rowHeight)
      case Placeholder(rowIndex, tableView, columns, rowHeight) =>
        renderPlaceholder(rowIndex, tableView, columns, rowHeight)
    }
  }

  private[control] def bind(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    $indexProperty.set(rowIndex)
    $itemProperty.set(rowValue)
    rowStateProperty.set(Bound(rowIndex, rowValue, tableView, columns, rowHeight))
  }

  private def renderBound(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    given Component = this
    removeBaseClass("jfx-table-row-empty")
    removeBaseClass("jfx-table-row-placeholder")
    setParityClasses(rowIndex)

    val isSelected = tableView.$selectedIndexProperty.map(_ == rowIndex)
    classIf("jfx-table-row-selected", isSelected)
    
    attribute("aria-selected", isSelected.map(_.toString))

    onClick { _ =>
      tableView.select(rowIndex)
    }

    columns.zipWithIndex.foreach { case (col, colIndex) =>
      val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
      Box.box("div") {
        addClass("jfx-table-cell")
        style {
          val w = tableView.renderedWidthsProperty.map(ws => s"${ws.lift(colIndex).getOrElse(typedColumn.$prefWidth)}px")
          width_=(w)
          minWidth_=(w)
          flex = "0 0 auto"
        }
        typedColumn.$cellRenderer.get.foreach(r => r(rowValue))
      }
    }
  }

  private[control] def bindPlaceholder(
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    $indexProperty.set(rowIndex)
    $itemProperty.set(null)
    rowStateProperty.set(Placeholder(rowIndex, tableView, columns, rowHeight))
  }

  private def renderPlaceholder(
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  ): Unit = {
    given Component = this
    addBaseClass("jfx-table-row-empty")
    addBaseClass("jfx-table-row-placeholder")
    setParityClasses(rowIndex)

    attribute("aria-selected", "false")

    columns.zipWithIndex.foreach { case (col, colIndex) =>
      val typedColumn = col.asInstanceOf[TableColumn[S, Any]]
      Box.box("div") {
        addClass("jfx-table-cell")
        addClass("jfx-table-cell-empty")
        addClass("jfx-table-cell-loading-placeholder")
        style {
          val w = tableView.renderedWidthsProperty.map(ws => s"${ws.lift(colIndex).getOrElse(typedColumn.$prefWidth)}px")
          width_=(w)
          minWidth_=(w)
          flex = "0 0 auto"
        }
      }
    }
  }

  private def setParityClasses(rowIndex: Int): Unit = {
    if (rowIndex % 2 == 0) {
      addBaseClass("jfx-table-row-even")
      removeBaseClass("jfx-table-row-odd")
    } else {
      addBaseClass("jfx-table-row-odd")
      removeBaseClass("jfx-table-row-even")
    }
  }
}

object TableRow {
  def tableRow[S](init: TableRow[S] ?=> Unit): TableRow[S] =
    DslRuntime.build(new TableRow[S]())(init)

  def rowItem[S](
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  )(using row: TableRow[S]): Unit =
    row.bind(rowIndex, rowValue, tableView, columns, rowHeight)

  def placeholderRow[S](
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double
  )(using row: TableRow[S]): Unit =
    row.bindPlaceholder(rowIndex, tableView, columns, rowHeight)
}
