package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable
import scala.collection.mutable

final class SsrHostElement(val tagName: String) extends HostElement {
  val children = mutable.ArrayBuffer.empty[HostNode]
  val attributes = mutable.Map.empty[String, String]
  val styles = mutable.Map.empty[String, String]
  var text = ""

  override def html: String = {
    val attrStr = attributes.map { case (k, v) => s" $k=\"${HtmlEscaper.attribute(v)}\"" }.mkString("")
    val styleList = styles.map { case (k, v) => s"$k: $v" }.toSeq
    val styleStr = if (styleList.isEmpty) "" else s" style=\"${styleList.mkString("; ")}\""
    
    val content = if (text.nonEmpty) HtmlEscaper.text(text) else children.map(_.html).mkString("")
    s"<$tagName$attrStr$styleStr>$content</$tagName>"
  }

  override def domNode: Option[dom.Node] = None

  override def setAttribute(name: String, value: String): Unit = attributes.update(name, value)
  override def attribute(name: String): Option[String] = attributes.get(name)
  
  override def setClassNames(classes: Seq[String]): Unit = {
    if (classes.isEmpty) attributes.remove("class")
    else attributes.update("class", classes.mkString(" "))
  }

  override def setText(t: String): Unit = text = t

  override def setStyle(name: String, value: String): Unit = styles.update(name, value)

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = () => ()

  override def clearChildren(): Unit = children.clear()

  override def insertChild(index: Int, child: HostNode): Unit = {
    if (index < 0 || index >= children.length) {
      children += child
    } else {
      children.insert(index, child)
    }
  }

  override def removeChild(child: HostNode): Unit = {
    children -= child
  }
}
