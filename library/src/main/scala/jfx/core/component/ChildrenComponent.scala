package jfx.core.component

import jfx.core.state.ListProperty
import org.scalajs.dom

import scala.collection.mutable

abstract class ChildrenComponent[E <: dom.Element](tagName: String) extends ElementComponent[E](tagName) {

  private sealed trait ChildEntry {
    def length: Int
    def children: Vector[NodeComponent[? <: dom.Node]]
  }

  private final case class ComponentEntry(component: NodeComponent[? <: dom.Node]) extends ChildEntry {
    override def length: Int =
      1

    override def children: Vector[NodeComponent[? <: dom.Node]] =
      Vector(component)
  }

  private final case class SlotEntry(slot: MutableChildSlot) extends ChildEntry {
    override def length: Int =
      slot.currentChildren.length

    override def children: Vector[NodeComponent[? <: dom.Node]] =
      slot.currentChildren
  }

  private final class MutableChildSlot extends ChildSlot {
    private var children = Vector.empty[NodeComponent[? <: dom.Node]]
    private var disposed = false

    override def replace(nextChildren: IterableOnce[NodeComponent[? <: dom.Node]]): Unit = {
      if (disposed) return

      val next = nextChildren.iterator.toVector
      flatStartIndexOf(this) match {
        case Some(startIndex) =>
          val replaced = children.length
          children = next
          childrenProperty.patchInPlace(startIndex, next, replaced)

        case None =>
          children.foreach(_.dispose())
          next.foreach(_.dispose())
          children = Vector.empty
      }
    }

    override def currentChildren: Vector[NodeComponent[? <: dom.Node]] =
      children

    override def dispose(): Unit = {
      if (!disposed) {
        flatStartIndexOf(this) match {
          case Some(_) =>
            clear()
          case None =>
            children.foreach(_.dispose())
            children = Vector.empty
        }
        disposed = true
        removeSlot(this)
      }
    }
  }

  private val childEntries = mutable.ArrayBuffer.empty[ChildEntry]

  val childrenProperty: ListProperty[NodeComponent[? <: dom.Node]] =
    new ListProperty[NodeComponent[? <: dom.Node]]()

  override def dispose(): Unit = {
    childrenProperty.clear()
    childEntries.clear()
    super.dispose()
  }

  def addChild(child: NodeComponent[? <: dom.Node]): Unit =
    if (!childrenProperty.contains(child)) {
      childEntries += ComponentEntry(child)
      childrenProperty += child
    }

  def removeChild(child: NodeComponent[? <: dom.Node]): Unit = {
    val entryIndex = childEntries.indexWhere(_.children.contains(child))
    if (entryIndex < 0) return

    childEntries(entryIndex) match {
      case ComponentEntry(_) =>
        childEntries.remove(entryIndex)
        childrenProperty -= child

      case SlotEntry(slot) =>
        slot.replace(slot.currentChildren.filterNot(_ eq child))
    }
  }

  def insertChild(index: Int, child: NodeComponent[? <: dom.Node]): Unit =
    if (!childrenProperty.contains(child)) {
      val entryIndex = entryIndexForFlatInsert(index)
      childEntries.insert(entryIndex, ComponentEntry(child))
      childrenProperty.insert(math.max(0, math.min(index, childrenProperty.length)), child)
    }

  def clearChildren(): Unit = {
    childEntries.clear()
    childrenProperty.clear()
  }

  def reserveChildSlot(): ChildSlot = {
    val slot = new MutableChildSlot()
    childEntries += SlotEntry(slot)
    slot
  }

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

  private def flatStartIndexOf(slot: MutableChildSlot): Option[Int] = {
    var index = 0
    var entryIndex = 0

    while (entryIndex < childEntries.length) {
      childEntries(entryIndex) match {
        case SlotEntry(current) if current eq slot =>
          return Some(index)
        case entry =>
          index += entry.length
      }

      entryIndex += 1
    }

    None
  }

  private def removeSlot(slot: MutableChildSlot): Unit = {
    val index = childEntries.indexWhere {
      case SlotEntry(current) => current eq slot
      case _ => false
    }

    if (index >= 0) {
      childEntries.remove(index)
    }
  }

  private def entryIndexForFlatInsert(flatIndex: Int): Int = {
    val boundedIndex = math.max(0, math.min(flatIndex, childrenProperty.length))
    var cursor = 0
    var entryIndex = 0

    while (entryIndex < childEntries.length) {
      val entry = childEntries(entryIndex)
      if (boundedIndex <= cursor) {
        return entryIndex
      }
      cursor += entry.length
      entryIndex += 1
    }

    childEntries.length
  }

}
