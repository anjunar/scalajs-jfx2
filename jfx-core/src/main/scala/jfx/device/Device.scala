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
  enum Mode {
    case Mobile
    case Desktop
  }

  private val mobileCookieName = "mobile"

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

  def set(mode: Mode): Unit =
    modeProperty.set(mode)

  private[device] def syncFromEnvironment(): Unit =
    modeProperty.set(resolveCurrentMode())

  private def resolveInitialMode(): Mode =
    resolveCurrentMode()

  private def resolveCurrentMode(): Mode =
    currentCookieMobile()
      .map {
        case true  => Mode.Mobile
        case false => Mode.Desktop
      }
      .getOrElse(Mode.Desktop)

  private def currentCookieMobile(): Option[Boolean] =
    currentCookieString()
      .flatMap(cookieValue(_, mobileCookieName))
      .flatMap(parseBoolean)

  private def currentCookieString(): Option[String] =
    SsrContext.currentCookie.orElse(browserCookieString())

  private def browserCookieString(): Option[String] =
    if (isBrowser) {
      Option(dom.document.cookie).filter(_.nonEmpty)
    } else {
      None
    }

  private def cookieValue(cookieHeader: String, name: String): Option[String] =
    cookieHeader
      .split(";")
      .iterator
      .map(_.trim)
      .collectFirst {
        case entry if entry.startsWith(s"$name=") =>
          entry.drop(name.length + 1)
      }

  private def parseBoolean(value: String): Option[Boolean] =
    value.trim.toLowerCase match {
      case "true" | "1" | "yes" | "y" | "mobile" | "m" =>
        Some(true)
      case "false" | "0" | "no" | "n" =>
        Some(false)
      case _ =>
        None
    }

  private def isBrowser: Boolean =
    js.typeOf(js.Dynamic.global.document) != "undefined" &&
      js.typeOf(js.Dynamic.global.window) != "undefined"
}