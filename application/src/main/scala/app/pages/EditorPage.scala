package app.pages

import app.components.Showcase.*
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import scala.scalajs.js

object EditorPage {
  def render(): Unit = {
    val initialEditorText = "Dieser Text kommt bereits aus dem SSR-Fallback und wird nach der Hydration von Lexical übernommen."
    val mirroredEditorValue = Property[js.Any | Null](initialEditorText)

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
            val writableEditor = summon[jfx.form.Editor]
            value = mirroredEditorValue.get
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
            addDisposable(writableEditor.valueProperty.observeWithoutInitial { nextValue =>
              mirroredEditorValue.set(nextValue)
            })
          }
        }

        componentShowcase(
          "Readonly Editor",
          "Spiegelt den ersten Editor live, aber mit editable = false: keine Toolbar und kein editierbarer Root."
        ) {
          editor("editor-readonly", standalone = true) {
            val readonlyEditor = summon[jfx.form.Editor]
            value = mirroredEditorValue.get
            editable = false
            placeholder = "Readonly"
            style {
              width = "100%"
              minHeight = "220px"
            }
            basePlugin()
            headingPlugin()
            listPlugin()
            linkPlugin()
            imagePlugin()
            tablePlugin()
            codePlugin()
            addDisposable(mirroredEditorValue.observeWithoutInitial { nextValue =>
              readonlyEditor.valueProperty.set(nextValue)
            })
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
}

editor("readonly", standalone = true) {
  value = mirroredEditorValue.get
  editable = false
}""")
        }
      }
    }
  }
}
