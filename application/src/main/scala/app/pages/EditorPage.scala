package app.pages

import app.components.Showcase.*
import app.domain.BlogDraft
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.Property
import jfx.core.state.ReadOnlyProperty
import app.DemoI18n
import jfx.i18n.*
import jfx.form.Editor.*
import jfx.form.editor.plugins.{basePlugin, codePlugin, headingPlugin, imagePlugin, linkPlugin, listPlugin, tablePlugin}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.timers.setTimeout
import scala.util.control.NonFatal

object EditorPage {
  private val EditorStorageKey = "scalajs-jfx2.editor-showcase.content"
  private val EditorStorageVersion = 3

  def render(draft: BlogDraft): Unit = {
    val initialEditorValue = Option(draft.content.get).getOrElse(showcaseDocumentState())
    if (draft.content.get == null) {
      draft.content.set(initialEditorValue)
    }
    val editorContentProperty = Property(cloneLexicalState(initialEditorValue))

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
          i18n"Storage" -> i18n"The current editor state is saved in the browser."
        )

        componentShowcase(
          i18n"Editor showcase",
          i18n"A single editor keeps the demo focused while still showing SSR, plugins, and local persistence."
        ) {
          vbox {
            style { gap = "16px" }

            vbox {
              style { gap = "12px" }
              div {
                style { fontWeight = "800" }
                text = DemoI18n.text(i18n"Editor")
              }
              editor("editor-showcase", standalone = true) {
                val writableEditor = summon[jfx.form.Editor]
                value = editorContentProperty.get
                ribbonToolbar
                placeholder = DemoI18n.text(i18n"The writing surface activates on the client")
                style {
                  width = "100%"
                  minHeight = "340px"
                }
                installDefaultPlugins()
                bindShowcaseEditor(writableEditor, editorContentProperty, draft)
              }
            }

            hbox {
              style { gap = "10px"; flexWrap = "wrap" }

              button(DemoI18n.text(i18n"SSR sample text")) {
                onClick { _ =>
                  setEditorState(draft, editorContentProperty, initialEditorValue)
                }
              }

              button(DemoI18n.text(i18n"Short text")) {
                onClick { _ =>
                  setEditorState(
                    draft,
                    editorContentProperty,
                    paragraphDocumentState(DemoI18n.resolveNow(i18n"Short external property update. The editor adopts it after hydration."))
                  )
                }
              }

              button(DemoI18n.text(i18n"Restore saved demo text")) {
                onClick { _ =>
                  setEditorState(
                    draft,
                    editorContentProperty,
                    paragraphDocumentState(DemoI18n.resolveNow(i18n"This value was set outside the editor and synchronized back into Lexical."))
                  )
                }
              }

              button(DemoI18n.text(i18n"Save in browser")) {
                onClick { _ =>
                  saveEditorStateToBrowser(draft.content.get)
                }
              }

              button(DemoI18n.text(i18n"Load saved copy")) {
                onClick { _ =>
                  loadEditorStateFromBrowser(allowLegacyState = true).foreach { storedState =>
                    setEditorState(draft, editorContentProperty, storedState)
                  }
                }
              }
            }

            div {
              style { color = "var(--aj-ink-muted)" }
              text = DemoI18n.text(i18n"Changes are also auto-saved in the browser, so you can restore the last editor state after a reload.")
            }
          }
        }

        componentShowcase(
          i18n"SSR fallback contract",
          i18n"What is rendered on the server must structurally match the first client render."
        ) {
          hbox {
            style { gap = "14px"; flexWrap = "wrap"; alignItems = "stretch" }
            detailCard(
              DemoI18n.text(i18n"Shell stays stable"),
              DemoI18n.text(i18n"Fallback and client version use the same jfx-editor shell with toolbar area and surface frame.")
            )
            detailCard(
              DemoI18n.text(i18n"Toolbar does not flicker"),
              DemoI18n.text(i18n"The editor shell stays stable, so hydration can activate Lexical without layout surprises.")
            )
            detailCard(
              DemoI18n.text(i18n"Text stays visible"),
              DemoI18n.text(i18n"Plain text or Lexical JSON is extracted into preview text during SSR and then adopted by Lexical after hydration.")
            )
            detailCard(
              DemoI18n.text(i18n"External values"),
              DemoI18n.text(i18n"valueProperty updates are synchronized back into Lexical after mount via parseEditorState.")
            )
          }
        }

