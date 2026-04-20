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

  private val _baseClasses = mutable.ArrayBuffer.empty[String]
  private val _userClasses = mutable.ArrayBuffer.empty[String]

  def baseClasses: Seq[String] = _baseClasses.toSeq
  def userClasses: Seq[String] = _userClasses.toSeq

  def classes: Seq[String] = {
     _host match {
       case h: HostElement => h.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
       case _ => (_baseClasses ++ _userClasses).distinct.toSeq
     }
  }

  def classes_=(names: Seq[String]): Unit = setUserClasses(names)
  def classes_=(name: String): Unit = setUserClasses(name.split("\\s+").toSeq.filter(_.nonEmpty))

  private[jfx] def addBaseClass(name: String): Unit = {
    if (!_baseClasses.contains(name)) {
      _baseClasses += name
      syncClasses()
    }
  }

  private[jfx] def removeBaseClass(name: String): Unit = {
    if (_baseClasses.contains(name)) {
      _baseClasses -= name
      syncClasses()
    }
  }

  private[jfx] def setUserClasses(names: Seq[String]): Unit = {
    _userClasses.clear()
    _userClasses ++= names
    syncClasses()
  }

  private def syncClasses(): Unit = {
    _host match {
      case h: HostElement => h.setClassNames((_baseClasses ++ _userClasses).distinct.toSeq)
      case _ =>
    }
  }

  private[jfx] def bind(cursor: Cursor): Unit = {
    if (cursor == null) {
      throw new IllegalArgumentException(s"Cannot bind component (tagName: '$tagName') to a null cursor.")
    }
    _bindCursor = cursor
    if (isVirtual) {
      _host = null
    } else {
      _host = if (tagName == "#text") {
         cursor.claimText("") 
      } else {
         cursor.claimElement(tagName)
      }
      syncClasses() // Apply classes after bind
    }
  }

  def tagName: String
  def compose(): Unit = {}
  
  def onClickHandler(handler: dom.MouseEvent => Unit): Unit = {
    addDisposable(host.addEventListener("click", e => handler(e.asInstanceOf[dom.MouseEvent])))
  }

  def addDisposable(d: Disposable): Unit = disposable.add(d)

  override def dispose(): Unit = {
    _children.foreach(_.dispose())
    _children.clear()
    disposable.dispose()
  }
}

object Component {
  def classes(using c: Component): Seq[String] = c.classes
    
  def classes_=(names: Seq[String])(using c: Component): Unit = {
    c.classes = names
  }

  def classes_=(name: String)(using c: Component): Unit = {
    c.classes = name
  }

  def addClass(name: String)(using c: Component): Unit = {
    c.addBaseClass(name)
  }

  def removeClass(name: String)(using c: Component): Unit = {
    c.removeBaseClass(name)
  }

  def text(using c: Component): String = ""
  def text_=(value: String)(using c: Component): Unit = {
    Text.text(value)
  }
  def text_=(value: jfx.core.state.ReadOnlyProperty[String])(using c: Component): Unit = {
    Text.text(value)
  }

  def visible(using c: Component): Boolean = true // Default state, primarily needed for property assignment syntax
  def visible_=(value: jfx.core.state.ReadOnlyProperty[Boolean])(using c: Component): Unit = {
    c.addDisposable(value.observe { v =>
      if (v) c.removeBaseClass("is-hidden") else c.addBaseClass("is-hidden")
    })
  }

  def addDisposable(d: Disposable)(using c: Component): Unit = 
    c.addDisposable(d)

  def host(using c: Component): jfx.core.render.HostElement = c.host

  def onClick(handler: dom.MouseEvent => Unit)(using c: Component): Unit = c.onClickHandler(handler)

  export jfx.dsl.StyleDsl.*
}
