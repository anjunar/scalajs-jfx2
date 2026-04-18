package jfx.form

import jfx.core.component.{ClientOnlyComponent, NodeComponent}
import jfx.core.component.ElementComponent.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import jfx.form.editor.SsrLexicalRenderer
import jfx.form.editor.plugins.{DefaultDialogService, EditorPlugin}
import jfx.layout.Div.div
import lexical.*
import lexical.codemirror.CodeMirrorNode
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLDivElement, HTMLElement}

import scala.collection.mutable
import scala.scalajs.js

class Editor(
  val name: String,
  val standalone: Boolean = false
)(using Scope) extends ClientOnlyComponent[HTMLDivElement]("div", "LexicalEditor", Editor.loadingFallback)
    with Editable {

  val valueProperty: Property[String | Null] =
    Property(null)

  val placeholderProperty: Property[String] =
    Property("")

  val focusedProperty: Property[Boolean] =
    Property(false)

  val dirtyProperty: Property[Boolean] =
    Property(false)

  val errorsProperty: ListProperty[String] =
    new ListProperty[String]()

  val invalidProperty: ReadOnlyProperty[Boolean] =
    errorsProperty.map(_.length > 0)

  private var lexicalEditor: LexicalEditor | Null = null
  private var toolbarElement: HTMLElement | Null = null
  private var surfaceElement: HTMLElement | Null = null
  private var placeholderElement: HTMLElement | Null = null
  private val editorRegistrations = mutable.ArrayBuffer.empty[js.Function0[Unit]]
  private var currentClientValue = ""
  private var suppressClientChange = false
  private var syncingFromClient = false
  private val pluginBuffer = mutable.ArrayBuffer.empty[EditorPlugin]

  classProperty += "jfx-editor-host"
  setAttribute("data-jfx-control-name", name)

  refreshServerFallback()

  addDisposable(
    valueProperty.observeWithoutInitial { value =>
      syncExternalValue(value)
      refreshServerFallback()
    }
  )
  addDisposable(editableProperty.observeWithoutInitial(syncEditable))
  addDisposable(
    placeholderProperty.observeWithoutInitial { value =>
      syncPlaceholder(value)
      refreshServerFallback()
    }
  )

  def value: String | Null =
    valueProperty.get

  def value_=(nextValue: String | Null): Unit =
    valueProperty.set(nextValue)

  def placeholder: String =
    placeholderProperty.get

  def placeholder_=(value: String): Unit =
    placeholderProperty.set(Option(value).getOrElse(""))

  def setErrors(values: IterableOnce[String]): Unit =
    errorsProperty.setAll(values)

  def addPlugin(plugin: EditorPlugin): Unit = {
    pluginBuffer.indexWhere(_.name == plugin.name) match {
      case index if index >= 0 =>
        pluginBuffer.update(index, plugin)

      case _ =>
        pluginBuffer += plugin
    }

    syncPlugins()
  }

  def plugins: Seq[EditorPlugin] =
    pluginBuffer.toSeq

  override protected def mountClient(element: HTMLDivElement): Unit = {
    destroyEditor()

    setAttribute("data-jfx-editor-loading", "true")

    element.replaceChildren()
    element.classList.add("jfx-editor")
    element.classList.add("editor")

    val shell = dom.document.createElement("div").asInstanceOf[HTMLElement]
    shell.className = "jfx-editor__shell"

    val toolbar = dom.document.createElement("div").asInstanceOf[HTMLElement]
    toolbar.className = "jfx-editor__toolbar"

    val surfaceWrap = dom.document.createElement("div").asInstanceOf[HTMLElement]
    surfaceWrap.className = "jfx-editor__surface-wrap"

    val surface = dom.document.createElement("div").asInstanceOf[HTMLElement]
    surface.className = "jfx-editor__surface lexical-editor-input"
    surface.setAttribute("role", "textbox")
    surface.setAttribute("aria-multiline", "true")
    surface.setAttribute("spellcheck", "true")

    val placeholder = dom.document.createElement("div").asInstanceOf[HTMLElement]
    placeholder.className = "jfx-editor__placeholder"

    surfaceWrap.appendChild(surface)
    surfaceWrap.appendChild(placeholder)
    shell.appendChild(toolbar)
    shell.appendChild(surfaceWrap)
    element.appendChild(shell)

    toolbarElement = toolbar
    surfaceElement = surface
    placeholderElement = placeholder

    val editor =
      new LexicalBuilder()
        .withNamespace(name)
        .withTheme(defaultTheme())
        .withEditable(editableProperty.get)
        .withNodes(js.Array(collectNodes()*))
        .withModules(collectModules()*)
        .build(surface)

    lexicalEditor = editor
    editor.setDialogService(new DefaultDialogService())

    renderToolbar(editor)
    registerPluginInstallers(editor)
    registerDomListeners(surface)

    suppressClientChange = true
    try setEditorValue(editor, valueProperty.get)
    finally suppressClientChange = false

    registerUpdateListener(editor)
    updateEditableState(editableProperty.get)
    updatePlaceholder()
    removeAttribute("data-jfx-editor-loading")
  }

  override protected def afterUnmount(): Unit = {
    destroyEditor()
    focusedProperty.set(false)
  }

  override def dispose(): Unit = {
    destroyEditor()
    super.dispose()
  }

  private def destroyEditor(): Unit = {
    editorRegistrations.reverseIterator.foreach { unregister =>
      try unregister()
      catch {
        case _: Throwable => ()
      }
    }
    editorRegistrations.clear()

    if (lexicalEditor != null) {
      try lexicalEditor.nn.setRootElement(null)
      catch {
        case _: Throwable => ()
      }
    }

    lexicalEditor = null
    toolbarElement = null
    surfaceElement = null
    placeholderElement = null
    currentClientValue = ""
  }

  private def syncExternalValue(value: String | Null): Unit =
    if (!syncingFromClient && lexicalEditor != null) {
      val editor = lexicalEditor.nn
      val nextValue = normalizeValue(value)
      if (nextValue != currentClientValue) {
        suppressClientChange = true
        try setEditorValue(editor, value)
        finally suppressClientChange = false
        updatePlaceholder()
      }
    }

  private def syncEditable(value: Boolean): Unit =
    if (lexicalEditor != null) {
      updateEditableState(value)
    }

  private def syncPlaceholder(value: String): Unit =
    updatePlaceholder()

  private def syncPlugins(): Unit =
    if (!RenderBackend.current.isServer && lexicalEditor != null) {
      hostElement.domElementOption.collect { case element: HTMLDivElement =>
        mountClient(element)
      }
    }

  private def setEditorValue(editor: LexicalEditor, value: String | Null): Unit = {
    val nextValue = normalizeValue(value)
    currentClientValue = nextValue

    if (nextValue.nonEmpty) {
      try {
        editor.setEditorState(editor.parseEditorState(nextValue), js.Dynamic.literal())
        return
      } catch {
        case _: Throwable => ()
      }
    }

    editor.update(
      () => {
        val root = Lexical.$getRoot()
        root.clear()

        val paragraph = Lexical.$createParagraphNode()
        if (nextValue.nonEmpty) {
          paragraph.append(Lexical.$createTextNode(nextValue))
        }
        root.append(paragraph)
      },
      js.Dynamic.literal().asInstanceOf[EditorUpdateOptions]
    )
  }

  private def updateEditableState(value: Boolean): Unit = {
    if (lexicalEditor != null) {
      lexicalEditor.nn.setEditable(value)
    }

    Option(surfaceElement).foreach { surface =>
      surface.asInstanceOf[js.Dynamic].contentEditable = value.toString
      surface.setAttribute("aria-readonly", (!value).toString)
    }

    Option(toolbarElement).foreach { toolbar =>
      setHidden(toolbar, !value || collectToolbarElements().isEmpty)
    }
  }

  private def updatePlaceholder(): Unit =
    Option(placeholderElement).foreach { placeholder =>
      val text = placeholderProperty.get
      placeholder.textContent = text

      if (lexicalEditor == null) {
        setHidden(placeholder, text.isEmpty)
      } else {
        lexicalEditor.nn.read { () =>
          val root = Lexical.$getRoot()
          setHidden(placeholder, text.isEmpty || !root.isEmpty())
        }
      }
    }

  private def registerUpdateListener(editor: LexicalEditor): Unit = {
    val unregister =
      editor.registerUpdateListener { (_: js.Dynamic) =>
        updatePlaceholder()

        if (!suppressClientChange) {
          val json = editorStateJson(editor)
          if (json != currentClientValue) {
            currentClientValue = json
            syncingFromClient = true
            try {
              dirtyProperty.set(true)
              valueProperty.set(json)
            } finally {
              syncingFromClient = false
            }
          }
        }
      }

    editorRegistrations += unregister
  }

  private def registerPluginInstallers(editor: LexicalEditor): Unit =
    pluginBuffer.foreach { plugin =>
      val unregister = plugin.install(editor)
      editorRegistrations += unregister
    }

  private def registerDomListeners(surface: HTMLElement): Unit = {
    val focusInListener: Event => Unit = _ => focusedProperty.set(true)
    val focusOutListener: Event => Unit = _ => focusedProperty.set(false)

    surface.addEventListener("focusin", focusInListener)
    surface.addEventListener("focusout", focusOutListener)

    editorRegistrations += (() => surface.removeEventListener("focusin", focusInListener))
    editorRegistrations += (() => surface.removeEventListener("focusout", focusOutListener))
  }

  private def renderToolbar(editor: LexicalEditor): Unit =
    Option(toolbarElement).foreach { toolbar =>
      toolbar.replaceChildren()

      val elements = collectToolbarElements()
      setHidden(toolbar, !editableProperty.get || elements.isEmpty)

      if (elements.nonEmpty) {
        val registry = new ToolbarRegistry(elements.toList)
        val manager = new ToolbarManager(editor, registry, new RibbonRenderer())
        manager.createToolbar(toolbar)
      }
    }

  private def collectToolbarElements(): Seq[ToolbarElement] =
    pluginBuffer.iterator.flatMap(_.toolbarElements).toSeq

  private def collectModules(): Seq[EditorModule] =
    (
      Seq(new MarkdownModule()) ++
        pluginBuffer.iterator.flatMap(_.modules).toSeq ++
        collectToolbarElements().collect { case module: EditorModule => module }
    ).distinct

  private def collectNodes(): Seq[js.Any] =
    (
      Seq(
        LexicalRichText.HeadingNode,
        LexicalRichText.QuoteNode,
        LexicalList.ListNode,
        LexicalList.ListItemNode,
        LexicalLink.LinkNode,
        LexicalCode.CodeNode,
        js.constructorOf[CodeMirrorNode]
      ) ++ pluginBuffer.iterator.flatMap(_.nodes).toSeq
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

  private def editorStateJson(editor: LexicalEditor): String =
    js.JSON.stringify(editor.getEditorState().toJSON()).asInstanceOf[String]

  private def normalizeValue(value: String | Null): String =
    Option(value).getOrElse("")

  private def setHidden(element: HTMLElement, hidden: Boolean): Unit =
    if (hidden) element.setAttribute("hidden", "")
    else element.removeAttribute("hidden")

  private def refreshServerFallback(): Unit =
    if (RenderBackend.current.isServer) {
      replaceServerFallback {
        SsrLexicalRenderer.render(valueProperty.get, placeholderProperty.get)
      }
    }

}

object Editor {

  def editor(name: String): Editor =
    editor(name, standalone = false)({})

  def editor(name: String)(init: Editor ?=> Unit): Editor =
    editor(name, standalone = false)(init)

  def editor(name: String, standalone: Boolean)(init: Editor ?=> Unit): Editor =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()

      given Scope = currentScope
      val component = new Editor(name, standalone)

      DslRuntime.withComponentContext(ComponentContext(None)) {
        given Scope = currentScope
        given Editor = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def loadingFallback(using Scope): NodeComponent[? <: dom.Node] =
    div {
      classes = "jfx-editor-fallback"
      text = "Editor wird nach der Hydration geladen."
    }

  def value(using editor: Editor): String | Null =
    editor.value

  def value_=(nextValue: String | Null)(using editor: Editor): Unit =
    editor.value = nextValue

  def valueProperty(using editor: Editor): Property[String | Null] =
    editor.valueProperty

  def placeholder(using editor: Editor): String =
    editor.placeholder

  def placeholder_=(value: String)(using editor: Editor): Unit =
    editor.placeholder = value

  def focusedProperty(using editor: Editor): Property[Boolean] =
    editor.focusedProperty

  def dirtyProperty(using editor: Editor): Property[Boolean] =
    editor.dirtyProperty

}
