package app.pages

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object LayoutPage {
  def render() = {
    showcasePage("Layout & Struktur", "Die Architektur deines digitalen Raumes.") {
      vbox {
        style { gap = "24px" }
        div {
          style { opacity = "0.8"; fontSize = "14px"; marginBottom = "8px" }
          text = "Gutes Design atmet durch seine Struktur. Mit VBox und HBox komponierst du mühelos klare Zonen und eine ruhige, aufgeräumte Oberfläche, die den Nutzer sanft führt – ganz ohne dekorative Komplexität."
        }
        componentShowcase("Elegantes Box-Layout") {
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
        apiSection("VBox & HBox Usage") {
          codeBlock("scala", """vbox {
  style { gap = "10px" }
  
  hbox {
    div { text = "Left" }
    div { text = "Right" }
  }
}""")
        }
      }
    }
  }
}
