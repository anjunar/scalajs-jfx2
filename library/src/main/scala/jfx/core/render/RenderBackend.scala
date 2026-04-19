package jfx.core.render

import org.scalajs.dom
import scala.collection.mutable

trait RenderBackend {
  def isServer: Boolean
  def nextCursor(parent: Option[HostElement]): Cursor
  def insertionCursor(parent: HostElement, index: Int): Cursor
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
  def position: Option[Int] = None
}

object BrowserRenderBackend extends RenderBackend {
  override def isServer: Boolean = false
  override def nextCursor(parent: Option[HostElement]): Cursor = {
    new DomCreationCursor(parent.flatMap(_.domNode).collect { case e: dom.Element => e })
  }
  override def insertionCursor(parent: HostElement, index: Int): Cursor = {
    new DomInsertionCursor(parent, index)
  }
}

private class DomCreationCursor(parent: Option[dom.Element]) extends Cursor {
  override def claimElement(tagName: String): HostElement = {
    val el = dom.document.createElement(tagName)
    new DomHostElement(tagName, el)
  }
  
  override def claimText(initial: String): HostNode = {
    val t = dom.document.createTextNode(initial)
    new HostNode {
      def html = initial
      def domNode = Some(t)
    }
  }
  
  override def subCursor(element: HostElement): Cursor = {
    new DomCreationCursor(element.domNode.collect { case e: dom.Element => e })
  }
}

private class DomInsertionCursor(parent: HostElement, index: Int) extends Cursor {
  private var currentIndex = index
  override def position: Option[Int] = Some(currentIndex)
  
  override def claimElement(tagName: String): HostElement = {
    val el = dom.document.createElement(tagName)
    val host = new DomHostElement(tagName, el)
    currentIndex += 1
    host
  }
  
  override def claimText(initial: String): HostNode = {
    val t = dom.document.createTextNode(initial)
    val host = new HostNode {
      def html = initial
      def domNode = Some(t)
    }
    currentIndex += 1
    host
  }
  
  override def subCursor(element: HostElement): Cursor = {
    new DomCreationCursor(element.domNode.collect { case e: dom.Element => e })
  }
}

final class HydrationRenderBackend private (cursor: HydrationCursor) extends RenderBackend {
  override def isServer: Boolean = false
  override def nextCursor(parent: Option[HostElement]): Cursor = cursor
  override def insertionCursor(parent: HostElement, index: Int): Cursor = {
    // If we are still hydrating, we might need a mix or just use the dom insertion if we are adding new nodes
    new DomInsertionCursor(parent, index)
  }
}

object HydrationRenderBackend {
  def root(root: dom.Element): HydrationRenderBackend = 
    new HydrationRenderBackend(new HydrationCursor(root))
}
