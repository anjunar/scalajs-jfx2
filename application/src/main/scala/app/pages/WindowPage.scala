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
        style { gap = "24px" }
        
        componentShowcase("Viewport") {
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

        componentShowcase("Fenster") {
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
                    button("Schließen") {
                       // Note: In a real app, we might use CloseAware trait
                       onClick { _ => 
                         // We need a way to close this specific window.
                         // WindowConf has an ID, we can use that.
                       }
                    }
                  }
                }
              ))
            }
          }
        }

        apiSection("Nutzung") {
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
