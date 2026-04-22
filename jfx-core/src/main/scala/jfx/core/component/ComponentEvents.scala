package jfx.core.component

import org.scalajs.dom

trait ComponentEvents extends ComponentCore {
  def onClickHandler(handler: dom.MouseEvent => Unit): Unit = {
    addDisposable(host.addEventListener("click", e => {
      handler(e.asInstanceOf[dom.MouseEvent])
    }))
  }
}
