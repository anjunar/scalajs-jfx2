package jfx.layout

import jfx.action.Button.*
import jfx.core.component.{Box, Component, Text}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.layout.Span.span
import org.scalajs.dom.{Event, HTMLElement, Node, PointerEvent, window as browserWindow}

import scala.scalajs.js
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.control.NonFatal
import scala.compiletime.uninitialized

final class Window extends Box("div") {

  val titleProperty: Property[String] = Property.owned(disposable, "")
  val maximizedProperty: Property[Boolean] = Property.owned(disposable, false)
  val zIndexProperty: Property[Int] = Property.owned(disposable, 0)
  val activeProperty: Property[Boolean] = Property.owned(disposable, false)
  
  private val hasCloseHandler = Property.owned(disposable, false)

  var draggable: Boolean = true
  var resizeable: Boolean = true
  var centerOnOpen: Boolean = true
  var rememberPosition: Boolean = true
  var positionStorageKey: String | Null = null
  var rememberSize: Boolean = true

  private var didAutoCenter: Boolean = false
  private var closeHandler: Option[Window => Unit] = None
  private var clickHandler: Option[Window => Unit] = None

  private var headerHost: HTMLElement = uninitialized
  private var actionsHost: HTMLElement = uninitialized
  private var containerHost: Box = uninitialized

  private var didRunOpenSequence = false
  private var activePointerCleanup: Option[Boolean => Unit] = None
  private var openAnimationHandle: Option[SetTimeoutHandle] = None
  private var contentFactory: () => Component | Null = () => null

  override def compose(): Unit = {
    given Window = this
    addClass("jfx-window")

    addDisposable(zIndexProperty.observe { value =>
      host.setStyle("z-index", value.toString)
    })

    addDisposable(maximizedProperty.observe(syncMaximizedState))
    addDisposable(activeProperty.observe(syncActiveState))
    addDisposable(() => stopActivePointerInteraction(persistState = false))
    addDisposable(() => openAnimationHandle.foreach(clearTimeout))

    onClick { _ => clickHandler.foreach(_(this)) }

    div {
      addClass("jfx-window__surface")
      
      div {
        headerHost = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
        addClass("jfx-window__header")
        
        span {
          addClass("jfx-window__title")
          text = titleProperty
        }
        
        div {
          actionsHost = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
          addClass("jfx-window__actions")
          
          button("stat_minus_1") {
            addClass("material-icons jfx-window__chrome-button")
            buttonType = "button"
            onClick { event =>
              event.stopPropagation()
              maximizedProperty.set(false)
            }
          }
          
          button("close") {
            addClass("material-icons jfx-window__chrome-button")
            buttonType = "button"
            onClick { event =>
              event.stopPropagation()
              closeHandler.foreach(_(this))
            }
            visible = hasCloseHandler
          }
        }
      }
      
      containerHost = div {
        addClass("jfx-window__container")
      }
    }

    configureHeaderDrag()
    configureResizeHandles()

    if (!didRunOpenSequence) {
      didRunOpenSequence = true
      openAnimationHandle = Some(setTimeout(300) {
        maximizedProperty.set(true)
      })

      restoreSizeFromStorage()
      if (!restorePositionFromStorage()) {
        centerInViewport()
      }
    }
  }

  def title: String = titleProperty.get
  def title_=(v: String): Unit = titleProperty.set(v)

  def active: Boolean = activeProperty.get
  def active_=(v: Boolean): Unit = activeProperty.set(v)

  def onCloseWindow(block: Window => Unit): Unit = {
    closeHandler = Some(block)
    hasCloseHandler.set(true)
  }

  def onClickWindow(block: Window => Unit): Unit =
    clickHandler = Some(block)

  def restoreSizeFromStorage(force: Boolean = true): Boolean = {
    (for {
      key <- resolvedSizeStorageKey()
      raw <- storageGet(key)
    } yield {
      val parts = raw.split(",", -1)
      if (parts.length != 2) false
      else {
        val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
        val storedWidth = parts(0).trim.toIntOption.filter(_ > 0)
        val storedHeight = parts(1).trim.toIntOption.filter(_ > 0)

        if (storedWidth.isEmpty && storedHeight.isEmpty) false
        else {
          var applied = false
          storedWidth.foreach { width =>
            if (force || element.style.width.isBlank) {
              element.style.width = s"${width}px"
              applied = true
            }
          }
          storedHeight.foreach { height =>
            if (force || element.style.height.isBlank) {
              element.style.height = s"${height}px"
              applied = true
            }
          }
          applied
        }
      }
    }).getOrElse(false)
  }

