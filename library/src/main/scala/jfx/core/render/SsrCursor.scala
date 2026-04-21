package jfx.core.render

import jfx.core.component.Component
import scala.collection.mutable
import scala.compiletime.uninitialized

class SsrCursor(
  private val target: Option[SsrHostElement] = None,
  private val index: Option[Int] = None
) extends Cursor {
  private val rootElements = mutable.ArrayBuffer.empty[HostNode]
  private var currentIndex = index.getOrElse(0)

  override def position: Option[Int] = Some(currentIndex)

  override def claimElement(tagName: String): HostElement = {
    val el = new SsrHostElement(tagName)
    // Wir fügen hier NICHTS automatisch ein. 
    // Das erledigt DslRuntime über syncChildAddition -> host.insertChild
    if (target.isEmpty && index.isEmpty) {
      rootElements += el
    }
    currentIndex += 1
    el
  }

  override def claimText(initial: String): HostNode = {
    val t = new HostNode {
      override def renderHtml(indent: Int): String = HtmlEscaper.text(initial)
      def domNode = None
    }
    if (target.isEmpty && index.isEmpty) {
      rootElements += t
    }
    currentIndex += 1
    t
  }

  override def subCursor(element: HostElement): Cursor = {
    new SsrCursor(Some(element.asInstanceOf[SsrHostElement]), Some(0))
  }

  def resultHtml(root: Component): String = {
    if (root.isVirtual) {
      rootElements.map(_.renderHtml(0)).mkString("")
    } else {
      root.hostNode.renderHtml(0)
    }
  }
}
