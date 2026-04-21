package jfx.core.render

import org.scalajs.dom

/**
 * Traverses and binds existing DOM during hydration.
 * Provides detailed tracing and self-healing fallback logic.
 */
class HydrationCursor(val root: dom.Node, startIndex: Int = 0) extends Cursor {
  private val childNodes = root.childNodes
  private var index = startIndex

  override def position: Option[Int] = Some(index)

  private def skipWhitespaces(): Unit = {
    while (index < childNodes.length && 
           childNodes.item(index).nodeType == dom.Node.TEXT_NODE && 
           childNodes.item(index).textContent.trim.isEmpty) {
      index += 1
    }
  }

  override def claimElement(tagName: String): HostElement = {
    skipWhitespaces()
    val ctx = s"Parent: <${root.nodeName.toLowerCase}>, Index: $index"
    val existing = if (index < childNodes.length) Some(childNodes.item(index)) else None
    
    val matched = existing.collect {
      case el: dom.Element if el.tagName.toLowerCase == tagName.toLowerCase => el
    }

    matched match {
      case Some(el) =>
        dom.console.log(s"Hydrated <$tagName> at $ctx")
        index += 1
        new DomHostElement(tagName, el)
        
      case None =>
        dom.console.warn(s"Hydration Mismatch: Expected <$tagName> but found ${existing.map(_.nodeName).getOrElse("nothing")}. $ctx")
        val el = dom.document.createElement(tagName)
        if (index < childNodes.length) root.insertBefore(el, childNodes.item(index))
        else root.appendChild(el)
        index += 1
        new DomHostElement(tagName, el)
    }
  }

  override def claimText(initial: String): HostNode = {
    skipWhitespaces()
    val ctx = s"Parent: <${root.nodeName.toLowerCase}>, Index: $index"
    val existing = if (index < childNodes.length) Some(childNodes.item(index)) else None
    
    val matched = existing.collect {
      case t: dom.Text => t
    }

    matched match {
      case Some(t) =>
        if (t.textContent != initial) {
          dom.console.warn(s"Hydration Text Mismatch: Expected '$initial' but found '${t.textContent}'. $ctx")
          t.textContent = initial
        }
        val display = if (initial.length > 20) initial.substring(0, 20) + "..." else initial
        dom.console.log(s"Hydrated text('$display') at $ctx")
        index += 1
        new HostNode {
          override def renderHtml(indent: Int): String = t.textContent
          def domNode = Some(t)
        }
        
      case None =>
        dom.console.warn(s"Hydration Mismatch: Expected text('$initial') but found ${existing.map(_.nodeName).getOrElse("nothing")}. $ctx")
        val t = dom.document.createTextNode(initial)
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
    element.domNode.map(new HydrationCursor(_)).getOrElse(
      new HydrationCursor(dom.document.createElement("div"))
    )
  }

  def pruneRemaining(): Unit = {
    while (index < childNodes.length) {
      root.removeChild(childNodes.item(index))
    }
  }
}
