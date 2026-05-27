# scalajs-jfx2-router

Router provides route definitions, async component loading, optional stateful route reuse, current route context and browser navigation.

## Install

```scala
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-core" % "2.2.5-SNAPSHOT"
libraryDependencies += "com.anjunar" %%% "scalajs-jfx2-router" % "2.2.5-SNAPSHOT"
```

## Router

```scala
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.router.Route.*
import jfx.router.Router.{loading, router}
import scala.concurrent.Future

router(Seq(
  route("/") { _ =>
    Future.successful(component {
      div { text = "Home" }
    })
  },
  route("/users", stateful = true) { _ =>
    Future.successful(component {
      div { text = "Users" }
    })
  }
)) {
  loading {
    div {
      classes = Seq("app-router-loading")
      text = "Bitte warten ..."
    }
  }
}
```

## Route Context

The route context is passed directly into the route loader.

```scala
import scala.concurrent.Future

route("/users") { ctx =>
  Future.successful(component {
    div {
      text = s"Current path: ${ctx.path}"
    }
  })
}
```

## Async Routes

```scala
import jfx.router.Route
import scala.concurrent.Future

route("/reports") { _ =>
  Future.successful(Route.component {
    div { text = "Reports loaded" }
  })
}
```

## Navigation

```scala
import jfx.action.Button.button
import jfx.router.Route.{component, route}
import jfx.router.Router.router
import scala.concurrent.Future

val appRouter = router(Seq(
  route("/") { _ => Future.successful(component { div { text = "Home" } }) },
  route("/users") { _ => Future.successful(component { div { text = "Users" } }) }
))

button("Open users") {
  onClick { _ =>
    appRouter.navigate("/users")
  }
}
```

Use `replace = true` when the current browser history entry should be replaced.

## Loading View

Async routes can render a custom loading view directly through the router DSL.

```scala
import jfx.router.Router.{loading, router}

router(routes) {
  loading {
    div {
      classes = Seq("app-router-loading")
      text = "Inhalt wird geladen ..."
    }
  }
}
```

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
  routes = Seq(route("/") { _ => Future.successful(component { div { text = "Home" } }) }),
  initial = "/"
)
```
