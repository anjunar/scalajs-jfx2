package jfx.dsl

import jfx.core.component.{Component, Text}
import jfx.core.state.Disposable

trait ElementDsl[C <: Component] {
  def classes(using c: C): Seq[String] = 
    c.host.attribute("class").getOrElse("").split(" ").toSeq.filter(_.nonEmpty)
    
  def classes_=(names: Seq[String])(using c: C): Unit = 
    c.host.setClassNames(names)

  def text_=(value: String)(using c: C): Unit = {
    Text.text(value)
  }

  def addDisposable(d: Disposable)(using c: C): Unit = 
    c.addDisposable(d)
}
