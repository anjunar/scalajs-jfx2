package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.HostElement

class StyleProxy(val host: HostElement)

object StyleDsl {
  def style(init: StyleProxy ?=> Unit)(using c: Component): Unit = {
    val proxy = new StyleProxy(c.host)
    init(using proxy)
  }

  // DSL Properties
  def flex(using s: StyleProxy): String = ""
  def flex_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("flex", v)

  def height(using s: StyleProxy): String = ""
  def height_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("height", v)

  def width(using s: StyleProxy): String = ""
  def width_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("width", v)

  def minHeight(using s: StyleProxy): String = ""
  def minHeight_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("min-height", v)

  def minWidth(using s: StyleProxy): String = ""
  def minWidth_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("min-width", v)

  def maxHeight(using s: StyleProxy): String = ""
  def maxHeight_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("max-height", v)

  def maxWidth(using s: StyleProxy): String = ""
  def maxWidth_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("max-width", v)

  def marginTop(using s: StyleProxy): String = ""
  def marginTop_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("margin-top", v)

  def marginRight(using s: StyleProxy): String = ""
  def marginRight_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("margin-right", v)

  def marginBottom(using s: StyleProxy): String = ""
  def marginBottom_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("margin-bottom", v)

  def marginLeft(using s: StyleProxy): String = ""
  def marginLeft_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("margin-left", v)

  def display(using s: StyleProxy): String = ""
  def display_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("display", v)

  def flexDirection(using s: StyleProxy): String = ""
  def flexDirection_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("flex-direction", v)

  def alignItems(using s: StyleProxy): String = ""
  def alignItems_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("align-items", v)

  def justifyContent(using s: StyleProxy): String = ""
  def justifyContent_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("justify-content", v)

  def gap(using s: StyleProxy): String = ""
  def gap_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("gap", v)

  def padding(using s: StyleProxy): String = ""
  def padding_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("padding", v)

  def position(using s: StyleProxy): String = ""
  def position_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("position", v)

  def top(using s: StyleProxy): String = ""
  def top_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("top", v)

  def right(using s: StyleProxy): String = ""
  def right_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("right", v)

  def bottom(using s: StyleProxy): String = ""
  def bottom_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("bottom", v)

  def left(using s: StyleProxy): String = ""
  def left_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("left", v)

  def zIndex(using s: StyleProxy): String = ""
  def zIndex_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("z-index", v)

  def overflow(using s: StyleProxy): String = ""
  def overflow_=(v: String)(using s: StyleProxy): Unit = s.host.setStyle("overflow", v)
}
