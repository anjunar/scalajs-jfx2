package jfx.core.render

import jfx.core.component.Component
import scala.collection.mutable
import scala.compiletime.uninitialized

class SsrCursor(
  private val target: Option[SsrHostElement] = None,
  private val index: Option[Int] = None
) extends Cursor {
  private var currentIndex = index.getOrElse(0)

  override def position: Option[Int] = index.map(_ => currentIndex)

  override def claimElement(tagName: String): HostElement = {
    val el = new SsrHostElement(tagName)
    val res = el
    if (index.isDefined) currentIndex += 1
    res
  }

  override def claimText(initial: String): HostNode = {
    val t = new HostNode {
      def html = HtmlEscaper.text(initial)
      def domNode = None
    }
    if (index.isDefined) currentIndex += 1
    t
  }

  override def subCursor(element: HostElement): Cursor = {
    new SsrCursor(Some(element.asInstanceOf[SsrHostElement]))
  }

  def resultHtml(root: Component): String = {
    root.hostNode.html
  }
}
