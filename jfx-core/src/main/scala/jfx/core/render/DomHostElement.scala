package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable
import scala.collection.mutable
import scala.scalajs.js

final class DomHostElement(val tagName: String, private var _element: dom.Element) extends HostElement {
  private val listeners = mutable.ArrayBuffer.empty[(String, dom.Event => Unit)]
  private val attributes = mutable.Map.empty[String, String]
  private val styles = mutable.Map.empty[String, String]
  private val properties = mutable.Map.empty[String, Any]

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

      // 4. Re-apply attributes
      attributes.foreach { case (name, value) => _element.setAttribute(name, value) }

      // 5. Re-apply styles
      val htmlEl = _element.asInstanceOf[dom.html.Element]
      styles.foreach { case (name, value) => htmlEl.style.setProperty(name, value) }

      // 6. Re-apply DOM properties such as input.value/readOnly after hydration rebinds.
      properties.foreach { case (name, value) => writeProperty(name, value) }
    }
  }

  override def domNode: Option[dom.Node] = Some(_element)
  override def renderHtml(indent: Int): String = _element.outerHTML

  override def setAttribute(name: String, value: String): Unit = {
    attributes.update(name, value)
    _element.setAttribute(name, value)
  }

  override def attribute(name: String): Option[String] = attributes.get(name).orElse(Option(_element.getAttribute(name)))

  override def setProperty(name: String, value: Any): Unit = {
    properties.update(name, value)
    writeProperty(name, value)
  }

  override def property[T](name: String): Option[T] = {
    val value = _element.asInstanceOf[js.Dynamic].selectDynamic(name)
    if (js.isUndefined(value)) properties.get(name).asInstanceOf[Option[T]]
    else Option(value.asInstanceOf[T])
  }

  override def setClassNames(classes: Seq[String]): Unit = {
    val value = classes.mkString(" ")
    if (classes.isEmpty) {
      attributes.remove("class")
      _element.removeAttribute("class")
    } else {
      attributes.update("class", value)
      _element.setAttribute("class", value)
    }
  }

  override def getStyle(name: String): String =
    styles.getOrElse(name, _element.asInstanceOf[dom.html.Element].style.getPropertyValue(name))

  override def setStyle(name: String, value: String): Unit = {
    styles.update(name, value)
    _element.asInstanceOf[dom.html.Element].style.setProperty(name, value)
  }

  private def writeProperty(name: String, value: Any): Unit =
    _element.asInstanceOf[js.Dynamic].updateDynamic(name)(value.asInstanceOf[js.Any])


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
