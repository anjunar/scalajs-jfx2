package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Image.*
import jfx.control.Link.*
import jfx.control.{TableColumn, TableView}
import jfx.core.component.Box.box
import jfx.core.component.Component
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
import jfx.router.Route.{localized, route}
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.timers.setTimeout

import app.pages.*
import app.domain.DomainRegistry
import app.domain.BlogDraft
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
    DemoI18n.syncFromLanguage(DemoRoutes.languageFromUrl(initialPath).getOrElse(jfx.router.Language.default))
    dom.window.addEventListener("popstate", _ =>
      DemoI18n.syncFromLanguage(DemoRoutes.languageFromUrl(s"${dom.window.location.pathname}${dom.window.location.search}").getOrElse(jfx.router.Language.default))
    )

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
    DemoI18n.syncFromLanguage(DemoRoutes.languageFromUrl(path).getOrElse(jfx.router.Language.default))
    Ssr.renderToStringAsync {
      demo(path)
    }
  }

  private def demo(initialPath: String = "/") = {
    val routes = Seq(
      route(DemoRoutes.homePath) { _ => routePage { OverviewPage.render() } },
      localized(DemoRoutes.homePath) { (_, _) => routePage { OverviewPage.render() } },
      route(DemoRoutes.buttonPath) { _ => routePage { ButtonPage.render() } },
      localized(DemoRoutes.buttonPath) { (_, _) => routePage { ButtonPage.render() } },
      route(DemoRoutes.inputPath) { _ => routePage { InputPage.render() } },
      localized(DemoRoutes.inputPath) { (_, _) => routePage { InputPage.render() } },
      route(DemoRoutes.comboBoxPath) { _ => routePage { ComboBoxPage.render() } },
      localized(DemoRoutes.comboBoxPath) { (_, _) => routePage { ComboBoxPage.render() } },
      route(DemoRoutes.carouselPath) { _ => routePage { CarouselPage.render() } },
      localized(DemoRoutes.carouselPath) { (_, _) => routePage { CarouselPage.render() } },
      route(DemoRoutes.tableViewPath, stateful = true) { _ =>
        val books = TableViewPage.createRemoteBooks(pageSize = 50)

        books.reload(TableViewPage.ShowcaseBookQuery.first(50)).toFuture.map { _ =>
          tableViewPage(books)
        }
      },
      localized(DemoRoutes.tableViewPath, stateful = true) { (_, _) =>
        val books = TableViewPage.createRemoteBooks(pageSize = 50)

        books.reload(TableViewPage.ShowcaseBookQuery.first(50)).toFuture.map { _ =>
          tableViewPage(books)
        }
      },
      route(DemoRoutes.dataGridPath, stateful = true) { _ =>
        val tiles = DataGridPage.createRemoteTiles(pageSize = 24)

        tiles.reload(DataGridPage.ShowcaseTileQuery.first(24)).toFuture.map { _ =>
          dataGridPage(tiles)
        }
      },
      localized(DemoRoutes.dataGridPath, stateful = true) { (_, _) =>
        val tiles = DataGridPage.createRemoteTiles(pageSize = 24)

        tiles.reload(DataGridPage.ShowcaseTileQuery.first(24)).toFuture.map { _ =>
          dataGridPage(tiles)
        }
      },
      route(DemoRoutes.virtualListPath) { _ => routePage { VirtualListViewPage.render() } },
      localized(DemoRoutes.virtualListPath) { (_, _) => routePage { VirtualListViewPage.render() } },
      route(DemoRoutes.layoutPath) { _ => routePage { LayoutPage.render() } },
      localized(DemoRoutes.layoutPath) { (_, _) => routePage { LayoutPage.render() } },
      route(DemoRoutes.windowPath, stateful = true) { _ => routePage { WindowPage.render() } },
      localized(DemoRoutes.windowPath, stateful = true) { (_, _) => routePage { WindowPage.render() } },
      route(DemoRoutes.domainPath) { _ => routePage { DomainPage.render() } },
      localized(DemoRoutes.domainPath) { (_, _) => routePage { DomainPage.render() } },
      route(DemoRoutes.imagePath) { _ => routePage { ImagePage.render() } },
      localized(DemoRoutes.imagePath) { (_, _) => routePage { ImagePage.render() } },
      route(DemoRoutes.imageCropperPath) { _ => routePage { ImageCropperPage.render() } },
      localized(DemoRoutes.imageCropperPath) { (_, _) => routePage { ImageCropperPage.render() } },
      route(DemoRoutes.editorPath, stateful = true) { _ =>
        val draft = createEditorDraft()

        sleep(350) {
          routeComponent {
            EditorPage.render(draft)
          }
        }.toFuture
      },
      localized(DemoRoutes.editorPath, stateful = true) { (_, _) =>
        val draft = createEditorDraft()

        sleep(350) {
          routeComponent {
            EditorPage.render(draft)
          }
        }.toFuture
      },
      route(DemoRoutes.hydrationReproPath) { _ => routePage { HydrationReproPage.render() } },
      localized(DemoRoutes.hydrationReproPath) { (_, _) => routePage { HydrationReproPage.render() } },
      route(DemoRoutes.memoryLeakTestPath) { _ => routePage { MemoryLeakTestPage.render() } },
      localized(DemoRoutes.memoryLeakTestPath) { (_, _) => routePage { MemoryLeakTestPage.render() } }
    )

    div {
      classes = Seq("app-shell")

      drawer {
        classes = Seq("app-shell-drawer")
        open = true

        drawerNavigation {
          div {
            classes = Seq("app-sidebar")
            
            div {
              classes = Seq("app-sidebar__header")
              div { classes = Seq("app-sidebar__logo"); text = DemoI18n.text(i18n"JFX2 API") }
            }

            div {
              classes = Seq("app-sidebar__nav")
              sidebarSection(i18n"Welcome")
              navLink(DemoRoutes.homePath, i18n"Discover", i18n"The JFX2 vision")
              
              sidebarSection(i18n"Interaction")
              navLink(DemoRoutes.buttonPath, i18n"Actions", i18n"The pulse of the app")
              navLink(DemoRoutes.imagePath, i18n"Images", i18n"Visual identity")
              navLink(DemoRoutes.imageCropperPath, i18n"ImageCropper", i18n"Upload & crop")
              navLink(DemoRoutes.carouselPath, i18n"Carousel", i18n"Looping slides with SSR-visible states")
              
              sidebarSection(i18n"Conversation")
              navLink(DemoRoutes.inputPath, i18n"Forms", i18n"Natural dialogue")
              navLink(DemoRoutes.comboBoxPath, i18n"ComboBox", i18n"Elegant selection")
              navLink(DemoRoutes.editorPath, i18n"Editor", i18n"Lexical playground")
              navLink(DemoRoutes.hydrationReproPath, i18n"Hydration", i18n"Direct-load editor repro")
              navLink(DemoRoutes.memoryLeakTestPath, i18n"Memory Test", i18n"Editor lifecycle stress")
              
              sidebarSection(i18n"Architecture")
              navLink(DemoRoutes.layoutPath, i18n"Layout", i18n"Room for design")
              navLink(DemoRoutes.windowPath, i18n"Windows", i18n"Room for focus")

              sidebarSection(i18n"Knowledge")
              navLink(DemoRoutes.tableViewPath, i18n"Data", i18n"Breathing and flowing")
              navLink(DemoRoutes.dataGridPath, i18n"DataGrid", i18n"Virtual cards at scale")
              navLink(DemoRoutes.virtualListPath, i18n"VirtualList", i18n"Endless expanses")
              navLink(DemoRoutes.domainPath, i18n"Domain", i18n"Mapping & reflection")
            }
            
            div {
              classes = Seq("app-sidebar__footer")
              text = DemoI18n.text(i18n"Built with JFX2")
            }
          }
        }

        drawerContent {
          div {
            classes = Seq("app-main")
            
            div {
              classes = Seq("app-toolbar")
              button("menu") {
                classes = Seq("app-toolbar__menu-toggle", "material-icons")
                onClick { _ => toggle() }
              }
              div {
                classes = Seq("app-toolbar__title")
                text = DemoI18n.text(i18n"Live Documentation")
              }
              div { classes = Seq("spacer"); style { flex = "1" } }
              box("a") {
                classes = Seq("app-toolbar__scala-link")
                attribute("href", "https://www.scala-js.org/")
                attribute("target", "_blank")
                attribute("rel", "noopener noreferrer")
                image {
                  classes = Seq("app-toolbar__scala-badge")
                  src = "https://www.scala-js.org/assets/badges/scalajs-1.22.0.svg?logo=scala&logoColor=white"
                  alt = "Scala.js 1.22.0"
                }
              }
              box("a") {
                classes = Seq("app-toolbar__github")
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
                  onClick { _ => DemoRoutes.switchLanguage() }
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
                classes = Seq("app-toolbar__version")
                text = DemoI18n.text(i18n"v2.3.0")
              }
            }

            viewport {
              style { flex = "1"; overflow = "auto" }
              div {
                classes = Seq("app-content-viewport")
                router(routes, initialPath)
              }
            }

            div {
              classes = Seq("app-footer")
              div {
                classes = Seq("app-footer__text")
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
  ): Component =
    routeComponent {
      TableViewPage.render(books)
    }

  private def dataGridPage(
    tiles: RemoteListProperty[DataGridPage.ShowcaseTile, DataGridPage.ShowcaseTileQuery]
  ): Component =
    routeComponent {
      DataGridPage.render(tiles)
    }

  private def routePage(render: => Unit): Future[Component] =
    Future.successful(routeComponent(render))

  private def routeComponent(render: => Unit): Component =
    new RouteContentPage(() => render)

  private def createEditorDraft(): BlogDraft = {
    val draft = new BlogDraft()
    draft.title.set("Editor draft loaded by the router")
    draft
  }

  private def sleep[T](millis: Int)(value: => T): js.Promise[T] =
    new js.Promise[T]((resolve, reject) =>
      setTimeout(millis) {
        try resolve(value)
        catch {
          case error: Throwable => reject(error)
        }
      }
    )

  private def sidebarSection(title: RuntimeMessage) = {
    div {
      classes = Seq("app-sidebar__section-title")
      text = DemoI18n.text(title)
    }
  }

  private def navLink(path: String, label: RuntimeMessage, sub: RuntimeMessage)(using d: Drawer) = {
    link(path) {
      attribute(
        "href",
        DemoI18n.localeProperty.map {
          case DemoI18n.German => DemoRoutes.localizedPath(path, jfx.router.Language.German)
          case _ => DemoRoutes.localizedPath(path, jfx.router.Language.English)
        }
      )
      classes = Seq("app-nav-link")
      div {
        classes = Seq("app-nav-link__label")
        text = DemoI18n.text(label)
      }
      div {
        classes = Seq("app-nav-link__sub")
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

private final class RouteContentPage(render: () => Unit) extends Component {
  override def tagName: String = ""

  override def compose(): Unit =
    render()
}
