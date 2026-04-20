package jfx.layout

import java.util.UUID
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.{Disposable, ListProperty, Property}
import jfx.dsl.{DslRuntime, StyleDsl}
import jfx.statement.ForEach
import org.scalajs.dom.{Event, HTMLDivElement, HTMLElement, window as browserWindow}

import scala.scalajs.js
import scala.scalajs.js.timers.setTimeout
import scala.compiletime.uninitialized

final class Viewport extends Box("div") {

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-viewport")
    
    ForEach.forEach(Viewport.windows) { conf =>
      buildWindow(conf)
    }

    ForEach.forEach(Viewport.overlays) { conf =>
      Viewport.overlay(conf) {}
    }

    ForEach.forEach(Viewport.notifications) { conf =>
      Viewport.notification(conf) {}
    }
  }

  private def buildWindow(conf: Viewport.WindowConf): Unit = {
    import Window.*
    window {
      style {
        if (conf.width > 0) width = conf.width.toString + "px"
        if (conf.height > 0) height = conf.height.toString + "px"
      }

      title = conf.title
      draggable = conf.draggable
      resizeable = conf.resizable
      centerOnOpen = conf.centerOnOpen
      rememberPosition = conf.rememberPosition
      positionStorageKey = conf.positionStorageKey
      rememberSize = conf.rememberSize
      active = Viewport.isActive(conf)

      onCloseWindow { window =>
        conf.onClose.foreach(_(window))
        Viewport.closeWindow(conf)
      }

      onClickWindow { window =>
        conf.onClick.foreach(_(window))
        Viewport.touchWindow(conf)
      }

      zIndex = conf.zIndex
      maximized = conf.maximized
      
      addDisposable(conf.zIndex.observe(_ => active = Viewport.isActive(conf)))

      content { () =>
        val contentComponent = conf.component()
        contentComponent match {
          case closeAware: Viewport.CloseAware if contentComponent != null =>
            closeAware.close_=(() => Viewport.closeWindowById(conf.id))
          case _ =>
            ()
        }
        contentComponent
      }
    }
  }
}

object Viewport {

  def viewport(init: Viewport ?=> Unit = {}): Viewport = {
    DslRuntime.build(new Viewport())(init)
  }

  def overlay(conf: OverlayConf)(init: Overlay ?=> Unit = {}): Overlay = {
    DslRuntime.build(new Overlay(conf))(init)
  }

  def notification(conf: NotificationConf)(init: Notification ?=> Unit = {}): Notification = {
    DslRuntime.build(new Notification(conf))(init)
  }

  val windows: ListProperty[WindowConf] = ListProperty()
  val notifications: ListProperty[NotificationConf] = ListProperty()
  val overlays: ListProperty[OverlayConf] = ListProperty()

  private val notificationFadeOutMs: Int = 250
  private val windowCloseAnimationMs: Int = 300

  enum NotificationKind(val cssClass: String) {
    case Info extends NotificationKind("info")
    case Success extends NotificationKind("success")
    case Warning extends NotificationKind("warning")
    case Error extends NotificationKind("error")
  }

  trait CloseAware {
    def close_=(callback: () => Unit): Unit
  }

  final class NotificationConf(
    val message: String,
    val kind: NotificationKind = NotificationKind.Info
  ) {
    val id: String = UUID.randomUUID().toString
    val visible: Property[Boolean] = Property(true)
  }

  final class WindowConf(
    val title: String,
    val width: Int = -1,
    val height: Int = -1,
    val component: () => Component | Null,
    val zIndex: Property[Int] = Property(0),
    val onClose: Option[Window => Unit] = None,
    val onClick: Option[Window => Unit] = None,
    val maximized: Property[Boolean] = Property(false),
    val resizable: Boolean = true,
    val draggable: Boolean = true,
    val centerOnOpen: Boolean = true,
    val rememberPosition: Boolean = true,
    val positionStorageKey: String | Null = null,
    val rememberSize: Boolean = true
  ) {
    val id: String = UUID.randomUUID().toString
  }

  final class OverlayConf(
    val anchor: HTMLElement,
    val content: () => Component | Null,
    val id: String = UUID.randomUUID().toString,
    val offsetXPx: Double = 0.0,
    val offsetYPx: Double = 0.0,
    val widthPx: Option[Double] = None,
    val minWidthPx: Option[Double] = None,
    val maxHeightPx: Option[Double] = None,
    val marginViewportPx: Double = 8.0,
    val flipY: Boolean = true,
    val zIndex: Int = 90000
  )

  def addOverlay(conf: OverlayConf): Unit =
    overlays += conf

  def closeOverlay(conf: OverlayConf): Unit =
    overlays -= conf

  def closeOverlayById(id: String): Unit =
    overlays.find(_.id == id).foreach(overlays -= _)

  def notify(
    message: String,
    kind: NotificationKind = NotificationKind.Info,
    durationMs: Int = 3000
  ): NotificationConf = {
    val conf = new NotificationConf(message = message, kind = kind)
    notifications += conf

    setTimeout(durationMs) {
      conf.visible.set(false)
    }

    setTimeout(durationMs + notificationFadeOutMs) {
      notifications -= conf
    }

    conf
  }

  def closeNotification(conf: NotificationConf): Unit = {
    conf.visible.set(false)
    setTimeout(notificationFadeOutMs) {
      notifications -= conf
    }
  }

  def isActive(conf: WindowConf): Boolean =
    windows.forall(other => other.eq(conf) || other.zIndex.get < conf.zIndex.get)

