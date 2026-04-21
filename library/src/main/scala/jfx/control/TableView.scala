package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import org.scalajs.dom

final class TableView[S] extends Box("div") {

  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  val rowHeightProperty = Property(32.0)
  val prefWidthProperty = Property[Option[Double]](None)
  
  val scrollTopProperty = Property(0.0)
  val scrollLeftProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  private case class VisibleRow(index: Int, item: S)
  private val visibleRowsProperty = new ListProperty[VisibleRow]()

  private def recomputeVisibleRows(): Unit = {
    val top = scrollTopProperty.get
    val height = viewportHeightProperty.get
    val rh = rowHeightProperty.get
    val itms = items.toSeq
    val total = itms.length
    
    if (total == 0) {
      visibleRowsProperty.setAll(Seq.empty)
    } else {
      val overscan = 4
      val firstVisible = math.floor(top / rh).toInt
      val visibleCount = math.ceil(height / rh).toInt + 1
      val start = math.max(0, firstVisible - overscan)
      val end = math.min(total, firstVisible + visibleCount + overscan)
      visibleRowsProperty.setAll((start until end).map(i => VisibleRow(i, itms(i))))
    }
  }

  private val totalColumnWidthProperty: ReadOnlyProperty[Double] = columns.asProperty.map { cols =>
    cols.toSeq.foldLeft(0.0)((sum, col) => sum + col.prefWidth)
  }

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-view")
    
    addDisposable(scrollTopProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(items.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(columns.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(rowHeightProperty.observe(_ => recomputeVisibleRows()))

    style {
      display = "flex"
      flexDirection = "column"
      width = "100%"
      height = "100%"
      overflow = "hidden"
    }

    condition(showHeaderProperty) {
      thenDo {
        div {
          addClass("jfx-table-header")
          style { position = "relative"; overflow = "hidden"; width = "100%"; flex = "0 0 auto" }

          div {
            addClass("jfx-table-header-content")
            style {
              display = "flex"
              width_=(totalColumnWidthProperty.map(w => s"${w}px"))
              transform_=(scrollLeftProperty.map(l => s"translateX(-${l}px)"))
            }

            forEach(columns) { col =>
              div {
                addClass("jfx-table-header-cell")
                style {
                  width = s"${col.prefWidth}px"
                  flex = "0 0 auto"
                  boxSizing = "border-box"
                }
                text = col.text
              }
            }
          }
        }
      }
    }

    div {
      addClass("jfx-table-viewport")
      style { flex = "1 1 auto"; position = "relative"; overflow = "auto"; width = "100%" }

      onScroll { e =>
        val target = e.target.asInstanceOf[dom.html.Div]
        scrollTopProperty.set(target.scrollTop)
        scrollLeftProperty.set(target.scrollLeft)
        viewportHeightProperty.set(target.clientHeight.toDouble)
      }

      if (!RenderBackend.current.isServer) {
         dom.window.requestAnimationFrame { _ =>
            viewportHeightProperty.set(host.clientHeight.toDouble)
            recomputeVisibleRows()
         }
      }

      div {
        addClass("jfx-table-content")
        style {
          position = "relative"
          height_=(items.asProperty.map(it => s"${it.length * rowHeightProperty.get}px"))
          width_=(totalColumnWidthProperty.map(w => s"${w}px"))
        }

        forEach(visibleRowsProperty) { rowDef =>
          div {
            addClass("jfx-table-row-slot")
            style {
              position = "absolute"
              top_=(Property(s"${rowDef.index * rowHeightProperty.get}px"))
              left = "0"
              width_=(totalColumnWidthProperty.map(w => s"${w}px"))
              height = s"${rowHeightProperty.get}px"
              display = "flex"
            }

            val row = new TableRow[S]()
            DslRuntime.build(row) {
              row.bind(rowDef.index, rowDef.item, TableView.this, columns.get.toSeq, rowHeightProperty.get)
            }
          }
        }
      }
    }
  }
}

object TableView {

  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] =
    DslRuntime.build(new TableView[S])(init)

  def items[S](using t: TableView[S]): ListProperty[S] =
    t.items

  def items_=[S](v: scala.collection.IterableOnce[S])(using t: TableView[S]): Unit =
    t.items.setAll(v)

  def items_=[S](v: scala.scalajs.js.Array[S])(using t: TableView[S]): Unit =
    t.items.setAll(v)

  def rowHeight(using t: TableView[?]): Double = t.rowHeightProperty.get
  def rowHeight_=(v: Double)(using t: TableView[?]): Unit = t.rowHeightProperty.set(v)

  def showHeader(using t: TableView[?]): Boolean = t.showHeaderProperty.get
  def showHeader_=(v: Boolean)(using t: TableView[?]): Unit = t.showHeaderProperty.set(v)

  def prefWidth(using t: TableView[?]): Option[Double] = t.prefWidthProperty.get
  def prefWidth_=(v: Double)(using t: TableView[?]): Unit = t.prefWidthProperty.set(Some(v))
  def prefWidth_=(v: Option[Double])(using t: TableView[?]): Unit = t.prefWidthProperty.set(v)
  def prefWidth_=(v: jfx.core.state.ReadOnlyProperty[Double])(using t: TableView[?]): Unit =
    t.addDisposable(v.observe(w => t.prefWidthProperty.set(Some(w))))

  def columns[S](using t: TableView[S]): ListProperty[TableColumn[S, ?]] =
    t.columns
}
