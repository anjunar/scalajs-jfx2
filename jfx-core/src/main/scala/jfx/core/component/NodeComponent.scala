package jfx.core.component

import jfx.core.render.HostNode
import jfx.core.state.{CompositeDisposable, Disposable}
import org.scalajs.dom

/**
 * Base trait for ALL components in the JFX2 tree.
 * Enforces parent-child relationship and lifecycle.
 */
trait NodeComponent[E <: dom.Node] extends Disposable {
  private var _parent: Option[NodeComponent[? <: dom.Node]] = None
  private var mounted = false

  def parent: Option[NodeComponent[? <: dom.Node]] = _parent
  
  private[jfx] def setParent(newParent: Option[NodeComponent[? <: dom.Node]]): Unit = {
    _parent = newParent
  }

  private[jfx] def hostNode: HostNode
  
  def renderHtml: String = hostNode.html

  def onMount(): Unit = {
    if (mounted) return
    mounted = true
    mountContent()
    childComponentsIterator.foreach(_.onMount())
  }

  def onUnmount(): Unit = {
    if (!mounted) return
    childComponentsIterator.foreach(_.onUnmount())
    unmountContent()
    mounted = false
  }

  protected def mountContent(): Unit = {}
  protected def unmountContent(): Unit = {}

  def isMounted: Boolean = mounted

  private[jfx] def childComponentsIterator: Iterator[NodeComponent[? <: dom.Node]] = Iterator.empty

  private[jfx] def attachChild(child: NodeComponent[? <: dom.Node]): Unit =
    throw IllegalStateException(s"${getClass.getSimpleName} does not accept child components")

  private[jfx] def detachChild(child: NodeComponent[? <: dom.Node]): Unit = {}

  val disposable = new CompositeDisposable()
  def addDisposable(d: Disposable): Unit = disposable.add(d)
  override def dispose(): Unit = {
    onUnmount()
    disposable.dispose()
  }
}
