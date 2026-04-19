package jfx.core.render

import scala.collection.mutable
import scala.compiletime.uninitialized

class SsrCursor(private val target: Option[SsrHostElement] = None) extends Cursor {
  private val rootElements = mutable.ArrayBuffer.empty[SsrHostElement]

  override def claimElement(tagName: String): HostElement = {
    val el = new SsrHostElement(tagName)
    target match {
      case Some(parent) => parent.children += el
      case None => rootElements += el
    }
    el
  }

  override def claimText(initial: String): HostNode = {
    val t = new HostNode {
      def html = HtmlEscaper.text(initial)
      def domNode = None
    }
    target.foreach(_.children += t)
    t
  }

  override def subCursor(element: HostElement): Cursor = {
    new SsrCursor(Some(element.asInstanceOf[SsrHostElement]))
  }

  def resultHtml: String = {
    target.map(_.html).getOrElse(rootElements.map(_.html).mkString(""))
  }
}
