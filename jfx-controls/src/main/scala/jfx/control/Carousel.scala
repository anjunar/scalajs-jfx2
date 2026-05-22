package jfx.control

import jfx.action.Button.*
import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{Disposable, ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach

import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}

final class Carousel[T](
                         initialItems: ListProperty[T] | Null = null,
                         initialActiveIndex: Int = 0,
                         initialAutoAdvanceMs: Int = 0,
                         initialWrapAround: Boolean = true,
                         initialSsrShowAllStates: Boolean = true,
                         initialRenderer: Carousel.Renderer[T] = (_: T, _: Int) => ()
                       ) extends Box("section") {

  private val itemsRefProperty = Property[ListProperty[T]](normalizeItems(initialItems))

  val $activeIndexProperty = Property(math.max(0, initialActiveIndex))
  val $autoAdvanceMsProperty = Property(math.max(0, initialAutoAdvanceMs))
  val $wrapAroundProperty = Property(initialWrapAround)
  val $ssrShowAllStatesProperty = Property(initialSsrShowAllStates)

  private var slideRenderer: Carousel.Renderer[T] = normalizeRenderer(initialRenderer)
  private var itemsObserver: Disposable = Carousel.noopDisposable
  private var autoAdvanceHandle: SetIntervalHandle | Null = null

  def $itemsProperty: Property[ListProperty[T]] =
    itemsRefProperty

  def $items: ListProperty[T] =
    itemsRefProperty.get

  def setItems(value: ListProperty[T] | Null): Unit = {
    val normalized = normalizeItems(value)
    if (!itemsRefProperty.get.eq(normalized)) {
      itemsRefProperty.setAlways(normalized)
    }
  }

  def setRenderer(renderer: Carousel.Renderer[T]): Unit =
    slideRenderer = normalizeRenderer(renderer)

  def getRenderer: Carousel.Renderer[T] =
    slideRenderer

  def currentItem: Option[T] =
    if (slideCount == 0) None else Some($items(normalizedActiveIndex))

  def goTo(index: Int): Unit =
    $activeIndexProperty.set(normalizeIndex(index))

  def next(): Unit =
    if (slideCount > 1) {
      goTo(normalizedActiveIndex + 1)
    }

  def previous(): Unit =
    if (slideCount > 1) {
      goTo(normalizedActiveIndex - 1)
    }

  override def compose(): Unit = {
    given Component = this

    addClass("jfx-carousel")
    if (RenderBackend.current.isServer && $ssrShowAllStatesProperty.get) {
      addClass("jfx-carousel--ssr-all-states")
    }
    classIf("jfx-carousel--empty", itemsRefProperty.map(_.length == 0))
    classIf("jfx-carousel--single", itemsRefProperty.map(_.length <= 1))

    role = "region"
    tabIndex = 0

    addDisposable(itemsRefProperty.observe { _ =>
      rewireItemsObserver()
      normalizeActiveIndex()
      restartAutoAdvance()
    })
    addDisposable($autoAdvanceMsProperty.observeWithoutInitial(_ => restartAutoAdvance()))
    addDisposable($wrapAroundProperty.observeWithoutInitial { _ =>
      normalizeActiveIndex()
      restartAutoAdvance()
    })
    addDisposable(() => {
      itemsObserver.dispose()
      stopAutoAdvance()
    })

    onKeyDown { event =>
      event.key match {
        case "ArrowRight" =>
          event.preventDefault()
          next()
        case "ArrowLeft" =>
          event.preventDefault()
          previous()
        case _ =>
      }
    }

    div {
      addClass("jfx-carousel__viewport")

      div {
        addClass("jfx-carousel__track")
        style {
          transform_=(trackTransformProperty)
        }

        renderSlides()
      }
    }

    condition(itemsRefProperty.map(_.length > 1)) {
      thenDo {
        div {
          addClass("jfx-carousel__controls")

          button() {
            addClass("jfx-carousel__nav")
            buttonType = "button"
            text = "Previous"
            onClick { _ => previous() }
          }

          div {
            addClass("jfx-carousel__status")
            text = statusTextProperty
          }

          div {
            addClass("jfx-carousel__indicators")

            forEach($items) { (_: T, index: Int) =>
              Carousel.indicatorButton(index, $activeIndexProperty, goTo)
            }
          }

          button() {
            addClass("jfx-carousel__nav")
            buttonType = "button"
            text = "Next"
            onClick { _ => next() }
          }
        }
      }
    }
  }

  private def renderSlides()(using Component): Unit =
    if (RenderBackend.current.isServer && !$ssrShowAllStatesProperty.get) {
      currentItem.foreach { item =>
        Carousel.carouselSlide(
          item = item,
          index = normalizedActiveIndex,
          activeIndexProperty = $activeIndexProperty,
          renderer = slideRenderer
        )
      }
    } else {
      forEach($items) { (item: T, index: Int) =>
        Carousel.carouselSlide(
          item = item,
          index = index,
          activeIndexProperty = $activeIndexProperty,
          renderer = slideRenderer
        )
      }
    }

  private def rewireItemsObserver(): Unit = {
    itemsObserver.dispose()
    itemsObserver = $items.observeChanges { _ =>
      normalizeActiveIndex()
      restartAutoAdvance()
    }
  }

  private def restartAutoAdvance(): Unit = {
    stopAutoAdvance()

    if (RenderBackend.current.isServer) return
    if (slideCount <= 1) return

    val intervalMs = math.max(0, $autoAdvanceMsProperty.get)
    if (intervalMs <= 0) return

    autoAdvanceHandle = setInterval(intervalMs) {
      next()
    }
  }

  private def stopAutoAdvance(): Unit =
    if (autoAdvanceHandle != null) {
      clearInterval(autoAdvanceHandle.nn)
      autoAdvanceHandle = null
    }

  private def normalizeActiveIndex(): Unit =
    $activeIndexProperty.set(normalizeIndex($activeIndexProperty.get))

  private def normalizeIndex(index: Int): Int = {
    val count = slideCount

    if (count <= 0) {
      0
    } else if ($wrapAroundProperty.get) {
      math.floorMod(index, count)
    } else {
      math.max(0, math.min(count - 1, index))
    }
  }

  private def normalizedActiveIndex: Int =
    normalizeIndex($activeIndexProperty.get)

  private def slideCount: Int =
    $items.length

  private def trackTransformProperty: ReadOnlyProperty[String] =
    $activeIndexProperty.map { index =>
      if (RenderBackend.current.isServer && $ssrShowAllStatesProperty.get) "none"
      else s"translateX(-${normalizeIndex(index) * 100}%)"
    }

  private def statusTextProperty: ReadOnlyProperty[String] =
    $activeIndexProperty.map { index =>
      val count = slideCount
      if (count <= 0) "0 / 0"
      else s"${normalizeIndex(index) + 1} / $count"
    }

  private def normalizeItems(value: ListProperty[T] | Null): ListProperty[T] =
    if (value == null) new ListProperty[T]() else value

  private def normalizeRenderer(renderer: Carousel.Renderer[T]): Carousel.Renderer[T] =
    if (renderer == null) ((_: T, _: Int) => ()) else renderer
}

