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
        style { gap = "34px" }

        sectionIntro(
          "Virtualisierung",
          "Viele Zeilen dürfen sich leicht anfühlen.",
          "Diese Demo zeigt Elemente unterschiedlicher Höhe. Die Liste muss Scroll-Position, sichtbaren Bereich und Platzhalter sauber führen, obwohl nur ein Ausschnitt wirklich gerendert wird."
        )

        metricStrip(
          "1000" -> "Datensätze im Showcase.",
          "44-120px" -> "Variable Zeilenhöhen für echte Layoutspannung.",
          "Viewport" -> "Gerendert wird nur, was der Benutzer gerade braucht."
        )

        componentShowcase(
          "Variable Zeilenhöhen",
          "Kurze, mittlere und hohe Zeilen prüfen, ob die Scrollbar stabil bleibt."
        ) {
          val items = new ListProperty[ShowcaseItem]()
          val data = (1 to 1000).map { i =>
            val h = if (i % 5 == 0) 120.0 else if (i % 3 == 0) 80.0 else 44.0
            val c = if (h > 100) "#fecaca" else if (h > 50) "#fed7aa" else "transparent"
            ShowcaseItem(s"Datensatz #$i", h, c)
          }
          items.setAll(data)

          vbox {
            style { height = "500px"; border = "1px solid var(--aj-line)"; borderRadius = "8px"; overflow = "hidden" }

            virtualList(items) { (itemOrNull, index) =>
              val item = itemOrNull.asInstanceOf[ShowcaseItem]
              div {
                style {
                  height = if (item != null) s"${item.height}px" else "44px"
                  backgroundColor = if (item != null) item.color else "transparent"
                  display = "flex"
                  alignItems = "center"
                  padding = "0 16px"
                  borderBottom = "1px solid var(--aj-line-faint)"
                }
                text = if (item != null) s"$index - ${item.title}" else s"$index - Lade..."
              }
            }
          }
        }

        insightGrid(
          ("Cursor", "Nur sichtbare Kinder zählen", "Virtuelle Container müssen physische DOM-Knoten kontrolliert einfügen und entfernen."),
          ("Höhen", "Schätzung und Messung müssen zusammenpassen", "Variable Zeilenhöhen dürfen die Scrollposition nicht springen lassen."),
          ("Daten", "Listen bleiben Properties", "Wenn sich die Daten ändern, reagiert die Ansicht ohne manuelle DOM-Synchronisation im Template.")
        )

        apiSection(
          "VirtualList Usage",
          "Die Zeilenfunktion beschreibt nur den sichtbaren Inhalt. Die Virtualisierung bleibt Aufgabe der Komponente."
        ) {
          codeBlock("scala", """val items = new ListProperty[ShowcaseItem]()

virtualList(items) { (item, index) =>
  div {
    style { height = s"${item.height}px" }
    text = s"$index - ${item.title}"
  }
}""")
        }
      }
    }
  }
}
