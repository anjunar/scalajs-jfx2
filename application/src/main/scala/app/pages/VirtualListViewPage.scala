package app.pages

import app.DemoI18n
import jfx.control.VirtualListView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object VirtualListViewPage {

  final case class ShowcaseItem(title: String, height: Double, color: String)

  def render() = {
    showcasePage(i18n"VirtualListView", i18n"Variable heights, stable performance.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Virtualization",
          i18n"Many rows should still feel light.",
          i18n"This demo shows elements with different heights. The list must keep scroll position, visible range, and placeholders aligned even though only a slice is actually rendered."
        )

        metricStrip(
          DemoI18n.resolveNow(i18n"1000") -> DemoI18n.resolveNow(i18n"records in the showcase"),
          DemoI18n.resolveNow(i18n"44-120px") -> DemoI18n.resolveNow(i18n"variable row heights for real layout tension"),
          DemoI18n.resolveNow(i18n"Viewport") -> DemoI18n.resolveNow(i18n"Only what the user needs right now is rendered")
        )

        componentShowcase(
          i18n"Variable row heights",
          i18n"Short, medium, and tall rows test whether the scrollbar stays stable."
        ) {
          val items = new ListProperty[ShowcaseItem]()
          val data = (1 to 1000).map { i =>
            val h = if (i % 5 == 0) 120.0 else if (i % 3 == 0) 80.0 else 44.0
            val c = if (h > 100) "#fecaca" else if (h > 50) "#fed7aa" else "transparent"
            ShowcaseItem(DemoI18n.resolveNow(i18n"Record #$i"), h, c)
          }
          items.setAll(data)

          vbox {
            style { height = "500px"; border = "1px solid var(--aj-line)"; borderRadius = "8px"; overflow = "hidden" }

            virtualList(items, estimateHeightPx = 64, crawlable = true) { (itemOrNull, index) =>
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
                text = if (item != null) s"$index - ${item.title}" else s"$index - ${DemoI18n.resolveNow(i18n"Loading...")}"
              }
            }
          }
        }

        insightGrid(
          (i18n"Cursor", i18n"Only visible children count", i18n"Virtual containers must insert and remove physical DOM nodes in a controlled way."),
          (i18n"SEO", i18n"SSR renders a crawl window", i18n"With crawlable = true the list uses offset/limit and gives crawlers real next pages."),
          (i18n"Heights", i18n"Estimation and measurement must agree", i18n"Variable row heights must not make scroll position jump."),
          (i18n"Data", i18n"Lists remain properties", i18n"When data changes, the view reacts without manual DOM synchronization in the template.")
        )

        apiSection(
          i18n"VirtualList usage",
          i18n"The row function describes only the visible content. Virtualization remains the component's job."
        ) {
          codeBlock("scala", """val items = new ListProperty[ShowcaseItem]()

virtualList(items, estimateHeightPx = 64, crawlable = true) { (item, index) =>
  div {
    style { height = s"${item.height}px" }
    text = s"$index - ${item.title}"
  }
}""")
        }

        apiSection(
          i18n"Crawlable VirtualList",
          i18n"As with the TableView, SSR renders a stable window and links to the next page."
        ) {
          codeBlock("text", """/virtual-list?offset=0&limit=50
  rendert Items 0 bis 49

/virtual-list?offset=50&limit=50
  rendert Items 50 bis 99

Browser:
  scrollTop startet bei offset * estimateHeight

SSR:
  More-Link zeigt auf offset + limit""")
        }

        apiSection(
          i18n"Async route usage",
          i18n"The route remains the SSR shell. The VirtualList itself is configured with crawlable = true and reads offset/limit from the route context."
        ) {
          codeBlock("scala", """asyncRoute("/virtual-list") {
  page {
    VirtualListViewPage.render()
  }
}

// In der Page:
virtualList(items, estimateHeightPx = 64, crawlable = true) { (item, index) =>
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
