package jfx.router

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.router.Route.{component, localized, route}
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.ssr.Ssr
import scala.concurrent.Future

class RouterSpec extends AnyFlatSpec with Matchers {

  "RouteMatcher" should "resolve basic routes" in {
    val routes = Seq(
      route("/") { _ => Future.successful(component { div { text = "Root" } }) },
      route("/test") { _ => Future.successful(component { div { text = "Test" } }) }
    )
    
    val match1 = RouteMatcher.resolve(routes, "/")
    match1 should have size 1
    match1.head.route.path shouldBe "/"
    
    val match2 = RouteMatcher.resolve(routes, "/test")
    match2 should have size 1
    match2.head.route.path shouldBe "/test"
  }

  it should "resolve wildcard routes" in {
    val routes = Seq(
      route("/") { _ => Future.successful(component { div { text = "Root" } }) },
      route("/*") { _ => Future.successful(component { div { text = "Shell" } }) }
    )

    val rootMatch = RouteMatcher.resolve(routes, "/")
    rootMatch should have size 1
    rootMatch.head.route.path shouldBe "/"

    val nestedMatch = RouteMatcher.resolve(routes, "/blog/posts/post")
    nestedMatch should have size 1
    nestedMatch.head.route.path shouldBe "/*"
    nestedMatch.head.params("*") shouldBe "blog/posts/post"
  }

  it should "resolve child routes" in {
    val routes = Seq(
      route("/") { _ =>
        Future.successful(component { div { text = "Root" } })
      }.copy(
        children = Seq(
          route("/docs/:slug") { _ => Future.successful(component { div { text = "Docs" } }) }
        )
      )
    )

    val matches = RouteMatcher.resolve(routes, "/docs/router")
    matches should have size 2
    matches.head.route.path shouldBe "/"
    matches.last.route.path shouldBe "/docs/:slug"
    matches.last.params("slug") shouldBe "router"
  }

  it should "handle base paths correctly" in {
    val routes = Seq(
      route("/") { _ => Future.successful(component { div { text = "Root" } }) }
    )
    
    val match1 = RouteMatcher.resolve(routes, "/scalajs-jfx2/")
    match1 should have size 1
    match1.head.route.path shouldBe "/"
    
    val match2 = RouteMatcher.resolve(routes, "/scalajs-jfx2/about")
    // Should not match if "/about" is not defined, but here we just check normalization
  }

  it should "resolve localized routes with typed languages" in {
    val routes = Seq(
      localized("/blog") { (language, ctx) =>
        Future.successful(component {
          div {
            text = s"${language.code}:${ctx.language.map(_.code).getOrElse("missing")}"
          }
        })
      }
    )

    val germanHtml = Ssr.renderToString {
      Router.router(routes, "/de/blog")
    }
    germanHtml should include ("de:de")

    val englishHtml = Ssr.renderToString {
      Router.router(routes, "/en/blog")
    }
    englishHtml should include ("en:en")

    val frenchHtml = Ssr.renderToString {
      Router.router(routes, "/fr/blog")
    }
    frenchHtml should include ("fr:fr")
  }

  it should "reject unsupported languages for localized routes" in {
    val routes = Seq(
      localized("/blog") { (language, _) =>
        Future.successful(component {
          div {
            text = language.code
          }
        })
      }
    )

    RouteMatcher.resolve(routes, "/xx/blog") shouldBe Nil

    val html = Ssr.renderToString {
      Router.router(routes, "/xx/blog")
    }
    html should include ("No route matched for: /xx/blog")
  }

  it should "build localized paths consistently" in {
    LocalizedRoute.path(Language.German, "/blog") shouldBe "/de/blog"
    LocalizedRoute.path(Language.German, "blog") shouldBe "/de/blog"
    LocalizedRoute.path(Language.German, "/") shouldBe "/de"
  }

  it should "keep existing non-localized routes working" in {
    val routes = Seq(
      route("/blog") { _ =>
        Future.successful(component {
          div {
            text = "plain-blog"
          }
        })
      },
      localized("/blog") { (language, _) =>
        Future.successful(component {
          div {
            text = s"localized-${language.code}"
          }
        })
      }
    )

    val plainHtml = Ssr.renderToString {
      Router.router(routes, "/blog")
    }
    plainHtml should include ("plain-blog")

    val localizedHtml = Ssr.renderToString {
      Router.router(routes, "/de/blog")
    }
    localizedHtml should include ("localized-de")
  }

  "Router" should "render correctly in SSR" in {
    val routes = Seq(
      route("/") { _ => Future.successful(component { div { text = "Home" } }) },
      route("/about") { _ => Future.successful(component { div { text = "About" } }) }
    )
    
    val htmlHome = Ssr.renderToString {
      Router.router(routes, "/")
    }
    htmlHome should include ("Home")
    
    val htmlAbout = Ssr.renderToString {
      Router.router(routes, "/about")
    }
    htmlAbout should include ("About")
  }

  it should "allow configuring the loading view via DSL" in {
    val html = Ssr.renderToString {
      val appRouter = Router.router(Seq(route("/") { _ => Future.successful(component { div { text = "Home" } }) }), "/") {
        Router.loading {
          div {
            classes = Seq("custom-router-loading")
            text = "Bitte warten"
          }
        }
      }
      div {
        given Router = appRouter
        appRouter.renderLoadingView()
      }
    }

    html should include ("Bitte warten")
    html should include ("custom-router-loading")
  }
}
