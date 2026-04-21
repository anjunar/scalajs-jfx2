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
import jfx.router.Route.{asyncRoute, page}
import jfx.router.Router.router
import jfx.router.RouterConfig
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSExportTopLevel

import app.pages.*
import app.domain.DomainRegistry
import jfx.core.state.RemoteListProperty

object Main {

  def main(args: Array[String]): Unit =
    boot()

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    DomainRegistry.init()
    val maybeRoot = Option(dom.document.getElementById("root")).map(_.asInstanceOf[HTMLElement])
    val initialPath = s"${dom.window.location.pathname}${dom.window.location.search}"

    maybeRoot match {
      case Some(root) if root.children.length > 0 =>
        Hydration.hydrate(root) {
          demo(initialPath)
        }.`catch` { error =>
          dom.console.error(s"Hydration failed: $error")
          js.undefined
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
  def renderSsr(path: String): js.Promise[String] = {
    Ssr.renderToStringAsync {
      demo(path)
    }
  }

  private def demo(initialPath: String = "/") = {
    val routes = Seq(
      asyncRoute("/") { page { OverviewPage.render() } },
      asyncRoute("/button") { page { ButtonPage.render() } },
      asyncRoute("/input") { page { InputPage.render() } },
      asyncRoute("/combo-box") { page { ComboBoxPage.render() } },
      asyncRoute("/table-view") {
        val books = TableViewPage.createRemoteBooks(pageSize = 50)

        books.reload(TableViewPage.ShowcaseBookQuery.first(50)).toFuture.map { _ =>
          tableViewPage(books)
        }.toJSPromise
      },
      asyncRoute("/virtual-list") { page { VirtualListViewPage.render() } },
      asyncRoute("/layout") { page { LayoutPage.render() } },
      asyncRoute("/window") { page { WindowPage.render() } },
      asyncRoute("/domain") { page { DomainPage.render() } },
      asyncRoute("/image") { page { ImagePage.render() } },
      asyncRoute("/image-cropper") { page { ImageCropperPage.render() } },
      asyncRoute("/editor") { page { EditorPage.render() } }
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
              navLink("/image", "Bilder", "Visuelle Identität")
              navLink("/image-cropper", "ImageCropper", "Upload & Zuschnitt")
              
              sidebarSection("Gespräch")
              navLink("/input", "Formulare", "Natürlicher Dialog")
              navLink("/combo-box", "ComboBox", "Elegante Auswahl")
              navLink("/editor", "Editor", "Lexical Playground")
              
              sidebarSection("Architektur")
              navLink("/layout", "Struktur", "Raum für Design")
              navLink("/window", "Fenster", "Raum für Fokus")

              sidebarSection("Wissen")
              navLink("/table-view", "Daten", "Atmen und Fließen")
              navLink("/virtual-list", "VirtualList", "Unendliche Weiten")
              navLink("/domain", "Domain", "Mapping & Reflection")
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

  private def tableViewPage(
    books: RemoteListProperty[TableViewPage.ShowcaseBook, TableViewPage.ShowcaseBookQuery]
  ): Route.Factory =
    Route.factory {
      TableViewPage.render(books)
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
