# scalajs-jfx2-core

Core contains the component model, reactive state, base DSL, simple layout components, SSR and hydration.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.2"
```

## State

`Property[T]` is the basic reactive value.

```scala
import jfx.core.state.Property

val title = Property("Welcome")
val dispose = title.observe(next => println(next))

title.set("Welcome back")
dispose.dispose()
```

`ListProperty[T]` tracks list changes and is used by `forEach`.

```scala
import jfx.core.state.ListProperty

val items = ListProperty[String]()
items += "One"
items.setAll(Seq("One", "Two", "Three"))
```

`RemoteListProperty[T, Q]` describes async pages.

```scala
import jfx.core.state.ListProperty
import scala.scalajs.js

final case class Page(offset: Int, limit: Int)

val rows = ListProperty.remote[String, Page](
  loader = ListProperty.RemoteLoader { query =>
    js.Promise.resolve(
      ListProperty.RemotePage(
        items = Seq("A", "B"),
        offset = Some(query.offset),
        nextQuery = Some(Page(query.offset + query.limit, query.limit)),
        totalCount = None,
        hasMore = Some(true)
      )
    )
  },
  initialQuery = Page(0, 50)
)
```

## Components And Text

`Box.box` creates a generic element. `Text.text` creates text nodes.

```scala
import jfx.core.component.Box.box
import jfx.core.component.Component.*
import jfx.core.component.Text.text

box("section") {
  classes = Seq("profile")
  text("Static text")
}
```

`ClientSideComponent` is for browser-only components with SSR fallback content.

```scala
import jfx.core.component.ClientSideComponent
import jfx.core.component.Component.*

final class BrowserOnlyChart extends ClientSideComponent {
  override def tagName: String = "div"

  override protected def composeFallback(): Unit =
    text = "Chart"

  override protected def mountClient(): Unit =
    ()
}
```

## Button

```scala
import jfx.action.Button.button
import jfx.core.component.Component.*

button("Save") {
  buttonType = "submit"
  onClick { event =>
    event.preventDefault()
  }
}
```

## Layout

`div`, `vbox`, `hbox`, `span`, `heading` and `horizontalLine` are small structural tags.

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Heading.heading
import jfx.layout.HorizontalLine.horizontalLine
import jfx.layout.Span.span
import jfx.layout.VBox.vbox

vbox {
  heading(2) {
    text = "Settings"
  }

  horizontalLine()

  hbox {
    span { text = "Mode" }
    div { text = "Automatic" }
  }
}
```

## Drawer

```scala
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.Drawer.*

drawer {
  open = true
  drawerWidth = "280px"

  drawerNavigation {
    button("Toggle") {
      onClick { _ => toggle() }
    }
  }

  drawerContent {
    div { text = "Main content" }
  }
}
```

## Statements

`condition` renders one branch.

```scala
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.layout.Div.div
import jfx.statement.Condition.*

val loggedIn = Property(false)

condition(loggedIn) {
  thenDo {
    div { text = "Account" }
  }
  elseDo {
    div { text = "Sign in" }
  }
}
```

`conditional` is the context-aware variant.

```scala
import jfx.statement.Conditional.*

conditional(loggedIn) {
  thenDo {
    div { text = "Ready" }
  }
}
```

`forEach` renders `ListProperty` items.

```scala
import jfx.statement.ForEach.forEach

val names = ListProperty[String]()
names.setAll(Seq("Ada", "Grace"))

forEach(names) { (name, index) =>
  div { text = s"$index: $name" }
}
```

`observeRender` re-renders a small fragment when a property changes.

```scala
import jfx.statement.ObserveRender.observeRender

val status = Property("Idle")

observeRender(status) { value =>
  div { text = value }
}
```

`dynamicOutlet` renders the current component from a property.

```scala
import jfx.core.component.Component
import jfx.statement.DynamicOutlet.dynamicOutlet

val current = Property[Component | Null](null)

dynamicOutlet(current)
```

## Styles And Events

Component DSL setters accept plain values and reactive properties.

```scala
val width = Property("320px")

div {
  classes = Seq("panel")
  visible = Property(true)

  style {
    width_=(width)
  }

  onClick { _ => width.set("480px") }
}
```

## Scoped Services

```scala
import jfx.dsl.DslRuntime

final class SessionService {
  def userName: String = "Ada"
}

DslRuntime.provide(new SessionService) {
  div {
    text = DslRuntime.service[SessionService].userName
  }
}
```

## SSR And Hydration

```scala
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.ssr.Ssr
import org.scalajs.dom

val html = Ssr.renderToString {
  div { text = "Hello SSR" }
}

Hydration.hydrate(dom.document.getElementById("app")) {
  div { text = "Hello SSR" }
}
```

## Domain Types

`Media` and `Thumbnail` are property-based models used by media controls.

```scala
import jfx.core.state.Property
import jfx.domain.Media

val media = Media()
media.name.set("avatar.png")
media.contentType.set("image/png")
media.data.set("data:image/png;base64,...")
```