  def restorePositionFromStorage(force: Boolean = false): Boolean = {
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    if (!force && (!element.style.left.isBlank || !element.style.top.isBlank)) false
    else {
      val parsedPosition = for {
        key <- resolvedPositionStorageKey()
        raw <- storageGet(key)
        parts = raw.split(",", -1)
        if parts.length == 2
        storedLeft <- parts(0).trim.toIntOption
        storedTop <- parts(1).trim.toIntOption
      } yield (storedLeft, storedTop)

      parsedPosition match {
        case Some((storedLeft, storedTop)) =>
          didAutoCenter = true
          setLeftTopPx(storedLeft.toDouble.max(0.0), storedTop.toDouble.max(0.0))
          def attempt(triesLeft: Int): Unit = {
            val width = element.offsetWidth
            val height = element.offsetHeight
            if ((width <= 0 || height <= 0) && triesLeft > 0) {
              browserWindow.requestAnimationFrame(_ => attempt(triesLeft - 1))
              ()
            } else {
              val containerWidth = element.offsetParent match {
                case host: HTMLElement if host.clientWidth > 0 => host.clientWidth.toDouble
                case _ => browserWindow.innerWidth.toDouble
              }
              val containerHeight = element.offsetParent match {
                case host: HTMLElement if host.clientHeight > 0 => host.clientHeight.toDouble
                case _ => browserWindow.innerHeight.toDouble
              }
              val maxLeft = (containerWidth - width.toDouble).max(0.0)
              val maxTop = (containerHeight - height.toDouble).max(0.0)
              setLeftTopPx(storedLeft.toDouble.max(0.0).min(maxLeft), storedTop.toDouble.max(0.0).min(maxTop))
            }
          }
          browserWindow.requestAnimationFrame(_ => attempt(5))
          true
        case None => false
      }
    }
  }

  def centerInViewport(force: Boolean = false): Unit = {
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    if (!force) {
      if (!centerOnOpen || didAutoCenter) return
      if (!element.style.left.isBlank || !element.style.top.isBlank) return
    }
    def attempt(triesLeft: Int): Unit = {
      val width = element.offsetWidth
      val height = element.offsetHeight
      if ((width <= 0 || height <= 0) && triesLeft > 0) {
        browserWindow.requestAnimationFrame(_ => attempt(triesLeft - 1))
        ()
      } else {
        val left = (browserWindow.scrollX + (browserWindow.innerWidth - width) / 2.0).max(0.0)
        val top = (browserWindow.scrollY + (browserWindow.innerHeight - height) / 2.0).max(0.0)
        setLeftTopPx(left, top)
        didAutoCenter = true
      }
    }
    browserWindow.requestAnimationFrame(_ => attempt(5))
  }

  private[jfx] def setContentFactory(factory: () => Component | Null): Unit = {
    contentFactory = if (factory == null) (() => null) else factory
    if (containerHost != null) {
       containerHost.host.clearChildren()
       containerHost._children.foreach(_.dispose())
       containerHost._children.clear()
       
       DslRuntime.withComponentScope(containerHost) {
         contentFactory()
       }
    }
  }

