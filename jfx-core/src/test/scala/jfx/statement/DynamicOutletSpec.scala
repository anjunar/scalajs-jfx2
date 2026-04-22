package jfx.statement

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.layout.Div.div
import jfx.ssr.Ssr
import jfx.statement.DynamicOutlet.outlet
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DynamicOutletSpec extends AnyFlatSpec with Matchers {

  "DynamicOutlet" should "mount the initial component from a property" in {
    val page = standalonePage("A")
    val active = Property[Component | Null](page)

    val html = Ssr.renderToString {
      div {
        outlet(active)
      }
    }

    html shouldBe "<div><div>A</div></div>"
  }

  it should "swap mounted components without disposing the previous value" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}

    val pageA = standalonePage("A")
    val pageB = standalonePage("B")
    val active = Property[Component | Null](pageA)
    var mount: DynamicOutlet = null
    val backend = new SsrRenderBackend()

    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        div {
          mount = outlet(active)
        }
      }

      active.set(pageB)
    }

    mount.children should contain only pageB
    pageA.parent shouldBe empty
    pageB.parent should contain (mount)
  }

  it should "support empty mount points" in {
    val active = Property[Component | Null](null)

    val html = Ssr.renderToString {
      div {
        outlet(active)
      }
    }

    html shouldBe "<div></div>"
  }

  private def standalonePage(label: String): Component = {
    var page: Component = null
    Ssr.renderToString {
      page = div { text = label }
      page
    }
    page
  }
}
