package jfx.control

import jfx.core.component.Component
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime
import jfx.statement.Condition.*

class Image extends Component {
  override def tagName: String = "div"

  val srcProperty: Property[String] = Property("")
  val altProperty: Property[String] = Property("")

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-image")
    style {
      display = "inline-block"
      overflow = "hidden"
      background = "var(--aj-canvas)"
    }

    condition(srcProperty.map(value => Option(value).exists(_.trim.nonEmpty))) {
      thenDo {
        ImageNode(srcProperty, altProperty)
      }
    }
  }
}

object Image {
  def image(init: Image ?=> Unit = {}): Image = {
    DslRuntime.build(new Image())(init)
  }

  def src(using i: Image): String = i.srcProperty.get
  def src_=(value: String)(using i: Image): Unit = i.srcProperty.set(Option(value).getOrElse(""))
  def src_=(value: ReadOnlyProperty[String])(using i: Image): Unit = {
    i.addDisposable(value.observe(v => i.srcProperty.set(Option(v).getOrElse(""))))
  }

  def alt(using i: Image): String = i.altProperty.get
  def alt_=(value: String)(using i: Image): Unit = i.altProperty.set(Option(value).getOrElse(""))
  def alt_=(value: ReadOnlyProperty[String])(using i: Image): Unit = {
    i.addDisposable(value.observe(v => i.altProperty.set(Option(v).getOrElse(""))))
  }
}

private final class ImageNode(srcProperty: ReadOnlyProperty[String], altProperty: ReadOnlyProperty[String]) extends Component {
  override def tagName: String = "img"

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-image__img")
    attribute("src", srcProperty)
    attribute("alt", altProperty)
    style {
      display = "block"
      width = "100%"
      height = "100%"
      objectFit = "inherit"
      objectPosition = "inherit"
      borderRadius = "inherit"
    }
  }
}

private object ImageNode {
  def apply(srcProperty: ReadOnlyProperty[String], altProperty: ReadOnlyProperty[String]): ImageNode =
    DslRuntime.build(new ImageNode(srcProperty, altProperty)) {}
}
