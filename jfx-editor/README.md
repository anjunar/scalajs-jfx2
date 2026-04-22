# scalajs-jfx2-editor

Editor wraps Lexical as a form control and provides plugin hooks for common rich text features.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.0.2"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-controls" % "2.0.2"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-forms" % "2.0.2"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-editor" % "2.0.2"
```

## Editor

```scala
import jfx.form.Editor.editor

editor("body") {
  placeholder = "Write something"
  editable = true
}
```

It can be used inside a form like any other control.

```scala
import jfx.core.state.Property
import jfx.form.Form.form
import scala.scalajs.js

final class Article {
  val body: Property[js.Any | Null] = Property(null)
}

form(Article()) {
  editor("body") {
    placeholder = "Article body"
  }
}
```

## Base Plugin

```scala
import jfx.form.editor.plugins.basePlugin

editor("body") {
  basePlugin()
}
```

## Heading Plugin

```scala
import jfx.form.editor.plugins.headingPlugin

editor("body") {
  headingPlugin()
}
```

## List Plugin

```scala
import jfx.form.editor.plugins.listPlugin

editor("body") {
  listPlugin()
}
```

## Link Plugin

```scala
import jfx.form.editor.plugins.linkPlugin

editor("body") {
  linkPlugin()
}
```

## Image Plugin

```scala
import jfx.form.editor.plugins.imagePlugin

editor("body") {
  imagePlugin()
}
```

## Table Plugin

```scala
import jfx.form.editor.plugins.tablePlugin

editor("body") {
  tablePlugin()
}
```

## Code Plugin

```scala
import jfx.form.editor.plugins.codePlugin

editor("body") {
  codePlugin()
}
```

## Default Dialog Service

Plugins that need dialogs can use the default viewport-backed service.

```scala
import jfx.dsl.DslRuntime
import jfx.form.editor.plugins.DefaultDialogService
import jfx.form.editor.plugins.imagePlugin
import jfx.form.editor.plugins.linkPlugin
import lexical.DialogService

DslRuntime.provide[DialogService](new DefaultDialogService) {
  editor("body") {
    linkPlugin()
    imagePlugin()
  }
}
```
