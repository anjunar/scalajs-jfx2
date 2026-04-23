package jfx.core.render

import org.scalajs.dom
import jfx.core.state.Disposable
import scala.collection.mutable

final class SsrHostElement(val tagName: String) extends HostElement {
  val children = mutable.ArrayBuffer.empty[HostNode]
  private val attributes = mutable.Map.empty[String, String]
  private val styles = mutable.Map.empty[String, String]
  private val properties = mutable.Map.empty[String, Any]

  override def domNode: Option[dom.Node] = None

  override def renderHtml(indent: Int): String = {
    val attrStr = attributes.map { case (k, v) => s" $k=\"${HtmlEscaper.attribute(v)}\"" }.mkString("")
    val styleList = styles.map { case (k, v) => s"$k: $v" }.toSeq
    val styleStr = if (styleList.isEmpty) "" else s" style=\"${styleList.mkString("; ")}\""
    
    val content = children.map(_.renderHtml(0)).mkString("")
    s"<$tagName$attrStr$styleStr>$content</$tagName>"
  }

  override def setAttribute(name: String, value: String): Unit = attributes.update(name, value)
  override def attribute(name: String): Option[String] = attributes.get(name)

  override def setProperty(name: String, value: Any): Unit = {
    properties.update(name, value)
    reflectProperty(name, value)
  }

  override def property[T](name: String): Option[T] =
    properties.get(name).asInstanceOf[Option[T]]
  
  override def setClassNames(classes: Seq[String]): Unit = {
    if (classes.isEmpty) attributes.remove("class")
    else attributes.update("class", classes.mkString(" "))
  }

  override def getStyle(name: String): String = styles(name)

  override def setStyle(name: String, value: String): Unit = styles.update(name, value)

  override def clientHeight: Int = 0

  override def clientWidth: Int = 0

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

  private def reflectProperty(name: String, value: Any): Unit =
    name match {
      case "value" =>
        attributes.update("value", Option(value).map(_.toString).getOrElse(""))
      case "readOnly" =>
        reflectBooleanAttribute("readonly", value)
      case "checked" | "disabled" | "selected" =>
        reflectBooleanAttribute(name.toLowerCase, value)
      case _ =>
    }

  private def reflectBooleanAttribute(name: String, value: Any): Unit =
    value match {
      case true => attributes.update(name, name)
      case _    => attributes.remove(name)
    }
}
