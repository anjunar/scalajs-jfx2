package jfx.control

import jfx.core.component.{Box, Component}
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import jfx.statement.ForEach.forEach
import org.scalajs.dom

class TableView[S] extends Box("div") {
  val items = new ListProperty[S]()
  val columns = new ListProperty[TableColumn[S, ?]]()
  val showHeaderProperty = Property(true)
  
  val rowHeightProperty = Property(32.0)
  val scrollTopProperty = Property(0.0)
  val viewportHeightProperty = Property(400.0)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-table-view")
    
    // 1. Header
    jfx.statement.Condition.condition(showHeaderProperty) {
      jfx.statement.Condition.thenDo {
        Box.box("div") {
          addClass("jfx-table-header")
          style {
            display = "flex"
            width = "100%"
          }
          forEach(columns) { col =>
            Box.box("div") {
              addClass("jfx-table-header-cell")
              style {
                width = s"${col.prefWidth}px"
                minWidth = s"${col.prefWidth}px"
                flex = "0 0 auto"
              }
              text = col.text
            }
          }
        }
      }
    }

    // 2. Body with Virtual Scroll
    Box.box("div") {
      addClass("jfx-table-viewport")
      style {
        flex = "1"
        overflowY = "auto"
        position = "relative"
      }
      
      // Listen to scroll events
      addDisposable(host.addEventListener("scroll", (e: dom.Event) => {
        val target = e.target.asInstanceOf[dom.html.Div]
        scrollTopProperty.set(target.scrollTop)
      }))

      Box.box("div") {
        addClass("jfx-table-content")
        
        // Dynamic height based on items
        addDisposable(items.observe { list =>
          style {
            height = s"${list.length * rowHeightProperty.get}px"
            width = "100%"
            position = "relative"
          }
        })

        // Virtualized rows
        val visibleItems = Property(Seq.empty[(Int, S)])

        def updateVisibleRange(): Unit = {
          val scrollTop = scrollTopProperty.get
          val viewportHeight = viewportHeightProperty.get
          val rowHeight = rowHeightProperty.get
          val totalItems = items.length
          
          if (totalItems == 0) {
            visibleItems.set(Seq.empty)
            return
          }

          val startIdx = Math.max(0, Math.floor(scrollTop / rowHeight).toInt)
          val endIdx = Math.min(totalItems - 1, Math.ceil((scrollTop + viewportHeight) / rowHeight).toInt)
          
          val visible = (startIdx to endIdx).map(i => (i, items(i)))
          visibleItems.set(visible)
        }

        addDisposable(scrollTopProperty.observe(_ => updateVisibleRange()))
        addDisposable(items.observeChanges(_ => updateVisibleRange()))
        
        // Initial range
        updateVisibleRange()

        // Render visible rows
        // We use forEach on a ListProperty of visible items
        val visibleList = new ListProperty[(Int, S)]()
        addDisposable(visibleItems.observe(v => visibleList.setAll(v)))

        forEach(visibleList) { (idx, item) =>
          val row = new TableRow[S]()
          DslRuntime.build(row) {
            row.bind(idx, item, this, columns.get.toSeq, rowHeightProperty.get)
          }
        }
      }
    }
  }
}

object TableView {
  def tableView[S](init: TableView[S] ?=> Unit): TableView[S] = {
    DslRuntime.build(new TableView[S])(init)
  }

  def items[S](using t: TableView[S]): ListProperty[S] = t.items
}
