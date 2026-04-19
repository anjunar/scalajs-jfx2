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
  private[jfx] val _children = mutable.ArrayBuffer.empty[Component]
  protected val disposable = new CompositeDisposable()
  private var _bindCursor: Cursor = uninitialized

  def bindCursor: Cursor = _bindCursor
  def isVirtual: Boolean = tagName == ""

  def hostNode: HostNode = _host
  
  def host: HostElement = {
    if (isVirtual) _parent.map(_.host).getOrElse(
       throw new IllegalStateException(s"Virtual component $tagName has no physical parent to delegate 'host' to.")
    )
    else _host.asInstanceOf[HostElement]
  }

  def parent: Option[Component] = _parent
  def children: Seq[Component] = _children.toSeq

  /** Sum of physical DOM nodes this component and its descendants represent. */
  def domNodeCount: Int = {
    if (isVirtual) _children.map(_.domNodeCount).sum
    else if (_host != null) 1
    else 0
  }

  /** Calculates the physical index of this component within its nearest physical HostElement. */
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

  private[jfx] def addChild(child: Component): Unit = {
    _children += child
  }

  private[jfx] def syncChildAddition(child: Component): Unit = {
    _host match {
      case h: HostElement => h.insertChild(child.calculateDomOffset, child.hostNode)
      case _ => if (isVirtual) _parent.foreach(_.syncChildAddition(child))
    }
  }

  private[jfx] def insertChild(index: Int, child: Component): Unit = {
    _children.insert(index, child)
  }

  private[jfx] def removeChild(child: Component): Unit = {
    val childDomNodes = if (child.isVirtual) child._children.map(_.hostNode).toSeq else Seq(child.hostNode)
    _children -= child
    
    def performRemove(target: Component, nodes: Seq[HostNode]): Unit = {
      target._host match {
        case h: HostElement => nodes.foreach(h.removeChild)
        case _ => if (target.isVirtual) target._parent.foreach(p => performRemove(p, nodes))
      }
    }
    performRemove(this, childDomNodes)
  }

  private[jfx] def bind(cursor: Cursor): Unit = {
    _bindCursor = cursor
    if (isVirtual) {
      _host = null
    } else {
      _host = if (tagName == "#text") {
         cursor.claimText("") 
      } else {
         cursor.claimElement(tagName)
      }
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

object Component {
  def classes(using c: Component): Seq[String] = 
    c.host.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
    
  def classes_=(names: Seq[String])(using c: Component): Unit = {
    c.host.setClassNames(names.distinct)
  }

  def classes_=(name: String)(using c: Component): Unit = {
    classes_=(name.split("\\s+").toSeq.filter(_.nonEmpty))
  }

  def addClass(name: String)(using c: Component): Unit = {
    val current = classes
    if (!current.contains(name)) {
      c.host.setClassNames((current :+ name).distinct)
    }
  }

  def text(using c: Component): String = ""
  def text_=(value: String)(using c: Component): Unit = {
    Text.text(value)
  }

  def addDisposable(d: Disposable)(using c: Component): Unit = 
    c.addDisposable(d)

  def host(using c: Component): jfx.core.render.HostElement = c.host

  export jfx.dsl.StyleDsl.*
}
