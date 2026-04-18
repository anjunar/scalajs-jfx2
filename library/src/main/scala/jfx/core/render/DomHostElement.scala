package jfx.core.render

import jfx.core.state.Disposable
import org.scalajs.dom

final class DomHostElement(
  override val tagName: String,
  val element: dom.Element
) extends HostElement {

  override def domNodeOption: Option[dom.Node] =
    Some(element)

  override def domElementOption: Option[dom.Element] =
    Some(element)

  override def appendChild(child: HostNode): Unit =
    child.domNodeOption.foreach { node =>
      if (node.parentNode != element) {
        element.appendChild(node)
      }
    }

  override def insertChild(index: Int, child: HostNode): Unit =
    child.domNodeOption.foreach { node =>
      if (node.parentNode != element) {
        val boundedIndex = math.max(0, index)
        val referenceNode =
          if (boundedIndex >= element.children.length) null
          else element.children.item(boundedIndex)

        if (referenceNode == null) element.appendChild(node)
        else element.insertBefore(node, referenceNode)
      }
    }

  override def removeChild(child: HostNode): Unit =
    child.domNodeOption.foreach { node =>
      val parent = node.parentNode
      if (parent == element) element.removeChild(node)
      else if (parent != null) parent.removeChild(node)
    }

  override def clearChildren(): Unit =
    element.textContent = ""

  override def setTextContent(value: String): Unit =
    element.textContent = if (value == null) "" else value

  override def textContent: String =
    Option(element.textContent).getOrElse("")

  override def setAttribute(name: String, value: String): Unit =
    element.setAttribute(name, if (value == null) "" else value)

  override def removeAttribute(name: String): Unit =
    element.removeAttribute(name)

  override def attribute(name: String): Option[String] =
    Option(element.getAttribute(name))

  override def setStyleProperty(name: String, value: String): Unit =
    element match {
      case htmlElement: dom.HTMLElement =>
        htmlElement.style.setProperty(name, if (value == null) "" else value)
      case _ =>
        setAttribute("style", s"$name: $value")
    }

  override def removeStyleProperty(name: String): String =
    element match {
      case htmlElement: dom.HTMLElement =>
        val previous = htmlElement.style.getPropertyValue(name)
        htmlElement.style.removeProperty(name)
        previous
      case _ =>
        val previous = attribute("style").getOrElse("")
        removeAttribute("style")
        previous
    }

  override def styleProperty(name: String): String =
    element match {
      case htmlElement: dom.HTMLElement =>
        htmlElement.style.getPropertyValue(name)
      case _ =>
        ""
    }

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = {
    element.addEventListener(name, listener)
    () => element.removeEventListener(name, listener)
  }

  override def html: String =
    element.outerHTML

}
