package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Link.*
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.Input.*
import jfx.form.Editor.editor
import jfx.form.editor.plugins.*
import jfx.form.InputContainer.inputContainer
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.Drawer.*
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.layout.HorizontalLine.horizontalLine
import jfx.router.Route
import jfx.router.RouteContext.routeContext
import jfx.router.Router.router
import jfx.ssr.Ssr
import jfx.statement.Conditional.*
import jfx.statement.ForEach.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object Main {

  def main(args: Array[String]): Unit =
    boot()

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    val maybeRoot = Option(dom.document.getElementById("root")).map(_.asInstanceOf[HTMLElement])

    maybeRoot match {
      case Some(root) if root.children.length > 0 =>
        Hydration.hydrateInto(root) {
          demo()
        }

      case Some(root) =>
        Browser.mount(root) {
          demo()
        }

      case None =>
        dom.console.error("JFX2 demo could not find #root.")
    }
  }

  @JSExportTopLevel("renderSsr")
  def renderSsr(path: String): String =
    Ssr.renderToStringFor(Ssr.Request(path = path, attributes = Map("basePath" -> "/scalajs-jfx2"))) {
      demo()
    }

  private def demo() = {
    val drawerOpen = Property(true)

    val routes = js.Array(
      Route.scoped("/") {
        showcasePage("Overview", "Willkommen zur JFX2 API Dokumentation.") {
          vbox {
            classes = "showcase-intro"
            div {
              classes = "showcase-intro__text"
              text = "JFX2 ist ein reaktives UI-Framework für Scala.js mit Fokus auf SSR-Kompatibilität, Typisierung und architektonische Klarheit."
            }
            horizontalLine()
            div {
              classes = "showcase-intro__links"
              text = "Wähle eine Komponente aus der Sidebar, um Details und Live-Beispiele zu sehen."
            }
          }
        }
      },
      Route.scoped("/button") {
        showcasePage("Button", "Die primäre Aktions-Komponente.") {
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
      Route.scoped("/input") {
        showcasePage("Input", "Texteingabe-Felder für Formulare.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Text Input") {
              val name = Property("")
              vbox {
                input("name") {
                  placeholder = "Name eingeben..."
                  stringValueProperty.observe(name.set)
                }
                val label = div {
                  classes = "showcase-result"
                  text = s"Eingabe: ${name.get}"
                }
                label.addDisposable(name.observe(v => label.textContent = s"Eingabe: $v"))
              }
            }
            apiSection("Usage") {
              codeBlock("scala", "input(\"username\") {\n  placeholder = \"Benutzername\"\n  stringValueProperty.observe(v => println(v))\n}")
            }
          }
        }
      },
      Route.scoped("/editor") {
        showcasePage("Rich Text Editor", "Ein leistungsfähiger WYSIWYG Editor basierend auf Lexical.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Live Editor") {
              val e = editor("demo-editor") {
                defaultPlugins()
              }
              e.value = "Dies ist ein **fetter** Text im Editor."
            }
            apiSection("Usage") {
              codeBlock("scala", "editor(\"body\") {\n  defaultPlugins()\n  value = \"Startinhalt\"\n}")
            }
          }
        }
      },
      Route.scoped("/layout") {
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
      }
    )

    div {
      classes = "app-shell"

      val component = drawer {
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
              navLink("/editor", "Editor", "Rich text")
              
              sidebarSection("Layout")
              navLink("/layout", "Layouts", "HBox, VBox, Div")
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
                classes = "app-toolbar__menu-toggle material-icons"
                text = "menu"
                onClick { _ => drawerOpen.set(!drawerOpen.get) }
              }
              div {
                classes = "app-toolbar__title"
                text = "Live Documentation"
              }
              div { classes = "spacer" }
              div {
                classes = "app-toolbar__version"
                text = "v2.0.0-alpha"
              }
            }

            div {
              classes = "app-content-viewport"
              router(routes)
            }

            div {
              classes = "app-footer"
              div {
                classes = "app-footer__text"
                text = s"┬⌐ ${new js.Date().getFullYear()} Anjunar. Pure Scala.js Architecture."
              }
            }
          }
        }
      }

      component.addDisposable(drawerOpen.observe(component.isOpen = _))
    }
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

  private def navLink(path: String, label: String, sub: String) = {
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
    }
  }

}
