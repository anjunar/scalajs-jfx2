package app.pages

import app.components.Showcase.*
import jfx.core.component.Component.*
import jfx.form.Editor.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

object EditorPage {
  def render(): Unit = {
    showcasePage("Editor", "Lexical als Client-Island in der JFX2 DSL.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Client Side Island",
          "SSR bleibt stabil, Lexical startet erst im Browser.",
          "Der erste Port-Schritt rendert serverseitig einen Fallback und ersetzt ihn nach Hydration durch eine clientseitige Mount-Fläche. So können wir den Editor schrittweise aufbauen, ohne Hydration-Mismatches zu riskieren."
        )

        componentShowcase(
          "Editor Playground",
          "Noch kein echter Lexical-Mount, sondern der kleinste Test für Fallback und Client-Aktivierung."
        ) {
          editor("editor-playground", standalone = true) {
            placeholder = "Schreibfläche wird clientseitig aktiviert"
            style {
              width = "100%"
              minHeight = "320px"
            }
          }
        }

        noteBlock(
          "Nächster Schritt",
          "Wenn diese ClientSideComponent-Schicht stabil bleibt, kann der echte LexicalBuilder in mountClient() einziehen. Der SSR-Fallback bleibt weiterhin die serverseitige Wahrheit."
        )

        apiSection(
          "DSL Syntax",
          "Die API bleibt bewusst nah am ursprünglichen Editor."
        ) {
          codeBlock("scala", """editor("content", standalone = true) {
  placeholder = "Text schreiben..."
  style { minHeight = "320px" }
}""")
        }
      }
    }
  }
}
