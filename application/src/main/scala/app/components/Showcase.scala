package app.components

import app.DemoI18n
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.i18n.{RuntimeMessage, i18n}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import scala.annotation.targetName

object Showcase {

  def showcasePage(title: String, subtitle: String)(content: => Unit): Unit =
    renderShowcasePage(Property(title), Property(subtitle))(content)

  def showcasePage(title: RuntimeMessage, subtitle: RuntimeMessage)(content: => Unit): Unit =
    renderShowcasePage(DemoI18n.text(title), DemoI18n.text(subtitle))(content)

  def showcasePage(title: ReadOnlyProperty[String], subtitle: ReadOnlyProperty[String])(content: => Unit): Unit =
    renderShowcasePage(title, subtitle)(content)

  def sectionIntro(kicker: String, title: String, body: String): Unit =
    renderSectionIntro(Property(kicker), Property(title), Property(body))

  def sectionIntro(kicker: RuntimeMessage, title: RuntimeMessage, body: RuntimeMessage): Unit =
    renderSectionIntro(DemoI18n.text(kicker), DemoI18n.text(title), DemoI18n.text(body))

  def sectionIntro(kicker: ReadOnlyProperty[String], title: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit =
    renderSectionIntro(kicker, title, body)

  def metricStrip(items: (String, String)*): Unit =
    renderMetricStrip(items.map { case (value, label) => Property(value) -> Property(label) }*)

  @targetName("metricStripMessages")
  def metricStrip(items: (RuntimeMessage, RuntimeMessage)*): Unit =
    renderMetricStrip(items.map { case (value, label) => DemoI18n.text(value) -> DemoI18n.text(label) }*)

  def insightGrid(items: (String, String, String)*): Unit =
    renderInsightGrid(items.map { case (label, title, body) => (Property(label), Property(title), Property(body)) }*)

  @targetName("insightGridMessages")
  def insightGrid(items: (RuntimeMessage, RuntimeMessage, RuntimeMessage)*): Unit =
    renderInsightGrid(items.map { case (label, title, body) => (DemoI18n.text(label), DemoI18n.text(title), DemoI18n.text(body)) }*)

  def noteBlock(title: String, body: String): Unit =
    renderNoteBlock(Property(title), Property(body))

  def noteBlock(title: RuntimeMessage, body: RuntimeMessage): Unit =
    renderNoteBlock(DemoI18n.text(title), DemoI18n.text(body))

  def patternList(title: String, items: String*): Unit =
    renderPatternList(Property(title), items.map(Property(_))*)

  def patternList(title: RuntimeMessage, items: RuntimeMessage*): Unit =
    renderPatternList(DemoI18n.text(title), items.map(DemoI18n.text)*)

  def componentShowcase(title: String)(content: => Unit): Unit =
    componentShowcase(title, "")(content)

  def componentShowcase(title: String, summary: String)(content: => Unit): Unit =
    renderComponentShowcase(Property(title), Some(Property(summary)))(content)

  def componentShowcase(title: RuntimeMessage)(content: => Unit): Unit =
    componentShowcase(DemoI18n.text(title))(content)

  def componentShowcase(title: RuntimeMessage, summary: RuntimeMessage)(content: => Unit): Unit =
    renderComponentShowcase(DemoI18n.text(title), Some(DemoI18n.text(summary)))(content)

  def componentShowcase(title: ReadOnlyProperty[String])(content: => Unit): Unit =
    renderComponentShowcase(title)(content)

  def componentShowcase(title: ReadOnlyProperty[String], summary: ReadOnlyProperty[String])(content: => Unit): Unit =
    renderComponentShowcase(title, Some(summary))(content)

  def apiSection(title: String)(content: => Unit): Unit =
    apiSection(title, "")(content)

  def apiSection(title: String, summary: String)(content: => Unit): Unit =
    renderApiSection(Property(title), Some(Property(summary)))(content)

  def apiSection(title: RuntimeMessage)(content: => Unit): Unit =
    renderApiSection(DemoI18n.text(title))(content)

  def apiSection(title: RuntimeMessage, summary: RuntimeMessage)(content: => Unit): Unit =
    renderApiSection(DemoI18n.text(title), Some(DemoI18n.text(summary)))(content)

  def apiSection(title: ReadOnlyProperty[String])(content: => Unit): Unit =
    renderApiSection(title)(content)

  def apiSection(title: ReadOnlyProperty[String], summary: ReadOnlyProperty[String])(content: => Unit): Unit =
    renderApiSection(title, Some(summary))(content)

  def codeBlock(lang: String, code: String): Unit = {
    vbox {
      classes = Seq("code-block")
      div {
        classes = Seq("code-block__lang")
        text = lang
      }
      div {
        classes = Seq("code-block__content")
        text = code
      }
    }
  }

  private def renderShowcasePage(title: ReadOnlyProperty[String], subtitle: ReadOnlyProperty[String])(content: => Unit): Unit = {
    vbox {
      classes = Seq("showcase-page")
      vbox {
        classes = Seq("showcase-page__header")
        div { classes = Seq("showcase-page__eyebrow"); text = DemoI18n.text(i18n"JFX2 Showcase") }
        div { classes = Seq("showcase-page__title"); text = title }
        div { classes = Seq("showcase-page__subtitle"); text = subtitle }
      }
      div {
        classes = Seq("showcase-page__content")
        content
      }
    }
  }

  private def renderSectionIntro(kicker: ReadOnlyProperty[String], title: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("showcase-section-intro")
      div { classes = Seq("showcase-section-intro__kicker"); text = kicker }
      div { classes = Seq("showcase-section-intro__title"); text = title }
      div { classes = Seq("showcase-section-intro__body"); text = body }
    }
  }

