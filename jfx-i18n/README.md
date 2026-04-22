# scalajs-jfx2-i18n

I18n keeps the source sentence in Scala and builds structured runtime messages with placeholders, fingerprints and source positions.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-i18n" % "2.1.0"
```

## Messages

```scala
import jfx.i18n.*

val name = "Mira"
val message = i18n"Hello $name"
```

Use named placeholders when the expression is not a simple stable identifier.

```scala
val count = 3
val message = i18n"Selected ${I18n.named("count", count)} documents"
```

## Context

```scala
val verb = i18nc("button")"Open"
val adjective = i18nc("state")"Open"
```

## Catalog

```scala
import jfx.i18n.*

val deleteMessage = i18n"Delete document"

val catalog = MessageCatalog(
  I18n.entry(deleteMessage.key).translations(
    I18nLocale("de") -> "Dokument loeschen",
    I18nLocale("fr") -> "Supprimer le document"
  )
)
```

## Resolver

```scala
val resolver = I18nResolver(catalog)

val text = resolver.resolve(deleteMessage, I18nLocale("de-AT"))
```

Locale fallback walks `de-AT`, then `de`, then the default locale.

## Reactive Locale

```scala
import jfx.core.state.Property

val locale = Property(I18nLocale("en"))
val resolved = resolver.resolve(deleteMessage, locale)

locale.set(I18nLocale("de"))
```

## Message State

Catalog entries can record review state and stale sources.

```scala
val value = MessageValue(
  translations = Map(I18nLocale("de") -> LocalizedPattern("Hallo")),
  state = MessageState.NeedsReview("Source text changed")
)
```
