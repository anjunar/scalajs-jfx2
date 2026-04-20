package app.pages

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HorizontalLine.horizontalLine
import app.components.Showcase.*

object OverviewPage {
  def render() = {
    showcasePage("Willkommen bei JFX2", "Deinem neuen Zuhause für reaktive UIs in Scala.js.") {
      vbox {
        classes = "showcase-intro"
        div {
          classes = "showcase-intro__text"
          text = "JFX2 verbindet die Eleganz einer JavaFX-inspirierten DSL mit der Power moderner Web-Technologien. Unser Herz schlägt für kompromisslose SSR-Kompatibilität, starke Typisierung und architektonische Klarheit. Lass die Magie expliziter Komposition wirken und entdecke, wie fließend, lebendig und intuitiv UI-Entwicklung wirklich sein kann."
        }
        horizontalLine()
        div {
          classes = "showcase-intro__links"
          text = "Tauche ein: Wähle eine Komponente aus der Sidebar und erlebe unsere Live-Beispiele in Aktion."
        }
      }
    }
  }
}
