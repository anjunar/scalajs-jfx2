package jfx.control

import jfx.control.Link.link
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{CompositeDisposable, Disposable, ListProperty, Property, ReadOnlyProperty, RemoteListProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.router.RouteContext
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import jfx.control.TableRow.tableRow
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

final class TableView[S] extends Box("div") {

  private given ExecutionContext = ExecutionContext.global

  private val itemsRefProperty = Property[ListProperty[S]](new ListProperty[S]())
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)

  val rowHeightProperty = Property(32.0)
  val prefWidthProperty = Property[Option[Double]](None)

  val scrollTopProperty = Property(0.0)
  val scrollLeftProperty = Property(0.0)
  val viewportWidthProperty = Property(800.0)
  val viewportHeightProperty = Property(400.0)

  val crawlableProperty = Property(false)
  private val defaultLimit = 50

  val selectedIndexProperty = Property(-1)
  val selectedItemProperty = Property[S | Null](null)
  val placeholderProperty = Property[Component | Null](null)

  private case class VisibleRow(index: Int, item: Option[S])
  private val visibleRowsProperty = new ListProperty[VisibleRow]()
  private val itemStateRevisionProperty = Property(0)
  private val remoteStateRevisionProperty = Property(0)

  private var itemsObserver: Disposable = TableView.noopDisposable
  private var remoteItemsObserver: Disposable = TableView.noopDisposable
  private var routeContext: Option[RouteContext] = None

  def itemsProperty: Property[ListProperty[S]] = itemsRefProperty

  def getItems: ListProperty[S] =
    itemsRefProperty.get

  def setItems(value: ListProperty[S]): Unit = {
    val normalized = if (value == null) new ListProperty[S]() else value
    if (!itemsRefProperty.get.eq(normalized)) {
      itemsRefProperty.setAlways(normalized)
    }
  }

  def items: ListProperty[S] =
    getItems

  def items_=(value: ListProperty[S]): Unit =
    setItems(value)

  def getColumns: ListProperty[TableColumn[S, ?]] =
    columns

  def getFixedCellSize: Double =
    rowHeightProperty.get

  def setFixedCellSize(value: Double): Unit =
    rowHeightProperty.set(value)

  private def captureRouteContext(): Unit =
    routeContext = currentRouteContext()

  private def currentRouteContext(): Option[RouteContext] =
    routeContext.orElse {
      try Some(DslRuntime.service[RouteContext])
      catch { case _: Throwable => None }
    }

  private def getCrawlParams: (Int, Int) = {
    val ctx = currentRouteContext()
    val offset = ctx.flatMap(_.queryParams.get("offset")).flatMap(_.toIntOption).getOrElse(0)
    val limit = ctx.flatMap(_.queryParams.get("limit")).flatMap(_.toIntOption).getOrElse(defaultLimit)
    (math.max(0, offset), math.max(1, limit))
  }

  private def currentRemoteItems: RemoteListProperty[S, ?] | Null =
    items.remotePropertyOrNull

  private def totalItemCount: Int =
    math.max(0, items.totalLength)

  private def itemAt(index: Int): Option[S] = {
    val remote = currentRemoteItems
    if (remote == null) {
      if (index >= 0 && index < items.length) Some(items(index)) else None
    } else {
      remote.getLoadedItem(index)
    }
  }

  private def bumpItemState(): Unit =
    itemStateRevisionProperty.set(itemStateRevisionProperty.get + 1)

  private def bumpRemoteState(): Unit =
    remoteStateRevisionProperty.set(remoteStateRevisionProperty.get + 1)

  private def refreshSelectedItem(): Unit = {
    val selectedIndex = selectedIndexProperty.get
    if (selectedIndex >= 0 && selectedIndex < totalItemCount) {
      selectedItemProperty.set(itemAt(selectedIndex).orNull)
    } else {
      selectedItemProperty.set(null)
    }
  }

  private def refreshItemState(): Unit = {
    bumpItemState()
    recomputeVisibleRows()
    refreshSelectedItem()
  }

  private def rewireItemsObserver(): Unit = {
    itemsObserver.dispose()
    remoteItemsObserver.dispose()
    bumpRemoteState()

    val currentItems = items
    itemsObserver = currentItems.observeChanges(_ => refreshItemState())

    val remote = currentRemoteItems
    if (remote == null) {
      remoteItemsObserver = TableView.noopDisposable
    } else {
      val composite = new CompositeDisposable()
      composite.add(remote.loadingProperty.observe { _ =>
        bumpRemoteState()
        refreshItemState()
      })
      composite.add(remote.errorProperty.observe { _ =>
        bumpRemoteState()
        refreshItemState()
      })
      composite.add(remote.sortingProperty.observe { _ =>
        bumpRemoteState()
        refreshItemState()
      })
      composite.add(remote.totalCountProperty.observe(_ => refreshItemState()))
      composite.add(remote.hasMoreProperty.observe(_ => refreshItemState()))
      remoteItemsObserver = composite
    }

    refreshItemState()
  }

  private def visibleRange(total: Int): (Int, Int) =
    if (RenderBackend.current.isServer && crawlableProperty.get) {
      val (offset, limit) = getCrawlParams
      val start = math.min(offset, total)
      val end = math.min(total, start + limit)
      (start, end)
    } else {
      val rowHeight = math.max(1.0, rowHeightProperty.get)
      val firstVisible = math.floor(scrollTopProperty.get / rowHeight).toInt
      val visibleCount = math.ceil(math.max(1.0, viewportHeightProperty.get) / rowHeight).toInt + 1
      val start = math.max(0, firstVisible - TableView.overscanRows)
      val end = math.min(total, firstVisible + visibleCount + TableView.overscanRows)
      (start, end)
    }

  private def recomputeVisibleRows(): Unit = {
    val total = totalItemCount

    if (total == 0) {
      visibleRowsProperty.setAll(Seq.empty)
    } else {
      val (start, end) = visibleRange(total)
      visibleRowsProperty.setAll((start until end).map(index => VisibleRow(index, itemAt(index))))

      if (!RenderBackend.current.isServer) {
        requestLazyLoadIfNecessary(start, end)
      }
    }
  }

  private def requestLazyLoadIfNecessary(visibleStartIndex: Int, visibleEndExclusive: Int): Unit = {
    val remote = currentRemoteItems
    if (remote == null) return
    if (remote.loadingProperty.get) return
    if (remote.errorProperty.get.nonEmpty) return

    if (remote.supportsRangeLoading) {
      if (!remote.isRangeLoaded(visibleStartIndex, visibleEndExclusive)) {
        discardPromise(remote.ensureRangeLoaded(visibleStartIndex, visibleEndExclusive))
      }
      return
    }

    if (!remote.hasMoreProperty.get) return

    val remainingLoadedRows = math.max(0, remote.length - visibleEndExclusive)
    if (remainingLoadedRows <= TableView.lazyLoadThresholdRows) {
      discardPromise(remote.loadMore())
    }
  }

  private def discardPromise(promise: js.Promise[?]): Unit = {
    promise.toFuture.recover { case _ => () }
    ()
  }

  private def contentHeightProperty: ReadOnlyProperty[String] =
    itemStateRevisionProperty.flatMap { _ =>
      rowHeightProperty.map(rowHeight => s"${totalItemCount * rowHeight}px")
    }

  private def hasRowsProperty: ReadOnlyProperty[Boolean] =
    itemStateRevisionProperty.map(_ => totalItemCount > 0)

  private def placeholderTextProperty: ReadOnlyProperty[String] =
    remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      if (remote != null && remote.loadingProperty.get) {
        "Loading table data..."
      } else if (remote != null && remote.errorProperty.get.nonEmpty) {
        remote.errorProperty.get.flatMap(error => Option(error.getMessage)).filter(_.nonEmpty).getOrElse("Could not load table data")
      } else {
        "No content in table"
      }
    }

  val renderedWidthsProperty: ReadOnlyProperty[Vector[Double]] =
    viewportWidthProperty.flatMap { viewportWidth =>
      columns.asProperty.map(cols => resolveRenderedColumnWidths(cols.toSeq, viewportWidth))
    }

  private val totalColumnWidthProperty: ReadOnlyProperty[Double] =
    renderedWidthsProperty.map(_.sum)

  override def compose(): Unit = {
    given Component = this
    captureRouteContext()

    addClass("jfx-table-view")
    classIf("jfx-table-view-loading", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.loadingProperty.get
    })
    classIf("jfx-table-view-error", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.errorProperty.get.nonEmpty
    })

    addDisposable(itemsRefProperty.observe(_ => rewireItemsObserver()))
    addDisposable(() => {
      itemsObserver.dispose()
      remoteItemsObserver.dispose()
    })
    addDisposable(scrollTopProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(viewportWidthProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(columns.observeChanges(_ => recomputeVisibleRows()))
    addDisposable(rowHeightProperty.observe(_ => recomputeVisibleRows()))
    addDisposable(crawlableProperty.observe(_ => refreshItemState()))
    addDisposable(selectedIndexProperty.observe(_ => refreshSelectedItem()))

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
          addClass("jfx-table-header-viewport")
          style {
            position = "relative"
            overflow = "hidden"
            width = "100%"
            flex = "0 0 auto"
            height_=(rowHeightProperty.map(rowHeight => s"${math.max(30.0, rowHeight)}px"))
          }

          div {
            addClass("jfx-table-header-content")
            style {
              display = "flex"
              width_=(totalColumnWidthProperty.map(width => s"${width}px"))
              minWidth_=(totalColumnWidthProperty.map(width => s"${width}px"))
              height = "100%"
              transform_=(scrollLeftProperty.map(left => s"translateX(-${left}px)"))
            }

            forEach(columns) { column =>
              val typedColumn = column.asInstanceOf[TableColumn[S, Any]]
              div {
                addClass("jfx-table-header-cell")
                classIf("jfx-table-header-cell-last", renderedWidthsProperty.map(_ => columns.indexOf(column) == columns.length - 1))
                classIf("jfx-table-header-cell-sortable", remoteStateRevisionProperty.map(_ => isRemoteSortable(typedColumn)))
                classIf("jfx-table-header-cell-sorted", remoteStateRevisionProperty.map(_ => currentSortFor(typedColumn).nonEmpty))
                classIf("jfx-table-header-cell-sorted-asc", remoteStateRevisionProperty.map(_ => currentSortFor(typedColumn).exists(_.ascending)))
                classIf("jfx-table-header-cell-sorted-desc", remoteStateRevisionProperty.map(_ => currentSortFor(typedColumn).exists(!_.ascending)))
                style {
                  width_=(renderedWidthsProperty.map(widths => s"${widths.lift(columns.indexOf(column)).getOrElse(typedColumn.prefWidth)}px"))
                  minWidth_=(renderedWidthsProperty.map(widths => s"${widths.lift(columns.indexOf(column)).getOrElse(typedColumn.prefWidth)}px"))
                  flex = "0 0 auto"
                  boxSizing = "border-box"
                }
                onClick(_ => toggleRemoteSort(typedColumn))
                text = column.textProperty
              }
            }
          }
        }
      }
    }

    div {
      addClass("jfx-table-body-wrapper")
      style {
        position = "relative"
        flex = "1 1 auto"
        overflow = "hidden"
        width = "100%"
      }

      div {
        addClass("jfx-table-viewport")
        style {
          position = "relative"
          overflow = "auto"
          width = "100%"
          height = "100%"
        }

        onScroll { event =>
          val target = event.target.asInstanceOf[dom.html.Div]
          scrollTopProperty.set(target.scrollTop)
          scrollLeftProperty.set(target.scrollLeft)
          viewportHeightProperty.set(target.clientHeight.toDouble)
          viewportWidthProperty.set(target.clientWidth.toDouble)
        }

        div {
          addClass("jfx-table-content")
          style {
            position = "relative"
            width_=(totalColumnWidthProperty.map(width => s"${width}px"))
            minWidth_=(totalColumnWidthProperty.map(width => s"${width}px"))
            if (!RenderBackend.current.isServer) {
              height_=(contentHeightProperty)
            }
          }

          condition(hasRowsProperty) {
            thenDo {
              forEach(visibleRowsProperty) { rowDef =>
                div {
                  addClass("jfx-table-row-slot")
                  style {
                    position = "absolute"
                    top = s"${rowDef.index * rowHeightProperty.get}px"
                    left = "0"
                    width_=(totalColumnWidthProperty.map(width => s"${width}px"))
                    height = s"${rowHeightProperty.get}px"
                    display = "flex"
                  }

                  tableRow[S] {
                    val row = summon[TableRow[S]]
                    rowDef.item match {
                      case Some(value) =>
                        row.bind(rowDef.index, value, TableView.this, columns.get.toSeq, rowHeightProperty.get)
                      case None =>
                        row.bindPlaceholder(rowDef.index, TableView.this, columns.get.toSeq, rowHeightProperty.get)
                    }
                  }
                }
              }
            }
          }
        }

        condition(itemStateRevisionProperty.map(_ => hasMoreCrawlPage)) {
          thenDo {
            val (offset, limit) = getCrawlParams
            link(nextCrawlHref(offset, limit)) {
              addClass("jfx-table-more-link")
              style {
                display = "block"
                padding = "20px"
                textAlign = "center"
                if (RenderBackend.current.isServer) {
                  marginTop = s"${(offset + limit) * rowHeightProperty.get}px"
                }
              }
              text = "More items..."
            }
          }
        }
      }

      condition(itemStateRevisionProperty.map(_ => totalItemCount == 0)) {
        thenDo {
          div {
            addClass("jfx-table-placeholder")
            style {
              display = "flex"
            }

            div {
              addClass("jfx-table-default-placeholder")
              text = placeholderTextProperty
            }
          }
        }
      }
    }
  }

  private def hasMoreCrawlPage: Boolean = {
    val (offset, limit) = getCrawlParams
    crawlableProperty.get && offset + limit < totalItemCount
  }

  private def nextCrawlHref(offset: Int, limit: Int): String = {
    val next = s"offset=${offset + limit}&limit=$limit"
    currentRouteContext().map(_.path).filter(path => path.nonEmpty && path != "/") match {
      case Some(path) => s"$path?$next"
      case None       => s"?$next"
    }
  }

  private def sortKeyOf(column: TableColumn[S, Any]): Option[String] =
    column.sortKeyProperty.get.map(_.trim).filter(_.nonEmpty)

  private def currentRemoteSorting: Vector[ListProperty.RemoteSort] = {
    val remote = currentRemoteItems
    if (remote == null) Vector.empty else remote.getSorting
  }

  private def currentSortFor(column: TableColumn[S, Any]): Option[ListProperty.RemoteSort] =
    sortKeyOf(column).flatMap(key => currentRemoteSorting.find(_.field == key))

  private def isRemoteSortable(column: TableColumn[S, Any]): Boolean = {
    val remote = currentRemoteItems
    remote != null && remote.supportsSorting && column.sortableProperty.get && sortKeyOf(column).nonEmpty
  }

  private def toggleRemoteSort(column: TableColumn[S, Any]): Unit = {
    val remote = currentRemoteItems
    val sortKey = sortKeyOf(column)

    if (remote == null || !remote.supportsSorting || sortKey.isEmpty) return

    val nextSorting =
      currentSortFor(column) match {
        case Some(sort) if sort.ascending =>
          Vector(ListProperty.RemoteSort(sort.field, ascending = false))
        case Some(_) =>
          Vector.empty
        case None =>
          Vector(ListProperty.RemoteSort(sortKey.get, ascending = true))
      }

    discardPromise(remote.applySorting(nextSorting))
  }

  def select(index: Int): Unit = {
    if (index >= 0 && index < totalItemCount) {
      selectedIndexProperty.set(index)
    } else {
      selectedIndexProperty.set(-1)
    }
  }

  def select(item: S): Unit = {
    val remote = currentRemoteItems
    val index =
      if (remote == null) {
        items.toSeq.indexOf(item)
      } else {
        (0 until remote.totalLength).find(index => remote.getLoadedItem(index).contains(item)).getOrElse(-1)
      }
    select(index)
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
  private[control] val overscanRows = 6
  private[control] val lazyLoadThresholdRows = 3
  private[control] val noopDisposable: Disposable = () => ()

  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] =
    DslRuntime.build(new TableView[S])(init)

  def items[S](using t: TableView[S]): ListProperty[S] =
    t.items

  def items_=[S](v: ListProperty[S])(using t: TableView[S]): Unit =
    t.setItems(v)

  def items_=[S](v: scala.collection.IterableOnce[S])(using t: TableView[S]): Unit =
    v match {
      case property: ListProperty[?] =>
        t.setItems(property.asInstanceOf[ListProperty[S]])
      case _ =>
        t.items.setAll(v)
    }

  def rowHeight(using t: TableView[?]): Double = t.rowHeightProperty.get
  def rowHeight_=(v: Double)(using t: TableView[?]): Unit = t.rowHeightProperty.set(v)

  def fixedCellSize(using t: TableView[?]): Double = t.rowHeightProperty.get
  def fixedCellSize_=(v: Double)(using t: TableView[?]): Unit = t.rowHeightProperty.set(v)

  def showHeader(using t: TableView[?]): Boolean = t.showHeaderProperty.get
  def showHeader_=(v: Boolean)(using t: TableView[?]): Unit = t.showHeaderProperty.set(v)

  def prefWidth(using t: TableView[?]): Option[Double] = t.prefWidthProperty.get
  def prefWidth_=(v: Double)(using t: TableView[?]): Unit = t.prefWidthProperty.set(Some(v))
  def prefWidth_=(v: ReadOnlyProperty[Double])(using t: TableView[?]): Unit =
    t.addDisposable(v.observe(width => t.prefWidthProperty.set(Some(width))))

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
