package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.statement.ForEach.forEach
import org.scalajs.dom
import org.scalajs.dom.Event

final class TableView[S] extends Box("div") {

  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  val rowHeightProperty = Property(32.0)
  val prefWidthProperty = Property[Option[Double]](None)
  val scrollTopProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  private case class VisibleRow(index: Int, item: S, epoch: Long)

  override def compose(): Unit = {
    given Component = this

    val scrollLeftProperty = Property(0.0)
    val renderEpochProperty = Property(0L)
    val visibleRows = new ListProperty[VisibleRow]()

    def totalColumnWidth: Double =
      columns.get.toSeq.foldLeft(0.0)((sum, col) => sum + col.prefWidth)

    def bumpEpoch(): Unit =
      renderEpochProperty.set(renderEpochProperty.get + 1)

    def recomputeVisibleRows(): Unit = {
      val total = items.length
      val rowHeight = math.max(1.0, rowHeightProperty.get)
      val viewportHeight = math.max(0.0, viewportHeightProperty.get)

      if (total == 0) {
        visibleRows.setAll(Seq.empty)
      } else {
        val overscan = 4
        val firstVisible = math.floor(scrollTopProperty.get / rowHeight).toInt
        val visibleCount = math.ceil(viewportHeight / rowHeight).toInt + 1

        val start = math.max(0, firstVisible - overscan)
        val end = math.min(total, firstVisible + visibleCount + overscan)

        visibleRows.setAll(
          (start until end).map(i => VisibleRow(i, items(i), renderEpochProperty.get))
        )
      }
    }

    addClass("jfx-table-view")
    style {
      display = "flex"
      flexDirection = "column"
      width = "100%"
      prefWidthProperty.get.foreach(w => width = s"${w}px")
      height = "100%"
      overflow = "hidden"
    }

    addDisposable(scrollTopProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(items.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(columns.observeChanges(_ => {
      bumpEpoch()
      recomputeVisibleRows()
    }))
    addDisposable(rowHeightProperty.observe(_ => {
      bumpEpoch()
      recomputeVisibleRows()
    }))

    jfx.statement.Condition.condition(showHeaderProperty) {
      jfx.statement.Condition.thenDo {
        Box.box("div") {
          addClass("jfx-table-header")
          style {
            position = "relative"
            overflow = "hidden"
            width = "100%"
            flex = "0 0 auto"
          }

          Box.box("div") {
            addClass("jfx-table-header-content")

            def syncHeaderLayout(): Unit = {
              val columnWidth = totalColumnWidth
              style {
                display = "flex"
                width = s"${columnWidth}px"
                minWidth = s"${columnWidth}px"
                transform = s"translateX(-${scrollLeftProperty.get}px)"
              }
            }

            syncHeaderLayout()

            addDisposable(scrollLeftProperty.observe(_ => syncHeaderLayout()))
            addDisposable(columns.observeChanges(_ => syncHeaderLayout()))

            forEach(columns) { col =>
              Box.box("div") {
                addClass("jfx-table-header-cell")
                style {
                  width = s"${col.prefWidth}px"
                  minWidth = s"${col.prefWidth}px"
                  maxWidth = s"${col.prefWidth}px"
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

    Box.box("div") {
      addClass("jfx-table-viewport")
      style {
        flex = "1 1 auto"
        position = "relative"
        overflow = "auto"
        width = "100%"
      }

      def syncViewportMetrics(): Unit =
        viewportHeightProperty.set(math.max(0.0, host.clientHeight.toDouble))

      if (! RenderBackend.current.isServer) {
        val listener: Event => Unit = (_: Event) => {
          syncViewportMetrics()
        }

        dom.window.requestAnimationFrame((_: Double) => syncViewportMetrics())

        dom.window.addEventListener("resize", listener)

        addDisposable(() => {
          dom.window.removeEventListener("resize", listener)
        })
      }

      addDisposable(summon[Box].host.addEventListener("scroll", (e: dom.Event) => {
        val target = e.currentTarget.asInstanceOf[dom.html.Div]
        scrollTopProperty.set(target.scrollTop)
        scrollLeftProperty.set(target.scrollLeft)
        viewportHeightProperty.set(target.clientHeight.toDouble)
      }))

      Box.box("div") {
        addClass("jfx-table-content")

        def syncContentLayout(): Unit = {
          val columnWidth = totalColumnWidth
          style {
            position = "relative"
            height = s"${items.length * rowHeightProperty.get}px"
            width = s"${columnWidth}px"
            minWidth = s"${columnWidth}px"
          }
        }

        syncContentLayout()

        addDisposable(items.observeChanges(_ => syncContentLayout()))
        addDisposable(columns.observeChanges(_ => syncContentLayout()))
        addDisposable(rowHeightProperty.observe(_ => syncContentLayout()))

        forEach(visibleRows) { rowDef =>
          Box.box("div") {
            addClass("jfx-table-row-slot")
            style {
              position = "absolute"
              top = s"${rowDef.index * rowHeightProperty.get}px"
              left = "0"
              width = s"${totalColumnWidth}px"
              height = s"${rowHeightProperty.get}px"
              display = "flex"
            }

            val row = new TableRow[S]()
            DslRuntime.build(row) {
              row.bind(
                rowDef.index,
                rowDef.item,
                TableView.this,
                columns.get.toSeq,
                rowHeightProperty.get
              )
            }
          }
        }
      }
    }

    recomputeVisibleRows()
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