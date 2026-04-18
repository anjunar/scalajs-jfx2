package jfx.core.render

import jfx.core.state.Disposable
import org.scalajs.dom

import scala.collection.mutable

final class SsrHostElement(override val tagName: String) extends HostElement {

  private val attributes = mutable.LinkedHashMap.empty[String, String]
  private val styles = mutable.LinkedHashMap.empty[String, String]
  private val children = mutable.ArrayBuffer.empty[HostNode]
  private var currentTextContent: Option[String] = None

  override def domNodeOption: Option[dom.Node] =
    None

  override def domElementOption: Option[dom.Element] =
    None

  override def appendChild(child: HostNode): Unit =
    children += child

  override def insertChild(index: Int, child: HostNode): Unit = {
    val boundedIndex = math.max(0, math.min(index, children.length))
    children.insert(boundedIndex, child)
  }

  override def removeChild(child: HostNode): Unit =
    children -= child

  override def clearChildren(): Unit = {
    currentTextContent = None
    children.clear()
  }

  override def setTextContent(value: String): Unit = {
    currentTextContent = Some(if (value == null) "" else value)
    children.clear()
  }

  override def textContent: String =
    currentTextContent.getOrElse("")

  override def setAttribute(name: String, value: String): Unit =
    attributes.update(name, if (value == null) "" else value)

  override def removeAttribute(name: String): Unit =
    attributes.remove(name)

  override def attribute(name: String): Option[String] =
    attributes.get(name)

  override def setStyleProperty(name: String, value: String): Unit = {
    if (value == null || value.isEmpty) styles.remove(name)
    else styles.update(name, value)
    syncStyleAttribute()
  }

  override def removeStyleProperty(name: String): String = {
    val previous = styles.remove(name).getOrElse("")
    syncStyleAttribute()
    previous
  }

  override def styleProperty(name: String): String =
    styles.getOrElse(name, "")

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable =
    () => ()

  override def html: String = {
    val normalizedTagName = tagName.toLowerCase()
    val attrs = renderAttributes()

    if (SsrHostElement.voidTags.contains(normalizedTagName)) {
      s"<$normalizedTagName$attrs>"
    } else {
      s"<$normalizedTagName$attrs>${renderChildren}</$normalizedTagName>"
    }
  }

  private def renderChildren: String = {
    val text = currentTextContent.map(HtmlEscaper.text).getOrElse("")
    text + children.map(_.html).mkString
  }

  private def renderAttributes(): String =
    attributes.iterator
      .map {
        case (name, "") => s" $name"
        case (name, value) => s""" $name="${HtmlEscaper.attribute(value)}""""
      }
      .mkString

  private def syncStyleAttribute(): Unit = {
    if (styles.isEmpty) {
      attributes.remove("style")
    } else {
      val value = styles.iterator.map { case (name, value) => s"$name: $value" }.mkString("; ")
      attributes.update("style", value)
    }
  }

}

object SsrHostElement {

  private val voidTags: Set[String] =
    Set(
      "area",
      "base",
      "br",
      "col",
      "embed",
      "hr",
      "img",
      "input",
      "link",
      "meta",
      "param",
      "source",
      "track",
      "wbr"
    )

}
