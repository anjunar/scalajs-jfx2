# scalajs-jfx2-editor

Rich text editing for JFX2, powered by Lexical and exposed as a normal form
control. The editor keeps the JFX2 contract intact: application state flows
through `Property`, forms bind by control name, SSR gets a stable fallback, and
plugins own editor-specific commands.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.2.1"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-controls" % "2.2.1"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-forms" % "2.2.1"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-editor" % "2.2.1"
```

## Productive Setup

Use one dialog service around the page or application shell, then install the
editor plugin set you actually want. `linkPlugin()` and `imagePlugin()` use the
dialog service for their insert dialogs.

```scala
import jfx.dsl.DslRuntime
import jfx.form.Editor.editor
import jfx.form.editor.plugins.*
import lexical.DialogService

val dialogs: DialogService = new DefaultDialogService()

DslRuntime.provide[DialogService](dialogs) {
  editor("body") {
    placeholder = "Write the article..."
    style { minHeight = "420px" }

    basePlugin()
    headingPlugin()
    listPlugin()
    linkPlugin()
    imagePlugin()
    tablePlugin()
    codePlugin()
  }
}
```

`DefaultDialogService` is useful out of the box and renders a small DOM modal with
the `jfx-dialog-*` CSS classes. In a real product you usually provide your own
`DialogService` once, backed by your app shell, viewport, window system, focus
trap, translations, and upload policy.

```scala
import lexical.DialogService
import org.scalajs.dom.HTMLElement

final class AppDialogService extends DialogService {
  override def show(
    title: String,
    contentProvider: () => HTMLElement,
    onConfirm: HTMLElement => Unit
  ): Unit = {
    // Open your app modal/window, mount contentProvider(),
    // call onConfirm(content) when the user confirms,
    // then close and dispose the dialog.
  }
}
```

For one special editor, override the service locally:

```scala
editor("body") {
  dialogService = AppDialogService()
  linkPlugin()
  imagePlugin()
}
```

## Blog Editor In Fewer Than 100 Lines

This is the whole flow: article properties bind into the form, the editor writes
Lexical state JSON into `body`, `JsonMapper` serializes the model, and the save
button posts it.

```scala
import jfx.action.Button.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.form.Editor.editor
import jfx.form.Form.form
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.editor.plugins.*
import jfx.json.JsonMapper
import jfx.layout.VBox.vbox
import lexical.DialogService
import org.scalajs.dom
import scala.scalajs.js

final class Article {
  val title: Property[String] = Property("")
  val teaser: Property[String] = Property("")
  val body: Property[js.Any | Null] = Property(null) // Lexical EditorState JSON
}

object ArticleEditor {
  private val article = Article()
  private val mapper = JsonMapper()
  private val dialogs: DialogService = new DefaultDialogService()

  def render(): Unit =
    DslRuntime.provide[DialogService](dialogs) {
      form(article) {
        vbox {
          style { gap = "16px" }

          inputContainer("Title") {
            input("title") { placeholder = "Why JFX2 now feels ready" }
          }

          inputContainer("Teaser") {
            input("teaser") { placeholder = "One sentence for the overview page" }
          }

          inputContainer("Body") {
            editor("body") {
              placeholder = "Write the article..."
              style { minHeight = "420px" }

              basePlugin()
              headingPlugin()
              listPlugin()
              linkPlugin()
              imagePlugin()
              tablePlugin()
              codePlugin()
            }
          }

          button("Save") {
            buttonType = "button"
            onClick { _ =>
              val payload = mapper.serialize(article)
              val request = js.Dynamic.literal(
                method = "POST",
                headers = js.Dictionary("Content-Type" -> "application/json"),
                body = js.JSON.stringify(payload)
              ).asInstanceOf[dom.RequestInit]

              dom.fetch("/api/articles", request)
            }
          }
        }
      }
    }
}
```

```text
Article properties -> Form binding -> Editor.valueProperty -> JsonMapper -> POST
```

Treat the editor value as serialized editor state, not as HTML. That preserves
undo/redo, selection, rich nodes, image nodes, SSR preview, and future migrations.

## Plugins

Plugins are meant to be installed together as a capability set, not discovered one
by one from scattered README snippets.

| Plugin | Adds |
| --- | --- |
| `basePlugin()` | Bold, italic, underline, strike-through and inline code commands |
| `headingPlugin()` | Paragraph and heading formats |
| `listPlugin()` | Ordered and unordered lists |
| `linkPlugin()` | Link insertion/editing through `DialogService` |
| `imagePlugin()` | Image dialog, preview, alt text, width and image node insertion |
| `tablePlugin()` | Table nodes and table toolbar commands |
| `codePlugin()` | Code block node and toolbar command |

For a blog/CMS editor, the full set above is the normal starting point. For a
comment box, start smaller:

```scala
editor("comment") {
  basePlugin()
  linkPlugin()
}
```

## Dialogs

Dialogs are part of the editor contract because several rich-text actions need
more than one click. JFX2 keeps that dependency explicit:

- `Editor` first uses a per-editor `dialogService = ...` override.
- If none is set, it asks `DslRuntime.service[DialogService]`.
- If no service is provided, it falls back to `DefaultDialogService`.

That means demos work immediately, while production code can install one app-wide
dialog implementation and keep link/image UX consistent with the rest of the
application.

## State Control

The raw `LexicalEditor` belongs at the plugin boundary:

```scala
final class MentionPlugin(users: Property[Seq[User]]) extends EditorPlugin {
  override val name = "mention"

  override def install(editor: lexical.LexicalEditor): js.Function0[Unit] = {
    // Register commands, nodes, listeners and cleanup here.
    () => ()
  }
}
```

Product code stays declarative and property-driven. Plugin code may use Lexical
commands because that is exactly where editor-specific imperative work belongs.

## Why Lexical And Not Quill Or TipTap?

JFX2 wants a declarative Scala DSL with explicit state flow. Lexical fits that
shape because its core is an editor model plus commands, nodes, and plugins. It
does not require React, Vue, or a virtual DOM in the integration layer, so JFX2 can
mount it as a client island while keeping the outer component SSR-safe.

Quill is easy to wrap at first, but the integration tends to become
`contenteditable` event archaeology: DOM mutations, selection edge cases, and
format deltas leak into application code. That works for simple text areas, but it
does not match JFX2's form-control contract.

TipTap is a strong editor, but its mainstream ergonomics are built around
framework bindings and ProseMirror extension wiring. It can be integrated, but the
result would pull JFX2 toward another component model instead of keeping the editor
as a normal JFX2 control.

Lexical gives this module the cleanest boundary:

- JFX2 owns the component shell, form binding, SSR fallback, validation, dialogs,
  and JSON serialization.
- Lexical owns editing, selection, commands, history, and rich text nodes.
- Plugins bridge both worlds without leaking editor internals into forms.
