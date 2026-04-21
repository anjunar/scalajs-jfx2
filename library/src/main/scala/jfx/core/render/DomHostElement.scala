package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable
import scala.collection.mutable

final class DomHostElement(val tagName: String, private var _element: dom.Element) extends HostElement {
  private val listeners = mutable.ArrayBuffer.empty[(String, dom.Event => Unit)]

  def element: dom.Element = _element

  /**
   * Updates the underlying DOM element. 
   * Used during rehydration to move a component from a dry-run node to a real DOM node.
   */
  def updateElement(newElement: dom.Element): Unit = {
    if (_element != newElement) {
      // 1. Remove listeners from old element
      listeners.foreach { case (name, listener) => _element.removeEventListener(name, listener) }
      
      // 2. Update element
      _element = newElement
      
      // 3. Re-attach listeners to new element
      listeners.foreach { case (name, listener) => _element.addEventListener(name, listener) }
    }
  }

  override def domNode: Option[dom.Node] = Some(_element)
  override def renderHtml(indent: Int): String = _element.outerHTML

  override def setAttribute(name: String, value: String): Unit = _element.setAttribute(name, value)
  override def attribute(name: String): Option[String] = Option(_element.getAttribute(name))
  
  override def setClassNames(classes: Seq[String]): Unit = {
    if (classes.isEmpty) _element.removeAttribute("class")
    else _element.setAttribute("class", classes.mkString(" "))
  }

  override def getStyle(name: String): String =
    _element.asInstanceOf[dom.html.Element].style.getPropertyValue(name)
    
  override def setStyle(name: String, value: String): Unit = 
    _element.asInstanceOf[dom.html.Element].style.setProperty(name, value)

  override def clientHeight: Int = _element.clientHeight
  override def clientWidth: Int = _element.clientWidth

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = {
    _element.addEventListener(name, listener)
    val entry = (name, listener)
    listeners += entry
    () => {
      _element.removeEventListener(name, listener)
      listeners -= entry
    }
  }

  override def clearChildren(): Unit = {
    while (_element.firstChild != null) _element.removeChild(_element.firstChild)
  }

  override def insertChild(index: Int, child: HostNode): Unit = {
    val node = child.domNode.getOrElse(throw new IllegalArgumentException("Cannot insert HostNode without DOM node"))
    if (index >= _element.childNodes.length) {
      _element.appendChild(node)
    } else {
      _element.insertBefore(node, _element.childNodes.item(index))
    }
  }

  override def removeChild(child: HostNode): Unit = {
    child.domNode.foreach(_element.removeChild)
  }
}
