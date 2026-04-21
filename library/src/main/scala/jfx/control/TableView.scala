package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import jfx.router.RouteContext
import jfx.control.Link.link
import org.scalajs.dom
import scala.scalajs.js

final class TableView[S] extends Box("div") {

  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  val rowHeightProperty = Property(32.0)
  val prefWidthProperty = Property[Option[Double]](None)
  
  val scrollTopProperty = Property(0.0)
  val scrollLeftProperty = Property(0.0)
  val viewportWidthProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  val crawlableProperty = Property(false)
  private val defaultLimit = 50

  val selectedIndexProperty = Property(-1)
  val selectedItemProperty = Property[S | Null](null)
  val placeholderProperty = Property[Component | Null](null)

  private case class VisibleRow(index: Int, item: S)
  private val visibleRowsProperty = new ListProperty[VisibleRow]()

  private def getRouteContext: Option[RouteContext] = 
    try { Some(DslRuntime.service[RouteContext]) } catch { case _: Exception => None }

  private def getCrawlParams: (Int, Int) = {
    val ctx = getRouteContext
    val offset = ctx.flatMap(_.queryParams.get("offset")).flatMap(_.toIntOption).getOrElse(0)
    val limit = ctx.flatMap(_.queryParams.get("limit")).flatMap(_.toIntOption).getOrElse(defaultLimit)
    (offset, limit)
  }

