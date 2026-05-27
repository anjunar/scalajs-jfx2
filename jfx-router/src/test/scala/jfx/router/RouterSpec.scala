package jfx.router

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.router.Route.{component, route}
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
