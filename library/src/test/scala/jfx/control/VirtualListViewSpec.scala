package jfx.control

import jfx.control.VirtualListView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.Viewport.viewport
import jfx.router.Route.{asyncRoute, page}
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class VirtualListViewSpec extends AnyFlatSpec with Matchers {

  private final case class PageQuery(index: Int, limit: Int)

  private def remoteMembers(pageSize: Int): jfx.core.state.RemoteListProperty[String, PageQuery] = {
    val members = (0 until 20).map(index => s"Member $index")

    ListProperty.remote[String, PageQuery](
      loader = ListProperty.RemoteLoader { query =>
        val items = members.slice(query.index, query.index + query.limit)
        val nextIndex = query.index + items.length

        js.Promise.resolve(
          ListProperty.RemotePage[String, PageQuery](
            items = items,
            offset = Some(query.index),
            nextQuery =
              if (nextIndex < members.length) Some(PageQuery(nextIndex, pageSize))
              else None,
            totalCount = Some(members.length),
            hasMore = Some(nextIndex < members.length)
          )
        )
      },
      initialQuery = PageQuery(0, pageSize),
      rangeQueryUpdater = Some((_, index, limit) => PageQuery(index, limit))
    )
  }

  "VirtualListView SSR" should "render local items from the assigned ListProperty" in {
    val items = ListProperty[String]()
    items.setAll((0 until 30).map(index => s"Item $index"))

    val html = Ssr.renderToString {
      virtualList(items, estimateHeightPx = 40, overscanPx = 0, prefetchItems = 4) { (item, index) =>
        div {
          text = s"$index:${Option(item).getOrElse("loading")}"
        }
      }
    }

    html should include("jfx-virtual-list")
    html should include("0:Item 0")
    html should include("9:Item 9")
    html should not include "10:Item 10"
  }

  it should "render unloaded remote ranges as placeholder cells" in {
    val remote = remoteMembers(pageSize = 5)
    remote.totalCountProperty.set(Some(20))
    remote.hasMoreProperty.set(true)

    val html = Ssr.renderToString {
      virtualList(remote, estimateHeightPx = 40, overscanPx = 0, prefetchItems = 4) { (item, index) =>
        div {
          if (item == null) {
            addClass("remote-placeholder")
            text = s"Loading $index"
          } else {
            text = s"$index:$item"
          }
        }
      }
    }

    html should include("jfx-virtual-list-cell-loading")
    html should include("remote-placeholder")
    html should include("Loading 5")
    html should include("top: 200px")
  }

  it should "render nested row content in SSR" in {
    final case class Row(title: String, height: Double)
    val items = ListProperty[Row]()
    items.setAll((1 to 20).map(index => Row(s"Datensatz #$index", if (index % 3 == 0) 80.0 else 44.0)))

    val html = Ssr.renderToString {
      virtualList(items, estimateHeightPx = 44, overscanPx = 0, prefetchItems = 4) { (item, index) =>
        val row = if (item == null) Row("Lädt...", 44.0) else item
        div {
          style {
            height = s"${row.height}px"
            display = "flex"
            alignItems = "center"
          }
          div {
            text = index.toString
          }
          div {
            text = row.title
          }
        }
      }
    }

    html should include("Datensatz #1")
    html should include("Datensatz #9")
  }

  it should "render inside a fixed-height wrapper in SSR" in {
    final case class Row(title: String, height: Double, color: String)
    val items = new ListProperty[Row]()
    items.setAll((1 to 1000).map { index =>
      val height = if (index % 5 == 0) 120.0 else if (index % 3 == 0) 80.0 else 44.0
      Row(s"Datensatz #$index", height, "transparent")
    })

    val html = Ssr.renderToString {
      vbox {
        style {
          height = "500px"
          overflow = "hidden"
        }

        virtualList(items) { (item, index) =>
          val row = if (item == null) Row("Lädt...", 44.0, "transparent") else item
          div {
            style {
              height = s"${row.height}px"
              padding = "0 16px"
              display = "flex"
              alignItems = "center"
              background = row.color
              boxSizing = "border-box"
            }
            div {
              text = index.toString
            }
            div {
              text = row.title
            }
          }
        }
      }
    }

    html should include("Datensatz #1")
    html should include("Datensatz #10")
  }

  it should "render through router and viewport in SSR" in {
    val items = new ListProperty[String]()
    items.setAll((1 to 1000).map(index => s"Datensatz #$index"))

    val html = Ssr.renderToString {
      viewport {
        router(Seq(
          asyncRoute("/virtual-list") {
            page {
              virtualList(items) { (item, index) =>
                div {
                  div {
                    text = index.toString
                  }
                  div {
                    text = if (item == null) "Lädt..." else item
                  }
                }
              }
            }
          }
        ), "/virtual-list")
      }
    }

    html should include("Datensatz #1")
  }

  it should "render inside a by-name showcase helper in SSR" in {
    def componentShowcase(title: String)(content: => Unit): Unit =
      vbox {
        div { text = title }
        div {
          content
        }
      }

    val items = new ListProperty[String]()
    items.setAll((1 to 1000).map(index => s"Datensatz #$index"))

    val html = Ssr.renderToString {
      vbox {
        componentShowcase("Virtual") {
          vbox {
            style { height = "500px" }
            virtualList(items) { (item, index) =>
              div {
                text = s"$index:${if (item == null) "Lädt..." else item}"
              }
            }
          }
        }
        div { text = "after" }
      }
    }

    html should include("0:Datensatz #1")
    html should include("after")
  }

  it should "render a Unit page through router in SSR" in {
    def pageContent(): Unit = {
      val items = new ListProperty[String]()
      items.setAll((1 to 1000).map(index => s"Datensatz #$index"))

      vbox {
        div { text = "before" }
        virtualList(items) { (item, index) =>
          div {
            text = s"$index:${if (item == null) "Lädt..." else item}"
          }
        }
      }
    }

    val html = Ssr.renderToString {
      viewport {
        router(Seq(
          asyncRoute("/virtual-list") {
            page {
              pageContent()
            }
          }
        ), "/virtual-list")
      }
    }

    html should include("before")
    html should include("0:Datensatz #1")
  }
}