  private def renderMetricStrip(items: (ReadOnlyProperty[String], ReadOnlyProperty[String])*): Unit = {
    div {
      classes = Seq("showcase-metric-strip")
      items.foreach { case (value, label) =>
        vbox {
          classes = Seq("showcase-metric")
          div { classes = Seq("showcase-metric__value"); text = value }
          div { classes = Seq("showcase-metric__label"); text = label }
        }
      }
    }
  }

  private def renderInsightGrid(items: (ReadOnlyProperty[String], ReadOnlyProperty[String], ReadOnlyProperty[String])*): Unit = {
    div {
      classes = Seq("showcase-insight-grid")
      items.zipWithIndex.foreach { case ((label, title, body), index) =>
        vbox {
          classes = s"showcase-insight showcase-insight--${index % 3}"
          div { classes = Seq("showcase-insight__label"); text = label }
          div { classes = Seq("showcase-insight__title"); text = title }
          div { classes = Seq("showcase-insight__body"); text = body }
        }
      }
    }
  }

  private def renderNoteBlock(title: ReadOnlyProperty[String], body: ReadOnlyProperty[String]): Unit = {
    vbox {
      classes = Seq("showcase-note")
      div { classes = Seq("showcase-note__title"); text = title }
      div { classes = Seq("showcase-note__body"); text = body }
    }
  }

  private def renderPatternList(title: ReadOnlyProperty[String], items: ReadOnlyProperty[String]*): Unit = {
    vbox {
      classes = Seq("showcase-pattern-list")
      div { classes = Seq("showcase-pattern-list__title"); text = title }
      div {
        classes = Seq("showcase-pattern-list__items")
        items.foreach { item =>
          div {
            classes = Seq("showcase-pattern-list__item")
            text = item
          }
        }
      }
    }
  }

  private def renderComponentShowcase(title: ReadOnlyProperty[String], summary: Option[ReadOnlyProperty[String]] = None)(content: => Unit): Unit = {
    vbox {
      classes = Seq("component-showcase")
      vbox {
        classes = Seq("component-showcase__header")
        div { classes = Seq("component-showcase__title"); text = title }
        summary.foreach { value =>
          if (value.get.nonEmpty) {
            div { classes = Seq("component-showcase__summary"); text = value }
          }
        }
      }
      div {
        classes = Seq("component-showcase__render")
        content
      }
    }
  }

  private def renderApiSection(title: ReadOnlyProperty[String], summary: Option[ReadOnlyProperty[String]] = None)(content: => Unit): Unit = {
    vbox {
      classes = Seq("api-section")
      vbox {
        classes = Seq("api-section__header")
        div { classes = Seq("api-section__title"); text = title }
        summary.foreach { value =>
          if (value.get.nonEmpty) {
            div { classes = Seq("api-section__summary"); text = value }
          }
        }
      }
      div {
        classes = Seq("api-section__content")
        content
      }
    }
  }
}
