package jfx.layout

import jfx.action.Button
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom.{Event, HTMLElement, Node, PointerEvent, window as browserWindow}

import scala.scalajs.js
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.control.NonFatal
import scala.compiletime.uninitialized

final class Window extends Box("div") {

  val maximized: Property[Boolean] = Property.owned(disposable, false)
  val zIndex: Property[Int] = Property.owned(disposable, 0)

  var draggable: Boolean = true

  private var _resizeable: Boolean = true
  private var _active: Boolean = false
  private var _title: String = ""

  var centerOnOpen: Boolean = true
  private var didAutoCenter: Boolean = false

  var rememberPosition: Boolean = true
  var positionStorageKey: String | Null = null

  var rememberSize: Boolean = true

  private var closeHandler: Option[Window => Unit] = None
  private var clickHandler: Option[Window => Unit] = None

  private var headerHost: Box = uninitialized
  private var titleHost: Span = uninitialized
  private var actionsHost: Box = uninitialized
  private var surfaceHost: Box = uninitialized
  private var containerHost: Box = uninitialized

  private var minimizeButton: Button = uninitialized
  private var closeButton: Button = uninitialized

  private var resizeHandles: Vector[(String, Int, Int, Box)] = Vector.empty

  private var didRunOpenSequence = false
  private var activePointerCleanup: Option[Boolean => Unit] = None
  private var openAnimationHandle: Option[SetTimeoutHandle] = None
  private var contentFactory: () => Component | Null = () => null
  private var contentInitialized = false

  override def compose(): Unit = {
    addBaseClass("jfx-window")

    addDisposable(zIndex.observe { value =>
      host.setStyle("z-index", value.toString)
    })

    addDisposable(maximized.observe(syncMaximizedState))
    addDisposable(() => stopActivePointerInteraction(persistState = false))
    addDisposable(() => openAnimationHandle.foreach(clearTimeout))

    val clickListener: js.Function1[Event, Unit] = _ => clickHandler.foreach(_(this))
    addDisposable(host.addEventListener("click", clickListener))

    surfaceHost = new Box("div")
    surfaceHost.addBaseClass("jfx-window__surface")
    addChild(surfaceHost)

    DslRuntime.withComponentScope(surfaceHost) {
      headerHost = new Box("div")
      headerHost.addBaseClass("jfx-window__header")
      surfaceHost.addChild(headerHost)

      DslRuntime.withComponentScope(headerHost) {
        titleHost = new Span()
        titleHost.addBaseClass("jfx-window__title")
        headerHost.addChild(titleHost)

        actionsHost = new Box("div")
        actionsHost.addBaseClass("jfx-window__actions")
        headerHost.addChild(actionsHost)

        DslRuntime.withComponentScope(actionsHost) {
          given Component = actionsHost
          minimizeButton = new Button()
          DslRuntime.withComponentScope(minimizeButton) {
            given Component = minimizeButton
            host.setAttribute("type", "button")
            text = "stat_minus_1"
            addBaseClass("material-icons")
            addBaseClass("jfx-window__chrome-button")
            onClick { event =>
              event.stopPropagation()
              maximized.set(false)
            }
          }
          actionsHost.addChild(minimizeButton)

          closeButton = new Button()
          DslRuntime.withComponentScope(closeButton) {
            given Component = closeButton
            host.setAttribute("type", "button")
            text = "close"
            addBaseClass("material-icons")
            addBaseClass("jfx-window__chrome-button")
            onClick { event =>
              event.stopPropagation()
              closeHandler.foreach(_(this))
            }
          }
          actionsHost.addChild(closeButton)
        }
      }

      containerHost = new Box("div")
      containerHost.addBaseClass("jfx-window__container")
      surfaceHost.addChild(containerHost)
    }

    configureHeaderDrag()
    configureResizeHandles()

    syncTitleState()
    syncCloseButtonState()
    syncResizableState()
    syncActiveState()

    if (!didRunOpenSequence) {
      didRunOpenSequence = true

      openAnimationHandle = Some(setTimeout(300) {
        maximized.set(true)
      })

      restoreSizeFromStorage()
      if (!restorePositionFromStorage()) {
        centerInViewport()
      }
    }

    ensureContentMounted()
  }

  def resizeable: Boolean =
    _resizeable

  def resizeable_=(value: Boolean): Unit = {
    _resizeable = value
    syncResizableState()
  }

  def resizable: Boolean =
    resizeable

  def resizable_=(value: Boolean): Unit =
    resizeable_=(value)

  def active: Boolean =
    _active

  def active_=(value: Boolean): Unit = {
    _active = value
    syncActiveState()
  }

  def title: String =
    _title

  def title_=(value: String): Unit = {
    _title = value
    syncTitleState()
  }

  def onCloseWindow(block: Window => Unit): Unit = {
    closeHandler = Some(block)
    syncCloseButtonState()
  }

  def onClickWindow(block: Window => Unit): Unit =
    clickHandler = Some(block)

