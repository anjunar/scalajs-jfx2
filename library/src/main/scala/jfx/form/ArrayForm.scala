package jfx.form

import jfx.core.component.Box
import jfx.core.component.Component
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import scala.scalajs.js

class ArrayForm[V](val name: String)
    extends Box("fieldset")
      with Control[js.Array[V]] {

  override val valueProperty: ListProperty[V] = new ListProperty[V]()

  private val itemsObserver =
    valueProperty.observeChanges(onItemsChange)
  addDisposable(itemsObserver)

  private var mounted: Vector[Control[?]] = Vector.empty
  private var _controlRenderer: (Int => Control[?]) | Null = null

  private def hasRenderer: Boolean =
    _controlRenderer != null

  def controlRenderer: Int => Control[?] =
    _controlRenderer.asInstanceOf[Int => Control[?]]

  def controlRenderer_=(renderer: Int => Control[?]): Unit =
    addControlRenderer(renderer)

  override def compose(): Unit = {
    given Component = this

    if (!standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case _: Exception =>
      }
    }
  }

  def addControlRenderer(renderer: Int => Control[?]): Unit = {
    _controlRenderer = renderer
    rebuildAll()
  }

  private def onItemsChange(change: ListProperty.Change[V]): Unit = {
    if (!hasRenderer) {
      return
    }

    change match {
      case ListProperty.Reset(_) =>
        rebuildAll()

      case ListProperty.Add(_, items) =>
        addAtEnd(items)

      case ListProperty.Insert(index, _, items) =>
        rebuildFrom(items, index)

      case ListProperty.InsertAll(index, _, items) =>
        rebuildFrom(items, index)

      case ListProperty.RemoveAt(index, _, items) =>
        rebuildFrom(items, index)

      case ListProperty.RemoveRange(index, _, items) =>
        rebuildFrom(items, index)

      case ListProperty.UpdateAt(index, _, _, items) =>
        replaceAt(items, index)

      case ListProperty.Patch(from, _, _, items) =>
        rebuildFrom(items, from)

      case ListProperty.Clear(_, _) =>
        rebuildAll()
    }
  }

  private def rebuildAll(): Unit = {
    if (!hasRenderer) {
      return
    }

    rebuildFrom(valueProperty, 0)
  }

  private def addAtEnd(items: ListProperty[V]): Unit = {
    val index = items.length - 1
    if (index < 0) return

    if (mounted.length != index || children.length != index) {
      rebuildFrom(items, 0)
      return
    }

    mounted = mounted :+ buildChild(items, index)
  }

  private def replaceAt(items: ListProperty[V], index: Int): Unit = {
    if (index < 0 || index >= items.length) return

    if (mounted.length != items.length || children.length != mounted.length) {
      rebuildFrom(items, index)
      return
    }

    removeMountedAt(index)
    val child = buildChild(items, index)
    mounted = mounted.patch(index, Seq(child), 0)
  }

  private def rebuildFrom(items: ListProperty[V], fromIndex: Int): Unit = {
    if (!hasRenderer) {
      return
    }

    val from = math.max(0, fromIndex)

    while (mounted.length > from) {
      removeMountedAt(mounted.length - 1)
    }

    var index = from
    val end = items.length
    while (index < end) {
      mounted = mounted :+ buildChild(items, index)
      index += 1
    }
  }

  private def removeMountedAt(index: Int): Unit = {
    if (index < 0 || index >= mounted.length) return

    val child = mounted(index)
    removeChild(child)
    child.dispose()
    mounted = mounted.patch(index, Nil, 1)
  }

  private def buildChild(items: ListProperty[V], index: Int): Control[?] = {
    var child: Control[?] = null

    DslRuntime.updateBranch(this, Some(index)) {
      child = _controlRenderer.asInstanceOf[Int => Control[?]](index)
    }

    child match {
      case form: Formular[?] =>
        form.valueProperty match {
          case property: Property[Any @unchecked] =>
            property.set(items(index))
          case _ =>
        }
      case _ =>
    }

    child
  }
}

object ArrayForm {
  def arrayForm[V](name: String)(init: ArrayForm[V] ?=> Unit): ArrayForm[V] = {
    val form = new ArrayForm[V](name)
    DslRuntime.build(form)(init)
  }

  def controlRenderer[V](using form: ArrayForm[V]): Int => Control[?] =
    form.controlRenderer

  def controlRenderer_=[V](using form: ArrayForm[V])(renderer: Int => Control[?]): Unit =
    form.addControlRenderer(renderer)
}
