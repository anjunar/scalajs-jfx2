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
import jfx.statement.ObserveRender.observeRender
import org.scalajs.dom

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

final class DataGrid[T] extends Box("div") {

  private given ExecutionContext = ExecutionContext.global

  private val itemsRefProperty = Property[ListProperty[T]](normalizeItems(null))

  val $itemWidthProperty = Property(320.0)
  val $itemHeightProperty = Property(220.0)
  val $gapProperty = Property(16.0)
  val $overscanRowsProperty = Property(2)
  val $prefetchItemsProperty = Property(40)

  val $scrollTopProperty = Property(0.0)
  val $viewportWidthProperty = Property(800.0)
  val $viewportHeightProperty = Property(400.0)

  val $crawlableProperty = Property(false)
  private val defaultLimit = 50
  private val headerFactoryProperty = Property[() => Component | Null](() => null)
  private val headerRevisionProperty = Property(0)
  private val headerHeightProperty = Property(0.0)

  private val visibleCellsProperty = new ListProperty[DataGrid.VisibleCell[T]]()
  private val itemStateRevisionProperty = Property(0)
  private val remoteStateRevisionProperty = Property(0)
  private val placeholderRevisionProperty = Property(0)

  private val pendingRangeLoads = mutable.Set.empty[(Int, Int)]

  private var lastVisibleCells = Vector.empty[DataGrid.VisibleCell[T]]
  private var itemRenderer: DataGrid.Renderer[T] = normalizeRenderer((_: T | Null, _: Int) => ())
  private var itemsObserver: Disposable = DataGrid.noopDisposable
  private var remoteItemsObserver: Disposable = DataGrid.noopDisposable
  private var viewportMeasureScheduled = false
  private var routeContext: Option[RouteContext] = None
  private var initialScrollIndex = -1
  private val loadingPlaceholderFactoryProperty = Property[() => Component | Null](() => null)
  private val emptyPlaceholderFactoryProperty = Property[() => Component | Null](() => null)

  def $itemsProperty: Property[ListProperty[T]] =
    itemsRefProperty

  def $getItems: ListProperty[T] =
    itemsRefProperty.get

  def setItems(value: ListProperty[T] | Null): Unit = {
    val normalized = normalizeItems(value)
    if (!itemsRefProperty.get.eq(normalized)) {
      itemsRefProperty.setAlways(normalized)
    }
  }

  def $items: ListProperty[T] =
    $getItems

  def $items_=(value: ListProperty[T]): Unit =
    setItems(value)

  def setRenderer(renderer: DataGrid.Renderer[T]): Unit =
    itemRenderer = normalizeRenderer(renderer)

  def getRenderer: DataGrid.Renderer[T] =
    itemRenderer

  def setLoadingPlaceholderFactory(factory: () => Component | Null): Unit =
    loadingPlaceholderFactoryProperty.set(if (factory == null) (() => null) else factory)

  def setEmptyPlaceholderFactory(factory: () => Component | Null): Unit =
    emptyPlaceholderFactoryProperty.set(if (factory == null) (() => null) else factory)

  def setHeaderFactory(factory: () => Component | Null): Unit =
    headerFactoryProperty.set(if (factory == null) (() => null) else factory)

  def refresh(): Unit =
    refreshItemState()

  def scrollTo(index: Int): Unit = {
    val total = totalItemCount
    if (total <= 0) return

    val clamped = math.max(0, math.min(total - 1, index))
    val nextScrollTop = topForIndex(clamped)
    $scrollTopProperty.set(nextScrollTop)
    host.domNode.collect { case element: dom.html.Element =>
      element.querySelector(".jfx-data-grid-viewport") match {
        case viewport: dom.html.Element => viewport.scrollTop = nextScrollTop
        case _                          =>
      }
    }
  }

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

