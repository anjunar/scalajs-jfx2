package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Link.*
import jfx.control.{TableColumn, TableView}
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.Input.*
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.Drawer.*
import jfx.layout.Drawer
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.layout.Viewport.*
import jfx.layout.Viewport
import jfx.layout.HorizontalLine.horizontalLine
import jfx.router.Route
import jfx.router.Route.route
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

import app.pages.*

object Main {

  def main(args: Array[String]): Unit =
    boot()

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    val maybeRoot = Option(dom.document.getElementById("root")).map(_.asInstanceOf[HTMLElement])
    val initialPath = s"${dom.window.location.pathname}${dom.window.location.search}"

    maybeRoot match {
      case Some(root) if root.children.length > 0 =>
        Hydration.hydrate(root) {
          demo(initialPath)
        }

      case Some(root) =>
        Browser.mount(root) {
          demo(initialPath)
        }

      case None =>
        dom.console.error("JFX2 demo could not find #root.")
    }
  }

  @JSExportTopLevel("renderSsr")
  def renderSsr(path: String): String =
    Ssr.renderToString {
      demo(path)
    }

  private def demo(initialPath: String = "/") = {
    val routes = Seq(
      route("/") { OverviewPage.render() },
      route("/button") { ButtonPage.render() },
      route("/input") { InputPage.render() },
      route("/layout") { LayoutPage.render() },
      route("/table-view") { TableViewPage.render() },
      route("/window") { WindowPage.render() }
    )

    div {
      classes = "app-shell"

      drawer {
        classes = "app-shell-drawer"
        open = true

        drawerNavigation {
          div {
            classes = "app-sidebar"
            
            div {
              classes = "app-sidebar__header"
              div { classes = "app-sidebar__logo"; text = "JFX2 API" }
            }

            div {
              classes = "app-sidebar__nav"
              sidebarSection("Willkommen")
              navLink("/", "Entdecken", "Die JFX2 Vision")
              
              sidebarSection("Interaktion")
              navLink("/button", "Aktion", "Der Puls der App")
              
              sidebarSection("Gespräch")
              navLink("/input", "Formulare", "Natürlicher Dialog")
              
              sidebarSection("Architektur")
              navLink("/layout", "Struktur", "Raum für Design")
              navLink("/window", "Fenster", "Raum für Fokus")

              sidebarSection("Wissen")
              navLink("/table-view", "Daten", "Atmen und Fließen")
            }
            
            div {
              classes = "app-sidebar__footer"
              text = "Built with JFX2"
            }
          }
        }

        drawerContent {
          div {
            classes = "app-main"
            
            div {
              classes = "app-toolbar"
              button("menu") {
                classes = Seq("app-toolbar__menu-toggle", "material-icons")
                onClick { _ => toggle() }
              }
              div {
                classes = "app-toolbar__title"
                text = "Live Documentation"
              }
              div { classes = "spacer"; style { flex = "1" } }
              div {
                classes = "app-toolbar__version"
                text = "v2.0.0-alpha"
              }
            }

            viewport {
              style { flex = "1"; overflow = "auto" }
              div {
                classes = "app-content-viewport"
                router(routes, initialPath)
              }
            }

            div {
              classes = "app-footer"
              div {
                classes = "app-footer__text"
                text = s"\u00a9 ${new js.Date().getFullYear()} Anjunar. Pure Scala.js Architecture."
              }
            }
          }
        }
      }
    }
  }

  private def sidebarSection(title: String) = {
    div {
      classes = "app-sidebar__section-title"
      text = title
    }
  }

  private def navLink(path: String, label: String, sub: String)(using d: Drawer) = {
    link(path) {
      classes = "app-nav-link"
      div {
        classes = "app-nav-link__label"
        text = label
      }
      div {
        classes = "app-nav-link__sub"
        text = sub
      }
      addDisposable(host.addEventListener("click", _ => {
        open = false // Close drawer after navigation
      }))
    }
  }

}
