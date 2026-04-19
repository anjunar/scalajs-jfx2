package jfx.dsl

import jfx.core.component.Component
import jfx.core.render.HostElement

class StyleProxy(val host: HostElement) {
  def flex: String = "" // Dummy getter for setter syntax
  def flex_=(value: String): Unit = host.setStyle("flex", value)
  
  def minHeight: String = ""
  def minHeight_=(value: String): Unit = host.setStyle("min-height", value)
  
  def height: String = ""
  def height_=(value: String): Unit = host.setStyle("height", value)

  def width: String = ""
  def width_=(value: String): Unit = host.setStyle("width", value)

  def marginTop: String = ""
  def marginTop_=(value: String): Unit = host.setStyle("margin-top", value)

  def display: String = ""
  def display_=(value: String): Unit = host.setStyle("display", value)

  def flexDirection: String = ""
  def flexDirection_=(value: String): Unit = host.setStyle("flex-direction", value)

  def gap: String = ""
  def gap_=(value: String): Unit = host.setStyle("gap", value)

  def padding: String = ""
  def padding_=(value: String): Unit = host.setStyle("padding", value)
}

object StyleDsl {
  def style(init: StyleProxy ?=> Unit)(using c: Component): Unit = {
    val proxy = new StyleProxy(c.host)
    init(using proxy)
  }
}
