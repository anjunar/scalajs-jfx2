package jfx.layout

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.Drawer.*
import jfx.ssr.Ssr

class DrawerSpec extends AnyFlatSpec with Matchers {

  "Drawer" should "synchronize open state with classes" in {
    var dr: Drawer = null
    Ssr.renderToString {
      dr = drawer {
        open = true
        drawerContent { div { text = "Content" } }
      }
      dr
    }

    dr.classes should contain ("jfx-drawer--open")
    
    dr.openProperty.set(false)
    dr.classes should not contain ("jfx-drawer--open")
  }

  it should "render content and navigation in their respective hosts" in {
    val html = Ssr.renderToString {
      drawer {
        drawerNavigation { div { classes = Seq("nav-item"); text = "Nav" } }
        drawerContent { div { classes = Seq("content-item"); text = "Body" } }
      }
    }

    html should include ("class=\"nav-item\"")
    html should include ("class=\"content-item\"")
    html should include ("Nav")
    html should include ("Body")
  }

  it should "toggle state via helper" in {
    var dr: Drawer = null
    Ssr.renderToString {
      dr = drawer { open = false }
      dr
    }

    toggle()(using dr)
    dr.openProperty.get shouldBe true
    
    toggle()(using dr)
    dr.openProperty.get shouldBe false
  }
}
