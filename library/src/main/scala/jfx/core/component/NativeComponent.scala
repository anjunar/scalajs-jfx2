package jfx.core.component

import jfx.core.render.RenderBackend
import jfx.core.state.ListProperty
import jfx.core.state.ListProperty.*
import org.scalajs.dom

abstract class NativeComponent[E <: dom.Element](tagName: String) extends ChildrenComponent[E](tagName) {

  private val childrenObserver =
    childrenProperty.observeChanges(onChildrenChange)
  disposable.add(childrenObserver)

  private def onChildrenChange(change: ListProperty.Change[NodeComponent[? <: dom.Node]]): Unit =
    change match {
      case Reset(_) =>
        hostElement.clearChildren()
        childrenProperty.foreach(attachDomChild)

      case Add(child, _) =>
        attachDomChild(child)

      case Insert(index, child, _) =>
        attachDomChildAt(index, child)

      case InsertAll(index, children, _) =>
        children.zipWithIndex.foreach { case (child, offset) =>
          attachDomChildAt(index + offset, child)
        }

      case RemoveAt(_, child, _) =>
        detachDomChild(child)

      case RemoveRange(_, children, _) =>
        children.foreach(detachDomChild)

      case UpdateAt(index, oldChild, newChild, _) =>
        detachDomChild(oldChild)
        attachDomChildAt(index, newChild)

      case Patch(from, removed, inserted, _) =>
        removed.foreach(detachDomChild)
        inserted.zipWithIndex.foreach { case (child, offset) =>
          attachDomChildAt(from + offset, child)
        }

      case Clear(removed, _) =>
        removed.foreach(detachDomChild)
    }

  private def attachDomChild(child: NodeComponent[? <: dom.Node]): Unit = {
    hostElement.appendChild(child.hostNode)
    finishAttach(child)
  }

  private def attachDomChildAt(index: Int, child: NodeComponent[? <: dom.Node]): Unit = {
    hostElement.insertChild(index, child.hostNode)
    finishAttach(child)
  }

  private def finishAttach(child: NodeComponent[? <: dom.Node]): Unit = {
    child.parent = Some(this)

    if (!RenderBackend.current.isServer && isMounted && !child.isMounted) {
      child.onMount()
    }
  }

  private def detachDomChild(child: NodeComponent[? <: dom.Node]): Unit = {
    hostElement.removeChild(child.hostNode)
    if (child.isMounted) {
      child.onUnmount()
    }
    child.parent = None
    child.dispose()
  }

}
