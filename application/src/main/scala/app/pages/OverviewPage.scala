package app.pages

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HorizontalLine.horizontalLine
import app.components.Showcase.*

object OverviewPage {
  def render() = {
    showcasePage("Overview", "Willkommen zur JFX2 API Dokumentation.") {
      vbox {
        classes = "showcase-intro"
        div {
          classes = "showcase-intro__text"
          text = "JFX2 ist ein reaktives UI-Framework für Scala.js mit Fokus auf SSR-Kompatibilität, Typisierung und architektonische Klarheit."
        }
        horizontalLine()
        div {
          classes = "showcase-intro__links"
          text = "Wähle eine Komponente aus der Sidebar, um Details und Live-Beispiele zu sehen."
        }
      }
    }
  }
}
