package jfx.core.component

import jfx.core.render.{HostElement, RenderBackend}
import jfx.core.state.{Disposable, ReadOnlyProperty}
import org.scalajs.dom

trait ComponentClassDsl {
  def classes(using c: Component): Seq[String] = c.classes

  def classes_=(names: Seq[String])(using c: Component): Unit = {
    c.classes = names
  }

  def addClass(name: String)(using c: Component): Unit = {
    c.addBaseClass(name)
  }

  def removeClass(name: String)(using c: Component): Unit = {
    c.removeBaseClass(name)
  }

  def classIf(name: String, condition: ReadOnlyProperty[Boolean])(using c: Component): Unit = {
    c.addDisposable(condition.observe { v =>
      if (v) c.addBaseClass(name) else c.removeBaseClass(name)
    })
  }
}

trait ComponentTextDsl {
  def text(using c: Component): String = ""

  def text_=(value: String)(using c: Component): Unit = {
    Text.text(value)
  }

  def text_=(value: ReadOnlyProperty[String])(using c: Component): Unit = {
    Text.text(value)
  }
}

trait ComponentVisibilityDsl {
  def visible(using c: Component): Boolean = true

  def visible_=(value: Boolean)(using c: Component): Unit = {
    if (value) c.host.setStyle("display", "") else c.host.setStyle("display", "none")
  }

  def visible_=(value: ReadOnlyProperty[Boolean])(using c: Component): Unit = {
    c.addDisposable(value.observe { v =>
      if (v) c.host.setStyle("display", "") else c.host.setStyle("display", "none")
    })
  }
}

trait ComponentHostDsl {
  def addDisposable(d: Disposable)(using c: Component): Unit =
    c.addDisposable(d)

  def host(using c: Component): HostElement = c.host
}

trait ComponentAttributeDsl {
  def tabIndex(using c: Component): Int = c.host.attribute("tabindex").flatMap(_.toIntOption).getOrElse(-1)
  def tabIndex_=(value: Int)(using c: Component): Unit = c.host.setAttribute("tabindex", value.toString)

  def role(using c: Component): String = c.host.attribute("role").getOrElse("")
  def role_=(value: String)(using c: Component): Unit = c.host.setAttribute("role", value)

  @deprecated("Use typed DSL properties or component-specific DSL methods instead. The generic attribute escape hatch will be removed.", "next")
  def attribute(name: String, value: String)(using c: Component): Unit = c.host.setAttribute(name, value)

  @deprecated("Use typed DSL properties or component-specific DSL methods instead. The generic attribute escape hatch will be removed.", "next")
  def attribute(name: String, value: ReadOnlyProperty[String])(using c: Component): Unit = {
    c.addDisposable(value.observe(v => c.host.setAttribute(name, v)))
  }
}

trait ComponentEventDsl {
  def onClick(handler: dom.MouseEvent => Unit)(using c: Component): Unit = c.onClickHandler(handler)

  def onInput(handler: dom.Event => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("input", handler))
  }

  def onFocus(handler: dom.FocusEvent => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("focus", e => handler(e.asInstanceOf[dom.FocusEvent])))
  }

  def onBlur(handler: dom.FocusEvent => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("blur", e => handler(e.asInstanceOf[dom.FocusEvent])))
  }

  def onSubmit(handler: dom.Event => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("submit", handler))
  }

  def onKeyDown(handler: dom.KeyboardEvent => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("keydown", e => handler(e.asInstanceOf[dom.KeyboardEvent])))
  }

  def onPointerDown(handler: dom.PointerEvent => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("pointerdown", e => handler(e.asInstanceOf[dom.PointerEvent])))
  }

  def onScroll(handler: dom.UIEvent => Unit)(using c: Component): Unit = {
    c.addDisposable(c.host.addEventListener("scroll", e => handler(e.asInstanceOf[dom.UIEvent])))
  }

  def onWindowKeyDown(handler: dom.KeyboardEvent => Unit)(using c: Component): Unit = {
    if (!RenderBackend.current.isServer) {
      val listener: dom.KeyboardEvent => Unit = handler
      dom.window.addEventListener("keydown", listener)
      c.addDisposable(() => dom.window.removeEventListener("keydown", listener))
    }
  }

  def onWindowPopState(handler: dom.Event => Unit)(using c: Component): Unit = {
    if (!RenderBackend.current.isServer) {
      val listener: dom.Event => Unit = handler
      dom.window.addEventListener("popstate", listener)
      c.addDisposable(() => dom.window.removeEventListener("popstate", listener))
    }
  }
}

trait ComponentDsl
    extends ComponentClassDsl
    with ComponentTextDsl
    with ComponentVisibilityDsl
    with ComponentHostDsl
    with ComponentAttributeDsl
    with ComponentEventDsl
