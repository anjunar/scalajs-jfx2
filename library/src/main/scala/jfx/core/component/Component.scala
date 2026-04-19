package jfx.core.component

import jfx.core.render.{Cursor, HostElement, HostNode}
import jfx.core.state.{CompositeDisposable, Disposable}
import jfx.dsl.DslRuntime
import org.scalajs.dom
import scala.compiletime.uninitialized
import scala.collection.mutable

trait Component extends Disposable {
  protected var _host: HostNode = uninitialized
  private var _parent: Option[Component] = None
  private val _children = mutable.ArrayBuffer.empty[Component]
  protected val disposable = new CompositeDisposable()

  def hostNode: HostNode = _host
  def host: HostElement = _host.asInstanceOf[HostElement]
  def parent: Option[Component] = _parent
  def children: Seq[Component] = _children.toSeq

  private[jfx] def setParent(newParent: Option[Component]): Unit = {
    _parent = newParent
  }

  private[jfx] def addChild(child: Component): Unit = {
    _children += child
    // Synchronize DOM if host is an element
    _host match {
      case h: HostElement => h.insertChild(_children.length - 1, child.hostNode)
      case _ =>
    }
  }

  private[jfx] def insertChild(index: Int, child: Component): Unit = {
    if (index < 0 || index >= _children.length) {
      _children += child
    } else {
      _children.insert(index, child)
    }
    
    // Synchronize DOM if host is an element
    _host match {
      case h: HostElement => h.insertChild(index, child.hostNode)
      case _ =>
    }
  }

  private[jfx] def removeChild(child: Component): Unit = {
    _children -= child
    _host match {
      case h: HostElement => h.removeChild(child.hostNode)
      case _ =>
    }
  }

  private[jfx] def bind(cursor: Cursor): Unit = {
    _host = if (tagName == "#text") {
       cursor.claimText("") 
    } else {
       cursor.claimElement(tagName)
    }
  }

  def tagName: String
  def compose(): Unit = {}
  
  def addDisposable(d: Disposable): Unit = disposable.add(d)

  override def dispose(): Unit = {
    _children.foreach(_.dispose())
    _children.clear()
    disposable.dispose()
  }
}
