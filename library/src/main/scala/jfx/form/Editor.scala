package jfx.form

import jfx.core.component.{Box, ClientSideComponent, Component}
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.form.editor.plugins.{DefaultDialogService, EditorPlugin}
import jfx.layout.Div.div
import lexical.{EditorModule, EditorTheme, EditorThemeBuilder, Lexical, LexicalBuilder, LexicalCode, LexicalEditor, LexicalLink, LexicalList, LexicalRichText, RibbonRenderer, ToolbarDropdown, ToolbarElement, ToolbarManager, ToolbarRegistry, setDialogService}
import org.scalajs.dom.{Event, HTMLDivElement, HTMLElement}

import scala.collection.mutable
import scala.scalajs.js

class Editor(val name: String, override val standalone: Boolean = false)
    extends ClientSideComponent
    with Control[js.Any | Null] {

  override def tagName: String = "div"

  override val valueProperty: Property[js.Any | Null] = Property(null)

  private var lexicalEditor: LexicalEditor | Null = null
  private var editorSurface: HTMLDivElement | Null = null
  private var toolbarHost: HTMLElement | Null = null
  private var placeholderElement: HTMLElement | Null = null
  private var editorUnregister: js.Function0[Unit] | Null = null
  private var editorDomCleanup: (() => Unit) | Null = null
  private var lastSeenStateJson: String | Null = null
  private var fallbackRendered = false
  private val editorRegistrations = mutable.ArrayBuffer.empty[js.Function0[Unit]]
  private val pluginComponents = mutable.ArrayBuffer.empty[EditorPlugin]

  override protected def composeFallback(): Unit = {
    given Component = this
    addClass("editor")
    addClass("jfx-editor-host")

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

  override def afterCompose(): Unit =
    if (!fallbackRendered) {
      fallbackRendered = true
      renderFallbackContent()
    }

  private def renderFallbackContent(): Unit = {
    given Component = this
    val previewText = extractPreviewText(valueProperty.get)

    div {
      addClass("jfx-editor")
      div {
        addClass("jfx-editor__shell")
        renderFallbackToolbar()

        div {
          addClass("jfx-editor__surface-wrap")

          Box.box("section") {
            addClass("jfx-editor__surface-frame")
            div {
              addClass("jfx-editor__surface")
              addClass("lexical-editor-input")
              role = "textbox"
              previewText.foreach(value => text = value)
            }
          }

          div {
            addClass("jfx-editor__placeholder")
            visible = previewText.isEmpty && placeholderProperty.get.trim.nonEmpty
            text = placeholderProperty.get.trim
          }
        }
      }
    }
  }

  private def renderFallbackToolbar()(using Component): Unit = {
    val elements = collectToolbarElements()

    div {
      addClass("jfx-editor__toolbar")
      visible = editableProperty.get && elements.nonEmpty

      if (editableProperty.get && elements.nonEmpty) {
        val model = new ToolbarRegistry(elements.toList).getModel

        div {
          addClass("lexical-ribbon-wrapper")

          div {
            addClass("lexical-ribbon-tabs")
            model.tabs.zipWithIndex.foreach { case (tab, index) =>
              div {
                addClass("lexical-ribbon-tab")
                if (index == 0) {
                  addClass("active")
                }
                text = tab.name
              }
            }
          }

          div {
            addClass("lexical-ribbon-content")
            model.tabs.zipWithIndex.foreach { case (tab, tabIndex) =>
              div {
                addClass("lexical-ribbon-tab-content")
                style {
                  display = if (tabIndex == 0) "flex" else "none"
                }

                tab.sections.foreach { section =>
                  div {
                    addClass("lexical-ribbon-section")

                    div {
                      addClass("lexical-ribbon-buttons-container")
                      section.modules.foreach(renderFallbackToolbarElement)
                    }

                    div {
                      addClass("lexical-ribbon-section-label")
                      text = section.name
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private def renderFallbackToolbarElement(element: ToolbarElement)(using Component): Unit =
    element match {
      case module: EditorModule =>
        div {
          addClass("lexical-ribbon-button")
          style {
            cursor = "not-allowed"
            opacity = "0.58"
          }
          module.iconName match {
            case Some(icon) =>
              Box.box("span") {
                addClass("material-icons")
                text = icon
              }
            case None =>
              text = module.name
          }
        }

      case dropdown: ToolbarDropdown =>
        div {
          addClass("lexical-ribbon-button")
          style {
            cursor = "not-allowed"
            opacity = "0.58"
          }
          val selectedLabel =
            dropdown.options.headOption.map(option => s"${dropdown.name}: ${option.label}").getOrElse(dropdown.name)
          text = selectedLabel
        }

      case _ =>
    }

  override protected def mountClient(): Unit = {
    editorSurface = null
    toolbarHost = null
    placeholderElement = null

    renderClient {
      given Component = this

      div {
        addClass("jfx-editor")
        div {
          addClass("jfx-editor__shell")
          EditorToolbarHost(element => toolbarHost = element)

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

  private[jfx] def registerPlugin(plugin: EditorPlugin): Unit =
    if (!pluginComponents.exists(_.name == plugin.name)) {
      pluginComponents += plugin
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
        .withModules(collectModules()*)

    initialStateJson.foreach(builder.withInitialState)

    val editor = builder.build(surface.nn)
    lexicalEditor = editor
    editor.setDialogService(new DefaultDialogService())
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

    renderToolbar(editor)
    installPlugins(editor)
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

    editorRegistrations.reverseIterator.foreach { unregister =>
      try unregister()
      catch {
        case _: Throwable =>
      }
    }
    editorRegistrations.clear()

    editorSurface = null
    if (toolbarHost != null) {
      toolbarHost.nn.innerHTML = ""
    }
    toolbarHost = null
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
      renderToolbar(lexicalEditor.nn)
    }

  private def renderToolbar(editor: LexicalEditor): Unit =
    if (toolbarHost != null) {
      val elements = collectToolbarElements()
      val host = toolbarHost.nn
      host.innerHTML = ""
      host.style.display =
        if (editableProperty.get && elements.nonEmpty) ""
        else "none"

      if (editableProperty.get && elements.nonEmpty) {
        val registry = new ToolbarRegistry(elements.toList)
        val manager = new ToolbarManager(editor, registry, new RibbonRenderer())
        manager.createToolbar(host)
      }
    }

  private def installPlugins(editor: LexicalEditor): Unit =
    pluginComponents.foreach { plugin =>
      val unregister = plugin.install(editor)
      if (unregister != null) {
        editorRegistrations += unregister
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

      Option(asString)
        .map(_.trim)
        .filter(_.nonEmpty)
        .map { text =>
          if (looksLikeJson(text)) text
          else plainTextStateJson(text)
        }
    }

  private def extractPreviewText(value: js.Any | Null): Option[String] =
    if (value == null || js.isUndefined(value.asInstanceOf[js.Any])) {
      None
    } else if (js.typeOf(value.asInstanceOf[js.Any]) == "string") {
      val text = value.asInstanceOf[String].trim
      if (text.isEmpty) None
      else if (looksLikeJson(text)) extractTextFromLexicalJson(text).orElse(Some(text))
      else Some(text)
    } else {
      extractTextFromLexicalJson(js.JSON.stringify(value.asInstanceOf[js.Any]))
    }

  private def looksLikeJson(value: String): Boolean =
    value.startsWith("{") || value.startsWith("[")

  private def extractTextFromLexicalJson(json: String): Option[String] =
    try {
      val parsed = js.JSON.parse(json)
      val root = parsed.asInstanceOf[js.Dynamic].selectDynamic("root").asInstanceOf[js.Any]
      val source =
        if (jsValueExists(root)) root
        else parsed.asInstanceOf[js.Any]
      val parts = mutable.ArrayBuffer.empty[String]
      collectText(source, parts)
      Option(parts.mkString(" ").trim).filter(_.nonEmpty)
    } catch {
      case _: Throwable => None
    }

  private def collectText(value: js.Any, parts: mutable.ArrayBuffer[String]): Unit =
    if (jsValueExists(value)) {
      val dynamic = value.asInstanceOf[js.Dynamic]
      val text = dynamic.selectDynamic("text").asInstanceOf[js.Any]
      if (jsValueExists(text) && js.typeOf(text) == "string") {
        parts += text.asInstanceOf[String]
      }

      val childrenValue = dynamic.selectDynamic("children").asInstanceOf[js.Any]
      if (jsValueExists(childrenValue)) {
        try {
          childrenValue.asInstanceOf[js.Array[js.Any]].foreach(child => collectText(child, parts))
        } catch {
          case _: Throwable =>
        }
      }
    }

  private def jsValueExists(value: js.Any): Boolean =
    value != null && !js.isUndefined(value)

  private def plainTextStateJson(text: String): String =
    js.JSON.stringify(
      js.Dynamic.literal(
        root = js.Dynamic.literal(
          children = js.Array(
            js.Dynamic.literal(
              children = js.Array(
                js.Dynamic.literal(
                  detail = 0,
                  format = 0,
                  mode = "normal",
                  style = "",
                  text = text,
                  `type` = "text",
                  version = 1
                )
              ),
              direction = null,
              format = "",
              indent = 0,
              `type` = "paragraph",
              version = 1
            )
          ),
          direction = null,
          format = "",
          indent = 0,
          `type` = "root",
          version = 1
        )
      )
    )

  private def defaultNodes(): js.Array[js.Any] =
    js.Array(
      (
        Seq(
          LexicalRichText.HeadingNode,
          LexicalRichText.QuoteNode,
          LexicalList.ListNode,
          LexicalList.ListItemNode,
          LexicalLink.LinkNode,
          LexicalCode.CodeNode
        ) ++ pluginComponents.iterator.flatMap(_.nodes).toSeq
      ).distinct*
    )

  private def collectToolbarElements(): Seq[ToolbarElement] =
    pluginComponents.iterator.flatMap(_.toolbarElements).toSeq

  private def collectModules(): Seq[EditorModule] =
    (
      pluginComponents.iterator.flatMap(_.modules).toSeq ++
        collectToolbarElements().collect { case module: EditorModule => module }
    ).distinct

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

private final class EditorToolbarHost(onReady: HTMLElement => Unit) extends Component {
  override def tagName: String = "div"

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-editor__toolbar")
    host.domNode.collect { case element: HTMLElement => onReady(element) }
  }
}

private object EditorToolbarHost {
  def apply(onReady: HTMLElement => Unit): EditorToolbarHost =
    DslRuntime.build(new EditorToolbarHost(onReady)) {}
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
