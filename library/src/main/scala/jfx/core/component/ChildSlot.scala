package jfx.core.component

import jfx.core.state.Disposable
import org.scalajs.dom

trait ChildSlot extends Disposable {

  def replace(children: IterableOnce[NodeComponent[? <: dom.Node]]): Unit

  def clear(): Unit =
    replace(Seq.empty)

  def currentChildren: Vector[NodeComponent[? <: dom.Node]]

}
