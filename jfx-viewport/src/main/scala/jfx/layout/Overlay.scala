package jfx.layout

import jfx.core.component.Component
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.compiletime.uninitialized

class Overlay(
  val widthPx: Option[Double] = None,
  val init: Overlay ?=> Unit
) extends Component {
  override def tagName: String = "" 
  private var conf: Viewport.OverlayConf = uninitialized
  
  private val _effectiveWidthProperty = Property(0.0)
  def effectiveWidthProperty: ReadOnlyProperty[Double] = _effectiveWidthProperty

  override def bind(cursor: jfx.core.render.Cursor): Unit = {
    super.bind(cursor)
    
    val anchorElement = parent.map(_.host.domNode.get.asInstanceOf[dom.HTMLElement])
      .getOrElse(throw new IllegalStateException("Overlay must be placed inside a physical component to have an anchor."))

    val rect = anchorElement.getBoundingClientRect()
    val w = widthPx.getOrElse(rect.width)
    _effectiveWidthProperty.set(w)

    conf = new Viewport.OverlayConf(
      anchor = anchorElement,
      content = () => {
        jfx.core.component.Box.box("div") {
          init(using this)
        }
      },
      widthPx = Some(w),
      offsetYPx = 4.0
    )

    Viewport.addOverlay(conf)
  }

  override def dispose(): Unit = {
    if (conf != null) Viewport.closeOverlay(conf)
    super.dispose()
  }
}

object Overlay {
  def overlay(init: Overlay ?=> Unit): Overlay = {
    DslRuntime.build(new Overlay(None, init)) { }
  }

  def overlay(widthPx: Double)(init: Overlay ?=> Unit): Overlay = {
    DslRuntime.build(new Overlay(Some(widthPx), init)) { }
  }

  def overlay(widthPx: Option[Double])(init: Overlay ?=> Unit): Overlay = {
    DslRuntime.build(new Overlay(widthPx, init)) { }
  }
  
  def effectiveWidth(using o: Overlay): ReadOnlyProperty[Double] = o.effectiveWidthProperty
}
