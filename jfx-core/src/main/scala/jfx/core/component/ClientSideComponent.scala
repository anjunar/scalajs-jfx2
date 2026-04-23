package jfx.core.component

import jfx.dsl.DslRuntime
import org.scalajs.dom

import scala.scalajs.js

final class ClientSideSsrContent private (val root: Option[dom.Element]) {
  def querySelector(selector: String): Option[dom.Element] =
    root.flatMap(element => Option(element.querySelector(selector)))

  def clear(): Unit =
    root.foreach { element =>
      while (element.firstChild != null) {
        element.removeChild(element.firstChild)
      }
    }
}

object ClientSideSsrContent {
  val empty: ClientSideSsrContent =
    new ClientSideSsrContent(None)

  private[component] def from(host: org.scalajs.dom.Node): ClientSideSsrContent =
    if (js.typeOf(js.Dynamic.global.Element) == "undefined") {
      empty
    } else {
      host match {
        case element: dom.Element => new ClientSideSsrContent(Some(element))
        case _                    => empty
      }
    }
}

trait ClientSideComponent extends Component {
  private var clientActivated = false

  final override def compose(): Unit =
    composeFallback()

  protected def composeFallback(): Unit

  protected def mountClient(): Unit

  final def activateClientSide(): Unit =
    if (!clientActivated && host.domNode.nonEmpty) {
      clientActivated = true
      val ssrContent = host.domNode
        .map(ClientSideSsrContent.from)
        .getOrElse(ClientSideSsrContent.empty)
      activateClientSideContent(ssrContent)
    }

  protected final def renderClient(block: => Unit): Unit =
    DslRuntime.withComponentScope(this)(block)

  protected def activateClientSideContent(ssrContent: ClientSideSsrContent): Unit =
    activateClientSideContent()

  protected def activateClientSideContent(): Unit = {
    clearFallback()
    mountClient()
  }

  protected final def releaseFallbackChildren(): Unit = {
    val fallbackChildren = children.toSeq
    _children.clear()
    fallbackChildren.foreach(_.dispose())
  }

  protected final def clearFallback(): Unit =
    children.toSeq.foreach { child =>
      removeChild(child)
      child.dispose()
    }
}

object ClientSideComponent {
  def activateTree(root: Component): Unit =
    root match {
      case clientSide: ClientSideComponent =>
        clientSide.activateClientSide()
      case _ =>
        root.children.foreach(activateTree)
    }
}
