package jfx.control

import jfx.control.Tabs.{RenderMode, selectedIndex_=, tab, tabs}
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TabsSpec extends AnyFlatSpec with Matchers {

  "Tabs" should "render triggers and the active tab content in SSR" in {
    val html = Ssr.renderToString {
      tabs {
        tab("Location") {
          div { text = "Current location panel" }
        }
        tab("Chat") {
          div { text = "Chat panel" }
        }
      }
    }

    html should include("jfx-tabs")
    html should include("Location")
    html should include("Chat")
    html should include("Current location panel")
    html should not include "Chat panel"
  }

  it should "optionally keep inactive tab content mounted and hidden in SSR" in {
    val html = Ssr.renderToString {
      tabs(renderMode = RenderMode.KeepMountedHidden) {
        tab("Location") {
          div { text = "Current location panel" }
        }
        tab("Chat") {
          div { text = "Chat panel" }
        }
      }
    }

    html should include("Current location panel")
    html should include("Chat panel")
    html should include("""display: none""")
  }

  it should "clamp the selected index when moving beyond available tabs" in {
    val control = new Tabs()
    control.addTab(new Tabs.TabSpec("One", _ => ()))
    control.addTab(new Tabs.TabSpec("Two", _ => ()))

    selectedIndex_=(99)(using control)
    control.$selectedIndexProperty.get shouldBe 1

    selectedIndex_=(-5)(using control)
    control.$selectedIndexProperty.get shouldBe 0
  }
}
