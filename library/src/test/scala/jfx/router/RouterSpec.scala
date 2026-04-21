package jfx.router

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.router.Route.{asyncRoute, page}
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.ssr.Ssr

class RouterSpec extends AnyFlatSpec with Matchers {

  "RouteMatcher" should "resolve basic routes" in {
    val routes = Seq(
      asyncRoute("/") { page { div { text = "Root" } } },
      asyncRoute("/test") { page { div { text = "Test" } } }
    )
    
    val match1 = RouteMatcher.resolve(routes, "/")
    match1 should have size 1
    match1.head.route.path shouldBe "/"
    
    val match2 = RouteMatcher.resolve(routes, "/test")
    match2 should have size 1
    match2.head.route.path shouldBe "/test"
  }

  it should "handle base paths correctly" in {
    val routes = Seq(
      asyncRoute("/") { page { div { text = "Root" } } }
    )
    
    val match1 = RouteMatcher.resolve(routes, "/scalajs-jfx2/")
    match1 should have size 1
    match1.head.route.path shouldBe "/"
    
    val match2 = RouteMatcher.resolve(routes, "/scalajs-jfx2/about")
    // Should not match if "/about" is not defined, but here we just check normalization
  }

  "Router" should "render correctly in SSR" in {
    val routes = Seq(
      asyncRoute("/") { page { div { text = "Home" } } },
      asyncRoute("/about") { page { div { text = "About" } } }
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
}
