# scalajs-jfx2

`scalajs-jfx2` is a Scala.js UI library for building interfaces whose structure is explicit, reactive, and ready for server-side rendering.

It is not a React wrapper and it is not a thin DOM facade. JFX2 is a component DSL with its own discipline: the Scala source should reveal the UI, the state model, the visible text, and the interaction points without hiding imperative DOM work inside the template.

## What It Is

JFX2 gives Scala.js applications a typed UI layer built around:

- declarative component composition
- small reactive state primitives
- server-side rendering and browser hydration
- typed forms and controls
- routing
- source-first internationalization
- a companion CSS package for the provided components

The library lives under `library/src/main/scala/jfx`. Its public surface is intentionally direct: components, properties, controls, layouts, routing, SSR, hydration, and I18n are all visible as Scala APIs.

## Why This Library Exists

Most web UI code drifts toward one of two problems: markup becomes string output, or application logic dissolves into framework ceremony.

JFX2 takes a stricter path. A component template should describe a stable DOM shape. State should be explicit. Events should be attached through the DSL. Server HTML and the first hydrated browser tree should agree. Translatable text should remain readable in source code.

That discipline matters when an application grows from a few controls into real product surfaces: forms, overlays, tables, lists, editors, route changes, locale changes, and SSR all need to cooperate without turning the UI into a bag of side effects.

## What You Can Build With It

JFX2 is aimed at application interfaces, not brochure pages.

It is a fit for:

- data-heavy internal tools
- form-driven business applications
- dashboards with live reactive state
- routed Scala.js frontends
- SSR-rendered application shells
- component libraries that need typed APIs instead of stringly DOM code
- interfaces where English source text must remain visible and translatable

## Core Ideas

### Declarative Components

A component's `compose()` method is treated as a template. It should declare structure, child components, classes, text, properties, and event bindings. Manual DOM mutation and layout measurement do not belong there.

### Explicit Reactive State

The core state model is small:

- `Property[T]`
- `ReadOnlyProperty[T]`
- `ListProperty[T]`
- `RemoteListProperty[T, Q]`

These types carry UI state into text nodes, classes, styles, forms, collections, controls, and locale-sensitive rendering.

### SSR And Hydration By Design

JFX2 includes server-side rendering entry points and a hydration layer. This shapes the component model: the initial server tree and initial client tree must be structurally stable, especially around conditional content and virtual containers.

### Typed Controls

Controls such as buttons, inputs, combo boxes, forms, tables, virtual lists, editors, image tools, links, windows, drawers, and overlays are exposed as Scala components with typed configuration rather than raw DOM strings.

### Source-First I18n

JFX2 does not make artificial message keys the primary identity of text. The English source sentence remains visible:

```scala
i18n"Delete document"
i18n"User $user invited you to $group"
i18nc("verb")"Open"
```

Internally, interpolated text becomes a structured message with named placeholders, metadata, and a multilingual value model. English stays the natural fallback; technical fingerprints may support lookup and stale handling, but they do not replace the visible source text.

## Highlights

- Scala 3 DSL for readable component templates
- reactive properties for values, derived text, classes, lists, and UI state
- SSR rendering via `jfx.ssr.Ssr`
- browser hydration via `jfx.hydration.Hydration`
- typed routing through `jfx.router`
- form APIs with validation-oriented building blocks
- data controls including table and virtual list components
- source-first I18n with `i18n"..."`, `i18nc("context")"..."`, and named placeholders
- companion CSS distributed through the npm package

## Quickstart

In a Scala.js build, add the library as a dependency:

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2" % "1.0.0"
```

Use the companion CSS package in the browser build:

```bash
npm install @anjunar/scalajs-jfx
```

Then import the CSS from your JavaScript or TypeScript entry point:

```js
import "@anjunar/scalajs-jfx/index.css"
```

For local development inside this repository, use the provided `sbtn` binary:

```powershell
sbtn-x86_64-pc-win32.exe scalajs-jfx2/test
```

## A Small Component

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

## Design Principles

- Keep `compose()` declarative.
- Attach behavior through DSL events such as `onClick`, `onInput`, `onScroll`, and `onKeyDown`.
- Bind styles and text through properties where the value is reactive.
- Preserve stable DOM paths for SSR and hydration.
- Translate complete messages, not fragments.
- Keep English text in source code.
- Model pluralization and variants structurally, never through string concatenation.
- Treat number, date, time, and currency formatting as separate concerns from text translation.

## Examples And Docs

The repository includes executable examples and generated documentation alongside the library:

- `library/src/main/scala/jfx` contains the library source.
- `application/src/main/scala/app` shows the components in use.
- `docs` contains generated Scaladoc.
- `npm/scalajs-jfx2` contains the CSS companion package source.

The best way to understand JFX2 is to read the library APIs and then follow the component usage in the repository. The code is small enough to inspect and strict enough to reward that inspection.

## Who It Is For

JFX2 is for Scala developers who want frontend code to remain architectural.

It suits teams that care about typed APIs, SSR discipline, stable component structure, readable source text, and UI code that can be reviewed without mentally reconstructing hidden DOM behavior.

It is not for projects that want to treat Scala.js as a syntax layer over an existing JavaScript framework.

## Current Direction

JFX2 is young, but its direction is clear:

- deepen the component DSL without weakening its declarative rules
- keep SSR and hydration constraints central
- grow typed controls from real application needs
- keep I18n source-first and message-centered
- preserve a codebase that experienced developers can read end to end

## Explore

Start with `library/src/main/scala/jfx/core`, then move through `layout`, `control`, `form`, `router`, `ssr`, `hydration`, and `i18n`.

The library's character is in the code: explicit state, visible structure, and a DSL that treats UI as something worth designing carefully.

## License

MIT
