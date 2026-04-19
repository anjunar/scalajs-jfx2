package jfx.control

import jfx.core.component.Box
import jfx.dsl.DslRuntime
import org.scalajs.dom
import org.scalajs.dom.window

class Link(initialHref: String) extends Box("a") {
  
  override def compose(): Unit = {
    super.compose()
    host.setAttribute("href", initialHref)
    
    addDisposable(host.addEventListener("click", { e =>
      e.preventDefault()
      val href = host.attribute("href").getOrElse("")
      window.history.pushState(null, "", href)
      window.dispatchEvent(new dom.Event("popstate"))
    }))
  }

  def href: String = host.attribute("href").getOrElse("")
  def href_=(value: String): Unit = host.setAttribute("href", value)
}

object Link {
  def link(href: String)(init: Link ?=> Unit): Link = {
    DslRuntime.build(new Link(href))(init)
  }

  def href(using l: Link): String = l.href
  def href_=(value: String)(using l: Link): Unit = l.href = value
}
