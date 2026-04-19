package jfx.hydration

import jfx.control.TableView
import jfx.ssr.Ssr
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime
import jfx.core.render.BrowserRenderBackend
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalajs.dom

class HydrationSpec extends AnyFlatSpec with Matchers {

  "Hydration" should "bind components to existing DOM nodes" ignore {
    // 1. Simulate SSR
    val items = new ListProperty[String]()
    items += "A"
    
    val ssrHtml = Ssr.renderToString {
      val tv = new TableView[String]()
      tv.items.setAll(items)
      DslRuntime.attach(tv)
      tv
    }
    
    // 2. Mock Browser DOM
    val container = dom.document.createElement("div")
    container.innerHTML = ssrHtml
    
    // 3. Perform Hydration
    val hydratedTv = Hydration.hydrate(container) {
      val tv = new TableView[String]()
      // Data MUST be the same for successful hydration of dynamic branches
      tv.items.setAll(items) 
      DslRuntime.attach(tv)
      tv
    }.asInstanceOf[TableView[String]]
    
    // 4. Verify
    hydratedTv.host.domNode shouldBe Some(container.firstChild)
    
    // Verify it finds children
    val body = container.querySelector(".jfx-table-body")
    body should not be null
    body.innerHTML should include("A")
  }
}
