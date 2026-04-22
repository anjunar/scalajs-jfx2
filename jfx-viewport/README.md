# scalajs-jfx2-viewport

Viewport owns global UI that floats above the page: windows, anchored overlays and notifications.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.0"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-viewport" % "2.1.0"
```

## Viewport

Place one viewport near the root of the application.

```scala
import jfx.layout.Viewport.viewport

viewport()
```

## Notifications

```scala
import jfx.action.Button.button
import jfx.layout.Viewport
import jfx.layout.Viewport.NotificationKind

button("Notify") {
  onClick { _ =>
    Viewport.notify("Saved", NotificationKind.Success)
  }
}
```

## Windows

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.Viewport

Viewport.addWindow(new Viewport.WindowConf(
  title = "Details",
  width = 420,
  height = 260,
  component = () =>
    div {
      text = "Window content"
    }
))
```

## Window DSL

`window` can also be used directly when a local window surface is needed.

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.Window.*

window {
  title = "Inspector"
  resizeable = true
  draggable = true
  rememberPosition = true
  positionStorageKey = "inspector"

  content { () =>
    div { text = "Inspector content" }
  }
}
```

## Anchored Overlay

The `Overlay.overlay` helper binds an overlay to the component that creates it.

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.Overlay.overlay

div {
  classes = Seq("menu-anchor")
  text = "Open"

  overlay(widthPx = 220) {
    div {
      classes = Seq("menu")
      text = "Overlay content"
    }
  }
}
```

## Viewport Overlay Configuration

Use `Viewport.OverlayConf` when opening an overlay from a DOM anchor.

```scala
import jfx.layout.Viewport
import org.scalajs.dom.HTMLElement

def openMenu(anchor: HTMLElement): Unit =
  Viewport.addOverlay(new Viewport.OverlayConf(
    anchor = anchor,
    widthPx = Some(240),
    content = () => div { text = "Menu item" }
  ))
```

## Close-Aware Window Content

Window content can close its containing viewport window by implementing `Viewport.CloseAware`.

```scala
import jfx.core.component.Component
import jfx.layout.Viewport

final class DialogBody extends Component with Viewport.CloseAware {
  override def tagName: String = "div"
  private var close: () => Unit = () => ()

  override def close_=(callback: () => Unit): Unit =
    close = callback

  override def compose(): Unit =
    jfx.action.Button.button("Close") {
      onClick { _ => close() }
    }
}
```
