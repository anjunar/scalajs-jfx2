package jfx.statement

import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.layout.Div.div
import jfx.ssr.Ssr
import jfx.statement.ObserveRender.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ObserveRenderSpec extends AnyFlatSpec with Matchers {

  "ObserveRender" should "render the initial property value" in {
    val selected = Property("A")

    val html = Ssr.renderToString {
      div {
        observeRender(selected) { value =>
          div { text = value }
        }
      }
    }

    html shouldBe "<div><div>A</div></div>"
  }

  it should "rebuild its branch when the property changes" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}

    val selected = Property("A")
    var statement: ObserveRender[String] = null
    val backend = new SsrRenderBackend()

    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        div {
          statement = observeRender(selected) { value =>
            div {
              classes = Seq(s"state-$value")
              text = value
            }
          }
        }
      }

      selected.set("B")
    }

    statement.children should have size 1
    statement.children.head.asInstanceOf[jfx.core.component.Box].classes should contain ("state-B")
  }

  it should "expose the current observeRender value inside the render block" in {
    val selected = Property("Current")

    val html = Ssr.renderToString {
      div {
        observeRender(selected) { _ =>
          div { text = observeRenderValue }
        }
      }
    }

    html shouldBe "<div><div>Current</div></div>"
  }
}
