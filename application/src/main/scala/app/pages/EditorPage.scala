package app.pages

import app.components.Showcase.*
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.scalajs.js

object EditorPage {
  def render(): Unit = {
    val initialEditorText =
      "Dieser Text kommt bereits aus dem SSR-Fallback und wird nach der Hydration von Lexical übernommen."
    val mirroredEditorValue = Property[js.Any | Null](initialEditorText)

    showcasePage("Editor", "Lexical als SSR-sichere Client-Island.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Lexical Island",
          "Der Editor rendert sofort Inhalt und wird erst im Browser zu Lexical.",
          "JFX2 behandelt den Editor als ClientSideComponent: SSR bekommt einen stabilen Fallback mit gleichem Shell-Aufbau, Hydration ersetzt die Arbeitsfläche durch Lexical, und externe Property-Änderungen fließen wieder in den Editor zurück."
        )

        metricStrip(
          "SSR" -> "Text ist schon vor Hydration sichtbar.",
          "Toolbar" -> "Plugins liefern ihre Controls über den Renderer.",
          "Readonly" -> "editable = false blendet Toolbar aus und sperrt Lexical."
        )

        componentShowcase(
          "Live Editor Playground",
          "Schreiben, Toolbar nutzen und unten direkt den readonly Spiegel sehen."
        ) {
          vbox {
            style { gap = "16px" }

            editor("editor-playground", standalone = true) {
              val writableEditor = summon[jfx.form.Editor]
              value = mirroredEditorValue.get
              placeholder = "Schreibfläche wird clientseitig aktiviert"
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

              button("SSR Beispieltext") {
                onClick { _ =>
                  mirroredEditorValue.set(initialEditorText)
                }
              }

              button("Kurzer Text") {
                onClick { _ =>
                  mirroredEditorValue.set("Kurzer externer Property-Update. Der Editor übernimmt ihn nach der Hydration.")
                }
              }

              button("Readonly Inhalt setzen") {
                onClick { _ =>
                  mirroredEditorValue.set("Dieser Wert wurde außerhalb des Editors gesetzt und wird in beide Instanzen synchronisiert.")
                }
              }
            }
          }
        }

        componentShowcase(
          "Readonly Mirror",
          "Dieselbe Editor-Komponente, aber mit editable = false: keine Toolbar, kein editierbarer Root, gleicher Inhalt."
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
          "SSR Fallback Contract",
          "Was serverseitig gerendert wird, muss dem ersten Client-Render strukturell entsprechen."
        ) {
          hbox {
            style { gap = "14px"; flexWrap = "wrap"; alignItems = "stretch" }
            detailCard(
              "Shell bleibt stabil",
              "Fallback und Client-Version nutzen dieselbe jfx-editor Shell mit Toolbar-Bereich und Surface-Frame."
            )
            detailCard(
              "Toolbar flackert nicht",
              "Readonly rendert die Toolbar serverseitig bereits ausgeblendet, damit Hydration keine Struktur überraschen muss."
            )
            detailCard(
              "Text bleibt sichtbar",
              "Plain Text oder Lexical JSON wird im SSR zu Preview-Text extrahiert und nach Hydration in Lexical übernommen."
            )
            detailCard(
              "Externe Werte",
              "valueProperty-Updates werden nach dem Mount über parseEditorState zurück in Lexical synchronisiert."
            )
          }
        }

        componentShowcase(
          "Plugin Set",
          "Der Showcase lädt alle portierten Plugins, damit Toolbar und Dialoge gemeinsam sichtbar sind."
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
          ("Island", "JavaScript erst im Browser", "SSR ruft Lexical nicht auf und bleibt damit frei von document/window Zugriffen."),
          ("Fallback", "Kein leerer Editor", "Der Nutzer und Crawler sehen Inhalt, auch bevor Lexical gemountet ist."),
          ("Editable", "Ein Control-Vertrag", "Der Editor folgt demselben editable-Pattern wie Input, ComboBox und Cropper.")
        )

        apiSection(
          "DSL Syntax",
          "Der Editor bleibt ein normales Form-Control mit Value, Placeholder, Editable und Plugins."
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
          "Client Island Flow",
          "Die Komponente trennt SSR-Fallback, Client-Mount und Lexical-Synchronisierung."
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
