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

final class TableView[S] extends Box("div") {

  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  val rowHeightProperty = Property(32.0)
  val prefWidthProperty = Property[Option[Double]](None)
  
  val scrollTopProperty = Property(0.0)
  val scrollLeftProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  val crawlableProperty = Property(false)
  private val defaultLimit = 50

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
            val (offset, _) = getCrawlParams
            if (offset > 0) {
               val target = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[dom.html.Div]
               target.scrollTop = offset * rowHeightProperty.get
            }
            recomputeVisibleRows()
         }
      }

      div {
        addClass("jfx-table-content")
        style {
          position = "relative"
          // Wir lassen die Höhe im SSR Modus weg, damit der Link nachrücken kann
          if (!RenderBackend.current.isServer) {
            height_=(items.asProperty.map(it => s"${it.length * rowHeightProperty.get}px"))
          }
          width_=(totalColumnWidthProperty.map(w => s"${w}px"))
        }

        forEach(visibleRowsProperty) { rowDef =>
          div {
            addClass("jfx-table-row-slot")
            style {
              // Immer absolute für Hydrierung, aber im SSR mit festen top-Werten
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

      val hasMoreProperty = items.asProperty.map { itms =>
         val (offset, limit) = getCrawlParams
         crawlableProperty.get && offset + limit < itms.length
      }

      condition(hasMoreProperty) {
        thenDo {
           // Wir entfernen das div um den Link, um Hydration-Probleme zu vermeiden
           val (offset, limit) = getCrawlParams
           val currentPath = getRouteContext.map(_.path).getOrElse("")
           link(s"$currentPath?offset=${offset + limit}&limit=$limit") {
             style { 
               display = "block"
               padding = "20px"
               textAlign = "center"
               // Im SSR Modus schieben wir den Link physisch unter die absoluten Zeilen
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

  def columns[S](using t: TableView[S]): ListProperty[TableColumn[S, ?]] =
    t.columns
}
