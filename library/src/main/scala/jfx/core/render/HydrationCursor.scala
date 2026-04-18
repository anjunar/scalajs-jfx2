package jfx.core.render

import org.scalajs.dom

final class HydrationCursor private (
  exact: Option[dom.Element],
  parent: Option[dom.Element]
) {

  private var exactClaimed = false
  private var childIndex = 0

  def claim(tagName: String): dom.Element = {
    val claimed =
      exact match {
        case Some(element) if !exactClaimed =>
          exactClaimed = true
          Some(element)
        case _ =>
          parent.flatMap(nextChild)
      }

    claimed match {
      case Some(element) if element.tagName.equalsIgnoreCase(tagName) =>
        element
      case Some(element) =>
        val parentInfo = parent.map(p => s" in <${p.tagName.toLowerCase}>").getOrElse("")
        val siblings = parent.map(p => s" (existing children: ${p.children.map(_.tagName.toLowerCase).mkString(", ")})").getOrElse("")
        dom.console.warn(
          s"JFX2 hydration mismatch: expected <$tagName> but found <${element.tagName.toLowerCase}>$parentInfo.$siblings Creating a fresh node."
        )
        dom.document.createElement(tagName)
      case None =>
        val parentInfo = parent.map(p => s" in <${p.tagName.toLowerCase}>").getOrElse("")
        val siblings = parent.map(p => s" (total children: ${p.children.length})").getOrElse("")
        dom.console.warn(s"JFX2 hydration mismatch: expected <$tagName> but no server node was available$parentInfo$siblings. Creating a fresh node.")
        dom.document.createElement(tagName)
    }
  }

  private def nextChild(parentElement: dom.Element): Option[dom.Element] = {
    val children = parentElement.children
    val child =
      if (childIndex >= children.length) None
      else Option(children.item(childIndex))

    childIndex += 1
    child
  }

}

object HydrationCursor {

  def exact(root: dom.Element): HydrationCursor =
    new HydrationCursor(Some(root), None)

  def children(parent: dom.Element): HydrationCursor =
    new HydrationCursor(None, Some(parent))

}
