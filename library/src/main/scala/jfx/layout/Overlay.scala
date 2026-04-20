package jfx.layout

import jfx.core.component.Component
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.compiletime.uninitialized

class Overlay(val init: Component ?=> Unit) extends Component {
  override def tagName: String = "" 
  private var conf: Viewport.OverlayConf = uninitialized

  override def bind(cursor: jfx.core.render.Cursor): Unit = {
    super.bind(cursor)
    
    // Wir nutzen die .host Methode, die bei virtuellen Parents automatisch nach oben delegiert
    val anchorElement = parent.map(_.host.domNode.get.asInstanceOf[dom.HTMLElement])
      .getOrElse(throw new IllegalStateException("Overlay must be placed inside a physical component to have an anchor."))

    conf = new Viewport.OverlayConf(
      anchor = anchorElement,
      content = () => {
        jfx.core.component.Box.box("div") {
          init
        }
      },
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
  def overlay(init: Component ?=> Unit): Overlay = {
    DslRuntime.build(new Overlay(init)) { }
  }
}
