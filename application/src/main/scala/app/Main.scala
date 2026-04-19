package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Link.*
import jfx.control.TableColumn.column
import jfx.control.TableView.tableView
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
import jfx.layout.HorizontalLine.horizontalLine
import jfx.router.Route
import jfx.router.Route.route
import jfx.router.Router.router
import jfx.ssr.Ssr
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object Main {

  private final case class ShowcaseBook(title: String, author: String, year: Int)

  private val showcaseBookCatalog: Vector[(String, String, Int)] = Vector(
    ("Der Hobbit", "J. R. R. Tolkien", 1937),
    ("Der Herr der Ringe", "J. R. R. Tolkien", 1954),
    ("1984", "George Orwell", 1949),
    ("Farm der Tiere", "George Orwell", 1945),
    ("Faust. Eine Trag\u00f6die", "Johann Wolfgang von Goethe", 1808)
  )

  private def buildShowcaseBooks(rowCount: Int): Seq[ShowcaseBook] = {
    val cat = showcaseBookCatalog
    val n = cat.length
    (0 until rowCount).map { i =>
      val (title, author, year) = cat(i % n)
      ShowcaseBook(title, author, year)
    }
  }

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
    val drawerOpen = Property(true)

    val routes = Seq(
      route("/") {
        showcasePage("Overview", "Willkommen zur JFX2 API Dokumentation.") {
          vbox {
            classes = "showcase-intro"
            div {
              classes = "showcase-intro__text"
              text = "JFX2 ist ein reaktives UI-Framework f\u00fcr Scala.js mit Fokus auf SSR-Kompatibilität, Typisierung und architektonische Klarheit."
            }
            horizontalLine()
            div {
              classes = "showcase-intro__links"
              text = "W\u00e4hle eine Komponente aus der Sidebar, um Details und Live-Beispiele zu sehen."
            }
          }
        }
      },
      route("/button") {
        showcasePage("Button", "Die prim\u00e4re Aktions-Komponente.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Standard Button") {
              button("Klick mich") {
                onClick { _ => dom.window.alert("Button geklickt!") }
              }
            }
            apiSection("Usage") {
              codeBlock("scala", "button(\"Klick mich\") {\n  onClick { _ => println(\"Geklickt\") }\n}")
            }
          }
        }
      },
      route("/input") {
        showcasePage("Input", "Texteingabe-Felder f\u00fcr Formulare.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Text Input") {
              val name = Property("")
              vbox {
                input("name") {
                  placeholder = "Name eingeben..."
                  addDisposable(stringValueProperty.observe(name.set))
                }
                div {
                  classes = "showcase-result"
                  val labelText = name.map(v => s"Eingabe: $v")
                  addDisposable(labelText.observe(text = _))
                  text = labelText.get
                }
              }
            }
            apiSection("Usage") {
              codeBlock("scala", "input(\"username\") {\n  placeholder = \"Benutzername\"\n  addDisposable(stringValueProperty.observe(v => println(v)))\n}")
            }
          }
        }
      },
      route("/layout") {
        showcasePage("Layout Components", "HBox, VBox und Div zur Strukturierung.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("HBox & VBox") {
              vbox {
                style { gap = "10px" }
                hbox {
                  style { gap = "10px" }
                  div { classes = "demo-box"; text = "H1" }
                  div { classes = "demo-box"; text = "H2" }
                }
                vbox {
                  style { gap = "5px" }
                  div { classes = "demo-box"; text = "V1" }
                  div { classes = "demo-box"; text = "V2" }
                }
              }
            }
          }
        }
      },
      route("/table-view") {
        showcasePage("TableView", "Einfache Tabellen-Struktur.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("B\u00fccher (statische Liste)") {
              val books = new ListProperty[ShowcaseBook]()
              books.setAll(buildShowcaseBooks(5))
              tableView[ShowcaseBook] {
                val table = summon[TableView[ShowcaseBook]]
                table.items.setAll(books.get)
                style { height = "200px" }

                column[ShowcaseBook, String]("Titel") { item =>
                   text = item.title
                }
                column[ShowcaseBook, String]("Autor") { item =>
                   text = item.author
                }
              }
            }
          }
        }
      }
    )

    val root = div {
      classes = "app-shell"

      val component = drawer {
        classes = "app-shell-drawer"
        addDisposable(drawerOpen.observe(open = _))
        open = drawerOpen.get

        drawerNavigation {
          div {
            classes = "app-sidebar"
            
            div {
              classes = "app-sidebar__header"
              div { classes = "app-sidebar__logo"; text = "JFX2 API" }
            }

            div {
              classes = "app-sidebar__nav"
              sidebarSection("General")
              navLink("/", "Overview", "Introduction")
              
              sidebarSection("Actions")
              navLink("/button", "Button", "Trigger actions")
              
              sidebarSection("Forms")
              navLink("/input", "Input", "Text fields")
              
              sidebarSection("Layout")
              navLink("/layout", "Layouts", "HBox, VBox, Div")

              sidebarSection("Data")
              navLink("/table-view", "TableView", "Listen & Spalten")
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
                onClick { _ => drawerOpen.set(!drawerOpen.get) }
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

            div {
              classes = "app-content-viewport"
              style { flex = "1"; overflow = "auto" }
              router(routes, initialPath)
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

      component.addDisposable(drawerOpen.observe(component.openProperty.set))
    }
    root
  }

  private def showcasePage(title: String, subtitle: String)(content: => Unit) = {
    vbox {
      classes = "showcase-page"
      vbox {
        classes = "showcase-page__header"
        div { classes = "showcase-page__title"; text = title }
        div { classes = "showcase-page__subtitle"; text = subtitle }
      }
      div {
        classes = "showcase-page__content"
        content
      }
    }
  }

  private def componentShowcase(title: String)(content: => Unit) = {
    vbox {
      classes = "component-showcase"
      div { classes = "component-showcase__title"; text = title }
      div {
        classes = "component-showcase__render"
        content
      }
    }
  }

  private def apiSection(title: String)(content: => Unit) = {
    vbox {
      classes = "api-section"
      div { classes = "api-section__title"; text = title }
      div {
        classes = "api-section__content"
        content
      }
    }
  }

  private def codeBlock(lang: String, code: String) = {
    div {
      classes = "code-block"
      div {
        classes = "code-block__content"
        text = code
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
