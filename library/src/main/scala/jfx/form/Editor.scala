package jfx.form

import jfx.core.component.{ClientSideComponent, Component}
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import lexical.{EditorTheme, EditorThemeBuilder, Lexical, LexicalBuilder, LexicalCode, LexicalEditor, LexicalLink, LexicalList, LexicalRichText}
import org.scalajs.dom.{Event, HTMLDivElement, HTMLElement}

import scala.scalajs.js

class Editor(val name: String, override val standalone: Boolean = false)
    extends ClientSideComponent
    with Control[js.Any | Null] {

  override def tagName: String = "div"

  override val valueProperty: Property[js.Any | Null] = Property(null)

  private var lexicalEditor: LexicalEditor | Null = null
  private var editorSurface: HTMLDivElement | Null = null
  private var placeholderElement: HTMLElement | Null = null
  private var editorUnregister: js.Function0[Unit] | Null = null
  private var editorDomCleanup: (() => Unit) | Null = null
  private var lastSeenStateJson: String | Null = null

  override protected def composeFallback(): Unit = {
    given Component = this
    addClass("editor")
    addClass("jfx-editor-host")
    attribute("data-client-side", "fallback")

    div {
      addClass("jfx-editor-fallback")
      text = "Editor wird im Browser aktiviert"
    }

    addDisposable(valueProperty.observe(_ => validate()))
    addDisposable(validators.observe(_ => validate()))
    addDisposable(dirtyProperty.observe(_ => validate()))
    addDisposable(placeholderProperty.observe(_ => refreshPlaceholder()))
    addDisposable(editableProperty.observe(editable => updateEditable(editable)))

    if (!standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case _: Exception =>
      }
    }
  }

  override protected def mountClient(): Unit = {
    editorSurface = null
    placeholderElement = null

    renderClient {
      given Component = this

      div {
        addClass("jfx-editor")
        div {
          addClass("jfx-editor__shell")
          div {
            addClass("jfx-editor__toolbar")
            div {
              addClass("jfx-editor__toolbar-group")
              div {
                addClass("jfx-editor__toolbar-group-label")
                text = "Lexical"
              }
              div {
                addClass("jfx-editor__toolbar-buttons")
                div {
                  addClass("jfx-editor__toolbar-button")
                  text = "Basis aktiv"
                }
              }
            }
          }

          div {
            addClass("jfx-editor__surface-wrap")

            EditorSurfaceFrame {
              EditorSurface(surface => editorSurface = surface)
            }
            EditorPlaceholder(placeholderProperty, element => placeholderElement = element)
          }
        }
      }
    }

    mountLexical()
  }

  override def dispose(): Unit = {
    destroyEditorView()
    super.dispose()
  }

  private def mountLexical(): Unit = {
    val surface = editorSurface
    if (surface == null) {
      return
    }

    registerDomListeners(surface.nn)

    val initialValue = valueProperty.get
    val initialStateJson = toLexicalJson(initialValue)

    val builder =
      new LexicalBuilder()
        .withNamespace(name)
        .withTheme(defaultTheme())
        .withEditable(editableProperty.get)
        .withNodes(defaultNodes())

    initialStateJson.foreach(builder.withInitialState)

    val editor = builder.build(surface.nn)
    lexicalEditor = editor
    editorUnregister = editor.registerUpdateListener { (_: js.Dynamic) =>
      refreshPlaceholder()
      publishEditorState(editor, markDirty = true)
    }

    surface.nn.setAttribute("role", "textbox")
    surface.nn.setAttribute("aria-multiline", "true")
    surface.nn.setAttribute("aria-readonly", (!editableProperty.get).toString)

    if (initialStateJson.nonEmpty) {
      lastSeenStateJson = initialStateJson.orNull
    } else {
      publishEditorState(editor, markDirty = false)
    }

    refreshPlaceholder()
  }

  private def destroyEditorView(): Unit = {
    focusedProperty.set(false)

    if (editorUnregister != null) {
      try editorUnregister.nn.apply()
      catch {
        case _: Throwable =>
      }
      editorUnregister = null
    }

    if (lexicalEditor != null) {
      try lexicalEditor.nn.setRootElement(null)
      catch {
        case _: Throwable =>
      }
      lexicalEditor = null
    }

    if (editorDomCleanup != null) {
      editorDomCleanup.nn.apply()
      editorDomCleanup = null
    }

    editorSurface = null
    placeholderElement = null
  }

  private def registerDomListeners(surface: HTMLDivElement): Unit = {
    val focusInListener: Event => Unit = _ => focusedProperty.set(true)
    val focusOutListener: Event => Unit = _ => focusedProperty.set(false)

    surface.addEventListener("focusin", focusInListener)
    surface.addEventListener("focusout", focusOutListener)

    editorDomCleanup = () => {
      surface.removeEventListener("focusin", focusInListener)
      surface.removeEventListener("focusout", focusOutListener)
    }
  }

  private def updateEditable(editable: Boolean): Unit =
    if (lexicalEditor != null) {
      lexicalEditor.nn.setEditable(editable)
      if (editorSurface != null) {
        editorSurface.nn.setAttribute("contenteditable", editable.toString)
        editorSurface.nn.setAttribute("aria-readonly", (!editable).toString)
      }
    }

  private def publishEditorState(editor: LexicalEditor, markDirty: Boolean): Unit = {
    val json = editorStateJson(editor)
    if (lastSeenStateJson != null && lastSeenStateJson == json) {
      return
    }

    lastSeenStateJson = json

    if (markDirty) {
      setDirty(true)
    }

    valueProperty.set(json.asInstanceOf[js.Any])
  }

  private def refreshPlaceholder(): Unit =
    if (placeholderElement != null && lexicalEditor != null) {
      val text = Option(placeholderProperty.get).map(_.trim).getOrElse("")
      placeholderElement.nn.textContent = text
      placeholderElement.nn.style.display =
        if (text.isEmpty || !editorIsEmpty(lexicalEditor.nn)) "none"
        else ""
    } else if (placeholderElement != null) {
      placeholderElement.nn.style.display =
        if (Option(placeholderProperty.get).forall(_.trim.isEmpty)) "none"
        else ""
    }

  private def editorIsEmpty(editor: LexicalEditor): Boolean = {
    var empty = true
    editor.getEditorState().read { () =>
      empty = Lexical.$getRoot().getTextContent().trim.isEmpty
    }
    empty
  }

  private def editorStateJson(editor: LexicalEditor): String =
    js.JSON.stringify(editor.getEditorState().toJSON())

  private def toLexicalJson(value: js.Any | Null): Option[String] =
    if (value == null || js.isUndefined(value.asInstanceOf[js.Any])) {
      None
    } else {
      val asString =
        if (js.typeOf(value.asInstanceOf[js.Any]) == "string") {
          value.asInstanceOf[String]
        } else {
          js.JSON.stringify(value.asInstanceOf[js.Any])
        }

      Option(asString).map(_.trim).filter(_.nonEmpty)
    }

  private def defaultNodes(): js.Array[js.Any] =
    js.Array(
      LexicalRichText.HeadingNode,
      LexicalRichText.QuoteNode,
      LexicalList.ListNode,
      LexicalList.ListItemNode,
      LexicalLink.LinkNode,
      LexicalCode.CodeNode
    )

  private def defaultTheme(): EditorTheme =
    new EditorThemeBuilder()
      .withParagraph("lexical-paragraph")
      .withQuote("lexical-quote")
      .withHeading(1, "lexical-heading-1")
      .withHeading(2, "lexical-heading-2")
      .withHeading(3, "lexical-heading-3")
      .withTextBold("lexical-text-bold")
      .withTextItalic("lexical-text-italic")
      .withTextUnderline("lexical-text-underline")
      .withTextStrikethrough("lexical-text-strikethrough")
      .withCode("lexical-text-code")
      .build()
}

