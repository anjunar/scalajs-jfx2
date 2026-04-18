package jfx.core.render

import jfx.core.state.Disposable
import org.scalajs.dom

trait HostNode {

  def domNodeOption: Option[dom.Node]

  def html: String

}

trait HostElement extends HostNode {

  def tagName: String

  def domElementOption: Option[dom.Element]

  def appendChild(child: HostNode): Unit

  def insertChild(index: Int, child: HostNode): Unit

  def removeChild(child: HostNode): Unit

  def clearChildren(): Unit

  def setTextContent(value: String): Unit

  def textContent: String

  def setAttribute(name: String, value: String): Unit

  def removeAttribute(name: String): Unit

  def attribute(name: String): Option[String]

  def setBooleanAttribute(name: String, enabled: Boolean): Unit =
    if (enabled) setAttribute(name, "")
    else removeAttribute(name)

  def setClassNames(names: Seq[String]): Unit =
    if (names.isEmpty) removeAttribute("class")
    else setAttribute("class", names.mkString(" "))

  def setStyleProperty(name: String, value: String): Unit

  def removeStyleProperty(name: String): String

  def styleProperty(name: String): String

  def addEventListener(name: String, listener: dom.Event => Unit): Disposable

}
