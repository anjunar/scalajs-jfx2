package app

import jfx.action.Button.*
import jfx.core.component.Box.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.Dsl.*
import jfx.dsl.DslRuntime
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HBox.hbox
import jfx.layout.Drawer.*
import jfx.layout.Drawer
import jfx.router.Router
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalajs.dom
import scala.scalajs.js.annotation.JSExportTopLevel

object Main {

  def main(args: Array[String]): Unit = {
    boot()
  }

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    val root = dom.document.getElementById("root")
    if (root != null && root.children.length > 0) {
      println("Existing content found, starting hydration...")
      Hydration.hydrate(root) {
        appShell()
      }
    } else if (root != null) {
      val backend = jfx.core.render.BrowserRenderBackend
      val cursor = backend.nextCursor(Some(new jfx.core.render.DomHostElement("div", root)))
      jfx.core.render.RenderBackend.withBackend(backend) {
        DslRuntime.withCursor(cursor) {
          appShell()
        }
      }
    }
  }

  private def appShell() = {
    drawer {
      classes = Seq("app-shell")
      width = "320px"

      drawerNavigation {
        // No extra vbox here, the navigationHost is already the container
        div {
          classes = Seq("app-nav-intro")
          
          div {
            classes = Seq("app-state-chip")
            text = "JFX2 Demo"
          }

          div {
            classes = Seq("app-nav-intro__title")
            text = "Architecture"
          }
          
          div {
            classes = Seq("app-nav-intro__copy")
            text = "Experience SSR and Hydration in action."
          }
        }

        div {
          classes = Seq("app-nav-group")
          style { marginTop = "18px" }
          
          navCard("Home", "Start", "Welcome", "/")
          navCard("Components", "Library", "Controls", "/components")
          navCard("About", "Info", "Architecture", "/about")
        }
      }

      drawerContent {
        vbox {
          classes = Seq("app-frame")
          
          div {
            classes = Seq("app-shell__controls")
            
            button("menu") {
              classes = Seq("material-icons", "app-menu-button")
              onClick { _ =>
                toggle()
              }
            }
          }

          div {
            classes = Seq("app-content")
            style { flex = "1" }
            
            router(Routes.routes)
          }

          hbox {
            classes = Seq("app-footer")
            div {
              classes = Seq("app-footer__copy")
              text = "Built with JFX2 - Consistency & Truth"
            }
          }
        }
      }
    }
  }

  private def navCard(title: String, zone: String, section: String, path: String)(using d: Drawer): Unit = {
    div {
      classes = Seq("app-nav-card")
      
      div {
        classes = Seq("app-nav-card__meta")
        div { classes = Seq("app-nav-card__zone"); text = zone }
        div { classes = Seq("app-nav-card__section"); text = section }
      }
      
      div {
        classes = Seq("app-nav-card__title")
        text = title
      }

      addDisposable(host.addEventListener("click", _ => {
        dom.window.history.pushState(null, "", path)
        dom.window.dispatchEvent(new dom.Event("popstate"))
        open = false // Close drawer after navigation
      }))
    }
  }

  @JSExportTopLevel("renderSsr")
  def renderSsr(path: String): String = {
    Ssr.renderToString {
      appShell()
    }
  }
}
