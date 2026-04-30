package app.pages

import app.DemoI18n
import app.components.Showcase.*
import jfx.action.Button.*
import jfx.core.component.Component.*
import jfx.core.state.ReadOnlyProperty
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

object OverviewPage {
  def render() = {
    showcasePage(i18n"Welcome to JFX2", i18n"Your new home for reactive UIs in Scala.js.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Origin story",
          i18n"After 17 years of looking for clarity, the project started to feel less like a thesis and more like relief.",
          i18n"JFX2 is the answer I wanted after living with frameworks that promised simplicity but quietly handed over control. It chooses explicit lifecycles, honest reactivity, and a DSL that stays readable when the codebase grows."
        )

        sectionIntro(
          i18n"Vision",
          i18n"A documentation site that feels like a real workbench.",
          i18n"The showcase should not just prove that components render. It should show how JFX2 is meant to feel: declarative, server-stable, reactive in the browser, and readable enough that you can still nod to it six months later."
        )

        metricStrip(
          i18n"SSR" -> i18n"Server HTML and client hydration share the same structure.",
          i18n"DSL" -> i18n"Templates stay declarative and free of DOM handwork.",
          i18n"Live" -> i18n"Every page shows a usable example instead of a dry API list."
        )

        componentShowcase(
          i18n"Message-centered I18n",
          i18n"The English source lives in Scala code. The catalog attaches multiple languages to exactly that one message."
        ) {
          vbox {
            classes = Seq("i18n-demo")

            div {
              classes = Seq("i18n-demo__toolbar")
              div {
                classes = Seq("i18n-demo__locale")
                text = DemoI18n.localeLabel.flatMap(locale => DemoI18n.text(i18n"Locale: $locale"))
              }
              button(DemoI18n.text(i18n"Switch locale")) {
                classes = Seq("calm-action", "calm-action--secondary")
                onClick { _ => DemoI18n.toggle() }
              }
            }

            div {
              classes = Seq("i18n-demo__grid")

              i18nSample(
                """i18n"Delete document"""",
                DemoI18n.text(DemoI18n.Messages.deleteDocument)
              )

              i18nSample(
                """i18n"User $user invited you to $group"""",
                DemoI18n.text(DemoI18n.Messages.invitation("Mira", "Core Team"))
              )

              i18nSample(
                """i18n"Missing translations fall back to English"""",
                DemoI18n.text(DemoI18n.Messages.fallbackRule)
              )
            }
          }
        }

        insightGrid(
          (i18n"01", i18n"Readability first", i18n"Components are shown so their purpose, state, and placement are immediately clear."),
          (i18n"02", i18n"Hydration in view", i18n"Examples avoid hidden DOM drift and keep virtual containers understandable."),
          (i18n"03", i18n"A growing system", i18n"New components get room for context, variants, API, and architectural hints.")
        )

        patternList(
          i18n"What you find on the component pages",
          i18n"A short explanation of when the component makes sense.",
          i18n"At least one real live state with data or interaction.",
          i18n"Concrete DSL examples that stay close to production code.",
          i18n"Notes about stability, cursor behavior, SSR, or reactive properties."
        )

        noteBlock(
          i18n"Next step",
          i18n"Pick a component on the left. Each page is now denser and still leaves room for more building blocks without losing the thread."
        )
      }
    }
  }

  private def i18nSample(source: String, resolved: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("i18n-demo__sample")
      div { classes = Seq("i18n-demo__label"); text = DemoI18n.text(i18n"Source") }
      div { classes = Seq("i18n-demo__source"); text = source }
      div { classes = Seq("i18n-demo__label"); text = DemoI18n.text(i18n"Resolved") }
      div { classes = Seq("i18n-demo__resolved"); text = resolved }
    }
  }
}
