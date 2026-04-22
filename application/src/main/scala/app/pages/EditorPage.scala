package app.pages

import app.components.Showcase.*
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.Property
import app.DemoI18n
import jfx.i18n.*
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.scalajs.js

object EditorPage {
  def render(): Unit = {
    val initialEditorText =
      "This text already comes from the SSR fallback and is adopted by Lexical after hydration."
    val mirroredEditorValue = Property[js.Any | Null](initialEditorText)

    showcasePage(i18n"Editor", i18n"Lexical as an SSR-safe client island.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Lexical island",
          i18n"The editor renders content immediately and only becomes Lexical in the browser.",
          i18n"JFX2 treats the editor as a ClientSideComponent: SSR gets a stable fallback with the same shell layout, hydration replaces the surface with Lexical, and external property changes flow back into the editor."
        )

        metricStrip(
          i18n"SSR" -> i18n"Text is visible before hydration.",
          i18n"Toolbar" -> i18n"Plugins deliver their controls through the renderer.",
          i18n"Readonly" -> i18n"editable = false hides the toolbar and locks Lexical."
        )

        componentShowcase(
          i18n"Live editor playground",
          i18n"Write, use the toolbar, and see the readonly mirror directly below."
        ) {
          vbox {
            style { gap = "16px" }

            editor("editor-playground", standalone = true) {
              val writableEditor = summon[jfx.form.Editor]
              value = mirroredEditorValue.get
              placeholder = "The writing surface activates on the client"
              style {
                width = "100%"
                minHeight = "340px"
              }
              installDefaultPlugins()
              addDisposable(writableEditor.valueProperty.observeWithoutInitial { nextValue =>
                mirroredEditorValue.set(nextValue)
              })
              addDisposable(mirroredEditorValue.observeWithoutInitial { nextValue =>
                writableEditor.valueProperty.set(nextValue)
              })
            }

            hbox {
              style { gap = "10px"; flexWrap = "wrap" }

              button(DemoI18n.text(i18n"SSR sample text")) {
                onClick { _ =>
                  mirroredEditorValue.set(initialEditorText)
                }
              }

              button(DemoI18n.text(i18n"Short text")) {
                onClick { _ =>
                  mirroredEditorValue.set("Kurzer externer Property-Update. Der Editor übernimmt ihn nach der Hydration.")
                }
              }

              button(DemoI18n.text(i18n"Set readonly content")) {
                onClick { _ =>
                  mirroredEditorValue.set("Dieser Wert wurde außerhalb des Editors gesetzt und wird in beide Instanzen synchronisiert.")
                }
              }
            }
          }
        }

        componentShowcase(
          i18n"Readonly mirror",
          i18n"The same editor component, but with editable = false: no toolbar, no editable root, same content."
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
            installDefaultPlugins()
            addDisposable(mirroredEditorValue.observeWithoutInitial { nextValue =>
              readonlyEditor.valueProperty.set(nextValue)
            })
          }
        }

        componentShowcase(
          i18n"SSR fallback contract",
          i18n"What is rendered on the server must structurally match the first client render."
        ) {
          hbox {
            style { gap = "14px"; flexWrap = "wrap"; alignItems = "stretch" }
            detailCard(
              "Shell stays stable",
              "Fallback and client version use the same jfx-editor shell with toolbar area and surface frame."
            )
            detailCard(
              "Toolbar does not flicker",
              "Readonly renders the toolbar hidden on the server so hydration does not have to surprise the structure."
            )
            detailCard(
              "Text stays visible",
              "Plain text or Lexical JSON is extracted into preview text during SSR and then adopted by Lexical after hydration."
            )
            detailCard(
              "External values",
              "valueProperty updates are synchronized back into Lexical after mount via parseEditorState."
            )
          }
        }

        componentShowcase(
          i18n"Plugin set",
          i18n"The showcase loads all ported plugins so toolbar and dialogs are visible together."
        ) {
          hbox {
            style { gap = "14px"; flexWrap = "wrap"; alignItems = "stretch" }
            pluginCard("Base", "Bold, Italic, Underline und Basis-Kommandos.")
            pluginCard("Heading", "Absätze und Überschriften über Dropdown.")
            pluginCard("List", "Bullet- und Numbered-List Kommandos.")
            pluginCard("Link", "Dialog-Service für Link-Einfügen.")
            pluginCard("Image", "Image-Dialog als Editor-Plugin.")
            pluginCard("Table", "Tabellen-Knoten und Toolbar-Aktion.")
            pluginCard("Code", "Code-Block Plugin als Spezialknoten.")
          }
        }

        insightGrid(
          (i18n"Island", i18n"JavaScript only in the browser", i18n"SSR does not call Lexical and stays free of document/window access."),
          (i18n"Fallback", i18n"No empty editor", i18n"Users and crawlers see content even before Lexical is mounted."),
          (i18n"Editable", i18n"One control contract", i18n"The editor follows the same editable pattern as Input, ComboBox, and Cropper.")
        )

        apiSection(
          i18n"DSL syntax",
          i18n"The editor remains a normal form control with value, placeholder, editable state, and plugins."
        ) {
          codeBlock("scala", """editor("content", standalone = true) {
  value = "Dieser Text ist schon im SSR sichtbar."
  placeholder = "Text schreiben..."
  style { minHeight = "340px" }

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

        apiSection(
          i18n"Client island flow",
          i18n"The component separates SSR fallback, client mount, and Lexical synchronization."
        ) {
          codeBlock("text", """SSR:
  renderFallbackContent()
  Preview-Text aus valueProperty
  Toolbar sichtbar nur wenn editable = true

Hydration:
  mountClient()
  mountLexical()
  renderToolbar(editor)
  installPlugins(editor)

Property Update:
  valueProperty.observeWithoutInitial(syncExternalValue)
  lexicalEditor.parseEditorState(...)
  lexicalEditor.setEditorState(...)""")
        }
      }
    }
  }

  private def installDefaultPlugins()(using jfx.form.Editor): Unit = {
    basePlugin()
    headingPlugin()
    listPlugin()
    linkPlugin()
    imagePlugin()
    tablePlugin()
    codePlugin()
  }

  private def detailCard(title: String, body: String): Unit = {
    vbox {
      classes = "showcase-result"
      style { gap = "8px"; flex = "1 1 240px" }
      div {
        style { fontWeight = "800" }
        text = title
      }
      div {
        style { color = "var(--aj-ink-muted)" }
        text = body
      }
    }
  }

  private def pluginCard(name: String, body: String): Unit = {
    vbox {
      classes = "showcase-result"
      style { gap = "8px"; flex = "1 1 180px" }
      div {
        style { fontWeight = "800" }
        text = name
      }
      div {
        style { color = "var(--aj-ink-muted)" }
        text = body
      }
    }
  }
}
