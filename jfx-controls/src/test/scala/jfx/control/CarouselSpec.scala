package jfx.control

import jfx.control.Carousel.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CarouselSpec extends AnyFlatSpec with Matchers {

  private def newCarousel(
                           items: ListProperty[String],
                           activeIndex: Int = 0,
                           wrapAround: Boolean = true
                         ): Carousel[String] =
    new Carousel[String](
      initialItems = items,
      initialActiveIndex = activeIndex,
      initialWrapAround = wrapAround
    )

  "Carousel" should "wrap forward when it reaches the end" in {
    val items = ListProperty[String]()
    items.setAll(Seq("One", "Two", "Three"))

    val carouselControl = newCarousel(items, activeIndex = 2)

    carouselControl.next()

    carouselControl.$activeIndexProperty.get shouldBe 0
    carouselControl.currentItem shouldBe Some("One")
  }

  it should "wrap backward when it moves before the first slide" in {
    val items = ListProperty[String]()
    items.setAll(Seq("One", "Two", "Three"))

    val carouselControl = newCarousel(items, activeIndex = 0)

    carouselControl.previous()

    carouselControl.$activeIndexProperty.get shouldBe 2
    carouselControl.currentItem shouldBe Some("Three")
  }

  it should "clamp the active index when wrapAround is disabled" in {
    val items = ListProperty[String]()
    items.setAll(Seq("One", "Two", "Three"))

    val carouselControl = newCarousel(items, activeIndex = 2, wrapAround = false)

    carouselControl.next()
    carouselControl.$activeIndexProperty.get shouldBe 2

    carouselControl.goTo(-10)
    carouselControl.$activeIndexProperty.get shouldBe 0
  }

  it should "render all slide states in SSR by default" in {
    val items = ListProperty[String]()
    items.setAll(Seq("One", "Two", "Three"))

    val html = Ssr.renderToString {
      carousel(items, activeIndex = 1) { (item, index) =>
        div { text = s"$index:$item" }
      }
    }

    html should include("jfx-carousel--ssr-all-states")
    html should include("0:One")
    html should include("1:Two")
    html should include("2:Three")
    html should include("2 / 3")
  }

  it should "render only the active state in SSR when configured" in {
    val items = ListProperty[String]()
    items.setAll(Seq("One", "Two", "Three"))

    val html = Ssr.renderToString {
      carousel(items, activeIndex = 1, ssrShowAllStates = false) { (item, index) =>
        div { text = s"$index:$item" }
      }
    }

    html should not include "jfx-carousel--ssr-all-states"
    html should not include "0:One"
    html should include("1:Two")
    html should not include "2:Three"
  }
}
