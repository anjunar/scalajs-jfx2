# scalajs-jfx2

`scalajs-jfx2` is a Scala.js UI library for building application interfaces whose structure, state, metadata, and visible text stay readable in Scala.

It is not a React wrapper and it is not a thin DOM facade. JFX2 is a composed answer to frontend development in Scala.js: declarative templates, reactive state, SSR, hydration, typed controls, runtime domain metadata, JSON mapping, and source-first I18n are designed to work as one system.

## What It Is

JFX2 gives Scala.js applications a typed UI layer built around:

- declarative component composition
- reactive state primitives
- server-side rendering and browser hydration
- typed forms and controls
- route-aware application rendering
- virtualized data controls
- runtime class descriptors through a Scala.js classloader model
- JSON mapping for reflected domain objects
- source-first internationalization
- a companion CSS package for the provided components

The library lives under `library/src/main/scala/jfx`. Its public surface is intentionally direct: components, properties, controls, layouts, routing, SSR, hydration, reflection, JSON, and I18n are visible Scala APIs.

## Why This Library Exists

Most UI libraries stop at the component boundary. They help you produce DOM, then leave domain metadata, form binding, validation, serialization, SSR stability, and translation discipline as separate problems.

JFX2 takes a stricter path. A component template should describe a stable DOM shape. State should be explicit. Runtime metadata should be available where forms and JSON need it. Server HTML and the first hydrated browser tree should agree. Translatable text should remain readable in source code.

The result is not a generic frontend wrapper. It is a UI architecture for Scala.js applications where the domain model, component tree, rendering backend, and text system can cooperate without stringly glue.

## What Makes It Different

### Runtime Classloader For Scala.js Domains

JFX2 includes a metadata layer under `jfx.core.meta`. Domain classes can be registered with factories and reflected descriptors, then loaded later by type name.

That is unusual in a frontend UI library. It means browser-side code can inspect classes, properties, annotations, accessors, subtypes, and factories as part of normal application behavior.

```scala
import jfx.core.meta.PackageClassLoader
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*
import scala.annotation.meta.field

final class Address(
  @(NotBlank @field)()
  var city: Property[String] = Property("")
)

final class User(
  @(Size @field)(min = 3)
  var name: Property[String] = Property(""),
  var address: Property[Address] = Property(new Address()),
  var emails: ListProperty[String] = new ListProperty[String]()
)

val domains = PackageClassLoader("my.app.domain")
val userDescriptor = domains.register(() => new User(), classOf[User])
domains.register(() => new Address(), classOf[Address])

val reflectedNames =
  userDescriptor.resolved.properties.map(_.name).toVector

val emptyUser =
  domains.createInstance[User](classOf[User].getName)
```

The same metadata is used by form binding, validators, and JSON mapping. The model is not reduced to plain JavaScript objects just because it runs in the browser.

### Forms Bind To Models, Not To String Maps

Controls register into a form context. A control named `email` binds to the reflected `email` property on the model. Validator annotations on that property become validators on the control.

```scala
import jfx.core.component.Component.*
import jfx.form.Form.form
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.SubForm.subForm
import jfx.json.JsonMapper

val user = new User()

form(user) {
  inputContainer("Name") {
    input("name") {}
  }

  subForm("address") {
    inputContainer("City") {
      input("city") {}
    }
  }
}

val mapper = new JsonMapper()
val json = mapper.serialize(user)
val copy = mapper.deserialize[User](json)
```

`Property[String]` becomes a JSON string. `ListProperty[T]` becomes a JSON array. During deserialization, JFX2 creates instances through the reflected descriptors and writes back into properties.

### Virtualization Can Still Be Crawlable

Virtual lists and tables are built for large data, remote loading, and browser scrolling. They also include a crawlable SSR mode: on the server, they can render a stable slice from route query parameters and expose a next-page link.

```scala
import jfx.control.VirtualListView.virtualList
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div

val rows = new ListProperty[String]()
rows.setAll((1 to 10000).map(i => s"Row $i"))

virtualList(rows, estimateHeightPx = 40, crawlable = true) { (row, index) =>
  div {
    classes = "row"
    text = if (row == null) s"Loading row $index" else row
  }
}
```

This is not only a scrolling trick. The rendering backend matters: browser rendering, SSR rendering, hydration, route query state, and virtualized content all participate in the same component model.

### Source-First I18n

JFX2 does not make artificial message keys the primary identity of text. The English source sentence is the visible identity:

```scala
import jfx.i18n.*

val locale = I18nLocale("de-AT")
val user = "Mira"
val group = "Architecture"

val invite =
  i18n"User $user invited you to $group"

val catalog =
  MessageCatalog(
    I18n.entry(invite.key).translations(
      I18nLocale("de") -> "Benutzer {user} hat dich zu {group} eingeladen",
      I18nLocale("fr") -> "L'utilisateur {user} vous a invite dans {group}"
    )
  )

val text =
  new I18nResolver(catalog).resolve(invite, locale)
```

The interpolator creates a structured message with placeholder names, source position, context, and fingerprint. Fallback chains such as `de-AT -> de -> en` are part of runtime resolution. The English sentence stays in the Scala source.

### SSR And Hydration Are First-Class Constraints

JFX2 has separate rendering backends for server rendering, browser rendering, and hydration. Components are expected to keep their first server and first client trees structurally stable.

```scala
import jfx.hydration.Hydration
import jfx.ssr.Ssr
import org.scalajs.dom

val html: String =
  Ssr.renderToString(appRoot())

Hydration.hydrate(dom.document.getElementById("app")) {
  appRoot()
}
```

