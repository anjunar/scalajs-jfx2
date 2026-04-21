package jfx.control

import jfx.core.component.Component
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime

class Image extends Component {
  override def tagName: String = "img"
}

object Image {
  def image(init: Image ?=> Unit = {}): Image = {
    DslRuntime.build(new Image())(init)
  }

  def src(using i: Image): String = i.host.attribute("src").getOrElse("")
  def src_=(value: String)(using i: Image): Unit = i.host.setAttribute("src", value)
  def src_=(value: ReadOnlyProperty[String])(using i: Image): Unit = {
    i.addDisposable(value.observe(v => i.host.setAttribute("src", v)))
  }

  def alt(using i: Image): String = i.host.attribute("alt").getOrElse("")
  def alt_=(value: String)(using i: Image): Unit = i.host.setAttribute("alt", value)
  def alt_=(value: ReadOnlyProperty[String])(using i: Image): Unit = {
    i.addDisposable(value.observe(v => i.host.setAttribute("alt", v)))
  }
}