  def touchWindow(conf: WindowConf): Unit = {
    var index = 0
    windows.foreach { current =>
      current.zIndex.set(index)
      index += 1
    }

    conf.zIndex.set(index)
    conf.maximized.set(true)
  }

  def addWindow(conf: WindowConf): Unit = {
    windows += conf
    var index = 0
    windows.foreach { current =>
      current.zIndex.set(index)
      index += 1
    }

    browserWindow.setTimeout(() => {
      conf.zIndex.set(index)
    }, 100)
  }

  def closeWindow(conf: WindowConf): Unit = {
    conf.maximized.set(false)
    setTimeout(windowCloseAnimationMs) {
      windows -= conf
    }
  }

  def closeWindowById(id: String): Unit =
    windows.find(_.id == id).foreach(windows -= _)

  final class Overlay(conf: Viewport.OverlayConf) extends Box("div") {

    override def compose(): Unit = {
      given Component = this
      addClass("jfx-viewport-overlay")
      host.setStyle("z-index", conf.zIndex.toString)

      val stopClickListener: Event => Unit = _.stopPropagation()
      addDisposable(host.addEventListener("click", stopClickListener))

      addDisposable(
        followAnchorFixed(
          overlayElement = host.asInstanceOf[jfx.core.render.DomHostElement].element.asInstanceOf[HTMLElement],
          anchorElement = conf.anchor,
          offsetXPx = conf.offsetXPx,
          offsetYPx = conf.offsetYPx,
          widthPx = conf.widthPx,
          minWidthPx = conf.minWidthPx,
          maxHeightPx = conf.maxHeightPx,
          marginViewportPx = conf.marginViewportPx,
          flipY = conf.flipY
        )
      )

      if (conf.content != null) {
        conf.content()
      }
    }
  }

  final class Notification(conf: Viewport.NotificationConf) extends Box("div") {

    override def compose(): Unit = {
      given Component = this
      addClass("jfx-viewport-notification")
      addClass(s"jfx-viewport-notification--${conf.kind.cssClass}")
      
      text = conf.message

      addDisposable(conf.visible.observe(syncVisibleState))

      val clickListener: Event => Unit = _ => Viewport.closeNotification(conf)
      addDisposable(host.addEventListener("click", clickListener))
    }

    private def syncVisibleState(visible: Boolean): Unit = {
      given Component = this
      if (visible) {
        removeClass("is-hidden")
      } else {
        addClass("is-hidden")
      }
    }
  }

  private def followAnchorFixed(
    overlayElement: HTMLElement,
    anchorElement: HTMLElement,
    offsetXPx: Double,
    offsetYPx: Double,
    widthPx: Option[Double],
    minWidthPx: Option[Double],
    maxHeightPx: Option[Double],
    marginViewportPx: Double,
    flipY: Boolean
  ): Disposable = {
    var disposed = false
    var rafId: Option[Int] = None

    def applyPosition(): Unit = {
      if (disposed) return

      val anchorRect = anchorElement.getBoundingClientRect()
      val viewportWidth = browserWindow.innerWidth.toDouble
      val viewportHeight = browserWindow.innerHeight.toDouble

      val resolvedWidth = widthPx.getOrElse(anchorRect.width)

      val desiredLeft = anchorRect.left + offsetXPx
      val minLeft = marginViewportPx
      val maxLeft = viewportWidth - resolvedWidth - marginViewportPx
      val left =
        if (maxLeft <= minLeft) minLeft
        else desiredLeft.max(minLeft).min(maxLeft)

      val measuredOverlayHeight =
        Option.when(overlayElement.offsetHeight > 0)(overlayElement.offsetHeight.toDouble).getOrElse(0.0)

      val belowTop = anchorRect.bottom + offsetYPx
      val aboveTop = anchorRect.top - measuredOverlayHeight - offsetYPx

      val preferredTop =
        if (flipY && measuredOverlayHeight > 0) {
          val spaceBelow = viewportHeight - belowTop - marginViewportPx
          val spaceAbove = anchorRect.top - marginViewportPx

          if (spaceBelow < measuredOverlayHeight && spaceAbove > spaceBelow) aboveTop else belowTop
        } else {
          belowTop
        }

      val minTop = marginViewportPx
      val maxTop = viewportHeight - marginViewportPx
      val top =
        if (maxTop <= minTop) minTop
        else preferredTop.max(minTop).min(maxTop)

      overlayElement.style.left = s"${left}px"
      overlayElement.style.top = s"${top}px"

      widthPx match {
        case Some(width) =>
          overlayElement.style.width = s"${width}px"
          overlayElement.style.removeProperty("min-width")
        case None =>
          overlayElement.style.removeProperty("width")
          overlayElement.style.minWidth = s"${resolvedWidth}px"
      }

      minWidthPx match {
        case Some(width) => overlayElement.style.minWidth = s"${width}px"
        case None if widthPx.nonEmpty =>
          ()
        case None =>
          ()
      }

      maxHeightPx match {
        case Some(height) => overlayElement.style.maxHeight = s"${height}px"
        case None => overlayElement.style.removeProperty("max-height")
      }

      rafId = Some(browserWindow.requestAnimationFrame(_ => applyPosition()))
    }

    rafId = Some(browserWindow.requestAnimationFrame(_ => applyPosition()))

    () => {
      disposed = true
      rafId.foreach(browserWindow.cancelAnimationFrame)
    }
  }
}
