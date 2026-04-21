package jfx.core.component

import jfx.core.render.{Cursor, DomHostElement, HostElement, HostNode}
import jfx.core.state.{CompositeDisposable, Disposable}
import jfx.di.{HierarchicalRegistry, ServiceRegistry}

import scala.collection.mutable
import scala.compiletime.uninitialized

trait ComponentCore extends Disposable {
  protected var _host: HostNode = uninitialized
  private var _parent: Option[Component] = None
  private[jfx] val _children = mutable.ArrayBuffer.empty[Component]
  protected val disposable = new CompositeDisposable()
  private var _bindCursor: Cursor = uninitialized
  private var _registry: ServiceRegistry = new HierarchicalRegistry(None)

  def tagName: String

  def initialize(): Unit = {}
  def compose(): Unit = {}
  def afterCompose(): Unit = {}

  def bindCursor: Cursor = _bindCursor
  private[jfx] def registry: ServiceRegistry = _registry
  def isVirtual: Boolean = tagName == ""

  def hostNode: HostNode = _host

  def host: HostElement = {
    if (isVirtual) _parent.map(_.host).getOrElse(
      throw new IllegalStateException(s"Virtual component [${getClass.getName}] (tagName: '$tagName') has no physical parent to delegate 'host' to. Parent: ${_parent.map(_.getClass.getName).getOrElse("None")}. Did you try to access 'host' before the component was attached to the tree?")
    )
    else if (_host == null) {
      throw new IllegalStateException(
        s"Component [${getClass.getName}] (tagName: '$tagName') has no host yet. " +
          s"BindCursor present: ${_bindCursor != null}. " +
          s"Parent: ${_parent.map(_.getClass.getName).getOrElse("None")}. " +
          "Access 'host' only inside or after 'compose'."
      )
    }
    else _host.asInstanceOf[HostElement]
  }

  def parent: Option[Component] = _parent
  def children: Seq[Component] = _children.toSeq

  private[jfx] def physicalHostNodes: Seq[HostNode] =
    if (isVirtual) _children.flatMap(_.physicalHostNodes).toSeq
    else Option(_host).toSeq

  def domNodeCount: Int =
    physicalHostNodes.length

  def calculateDomOffset: Int = {
    _parent.map { p =>
      val siblingsBefore = p._children.takeWhile(_ != this)
      val localOffset = siblingsBefore.map(_.domNodeCount).sum
      if (p.isVirtual) p.calculateDomOffset + localOffset
      else localOffset
    }.getOrElse(0)
  }

  private[jfx] def setParent(newParent: Option[Component]): Unit = {
    _parent = newParent
  }

  private[jfx] def setRegistry(registry: ServiceRegistry): Unit = {
    _registry = registry
  }

  private[jfx] def addChild(child: Component): Unit = {
    _children += child
  }

  private[jfx] def syncChildAddition(child: Component): Unit = {
    _host match {
      case h: HostElement =>
        val offset = child.calculateDomOffset
        child.physicalHostNodes.zipWithIndex.foreach { case (node, index) =>
          h.insertChild(offset + index, node)
        }
      case _              => if (isVirtual) _parent.foreach(_.syncChildAddition(child))
    }
  }

  private[jfx] def insertChild(index: Int, child: Component): Unit = {
    _children.insert(index, child)
  }

  private[jfx] def removeChild(child: Component): Unit = {
    val childDomNodes = child.physicalHostNodes
    _children -= child

    def performRemove(target: Component, nodes: Seq[HostNode]): Unit = {
      target.hostNode match {
        case h: HostElement => nodes.foreach(h.removeChild)
        case _              => if (target.isVirtual) target.parent.foreach(p => performRemove(p, nodes))
      }
    }

    performRemove(this.asInstanceOf[Component], childDomNodes)
  }

  private[jfx] def bind(cursor: Cursor): Unit = {
    if (cursor == null) {
      throw new IllegalArgumentException(s"Cannot bind component (tagName: '$tagName') to a null cursor.")
    }
    _bindCursor = cursor
    if (isVirtual) {
      _host = null
    } else {
      val nextHost = if (tagName == "#text") {
        cursor.claimText("")
      } else {
        cursor.claimElement(tagName)
      }

      (_host, nextHost) match {
        case (current: DomHostElement, next: DomHostElement) =>
          current.updateElement(next.element)
        case _ =>
          _host = nextHost
      }

      syncClasses()
    }
  }

  private[jfx] def syncClasses(): Unit

  def addDisposable(d: Disposable): Unit = disposable.add(d)

  override def dispose(): Unit = {
    _children.foreach(_.dispose())
    _children.clear()
    disposable.dispose()
  }
}
