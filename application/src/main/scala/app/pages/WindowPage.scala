package app.pages

import jfx.action.Button.button
import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.layout.Viewport.*
import org.scalajs.dom
import app.components.Showcase.*

object WindowPage {
  def render() = {
    showcasePage("Window & Viewport", "Architektur des Raums.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Raumverwaltung",
          "Der Viewport ist die Bühne für Dinge, die über der Seite liegen.",
          "Benachrichtigungen, Fenster und Overlays brauchen eine zentrale Ordnung. JFX2 sammelt diese Aufgaben im Viewport, damit einzelne Seiten nicht selbst globale UI-Schichten verwalten müssen."
        )

        metricStrip(
          "Notify" -> "Kurze Rückmeldungen ohne Kontextverlust.",
          "Window" -> "Fokussierte Arbeitsflächen über der Seite.",
          "Overlay" -> "Ankergebundene Flächen für Auswahl und Details."
        )
        
        componentShowcase(
          "Viewport",
          "Vier Benachrichtigungstypen zeigen, wie globale Rückmeldung aus einer Seite heraus ausgelöst wird."
        ) {
          div {
            style { marginBottom = "12px"; opacity = "0.8" }
            text = "Der Viewport ist das stille Zentrum, das Fenster, Benachrichtigungen und Overlays trägt. Er ordnet das Chaos und gibt ihm eine Bühne."
          }
          
          vbox {
             style { gap = "12px" }
             
             button("Info Benachrichtigung") {
               onClick { _ => 
                 Viewport.notify("Die Stille ist der Ursprung aller Form.", NotificationKind.Info)
               }
             }

             button("Erfolg Benachrichtigung") {
               onClick { _ => 
                 Viewport.notify("Die Struktur ist nun tragfähig.", NotificationKind.Success)
               }
             }

             button("Warnung Benachrichtigung") {
               onClick { _ => 
                 Viewport.notify("Achtung: Die Form könnte sich verhärten.", NotificationKind.Warning)
               }
             }

             button("Fehler Benachrichtigung") {
               onClick { _ => 
                 Viewport.notify("Ein Riss im Fundament wurde entdeckt.", NotificationKind.Error)
               }
             }
          }
        }

        componentShowcase(
          "Fenster",
          "Ein Fenster bleibt im globalen Viewport, während die Seite darunter ihren Zustand behält."
        ) {
          div {
            style { marginBottom = "12px"; opacity = "0.8" }
            text = "Fenster sind bewegliche Inseln im Viewport. Sie erlauben Fokus, ohne den Kontext zu verlieren."
          }

          button("Fenster öffnen") {
            onClick { _ =>
              Viewport.addWindow(new WindowConf(
                title = "Ein Raum für Gedanken",
                width = 400,
                height = 300,
                component = () => {
                  vbox {
                    style { padding = "20px"; gap = "12px" }
                    div { text = "Hier ist Platz für deine Ideen." }
                    button("Notiz bestätigen") {
                       onClick { _ =>
                         Viewport.notify("Die Notiz im Fenster wurde bestätigt.", NotificationKind.Success)
                       }
                    }
                  }
                }
              ))
            }
          }
        }

        insightGrid(
          ("Zentrum", "Globale UI gehört an einen Ort", "Viewport.windows und Viewport.notifications bilden den Zustand der obersten UI-Schicht."),
          ("Fokus", "Fenster erhalten Z-Index und Aktivität", "Der Viewport kann Fenster berühren, ordnen und schließen, ohne Seitenlogik zu duplizieren."),
          ("Lesbarkeit", "Seiten lösen nur Absichten aus", "Die Seite sagt notify oder addWindow, der Viewport kümmert sich um Darstellung und Lebenszyklus.")
        )

        apiSection(
          "Nutzung",
          "Die Seite bindet den Viewport einmal ein und sendet danach nur noch klare Absichten."
        ) {
          codeBlock("scala", """// Viewport in der App-Shell platzieren
viewport {
  router(routes)
}

// Benachrichtigung senden
Viewport.notify("Hallo Welt", NotificationKind.Success)

// Fenster hinzufügen
Viewport.addWindow(new WindowConf(
  title = "Mein Fenster",
  component = () => new MyContent()
))""")
        }
      }
    }
  }
}
