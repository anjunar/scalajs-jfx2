package jfx.control

import jfx.core.component.NativeComponent
import jfx.core.render.RenderBackend
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import jfx.router.Router
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLAnchorElement, MouseEvent, window}

class Link(initialHref: String)(using scope: Scope) extends NativeComponent[HTMLAnchorElement]("a") {

  private var currentHref = ""

  href = initialHref

  if (!RenderBackend.current.isServer) {
    addEventListener("click", handleClick)
  }

  def href: String =
    currentHref

  def href_=(value: String): Unit = {
    currentHref = Option(value).getOrElse("")

    if (currentHref.isEmpty) removeAttribute("href")
    else setAttribute("href", Link.renderHref(currentHref, scope))
  }

  def target: String =
    getAttribute("target").getOrElse("")

  def target_=(value: String): Unit =
    if (value == null || value.isBlank) removeAttribute("target")
    else setAttribute("target", value)

  private def handleClick(event: Event): Unit =
    if (shouldIntercept(event)) {
      event.preventDefault()

      val nextUrl = Router.toFullPath(currentHref)
      window.history.pushState(null, "", nextUrl)
      window.dispatchEvent(new Event("popstate"))
    }

  private def shouldIntercept(event: Event): Boolean =
    !RenderBackend.current.isServer &&
      Router.isInternalPath(currentHref) &&
      target.isBlank &&
      mouseEventAllowsInterception(event)

  private def mouseEventAllowsInterception(event: Event): Boolean =
    event match {
      case mouseEvent: MouseEvent =>
        mouseEvent.button == 0 &&
          !mouseEvent.altKey &&
          !mouseEvent.ctrlKey &&
          !mouseEvent.metaKey &&
          !mouseEvent.shiftKey

      case _ =>
        true
    }

}

object Link {

  def link(href: String): Link =
    link(href)({})

  def link(href: String)(init: Link ?=> Unit): Link =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()

      given Scope = currentScope
      val component = new Link(href)

      DslRuntime.withComponentContext(ComponentContext(Some(component))) {
        given Scope = currentScope
        given Link = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def href(using link: Link): String =
    link.href

  def href_=(value: String)(using link: Link): Unit =
    link.href = value

  def target(using link: Link): String =
    link.target

  def target_=(value: String)(using link: Link): Unit =
    link.target = value

  private[control] def renderHref(href: String, scope: Scope): String =
    if (!Router.isInternalPath(href)) {
      href
    } else if (RenderBackend.current.isServer) {
      Router.toFullPath(href, serverBasePath(scope))
    } else {
      Router.toFullPath(href)
    }

  private def serverBasePath(scope: Scope): String =
    try {
      scope.inject[Ssr.Request].attributes
        .get("basePath")
        .collect { case value: String => value }
        .getOrElse("")
    } catch {
      case _: NoSuchElementException => ""
    }

}
