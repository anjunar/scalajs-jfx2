package app

import jfx.router.Route
import jfx.router.Route.route
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.core.component.Component.*
import jfx.control.Heading.heading

object Routes {
  val routes = Seq(
    route("/") {
      vbox {
        classes = Seq("app-nav-intro") // Use existing card-like style for intro
        div {
          classes = Seq("app-nav-intro__title")
          text = "Welcome to JFX2"
        }
        div {
          classes = Seq("app-nav-intro__copy")
          text = "This is the next generation of the Scala.js JFX framework, featuring a unified architecture for SSR, Hydration, and dynamic UI updates."
        }
      }
    },
    route("/components") {
      vbox {
        classes = Seq("app-nav-intro")
        div {
          classes = Seq("app-nav-intro__title")
          text = "Components"
        }
        div {
          classes = Seq("app-nav-intro__copy")
          text = "All components now share a stable parent context and a single construction path."
        }
        
        div {
          classes = Seq("app-nav-group")
          div {
             classes = Seq("app-nav-card")
             div { classes = Seq("app-nav-card__title"); text = "Button" }
             div { classes = Seq("app-nav-card__copy"); text = "Standard action component." }
          }
          div {
             classes = Seq("app-nav-card")
             div { classes = Seq("app-nav-card__title"); text = "Input" }
             div { classes = Seq("app-nav-card__copy"); text = "Reactive form input." }
          }
        }
      }
    },
    route("/about") {
      vbox {
        classes = Seq("app-nav-intro")
        div {
          classes = Seq("app-nav-intro__title")
          text = "Consistency & Truth"
        }
        div {
          classes = Seq("app-nav-intro__copy")
          text = "JFX2 eliminates raw DOM manipulation in favor of a truthful component tree."
        }
      }
    }
  )
}
