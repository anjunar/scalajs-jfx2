package jfx.control

import jfx.control.Link.link
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{CompositeDisposable, Disposable, ListProperty, Property, RemoteListProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.router.RouteContext
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import org.scalajs.dom

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

final class VirtualListView[T](
  initialItems: ListProperty[T] | Null = null,
  initialEstimateHeight: Double = 44.0,
  initialOverscanPx: Double = 240.0,
  initialPrefetchItems: Int = 80,
  initialCrawlable: Boolean = false,
  initialRenderer: (T | Null, Int) => Unit = (_: T | Null, _: Int) => ()
) extends Box("div") {

  private given ExecutionContext = ExecutionContext.global

  private val itemsRefProperty = Property[ListProperty[T]](normalizeItems(initialItems))

  val $estimateHeightProperty = Property(initialEstimateHeight)
  val $overscanPxProperty = Property(initialOverscanPx)
  val $prefetchItemsProperty = Property(math.max(1, initialPrefetchItems))

  val $scrollTopProperty = Property(0.0)
  val $viewportHeightProperty = Property(400.0)

  val $crawlableProperty = Property(initialCrawlable)
  private val defaultLimit = 50

  private val visibleSlotsProperty = new ListProperty[VirtualListView.VisibleSlot[T]]()
  private val itemStateRevisionProperty = Property(0)
  private val remoteStateRevisionProperty = Property(0)

  private val heights = mutable.ArrayBuffer.empty[Double]
  private val prefix = mutable.ArrayBuffer(0.0)

  private var prefixDirtyFrom: Int = Int.MaxValue
  private var tailPaddingItems: Int = defaultTailPadding
  private var itemRenderer: (T | Null, Int) => Unit = initialRenderer
  private var itemsObserver: Disposable = VirtualListView.noopDisposable
  private var remoteItemsObserver: Disposable = VirtualListView.noopDisposable
  private var viewportMeasureScheduled = false
  private var routeContext: Option[RouteContext] = None

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

  def refresh(): Unit =
    refreshItemState()

  def scrollTo(index: Int): Unit = {
    val total = maxRenderableCount
    if (total <= 0) return

    val clamped = math.max(0, math.min(total - 1, index))
    $scrollTopProperty.set(offsetFor(clamped))
    host.domNode.collect { case element: dom.html.Element =>
      element.querySelector(".jfx-virtual-list-viewport") match {
        case viewport: dom.html.Element => viewport.scrollTop = $scrollTopProperty.get
        case _                          =>
      }
    }
  }

  def setRenderer(renderer: (T | Null, Int) => Unit): Unit =
    itemRenderer = if (renderer == null) ((_: T | Null, _: Int) => ()) else renderer

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

    addClass("jfx-virtual-list")
    classIf("jfx-virtual-list-loading", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.loadingProperty.get
    })
    classIf("jfx-virtual-list-error", remoteStateRevisionProperty.map { _ =>
      val remote = currentRemoteItems
      remote != null && remote.errorProperty.get.nonEmpty
    })

    addDisposable(itemsRefProperty.observe(_ => rewireItemsObserver()))
    addDisposable(() => {
      itemsObserver.dispose()
      remoteItemsObserver.dispose()
    })
    addDisposable($scrollTopProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable($viewportHeightProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable($overscanPxProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable($prefetchItemsProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable($crawlableProperty.observe(_ => refreshItemState()))
    addDisposable($estimateHeightProperty.observe { _ =>
      resetMeasurements()
      refreshItemState()
    })

    if (!RenderBackend.current.isServer) {
      val (offset, _) = getCrawlParams
      if (offset > 0) {
        $scrollTopProperty.set(offsetFor(offset))
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
      addClass("jfx-virtual-list-viewport")
      style {
        width = "100%"
        height = "100%"
        overflow = "auto"
        position = "relative"
      }

      onScroll { event =>
        val target = event.target.asInstanceOf[dom.html.Element]
        $scrollTopProperty.set(target.scrollTop)
        $viewportHeightProperty.set(target.clientHeight.toDouble)
      }

      div {
        addClass("jfx-virtual-list-content")
        style {
          position = "relative"
          width = "100%"
          minHeight = "100%"
          height_=(itemStateRevisionProperty.map(_ => s"${contentHeight}px"))
        }

        forEach(visibleSlotsProperty) { slot =>
          VirtualListView.virtualListCell(slot, itemRenderer, handleMeasuredHeight)
        }

        condition(itemStateRevisionProperty.map(_ => hasMoreCrawlPage)) {
          thenDo {
            val (offset, limit) = getCrawlParams
            link(nextCrawlHref(offset, limit)) {
              addClass("jfx-virtual-list-more-link")
              style {
                display = "block"
                padding = "20px"
                textAlign = "center"
                if (RenderBackend.current.isServer) {
                  marginTop = s"${offsetFor(offset + limit)}px"
                }
              }
              text = "More items..."
            }
          }
        }
      }
    }
  }

  override def afterCompose(): Unit = {
    if (!RenderBackend.current.isServer) {
      scheduleViewportMeasure()
      val listener: dom.Event => Unit = _ => scheduleViewportMeasure()
      dom.window.addEventListener("resize", listener)
      addDisposable(() => dom.window.removeEventListener("resize", listener))
    }
  }

  private def rewireItemsObserver(): Unit = {
    itemsObserver.dispose()
    remoteItemsObserver.dispose()
    bumpRemoteState()

    val currentItems = $items
    val remote = currentRemoteItems

    itemsObserver =
      currentItems.observeChanges { change =>
        if (remote == null) {
          handleLocalItemsChange(change)
        } else {
          bumpRemoteState()
          refreshItemState()
        }
      }

    remoteItemsObserver =
      if (remote == null) {
        VirtualListView.noopDisposable
      } else {
        val composite = new CompositeDisposable()
        composite.add(remote.loadingProperty.observe { _ =>
          bumpRemoteState()
          recomputeVisibleSlots()
        })
        composite.add(remote.errorProperty.observe { _ =>
          bumpRemoteState()
          recomputeVisibleSlots()
        })
        composite.add(remote.totalCountProperty.observe(_ => refreshItemState()))
        composite.add(remote.hasMoreProperty.observe(_ => refreshItemState()))
        composite.add(remote.nextQueryProperty.observe(_ => refreshItemState()))
        composite.add(remote.queryProperty.observeWithoutInitial { _ =>
          resetMeasurements()
          refreshItemState()
        })
        composite.add(remote.sortingProperty.observeWithoutInitial { _ =>
          resetMeasurements()
          refreshItemState()
        })

        if (!RenderBackend.current.isServer && remote.length == 0 && !remote.loadingProperty.get && remote.errorProperty.get.isEmpty) {
          discardPromise(remote.reload())
        }

        composite
      }

    refreshItemState()
  }

  private def handleLocalItemsChange(change: ListProperty.Change[T]): Unit = {
    change match {
      case ListProperty.UpdateAt(_, _, _, _) =>
        ()
      case ListProperty.Add(_, _) =>
        ()
      case _ =>
        resetMeasurements()
    }

    refreshItemState()
  }

  private def refreshItemState(): Unit = {
    bumpItemState()
    recomputeVisibleSlots()
  }

  private def recomputeVisibleSlots(): Unit = {
    rebuildPrefixIfDirty()

    val total = maxRenderableCount
    if (total <= 0) {
      visibleSlotsProperty.setAll(Seq.empty)
      bumpItemState()
      return
    }

    if (RenderBackend.current.isServer && $crawlableProperty.get) {
      val (offset, limit) = getCrawlParams
      val start = math.min(offset, total)
      val end = math.min(total, start + limit)
      visibleSlotsProperty.setAll((start until end).map(crawlSlotFor))
      bumpItemState()
      return
    }

    val viewportHeight = math.max(1.0, $viewportHeightProperty.get)
    val overscan = math.max(0.0, $overscanPxProperty.get)
    val startOffset = math.max(0.0, $scrollTopProperty.get - overscan)
    val endOffset = $scrollTopProperty.get + viewportHeight + overscan
    val maxSlots = maxSlotsForViewport(viewportHeight)

    val slots = mutable.ArrayBuffer.empty[VirtualListView.VisibleSlot[T]]
    var index = math.max(0, math.min(indexForOffset(startOffset), total - 1))
    var top = offsetFor(index)

    while (top < endOffset && index < total && slots.length < maxSlots) {
      val item = itemAt(index)
      val loaded = item != null
      val height = heightFor(index)
      slots += VirtualListView.VisibleSlot(index, item, loaded, top, height)

      top += height
      index += 1
    }

    visibleSlotsProperty.setAll(slots.toSeq)
    bumpItemState()

    if (!RenderBackend.current.isServer && slots.nonEmpty) {
      requestMoreIfNecessary(slots.head.index, slots.last.index + 1)
    }
  }

  private def crawlSlotFor(index: Int): VirtualListView.VisibleSlot[T] = {
    val item = itemAt(index)
    VirtualListView.VisibleSlot(
      index = index,
      item = item,
      loaded = item != null,
      top = offsetFor(index),
      height = heightFor(index)
    )
  }

  private def handleMeasuredHeight(index: Int, height: Double): Unit =
    if (height > 0 && updateHeight(index, height)) {
      rebuildPrefixIfDirty()
      refreshItemState()
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

  private def knownItemCount: Option[Int] =
    currentRemoteItems match {
      case null   => Some($items.length)
      case remote => remote.totalCountProperty.get
    }

  private def canStillGrow: Boolean =
    currentRemoteItems match {
      case null =>
        false
      case remote =>
        remote.loadingProperty.get ||
          remote.hasMoreProperty.get ||
          remote.nextQueryProperty.get.nonEmpty ||
          remote.totalCountProperty.get.isEmpty
    }

  private def maxRenderableCount: Int =
    knownItemCount.getOrElse {
      if (shouldRenderUnloadedPlaceholders) $items.length + tailPaddingItems
      else 0
    }

  private def shouldRenderUnloadedPlaceholders: Boolean =
    $items.length > 0 || knownItemCount.exists(_ > 0) || canStillGrow

  private def requestMoreIfNecessary(visibleStartIndex: Int, visibleEndExclusive: Int): Unit = {
    val remote = currentRemoteItems
    if (remote == null) return
    if (remote.loadingProperty.get) return
    if (remote.errorProperty.get.nonEmpty) return

    val prefetch = math.max(1, $prefetchItemsProperty.get)

    if (knownItemCount.isEmpty && canStillGrow) {
      val projectedEnd = $items.length + tailPaddingItems
      if (visibleEndExclusive + prefetch > projectedEnd) {
        tailPaddingItems += math.max(prefetch * 2, prefetch)
        bumpItemState()
      }
    }

    if (remote.supportsRangeLoading) {
      val requestFrom = math.max(0, visibleStartIndex - prefetch)
      val requestToExclusive = visibleEndExclusive + prefetch

      if (!remote.isRangeLoaded(requestFrom, requestToExclusive)) {
        discardPromise(remote.ensureRangeLoaded(requestFrom, requestToExclusive))
      }
    } else if (canStillGrow) {
      val loadedLength = $items.length
      val threshold = math.max(1, prefetch / 2)

      if (loadedLength == 0) {
        discardPromise(remote.reload())
      } else if (visibleEndExclusive >= math.max(0, loadedLength - threshold)) {
        discardPromise(remote.loadMore())
      }
    }
  }

  private def resetMeasurements(): Unit = {
    heights.clear()
    prefix.clear()
    prefix += 0.0
    prefixDirtyFrom = Int.MaxValue
    tailPaddingItems = defaultTailPadding
  }

  private def ensureHeightsSize(size: Int): Unit =
    while (heights.length < size) {
      heights += estimateHeight
      prefix += (prefix.last + estimateHeight)
    }

  private def updateHeight(index: Int, newHeight: Double): Boolean = {
    if (index < 0) return false

    ensureHeightsSize(index + 1)
    val previous = heights(index)
    if (math.abs(previous - newHeight) <= 0.5) return false

    heights(index) = newHeight
    prefixDirtyFrom = math.min(prefixDirtyFrom, index + 1)
    true
  }

  private def rebuildPrefixIfDirty(): Unit = {
    val from = prefixDirtyFrom
    if (from == Int.MaxValue) return

    val start = math.max(1, math.min(from, prefix.length - 1))
    var index = start
    while (index < prefix.length) {
      prefix(index) = prefix(index - 1) + heights(index - 1)
      index += 1
    }

    prefixDirtyFrom = Int.MaxValue
  }

  private def offsetFor(index: Int): Double = {
    val loaded = heights.length
    if (index <= loaded) {
      if (index < prefix.length) prefix(index) else prefix.last
    } else {
      prefix.last + (index - loaded) * estimateHeight
    }
  }

  private def indexForOffset(offset: Double): Int = {
    val normalizedOffset = math.max(0.0, offset)
    val loaded = heights.length
    if (loaded == 0) return math.floor(normalizedOffset / estimateHeight).toInt

    val totalKnownHeight = prefix.last
    if (normalizedOffset >= totalKnownHeight) {
      loaded + math.floor((normalizedOffset - totalKnownHeight) / estimateHeight).toInt
    } else {
      var low = 0
      var high = loaded

      while (low < high) {
        val mid = (low + high) / 2
        if (prefix(mid + 1) <= normalizedOffset) low = mid + 1
        else high = mid
      }

      low
    }
  }

  private def heightFor(index: Int): Double =
    if (index >= 0 && index < heights.length) heights(index)
    else estimateHeight

  private def contentHeight: Double = {
    rebuildPrefixIfDirty()

    if (!shouldRenderUnloadedPlaceholders) {
      0.0
    } else {
      val base = prefix.lastOption.getOrElse(0.0)
      val extra =
        knownItemCount match {
          case Some(total) =>
            math.max(0, total - heights.length) * estimateHeight
          case None if canStillGrow =>
            tailPaddingItems * estimateHeight
          case None =>
            0.0
        }

      base + extra
    }
  }

  private def hasMoreCrawlPage: Boolean = {
    val (offset, limit) = getCrawlParams
    $crawlableProperty.get && offset + limit < maxRenderableCount
  }

  private def nextCrawlHref(offset: Int, limit: Int): String = {
    val next = s"offset=${offset + limit}&limit=$limit"
    currentRouteContext().map(_.path).filter(path => path.nonEmpty && path != "/") match {
      case Some(path) => s"$path?$next"
      case None       => s"?$next"
    }
  }

  private def maxSlotsForViewport(viewportHeight: Double): Int = {
    val minRowHeight = math.max(12.0, math.min(estimateHeight, math.max(estimateHeight / 2.0, 1.0)))
    val area = viewportHeight + 2 * math.max(0.0, $overscanPxProperty.get)
    val raw = math.ceil(area / minRowHeight).toInt + 8
    math.min(600, math.max(32, raw))
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
      element.querySelector(".jfx-virtual-list-viewport") match {
        case viewport: dom.html.Element =>
          $viewportHeightProperty.set(math.max(1.0, viewport.clientHeight.toDouble))
        case _ =>
          $viewportHeightProperty.set(math.max(1.0, element.clientHeight.toDouble))
      }
    }

  private def bumpItemState(): Unit =
    itemStateRevisionProperty.set(itemStateRevisionProperty.get + 1)

  private def bumpRemoteState(): Unit =
    remoteStateRevisionProperty.set(remoteStateRevisionProperty.get + 1)

  private def discardPromise(promise: js.Promise[?]): Unit = {
    promise.toFuture.recover { case _ => () }
    ()
  }

  private def estimateHeight: Double =
    math.max(1.0, $estimateHeightProperty.get)

  private def defaultTailPadding: Int =
    math.max($prefetchItemsProperty.get * 3, $prefetchItemsProperty.get)

  private def normalizeItems(value: ListProperty[T] | Null): ListProperty[T] =
    if (value == null) new ListProperty[T]() else value
}

object VirtualListView {
  private[control] val noopDisposable: Disposable = () => ()

  type Renderer[T] = (T | Null, Int) => Unit

  private[control] final case class VisibleSlot[T](
    index: Int,
    item: T | Null,
    loaded: Boolean,
    top: Double,
    height: Double
  )

  def virtualList[T](items: ListProperty[T])(renderer: Renderer[T]): VirtualListView[T] =
    virtualList(items, estimateHeightPx = 44, overscanPx = 240, prefetchItems = 80)(renderer)

  def virtualList[T](
    items: ListProperty[T],
    estimateHeightPx: Int = 44,
    overscanPx: Int = 240,
    prefetchItems: Int = 80,
    crawlable: Boolean = false
  )(renderer: Renderer[T]): VirtualListView[T] =
    DslRuntime.build(
      new VirtualListView[T](
        initialItems = items,
        initialEstimateHeight = estimateHeightPx.toDouble,
        initialOverscanPx = overscanPx.toDouble,
        initialPrefetchItems = prefetchItems,
        initialCrawlable = crawlable,
        initialRenderer = renderer
      )
    ) {}

  def items[T](using v: VirtualListView[T]): ListProperty[T] =
    v.$items

  def items_=[T](value: ListProperty[T])(using v: VirtualListView[T]): Unit =
    v.setItems(value)

  def items_=[T](value: scala.collection.IterableOnce[T])(using v: VirtualListView[T]): Unit =
    value match {
      case property: ListProperty[?] =>
        v.setItems(property.asInstanceOf[ListProperty[T]])
      case _ =>
        v.$items.setAll(value)
    }

  def estimateHeight(using v: VirtualListView[?]): Double =
    v.$estimateHeightProperty.get

  def estimateHeight_=(value: Double)(using v: VirtualListView[?]): Unit =
    v.$estimateHeightProperty.set(value)

  def estimateHeightPx(using v: VirtualListView[?]): Double =
    v.$estimateHeightProperty.get

  def estimateHeightPx_=(value: Double)(using v: VirtualListView[?]): Unit =
    v.$estimateHeightProperty.set(value)

  def overscanPx(using v: VirtualListView[?]): Double =
    v.$overscanPxProperty.get

  def overscanPx_=(value: Double)(using v: VirtualListView[?]): Unit =
    v.$overscanPxProperty.set(value)

  def prefetchItems(using v: VirtualListView[?]): Int =
    v.$prefetchItemsProperty.get

  def prefetchItems_=(value: Int)(using v: VirtualListView[?]): Unit =
    v.$prefetchItemsProperty.set(math.max(1, value))

  def crawlable(using v: VirtualListView[?]): Boolean =
    v.$crawlableProperty.get

  def crawlable_=(value: Boolean)(using v: VirtualListView[?]): Unit =
    v.$crawlableProperty.set(value)

  private def virtualListCell[T](
    slot: VisibleSlot[T],
    renderer: Renderer[T],
    onMeasured: (Int, Double) => Unit
  ): VirtualListCell[T] =
    DslRuntime.build(new VirtualListCell[T](slot, renderer, onMeasured)) {}

  private final class VirtualListCell[T](
    slot: VisibleSlot[T],
    renderer: Renderer[T],
    onMeasured: (Int, Double) => Unit
  ) extends Box("div") {

    override def compose(): Unit = {
      given Component = this

      addClass("jfx-virtual-list-cell")
      if (!slot.loaded) addClass("jfx-virtual-list-cell-loading")

      style {
        position = "absolute"
        left = "0"
        width = "100%"
        top = s"${slot.top}px"
        minHeight = s"${slot.height}px"
        boxSizing = "border-box"
      }

      renderer(slot.item, slot.index)
    }

    override def afterCompose(): Unit =
      if (!RenderBackend.current.isServer) {
        dom.window.requestAnimationFrame { _ =>
          host.domNode.foreach {
            case element: dom.html.Element =>
              val height = element.offsetHeight.toDouble
              if (height > 0) {
                onMeasured(slot.index, height)
              }
            case _ =>
          }
        }
      }
  }
}