  override def compose(): Unit = {
    given Component = this
    captureRouteContext()

    addClass("jfx-data-grid")
    classIf("jfx-data-grid-loading", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.loadingProperty.get
    })
    classIf("jfx-data-grid-error", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.errorProperty.get.nonEmpty
    })

    addDisposable(itemsRefProperty.observe(_ => rewireItemsObserver()))
    addDisposable(() => {
      itemsObserver.dispose()
      remoteItemsObserver.dispose()
    })
    addDisposable($scrollTopProperty.observe(_ => recomputeVisibleCells()))
    addDisposable($viewportWidthProperty.observe(_ => refreshItemState()))
    addDisposable($viewportHeightProperty.observe(_ => recomputeVisibleCells()))
    addDisposable($itemWidthProperty.observe(_ => refreshItemState()))
    addDisposable($itemHeightProperty.observe(_ => refreshItemState()))
    addDisposable($gapProperty.observe(_ => refreshItemState()))
    addDisposable($overscanRowsProperty.observe(_ => recomputeVisibleCells()))
    addDisposable($prefetchItemsProperty.observe(_ => recomputeVisibleCells()))
    addDisposable($crawlableProperty.observe(_ => refreshItemState()))
    addDisposable(loadingPlaceholderFactoryProperty.observe(_ => bumpPlaceholderState()))
    addDisposable(emptyPlaceholderFactoryProperty.observe(_ => bumpPlaceholderState()))
    addDisposable(headerFactoryProperty.observe(_ => bumpHeaderRevision()))
    addDisposable(headerHeightProperty.observe(_ => refreshItemState()))

    if (!RenderBackend.current.isServer) {
      val (offset, _) = getCrawlParams
      if (offset > 0) {
        initialScrollIndex = offset
        $scrollTopProperty.set(topForIndex(offset))
      }
    }

    style {
      display = "block"
      width = "100%"
      height = "100%"
      overflow = "hidden"
      position = "relative"
    }

    div {
      addClass("jfx-data-grid-viewport")
      style {
        width = "100%"
        height = "100%"
        overflow = "auto"
        position = "relative"
      }

      onScroll { event =>
        val target = event.target.asInstanceOf[dom.html.Element]
        val nextScrollTop = target.scrollTop
        val nextViewportWidth = target.clientWidth.toDouble
        val nextViewportHeight = target.clientHeight.toDouble

        if (math.abs($scrollTopProperty.get - nextScrollTop) > 0.5) {
          $scrollTopProperty.set(nextScrollTop)
        }

        if (nextViewportWidth > 0.0 && math.abs($viewportWidthProperty.get - nextViewportWidth) > 0.5) {
          $viewportWidthProperty.set(nextViewportWidth)
        }

        if (nextViewportHeight > 0.0 && math.abs($viewportHeightProperty.get - nextViewportHeight) > 0.5) {
          $viewportHeightProperty.set(nextViewportHeight)
        }
      }

      div {
        addClass("jfx-data-grid-content")
        style {
          width_=(itemStateRevisionProperty.map(_ => s"${contentWidth}px"))
          minWidth = "100%"
          padding_=($gapProperty.map(value => s"${math.max(0.0, value)}px"))
          boxSizing = "border-box"
        }

        div {
          addClass("jfx-data-grid-header-slot")
          style {
            width = "100%"
            boxSizing = "border-box"
            marginBottom_=($gapProperty.map(value => s"${math.max(0.0, value)}px"))
          }

          observeRender(headerRevisionProperty) { _ =>
            renderHeader()
          }
        }

        div {
          addClass("jfx-data-grid-items-surface")
          style {
            position = "relative"
            width_=(itemStateRevisionProperty.map(_ => s"${surfaceWidth}px"))
            minWidth = "100%"
            height_=(itemStateRevisionProperty.map(_ => s"${contentHeight}px"))
          }

          forEach(visibleCellsProperty) { cell =>
            DataGrid.dataGridCell(cell, itemRenderer)
          }
        }
      }

      condition(itemStateRevisionProperty.map(_ => hasMoreCrawlPage)) {
        thenDo {
          val (offset, limit) = getCrawlParams
          link(nextCrawlHref(offset, limit)) {
            addClass("jfx-data-grid-more-link")
            style {
              display = "block"
              padding = "20px"
              textAlign = "center"
              if (RenderBackend.current.isServer) {
                marginTop = s"${topForIndex(offset + limit)}px"
              }
            }
            text = "More items..."
          }
        }
      }

      condition(itemStateRevisionProperty.map(_ => totalItemCount == 0)) {
        thenDo {
          div {
            addClass("jfx-data-grid-placeholder")
            style {
              display = "flex"
            }

            observeRender(placeholderRevisionProperty) { _ =>
              renderPlaceholder(placeholderState)
            }
          }
        }
      }
    }
  }

  override def afterCompose(): Unit = {
    if (!RenderBackend.current.isServer) {
      scheduleViewportMeasure()
      observeHeaderHeight()

      host.domNode.collect { case element: dom.html.Element =>
        val observedElement =
          element.querySelector(".jfx-data-grid-viewport") match {
            case viewport: dom.html.Element => viewport
            case _                          => element
          }

        val resizeObserver = new dom.ResizeObserver((_, _) => scheduleViewportMeasure())
        resizeObserver.observe(observedElement)
        addDisposable(() => resizeObserver.disconnect())
      }

      val listener: dom.Event => Unit = _ => scheduleViewportMeasure()
      dom.window.addEventListener("resize", listener)
      addDisposable(() => dom.window.removeEventListener("resize", listener))
    }
  }

  private def rewireItemsObserver(): Unit = {
    itemsObserver.dispose()
    remoteItemsObserver.dispose()
    pendingRangeLoads.clear()
    bumpRemoteState()

    val currentItems = $items
    val remote = currentRemoteItems

    itemsObserver =
      currentItems.observeChanges { _ =>
        if (remote != null) bumpRemoteState()
        refreshItemState()
      }

    remoteItemsObserver =
      if (remote == null) {
        DataGrid.noopDisposable
      } else {
        val composite = new CompositeDisposable()
        composite.add(remote.loadingProperty.observe { _ =>
          bumpRemoteState()
          recomputeVisibleCells()
        })
        composite.add(remote.errorProperty.observe { _ =>
          bumpRemoteState()
          recomputeVisibleCells()
        })
        composite.add(remote.totalCountProperty.observe(_ => refreshItemState()))
        composite.add(remote.hasMoreProperty.observe(_ => refreshItemState()))
        composite.add(remote.nextQueryProperty.observe(_ => refreshItemState()))
        composite.add(remote.queryProperty.observeWithoutInitial(_ => refreshItemState()))
        composite.add(remote.sortingProperty.observeWithoutInitial(_ => refreshItemState()))

        if (!RenderBackend.current.isServer && remote.length == 0 && !remote.loadingProperty.get && remote.errorProperty.get.isEmpty) {
          discardPromise(remote.reload())
        }

        composite
      }

    refreshItemState()
  }

  private def refreshItemState(): Unit = {
    bumpItemState()
    recomputeVisibleCells()
  }

  private def recomputeVisibleCells(): Unit = {
    val total = totalItemCount
    if (total <= 0) {
      publishVisibleCells(Seq.empty)
      return
    }

    val (start, end) = visibleRange(total)
    val cells = (start until end).map(cellFor)
    publishVisibleCells(cells)

    if (!RenderBackend.current.isServer && end > start) {
      requestLazyLoadIfNecessary(start, end)
    }
  }

  private def publishVisibleCells(cells: Seq[DataGrid.VisibleCell[T]]): Unit = {
    val next = cells.toVector
    if (next != lastVisibleCells) {
      lastVisibleCells = next
      visibleCellsProperty.setAll(next)
    }
  }

  private def visibleRange(total: Int): (Int, Int) =
    if (RenderBackend.current.isServer && $crawlableProperty.get) {
      val (offset, limit) = getCrawlParams
      val start = math.min(offset, total)
      val end = math.min(total, start + limit)
      (start, end)
    } else {
      val columns = columnCount
      val rows = rowCountFor(total, columns)
      val effectiveScrollTop = math.max(0.0, $scrollTopProperty.get - contentTopOffset)
      val firstVisibleRow = math.floor(effectiveScrollTop / rowStep).toInt
      val visibleRows = math.ceil(math.max(1.0, $viewportHeightProperty.get) / rowStep).toInt + 1
      val overscan = math.max(0, $overscanRowsProperty.get)
      val startRow = math.max(0, firstVisibleRow - overscan)
      val endRow = math.min(rows, firstVisibleRow + visibleRows + overscan)
      val start = math.min(total, startRow * columns)
      val end = math.min(total, endRow * columns)
      (start, end)
    }

  private def cellFor(index: Int): DataGrid.VisibleCell[T] = {
    val columns = columnCount
    val row = index / columns
    val column = index % columns
    val item = itemAt(index)

    DataGrid.VisibleCell(
      index = index,
      item = item,
      loaded = item != null,
      top = row * rowStep,
      left = outerGap + column * columnStep,
      width = renderedItemWidth,
      height = itemHeight
    )
  }

  private def requestLazyLoadIfNecessary(visibleStartIndex: Int, visibleEndExclusive: Int): Unit = {
    val remote = currentRemoteItems
    if (remote == null) return
    if (remote.loadingProperty.get) return
    if (remote.errorProperty.get.nonEmpty) return

    val prefetch = math.max(1, $prefetchItemsProperty.get)

    if (remote.supportsRangeLoading) {
      val total = totalItemCount
      val requestFrom = math.max(0, visibleStartIndex - prefetch)
      val requestedTo = visibleEndExclusive + prefetch
      val requestTo = if (total > 0) math.min(total, requestedTo) else requestedTo
      val pageSize = math.max(prefetch, math.max(1, visibleEndExclusive - visibleStartIndex))
      val pageFrom = requestFrom / pageSize * pageSize
      val pageTo = math.max(pageFrom + 1, ((requestTo + pageSize - 1) / pageSize) * pageSize)
      val cappedPageTo = if (total > 0) math.min(total, pageTo) else pageTo
      val key = (pageFrom, cappedPageTo)

      if (!remote.isRangeLoaded(pageFrom, cappedPageTo) && !pendingRangeLoads.contains(key)) {
        pendingRangeLoads += key
        remote.ensureRangeLoaded(pageFrom, cappedPageTo).toFuture.onComplete { _ =>
          pendingRangeLoads -= key
        }
      }
    } else {
      val canLoadMore = remote.hasMoreProperty.get || remote.nextQueryProperty.get.nonEmpty
      if (!canLoadMore) return

      val threshold = math.max(1, prefetch / 2)
      if (remote.length == 0) {
        discardPromise(remote.reload())
      } else if (visibleEndExclusive >= math.max(0, remote.length - threshold)) {
        discardPromise(remote.loadMore())
      }
    }
  }

  private def currentRemoteItems: RemoteListProperty[T, ?] | Null =
    $items.remotePropertyOrNull

  private def itemAt(index: Int): T | Null =
    currentRemoteItems match {
      case null =>
        if (index >= 0 && index < $items.length) $items(index)
        else null
      case remote =>
        remote.getLoadedItem(index).orNull
    }

  private def totalItemCount: Int =
    math.max(0, $items.totalLength)

  private def columnCount: Int =
    columnsFor($viewportWidthProperty.get)

  private def columnsFor(width: Double): Int = {
    val effectiveWidth = math.max(1.0, width)
    val available = math.max(1.0, effectiveWidth - gap)
    math.max(1, math.floor(available / preferredColumnStep).toInt)
  }

  private def rowCount: Int =
    rowCountFor(totalItemCount, columnCount)

  private def rowCountFor(total: Int, columns: Int): Int =
    if (total <= 0) 0 else math.ceil(total.toDouble / math.max(1, columns)).toInt

  private def topForIndex(index: Int): Double = {
    val columns = columnCount
    val row = math.max(0, index) / math.max(1, columns)
    contentTopOffset + row * rowStep
  }

  private def contentWidth: Double = {
    val columns = columnCount
    if (columns <= 0) 0.0
    else columns * renderedItemWidth + math.max(0, columns + 1) * gap
  }

  private def surfaceWidth: Double = {
    val columns = columnCount
    if (columns <= 0) 0.0
    else columns * renderedItemWidth + math.max(0, columns - 1) * gap
  }

  private def contentHeight: Double = {
    val rows = rowCount
    if (rows <= 0) 0.0
    else rows * itemHeight + math.max(0, rows - 1) * gap
  }

  private def hasMoreCrawlPage: Boolean = {
    val (offset, limit) = getCrawlParams
    $crawlableProperty.get && offset + limit < totalItemCount
  }

  private def nextCrawlHref(offset: Int, limit: Int): String = {
    val next = s"offset=${offset + limit}&limit=$limit"
    currentRouteContext().map(_.path).filter(path => path.nonEmpty && path != "/") match {
      case Some(path) => s"$path?$next"
      case None       => s"?$next"
    }
  }

  private def placeholderText: String = {
    val remote = currentRemoteItems
    if (remote != null && remote.loadingProperty.get) {
      "Loading grid data..."
    } else if (remote != null && remote.errorProperty.get.nonEmpty) {
      remote.errorProperty.get.flatMap(error => Option(error.getMessage)).filter(_.nonEmpty).getOrElse("Could not load grid data")
    } else {
      "No content in grid"
    }
  }

  private def placeholderTextProperty: ReadOnlyProperty[String] =
    remoteStateRevisionProperty.map(_ => placeholderText)

  private def placeholderState: DataGrid.PlaceholderState = {
    val remote = currentRemoteItems
    if (remote != null && remote.loadingProperty.get) {
      DataGrid.PlaceholderState.Loading
    } else if (remote != null && remote.errorProperty.get.nonEmpty) {
      DataGrid.PlaceholderState.Error
    } else {
      DataGrid.PlaceholderState.Empty
    }
  }

  private def renderPlaceholder(state: DataGrid.PlaceholderState)(using Component): Unit =
    state match {
      case DataGrid.PlaceholderState.Loading =>
        renderPlaceholderFactory(loadingPlaceholderFactoryProperty.get, placeholderTextProperty.get)
      case DataGrid.PlaceholderState.Empty =>
        renderPlaceholderFactory(emptyPlaceholderFactoryProperty.get, placeholderTextProperty.get)
      case DataGrid.PlaceholderState.Error =>
        renderDefaultPlaceholder(placeholderTextProperty.get)
    }

  private def renderPlaceholderFactory(factory: () => Component | Null, fallbackText: String)(using Component): Unit = {
    val rendered =
      Option(factory)
        .map(_())
        .orNull

    if (rendered == null) {
      renderDefaultPlaceholder(fallbackText)
    }
  }

  private def renderDefaultPlaceholder(message: String)(using Component): Unit =
    div {
      addClass("jfx-data-grid-default-placeholder")
      text = message
    }

  private def scheduleViewportMeasure(): Unit = {
    if (viewportMeasureScheduled || RenderBackend.current.isServer) return

    viewportMeasureScheduled = true
    dom.window.requestAnimationFrame { _ =>
      viewportMeasureScheduled = false
      measureViewport()
    }
  }

  private def measureViewport(): Unit =
    host.domNode.collect { case element: dom.html.Element =>
      element.querySelector(".jfx-data-grid-viewport") match {
        case viewport: dom.html.Element =>
          updateViewportSize(viewport)
          applyInitialScrollPosition(viewport)
        case _ =>
          updateViewportSize(element)
      }
    }

  private def observeHeaderHeight(): Unit =
    host.domNode.collect { case element: dom.html.Element =>
      element.querySelector(".jfx-data-grid-header-slot") match {
        case header: dom.html.Element =>
          val measure = () => {
            val next = math.max(0.0, header.offsetHeight.toDouble)
            if (math.abs(headerHeightProperty.get - next) > 0.5) {
              headerHeightProperty.set(next)
            }
          }

          dom.window.requestAnimationFrame(_ => measure())

          val resizeObserver = new dom.ResizeObserver((_, _) => measure())
          resizeObserver.observe(header)
          addDisposable(() => resizeObserver.disconnect())
        case _ =>
      }
    }

  private def updateViewportSize(element: dom.html.Element): Unit = {
    val width = element.clientWidth.toDouble
    val height = element.clientHeight.toDouble

    if (width > 0.0) {
      $viewportWidthProperty.set(width)
    }

    if (height > 0.0) {
      $viewportHeightProperty.set(height)
    }
  }

  private def applyInitialScrollPosition(viewport: dom.html.Element): Unit =
    if (initialScrollIndex > 0) {
      val nextScrollTop = topForIndex(initialScrollIndex)
      viewport.scrollTop = nextScrollTop
      $scrollTopProperty.set(nextScrollTop)
      initialScrollIndex = -1
    }

  private def bumpItemState(): Unit =
    itemStateRevisionProperty.set(itemStateRevisionProperty.get + 1)

  private def bumpRemoteState(): Unit =
    remoteStateRevisionProperty.set(remoteStateRevisionProperty.get + 1)
    bumpPlaceholderState()

  private def bumpPlaceholderState(): Unit =
    placeholderRevisionProperty.set(placeholderRevisionProperty.get + 1)

  private def bumpHeaderRevision(): Unit =
    headerRevisionProperty.set(headerRevisionProperty.get + 1)

  private def discardPromise(promise: js.Promise[?]): Unit = {
    promise.toFuture.recover { case _ => () }
    ()
  }

  private def itemWidth: Double =
    math.max(1.0, $itemWidthProperty.get)

  private def headerHeight: Double =
    math.max(0.0, headerHeightProperty.get)

  private def contentTopOffset: Double =
    outerGap + headerHeight + (if (headerHeight > 0.5) gap else 0.0)

  private def renderedItemWidth: Double = {
    val columns = math.max(1, columnCount)
    val totalGap = math.max(0, columns + 1) * gap
    val available = math.max(1.0, $viewportWidthProperty.get - totalGap)
    math.max(1.0, available / columns)
  }

  private def itemHeight: Double =
    math.max(1.0, $itemHeightProperty.get)

  private def gap: Double =
    math.max(0.0, $gapProperty.get)

  private def outerGap: Double =
    gap

  private def columnStep: Double =
    renderedItemWidth + gap

  private def preferredColumnStep: Double =
    itemWidth + gap

  private def rowStep: Double =
    itemHeight + gap

  private def normalizeItems(value: ListProperty[T] | Null): ListProperty[T] =
    if (value == null) new ListProperty[T]() else value

  private def normalizeRenderer(renderer: DataGrid.Renderer[T]): DataGrid.Renderer[T] =
    if (renderer == null) ((_: T | Null, _: Int) => ()) else renderer

  private def renderHeader()(using Component): Unit = {
    val factory = headerFactoryProperty.get
    if (factory != null) {
      factory()
      ()
    }
  }
}

