package jfx.control

import jfx.core.component.Box
import jfx.core.component.Component.*
import jfx.dsl.DslRuntime
import jfx.router.RouterConfig
import org.scalajs.dom
import org.scalajs.dom.window

class Link(initialHref: String) extends Box("a") {
  
  override def compose(): Unit = {
    given jfx.core.component.Component = this
    super.compose()
    
    val resolved = RouterConfig.resolve(initialHref)
    attribute("href", resolved)
    
    onClick { e =>
      e.preventDefault()
      val currentHref = host.attribute("href").getOrElse("")
      window.history.pushState(null, "", currentHref)
      window.dispatchEvent(new dom.Event("popstate"))
    }
  }

  def href: String = host.attribute("href").getOrElse("")
  def href_=(value: String): Unit = host.setAttribute("href", RouterConfig.resolve(value))
}

object Link {
  def link(href: String)(init: Link ?=> Unit): Link = {
    DslRuntime.build(new Link(href))(init)
  }

  def href(using l: Link): String = l.href
  def href_=(value: String)(using l: Link): Unit = l.href = value
}
