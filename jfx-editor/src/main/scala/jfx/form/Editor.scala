package jfx.form

import jfx.core.component.Component.*
import jfx.core.component.{Box, ClientSideComponent, ClientSideSsrContent, Component}
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.form.editor.plugins.{DefaultDialogService, EditorPlugin}
import jfx.layout.Div.div
import lexical.*
import org.scalajs.dom.{Event, HTMLDivElement, HTMLElement, window}

import scala.collection.mutable
import scala.scalajs.js

enum EditorToolbarMode:
  case Ribbon, Menu, Floating

class Editor(val $name: String, override val $standalone: Boolean = false)
  extends ClientSideComponent
    with Control[js.Any | Null] {

  override def tagName: String = "div"

  override val $valueProperty: Property[js.Any | Null] = Property(null)

  private var lexicalEditor: LexicalEditor | Null = null
  private var previewElement: HTMLElement | Null = null
  private var liveRoot: HTMLDivElement | Null = null
  private var toolbarHost: HTMLElement | Null = null
  private var placeholderElement: HTMLElement | Null = null
  private var editorUnregister: js.Function0[Unit] | Null = null
  private var editorDomCleanup: (() => Unit) | Null = null
  private var floatingToolbarCleanup: js.Function0[Unit] | Null = null
  private var lastSeenValueJson: String | Null = null
  private var fallbackRendered = false
  private var toolbarRendered = false
  private var hydrationMountScheduled = false
  private var $toolbarMode: EditorToolbarMode = EditorToolbarMode.Ribbon
  private val editorRegistrations = mutable.ArrayBuffer.empty[js.Function0[Unit]]
  private val pluginComponents = mutable.ArrayBuffer.empty[EditorPlugin]

  override protected def composeFallback(): Unit = {
    given Component = this

    addClass("editor")
    addClass("jfx-editor-host")

    addDisposable($valueProperty.observe(_ => validate()))
    addDisposable($valueProperty.observeWithoutInitial(syncExternalValue))
    addDisposable($validators.observe(_ => validate()))
    addDisposable($dirtyProperty.observe(_ => validate()))
    addDisposable($placeholderProperty.observe(_ => refreshPlaceholder()))
    addDisposable($editableProperty.observe(editable => updateEditable(editable)))

    if (!$standalone) {
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

    val previewText = extractPreviewText($valueProperty.get)

    div {
      addClass("jfx-editor")
      div {
        addClass("jfx-editor__shell")
        renderFallbackToolbar()

        div {
          addClass("jfx-editor__surface-wrap")

          div {
            addClass("jfx-editor__preview")
            previewText.foreach(value => text = value)
          }

          div {
            addClass("jfx-editor__live-root")
          }

          div {
            addClass("jfx-editor__placeholder")
            visible = $placeholderProperty.map { placeholder =>
              previewText.isEmpty && Option(placeholder).exists(_.trim.nonEmpty)
            }
            text = $placeholderProperty.map(value => Option(value).map(_.trim).getOrElse(""))
          }
        }
      }
    }
  }

  private def renderFallbackToolbar()(using Component): Unit = {
    val elements = collectToolbarElements()
    val showToolbar = $editableProperty.get && elements.nonEmpty

    div {
      addClass("jfx-editor__toolbar")
      visible = showToolbar

      if (showToolbar) {
        $toolbarMode match {
          case EditorToolbarMode.Ribbon =>
            renderFallbackRibbonToolbar(elements)
          case EditorToolbarMode.Menu =>
            renderFallbackMenuToolbar(elements)
          case EditorToolbarMode.Floating =>
            renderFallbackFloatingToolbar(collectFloatingToolbarModules())
        }
      }
    }
  }

  private def renderFallbackRibbonToolbar(elements: Seq[ToolbarElement])(using Component): Unit = {
    val model = new ToolbarRegistry(elements.toList, ToolbarLayout.Ribbon).getModel

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

  private def renderFallbackMenuToolbar(elements: Seq[ToolbarElement])(using Component): Unit = {
    val model = new ToolbarRegistry(elements.toList, ToolbarLayout.Menu).getModel

    div {
      addClass("lexical-menu")

      div {
        addClass("lexical-menu-bar")

        model.tabs.foreach { tab =>
          div {
            addClass("lexical-menu-item")

            div {
              addClass("lexical-menu-trigger")
              addClass("lexical-ribbon-button")
              text = tab.name
            }

            div {
              addClass("lexical-menu-panel")
              addClass("lexical-dropdown-menu")
              style {
                display = "block"
                position = "static"
              }
              val showSectionLabels =
                !(tab.sections.length == 1 && tab.sections.headOption.exists(_.name == tab.name))

              tab.sections.foreach { section =>
                div {
                  addClass("lexical-menu-section")

                  if (showSectionLabels) {
                    div {
                      addClass("lexical-menu-section-label")
                      text = section.name
                    }
                  }

                  div {
                    addClass("lexical-menu-section-content")
                    section.modules.foreach(renderFallbackToolbarElement)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private def renderFallbackFloatingToolbar(modules: Seq[EditorModule])(using Component): Unit =
    if (modules.nonEmpty) {
      div {
        addClass("lexical-floating-toolbar")
        modules.foreach(renderFallbackToolbarElement)
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
    previewElement = null
    liveRoot = null
    toolbarHost = null
    placeholderElement = null

    renderClient {
      given Component = this

      div {
        addClass("jfx-editor")
        div {
          addClass("jfx-editor__shell")
          EditorToolbarHost(_ => ())

          div {
            addClass("jfx-editor__surface-wrap")

            div {
              addClass("jfx-editor__preview")
              text = previewProperty
            }

            EditorLiveRoot(_ => ())
            EditorPlaceholder($placeholderProperty, _ => ())
          }
        }
      }
    }

    host.domNode.collect { case element: HTMLElement =>
      toolbarHost = element.querySelector(".jfx-editor__toolbar").asInstanceOf[HTMLElement]
      previewElement = element.querySelector(".jfx-editor__preview").asInstanceOf[HTMLElement]
      liveRoot = element.querySelector(".jfx-editor__live-root").asInstanceOf[HTMLDivElement]
      placeholderElement = element.querySelector(".jfx-editor__placeholder").asInstanceOf[HTMLElement]
    }

    mountLexical()
  }

  override protected def activateClientSideContent(ssrContent: ClientSideSsrContent): Unit = {
    releaseFallbackChildren()
    toolbarRendered = false

    if (bindEditorDom(ssrContent.root.collect { case element: HTMLElement => element }.orNull) && hydratedDomReady()) {
      mountLexical()
    } else {
      scheduleHydratedMount(ssrContent)
    }
  }

  private[form] def registerPlugin(plugin: EditorPlugin): Unit =
    if (!pluginComponents.exists(_.name == plugin.name)) {
      pluginComponents += plugin
    }

  override def dispose(): Unit = {
    destroyEditorView()
    super.dispose()
  }

  private def bindEditorDom(rootElement: HTMLElement | Null): Boolean = {
    if (rootElement == null) {
      toolbarHost = null
      previewElement = null
      liveRoot = null
      placeholderElement = null
      false
    } else {
      toolbarHost = rootElement.querySelector(".jfx-editor__toolbar").asInstanceOf[HTMLElement | Null]
      previewElement = rootElement.querySelector(".jfx-editor__preview").asInstanceOf[HTMLElement | Null]
      liveRoot = rootElement.querySelector(".jfx-editor__live-root").asInstanceOf[HTMLDivElement | Null]
      placeholderElement = rootElement.querySelector(".jfx-editor__placeholder").asInstanceOf[HTMLElement | Null]
      true
    }
  }

  private def hydratedDomReady(): Boolean = {
    val hostElement = host.domNode.collect { case element: HTMLElement => element }.orNull

    hostElement != null &&
      hostElement.isConnected &&
      toolbarHost != null &&
      previewElement != null &&
      liveRoot != null &&
      liveRoot.nn.isConnected &&
      placeholderElement != null
  }

  private def scheduleHydratedMount(ssrContent: ClientSideSsrContent): Unit =
    if (!hydrationMountScheduled) {
      hydrationMountScheduled = true

      def attemptMount(): Unit = {
        hydrationMountScheduled = false

        val rootElement =
          ssrContent.root.collect { case element: HTMLElement => element }
            .orElse(host.domNode.collect { case element: HTMLElement => element })
            .orNull

        if (bindEditorDom(rootElement) && hydratedDomReady()) {
          mountLexical()
        } else {
          recoverWithClientRemount()
        }
      }

      window.requestAnimationFrame { (_: Double) =>
        if (lexicalEditor == null) {
          window.requestAnimationFrame { (_: Double) =>
            if (lexicalEditor == null) {
              attemptMount()
            }
          }
        }
      }
    }

  private def recoverWithClientRemount(): Unit = {
    clearFallback()
    mountClient()
  }

  private def mountLexical(): Unit = {
    val root = liveRoot
    if (root == null) {
      return
    }

    registerDomListeners(root.nn)
    root.nn.style.opacity = "0"

    val initialValue = $valueProperty.get

    val builder =
      new LexicalBuilder()
        .withNamespace($name)
        .withTheme(defaultTheme())
        .withEditable($editableProperty.get)
        .withNodes(defaultNodes())
        .withModules(collectModules() *)

    val editor = builder.build(root.nn)
    lexicalEditor = editor
    editor.setDialogService(new DefaultDialogService())

    root.nn.setAttribute("role", "textbox")
    root.nn.setAttribute("aria-multiline", "true")
    syncEditableSurface($editableProperty.get)

    if (initialValue != null) {
      applyEditorState(editor, initialValue)
    } else {
      publishEditorState(editor, markDirty = false)
    }

    editorUnregister = editor.registerUpdateListener { (_: js.Dynamic) =>
      refreshPlaceholder()
      publishEditorState(editor, markDirty = true)
    }

    renderToolbar(editor)
    installPlugins(editor)
    refreshPlaceholder()
    markClientReady()
  }

  private def destroyEditorView(): Unit = {
    $focusedProperty.set(false)

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

    cleanupFloatingToolbar()

    editorRegistrations.reverseIterator.foreach { unregister =>
      try unregister()
      catch {
        case _: Throwable =>
      }
    }
    editorRegistrations.clear()

    previewElement = null
    liveRoot = null
    if (toolbarHost != null) {
      toolbarHost.nn.innerHTML = ""
    }
    toolbarHost = null
    toolbarRendered = false
    placeholderElement = null
  }

  private def registerDomListeners(surface: HTMLDivElement): Unit = {
    val focusInListener: Event => Unit = _ => $focusedProperty.set(true)
    val focusOutListener: Event => Unit = _ => $focusedProperty.set(false)

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
      syncEditableSurface(editable)
      if (!editable) {
        lexicalEditor.nn.blur()
      }
      renderToolbar(lexicalEditor.nn)
    }

  private def syncEditableSurface(editable: Boolean): Unit =
    if (liveRoot != null) {
      val root = liveRoot.nn
      root.setAttribute("contenteditable", editable.toString)
      root.setAttribute("aria-readonly", (!editable).toString)
      root.classList.toggle("lexical-read-only", !editable)
      root.classList.toggle("lexical-editor-input", true)
    }

  private def renderToolbar(editor: LexicalEditor): Unit =
    if (toolbarHost != null) {
      val elements = collectToolbarElements()
      val floatingModules = collectFloatingToolbarModules()
      val host = toolbarHost.nn
      val showInlineToolbar =
        $editableProperty.get &&
          elements.nonEmpty &&
          $toolbarMode != EditorToolbarMode.Floating
      val showFloatingToolbar =
        $editableProperty.get &&
          floatingModules.nonEmpty &&
          $toolbarMode == EditorToolbarMode.Floating

      if (elements.isEmpty || !showInlineToolbar) {
        host.innerHTML = ""
        toolbarRendered = false
      }

      host.style.display =
        if (showInlineToolbar) ""
        else "none"

      if (showInlineToolbar && !toolbarRendered) {
        host.innerHTML = ""
        val registry = new ToolbarRegistry(elements.toList, inlineToolbarLayout())
        val manager = new ToolbarManager(editor, registry, inlineToolbarRenderer())
        manager.createToolbar(host)
        toolbarRendered = true
      }

      if (showFloatingToolbar) {
        if (floatingToolbarCleanup == null) {
          floatingToolbarCleanup = new FloatingToolbarManager(editor, floatingModules).register()
        }
      } else {
        cleanupFloatingToolbar()
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
    val state = editor.getEditorState().toJSON()
    val json = js.JSON.stringify(state)
    if (lastSeenValueJson != null && lastSeenValueJson == json) {
      return
    }

    lastSeenValueJson = json

    if (markDirty) {
      setDirty(true)
    }

    $valueProperty.set(state)
  }

  private def previewProperty: ReadOnlyProperty[String] =
    $valueProperty.map(value => extractPreviewText(value).getOrElse(""))

  private def markClientReady(): Unit = {
    if (liveRoot != null) {
      liveRoot.nn.style.opacity = "1"
    }

    if (previewElement != null) {
      previewElement.nn.classList.add("is-hidden")
      previewElement.nn.style.display = "none"
    }
  }

  private def updatePreviewElement(value: js.Any | Null): Unit =
    if (previewElement != null) {
      previewElement.nn.textContent = extractPreviewText(value).getOrElse("")
    }

  private def syncExternalValue(value: js.Any | Null): Unit =
    if (lexicalEditor != null) {
      val json = if (value == null) null else js.JSON.stringify(value)
      if (lastSeenValueJson == json) {
        return
      }

      applyEditorState(lexicalEditor.nn, value)
    }

  private def applyEditorState(editor: LexicalEditor, value: js.Any | Null): Unit = {
    try {
      parseEditorState(editor, value).foreach { state =>
        editor.setEditorState(state, js.Dynamic.literal())
        editor.read(() => ())
        updatePreviewElement(value)
        refreshPlaceholder()
        lastSeenValueJson = js.JSON.stringify(value)
      }
    } catch {
      case _: Throwable =>
    }
  }

  private def parseEditorState(editor: LexicalEditor, value: js.Any | Null): Option[js.Dynamic] =
    if (value == null || js.isUndefined(value.asInstanceOf[js.Any])) {
      None
    } else if (js.typeOf(value.asInstanceOf[js.Any]) == "string") {
      Option(value.asInstanceOf[String])
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(editor.parseEditorState)
    } else {
      Some(editor.parseEditorState(js.JSON.stringify(value)))
    }

  private def refreshPlaceholder(): Unit =
    if (placeholderElement != null && lexicalEditor != null) {
      val text = Option($placeholderProperty.get).map(_.trim).getOrElse("")
      placeholderElement.nn.textContent = text
      placeholderElement.nn.style.display =
        if (text.isEmpty || !editorIsEmpty(lexicalEditor.nn)) "none"
        else ""
    } else if (placeholderElement != null) {
      val previewEmpty = previewProperty.get.trim.isEmpty
      placeholderElement.nn.style.display =
        if (Option($placeholderProperty.get).forall(_.trim.isEmpty) || !previewEmpty) "none"
        else ""
    }

  private def editorIsEmpty(editor: LexicalEditor): Boolean = {
    var empty = true
    editor.getEditorState().read { () =>
      empty = Lexical.$getRoot().getTextContent().trim.isEmpty
    }
    empty
  }

  private def extractPreviewText(value: js.Any | Null): Option[String] =
    if (value == null || js.isUndefined(value.asInstanceOf[js.Any])) {
      None
    } else if (js.typeOf(value.asInstanceOf[js.Any]) == "string") {
      val text = value.asInstanceOf[String].trim
      if (text.isEmpty) None else Some(text)
    } else {
      val parts = mutable.ArrayBuffer.empty[String]
      val root = value.asInstanceOf[js.Dynamic].selectDynamic("root")
      val source = if (jsValueExists(root.asInstanceOf[js.Any])) root.asInstanceOf[js.Any] else value.asInstanceOf[js.Any]
      collectText(source, parts)
      Option(parts.mkString(" ").trim).filter(_.nonEmpty)
    }

  private def collectText(value: js.Any, parts: mutable.ArrayBuffer[String]): Unit =
    if (jsValueExists(value)) {
      val dynamic = value.asInstanceOf[js.Dynamic]
      val text = dynamic.selectDynamic("text").asInstanceOf[js.Any]
      if (jsValueExists(text) && js.typeOf(text) == "string") {
        parts += text.asInstanceOf[String]
      }

      val code = dynamic.selectDynamic("code").asInstanceOf[js.Any]
      if (jsValueExists(code) && js.typeOf(code) == "string") {
        parts += code.asInstanceOf[String]
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
        ).distinct *
    )

  private def collectToolbarElements(): Seq[ToolbarElement] =
    pluginComponents.iterator.flatMap(_.$toolbarElements).toSeq

  private def collectFloatingToolbarModules(): Seq[EditorModule] =
    collectToolbarElements().collect { case module: EditorModule => module }.distinct

  private def collectModules(): Seq[EditorModule] =
    (
      pluginComponents.iterator.flatMap(_.modules).toSeq ++
        collectToolbarElements().collect { case module: EditorModule => module }
      ).distinct

  private def inlineToolbarLayout(): ToolbarLayout =
    $toolbarMode match {
      case EditorToolbarMode.Ribbon => ToolbarLayout.Ribbon
      case EditorToolbarMode.Menu => ToolbarLayout.Menu
      case EditorToolbarMode.Floating => ToolbarLayout.Ribbon
    }

  private def inlineToolbarRenderer(): ToolbarRenderer =
    $toolbarMode match {
      case EditorToolbarMode.Ribbon => new RibbonRenderer()
      case EditorToolbarMode.Menu => new MenuRenderer()
      case EditorToolbarMode.Floating => new RibbonRenderer()
    }

  private def cleanupFloatingToolbar(): Unit =
    if (floatingToolbarCleanup != null) {
      try floatingToolbarCleanup.nn.apply()
      catch {
        case _: Throwable =>
      }
      floatingToolbarCleanup = null
    }

  private def defaultTheme(): EditorTheme =
    new EditorThemeBuilder()
      .withParagraph("lexical-paragraph")
      .withQuote("lexical-quote")

      .withHeading(1, "lexical-heading-h1")
      .withHeading(2, "lexical-heading-h2")
      .withHeading(3, "lexical-heading-h3")
      .withList("ul", "lexical-list-ul")

      .withList("ol", "lexical-list-ol")
      .withList("listitem", "lexical-listitem")
      .withTextBold("lexical-text-bold")
      .withTextItalic("lexical-text-italic")
      .withTextUnderline("lexical-text-underline")
      .withTextStrikethrough("lexical-text-strikethrough")
      .withCode("lexical-text-code")
      .build()
}

object Editor extends HasPlaceholder[Editor] with HasEditable[Editor] {
  def editor(name: String, standalone: Boolean = false)(init: Editor ?=> Unit = {}): Editor =
    DslRuntime.build(new Editor(name, standalone))(init)

  def value(using e: Editor): js.Any | Null =
    e.$valueProperty.get

  def value_=(using e: Editor)(nextValue: js.Any | Null): Unit =
    e.$valueProperty.set(nextValue)

  def valueProperty(using e: Editor): Property[js.Any | Null] =
    e.$valueProperty

  def toolbarMode(using e: Editor): EditorToolbarMode =
    e.$toolbarMode

  def toolbarMode_=(using e: Editor)(mode: EditorToolbarMode): Unit =
    e.$toolbarMode = mode

  def ribbonToolbar(using e: Editor): Unit =
    e.$toolbarMode = EditorToolbarMode.Ribbon

  def menuToolbar(using e: Editor): Unit =
    e.$toolbarMode = EditorToolbarMode.Menu

  def floatingToolbar(using e: Editor): Unit =
    e.$toolbarMode = EditorToolbarMode.Floating

}

private final class EditorLiveRoot(onReady: HTMLDivElement => Unit) extends Component {
  override def tagName: String = "div"

  override def compose(): Unit = {
    given Component = this

    addClass("jfx-editor__live-root")
    host.domNode.collect { case surface: HTMLDivElement => onReady(surface) }
  }
}

private object EditorLiveRoot {
  def apply(onReady: HTMLDivElement => Unit): EditorLiveRoot =
    DslRuntime.build(new EditorLiveRoot(onReady)) {}
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
