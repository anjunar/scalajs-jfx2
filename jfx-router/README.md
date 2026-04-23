# scalajs-jfx2-router

Router provides route definitions, async route loading, current route context and browser navigation.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.1.2"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-router" % "2.1.2"
```

## Router

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.router.Route.*
import jfx.router.Router.router

router(Seq(
  asyncRoute("/") {
    page {
      div { text = "Home" }
    }
  },
  asyncRoute("/users") {
    page {
      div { text = "Users" }
    }
  }
))
```

## Route Context

The route context is available inside a route factory.

```scala
import jfx.router.RouteContext

asyncRoute("/users") {
  page {
    val ctx = summon[RouteContext]
    div {
      text = s"Current path: ${ctx.path}"
    }
  }
}
```

## Async Routes

```scala
import jfx.router.Route
import scala.scalajs.js

asyncRoute("/reports") {
  Route.load(
    js.Promise.resolve(
      Route.factory {
        div { text = "Reports loaded" }
      }
    )
  )
}
```

## Navigation

```scala
import jfx.action.Button.button
import jfx.router.Route.asyncRoute
import jfx.router.Route.page
import jfx.router.Router.router

val appRouter = router(Seq(
  asyncRoute("/") { page { div { text = "Home" } } },
  asyncRoute("/users") { page { div { text = "Users" } } }
))

button("Open users") {
  onClick { _ =>
    appRouter.navigate("/users")
  }
}
```

Use `replace = true` when the current browser history entry should be replaced.

## Base Path

`RouterConfig` resolves links against the configured base path.

```scala
import jfx.router.RouterConfig

RouterConfig.basePath = "/scalajs-jfx2"

val href = RouterConfig.resolve("/users")
```

## SSR

Pass the initial URL when rendering on the server or in tests.

```scala
router(
  routes = Seq(asyncRoute("/") { page { div { text = "Home" } } }),
  initial = "/"
)
```
