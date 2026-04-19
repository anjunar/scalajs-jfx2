package jfx.core.render

import org.scalajs.dom
import scala.collection.mutable

/**
 * Traverses existing DOM during hydration.
 * Self-healing mode: repairs DOM on the fly to match the component tree.
 */
class HydrationCursor(val root: dom.Node, startIndex: Int = 0) extends Cursor {
  private val childNodes = root.childNodes
  private var index = startIndex

  override def position: Option[Int] = Some(index)

  private def skipWhitespaces(): Unit = {
    while (index < childNodes.length) {
      val n = childNodes.item(index)
      if (n.nodeType == dom.Node.TEXT_NODE && n.textContent.trim.isEmpty) {
        root.removeChild(n)
      } else {
        return
      }
    }
  }

  override def claimElement(tagName: String): HostElement = {
    skipWhitespaces()
    val ctx = s"Parent: <${root.nodeName.toLowerCase}>, Index: $index"
    val existingNode = if (index < childNodes.length) Some(childNodes.item(index)) else None
    
    existingNode match {
      case Some(node) if node.nodeType == dom.Node.ELEMENT_NODE && 
                         node.asInstanceOf[dom.Element].tagName.toLowerCase == tagName.toLowerCase =>
        val el = node.asInstanceOf[dom.Element]
        dom.console.log(s"Hydrated <$tagName> at $ctx")
        index += 1
        new DomHostElement(tagName, el)
        
      case _ =>
        val msg = s"Hydration Mismatch: Expected <$tagName> but ${existingNode.map(n => s"found node type ${n.nodeType}").getOrElse("no more nodes found")}. $ctx"
        dom.console.warn(msg)
        val el = dom.document.createElement(tagName)
        // Repair DOM by inserting the missing element
        if (index < childNodes.length) root.insertBefore(el, childNodes.item(index))
        else root.appendChild(el)
        index += 1
        new DomHostElement(tagName, el)
    }
  }

  override def claimText(initial: String): HostNode = {
    skipWhitespaces()
    val ctx = s"Parent: <${root.nodeName.toLowerCase}>, Index: $index"
    val existingNode = if (index < childNodes.length) Some(childNodes.item(index)) else None
    
    existingNode match {
      case Some(node) if node.nodeType == dom.Node.TEXT_NODE =>
        val content = node.textContent.replace("\n", "\\n")
        val displayContent = if (content.length > 20) content.substring(0, 20) + "..." else content
        dom.console.log(s"Hydrated text('$displayContent') at $ctx")
        index += 1
        new HostNode {
          override def renderHtml(indent: Int): String = node.textContent
          def domNode = Some(node)
        }
        
      case _ =>
        val expected = if (initial.length > 20) initial.substring(0, 20) + "..." else initial
        val msg = s"Hydration Mismatch: Expected text('$expected') but ${existingNode.map(n => s"found node type ${n.nodeType}").getOrElse("no more nodes found")}. $ctx"
        dom.console.warn(msg)
        val t = dom.document.createTextNode(initial)
        // Repair DOM by inserting the missing text node
        if (index < childNodes.length) root.insertBefore(t, childNodes.item(index))
        else root.appendChild(t)
        index += 1
        new HostNode {
          override def renderHtml(indent: Int): String = initial
          def domNode = Some(t)
        }
    }
  }

  override def subCursor(element: HostElement): Cursor = {
    new HydrationCursor(element.domNode.get)
  }
}