object DataGrid {
  private[control] val noopDisposable: Disposable = () => ()

  type Renderer[T] = (T | Null, Int) => Unit

  enum PlaceholderState {
    case Loading
    case Empty
    case Error
  }

  private[control] final case class VisibleCell[T](
                                                    index: Int,
                                                    item: T | Null,
                                                    loaded: Boolean,
                                                    top: Double,
                                                    left: Double,
                                                    width: Double,
                                                    height: Double
                                                  )

  def dataGrid[T](init: DataGrid[T] ?=> Unit): DataGrid[T] =
    DslRuntime.build(new DataGrid[T]())(init)

  def dataGrid[T](init: DataGrid[T] ?=> Unit)(renderer: Renderer[T]): DataGrid[T] = {
    val grid = new DataGrid[T]()
    grid.setRenderer(renderer)
    DslRuntime.build(grid)(init)
  }

  def items[T](using grid: DataGrid[T]): ListProperty[T] =
    grid.$items

  def items_=[T](value: ListProperty[T])(using grid: DataGrid[T]): Unit =
    grid.setItems(value)

  def items_=[T](value: scala.collection.IterableOnce[T])(using grid: DataGrid[T]): Unit =
    value match {
      case property: ListProperty[?] =>
        grid.setItems(property.asInstanceOf[ListProperty[T]])
      case _ =>
        grid.$items.setAll(value)
    }

