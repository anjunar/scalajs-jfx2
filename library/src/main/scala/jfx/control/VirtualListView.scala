package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.ListProperty
import jfx.dsl.DslRuntime

class VirtualListView[T] extends Component {
  override def tagName: String = "div"
  
  val items = new ListProperty[T]()
  
  // Internal state for virtualization
  private var viewportSize = 10
  private var scrollOffset = 0

  override def compose(): Unit = {
    host.setClassNames(Seq("jfx-virtual-list-view"))
    
    // Create a fixed number of row components (the "Pool")
    // These are real components in the tree!
    (0 until viewportSize).foreach { i =>
       Box.box("div") { row ?=>
         row.host.setClassNames(Seq("jfx-virtual-row"))
         // Content will be updated dynamically
         // We could use a Property[T] for each row and observe it inside the row
       }
    }
  }
}

object VirtualListView {
  def virtualListView[T](init: VirtualListView[T] ?=> Unit): VirtualListView[T] = {
    DslRuntime.build(new VirtualListView[T])(init)
  }
}
