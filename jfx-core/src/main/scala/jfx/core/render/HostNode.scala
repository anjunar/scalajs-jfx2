package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable

trait HostNode {
  def html: String = renderHtml(0)
  def renderHtml(indent: Int): String
  def domNode: Option[dom.Node]
}

trait HostElement extends HostNode {
  def tagName: String
  def setAttribute(name: String, value: String): Unit
  def attribute(name: String): Option[String]
  def setClassNames(classes: Seq[String]): Unit
  def getStyle(name: String): String
  def setStyle(name: String, value: String): Unit
  def clientHeight : Int
  def clientWidth : Int
  def addEventListener(name: String, listener: dom.Event => Unit): Disposable
  def clearChildren(): Unit
  def insertChild(index: Int, child: HostNode): Unit
  def removeChild(child: HostNode): Unit
}
