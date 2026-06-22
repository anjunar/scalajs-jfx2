package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.HostElement
import jfx.core.state.ReadOnlyProperty

class StyleProxy(val host: HostElement)

object StyleDsl {
  def style(init: StyleProxy ?=> Unit)(using c: Component): Unit = {
    val proxy = new StyleProxy(c.host)
    init(using proxy)
  }

  private def set(name: String, value: String)(using s: StyleProxy): Unit =
    s.host.setStyle(name, value)

  private def bind(name: String, value: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit =
    c.addDisposable(value.observe(v => s.host.setStyle(name, v)))

  def boxSizing(using s: StyleProxy): String = ""
  def boxSizing_=(v: String)(using s: StyleProxy): Unit = set("box-sizing", v)
  def boxSizing_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("box-sizing", v)

  def width(using s: StyleProxy): String = ""
  def width_=(v: String)(using s: StyleProxy): Unit = set("width", v)
  def width_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("width", v)

  def height(using s: StyleProxy): String = ""
  def height_=(v: String)(using s: StyleProxy): Unit = set("height", v)
  def height_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("height", v)

  def transform(using s: StyleProxy): String = ""
  def transform_=(v: String)(using s: StyleProxy): Unit = set("transform", v)
  def transform_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("transform", v)

  def top(using s: StyleProxy): String = ""
  def top_=(v: String)(using s: StyleProxy): Unit = set("top", v)
  def top_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("top", v)

  def minHeight(using s: StyleProxy): String = ""
  def minHeight_=(v: String)(using s: StyleProxy): Unit = set("min-height", v)
  def minHeight_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("min-height", v)

  def minWidth(using s: StyleProxy): String = ""
  def minWidth_=(v: String)(using s: StyleProxy): Unit = set("min-width", v)
  def minWidth_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("min-width", v)

  def maxHeight(using s: StyleProxy): String = ""
  def maxHeight_=(v: String)(using s: StyleProxy): Unit = set("max-height", v)
  def maxHeight_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("max-height", v)

  def maxWidth(using s: StyleProxy): String = ""
  def maxWidth_=(v: String)(using s: StyleProxy): Unit = set("max-width", v)
  def maxWidth_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("max-width", v)

  def marginTop(using s: StyleProxy): String = ""
  def marginTop_=(v: String)(using s: StyleProxy): Unit = set("margin-top", v)
  def marginTop_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("margin-top", v)

  def marginRight(using s: StyleProxy): String = ""
  def marginRight_=(v: String)(using s: StyleProxy): Unit = set("margin-right", v)
  def marginRight_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("margin-right", v)

  def marginBottom(using s: StyleProxy): String = ""
  def marginBottom_=(v: String)(using s: StyleProxy): Unit = set("margin-bottom", v)
  def marginBottom_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("margin-bottom", v)

  def marginLeft(using s: StyleProxy): String = ""
  def marginLeft_=(v: String)(using s: StyleProxy): Unit = set("margin-left", v)
  def marginLeft_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("margin-left", v)

  def display(using s: StyleProxy): String = ""
  def display_=(v: String)(using s: StyleProxy): Unit = set("display", v)
  def display_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("display", v)

  def flexDirection(using s: StyleProxy): String = ""
  def flexDirection_=(v: String)(using s: StyleProxy): Unit = set("flex-direction", v)
  def flexDirection_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("flex-direction", v)

  def alignItems(using s: StyleProxy): String = ""
  def alignItems_=(v: String)(using s: StyleProxy): Unit = set("align-items", v)
  def alignItems_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("align-items", v)

  def justifyContent(using s: StyleProxy): String = ""
  def justifyContent_=(v: String)(using s: StyleProxy): Unit = set("justify-content", v)
  def justifyContent_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("justify-content", v)

  def flex(using s: StyleProxy): String = ""
  def flex_=(v: String)(using s: StyleProxy): Unit = set("flex", v)
  def flex_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("flex", v)

  def gap(using s: StyleProxy): String = ""
  def gap_=(v: String)(using s: StyleProxy): Unit = set("gap", v)
  def gap_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("gap", v)

  def padding(using s: StyleProxy): String = ""
  def padding_=(v: String)(using s: StyleProxy): Unit = set("padding", v)
  def padding_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("padding", v)

  def background(using s: StyleProxy): String = ""
  def background_=(v: String)(using s: StyleProxy): Unit = set("background", v)
  def background_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("background", v)

  def backgroundColor(using s: StyleProxy): String = ""
  def backgroundColor_=(v: String)(using s: StyleProxy): Unit = set("background-color", v)
  def backgroundColor_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("background-color", v)

  def borderRadius(using s: StyleProxy): String = ""
  def borderRadius_=(v: String)(using s: StyleProxy): Unit = set("border-radius", v)
  def borderRadius_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border-radius", v)

  def border(using s: StyleProxy): String = ""
  def border_=(v: String)(using s: StyleProxy): Unit = set("border", v)
  def border_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border", v)

  def borderBottom(using s: StyleProxy): String = ""
  def borderBottom_=(v: String)(using s: StyleProxy): Unit = set("border-bottom", v)
  def borderBottom_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border-bottom", v)

  def borderTop(using s: StyleProxy): String = ""
  def borderTop_=(v: String)(using s: StyleProxy): Unit = set("border-top", v)
  def borderTop_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border-top", v)

  def borderLeft(using s: StyleProxy): String = ""
  def borderLeft_=(v: String)(using s: StyleProxy): Unit = set("border-left", v)
  def borderLeft_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border-left", v)

  def borderRight(using s: StyleProxy): String = ""
  def borderRight_=(v: String)(using s: StyleProxy): Unit = set("border-right", v)
  def borderRight_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("border-right", v)

  def fontWeight(using s: StyleProxy): String = ""
  def fontWeight_=(v: String)(using s: StyleProxy): Unit = set("font-weight", v)
  def fontWeight_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("font-weight", v)

  def textAlign(using s: StyleProxy): String = ""
  def textAlign_=(v: String)(using s: StyleProxy): Unit = set("text-align", v)
  def textAlign_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("text-align", v)

  def color(using s: StyleProxy): String = ""
  def color_=(v: String)(using s: StyleProxy): Unit = set("color", v)
  def color_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("color", v)

  def fontSize(using s: StyleProxy): String = ""
  def fontSize_=(v: String)(using s: StyleProxy): Unit = set("font-size", v)
  def fontSize_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("font-size", v)

  def opacity(using s: StyleProxy): String = ""
  def opacity_=(v: String)(using s: StyleProxy): Unit = set("opacity", v)
  def opacity_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("opacity", v)

  def position(using s: StyleProxy): String = ""
  def position_=(v: String)(using s: StyleProxy): Unit = set("position", v)
  def position_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("position", v)

  def right(using s: StyleProxy): String = ""
  def right_=(v: String)(using s: StyleProxy): Unit = set("right", v)
  def right_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("right", v)

  def bottom(using s: StyleProxy): String = ""
  def bottom_=(v: String)(using s: StyleProxy): Unit = set("bottom", v)
  def bottom_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("bottom", v)

  def left(using s: StyleProxy): String = ""
  def left_=(v: String)(using s: StyleProxy): Unit = set("left", v)
  def left_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("left", v)

  def zIndex(using s: StyleProxy): String = ""
  def zIndex_=(v: String)(using s: StyleProxy): Unit = set("z-index", v)
  def zIndex_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("z-index", v)

  def overflow(using s: StyleProxy): String = ""
  def overflow_=(v: String)(using s: StyleProxy): Unit = set("overflow", v)
  def overflow_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("overflow", v)

  def overflowY(using s: StyleProxy): String = ""
  def overflowY_=(v: String)(using s: StyleProxy): Unit = set("overflow-y", v)
  def overflowY_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("overflow-y", v)

  def overflowX(using s: StyleProxy): String = ""
  def overflowX_=(v: String)(using s: StyleProxy): Unit = set("overflow-x", v)
  def overflowX_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("overflow-x", v)

  def boxShadow(using s: StyleProxy): String = ""
  def boxShadow_=(v: String)(using s: StyleProxy): Unit = set("box-shadow", v)
  def boxShadow_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("box-shadow", v)

  def objectFit(using s: StyleProxy): String = ""
  def objectFit_=(v: String)(using s: StyleProxy): Unit = set("object-fit", v)
  def objectFit_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("object-fit", v)

  def objectPosition(using s: StyleProxy): String = ""
  def objectPosition_=(v: String)(using s: StyleProxy): Unit = set("object-position", v)
  def objectPosition_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("object-position", v)

  def cursor(using s: StyleProxy): String = ""
  def cursor_=(v: String)(using s: StyleProxy): Unit = set("cursor", v)
  def cursor_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("cursor", v)

  def pointerEvents(using s: StyleProxy): String = ""
  def pointerEvents_=(v: String)(using s: StyleProxy): Unit = set("pointer-events", v)
  def pointerEvents_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("pointer-events", v)

  def flexWrap(using s: StyleProxy): String = ""
  def flexWrap_=(v: String)(using s: StyleProxy): Unit = set("flex-wrap", v)
  def flexWrap_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("flex-wrap", v)

  def transition(using s: StyleProxy): String = ""
  def transition_=(v: String)(using s: StyleProxy): Unit = set("transition", v)
  def transition_=(v: ReadOnlyProperty[String])(using s: StyleProxy, c: Component): Unit = bind("transition", v)
}