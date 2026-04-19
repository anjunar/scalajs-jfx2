package jfx.ssr

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.router.Route.route
import jfx.router.Router.router

class SsrSpec extends AnyFlatSpec with Matchers {

  "Ssr.renderToString" should "render a simple div with text" in {
    val html = Ssr.renderToString {
      div {
        text = "Hello SSR"
      }
    }
    html shouldBe "<div>Hello SSR</div>"
  }

  it should "render nested structures" in {
    val html = Ssr.renderToString {
      div {
        classes = Seq("outer")
        div {
          classes = Seq("inner")
          text = "Nested"
        }
      }
    }
    html should include ("class=\"outer\"")
    html should include ("class=\"inner\"")
    html should include ("Nested")
  }

  it should "render router content" in {
    val routes = Seq(
      route("/") {
        div { text = "Home" }
      }
    )
    val html = Ssr.renderToString {
      router(routes, "/")
    }
    html should include ("Home")
  }

  it should "handle virtual fragments at the root" in {
    import jfx.statement.Condition.condition
    import jfx.core.state.Property
    
    val html = Ssr.renderToString {
      condition(Property(true)) {
        jfx.statement.Condition.thenDo {
          div { text = "Fragment Root" }
        }
      }
    }
    html shouldBe "<div>Fragment Root</div>"
  }

  it should "render attributes and styles correctly" in {
    val html = Ssr.renderToString {
      div {
        classes = Seq("test-class")
        style {
          height = "100px"
          flex = "1"
        }
        text = "Content"
      }
    }
    html should include ("class=\"test-class\"")
    html should include ("style=\"")
    html should include ("height: 100px")
    html should include ("flex: 1")
    html should include ("Content")
  }
}
