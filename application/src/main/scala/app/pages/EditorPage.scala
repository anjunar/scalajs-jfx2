package app.pages

import app.components.Showcase.*
import jfx.core.component.Component.*
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
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
          "Echter Lexical-Mount mit Toolbar-Renderer und allen Editor-Plugins."
        ) {
          editor("editor-playground", standalone = true) {
            value = "Dieser Text kommt bereits aus dem SSR-Fallback und wird nach der Hydration von Lexical übernommen."
            placeholder = "Schreibfläche wird clientseitig aktiviert"
            style {
              width = "100%"
              minHeight = "320px"
            }
            basePlugin()
            headingPlugin()
            listPlugin()
            linkPlugin()
            imagePlugin()
            tablePlugin()
            codePlugin()
          }
        }

        noteBlock(
          "Nächster Schritt",
          "Jetzt können wir die einzelnen Dialoge und Spezialknoten im Browser gezielt polieren: Link, Image, Table und CodeMirror."
        )

        apiSection(
          "DSL Syntax",
          "Die API bleibt bewusst nah am ursprünglichen Editor."
        ) {
          codeBlock("scala", """editor("content", standalone = true) {
  value = "Dieser Text ist schon im SSR sichtbar."
  placeholder = "Text schreiben..."
  style { minHeight = "320px" }
  basePlugin()
  headingPlugin()
  listPlugin()
  linkPlugin()
  imagePlugin()
  tablePlugin()
  codePlugin()
}""")
        }
      }
    }
  }
}