  def itemWidthPx(using grid: DataGrid[?]): Double =
    grid.$itemWidthProperty.get

  def itemWidthPx_=(value: Double)(using grid: DataGrid[?]): Unit =
    grid.$itemWidthProperty.set(value)

  def itemHeightPx(using grid: DataGrid[?]): Double =
    grid.$itemHeightProperty.get

  def itemHeightPx_=(value: Double)(using grid: DataGrid[?]): Unit =
    grid.$itemHeightProperty.set(value)

  def gapPx(using grid: DataGrid[?]): Double =
    grid.$gapProperty.get

  def gapPx_=(value: Double)(using grid: DataGrid[?]): Unit =
    grid.$gapProperty.set(value)

  def overscanRows(using grid: DataGrid[?]): Int =
    grid.$overscanRowsProperty.get

  def overscanRows_=(value: Int)(using grid: DataGrid[?]): Unit =
    grid.$overscanRowsProperty.set(math.max(0, value))

  def prefetchItems(using grid: DataGrid[?]): Int =
    grid.$prefetchItemsProperty.get

  def prefetchItems_=(value: Int)(using grid: DataGrid[?]): Unit =
    grid.$prefetchItemsProperty.set(math.max(1, value))

  def crawlable(using grid: DataGrid[?]): Boolean =
    grid.$crawlableProperty.get

