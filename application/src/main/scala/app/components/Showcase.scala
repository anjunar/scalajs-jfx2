package app.components

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

object Showcase {

  def showcasePage(title: String, subtitle: String)(content: => Unit): Unit = {
    vbox {
      classes = "showcase-page"
      vbox {
        classes = "showcase-page__header"
        div { classes = "showcase-page__eyebrow"; text = "JFX2 Showcase" }
        div { classes = "showcase-page__title"; text = title }
        div { classes = "showcase-page__subtitle"; text = subtitle }
      }
      div {
        classes = "showcase-page__content"
        content
      }
    }
  }

  def sectionIntro(kicker: String, title: String, body: String): Unit = {
    vbox {
      classes = "showcase-section-intro"
      div { classes = "showcase-section-intro__kicker"; text = kicker }
      div { classes = "showcase-section-intro__title"; text = title }
      div { classes = "showcase-section-intro__body"; text = body }
    }
  }

  def metricStrip(items: (String, String)*): Unit = {
    div {
      classes = "showcase-metric-strip"
      items.foreach { case (value, label) =>
        vbox {
          classes = "showcase-metric"
          div { classes = "showcase-metric__value"; text = value }
          div { classes = "showcase-metric__label"; text = label }
        }
      }
    }
  }

  def insightGrid(items: (String, String, String)*): Unit = {
    div {
      classes = "showcase-insight-grid"
      items.zipWithIndex.foreach { case ((label, title, body), index) =>
        vbox {
          classes = s"showcase-insight showcase-insight--${index % 3}"
          div { classes = "showcase-insight__label"; text = label }
          div { classes = "showcase-insight__title"; text = title }
          div { classes = "showcase-insight__body"; text = body }
        }
      }
    }
  }

  def noteBlock(title: String, body: String): Unit = {
    vbox {
      classes = "showcase-note"
      div { classes = "showcase-note__title"; text = title }
      div { classes = "showcase-note__body"; text = body }
    }
  }

  def patternList(title: String, items: String*): Unit = {
    vbox {
      classes = "showcase-pattern-list"
      div { classes = "showcase-pattern-list__title"; text = title }
      div {
        classes = "showcase-pattern-list__items"
        items.foreach { item =>
          div {
            classes = "showcase-pattern-list__item"
            text = item
          }
        }
      }
    }
  }

  def componentShowcase(title: String)(content: => Unit): Unit =
    componentShowcase(title, "")(content)

  def componentShowcase(title: String, summary: String)(content: => Unit): Unit = {
    vbox {
      classes = "component-showcase"
      vbox {
        classes = "component-showcase__header"
        div { classes = "component-showcase__title"; text = title }
        if (summary.nonEmpty) {
          div { classes = "component-showcase__summary"; text = summary }
        }
      }
      div {
        classes = "component-showcase__render"
        content
      }
    }
  }

  def apiSection(title: String)(content: => Unit): Unit =
    apiSection(title, "")(content)

  def apiSection(title: String, summary: String)(content: => Unit): Unit = {
    vbox {
      classes = "api-section"
      vbox {
        classes = "api-section__header"
        div { classes = "api-section__title"; text = title }
        if (summary.nonEmpty) {
          div { classes = "api-section__summary"; text = summary }
        }
      }
      div {
        classes = "api-section__content"
        content
      }
    }
  }

  def codeBlock(lang: String, code: String): Unit = {
    vbox {
      classes = "code-block"
      div {
        classes = "code-block__lang"
        text = lang
      }
      div {
        classes = "code-block__content"
        text = code
      }
    }
  }

}
