package app.pages

import jfx.control.VirtualListView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object VirtualListViewPage {

  final case class ShowcaseItem(title: String, height: Double, color: String)

  def render() = {
    showcasePage("VirtualListView", "Variable Höhen, stabile Performance.") {
      vbox {
        style { gap = "24px" }
        div {
          style { opacity = "0.8"; fontSize = "14px"; marginBottom = "8px" }
          text = "Diese Demo zeigt die VirtualListView mit Elementen unterschiedlicher Höhe. Das System muss die Scroll-Position präzise berechnen, auch wenn die exakten Höhen erst beim Rendern bekannt werden."
        }

        componentShowcase("Variable Zeilenhöhen") {
          val items = new ListProperty[ShowcaseItem]()
          val data = (1 to 1000).map { i =>
            val h = if (i % 5 == 0) 120.0 else if (i % 3 == 0) 80.0 else 44.0
            val c = if (h > 100) "#fecaca" else if (h > 50) "#fed7aa" else "transparent"
            ShowcaseItem(s"Datensatz #$i", h, c)
          }
          items.setAll(data)

          vbox {
            style { height = "500px"; border = "1px solid var(--aj-surface-border)"; borderRadius = "8px"; overflow = "hidden" }
            
            virtualList(items) { (item, index) =>
              val itm = if (item == null) ShowcaseItem("Lädt...", 44.0, "transparent") else item
              div {
                style { 
                  height = s"${itm.height}px"
                  padding = "0 16px"
                  display = "flex"; alignItems = "center"
                  background = itm.color
                  borderBottom = "1px solid var(--aj-surface-subtle)"
                  boxSizing = "border-box"
                }
                div {
                  style { 
                    width = "40px"; height = "24px"; marginRight = "16px"
                    background = "var(--aj-accent)"
                    borderRadius = "4px"; display = "flex"; alignItems = "center"; justifyContent = "center"
                    color = "white"; fontSize = "11px"; fontWeight = "bold"
                  }
                  text = index.toString
                }
                div {
                  text = itm.title
                }
              }
            }
          }
        }
      }
    }
  }
}
