package jfx.core.render

import org.scalajs.dom
import scala.collection.mutable

/**
 * Traverses existing DOM during hydration.
 */
class HydrationCursor(val root: dom.Node) extends Cursor {
  private val childNodes = root.childNodes
  private var index = 0

  private def skipWhitespaces(): Unit = {
    while (index < childNodes.length && 
           childNodes.item(index).nodeType == dom.Node.TEXT_NODE && 
           childNodes.item(index).textContent.trim.isEmpty) {
      index += 1
    }
  }

  override def claimElement(tagName: String): HostElement = {
    skipWhitespaces()
    if (index >= childNodes.length) {
      throw new IllegalStateException(s"Hydration failed: expected <$tagName> but no more nodes found in parent <${root.nodeName}>")
    }
    
    val node = childNodes.item(index)
    if (node.nodeType != dom.Node.ELEMENT_NODE) {
      throw new IllegalStateException(s"Hydration failed: expected <$tagName> but found node type ${node.nodeType}")
    }
    
    val el = node.asInstanceOf[dom.Element]
    if (el.tagName.toLowerCase != tagName.toLowerCase) {
      throw new IllegalStateException(s"Hydration failed: expected <$tagName> but found <${el.tagName}>")
    }
    
    index += 1
    new DomHostElement(tagName, el)
  }

  override def claimText(initial: String): HostNode = {
    // For text, we don't skip whitespaces as they might be the text itself
    if (index >= childNodes.length) throw new IllegalStateException(s"Hydration failed: expected text but no more nodes found")
    
    val t = childNodes.item(index)
    if (t.nodeType != dom.Node.TEXT_NODE) {
       // If we expect text but find an element, it might be due to whitespace trimming in SSR vs DOM
       // Try skipping whitespaces if this is just a whitespace node
       if (t.textContent.trim.isEmpty) {
         index += 1
         return claimText(initial)
       }
       throw new IllegalStateException(s"Hydration failed: expected text node but found type ${t.nodeType}")
    }
    
    index += 1
    new HostNode {
      def html = t.textContent
      def domNode = Some(t)
    }
  }

  override def subCursor(element: HostElement): Cursor = {
    new HydrationCursor(element.domNode.get)
  }
}
