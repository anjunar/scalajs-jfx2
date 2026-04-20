package app.pages

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object LayoutPage {
  def render() = {
    showcasePage("Layout Components", "HBox, VBox und Div zur Strukturierung.") {
      vbox {
        style { gap = "24px" }
        componentShowcase("HBox & VBox") {
          vbox {
            style { gap = "10px" }
            hbox {
              style { gap = "10px" }
              div { classes = "demo-box"; text = "H1" }
              div { classes = "demo-box"; text = "H2" }
            }
            vbox {
              style { gap = "5px" }
              div { classes = "demo-box"; text = "V1" }
              div { classes = "demo-box"; text = "V2" }
            }
          }
        }
      }
    }
  }
}