  def restoreSizeFromStorage(force: Boolean = true): Boolean = {
    (for {
      key <- resolvedSizeStorageKey()
      raw <- storageGet(key)
    } yield {
      val parts = raw.split(",", -1)

      if (parts.length != 2) {
        false
      } else {
        val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
        val storedWidth = parts(0).trim.toIntOption.filter(_ > 0)
        val storedHeight = parts(1).trim.toIntOption.filter(_ > 0)

        if (storedWidth.isEmpty && storedHeight.isEmpty) {
          false
        } else {
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

    if (!force && (!element.style.left.isBlank || !element.style.top.isBlank)) {
      false
    } else {
      val parsedPosition =
        for {
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
              val containerWidth =
                element.offsetParent match {
                  case host: HTMLElement if host.clientWidth > 0 => host.clientWidth.toDouble
                  case _                                         => browserWindow.innerWidth.toDouble
                }
              val containerHeight =
                element.offsetParent match {
                  case host: HTMLElement if host.clientHeight > 0 => host.clientHeight.toDouble
                  case _                                          => browserWindow.innerHeight.toDouble
                }

              val maxLeft = (containerWidth - width.toDouble).max(0.0)
              val maxTop = (containerHeight - height.toDouble).max(0.0)

              setLeftTopPx(
                storedLeft.toDouble.max(0.0).min(maxLeft),
                storedTop.toDouble.max(0.0).min(maxTop)
              )
            }
          }

          browserWindow.requestAnimationFrame(_ => attempt(5))
          true

        case None =>
          false
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
    contentFactory =
      if (factory == null) (() => null)
      else factory

    contentInitialized = false
    if (containerHost != null) {
       containerHost.host.clearChildren()
       // Also need to clear logical children
       containerHost._children.clear()
    }

    ensureContentMounted()
  }

  private def configureHeaderDrag(): Unit = {
    val pointerDownListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if shouldStartDrag(event) =>
        val target = headerHost.host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
        startDrag(event, target)
      case _ =>
        ()
    }

    headerHost.host.addEventListener("pointerdown", pointerDownListener)
  }

  private def configureResizeHandles(): Unit = {
    val handleNames = Vector(
      ("n", 0, -1),
      ("ne", 1, -1),
      ("e", 1, 0),
      ("se", 1, 1),
      ("s", 0, 1),
      ("sw", -1, 1),
      ("w", -1, 0),
      ("nw", -1, -1)
    )

    resizeHandles = handleNames.map { case (name, horizontal, vertical) =>
      val handle = new Box("div")
      handle.addBaseClass("jfx-window__handle")
      handle.addBaseClass(s"jfx-window__handle--$name")
      addChild(handle)

      val pointerDownListener: js.Function1[Event, Unit] = {
        case event: PointerEvent if isPrimaryPointerButton(event) =>
          val target = handle.host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
          startResize(event, target, horizontal = horizontal, vertical = vertical)
        case _ =>
          ()
      }

      handle.host.addEventListener("pointerdown", pointerDownListener)
      (name, horizontal, vertical, handle)
    }
  }

  private def ensureContentMounted(): Unit =
    if (!contentInitialized && containerHost != null) {
      contentInitialized = true
      val content = contentFactory()
      if (content != null) {
        DslRuntime.withComponentScope(containerHost) {
          containerHost.addChild(content)
          // Since it's added manually after compose, we need to bind and sync
          content.setParent(Some(containerHost))
          content.bind(DslRuntime.currentCursor)
          containerHost.syncChildAddition(content)
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

  private def startResize(
    event: PointerEvent,
    captureTarget: HTMLElement,
    horizontal: Int,
    vertical: Int
  ): Unit = {
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

      val nextWidth =
        horizontal match {
          case -1 => (startWidth - dx).max(minWidth)
          case 1  => (startWidth + dx).max(minWidth)
          case _  => startWidth
        }

      val nextHeight =
        vertical match {
          case -1 => (startHeight - dy).max(minHeight)
          case 1  => (startHeight + dy).max(minHeight)
          case _  => startHeight
        }

      val nextLeft =
        if (horizontal < 0) startLeft + (startWidth - nextWidth)
        else startLeft

      val nextTop =
        if (vertical < 0) startTop + (startHeight - nextHeight)
        else startTop

      element.style.left = s"${nextLeft.round.toInt}px"
      element.style.top = s"${nextTop.round.toInt}px"
      element.style.right = ""
      element.style.bottom = ""
      element.style.width = s"${nextWidth.round.toInt}px"
      element.style.height = s"${nextHeight.round.toInt}px"
    }
  }

  private def beginPointerInteraction(
    startEvent: PointerEvent,
    captureTarget: HTMLElement
  )(onMove: PointerEvent => Unit): Unit = {
    stopActivePointerInteraction(persistState = false)
    startEvent.preventDefault()
    startEvent.stopPropagation()

    val moveListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if event.pointerId == startEvent.pointerId =>
        event.preventDefault()
        onMove(event)
      case _ => ()
    }

    val finishListener: js.Function1[Event, Unit] = {
      case event: PointerEvent if event.pointerId == startEvent.pointerId =>
        stopActivePointerInteraction(persistState = true)
      case _ =>
        ()
    }

    captureTarget.addEventListener("pointermove", moveListener)
    captureTarget.addEventListener("pointerup", finishListener)
    captureTarget.addEventListener("pointercancel", finishListener)
    captureTarget.addEventListener("lostpointercapture", finishListener)

    try {
      captureTarget.setPointerCapture(startEvent.pointerId)
    } catch {
      case NonFatal(_) => ()
    }

    activePointerCleanup = Some { persistState =>
      captureTarget.removeEventListener("pointermove", moveListener)
      captureTarget.removeEventListener("pointerup", finishListener)
      captureTarget.removeEventListener("pointercancel", finishListener)
      captureTarget.removeEventListener("lostpointercapture", finishListener)

      try {
        if (captureTarget.hasPointerCapture(startEvent.pointerId)) {
          captureTarget.releasePointerCapture(startEvent.pointerId)
        }
      } catch {
        case NonFatal(_) => ()
      }

      if (persistState) {
        persistWindowStateToStorage()
      }
    }
  }

  private def isPrimaryPointerButton(event: PointerEvent): Boolean =
    event.button == 0

  private def shouldStartDrag(event: PointerEvent): Boolean =
    isPrimaryPointerButton(event) && !isChromeActionTarget(event.target)

  private def isChromeActionTarget(target: org.scalajs.dom.EventTarget | Null): Boolean =
    target match {
      case node: Node => actionsHost.host.asInstanceOf[jfx.core.render.DomHostElement].element.contains(node)
      case _          => false
    }

  private def stopActivePointerInteraction(persistState: Boolean): Unit = {
    val cleanup = activePointerCleanup
    activePointerCleanup = None
    cleanup.foreach(_(persistState))
  }

  private def resolvedPositionStorageKey(): Option[String] = {
    if (!rememberPosition) return None

    val raw =
      Option(positionStorageKey)
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(Option(title).map(_.trim).filter(_.nonEmpty))

    raw.map(value => s"jFx2.window.position:$value")
  }

  private def resolvedSizeStorageKey(): Option[String] = {
    if (!rememberSize) return None

    val raw =
      Option(positionStorageKey)
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(Option(title).map(_.trim).filter(_.nonEmpty))

    raw.map(value => s"jFx2.window.size:$value")
  }

  private def setLeftTopPx(left: Double, top: Double): Unit = {
    val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
    element.style.left = s"${left.round.toInt}px"
    element.style.top = s"${top.round.toInt}px"
    element.style.right = ""
    element.style.bottom = ""
  }

  private def persistSizeToStorage(): Unit = {
    resolvedSizeStorageKey().foreach { key =>
      val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]

      def pxToInt(value: String): Option[Int] = {
        val trimmed = value.trim
        if (trimmed.isBlank || !trimmed.endsWith("px")) None
        else trimmed.stripSuffix("px").trim.toIntOption
      }

      val width = pxToInt(element.style.width)
      val height = pxToInt(element.style.height)

      if (width.nonEmpty || height.nonEmpty) {
        storageSet(key, s"${width.map(_.toString).getOrElse("")},${height.map(_.toString).getOrElse("")}")
      }
    }
  }

  private def persistPositionToStorage(): Unit = {
    resolvedPositionStorageKey().foreach { key =>
      val element = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement]
      storageSet(key, s"${element.offsetLeft},${element.offsetTop}")
    }
  }

  private def persistWindowStateToStorage(): Unit = {
    persistPositionToStorage()
    persistSizeToStorage()
  }

  private def syncTitleState(): Unit =
    if (titleHost != null) {
       titleHost.host.asInstanceOf[jfx.core.render.DomHostElement].element.textContent = title
    }

  private def syncCloseButtonState(): Unit =
    if (closeButton != null) {
      if (closeHandler.nonEmpty) {
        closeButton.removeBaseClass("is-hidden")
      } else {
        closeButton.addBaseClass("is-hidden")
      }
    }

  private def syncResizableState(): Unit =
    if (resizeable) {
      addBaseClass("jfx-window--resizable")
    } else {
      removeBaseClass("jfx-window--resizable")
    }

  private def syncActiveState(): Unit =
    if (active) {
      addBaseClass("is-active")
    } else {
      removeBaseClass("is-active")
    }

  private def syncMaximizedState(isMaximized: Boolean): Unit =
    if (isMaximized) {
      removeBaseClass("is-hidden")
    } else {
      addBaseClass("is-hidden")
    }

  private def storageGet(key: String): Option[String] =
    try {
      Option(browserWindow.localStorage.getItem(key)).map(_.trim).filter(_.nonEmpty)
    } catch {
      case NonFatal(_) => None
    }

  private def storageSet(key: String, value: String): Unit =
    try {
      browserWindow.localStorage.setItem(key, value)
    } catch {
      case NonFatal(_) => ()
    }
}
