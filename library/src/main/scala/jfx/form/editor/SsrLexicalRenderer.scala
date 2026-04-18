package jfx.form.editor

import jfx.core.component.{ElementComponent, NativeComponent, NodeComponent}
import jfx.core.component.ElementComponent.*
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

object SsrLexicalRenderer {

  def render(value: String | Null, placeholder: String = "")(using Scope): NodeComponent[? <: dom.Node] =
    element("div") {
      classes = "jfx-editor-readonly"

      parseState(value) match {
        case Some(root) =>
          val children = childNodes(root)
          if (children.isEmpty) renderPlaceholder(placeholder)
          else children.foreach(renderNode)

        case None =>
          val text = Option(value).map(_.trim).filter(_.nonEmpty)
          text match {
            case Some(content) => renderParagraphText(content)
            case None => renderPlaceholder(placeholder)
          }
      }
    }

  private def renderPlaceholder(value: String)(using Scope): Unit =
    element("p") {
      classes = "jfx-editor-readonly__placeholder"
      text = Option(value).filter(_.nonEmpty).getOrElse("Noch kein Inhalt.")
    }

  private def renderParagraphText(value: String)(using Scope): Unit =
    element("p") {
      classes = "lexical-paragraph"
      text = value
    }

  private def renderNode(node: js.Dynamic)(using Scope): Unit =
    nodeType(node) match {
      case "paragraph" =>
        element("p") {
          classes = "lexical-paragraph"
          renderChildren(node)
        }

      case "heading" =>
        element(safeHeadingTag(stringField(node, "tag").getOrElse("h2"))) {
          classes = "lexical-heading"
          renderChildren(node)
        }

      case "quote" =>
        element("blockquote") {
          classes = "lexical-quote"
          renderChildren(node)
        }

      case "list" =>
        val tag =
          if (stringField(node, "listType").contains("number")) "ol"
          else "ul"

        element(tag) {
          classes = s"lexical-list lexical-list--${stringField(node, "listType").getOrElse("bullet")}"
          renderChildren(node)
        }

      case "listitem" =>
        element("li") {
          classes = "lexical-listitem"
          renderChildren(node)
        }

      case "link" =>
        element("a") {
          classes = "lexical-link"
          stringField(node, "url").foreach(summon[SsrElement].setAttribute("href", _))
          renderChildren(node)
        }

      case "text" =>
        renderText(node)

      case "linebreak" =>
        element("br") {}

      case "tab" =>
        element("span") {
          classes = "lexical-tab"
          text = "\t"
        }

      case "code" =>
        renderCodeBlock(
          code = textContent(node),
          language = stringField(node, "language").orElse(stringField(node, "lang")).getOrElse("")
        )

      case "codemirror" =>
        renderCodeBlock(
          code = stringField(node, "code").getOrElse(""),
          language = stringField(node, "language").getOrElse("")
        )

      case "image" =>
        element("img") {
          classes = "lexical-image"
          stringField(node, "src").foreach(summon[SsrElement].setAttribute("src", _))
          stringField(node, "altText").foreach(summon[SsrElement].setAttribute("alt", _))
          numberField(node, "maxWidth").foreach(width => summon[SsrElement].setAttribute("style", s"max-width: ${width.toInt}px"))
        }

      case _ =>
        val children = childNodes(node)
        if (children.nonEmpty) children.foreach(renderNode)
        else stringField(node, "text").foreach(renderParagraphText)
    }

  private def renderChildren(node: js.Dynamic)(using Scope): Unit =
    childNodes(node).foreach(renderNode)

  private def renderText(node: js.Dynamic)(using Scope): Unit = {
    val value = stringField(node, "text").getOrElse("")
    if (value.isEmpty) return

    val classNames = mutable.ArrayBuffer("lexical-text")
    val format = intField(node, "format").getOrElse(0)

    if ((format & 1) != 0) classNames += "lexical-text-bold"
    if ((format & 2) != 0) classNames += "lexical-text-italic"
    if ((format & 4) != 0) classNames += "lexical-text-strikethrough"
    if ((format & 8) != 0) classNames += "lexical-text-underline"
    if ((format & 16) != 0) classNames += "lexical-text-code"
    if ((format & 32) != 0) classNames += "lexical-text-subscript"
    if ((format & 64) != 0) classNames += "lexical-text-superscript"
    if ((format & 128) != 0) classNames += "lexical-text-highlight"

    stringField(node, "style").filter(_.nonEmpty).foreach { style =>
      classNames += "lexical-text-styled"
    }

    element("span") {
      classes = classNames.toVector
      stringField(node, "style").filter(_.nonEmpty).foreach(summon[SsrElement].setAttribute("style", _))
      text = value
    }
  }

  private def renderCodeBlock(code: String, language: String)(using Scope): Unit =
    element("pre") {
      classes = Vector("jfx-editor-code", Option(language).filter(_.nonEmpty).map(lang => s"language-$lang").getOrElse(""))

      element("code") {
        classes = "jfx-editor-code__content"
        SsrCodeHighlighter.highlight(code, language).foreach { token =>
          element("span") {
            if (token.className.nonEmpty) classes = token.className
            text = token.value
          }
        }
      }
    }

  private def textContent(node: js.Dynamic): String = {
    val builder = new StringBuilder()

    def append(current: js.Dynamic): Unit =
      nodeType(current) match {
        case "text" =>
          builder.append(stringField(current, "text").getOrElse(""))
        case "linebreak" =>
          builder.append('\n')
        case _ =>
          childNodes(current).foreach(append)
      }

    append(node)
    builder.toString()
  }

  private def parseState(value: String | Null): Option[js.Dynamic] =
    Option(value)
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap { raw =>
        try {
          val parsed = js.JSON.parse(raw).asInstanceOf[js.Dynamic]
          dynamicField(parsed, "root")
        } catch {
          case _: Throwable => None
        }
      }

  private def childNodes(node: js.Dynamic): Seq[js.Dynamic] =
    dynamicField(node, "children")
      .map(_.asInstanceOf[js.Array[js.Dynamic]].toSeq)
      .getOrElse(Seq.empty)

  private def nodeType(node: js.Dynamic): String =
    stringField(node, "type").getOrElse("")

  private def stringField(node: js.Dynamic, name: String): Option[String] =
    dynamicField(node, name).flatMap { value =>
      if (js.isUndefined(value) || value == null) None
      else Some(value.asInstanceOf[String])
    }

  private def intField(node: js.Dynamic, name: String): Option[Int] =
    numberField(node, name).map(_.toInt)

  private def numberField(node: js.Dynamic, name: String): Option[Double] =
    dynamicField(node, name).flatMap { value =>
      if (js.isUndefined(value) || value == null) None
      else Some(value.asInstanceOf[Double])
    }

  private def dynamicField(node: js.Dynamic, name: String): Option[js.Dynamic] = {
    val value = node.selectDynamic(name)
    if (js.isUndefined(value) || value == null) None
    else Some(value)
  }

  private def safeHeadingTag(value: String): String =
    value.toLowerCase match {
      case "h1" => "h1"
      case "h2" => "h2"
      case "h3" => "h3"
      case "h4" => "h4"
      case "h5" => "h5"
      case "h6" => "h6"
      case _ => "h2"
    }

  private final class SsrElement(tagName: String) extends NativeComponent[dom.HTMLElement](tagName)

  private def element(tagName: String)(init: SsrElement ?=> Unit)(using Scope): SsrElement =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new SsrElement(tagName)

      DslRuntime.withComponentContext(ComponentContext(Some(component))) {
        given Scope = currentScope
        given SsrElement = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

}
