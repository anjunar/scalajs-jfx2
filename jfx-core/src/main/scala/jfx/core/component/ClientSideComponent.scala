package jfx.core.component

import jfx.dsl.DslRuntime

trait ClientSideComponent extends Component {
  private var clientActivated = false

  final override def compose(): Unit =
    composeFallback()

  protected def composeFallback(): Unit

  protected def mountClient(): Unit

  final def activateClientSide(): Unit =
    if (!clientActivated && host.domNode.nonEmpty) {
      clientActivated = true
      clearFallback()
      mountClient()
    }

  protected final def renderClient(block: => Unit): Unit =
    DslRuntime.withComponentScope(this)(block)

  private def clearFallback(): Unit =
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
