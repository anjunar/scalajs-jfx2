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
import app.I18n.Key
import app.Theme.Mode
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
        }.`then` { _ =>
          Theme.syncFromDocument()
          js.undefined
        }.`catch` { error =>
          dom.console.error(s"Hydration failed: $error")
          js.undefined
        }

      case Some(root) =>
        Theme.syncFromDocument()
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
              div { classes = "app-sidebar__logo"; text = I18n.text(Key.SidebarLogo) }
            }

            div {
              classes = "app-sidebar__nav"
              sidebarSection(Key.SectionWelcome)
              navLink("/", Key.NavOverview, Key.NavOverviewSub)
              
              sidebarSection(Key.SectionInteraction)
              navLink("/button", Key.NavButton, Key.NavButtonSub)
              navLink("/image", Key.NavImage, Key.NavImageSub)
              navLink("/image-cropper", Key.NavImageCropper, Key.NavImageCropperSub)
              
              sidebarSection(Key.SectionConversation)
              navLink("/input", Key.NavInput, Key.NavInputSub)
              navLink("/combo-box", Key.NavComboBox, Key.NavComboBoxSub)
              navLink("/editor", Key.NavEditor, Key.NavEditorSub)
              
              sidebarSection(Key.SectionArchitecture)
              navLink("/layout", Key.NavLayout, Key.NavLayoutSub)
              navLink("/window", Key.NavWindow, Key.NavWindowSub)

              sidebarSection(Key.SectionKnowledge)
              navLink("/table-view", Key.NavTableView, Key.NavTableViewSub)
              navLink("/virtual-list", Key.NavVirtualList, Key.NavVirtualListSub)
              navLink("/domain", Key.NavDomain, Key.NavDomainSub)
            }
            
            div {
              classes = "app-sidebar__footer"
              text = I18n.text(Key.Footer)
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
                text = I18n.text(Key.AppTitle)
              }
              div { classes = "spacer"; style { flex = "1" } }
              hbox {
                classes = "app-toolbar__chooser app-toolbar__language"
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = I18n.text(Key.Language)
                  onClick { _ => I18n.toggle() }
                }
              }
              hbox {
                classes = "app-toolbar__chooser app-toolbar__theme"
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = I18n.text(Key.ThemeLight)
                  classIf("is-active", Theme.modeProperty.map(_ == Mode.Light))
                  onClick { _ => Theme.set(Mode.Light) }
                }
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = I18n.text(Key.ThemeDark)
                  classIf("is-active", Theme.modeProperty.map(_ == Mode.Dark))
                  onClick { _ => Theme.set(Mode.Dark) }
                }
              }
              div {
                classes = "app-toolbar__version"
                text = I18n.text(Key.Version)
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
                text = I18n.text(Key.Copyright).map(copy => s"\u00a9 ${new js.Date().getFullYear()} Anjunar. $copy")
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

  private def sidebarSection(title: Key) = {
    div {
      classes = "app-sidebar__section-title"
      text = I18n.text(title)
    }
  }

  private def navLink(path: String, label: Key, sub: Key)(using d: Drawer) = {
    link(path) {
      classes = "app-nav-link"
      div {
        classes = "app-nav-link__label"
        text = I18n.text(label)
      }
      div {
        classes = "app-nav-link__sub"
        text = I18n.text(sub)
      }
      addDisposable(host.addEventListener("click", _ => {
        open = false // Close drawer after navigation
      }))
    }
  }

}
