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
        style { gap = "34px" }

        sectionIntro(
          "Komposition",
          "Layout ist die Grammatik der Oberfläche.",
          "VBox und HBox sind absichtlich schlicht. Sie erzwingen keine fremde Abstraktion, sondern machen die räumliche Struktur direkt im Template sichtbar."
        )

        metricStrip(
          "VBox" -> "Vertikale Ordnung für Formulare, Panels und Seiten.",
          "HBox" -> "Horizontale Gruppen für Toolbar, Aktionen und kurze Zeilen.",
          "Div" -> "Neutrale Fläche für semantische oder visuelle Spezialisierung."
        )

        componentShowcase(
          "App-Shell Skizze",
          "Ein dichteres Layout zeigt, wie aus wenigen Bausteinen Navigation, Inhalt und Detailbereich entstehen."
        ) {
          hbox {
            classes = "layout-shell-demo"
            vbox {
              classes = "layout-shell-demo__rail"
              div { classes = "layout-shell-demo__brand"; text = "JFX2" }
              div { classes = "layout-shell-demo__nav is-active"; text = "Komponenten" }
              div { classes = "layout-shell-demo__nav"; text = "Formulare" }
              div { classes = "layout-shell-demo__nav"; text = "Daten" }
            }
            vbox {
              classes = "layout-shell-demo__content"
              div { classes = "layout-shell-demo__headline"; text = "Showcase Fläche" }
              div { classes = "layout-shell-demo__copy"; text = "Links führt die Navigation, rechts bleibt Raum für die aktive Komponente und ihre Erklärung." }
              hbox {
                classes = "layout-shell-demo__tiles"
                div { classes = "layout-shell-demo__tile"; text = "Live Demo" }
                div { classes = "layout-shell-demo__tile"; text = "API" }
                div { classes = "layout-shell-demo__tile"; text = "Hinweise" }
              }
            }
          }
        }

        componentShowcase(
          "Elegantes Box-Layout",
          "Das Grundprinzip bleibt klein und nachvollziehbar: Container schachteln, Abstand setzen, Inhalt platzieren."
        ) {
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

        insightGrid(
          ("Lesbarkeit", "Die Struktur liest sich von außen nach innen", "Erst kommt die Seite, dann die Zone, dann die konkrete Zeile oder Spalte."),
          ("Stabilität", "Abstände bleiben an Containern", "Gap und Padding beschreiben den Raum, nicht jedes einzelne Kind."),
          ("Erweiterung", "Neue Bereiche bleiben lokal", "Ein späteres Panel fügt sich als weiterer Container ein, ohne bestehende Elemente umzubauen.")
        )

        apiSection(
          "VBox & HBox Usage",
          "Die Layout-DSL bleibt nahe am mentalen Modell einer UI-Skizze."
        ) {
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