  private def configureHeaderDrag(): Unit = {
    val pointerDownListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if shouldStartDrag(event) =>
        startDrag(event, headerHost)
      case _ =>
    }
    headerHost.addEventListener("pointerdown", pointerDownListener)
  }

  private def configureResizeHandles(): Unit = {
    val handleNames = Vector(
      ("n", 0, -1), ("ne", 1, -1), ("e", 1, 0), ("se", 1, 1),
      ("s", 0, 1), ("sw", -1, 1), ("w", -1, 0), ("nw", -1, -1)
    )
    DslRuntime.withComponentScope(this) {
      handleNames.map { case (name, horizontal, vertical) =>
        div {
          addClass("jfx-window__handle")
          addClass(s"jfx-window__handle--$name")
          val handleEl = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
          val pointerDownListener: js.Function1[Event, Unit] = {
            case event: PointerEvent if isPrimaryPointerButton(event) =>
              startResize(event, handleEl, horizontal = horizontal, vertical = vertical)
            case _ =>
          }
          handleEl.addEventListener("pointerdown", pointerDownListener)
        }
      }
    }
  }

  private def startDrag(event: PointerEvent, captureTarget: HTMLElement): Unit = {
    if (!draggable) return
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    val startLeft = element.offsetLeft.toDouble
    val startTop = element.offsetTop.toDouble
    val startX = event.clientX.toDouble
    val startY = event.clientY.toDouble
    beginPointerInteraction(event, captureTarget) { current =>
      val dx = current.clientX.toDouble - startX
      val dy = current.clientY.toDouble - startY
      setLeftTopPx(startLeft + dx, (startTop + dy).max(0.0))
    }
  }

  private def startResize(event: PointerEvent, captureTarget: HTMLElement, horizontal: Int, vertical: Int): Unit = {
    if (!resizeable) return
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    val startLeft = element.offsetLeft.toDouble
    val startTop = element.offsetTop.toDouble
    val startWidth = element.offsetWidth.toDouble
    val startHeight = element.offsetHeight.toDouble
    val startX = event.clientX.toDouble
    val startY = event.clientY.toDouble
    val minWidth = 32.0
    val minHeight = 32.0
    beginPointerInteraction(event, captureTarget) { current =>
      val dx = current.clientX.toDouble - startX
      val dy = current.clientY.toDouble - startY
      val nextWidth = horizontal match {
        case -1 => (startWidth - dx).max(minWidth)
        case 1  => (startWidth + dx).max(minWidth)
        case _  => startWidth
      }
      val nextHeight = vertical match {
        case -1 => (startHeight - dy).max(minHeight)
        case 1  => (startHeight + dy).max(minHeight)
        case _  => startHeight
      }
      val nextLeft = if (horizontal < 0) startLeft + (startWidth - nextWidth) else startLeft
      val nextTop = if (vertical < 0) startTop + (startHeight - nextHeight) else startTop
      element.style.left = s"${nextLeft.round.toInt}px"
      element.style.top = s"${nextTop.round.toInt}px"
      element.style.width = s"${nextWidth.round.toInt}px"
      element.style.height = s"${nextHeight.round.toInt}px"
    }
  }

  private def beginPointerInteraction(startEvent: PointerEvent, captureTarget: HTMLElement)(onMove: PointerEvent => Unit): Unit = {
    stopActivePointerInteraction(persistState = false)
    startEvent.preventDefault()
    startEvent.stopPropagation()
    val moveListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if event.pointerId == startEvent.pointerId =>
        event.preventDefault()
        onMove(event)
      case _ =>
    }
    val finishListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if event.pointerId == startEvent.pointerId =>
        stopActivePointerInteraction(persistState = true)
      case _ =>
    }
    captureTarget.addEventListener("pointermove", moveListener)
    captureTarget.addEventListener("pointerup", finishListener)
    captureTarget.addEventListener("pointercancel", finishListener)
    captureTarget.addEventListener("lostpointercapture", finishListener)
    try { captureTarget.setPointerCapture(startEvent.pointerId) } catch { case NonFatal(_) => }
    activePointerCleanup = Some { persistState =>
      captureTarget.removeEventListener("pointermove", moveListener)
      captureTarget.removeEventListener("pointerup", finishListener)
      captureTarget.removeEventListener("pointercancel", finishListener)
      captureTarget.removeEventListener("lostpointercapture", finishListener)
      try { if (captureTarget.hasPointerCapture(startEvent.pointerId)) captureTarget.releasePointerCapture(startEvent.pointerId) } catch { case NonFatal(_) => }
      if (persistState) persistWindowStateToStorage()
    }
  }

  private def isPrimaryPointerButton(event: PointerEvent): Boolean = event.button == 0
  private def shouldStartDrag(event: PointerEvent): Boolean = isPrimaryPointerButton(event) && !isChromeActionTarget(event.target)
  private def isChromeActionTarget(target: org.scalajs.dom.EventTarget | Null): Boolean = target match {
    case node: Node => headerHost != null && headerHost.contains(node) && (actionsHost == null || !actionsHost.contains(node))
    case _ => false
  }

  private def stopActivePointerInteraction(persistState: Boolean): Unit = {
    val cleanup = activePointerCleanup
    activePointerCleanup = None
    cleanup.foreach(_(persistState))
  }

  private def resolvedPositionStorageKey(): Option[String] = {
    if (!rememberPosition) return None
    val raw = Option(positionStorageKey).map(_.trim).filter(_.nonEmpty).orElse(Option(title).map(_.trim).filter(_.nonEmpty))
    raw.map(value => s"jFx2.window.position:$value")
  }

  private def resolvedSizeStorageKey(): Option[String] = {
    if (!rememberSize) return None
    val raw = Option(positionStorageKey).map(_.trim).filter(_.nonEmpty).orElse(Option(title).map(_.trim).filter(_.nonEmpty))
    raw.map(value => s"jFx2.window.size:$value")
  }

  private def setLeftTopPx(left: Double, top: Double): Unit = {
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    element.style.left = s"${left.round.toInt}px"
    element.style.top = s"${top.round.toInt}px"
  }

  private def persistWindowStateToStorage(): Unit = {
    resolvedPositionStorageKey().foreach { key =>
      val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
      storageSet(key, s"${element.offsetLeft},${element.offsetTop}")
    }
    resolvedSizeStorageKey().foreach { key =>
      val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
      storageSet(key, s"${element.style.width.stripSuffix("px")},${element.style.height.stripSuffix("px")}")
    }
  }

  private def syncMaximizedState(isMaximized: Boolean): Unit = {
    given Component = this
    if (isMaximized) removeClass("is-hidden") else addClass("is-hidden")
  }

  private def syncActiveState(active: Boolean): Unit = {
    given Component = this
    if (active) addClass("is-active") else removeClass("is-active")
  }

  private def storageGet(key: String): Option[String] = try { Option(browserWindow.localStorage.getItem(key)).map(_.trim).filter(_.nonEmpty) } catch { case NonFatal(_) => None }
  private def storageSet(key: String, value: String): Unit = try { browserWindow.localStorage.setItem(key, value) } catch { case NonFatal(_) => () }
}

