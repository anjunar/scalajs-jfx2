package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Image.*
import jfx.control.Link.*
import jfx.control.{TableColumn, TableView}
import jfx.core.component.Box.box
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.Input.*
import jfx.hydration.Hydration
import jfx.i18n.*
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
              div { classes = "app-sidebar__logo"; text = DemoI18n.text(i18n"JFX2 API") }
            }

            div {
              classes = "app-sidebar__nav"
              sidebarSection(i18n"Welcome")
              navLink("/", i18n"Discover", i18n"The JFX2 vision")
              
              sidebarSection(i18n"Interaction")
              navLink("/button", i18n"Actions", i18n"The pulse of the app")
              navLink("/image", i18n"Images", i18n"Visual identity")
              navLink("/image-cropper", i18n"ImageCropper", i18n"Upload & crop")
              
              sidebarSection(i18n"Conversation")
              navLink("/input", i18n"Forms", i18n"Natural dialogue")
              navLink("/combo-box", i18n"ComboBox", i18n"Elegant selection")
              navLink("/editor", i18n"Editor", i18n"Lexical playground")
              
              sidebarSection(i18n"Architecture")
              navLink("/layout", i18n"Layout", i18n"Room for design")
              navLink("/window", i18n"Windows", i18n"Room for focus")

              sidebarSection(i18n"Knowledge")
              navLink("/table-view", i18n"Data", i18n"Breathing and flowing")
              navLink("/virtual-list", i18n"VirtualList", i18n"Endless expanses")
              navLink("/domain", i18n"Domain", i18n"Mapping & reflection")
            }
            
            div {
              classes = "app-sidebar__footer"
              text = DemoI18n.text(i18n"Built with JFX2")
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
                text = DemoI18n.text(i18n"Live Documentation")
              }
              div { classes = "spacer"; style { flex = "1" } }
              box("a") {
                classes = "app-toolbar__scala-link"
                attribute("href", "https://www.scala-js.org/")
                attribute("target", "_blank")
                attribute("rel", "noopener noreferrer")
                image {
                  classes = "app-toolbar__scala-badge"
                  src = "https://img.shields.io/badge/Scala.js-1.21.0-DC322F.svg?logo=scala&logoColor=white"
                  alt = "Scala.js 1.21.0"
                }
              }
              box("a") {
                classes = "app-toolbar__github"
                attribute("href", "https://github.com/anjunar/scalajs-jfx2")
                attribute("target", "_blank")
                attribute("rel", "noopener noreferrer")
                text = DemoI18n.text(i18n"GitHub")
              }
              hbox {
                classes = "app-toolbar__chooser app-toolbar__language"
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = DemoI18n.localeLabel
                  onClick { _ => DemoI18n.toggle() }
                }
              }
              hbox {
                classes = "app-toolbar__chooser app-toolbar__theme"
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = DemoI18n.text(i18n"Light")
                  classIf("is-active", Theme.modeProperty.map(_ == Mode.Light))
                  onClick { _ => Theme.set(Mode.Light) }
                }
                button() {
                  classes = Seq("app-toolbar__choice")
                  text = DemoI18n.text(i18n"Dark")
                  classIf("is-active", Theme.modeProperty.map(_ == Mode.Dark))
                  onClick { _ => Theme.set(Mode.Dark) }
                }
              }
              div {
                classes = "app-toolbar__version"
                text = DemoI18n.text(i18n"v2.0.1")
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
                val year = new js.Date().getFullYear().toInt
                text = DemoI18n.text(i18n"© $year Anjunar. Pure Scala.js Architecture.")
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

  private def sidebarSection(title: RuntimeMessage) = {
    div {
      classes = "app-sidebar__section-title"
      text = DemoI18n.text(title)
    }
  }

  private def navLink(path: String, label: RuntimeMessage, sub: RuntimeMessage)(using d: Drawer) = {
    link(path) {
      classes = "app-nav-link"
      div {
        classes = "app-nav-link__label"
        text = DemoI18n.text(label)
      }
      div {
        classes = "app-nav-link__sub"
        text = DemoI18n.text(sub)
      }
      addDisposable(host.addEventListener("click", _ => {
        if (dom.window.innerWidth <= 720) {
          open = false // Close drawer after navigation only on mobile
        }
      }))
    }
  }

}
