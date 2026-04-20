package app.components

import jfx.core.component.Component.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

object Showcase {

  def showcasePage(title: String, subtitle: String)(content: => Unit) = {
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

  def componentShowcase(title: String)(content: => Unit) = {
    vbox {
      classes = "component-showcase"
      div { classes = "component-showcase__title"; text = title }
      div {
        classes = "component-showcase__render"
        content
      }
    }
  }

  def apiSection(title: String)(content: => Unit) = {
    vbox {
      classes = "api-section"
      div { classes = "api-section__title"; text = title }
      div {
        classes = "api-section__content"
        content
      }
    }
  }

  def codeBlock(lang: String, code: String) = {
    div {
      classes = "code-block"
      div {
        classes = "code-block__content"
        text = code
      }
    }
  }

}