object Carousel {
  private[control] val noopDisposable: Disposable = () => ()

  type Renderer[T] = (T, Int) => Unit

  def carousel[T](init: Carousel[T] ?=> Unit): Carousel[T] =
    DslRuntime.build(new Carousel[T]())(init)

  def carousel[T](
                   items: ListProperty[T],
                   activeIndex: Int = 0,
                   autoAdvanceMs: Int = 0,
                   wrapAround: Boolean = true,
                   ssrShowAllStates: Boolean = true
                 )(renderer: Renderer[T]): Carousel[T] =
    DslRuntime.build(
      new Carousel[T](
        initialItems = items,
        initialActiveIndex = activeIndex,
        initialAutoAdvanceMs = autoAdvanceMs,
        initialWrapAround = wrapAround,
        initialSsrShowAllStates = ssrShowAllStates,
        initialRenderer = renderer
      )
    ) {}

  def items[T](using carousel: Carousel[T]): ListProperty[T] =
    carousel.$items

  def items_=[T](value: ListProperty[T])(using carousel: Carousel[T]): Unit =
    carousel.setItems(value)

  def items_=[T](value: scala.collection.IterableOnce[T])(using carousel: Carousel[T]): Unit =
    value match {
      case property: ListProperty[?] =>
        carousel.setItems(property.asInstanceOf[ListProperty[T]])
      case _ =>
        carousel.$items.setAll(value)
    }