        componentShowcase(
          i18n"Plugin set",
          i18n"The showcase loads all ported plugins so toolbar and dialogs are visible together."
        ) {
          hbox {
            style { gap = "14px"; flexWrap = "wrap"; alignItems = "stretch" }
            pluginCard(DemoI18n.text(i18n"Base"), DemoI18n.text(i18n"Bold, italic, underline, and basic commands."))
            pluginCard(DemoI18n.text(i18n"Heading"), DemoI18n.text(i18n"Paragraphs and headings through a dropdown."))
            pluginCard(DemoI18n.text(i18n"List"), DemoI18n.text(i18n"Bullet and numbered list commands."))
            pluginCard(DemoI18n.text(i18n"Link"), DemoI18n.text(i18n"Dialog service for inserting links."))
            pluginCard(DemoI18n.text(i18n"Image"), DemoI18n.text(i18n"Image dialog as an editor plugin."))
            pluginCard(DemoI18n.text(i18n"Table"), DemoI18n.text(i18n"Table nodes and toolbar actions."))
            pluginCard(DemoI18n.text(i18n"Code"), DemoI18n.text(i18n"Code block plugin as a specialized node."))
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
          codeBlock("scala", """def render(draft: BlogDraft): Unit = {
  val contentProperty = draft.content

  editor("content", standalone = true) {
    value = contentProperty.get
    placeholder = "Write text..."
    style { minHeight = "340px" }

    basePlugin()
    headingPlugin()
    listPlugin()
    linkPlugin()
    imagePlugin()
    tablePlugin()
    codePlugin()
  }
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

    restoreStoredEditorStateOnClient(draft, editorContentProperty)
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

  private def bindShowcaseEditor(
      editor: jfx.form.Editor,
      editorContentProperty: Property[js.Any | Null],
      draft: BlogDraft
  )(using jfx.core.component.Component): Unit = {
    addDisposable(editor.$valueProperty.observeWithoutInitial { nextValue =>
      editorContentProperty.set(nextValue)
      draft.content.set(cloneLexicalState(nextValue))
      saveEditorStateToBrowser(nextValue)
    })
    addDisposable(editorContentProperty.observeWithoutInitial { nextValue =>
      editor.$valueProperty.set(nextValue)
    })
  }

  private def setEditorState(
      draft: BlogDraft,
      editorContentProperty: Property[js.Any | Null],
      state: js.Any | Null
  ): Unit = {
    draft.content.set(cloneLexicalState(state))
    editorContentProperty.set(cloneLexicalState(state))
  }

  private def cloneLexicalState(state: js.Any | Null): js.Any | Null =
    if (state == null) null
    else js.JSON.parse(js.JSON.stringify(state))

  private def restoreStoredEditorStateOnClient(
      draft: BlogDraft,
      editorContentProperty: Property[js.Any | Null]
  ): Unit =
    if (!RenderBackend.current.isServer) {
      setTimeout(0) {
        loadEditorStateFromBrowser(allowLegacyState = false).foreach { storedState =>
          setEditorState(draft, editorContentProperty, storedState)
        }
      }
    }

  private def saveEditorStateToBrowser(state: js.Any | Null): Unit =
    if (!RenderBackend.current.isServer && state != null) {
      try {
        val payload = js.Dynamic.literal(
          version = EditorStorageVersion,
          state = state
        )
        dom.window.localStorage.setItem(EditorStorageKey, js.JSON.stringify(payload))
      }
      catch { case NonFatal(_) => () }
    }

  private def loadEditorStateFromBrowser(allowLegacyState: Boolean): Option[js.Any] =
    if (RenderBackend.current.isServer) {
      None
    } else {
      try {
        Option(dom.window.localStorage.getItem(EditorStorageKey))
          .filter(_.trim.nonEmpty)
          .flatMap { raw =>
            val parsed = js.JSON.parse(raw)
            val dynamic = parsed.asInstanceOf[js.Dynamic]
            val storedVersion = dynamic.selectDynamic("version").asInstanceOf[js.UndefOr[Int]]
            val storedState = dynamic.selectDynamic("state").asInstanceOf[js.UndefOr[js.Any]]

            storedVersion.toOption match {
              case Some(EditorStorageVersion) => storedState.toOption
              case Some(_)                    => None
              case None if allowLegacyState   => Some(parsed)
              case None                       => None
            }
          }
      } catch {
        case NonFatal(_) => None
      }
    }

  private def showcaseDocumentState(): js.Any =
    lexicalRoot(
      headingNode(
        "h2",
        textNode("Editor showcase: one document with all core content types")
      ),
      paragraphNode(
        textNode("This sample is rendered on the server first, then hydrated into Lexical on the client. "),
        textNode("It intentionally includes lists, an image, a table, and a code snippet", format = 1),
        textNode(" so the showcase immediately covers the common editing cases.")
      ),
      headingNode(
        "h3",
        textNode("Checklist")
      ),
      listNode(
        "bullet",
        listItemNode(
          paragraphNode(textNode("Keep the fallback readable before hydration."))
        ),
        listItemNode(
          paragraphNode(textNode("Show rich content blocks, not just paragraphs."))
        ),
        listItemNode(
          paragraphNode(textNode("Demonstrate how external state sync still works with complex content."))
        )
      ),
      headingNode(
        "h3",
        textNode("Embedded image")
      ),
      paragraphNode(
        textNode("A visual block should also be present in the demo document.")
      ),
      imageNode(
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=80",
        "Team collaborating around a laptop",
        720
      ),
      headingNode(
        "h3",
        textNode("Planning table")
      ),
      tableNode(
        tableRowNode(
          tableCellNode(headerState = 3, paragraphNode(textNode("Area"))),
          tableCellNode(headerState = 3, paragraphNode(textNode("Goal"))),
          tableCellNode(headerState = 3, paragraphNode(textNode("Status")))
        ),
        tableRowNode(
          tableCellNode(paragraphNode(textNode("SSR"))),
          tableCellNode(paragraphNode(textNode("Content visible on first paint"))),
          tableCellNode(paragraphNode(textNode("Done")))
        ),
        tableRowNode(
          tableCellNode(paragraphNode(textNode("Hydration"))),
          tableCellNode(paragraphNode(textNode("Lexical adopts the existing shell"))),
          tableCellNode(paragraphNode(textNode("Done")))
        ),
        tableRowNode(
          tableCellNode(paragraphNode(textNode("Persistence"))),
          tableCellNode(paragraphNode(textNode("State survives reloads through local storage"))),
          tableCellNode(paragraphNode(textNode("Enabled")))
        )
      ),
      headingNode(
        "h3",
        textNode("Code snippet")
      ),
      codeMirrorNode(
        "scala",
        """editor("content", standalone = true) {
  ribbonToolbar
  basePlugin()
  headingPlugin()
  listPlugin()
  imagePlugin()
  tablePlugin()
  codePlugin()
}"""
      ),
      paragraphNode(
        textNode("Use the toolbar to modify this content, then reload the page to verify the saved editor state is restored.")
      )
    )

  private def paragraphDocumentState(text: String): js.Any =
    lexicalRoot(
      paragraphNode(textNode(text))
    )

  private def lexicalRoot(children: js.Any*): js.Any =
    js.Dynamic.literal(
      root = js.Dynamic.literal(
        `type` = "root",
        version = 1,
        indent = 0,
        format = "",
        direction = null,
        children = js.Array(children*)
      )
    )

  private def paragraphNode(children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "paragraph",
      version = 1,
      indent = 0,
      format = "",
      direction = null,
      children = js.Array(children*)
    )

  private def headingNode(tag: String, children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "heading",
      version = 1,
      indent = 0,
      format = "",
      direction = null,
      tag = tag,
      children = js.Array(children*)
    )

  private def listNode(listType: String, children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "list",
      version = 1,
      indent = 0,
      format = "",
      direction = null,
      listType = listType,
      start = 1,
      tag = if (listType == "number") "ol" else "ul",
      children = js.Array(children*)
    )

  private def listItemNode(children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "listitem",
      version = 1,
      indent = 0,
      format = "",
      direction = null,
      value = 1,
      checked = null,
      children = js.Array(children*)
    )

  private def imageNode(src: String, altText: String, maxWidth: Int): js.Any =
    js.Dynamic.literal(
      `type` = "image",
      version = 1,
      src = src,
      altText = altText,
      maxWidth = maxWidth
    )

  private def tableNode(children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "table",
      version = 1,
      indent = 0,
      format = "",
      direction = null,
      children = js.Array(children*)
    )

  private def tableRowNode(children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "tablerow",
      version = 1,
      height = null,
      children = js.Array(children*)
    )

  private def tableCellNode(children: js.Any*): js.Any =
    tableCellNode(headerState = 0, children*)

  private def tableCellNode(headerState: Int, children: js.Any*): js.Any =
    js.Dynamic.literal(
      `type` = "tablecell",
      version = 1,
      colSpan = 1,
      rowSpan = 1,
      headerState = headerState,
      width = null,
      backgroundColor = null,
      children = js.Array(children*)
    )

  private def codeMirrorNode(language: String, code: String): js.Any =
    js.Dynamic.literal(
      `type` = "codemirror",
      version = 1,
      code = code,
      language = language,
    )

  private def textNode(text: String, format: Int = 0): js.Any =
    js.Dynamic.literal(
      `type` = "text",
      version = 1,
      text = text,
      detail = 0,
      format = format,
      mode = "normal",
      style = ""
    )

  private def detailCard(title: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("showcase-result")
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

  private def pluginCard(name: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("showcase-result")
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
