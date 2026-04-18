package jfx.layout

import jfx.core.component.ElementComponent.*
import jfx.layout.Drawer.*
import jfx.layout.Div.div
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class DrawerSpec extends AnyFlatSpec with Matchers {

  "Drawer" should "render with its internal structure in SSR" in {
    val html =
      Ssr.renderToString {
        drawer {
          drawerNavigation {
            div {
              text = "Nav"
            }
          }
          drawerContent {
            div {
              text = "Content"
            }
          }
        }
      }

    // Check for root and internal components
    html.should(include("jfx-drawer"))
    html.should(include("jfx-drawer__scrim"))
    html.should(include("jfx-drawer__panel-shell"))
    html.should(include("jfx-drawer__panel"))
    html.should(include("jfx-drawer__navigation"))
    html.should(include("jfx-drawer__content"))
    
    // Check for content
    html.should(include("Nav"))
    html.should(include("Content"))
  }

  it should "apply side classes correctly" in {
    val htmlStart = Ssr.renderToString {
      drawer {
        drawerSide = Drawer.Side.Start
      }
    }
    htmlStart.should(include("jfx-drawer--start"))

    val htmlEnd = Ssr.renderToString {
      drawer {
        drawerSide = Drawer.Side.End
      }
    }
    htmlEnd.should(include("jfx-drawer--end"))
  }

  it should "apply open class correctly" in {
    val htmlClosed = Ssr.renderToString {
      drawer {
        drawerOpen = false
      }
    }
    htmlClosed.shouldNot(include("jfx-drawer--open"))

    val htmlOpen = Ssr.renderToString {
      drawer {
        drawerOpen = true
      }
    }
    htmlOpen.should(include("jfx-drawer--open"))
  }
}
