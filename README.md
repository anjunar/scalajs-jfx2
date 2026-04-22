# scalajs-jfx2

> A reactive UI library for Scala.js - built from 17 years of searching for clarity.

---

## Why this exists

I have used Angular. React. Svelte. SolidJS. Dozens of others.

Each one promised simplicity. Each one delivered a different kind of complexity: hidden lifecycle management, implicit reactivity storms, state that moves in ways you cannot predict, and a growing feeling that the framework - not you - is in charge.

After 17 years I stopped looking for the right framework and built the thing I actually wanted.

Not a framework that does more. A framework that does *less, but honestly*.

---

## What JFX2 is

JFX2 is a reactive UI library for Scala.js with a hard focus on three things:

**Explicit lifecycles.** Every component lives in a controlled scope. You know exactly when it starts, what it owns, and when it is gone. No hidden subscriptions. No memory leaks you discover six months later.

**Predictable state.** `Property[T]` is the core reactive primitive. It has no magic. It does not re-render your whole tree. It updates exactly what you tell it to update.

**A declarative DSL that stays readable.** UI structure is declared in Scala, close to the metal, without virtual DOM overhead and without the ceremony of a JSX transpiler.

Here is the kind of code JFX2 is trying to protect:

```scala
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

val count = Property(0)

vbox {
  classes = Seq("page")

  div {
    text = count.map(value => s"Count: $value")
  }

  button("Increment") {
    onClick { _ => count.set(count.get + 1) }
  }
}
```

You can read this six months later and still nod at it.

---

## What it feels like

JFX2 is not trying to hide the shape of the UI behind abstractions that need a manual.

It is trying to keep the shape visible.

The value is not just that this compiles. The value is that it still makes sense later.

---

## How it compares

| | JFX2 | Laminar | Raw Scala.js DOM |
|---|---|---|---|
| Lifecycle model | Explicit scopes | Signal-based | Manual |
| State | `Property[T]`, predictable | `Signal`/`Var`, reactive graph | Manual |
| DSL | Declarative, JavaFX-inspired | Declarative, FRP-style | Imperative |
| SSR + Hydration | Yes | Limited | No |
| Forms + Validation | Built-in | Third-party | Manual |
| Virtual lists | Built-in | Third-party | Manual |

JFX2 is not better than Laminar. It is *different*. Laminar is excellent for functional-reactive programming. JFX2 is for developers who want **structure over abstraction** - explicit over implicit, scoped over ambient, readable over clever.

---

## Core concepts

### Scoped lifecycle

Every UI subtree lives in a component scope. When the scope ends, everything inside it - subscriptions, services, child components - is disposed deterministically.

That makes it safe to use inside virtualized lists, modals, and dynamically mounted routes without worrying about what you left behind.

No hidden dependency tracking. No surprise re-renders. You observe what you choose to observe.

### Buttons, links, and simple interaction

The smallest useful screen should still feel calm.

The event model stays direct. The code reads like intent, not ceremony.

### Reactive state

`Property[T]` is intentionally small.

There is no hidden graph to understand. If you observe it, you own it.

### Scoped dependency injection

Services are registered and resolved within the current DSL context. No global singletons. No implicit parameters threading through your entire codebase.

### Forms with validation

Forms bind directly to model properties by name. Validation annotations live on the model, so the form does not need to be hand-wired field by field.

The form connects automatically. No manual wiring. No string-map choreography.

### JSON stays close to the model

The same property-oriented model can be serialized and restored without hand-written mapping code.

That matters because the data model and the UI model are not treated as separate kingdoms.

### Combo boxes that stay honest

Complex controls are still kept inside the same DSL.

That matters because the control is not a separate universe. It is still just a component with a lifecycle, state, and structure.

### Routing and navigation

Routes are part of the component model too.

The important part is not syntax. It is that navigation, render state, and URL state remain legible.

### SSR + Hydration

Server HTML and client hydration share the same component structure. No duplicated templates. No DOM drift.

### Virtual lists

Large data sets should feel calm too.

This is not only a scrolling trick. Browser rendering, SSR rendering, hydration, route query state, and virtualized content all participate in the same component model.

### Source-first i18n

JFX2 does not make artificial message keys the primary identity of text. The English source sentence stays visible in Scala.

The interpolator creates structured messages with placeholder names, source position, and fingerprinting for lookup and stale handling.

---

## What it ships with

- `Property[T]` and `ListProperty[T]` - reactive primitives
- `RemoteListProperty[T, Q]` - async list loading and range queries
- Component scopes via `DslRuntime.withComponentScope`
- `DslRuntime.provide` / `DslRuntime.service` - scoped dependency lookup
- Layout DSL - `vbox`, `hbox`, `div`, `viewport`, `window`, `drawer`
- `Form` - model binding and validation with annotations
- `Router` - async, nested, with path params and query strings
- `VirtualListView` - efficient rendering of large datasets
- `JsonMapper` - serialize/deserialize models with `Property` fields
- i18n - message-centered, type-safe, multi-locale

---

## Module documentation

Each module has its own README with installation notes and examples for the components or APIs it owns.

| Module | Purpose |
|---|---|
| [jfx-core](jfx-core/README.md) | Component lifecycle, DSL, state, layouts, SSR and hydration |
| [jfx-router](jfx-router/README.md) | Async routes, route context, query parameters and navigation |
| [jfx-viewport](jfx-viewport/README.md) | Global viewport, windows, notifications and anchored overlays |
| [jfx-i18n](jfx-i18n/README.md) | Source-first messages, catalogs, locale fallback and interpolation |
| [jfx-json](jfx-json/README.md) | Reflection based JSON serialization for `Property` models |
| [jfx-controls](jfx-controls/README.md) | Link, image, table view and virtual list view |
| [jfx-forms](jfx-forms/README.md) | Forms, inputs, validation, combo boxes, array forms and image cropper |
| [jfx-editor](jfx-editor/README.md) | Lexical based rich text editor and editor plugins |

---

## Installation

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-router" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-viewport" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-i18n" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-json" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-controls" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-forms" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-editor" % "2.1.0" // optional, includes Lexical
```

Requires Scala 3.8.3, Scala.js, and ES module output targeting ES2021.

For local development in this repository:

```powershell
sbtn-x86_64-pc-win32.exe scalajs-jfx2-core/test
sbtn-x86_64-pc-win32.exe scalajs-jfx2-controls/test
```

If you want the companion CSS package in the browser build:

```bash
npm install @anjunar/scalajs-jfx2
```

---

## When to use it

Use JFX2 if you are building:

- complex data UIs - tables, dashboards, editors
- applications where lifecycle leaks are a real cost
- Scala backends where you want the same language on the frontend
- anything where you want to understand every moving part

Do not use it if:

- you want the smallest possible bundle for a simple page
- you prefer a purely functional reactive model
- you need a large ecosystem of third-party components today

---

## Status

- Published on Maven Central
- Used in production (Technology Speaks)
- Core patterns are stable
- Active development - feedback is welcome

---

## A note on the design

This library is not an experiment. It is the result of a long and honest search for an architecture that I could trust - one where I know what is happening, where state lives, and what gets cleaned up.

Every decision in JFX2 is a choice for explicitness over magic. That comes with tradeoffs. It asks more of you upfront. In return it gives you something most frameworks do not: a codebase that is still legible and maintainable when it grows.

If that matters to you, you will feel at home here.

---

## License

MIT - © 2026 Anjunar
