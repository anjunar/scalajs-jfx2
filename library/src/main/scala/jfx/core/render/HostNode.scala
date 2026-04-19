package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable

trait HostNode {
  def html: String
  def domNode: Option[dom.Node]
}

trait HostElement extends HostNode {
  def tagName: String
  def setAttribute(name: String, value: String): Unit
  def attribute(name: String): Option[String]
  def setClassNames(classes: Seq[String]): Unit
  def setText(text: String): Unit
  def setStyle(name: String, value: String): Unit
  def addEventListener(name: String, listener: dom.Event => Unit): Disposable
  def clearChildren(): Unit
}
