package jfx.core.render

import org.scalajs.dom
import scala.collection.mutable

trait RenderBackend {
  def isServer: Boolean
  def nextCursor(parent: Option[HostElement]): Cursor
}

object RenderBackend {
  private val stack = mutable.ArrayBuffer.empty[RenderBackend]
  def current: RenderBackend = stack.lastOption.getOrElse(BrowserRenderBackend)
  def withBackend[A](backend: RenderBackend)(block: => A): A = {
    stack += backend
    try block finally stack.remove(stack.length - 1)
  }
}

trait Cursor {
  def claimElement(tagName: String): HostElement
  def claimText(initial: String): HostNode
  def subCursor(element: HostElement): Cursor
}

object BrowserRenderBackend extends RenderBackend {
  override def isServer: Boolean = false
  override def nextCursor(parent: Option[HostElement]): Cursor = {
    new DomCreationCursor(parent.flatMap(_.domNode).collect { case e: dom.Element => e })
  }
}

private class DomCreationCursor(parent: Option[dom.Element]) extends Cursor {
  override def claimElement(tagName: String): HostElement = {
    val el = dom.document.createElement(tagName)
    parent.foreach(_.appendChild(el))
    new DomHostElement(tagName, el)
  }
  
  override def claimText(initial: String): HostNode = {
    val t = dom.document.createTextNode(initial)
    parent.foreach(_.appendChild(t))
    new HostNode {
      def html = initial
      def domNode = Some(t)
    }
  }
  
  override def subCursor(element: HostElement): Cursor = {
    new DomCreationCursor(element.domNode.collect { case e: dom.Element => e })
  }
}

final class HydrationRenderBackend private (cursor: HydrationCursor) extends RenderBackend {
  override def isServer: Boolean = false
  override def nextCursor(parent: Option[HostElement]): Cursor = cursor
}

object HydrationRenderBackend {
  def root(root: dom.Element): HydrationRenderBackend = 
    new HydrationRenderBackend(new HydrationCursor(root))
}
