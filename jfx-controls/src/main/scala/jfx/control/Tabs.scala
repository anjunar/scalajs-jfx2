package jfx.control

import jfx.action.Button.{button, buttonType}
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.ObserveRender.observeRender

final class Tabs(
    initialSelectedIndex: Int = 0,
    initialRenderMode: Tabs.RenderMode = Tabs.RenderMode.ActiveOnly
) extends Box("section") {

  val $tabsProperty = ListProperty[Tabs.TabSpec]()
  val $selectedIndexProperty = Property(math.max(0, initialSelectedIndex))
  val $renderModeProperty = Property(initialRenderMode)
  private val $renderVersionProperty = Property(0)

  def addTab(tab: Tabs.TabSpec): Unit = {
    $tabsProperty += tab
    normalizeSelectedIndex()
    requestRender()
  }

  override def compose(): Unit = {
    given Component = this

    addClass("jfx-tabs")
    classIf("jfx-tabs--empty", $tabsProperty.map(_.isEmpty))

    onKeyDown { event =>
      event.key match {
        case "ArrowRight" | "ArrowDown" =>
          event.preventDefault()
          selectNext()
        case "ArrowLeft" | "ArrowUp" =>
          event.preventDefault()
          selectPrevious()
        case "Home" =>
          event.preventDefault()
          select(0)
        case "End" =>
          event.preventDefault()
          select($tabsProperty.length - 1)
        case _ =>
      }
    }

    addDisposable($tabsProperty.observeChanges { _ =>
      normalizeSelectedIndex()
      requestRender()
    })
    addDisposable($selectedIndexProperty.observeWithoutInitial { _ =>
      normalizeSelectedIndex()
      requestRender()
    })

    div {
      addClass("jfx-tabs__header")
      role = "tablist"

      observeRender($tabsProperty.asProperty) { tabs =>
        tabs.zipWithIndex.foreach { case (tab, index) =>
          button(tab.title) {
            buttonType = "button"
            classes =
              if (index == selectedIndex) Seq("jfx-tabs__trigger", "jfx-tabs__trigger--active")
              else Seq("jfx-tabs__trigger")
            role = "tab"
            tabIndex = if (index == selectedIndex) 0 else -1
            onClick { _ =>
              select(index)
            }
          }
        }
      }
    }

    div {
      addClass("jfx-tabs__content")

      observeRender($renderModeProperty) { renderMode =>
        renderMode match {
          case Tabs.RenderMode.ActiveOnly =>
            observeRender($renderVersionProperty) { _ =>
              activeTab.foreach { tab =>
                tab.render(summon[Component])
              }
            }
          case Tabs.RenderMode.KeepMountedHidden =>
            observeRender($tabsProperty.asProperty) { tabs =>
              tabs.zipWithIndex.foreach { case (tab, index) =>
                val visibleDisplay =
                  $selectedIndexProperty.map { currentIndex =>
                    if (normalizedIndex(currentIndex) == index) ""
                    else "none"
                  }

                div {
                  addClass("jfx-tabs__panel")
                  style {
                    display = visibleDisplay
                  }
                  tab.render(summon[Component])
                }
              }
            }
        }
      }
    }
  }

  private def selectedIndex: Int =
    normalizedIndex($selectedIndexProperty.get)

  private def activeTab: Option[Tabs.TabSpec] =
    if ($tabsProperty.isEmpty) None
    else Some($tabsProperty(selectedIndex))

  def setSelectedIndex(index: Int): Unit =
    $selectedIndexProperty.set(normalizedIndex(index))

  private def select(index: Int): Unit =
    setSelectedIndex(index)

  private def selectNext(): Unit =
    if ($tabsProperty.nonEmpty) {
      select(selectedIndex + 1)
    }

  private def selectPrevious(): Unit =
    if ($tabsProperty.nonEmpty) {
      select(selectedIndex - 1)
    }

  private def normalizedIndex(index: Int): Int =
    if ($tabsProperty.isEmpty) 0
    else math.max(0, math.min($tabsProperty.length - 1, index))

  private def normalizeSelectedIndex(): Unit =
    $selectedIndexProperty.set(normalizedIndex($selectedIndexProperty.get))

  private def requestRender(): Unit =
    $renderVersionProperty.setAlways($renderVersionProperty.get + 1)
}

object Tabs {
  enum RenderMode {
    case ActiveOnly
    case KeepMountedHidden
  }

  final class TabSpec(val title: String, val render: Component => Unit)

  def tabs(init: Tabs ?=> Unit): Tabs =
    tabs(renderMode = Tabs.RenderMode.ActiveOnly)(init)

  def tabs(
      renderMode: Tabs.RenderMode
  )(init: Tabs ?=> Unit): Tabs =
    DslRuntime.build(new Tabs(initialRenderMode = renderMode))(init)

  def tab(title: String)(content: Component ?=> Unit)(using tabs: Tabs): Unit =
    tabs.addTab(
      new TabSpec(
        title = title,
        render = component => {
          given Component = component
          content
        }
      )
    )

  def selectedIndex(using tabs: Tabs): Int =
    tabs.$selectedIndexProperty.get

  def selectedIndex_=(value: Int)(using tabs: Tabs): Unit =
    tabs.setSelectedIndex(value)

  def selectedIndex_=(value: ReadOnlyProperty[Int])(using tabs: Tabs): Unit =
    tabs.addDisposable(value.observe(v => tabs.setSelectedIndex(v)))

  def renderMode(using tabs: Tabs): Tabs.RenderMode =
    tabs.$renderModeProperty.get

  def renderMode_=(value: Tabs.RenderMode)(using tabs: Tabs): Unit = {
    tabs.$renderModeProperty.set(value)
    tabs.$renderVersionProperty.setAlways(tabs.$renderVersionProperty.get + 1)
  }
}
