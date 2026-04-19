package jfx.core.component

import org.scalajs.dom

import scala.collection.mutable

abstract class ManagedElementComponent[E <: dom.Element](tagName: String) extends ElementComponent[E](tagName) {

  private val managedChildren = mutable.ArrayBuffer.empty[NodeComponent[? <: dom.Node]]

  protected final def addChild(child: NodeComponent[? <: dom.Node]): Unit = {
    if (managedChildren.exists(_ eq child)) {
      return
    }

    managedChildren += child
    hostElement.appendChild(child.hostNode)
    child.parent = Some(this)
    child.onMount()
  }

  protected final def insertChild(index: Int, child: NodeComponent[? <: dom.Node]): Unit = {
    if (managedChildren.exists(_ eq child)) {
      return
    }

    val boundedIndex = math.max(0, math.min(index, managedChildren.length))
    managedChildren.insert(boundedIndex, child)
    hostElement.insertChild(boundedIndex, child.hostNode)
    child.parent = Some(this)
    child.onMount()
  }

  protected final def removeChild(child: NodeComponent[? <: dom.Node]): Unit = {
    val index = managedChildren.indexWhere(_ eq child)

    if (index >= 0) {
      managedChildren.remove(index)

      hostElement.removeChild(child.hostNode)

      if (child.isMounted) {
        child.onUnmount()
      }

      child.parent = None
      child.dispose()
    }
  }

  protected final def clearChildren(): Unit =
    managedChildren.toVector.foreach(removeChild)

  override def dispose(): Unit = {
    clearChildren()
    super.dispose()
  }

  override private[jfx] def attachChild(child: NodeComponent[? <: dom.Node]): Unit =
    addChild(child)

  override private[jfx] def detachChild(child: NodeComponent[? <: dom.Node]): Boolean = {
    val contains = managedChildren.exists(_ eq child)
    if (contains) {
      removeChild(child)
    }
    contains
  }

  override private[jfx] def childComponentsIterator: Iterator[NodeComponent[? <: dom.Node]] =
    managedChildren.iterator

}
