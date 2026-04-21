package app

import jfx.core.render.RenderBackend
import jfx.core.state.Property
import org.scalajs.dom

import scala.util.control.NonFatal

object Theme {
  enum Mode(val value: String) {
    case Light extends Mode("light")
    case Dark extends Mode("dark")
  }

  val modeProperty: Property[Mode] = Property(Mode.Light)

  def mode: Mode =
    modeProperty.get

  def mode_=(value: Mode): Unit =
    set(value)

  def set(value: Mode): Unit = {
    modeProperty.set(value)
    applyToDocument(value)
    persist(value)
  }

  def syncFromDocument(): Unit =
    if (!RenderBackend.current.isServer) {
      try {
        dom.document.documentElement.getAttribute("data-theme") match {
          case "dark"  => modeProperty.set(Mode.Dark)
          case "light" => modeProperty.set(Mode.Light)
          case _       => ()
        }
      } catch {
        case NonFatal(_) => ()
      }
    }

  private def applyToDocument(value: Mode): Unit =
    if (!RenderBackend.current.isServer) {
      try {
        dom.document.documentElement.setAttribute("data-theme", value.value)
        Option(dom.document.querySelector("meta[name='theme-color']")).foreach { meta =>
          meta.setAttribute("content", if (value == Mode.Dark) "#171918" else "#eee9e1")
        }
      } catch {
        case NonFatal(_) => ()
      }
    }

  private def persist(value: Mode): Unit =
    if (!RenderBackend.current.isServer) {
      try dom.window.localStorage.setItem("scalajs-jfx2.theme", value.value)
      catch { case NonFatal(_) => () }
    }
}
