package app.pages

import app.components.Showcase.*
import jfx.action.Button.button
import jfx.control.Carousel
import jfx.control.Carousel.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

object CarouselPage {

  final case class SlideCard(kicker: String, title: String, copy: String, accent: String)

  private val showcaseSlides = Seq(
    SlideCard("Atlas", "Architecture that keeps moving", "The carousel owns the active state while the slide renderer stays clean and declarative.", "#2563eb"),
    SlideCard("Signal", "Auto-advance without hidden magic", "A timer can keep rotating through slides, but the state still lives in a normal property-backed control.", "#0f766e"),
    SlideCard("Northwind", "SSR can surface every state", "Server rendering can expose all slides at once so crawlers and tests can inspect the whole sequence.", "#ea580c"),
    SlideCard("Harbor", "Wrap-around is part of the contract", "When the user reaches the right edge, the next step returns to the beginning instead of getting stuck.", "#7c3aed")
  )

  def render() = {
    showcasePage("Carousel", "Looping slides with explicit state and SSR-friendly output.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Sequenced content",
          "One active slide, no hidden workaround",
          "Carousel keeps the current slide index as explicit component state, wraps forward and backward, and can auto-advance on a configurable interval without breaking the DSL shape."
        )

        metricStrip(
          "Looping" -> "Next after the last slide starts again at the beginning.",
          "Auto timer" -> "Set autoAdvanceMs to rotate automatically.",
          "SSR states" -> "Server rendering can expose every slide state in one response."
        )

        componentShowcase(
          "Autoplay carousel",
          "The control keeps moving on its own, but Previous, Next, and indicators still work as normal explicit actions."
        ) {
          val slides = ListProperty[SlideCard]()
          slides.setAll(showcaseSlides)

          vbox {
            style { gap = "16px" }

            val carouselControl = carousel(slides, autoAdvanceMs = 2600, ssrShowAllStates = true) { (slide, index) =>
              renderSlide(slide, index)
            }

            hbox {
              classes = Seq("showcase-action-row")

              button("Previous") {
                onClick { _ => carouselControl.previous() }
              }

              button("Next") {
                onClick { _ => carouselControl.next() }
              }

              button("Fast autoplay") {
                onClick { _ => carouselControl.$autoAdvanceMsProperty.set(1400) }
              }

              button("Slow autoplay") {
                onClick { _ => carouselControl.$autoAdvanceMsProperty.set(3400) }
              }

              button("Stop timer") {
                onClick { _ => carouselControl.$autoAdvanceMsProperty.set(0) }
              }
            }

            div {
              classes = Seq("showcase-result")
              text =
                carouselControl.$activeIndexProperty.flatMap { index =>
                  carouselControl.$autoAdvanceMsProperty.map { ms =>
                    s"Active slide: ${index + 1} / ${slides.length} | autoAdvanceMs = $ms"
                  }
                }
            }
          }
        }

        apiSection(
          "Carousel DSL",
          "The slide renderer only describes slide content. Looping, indicators, timer, and active state stay inside the control."
        ) {
          codeBlock("scala", """val slides = ListProperty[SlideCard]()
slides.setAll(seedSlides)

carousel(slides, autoAdvanceMs = 2600, ssrShowAllStates = true) { (slide, index) =>
  div {
    classes = Seq("hero-slide")
    text = slide.title
  }
}""")
        }

        insightGrid(
          ("Loop", "No dead right edge", "next() and Previous wrap through the same activeIndex state instead of bolting on a second code path."),
          ("Timer", "Autoplay remains configurable", "autoAdvanceMs = 0 disables the timer, any positive value rotates the active slide in the browser."),
          ("SSR", "All states can be rendered", "With ssrShowAllStates = true the server keeps every slide in the HTML so documentation and crawlers can inspect the whole sequence.")
        )

        apiSection(
          "SSR behavior",
          "The component can either expose the whole sequence or only the active state."
        ) {
          codeBlock("text", """ssrShowAllStates = true
  server HTML contains every slide
  active slide is still marked

ssrShowAllStates = false
  server HTML renders only the active slide

Browser:
  the track translates by activeIndex * 100%
  autoAdvanceMs rotates with the same next() logic""")
        }
      }
    }
  }

  private def renderSlide(slide: SlideCard, index: Int): Unit = {
    vbox {
      classes = Seq("carousel-demo-slide")

      style {
        minHeight = "320px"
        padding = "28px"
        boxSizing = "border-box"
      }

      div {
        classes = Seq("carousel-demo-slide__kicker")
        text = slide.kicker
      }

      div {
        classes = Seq("carousel-demo-slide__title")
        text = s"${index + 1}. ${slide.title}"
      }

      div {
        classes = Seq("carousel-demo-slide__copy")
        text = slide.copy
      }

      div {
        classes = Seq("carousel-demo-slide__footer")

        div {
          classes = Seq("carousel-demo-slide__pill")
          style {
            background = slide.accent
          }
          text = "State"
        }

        div {
          classes = Seq("carousel-demo-slide__accent")
          style {
            color = slide.accent
          }
          text = "Looping sequence"
        }
      }
    }
  }
}
