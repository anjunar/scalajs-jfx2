package jfx.statement

import jfx.core.component.Component
import jfx.core.render.HydrationRenderBackend
import jfx.core.render.RenderBackend
import jfx.core.state.ReadOnlyProperty
import jfx.dsl.DslRuntime

class DynamicOutlet(
  val content: ReadOnlyProperty[? <: Component | Null]
) extends Component {

  override def tagName: String = ""

  private var mounted: Component | Null = null

  override def compose(): Unit = {
    addDisposable(content.observe(_ => reconcile()))
  }

  override def dispose(): Unit = {
    detachMounted()
    super.dispose()
  }

  private def reconcile(): Unit = {
    DslRuntime.updateBranch(this) {
      val next = content.get.asInstanceOf[Component | Null]

      if (mounted eq next) {
        ()
      } else {
        detachMounted()
        mounted = null

        if (next != null) {
          attachMounted(next)
          mounted = next
        }
      }
    }
  }

  private def attachMounted(child: Component): Unit = {
    child.parent.foreach { oldParent =>
      if (oldParent != this) {
        oldParent.removeChild(child)
      }
    }

    if (!children.contains(child)) {
      child.setParent(Some(this))
      addChild(child)

      if (!RenderBackend.current.isInstanceOf[HydrationRenderBackend]) {
        syncChildAddition(child)
      }
    }
  }

  private def detachMounted(): Unit = {
    val child = mounted
    if (child != null && children.contains(child)) {
      removeChild(child)
      child.setParent(None)
    }
  }
}

object DynamicOutlet {

  def apply(content: ReadOnlyProperty[? <: Component | Null]): DynamicOutlet =
    new DynamicOutlet(content)

  def outlet(content: ReadOnlyProperty[? <: Component | Null]): DynamicOutlet =
    DslRuntime.build(new DynamicOutlet(content)) {}

  def dynamicOutlet(content: ReadOnlyProperty[? <: Component | Null]): DynamicOutlet =
    outlet(content)

  def outletContent(using outlet: DynamicOutlet): ReadOnlyProperty[? <: Component | Null] =
    outlet.content
}