  def activeIndex(using carousel: Carousel[?]): Int =
    carousel.$activeIndexProperty.get

  def activeIndexProperty(using carousel: Carousel[?]): Property[Int] =
    carousel.$activeIndexProperty

  def activeIndex_=(value: Int)(using carousel: Carousel[?]): Unit =
    carousel.goTo(value)

  def autoAdvanceMs(using carousel: Carousel[?]): Int =
    carousel.$autoAdvanceMsProperty.get

  def autoAdvanceMs_=(value: Int)(using carousel: Carousel[?]): Unit =
    carousel.$autoAdvanceMsProperty.set(math.max(0, value))

  def wrapAround(using carousel: Carousel[?]): Boolean =
    carousel.$wrapAroundProperty.get

  def wrapAround_=(value: Boolean)(using carousel: Carousel[?]): Unit =
    carousel.$wrapAroundProperty.set(value)

  def ssrShowAllStates(using carousel: Carousel[?]): Boolean =
    carousel.$ssrShowAllStatesProperty.get

  def ssrShowAllStates_=(value: Boolean)(using carousel: Carousel[?]): Unit =
    carousel.$ssrShowAllStatesProperty.set(value)

  def slideRenderer[T](using carousel: Carousel[T]): Renderer[T] =
    carousel.getRenderer

  def slideRenderer_=[T](value: Renderer[T])(using carousel: Carousel[T]): Unit =
    carousel.setRenderer(value)

  def next(using carousel: Carousel[?]): Unit =
    carousel.next()

  def previous(using carousel: Carousel[?]): Unit =
    carousel.previous()

  def goTo(index: Int)(using carousel: Carousel[?]): Unit =
    carousel.goTo(index)

  private def indicatorButton(
                               index: Int,
                               activeIndexProperty: Property[Int],
                               onSelect: Int => Unit
                             ): Box =
    DslRuntime.build(new Box("button")) {
      given Component = summon[Box]

      addClass("jfx-carousel__indicator")
      classIf("is-active", activeIndexProperty.map(active => active == index))
      host.setAttribute("type", "button")
      host.setAttribute("aria-label", s"Go to slide ${index + 1}")
      text = (index + 1).toString
      onClick { _ => onSelect(index) }
    }

  private def carouselSlide[T](
                                item: T,
                                index: Int,
                                activeIndexProperty: Property[Int],
                                renderer: Renderer[T]
                              ): CarouselSlide[T] =
    DslRuntime.build(new CarouselSlide(item, index, activeIndexProperty, renderer)) {}

  private final class CarouselSlide[T](
                                        item: T,
                                        index: Int,
                                        activeIndexProperty: Property[Int],
                                        renderer: Renderer[T]
                                      ) extends Box("div") {

    override def compose(): Unit = {
      given Component = this

      addClass("jfx-carousel__slide")
      classIf("is-active", activeIndexProperty.map(_ == index))

      addDisposable(activeIndexProperty.observe { active =>
        host.setAttribute("aria-hidden", (active != index).toString)
      })
      host.setAttribute("data-slide-index", index.toString)

      renderer(item, index)
    }
  }
}
