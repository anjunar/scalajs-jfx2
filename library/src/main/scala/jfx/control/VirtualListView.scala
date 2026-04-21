package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.render.RenderBackend
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.statement.ForEach.forEach
import org.scalajs.dom
import scala.scalajs.js
import scala.collection.mutable

class VirtualListView[T] extends Box("div") {

  val itemsProperty = new ListProperty[T]()
  val estimateHeightProperty = Property(44.0)
  
  val scrollTopProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  private case class VisibleSlot(index: Int, item: T | Null, top: Double, height: Double)
  private val visibleSlotsProperty = new ListProperty[VisibleSlot]()
  
  private var itemRenderer: (T | Null, Int) => Unit = (_, _) => ()

  private val heights = mutable.Map[Int, Double]()

  private def heightFor(index: Int): Double = heights.getOrElse(index, estimateHeightProperty.get)

  private def offsetFor(index: Int): Double = {
    (0 until index).foldLeft(0.0)((sum, i) => sum + heightFor(i))
  }

  private def recomputeVisibleSlots(): Unit = {
    val top = scrollTopProperty.get
    val vh = viewportHeightProperty.get
    val itms = itemsProperty.toSeq
    val total = itms.length
    
    if (total == 0) {
      visibleSlotsProperty.setAll(Seq.empty)
    } else {
      val overscan = 240.0
      val startOffset = math.max(0.0, top - overscan)
      val endOffset = top + vh + overscan
      
      val slots = mutable.ArrayBuffer.empty[VisibleSlot]
      var currentOffset = 0.0
      var i = 0
      
      while (currentOffset < endOffset && i < total) {
        val h = heightFor(i)
        if (currentOffset + h >= startOffset) {
          slots += VisibleSlot(i, itms(i), currentOffset, h)
        }
        currentOffset += h
        i += 1
      }
      
      visibleSlotsProperty.setAll(slots.toSeq)
    }
  }

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-virtual-list")
    
    addDisposable(scrollTopProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable(viewportHeightProperty.observe(_ => recomputeVisibleSlots()))
    addDisposable(itemsProperty.observeChanges(_ => {
      heights.clear()
      recomputeVisibleSlots()
    }))

    style {
      display = "block"
      width = "100%"
      height = "100%"
      overflow = "hidden"
      position = "relative"
    }

    div {
      addClass("jfx-virtual-list-viewport")
      style { width = "100%"; height = "100%"; overflow = "auto"; position = "relative" }
      
      onScroll { e =>
        val target = e.target.asInstanceOf[dom.html.Div]
        scrollTopProperty.set(target.scrollTop)
        viewportHeightProperty.set(target.clientHeight.toDouble)
      }

      div {
        addClass("jfx-virtual-list-content")
        style {
          position = "relative"
          width = "100%"
          val totalHeight = itemsProperty.asProperty.map { itms =>
            itms.indices.foldLeft(0.0)((sum, i) => sum + heightFor(i))
          }
          height_=(totalHeight.map(h => s"${h}px"))
        }

        forEach(visibleSlotsProperty) { slot =>
          div {
            addClass("jfx-virtual-list-cell")
            style {
              position = "absolute"
              left = "0"; width = "100%"
              top = s"${slot.top}px"
              minHeight = s"${slot.height}px"
            }
            itemRenderer(slot.item, slot.index)
            measureCellAfterRender(slot)
          }
        }
      }
    }
    
    measureViewportAfterRender()
  }

  private def measureCellAfterRender(slot: VisibleSlot)(using cell: Box): Unit = {
    if (!RenderBackend.current.isServer) {
      dom.window.requestAnimationFrame { _ =>
        cell.host.domNode.foreach { node =>
          val actualHeight = node.asInstanceOf[dom.html.Div].offsetHeight.toDouble
          if (actualHeight > 0 && math.abs(actualHeight - slot.height) > 0.5) {
            heights(slot.index) = actualHeight
            recomputeVisibleSlots()
          }
        }
      }
    }
  }

  private def measureViewportAfterRender(): Unit = {
    if (!RenderBackend.current.isServer) {
      dom.window.requestAnimationFrame { _ =>
        viewportHeightProperty.set(host.clientHeight.toDouble)
        recomputeVisibleSlots()
      }
    }
  }
  
  def setRenderer(r: (T | Null, Int) => Unit): Unit = {
    itemRenderer = r
  }
}

object VirtualListView {
  def virtualList[T](items: ListProperty[T])(renderer: (T | Null, Int) => Unit): VirtualListView[T] = {
    val list = new VirtualListView[T]()
    list.itemsProperty.setAll(items.toSeq)
    list.setRenderer(renderer)
    DslRuntime.build(list) {}
  }

  def items[T](using v: VirtualListView[T]): ListProperty[T] = v.itemsProperty
  def items_=[T](it: scala.collection.IterableOnce[T])(using v: VirtualListView[T]): Unit = v.itemsProperty.setAll(it)

  def estimateHeight(using v: VirtualListView[?]): Double = v.estimateHeightProperty.get
  def estimateHeight_=(h: Double)(using v: VirtualListView[?]): Unit = v.estimateHeightProperty.set(h)
}
