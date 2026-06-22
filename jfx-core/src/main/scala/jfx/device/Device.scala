package jfx.device

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.ssr.context.SsrContext
import org.scalajs.dom

import scala.scalajs.js

final class Device extends Component {
  override def tagName: String = ""

  override def compose(): Unit =
    Device.syncFromEnvironment()
}

object Device {
  enum Mode(val cookieValue: String) {
    case Mobile extends Mode("mobile")
    case Desktop extends Mode("desktop")
  }

  private val cookieName = "jfx-device"
  private val viewportBreakpointPx = 720

  val modeProperty: Property[Mode] = Property(resolveInitialMode())
  val isMobileProperty: ReadOnlyProperty[Boolean] = modeProperty.map(_ == Mode.Mobile)
  val isDesktopProperty: ReadOnlyProperty[Boolean] = modeProperty.map(_ == Mode.Desktop)

  def device(init: Device ?=> Unit = {}): Device = {
    val component = new Device()
    DslRuntime.provide(component) {
      DslRuntime.build(component)(init)
    }
    component
  }

  def mode: Mode = modeProperty.get

  def isMobile: Boolean = mode == Mode.Mobile

  def isDesktop: Boolean = mode == Mode.Desktop

  def set(mode: Mode): Unit = {
    modeProperty.set(mode)
    persistCookie(mode)
  }

  private[device] def syncFromEnvironment(): Unit = {
    val next = resolveCurrentMode()
    modeProperty.set(next)
    persistCookie(next)
  }

  private def resolveInitialMode(): Mode =
    resolveCurrentMode()

  private def resolveCurrentMode(): Mode = {
    currentCookieMode()
      .orElse(browserViewportMode())
      .getOrElse(Mode.Desktop)
  }

  private def currentCookieMode(): Option[Mode] =
    currentCookieString().flatMap(parseCookieHeader)

  private def currentCookieString(): Option[String] =
    SsrContext.currentCookie.orElse(browserCookieString())

  private def browserCookieString(): Option[String] =
    if (isBrowser) {
      Option(dom.document.cookie).filter(_.nonEmpty)
    } else {
      None
    }

  private def browserViewportMode(): Option[Mode] =
    if (isBrowser) {
      Option.when(dom.window.innerWidth.toDouble <= viewportBreakpointPx)(Mode.Mobile).orElse(Some(Mode.Desktop))
    } else {
      None
    }

  private def persistCookie(mode: Mode): Unit = {
    if (isBrowser) {
      val cookie = s"$cookieName=${mode.cookieValue}; path=/; max-age=31536000; samesite=lax"
      dom.document.cookie = cookie
    }
  }

  private def parseCookieHeader(cookieHeader: String): Option[Mode] =
    cookieHeader
      .split(";")
      .iterator
      .map(_.trim)
      .collectFirst {
        case entry if entry.startsWith(s"$cookieName=") =>
          parseCookieValue(entry.drop(cookieName.length + 1))
      }
      .flatten

  private def parseCookieValue(value: String): Option[Mode] =
    value.trim.toLowerCase match {
      case "mobile" | "m"   => Some(Mode.Mobile)
      case "desktop" | "d"  => Some(Mode.Desktop)
      case _                => None
    }

  private def isBrowser: Boolean =
    js.typeOf(js.Dynamic.global.document) != "undefined" &&
      js.typeOf(js.Dynamic.global.window) != "undefined"
}
