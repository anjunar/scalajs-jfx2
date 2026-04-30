package app.pages

import app.DemoI18n
import jfx.core.component.Component.*
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object LayoutPage {
  def render() = {
    showcasePage(i18n"Layout & structure", i18n"The architecture of your digital space.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Composition",
          i18n"Layout is the grammar of the surface.",
          i18n"VBox and HBox are deliberately simple. They do not force an external abstraction; they make spatial structure visible directly in the template."
        )

        metricStrip(
          i18n"VBox" -> i18n"Vertical order for forms, panels, and pages.",
          i18n"HBox" -> i18n"Horizontal groups for toolbars, actions, and short rows.",
          i18n"Div" -> i18n"Neutral space for semantic or visual specialization."
        )

        componentShowcase(
          i18n"App shell sketch",
          i18n"A denser layout shows how navigation, content, and detail areas emerge from a few building blocks."
        ) {
          hbox {
            classes = Seq("layout-shell-demo")
            vbox {
              classes = Seq("layout-shell-demo__rail")
              div { classes = Seq("layout-shell-demo__brand"); text = DemoI18n.text(i18n"JFX2") }
              div { classes = "layout-shell-demo__nav is-active"; text = DemoI18n.text(i18n"Components") }
              div { classes = Seq("layout-shell-demo__nav"); text = DemoI18n.text(i18n"Forms") }
              div { classes = Seq("layout-shell-demo__nav"); text = DemoI18n.text(i18n"Data") }
            }
            vbox {
              classes = Seq("layout-shell-demo__content")
              div { classes = Seq("layout-shell-demo__headline"); text = DemoI18n.text(i18n"Showcase surface") }
              div { classes = Seq("layout-shell-demo__copy"); text = DemoI18n.text(i18n"Navigation leads from the left, while the right side keeps room for the active component and its explanation.") }
              hbox {
                classes = Seq("layout-shell-demo__tiles")
                div { classes = Seq("layout-shell-demo__tile"); text = DemoI18n.text(i18n"Live demo") }
                div { classes = Seq("layout-shell-demo__tile"); text = DemoI18n.text(i18n"API") }
                div { classes = Seq("layout-shell-demo__tile"); text = DemoI18n.text(i18n"Notes") }
              }
            }
          }
        }

        componentShowcase(
          i18n"Elegant box layout",
          i18n"The core idea stays small and legible: nest containers, set spacing, place content."
        ) {
          vbox {
            style { gap = "10px" }
            hbox {
              style { gap = "10px" }
              div { classes = Seq("demo-box"); text = DemoI18n.text(i18n"H1") }
              div { classes = Seq("demo-box"); text = DemoI18n.text(i18n"H2") }
            }
            vbox {
              style { gap = "5px" }
              div { classes = Seq("demo-box"); text = DemoI18n.text(i18n"V1") }
              div { classes = Seq("demo-box"); text = DemoI18n.text(i18n"V2") }
            }
          }
        }

        insightGrid(
          (i18n"Readability", i18n"The structure reads from the outside in", i18n"First comes the page, then the zone, then the concrete row or column."),
          (i18n"Stability", i18n"Spacing belongs to containers", i18n"Gap and padding describe the space, not every individual child."),
          (i18n"Extension", i18n"New areas stay local", i18n"A later panel slots in as another container without reshaping existing elements.")
        )

        apiSection(
          i18n"VBox & HBox usage",
          i18n"The layout DSL stays close to the mental model of a UI sketch."
        ) {
          codeBlock("scala", """vbox {
  style { gap = "10px" }
  
  hbox {
    div { text = DemoI18n.text(i18n"Left") }
    div { text = DemoI18n.text(i18n"Right") }
  }
}""")
        }
      }
    }
  }
}
