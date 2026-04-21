package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import org.scalajs.dom
import app.components.Showcase.*

object ButtonPage {
  def render() = {
    showcasePage("Button", "Der Puls deiner Anwendung.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Interaktion",
          "Ein Button ist klein, aber er trägt Verantwortung.",
          "In JFX2 bleibt die Aktion im Template sichtbar: Label, Ereignis und Umgebung stehen direkt beieinander. Das macht einfache Buttons leicht lesbar und komplexere Workflows später gut erweiterbar."
        )

        componentShowcase(
          "Standard Button",
          "Ein fokussierter Klickpunkt mit direkter Event-Anbindung. Ideal für klare, abgeschlossene Aktionen."
        ) {
          div { 
            style { marginBottom = "12px"; opacity = "0.8" }
            text = "Buttons sind das Herzstück der Interaktion. Sie sind nicht nur Klick-Ziele, sondern bringen deine App zum Leben."
          }
          button("Klick mich und erwecke mich zum Leben") {
            onClick { _ => dom.window.alert("Ich wurde geklickt! Die Magie beginnt.") }
          }
        }

        componentShowcase(
          "Aktionsgruppe",
          "Mehrere Buttons dürfen nah beieinander liegen, solange ihre Absicht unterscheidbar bleibt."
        ) {
          hbox {
            classes = "showcase-action-row"
            button("Speichern") {
              onClick { _ => dom.window.alert("Gespeichert.") }
            }
            button("Prüfen") {
              onClick { _ => dom.window.alert("Alles geprüft.") }
            }
            button("Zurücksetzen") {
              onClick { _ => dom.window.alert("Zurückgesetzt.") }
            }
          }
        }

        insightGrid(
          ("Zustand", "Der Button sagt, was passiert", "Ein gutes Label beschreibt die nächste Aktion, nicht die technische Implementierung dahinter."),
          ("Ereignis", "onClick bleibt lokal", "Die DSL macht den Auslöser und die Reaktion an derselben Stelle sichtbar."),
          ("Feedback", "Aktionen brauchen Antwort", "Nach dem Klick sollte die Oberfläche eine sichtbare Reaktion geben: Meldung, Status, Navigation oder Datenupdate.")
        )

        apiSection(
          "Die Einfachheit der DSL",
          "Der Kern bleibt bewusst knapp: Button erzeugen, Handler binden, fertig."
        ) {
          codeBlock("scala", "button(\"Klick mich\") {\n  onClick { _ => println(\"Geklickt\") }\n}")
        }
      }
    }
  }
}
