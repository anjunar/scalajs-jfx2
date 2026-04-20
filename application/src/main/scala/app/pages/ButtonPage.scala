package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import org.scalajs.dom
import app.components.Showcase.*

object ButtonPage {
  def render() = {
    showcasePage("Button", "Der Puls deiner Anwendung.") {
      vbox {
        style { gap = "24px" }
        componentShowcase("Standard Button") {
          div { 
            style { marginBottom = "12px"; opacity = "0.8" }
            text = "Buttons sind das Herzstück der Interaktion. Sie sind nicht nur Klick-Ziele, sondern bringen deine App zum Leben."
          }
          button("Klick mich und erwecke mich zum Leben") {
            onClick { _ => dom.window.alert("Ich wurde geklickt! Die Magie beginnt.") }
          }
        }
        apiSection("Die Einfachheit der DSL") {
          codeBlock("scala", "button(\"Klick mich\") {\n  onClick { _ => println(\"Geklickt\") }\n}")
        }
      }
    }
  }
}
