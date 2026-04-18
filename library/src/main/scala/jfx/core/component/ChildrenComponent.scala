package jfx.core.component

import jfx.core.state.ListProperty
import org.scalajs.dom

abstract class ChildrenComponent[E <: dom.Element](tagName: String) extends ElementComponent[E](tagName) {

  val childrenProperty: ListProperty[NodeComponent[? <: dom.Node]] =
    new ListProperty[NodeComponent[? <: dom.Node]]()

  override def dispose(): Unit = {
    childrenProperty.clear()
    super.dispose()
  }

  def addChild(child: NodeComponent[? <: dom.Node]): Unit =
    if (!childrenProperty.contains(child)) {
      childrenProperty += child
    }

  def removeChild(child: NodeComponent[? <: dom.Node]): Unit =
    childrenProperty -= child

  def insertChild(index: Int, child: NodeComponent[? <: dom.Node]): Unit =
    if (!childrenProperty.contains(child)) {
      childrenProperty.insert(index, child)
    }

  def clearChildren(): Unit =
    childrenProperty.clear()

  override private[jfx] def attachChild(child: NodeComponent[? <: dom.Node]): Unit =
    addChild(child)

  override private[jfx] def detachChild(child: NodeComponent[? <: dom.Node]): Boolean = {
    val containsChild = childrenProperty.contains(child)
    if (containsChild) {
      removeChild(child)
    }
    containsChild
  }

  override private[jfx] def childComponentsIterator: Iterator[NodeComponent[? <: dom.Node]] =
    childrenProperty.iterator

}
