package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.core.component.ClientOnly.*
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.form.Input.*
import jfx.hydration.Hydration
import jfx.layout.Div.div
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
    Ssr.renderToStringFor(Ssr.Request(path = path)) {
      demo()
    }

  private def demo() = {
    val name = Property("Ada")
    val clicks = Property(0)
    val milestones = ListProperty[String]()
    milestones += "SSR rendert ohne Browser-DOM"
    milestones += "Hydration claimed bestehende Nodes"
    milestones += "Statements arbeiten ueber NativeComponent-Slots"
    milestones += "Router ersetzt Pages ueber denselben Slot-Mechanismus"

    val routes = js.Array(
      Route.scoped("/") {
        val context = routeContext

        div {
          classes = "jfx2-demo__route-page"

          div {
            classes = "jfx2-demo__route-kicker"
            text = "Route /"
          }

          div {
            classes = "jfx2-demo__route-title"
            text = "Home aus dem NativeComponent-Router"
          }

          div {
            classes = "jfx2-demo__route-copy"
            text = s"Diese Page wurde fuer ${context.url} direkt in einen ChildSlot gerendert."
          }
        }
      },
      Route.scoped("/hello/:name") {
        val context = routeContext
        val routeName = context.pathParams("name")
        val tab = context.queryParams.get("tab").getOrElse("overview")

        div {
          classes = "jfx2-demo__route-page is-accent"

          div {
            classes = "jfx2-demo__route-kicker"
            text = s"Route ${context.fullPath}"
          }

          div {
            classes = "jfx2-demo__route-title"
            text = s"Hallo $routeName"
          }

          div {
            classes = "jfx2-demo__route-copy"
            text = s"Path-Param und Query sind im RouteContext angekommen. tab=$tab"
          }
        }
      },
      Route.scoped("/editor") {
        div {
          classes = "jfx2-demo__route-page is-editor"

          div {
            classes = "jfx2-demo__route-kicker"
            text = "Route /editor"
          }

          clientOnly("RouteEditor")(
            div {
              classes = "jfx2-demo__client-fallback"
              text = "SSR-Fallback: Route kann browser-only Komponenten enthalten."
            }
          ) {
            div {
              classes = "jfx2-demo__client-widget"

              div {
                classes = "jfx2-demo__client-widget-title"
                text = "Editor-Route hydriert"
              }

              div {
                classes = "jfx2-demo__client-widget-copy"
                text = "Auch innerhalb einer Router-Page bleibt ClientOnly sauber vom SSR getrennt."
              }
            }
          }
        }
      },
      Route.scoped("*") {
        val context = routeContext

        div {
          classes = "jfx2-demo__route-page is-missing"

          div {
            classes = "jfx2-demo__route-kicker"
            text = "Wildcard"
          }

          div {
            classes = "jfx2-demo__route-title"
            text = "Route nicht gefunden"
          }

          div {
            classes = "jfx2-demo__route-copy"
            text = s"Kein Problem: ${context.path} landet im *-Fallback."
          }
        }
      }
    )

    div {
      classes = "jfx2-demo"

      div {
        classes = "jfx2-demo__ambient"
      }

      div {
        classes = "jfx2-demo__panel"

        div {
          classes = "jfx2-demo__eyebrow"
          text = "request-time SSR + hydration"
        }

        div {
          classes = "jfx2-demo__title"
          text = "scalajs-jfx2"
        }

        div {
          classes = "jfx2-demo__copy"
          text = "Eine kleine Core-Demo: dieselbe DSL rendert HTML auf dem Server, hydriert vorhandenes Markup und laeuft danach interaktiv im Browser."
        }

        div {
          classes = "jfx2-demo__card"

          div {
            classes = "jfx2-demo__label"
            text = "Name"
          }

          val nameField =
            input("name") {
              classes = "jfx2-demo__input"
              placeholder = "Dein Name"
              stringValueProperty.set(name.get)
            }

          nameField.addDisposable(
            Property.subscribeBidirectional(nameField.stringValueProperty, name)
          )

          val greeting =
            div {
              classes = "jfx2-demo__result"
              text = greetingText(name.get, clicks.get)
            }

          greeting.addDisposable(
            name.observe { value =>
              greeting.textContent = greetingText(value, clicks.get)
            }
          )

          greeting.addDisposable(
            clicks.observe { value =>
              greeting.textContent = greetingText(name.get, value)
            }
          )

          button("Hydration testen") {
            classes = "jfx2-demo__button"

            onClick { _ =>
              clicks.set(clicks.get + 1)
            }
          }

          conditional(clicks.map(_ % 2 == 0)) {
            thenDo {
              div {
                classes = "jfx2-demo__slot-status"
                text = "Conditional-Slot: gerade Anzahl Klicks"
              }
            }

            elseDo {
              div {
                classes = "jfx2-demo__slot-status is-odd"
                text = "Conditional-Slot: ungerade Anzahl Klicks"
              }
            }
          }

          div {
            classes = "jfx2-demo__router"

            div {
              classes = "jfx2-demo__label"
              text = "Router"
            }

            val appRouter = router(routes)

            div {
              classes = "jfx2-demo__route-actions"

              button("Home") {
                classes = "jfx2-demo__route-button"
                onClick { _ =>
                  appRouter.navigate("/")
                }
              }

              button("Hello Ada") {
                classes = "jfx2-demo__route-button"
                onClick { _ =>
                  appRouter.navigate("/hello/ada?tab=notes")
                }
              }

              button("Editor Route") {
                classes = "jfx2-demo__route-button"
                onClick { _ =>
                  appRouter.navigate("/editor")
                }
              }
            }
          }

          div {
            classes = "jfx2-demo__milestones"

            forEach(milestones) { item =>
              div {
                classes = "jfx2-demo__milestone"
                text = item
              }
            }
          }

          clientOnly("DemoEditor")(
            div {
              classes = "jfx2-demo__client-fallback"
              text = "SSR-Fallback: dieser Editor ist browser-only und wird erst nach Hydration aktiv."
            }
          ) {
            div {
              classes = "jfx2-demo__client-widget"

              div {
                classes = "jfx2-demo__client-widget-title"
                text = "Client-only Editor Boundary"
              }

              div {
                classes = "jfx2-demo__client-widget-copy"
                text = "Stell dir hier Lexical vor: Der Client-Block wurde auf dem Server nicht ausgefuehrt, ersetzt den Fallback aber im Browser."
              }
            }
          }
        }

        div {
          classes = "jfx2-demo__note"
          text = "SSR export: window.renderSsr(location.pathname). Hydration: wenn #root bereits Markup hat, wird es wiederverwendet."
        }
      }
    }
  }

  private def greetingText(name: String, clicks: Int): String = {
    val displayName =
      Option(name)
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse("Welt")

    s"Hallo $displayName. Button-Klicks: $clicks"
  }

}