object Window {
  def window(init: Window ?=> Unit): Window = {
    DslRuntime.build(new Window())(init)
  }
  
  def title(using w: Window): String = w.title
  def title_=(v: String)(using w: Window): Unit = w.title = v
  
  def draggable(using w: Window): Boolean = w.draggable
  def draggable_=(v: Boolean)(using w: Window): Unit = w.draggable = v

  def resizeable(using w: Window): Boolean = w.resizeable
  def resizeable_=(v: Boolean)(using w: Window): Unit = w.resizeable = v

  def centerOnOpen(using w: Window): Boolean = w.centerOnOpen
  def centerOnOpen_=(v: Boolean)(using w: Window): Unit = w.centerOnOpen = v

  def rememberPosition(using w: Window): Boolean = w.rememberPosition
  def rememberPosition_=(v: Boolean)(using w: Window): Unit = w.rememberPosition = v

  def positionStorageKey(using w: Window): String | Null = w.positionStorageKey
  def positionStorageKey_=(v: String | Null)(using w: Window): Unit = w.positionStorageKey = v

  def rememberSize(using w: Window): Boolean = w.rememberSize
  def rememberSize_=(v: Boolean)(using w: Window): Unit = w.rememberSize = v

  def active(using w: Window): Boolean = w.active
  def active_=(v: Boolean | jfx.core.state.ReadOnlyProperty[Boolean])(using w: Window): Unit = v match {
    case b: Boolean => w.active = b
    case p: jfx.core.state.ReadOnlyProperty[Boolean] => 
       w.addDisposable(p.observe(w.active = _))
  }

  def zIndex(using w: Window): Property[Int] = w.zIndexProperty
  def zIndex_=(p: Property[Int])(using w: Window): Unit = 
     w.addDisposable(jfx.core.state.Property.subscribeBidirectional(w.zIndexProperty, p))

  def maximized(using w: Window): Property[Boolean] = w.maximizedProperty
  def maximized_=(p: Property[Boolean])(using w: Window): Unit = 
     w.addDisposable(jfx.core.state.Property.subscribeBidirectional(w.maximizedProperty, p))

  def onCloseWindow(handler: Window => Unit)(using w: Window): Unit = w.onCloseWindow(handler)
  def onClickWindow(handler: Window => Unit)(using w: Window): Unit = w.onClickWindow(handler)
  
  def content(factory: () => Component | Null)(using w: Window): Unit = w.setContentFactory(factory)
}
