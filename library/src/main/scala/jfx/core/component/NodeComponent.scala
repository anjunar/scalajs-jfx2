package jfx.core.component

import jfx.core.render.HostNode
import jfx.core.state.{CompositeDisposable, Disposable}
import jfx.dsl.DslRuntime
import org.scalajs.dom

trait NodeComponent[E <: dom.Node] extends Disposable {

  private var mounted = false

  var parent: Option[NodeComponent[? <: dom.Node]] = None

  private[jfx] def hostNode: HostNode

  def element: E =
    hostNode.domNodeOption
      .getOrElse {
        throw IllegalStateException(
          s"${getClass.getSimpleName}.element is only available while rendering in the browser or during hydration"
        )
      }
      .asInstanceOf[E]

  def renderHtml: String =
    hostNode.html

  final def onMount(): Unit = {
    if (mounted) return

    mounted = true
    mountContent()
    afterMount()
    childComponentsIterator.foreach { child =>
      if (!child.isMounted) {
        child.onMount()
      }
    }
  }

  protected def mountContent(): Unit = {}

  protected def afterMount(): Unit = {}

  private[jfx] final def onUnmount(): Unit = {
    if (!mounted) return

    mounted = false
    childComponentsIterator.foreach(_.onUnmount())
    afterUnmount()
  }

  protected def afterUnmount(): Unit = {}

  private[jfx] final def isMounted: Boolean =
    mounted

  val disposable = new CompositeDisposable()

  def addDisposable(value: Disposable): Unit =
    disposable.add(value)

  override def dispose(): Unit =
    disposable.dispose()

  private[jfx] def attachChild(child: NodeComponent[? <: dom.Node]): Unit =
    throw IllegalStateException(s"${getClass.getSimpleName} does not accept child components")

  private[jfx] def detachChild(child: NodeComponent[? <: dom.Node]): Boolean =
    false

  private[jfx] def childComponentsIterator: Iterator[NodeComponent[? <: dom.Node]] =
    Iterator.empty

}

object NodeComponent {

  def mount[C <: NodeComponent[? <: dom.Node]](component: C): C =
    DslRuntime.currentScope { _ =>
      val currentContext = DslRuntime.currentComponentContext()
      DslRuntime.attach(component, currentContext)
      component
    }

}
