package app.pages

import app.DemoI18n
import jfx.action.Button.button
import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.layout.Viewport.*
import org.scalajs.dom
import app.components.Showcase.*

object WindowPage {
  def render() = {
    showcasePage(i18n"Window & viewport", i18n"Architecture of space.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Space management",
          i18n"The viewport is the stage for things that live above the page.",
          i18n"Notifications, windows, and overlays need a central order. JFX2 gathers these concerns in the viewport so pages do not have to manage global UI layers on their own."
        )

        metricStrip(
          i18n"Notify" -> i18n"Short feedback without losing context.",
          i18n"Window" -> i18n"Focused work surfaces above the page.",
          i18n"Overlay" -> i18n"Anchor-bound surfaces for selection and details."
        )
        
        componentShowcase(
          i18n"Viewport",
          i18n"Four notification types show how global feedback is triggered from a page."
        ) {
          div {
            style { marginBottom = "12px"; opacity = "0.8" }
            text = DemoI18n.text(i18n"The viewport is the quiet center that carries windows, notifications, and overlays. It brings order to the chaos and gives it a stage.")
          }
          
          vbox {
             style { gap = "12px" }
             
             button(DemoI18n.text(i18n"Info notification")) {
               onClick { _ => 
                 Viewport.notify("Silence is the origin of every form.", NotificationKind.Info)
               }
             }

             button(DemoI18n.text(i18n"Success notification")) {
               onClick { _ => 
                 Viewport.notify("The structure is now sound.", NotificationKind.Success)
               }
             }

             button(DemoI18n.text(i18n"Warning notification")) {
               onClick { _ => 
                 Viewport.notify("Warning: the form may be hardening.", NotificationKind.Warning)
               }
             }

             button(DemoI18n.text(i18n"Error notification")) {
               onClick { _ => 
                 Viewport.notify("A crack in the foundation was discovered.", NotificationKind.Error)
               }
             }
          }
        }

        componentShowcase(
          i18n"Window",
          i18n"A window remains in the global viewport while the page underneath keeps its state."
        ) {
          div {
            style { marginBottom = "12px"; opacity = "0.8" }
            text = DemoI18n.text(i18n"Windows are movable islands in the viewport. They allow focus without losing context.")
          }

          button(DemoI18n.text(i18n"Open window")) {
            onClick { _ =>
              Viewport.addWindow(new WindowConf(
                title = "A room for thoughts",
                width = 400,
                height = 300,
                component = () => {
                  vbox {
                    style { padding = "20px"; gap = "12px" }
                    div { text = DemoI18n.text(i18n"There is room for your ideas here.") }
                    button(DemoI18n.text(i18n"Confirm note")) {
                       onClick { _ =>
                         Viewport.notify("The note in the window was confirmed.", NotificationKind.Success)
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
          i18n"Usage",
          i18n"The page binds the viewport once and then only sends clear intents."
        ) {
          codeBlock("scala", """// Viewport in der App-Shell platzieren
viewport {
  router(routes)
}

// Benachrichtigung senden
Viewport.notify("Hello world", NotificationKind.Success)

// Fenster hinzufügen
Viewport.addWindow(new WindowConf(
  title = "My window",
  component = () => new MyContent()
))""")
        }
      }
    }
  }
}
