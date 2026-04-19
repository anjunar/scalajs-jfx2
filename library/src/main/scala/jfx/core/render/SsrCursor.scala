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

  override def position: Option[Int] = index.map(_ => currentIndex)

  override def claimElement(tagName: String): HostElement = {
    val el = new SsrHostElement(tagName)
    if (target.isEmpty && index.isEmpty) {
      rootElements += el
    }
    if (index.isDefined) currentIndex += 1
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
    if (index.isDefined) currentIndex += 1
    t
  }

  override def subCursor(element: HostElement): Cursor = {
    new SsrCursor(Some(element.asInstanceOf[SsrHostElement]))
  }

  def resultHtml(root: Component): String = {
    if (root.isVirtual) {
      rootElements.map(_.renderHtml(0)).mkString("")
    } else {
      root.hostNode.renderHtml(0)
    }
  }
}