The DSL is shaped by this: conditional rendering, virtual containers, async routes, and client-only behavior must preserve predictable DOM paths.

## Core Ideas

### The Template Should Tell The Truth

A component's `compose()` method is treated as a template. It should declare structure, child components, classes, text, properties, and event bindings. Manual DOM mutation and layout measurement do not belong there.

```scala
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

val locale = Property(I18nLocale.En)
val resolver = new I18nResolver(MessageCatalog.empty)

def t(message: RuntimeMessage): ReadOnlyProperty[String] =
  resolver.resolve(message, locale)

def profilePanel(name: Property[String]) =
  vbox {
    classes = "profile-panel"

    div {
      classes = "profile-panel__title"
      text = t(i18n"User profile")
    }

    div {
      classes = "profile-panel__name"
      text = name.map(current => s"Name: $current")
    }

    button(t(i18n"Reset")) {
      onClick { _ => name.set("Ada") }
    }
  }
```

The important parts stay visible: structure, classes, reactive state, interaction, and source text identity.

### State Is Small And Explicit

The core state model is intentionally compact:

- `Property[T]`
- `ReadOnlyProperty[T]`
- `ListProperty[T]`
- `RemoteListProperty[T, Q]`

These types carry values into text nodes, classes, styles, forms, lists, tables, route-sensitive views, and locale-sensitive rendering.

### Metadata Is Part Of The UI Architecture

The classloader and reflection layer are not decorative. They let forms, validators, JSON mapping, and runtime tooling talk about real Scala classes rather than hand-maintained string schemas.

### Text Is Source, Not A Hidden Key

Translation starts with complete English messages in Scala source:

- `i18n"Delete document"`
- `i18n"User $user invited you to $group"`
- `i18nc("verb")"Open"`
- `i18nc("adjective")"Open"`

Technical fingerprints exist for lookup and stale handling, but they do not replace the visible source sentence.

## Highlights

- Scala 3 DSL for readable component templates
- runtime class descriptors and factories through `jfx.core.meta`
- reflection-aware JSON mapping through `jfx.json.JsonMapper`
- model-bound forms with annotation-derived validators
- reactive properties for values, derived text, classes, lists, and UI state
- SSR rendering via `jfx.ssr.Ssr`
- browser hydration via `jfx.hydration.Hydration`
- typed routing through `jfx.router`
- data controls including table and virtual list components
- crawlable SSR slices for virtualized content
- source-first I18n with `i18n"..."`, `i18nc("context")"..."`, and named placeholders
- companion CSS distributed through the npm package

## Quickstart

In a Scala.js build, add the library as a dependency:

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2" % "2.0.0"
```

Use the companion CSS package in the browser build:

```bash
npm install @anjunar/scalajs-jfx2
```

Import the CSS from your JavaScript or TypeScript entry point:

```js
import "@anjunar/scalajs-jfx2/index.css"
```

For local development inside this repository, use the provided `sbtn` binary:

```powershell
sbtn-x86_64-pc-win32.exe scalajs-jfx2/test
```

## Design Principles

- Keep `compose()` declarative.
- Attach behavior through DSL events such as `onClick`, `onInput`, `onScroll`, and `onKeyDown`.
- Bind styles and text through properties where the value is reactive.
- Preserve stable DOM paths for SSR and hydration.
- Register domain classes when runtime metadata is needed.
- Translate complete messages, not fragments.
- Keep English text in source code.
- Model pluralization and variants structurally, never through string concatenation.
- Treat number, date, time, and currency formatting as separate concerns from text translation.

## What You Can Build With It

JFX2 is aimed at application interfaces, not brochure pages.

It is a fit for:

- data-heavy internal tools
- form-driven business applications
- dashboards with live reactive state
- routed Scala.js frontends
- SSR-rendered application shells
- metadata-driven forms and inspectors
- JSON-backed domain editing tools
- virtualized tables and lists with remote loading
- interfaces where English source text must remain visible and translatable

## Examples And Docs

The repository includes executable examples and generated documentation alongside the library:

- `library/src/main/scala/jfx` contains the library source.
- `application/src/main/scala/app` shows the components in use.
- `docs` contains generated Scaladoc.
- `npm/scalajs-jfx2` contains the CSS companion package source.

The best way to understand JFX2 is to read the library APIs and then follow how the same primitives appear in composed screens: class descriptors, forms, JSON, controls, virtual lists, routing, SSR, hydration, and I18n all meet there.

## Who It Is For

JFX2 is for Scala developers who want frontend code to remain architectural.

It suits teams that care about typed APIs, runtime domain metadata, SSR discipline, stable component structure, readable source text, and UI code that can be reviewed without mentally reconstructing hidden DOM behavior.

It is not for projects that want to treat Scala.js as a syntax layer over an existing JavaScript framework.

## Current Direction

JFX2 is young, but its direction is clear:

- deepen the component DSL without weakening its declarative rules
- keep SSR and hydration constraints central
- make runtime metadata useful across forms, JSON, validation, and tooling
- grow typed controls from real application needs
- keep I18n source-first and message-centered
- preserve a codebase that experienced developers can read end to end

## Explore

Start with `library/src/main/scala/jfx/core`, then move through `core/meta`, `form`, `json`, `control`, `router`, `ssr`, `hydration`, and `i18n`.

The library's character is in the code: explicit state, visible structure, runtime metadata, and a DSL that treats UI as something worth designing carefully.

## License

MIT
