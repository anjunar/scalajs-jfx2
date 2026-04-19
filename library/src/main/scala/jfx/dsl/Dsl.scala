package jfx.dsl

import jfx.core.component.{Component, Text}
import jfx.core.state.Disposable

object Dsl {
  def classes(using c: Component): Seq[String] = 
    c.host.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
    
  def classes_=(names: Seq[String])(using c: Component): Unit = {
    val current = classes
    c.host.setClassNames((current ++ names).distinct)
  }

  def addClass(name: String)(using c: Component): Unit = {
    val current = classes
    if (!current.contains(name)) {
      c.host.setClassNames(current :+ name)
    }
  }

  def text(using c: Component): String = ""
  def text_=(value: String)(using c: Component): Unit = {
    Text.text(value)
  }

  def addDisposable(d: Disposable)(using c: Component): Unit = 
    c.addDisposable(d)

  def host(using c: Component): jfx.core.render.HostElement = c.host

  def style(init: StyleProxy ?=> Unit)(using c: Component): Unit = 
    StyleDsl.style(init)
}
