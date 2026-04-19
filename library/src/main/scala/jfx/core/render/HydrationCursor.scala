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

  private def nextNodesContext: String = {
    val remaining = (index until math.min(index + 3, childNodes.length)).map { i =>
      val n = childNodes.item(i)
      val name = if (n.nodeType == dom.Node.ELEMENT_NODE) s"<${n.asInstanceOf[dom.Element].tagName.toLowerCase}>" else s"type:${n.nodeType}"
      s"[$i]$name"
    }.mkString(", ")
    if (remaining.isEmpty) "none" else remaining
  }

  override def claimElement(tagName: String): HostElement = {
    skipWhitespaces()
    if (index >= childNodes.length) {
      throw new IllegalStateException(
        s"Hydration failed: expected <$tagName> but no more nodes found.\n" +
        s"  Parent: <${root.nodeName.toLowerCase}>\n" +
        s"  Index: $index, Total children: ${childNodes.length}\n" +
        s"  Context: $nextNodesContext"
      )
    }
    
    val node = childNodes.item(index)
    if (node.nodeType != dom.Node.ELEMENT_NODE) {
      throw new IllegalStateException(
        s"Hydration failed: expected <$tagName> but found node type ${node.nodeType}.\n" +
        s"  Parent: <${root.nodeName.toLowerCase}>\n" +
        s"  Context: $nextNodesContext"
      )
    }
    
    val el = node.asInstanceOf[dom.Element]
    if (el.tagName.toLowerCase != tagName.toLowerCase) {
      throw new IllegalStateException(
        s"Hydration failed: expected <$tagName> but found <${el.tagName.toLowerCase}>.\n" +
        s"  Parent: <${root.nodeName.toLowerCase}>\n" +
        s"  Index: $index\n" +
        s"  Context: $nextNodesContext"
      )
    }
    
    index += 1
    new DomHostElement(tagName, el)
  }

  override def claimText(initial: String): HostNode = {
    if (index >= childNodes.length) {
      throw new IllegalStateException(
        s"Hydration failed: expected text but no more nodes found.\n" +
        s"  Parent: <${root.nodeName.toLowerCase}>\n" +
        s"  Context: $nextNodesContext"
      )
    }
    
    val t = childNodes.item(index)
    if (t.nodeType != dom.Node.TEXT_NODE) {
       if (t.textContent.trim.isEmpty) {
         index += 1
         return claimText(initial)
       }
       throw new IllegalStateException(
         s"Hydration failed: expected text node but found type ${t.nodeType} (<${t.asInstanceOf[dom.Element].tagName.toLowerCase}>).\n" +
         s"  Parent: <${root.nodeName.toLowerCase}>\n" +
         s"  Context: $nextNodesContext"
       )
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
