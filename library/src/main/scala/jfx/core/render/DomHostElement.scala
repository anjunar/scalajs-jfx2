package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable

final class DomHostElement(val tagName: String, val element: dom.Element) extends HostElement {
  override def domNode: Option[dom.Node] = Some(element)
  override def html: String = element.outerHTML

  override def setAttribute(name: String, value: String): Unit = element.setAttribute(name, value)
  override def attribute(name: String): Option[String] = Option(element.getAttribute(name))
  
  override def setClassNames(classes: Seq[String]): Unit = {
    if (classes.isEmpty) element.removeAttribute("class")
    else element.setAttribute("class", classes.mkString(" "))
  }

  override def setText(text: String): Unit = element.textContent = text

  override def setStyle(name: String, value: String): Unit = 
    element.asInstanceOf[dom.html.Element].style.setProperty(name, value)

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = {
    element.addEventListener(name, listener)
    () => element.removeEventListener(name, listener)
  }

  override def clearChildren(): Unit = {
    while (element.firstChild != null) element.removeChild(element.firstChild)
  }

  override def insertChild(index: Int, child: HostNode): Unit = {
    val node = child.domNode.getOrElse(throw new IllegalArgumentException("Cannot insert HostNode without DOM node"))
    if (index >= element.childNodes.length) {
      element.appendChild(node)
    } else {
      element.insertBefore(node, element.childNodes.item(index))
    }
  }

  override def removeChild(child: HostNode): Unit = {
    child.domNode.foreach(element.removeChild)
  }
}