  private def recomputeVisibleRows(): Unit = {
    val itms = items.toSeq
    val total = itms.length

    if (RenderBackend.current.isServer && crawlableProperty.get) {
      val (offset, limit) = getCrawlParams
      val end = math.min(total, offset + limit)
      visibleRowsProperty.setAll((offset until end).map(i => VisibleRow(i, itms(i))))
    } else {
      val top = scrollTopProperty.get
      val height = viewportHeightProperty.get
      val rh = rowHeightProperty.get
      
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
  }

  val renderedWidthsProperty: ReadOnlyProperty[Vector[Double]] = {
    viewportWidthProperty.flatMap { vw =>
      columns.asProperty.map { cols =>
        resolveRenderedColumnWidths(cols.toSeq, vw)
      }
    }
  }

  private val totalColumnWidthProperty: ReadOnlyProperty[Double] = renderedWidthsProperty.map(_.sum)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-view")
    
    addDisposable(scrollTopProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportWidthProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(items.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(columns.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(rowHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(selectedIndexProperty.observe(_ => recomputeVisibleRows()))

    addDisposable(selectedIndexProperty.observe { idx =>
      if (idx >= 0 && idx < items.length) {
        selectedItemProperty.set(items(idx))
      } else {
        selectedItemProperty.set(null)
      }
    })

    if (!RenderBackend.current.isServer) {
       val (offset, _) = getCrawlParams
       if (offset > 0) {
          scrollTopProperty.set(offset * rowHeightProperty.get)
       }
    }

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
              val index = columns.indexOf(col)
              div {
                addClass("jfx-table-header-cell")
                classIf("jfx-table-header-cell-sortable", col.sortableProperty)
                style {
                  width_=(renderedWidthsProperty.map(ws => s"${ws.lift(index).getOrElse(col.prefWidth)}px"))
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
        viewportWidthProperty.set(target.clientWidth.toDouble)
      }

      if (!RenderBackend.current.isServer) {
         dom.window.requestAnimationFrame { _ =>
            val target = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[dom.html.Div]
            viewportHeightProperty.set(target.clientHeight.toDouble)
            viewportWidthProperty.set(target.clientWidth.toDouble)
            
            val (offset, _) = getCrawlParams
            if (offset > 0) {
               target.scrollTop = offset * rowHeightProperty.get
            }
            recomputeVisibleRows()
         }
      }

      div {
        addClass("jfx-table-content")
        style {
          position = "relative"
          if (!RenderBackend.current.isServer) {
            height_=(items.asProperty.map(it => s"${it.length * rowHeightProperty.get}px"))
          }
          width_=(totalColumnWidthProperty.map(w => s"${w}px"))
        }

        condition(items.asProperty.map(_.isEmpty)) {
          thenDo {
            div {
              addClass("jfx-table-placeholder")
              style {
                position = "absolute"
                top = "0"; left = "0"; width = "100%"; height = "100%"
                display = "flex"; alignItems = "center"; justifyContent = "center"
              }
              val custom = placeholderProperty.get
              if (custom != null) {
                // To follow pure DSL, placeholders should be defined in compose
                // or via a mechanism that respects component scope.
                // For now we just don't do anything complex here.
                ()
              } else {
                text = "No content in table"
              }
            }
          }
          elseDo {
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

      val hasMoreProperty = items.asProperty.map { itms =>
         val (offset, limit) = getCrawlParams
         crawlableProperty.get && offset + limit < itms.length
      }

      condition(hasMoreProperty) {
        thenDo {
           val (offset, limit) = getCrawlParams
           val currentPath = getRouteContext.map(_.path).getOrElse("")
           link(s"$currentPath?offset=${offset + limit}&limit=$limit") {
             style { 
               display = "block"
               padding = "20px"
               textAlign = "center"
               if (RenderBackend.current.isServer) {
                 marginTop = s"${(offset + limit) * rowHeightProperty.get}px"
               }
             }
             addClass("jfx-table-more-link")
             text = "More items..."
           }
        }
      }
    }
  }

  def select(index: Int): Unit = {
    if (index >= 0 && index < items.length) {
      selectedIndexProperty.set(index)
    } else {
      selectedIndexProperty.set(-1)
    }
  }

  def select(item: S): Unit = {
    val idx = items.toSeq.indexOf(item)
    select(idx)
  }

  private def resolveRenderedColumnWidths(
    columns: Seq[TableColumn[S, ?]],
    viewportWidth: Double
  ): Vector[Double] = {
    if (columns.isEmpty) return Vector.empty

    val baseWidths = columns.map(_.prefWidth).toVector
    val minWidths = columns.map(_ => 40.0).toVector
    val maxWidths = columns.map(_ => Double.PositiveInfinity).toVector
    val resizableIndices = columns.indices.toVector

    val minTotal = minWidths.sum
    val targetTotal = math.max(minTotal, viewportWidth)

    val delta = targetTotal - baseWidths.sum
    if (math.abs(delta) < 0.5) return baseWidths

    val adjusted =
      if (delta > 0) {
        distributeWidthDelta(
          widths = baseWidths,
          indices = resizableIndices,
          lowerBounds = minWidths,
          upperBounds = maxWidths,
          delta = delta,
          weight = index => math.max(1.0, baseWidths(index))
        )
      } else {
        distributeWidthDelta(
          widths = baseWidths,
          indices = resizableIndices,
          lowerBounds = minWidths,
          upperBounds = maxWidths,
          delta = delta,
          weight = index => math.max(1.0, baseWidths(index) - minWidths(index))
        )
      }

    adjusted
  }

  private def distributeWidthDelta(
    widths: Vector[Double],
    indices: Vector[Int],
    lowerBounds: Vector[Double],
    upperBounds: Vector[Double],
    delta: Double,
    weight: Int => Double
  ): Vector[Double] = {
    val buffer = widths.toArray
    var remaining = delta
    var active = indices
    var iterations = 0

    while (active.nonEmpty && math.abs(remaining) > 0.5 && iterations < 12) {
      iterations += 1
      val direction = math.signum(remaining)
      val boundedActive = active.filter { index =>
        if (direction > 0) buffer(index) + 0.5 < upperBounds(index)
        else buffer(index) - 0.5 > lowerBounds(index)
      }
      active = boundedActive
      if (active.isEmpty) {
        remaining = 0.0
      } else {
        val totalWeight = active.map(index => math.max(0.0001, weight(index))).sum
        var consumed = 0.0
        active.foreach { index =>
          val share = remaining * math.max(0.0001, weight(index)) / totalWeight
          val updated = math.max(lowerBounds(index), math.min(buffer(index) + share, upperBounds(index)))
          val actual = updated - buffer(index)
          buffer(index) = updated
          consumed += actual
        }
        if (math.abs(consumed) < 0.1) remaining = 0.0
        else remaining -= consumed
      }
    }
    buffer.toVector
  }
}

object TableView {

  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] =
    DslRuntime.build(new TableView[S])(init)

  def items[S](using t: TableView[S]): ListProperty[S] =
    t.items

  def items_=[S](v: scala.collection.IterableOnce[S])(using t: TableView[S]): Unit =
    t.items.setAll(v)

  def rowHeight(using t: TableView[?]): Double = t.rowHeightProperty.get
  def rowHeight_=(v: Double)(using t: TableView[?]): Unit = t.rowHeightProperty.set(v)

  def showHeader(using t: TableView[?]): Boolean = t.showHeaderProperty.get
  def showHeader_=(v: Boolean)(using t: TableView[?]): Unit = t.showHeaderProperty.set(v)

  def prefWidth(using t: TableView[?]): Option[Double] = t.prefWidthProperty.get
  def prefWidth_=(v: Double)(using t: TableView[?]): Unit = t.prefWidthProperty.set(Some(v))
  def prefWidth_=(v: jfx.core.state.ReadOnlyProperty[Double])(using t: TableView[?]): Unit =
    t.addDisposable(v.observe(w => t.prefWidthProperty.set(Some(w))))

  def crawlable(using t: TableView[?]): Boolean = t.crawlableProperty.get
  def crawlable_=(v: Boolean)(using t: TableView[?]): Unit = t.crawlableProperty.set(v)

  def selectedIndex(using t: TableView[?]): Int = t.selectedIndexProperty.get
  def selectedIndex_=(v: Int)(using t: TableView[?]): Unit = t.selectedIndexProperty.set(v)

  def selectedItem[S](using t: TableView[S]): S | Null = t.selectedItemProperty.get

  def placeholder[S](using t: TableView[S]): Component | Null = t.placeholderProperty.get
  def placeholder_=[S](v: Component | Null)(using t: TableView[S]): Unit = t.placeholderProperty.set(v)

  def columns[S](using t: TableView[S]): ListProperty[TableColumn[S, ?]] =
    t.columns
}
