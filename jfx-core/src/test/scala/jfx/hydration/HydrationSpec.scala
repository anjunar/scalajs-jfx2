package jfx.hydration

import jfx.core.component.Component.*
import jfx.core.render.{BrowserRenderBackend, HydrationCursor, HydrationRenderBackend, RenderBackend}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.layout.Span.span
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HydrationSpec extends AnyFlatSpec with Matchers {

  "HydrationCursor" should "prune unclaimed DOM nodes" ignore {
    val container = dom.document.createElement("div")
    container.innerHTML = "<span>A</span><span>B</span>"

    val cursor = new HydrationCursor(container)
    cursor.claimElement("span")
    cursor.pruneRemaining()

    container.innerHTML shouldBe "<span>A</span>"
  }

  it should "apply the expected client text on mismatch" ignore {
    val container = dom.document.createElement("div")
    container.innerHTML = "server"

    val cursor = new HydrationCursor(container)
    val textNode = cursor.claimText("client")

    container.textContent shouldBe "client"
    textNode.renderHtml(0) shouldBe "client"
  }

  "DslRuntime.rehydrate" should "prune server children outside the client tree" ignore {
    val container = dom.document.createElement("div")
    container.innerHTML = "<div><span>A</span><span>B</span></div>"

    val root = RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(BrowserRenderBackend.nextCursor(None)) {
        div {
          span {
            text = "A"
          }
        }
      }
    }

    val backend = HydrationRenderBackend.root(container)
    RenderBackend.withBackend(backend) {
      DslRuntime.rehydrate(root, backend.nextCursor(None))
    }

    root.host.domNode shouldBe Some(container.firstChild)
    container.innerHTML shouldBe "<div><span>A</span></div>"
  }
}
