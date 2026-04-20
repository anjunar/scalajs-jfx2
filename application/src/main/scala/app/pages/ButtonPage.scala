package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.layout.VBox.vbox
import org.scalajs.dom
import app.components.Showcase.*

object ButtonPage {
  def render() = {
    showcasePage("Button", "Die primäre Aktions-Komponente.") {
      vbox {
        style { gap = "24px" }
        componentShowcase("Standard Button") {
          button("Klick mich") {
            onClick { _ => dom.window.alert("Button geklickt!") }
          }
        }
        apiSection("Usage") {
          codeBlock("scala", "button(\"Klick mich\") {\n  onClick { _ => println(\"Geklickt\") }\n}")
        }
      }
    }
  }
}
