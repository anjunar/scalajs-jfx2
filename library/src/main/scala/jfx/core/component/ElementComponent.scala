package jfx.core.component

import jfx.core.render.{HostElement, RenderBackend}
import jfx.core.state.{Disposable, ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.{DslRuntime, StyleTarget}
import org.scalajs.dom

import scala.collection.mutable

abstract class ElementComponent[E <: dom.Element](tagName: String) extends NodeComponent[E] {

  given self: ElementComponent[E] = this

  private[jfx] final val hostElement: HostElement = {
    val parentHost =
      DslRuntime.currentComponentContext().parent.collect {
        case parent: ElementComponent[?] => parent.hostElement
      }

    RenderBackend.current.createElement(tagName, parentHost)
  }

  override private[jfx] final def hostNode =
    hostElement

  val textContentProperty = new Property[String]("")

  val classProperty = new ListProperty[String]()

  private val styleBindings =
    mutable.LinkedHashMap.empty[String, Disposable]

  addDisposable(() => {
    styleBindings.values.foreach(_.dispose())
    styleBindings.clear()
  })

  private val textContentObserver =
    textContentProperty.observeWithoutInitial(text => hostElement.setTextContent(text))
  addDisposable(textContentObserver)

  private val classObserver =
    classProperty.observe { classNames =>
      hostElement.setClassNames(ElementComponent.normalizeClassNames(classNames.toSeq))
    }
  addDisposable(classObserver)

  def textContent: String =
    textContentProperty.get

  def textContent_=(value: String): Unit =
    textContentProperty.set(value)

  def css: dom.CSSStyleDeclaration =
    hostElement.domElementOption
      .collect { case html: dom.HTMLElement => html.style }
      .getOrElse {
        throw IllegalStateException("css is only available for browser-backed HTMLElement components")
      }

  def addStyle(init: StyleTarget ?=> Unit): Unit = {
    given StyleTarget = StyleTarget(this, hostElement)
    init
  }

  def setAttribute(name: String, value: String): Unit =
    hostElement.setAttribute(name, value)

  def removeAttribute(name: String): Unit =
    hostElement.removeAttribute(name)

  def getAttribute(name: String): Option[String] =
    hostElement.attribute(name)

  def addEventListener(name: String, listener: dom.Event => Unit): Disposable = {
    val disposable = hostElement.addEventListener(name, listener)
    addDisposable(disposable)
    disposable
  }

  private[jfx] final def bindStyleProperty(
    name: String,
    property: ReadOnlyProperty[String]
  )(applyValue: String => Unit): Unit = {
    clearStylePropertyBinding(name)
    val binding = property.observe(applyValue)
    styleBindings.update(name, binding)
  }

  private[jfx] final def clearStylePropertyBinding(name: String): Unit =
    styleBindings.remove(name).foreach(_.dispose())

}

object ElementComponent {

  private[jfx] def normalizeClassNames(classNames: IterableOnce[String]): Vector[String] = {
    val normalized = mutable.LinkedHashSet.empty[String]

    classNames.iterator.foreach { className =>
      if (className != null) {
        className
          .split("\\s+")
          .iterator
          .map(_.trim)
          .filter(_.nonEmpty)
          .foreach(normalized += _)
      }
    }

    normalized.toVector
  }

  def textProperty(using component: ElementComponent[?]): Property[String] =
    component.textContentProperty

  def text(using component: ElementComponent[?]): String =
    component.textContent

  def text_=(value: String)(using component: ElementComponent[?]): Unit =
    component.textContent = value

  def classes(using component: ElementComponent[?]): ListProperty[String] =
    component.classProperty

  def classes_=(value: String)(using component: ElementComponent[?]): Unit =
    component.classProperty.setAll(normalizeClassNames(Seq(value)))

  def classes_=(value: IterableOnce[String])(using component: ElementComponent[?]): Unit =
    component.classProperty.setAll(normalizeClassNames(value))

  def addClass(value: String)(using component: ElementComponent[?]): Unit =
    addClasses(Seq(value))

  def addClasses(values: IterableOnce[String])(using component: ElementComponent[?]): Unit = {
    val additions = normalizeClassNames(values)
    if (additions.nonEmpty) {
      updateClasses(component)(_ ++ additions.filterNot(component.classProperty.contains))
    }
  }

  def removeClass(value: String)(using component: ElementComponent[?]): Unit =
    removeClasses(Seq(value))

  def removeClasses(values: IterableOnce[String])(using component: ElementComponent[?]): Unit = {
    val removed = normalizeClassNames(values).toSet
    if (removed.nonEmpty) {
      updateClasses(component)(_.filterNot(removed.contains))
    }
  }

  def onClick(listener: dom.Event => Unit)(using component: ElementComponent[?]): Disposable =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val wrapped: dom.Event => Unit = event =>
        DslRuntime.withScope(currentScope) {
          DslRuntime.withComponentContext(currentContext) {
            listener(event)
          }
        }

      component.addEventListener("click", wrapped)
    }

  private def updateClasses(
    component: ElementComponent[?]
  )(update: Vector[String] => Vector[String]): Unit = {
    val currentRaw = component.classProperty.iterator.toVector
    val current = normalizeClassNames(currentRaw)
    val next = normalizeClassNames(update(current))

    if (currentRaw != next) {
      component.classProperty.setAll(next)
    }
  }

}
