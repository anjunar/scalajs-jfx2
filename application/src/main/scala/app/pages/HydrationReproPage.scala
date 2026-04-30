package app.pages

import app.components.Showcase.*
import app.domain.BlogDraft
import jfx.core.component.Component.*
import jfx.form.Editor.{editor, placeholder as editorPlaceholder, value as editorValue}
import jfx.form.Form.form
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.editor.plugins.{basePlugin, headingPlugin, listPlugin}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

import scala.scalajs.js

object HydrationReproPage {
  def render(): Unit = {
    val draft = new BlogDraft()
    draft.title.set("Hydrated title")
    draft.content.set(lexicalState("Hydrated editor body"))

    showcasePage("Hydration repro", "Direct-load form hydration with an editor client island.") {
      vbox {
        style { gap = "24px" }

        componentShowcase(
          "Form-bound editor",
          "Open this route directly. The title input and editor should both adopt the server-loaded model after hydration."
        ) {
          form(draft) {
            vbox {
              style { gap = "16px" }

              inputContainer("Title") {
                input("title") {}
              }

              inputContainer("Content") {
                editor("content") {
                  editorPlaceholder = "Editor placeholder before model content"
                  style {
                    width = "100%"
                    minHeight = "260px"
                  }

                  basePlugin()
                  headingPlugin()
                  listPlugin()
                }
              }

              div {
                classes = Seq("showcase-result")
                text = "Expected after direct hydration: title input = Hydrated title, editor text = Hydrated editor body."
              }
            }
          }
        }

        apiSection("Minimal shape") {
          codeBlock("scala", """val draft = new BlogDraft()
draft.title.set("Hydrated title")
draft.content.set(lexicalState("Hydrated editor body"))

form(draft) {
  inputContainer("Title") {
    input("title") {}
  }

  inputContainer("Content") {
    editor("content") {
      placeholder = "Editor placeholder before model content"
      basePlugin()
      headingPlugin()
      listPlugin()
    }
  }
}""")
        }
      }
    }
  }

  private def lexicalState(text: String): js.Any =
    js.Dynamic.literal(
      root = js.Dynamic.literal(
        `type` = "root",
        version = 1,
        indent = 0,
        children = js.Array(
          js.Dynamic.literal(
            `type` = "paragraph",
            version = 1,
            indent = 0,
            children = js.Array(
              js.Dynamic.literal(
                `type` = "text",
                version = 1,
                text = text,
                detail = 0,
                format = 0,
                mode = "normal"
              )
            )
          )
        )
      )
    )
}