object Editor {
  def editor(name: String, standalone: Boolean = false)(init: Editor ?=> Unit = {}): Editor =
    DslRuntime.build(new Editor(name, standalone))(init)

  def value(using e: Editor): js.Any | Null =
    e.valueProperty.get

  def value_=(using e: Editor)(nextValue: js.Any | Null): Unit =
    e.valueProperty.set(nextValue)

  def valueProperty(using e: Editor): Property[js.Any | Null] =
    e.valueProperty

  def placeholder(using e: Editor): String =
    e.placeholder

  def placeholder_=(using e: Editor)(value: String): Unit =
    e.placeholder = value
}

private final class EditorSurface(onReady: HTMLDivElement => Unit) extends Component {
  override def tagName: String = "div"

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-editor__surface")
    host.domNode.collect { case surface: HTMLDivElement => onReady(surface) }
  }
}

private object EditorSurface {
  def apply(onReady: HTMLDivElement => Unit): EditorSurface =
    DslRuntime.build(new EditorSurface(onReady)) {}
}

private final class EditorSurfaceFrame extends Component {
  override def tagName: String = "section"

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-editor__surface-frame")
  }
}

private object EditorSurfaceFrame {
  def apply(init: EditorSurfaceFrame ?=> Unit): EditorSurfaceFrame =
    DslRuntime.build(new EditorSurfaceFrame)(init)
}

private final class EditorPlaceholder(textProperty: ReadOnlyProperty[String], onReady: HTMLElement => Unit) extends Component {
  override def tagName: String = "div"

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-editor__placeholder")
    text = textProperty.map { value =>
      Option(value).map(_.trim).getOrElse("")
    }
    host.domNode.collect { case element: HTMLElement => onReady(element) }
  }
}

private object EditorPlaceholder {
  def apply(textProperty: ReadOnlyProperty[String], onReady: HTMLElement => Unit): EditorPlaceholder =
    DslRuntime.build(new EditorPlaceholder(textProperty, onReady)) {}
}