  def crawlable_=(value: Boolean)(using grid: DataGrid[?]): Unit =
    grid.$crawlableProperty.set(value)

  def cellRenderer[T](using grid: DataGrid[T]): Renderer[T] =
    grid.getRenderer

  def cellRenderer_=[T](value: Renderer[T])(using grid: DataGrid[T]): Unit =
    grid.setRenderer(value)

  def loadingPlaceholder[T](factory: => Component | Null)(using grid: DataGrid[T]): Unit =
    grid.setLoadingPlaceholderFactory(() => factory)

  def emptyPlaceholder[T](factory: => Component | Null)(using grid: DataGrid[T]): Unit =
    grid.setEmptyPlaceholderFactory(() => factory)

  def header[T](factory: => Component | Null)(using grid: DataGrid[T]): Unit =
    grid.setHeaderFactory(() => factory)

  private def dataGridCell[T](
                               cell: VisibleCell[T],
                               renderer: Renderer[T]
                             ): DataGridCell[T] =
    DslRuntime.build(new DataGridCell[T](cell, renderer)) {}

  private final class DataGridCell[T](
                                       cell: VisibleCell[T],
                                       renderer: Renderer[T]
                                     ) extends Box("div") {

    override def compose(): Unit = {
      given Component = this

      addClass("jfx-data-grid-cell")
      if (!cell.loaded) addClass("jfx-data-grid-cell-loading")

      style {
        position = "absolute"
        left = s"${cell.left}px"
        top = s"${cell.top}px"
        width = s"${cell.width}px"
        height = s"${cell.height}px"
        boxSizing = "border-box"
      }

      renderer(cell.item, cell.index)
    }
  }
}
