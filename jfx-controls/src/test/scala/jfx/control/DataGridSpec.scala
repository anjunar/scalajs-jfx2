package jfx.control

import jfx.control.DataGrid.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.Div.div
import jfx.router.Route.{asyncRoute, page}
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class DataGridSpec extends AnyFlatSpec with Matchers {

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

  "DataGrid SSR" should "render only the visible local grid cells" in {
    val items = ListProperty[String]()
    items.setAll((0 until 30).map(index => s"Item $index"))

    val html = Ssr.renderToString {
      dataGrid(items, itemWidthPx = 400, itemHeightPx = 100, gapPx = 0, overscanRows = 0) { (item, index) =>
        div {
          text = s"$index:${Option(item).getOrElse("loading")}"
        }
      }
    }

    html should include("jfx-data-grid")
    html should include("0:Item 0")
    html should include("9:Item 9")
    html should not include "10:Item 10"
  }

  it should "stretch fixed grid columns to the viewport width" in {
    val items = ListProperty[String]()
    items.setAll((0 until 4).map(index => s"Item $index"))

    val html = Ssr.renderToString {
      dataGrid(items, itemWidthPx = 300, itemHeightPx = 100, gapPx = 20, overscanRows = 0) { (item, index) =>
        div {
          text = s"$index:${Option(item).getOrElse("loading")}"
        }
      }
    }

    html should include regex "width: 390(?:\\.0)?px"
    html should include regex "left: 410(?:\\.0)?px"
  }

  it should "render unloaded remote ranges as placeholder cells" in {
    val remote = remoteMembers(pageSize = 5)
    remote.totalCountProperty.set(Some(20))
    remote.hasMoreProperty.set(true)

    val html = Ssr.renderToString {
      dataGrid(remote, itemWidthPx = 400, itemHeightPx = 100, gapPx = 0, overscanRows = 0) { (item, index) =>
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

    html should include("jfx-data-grid-cell-loading")
    html should include("remote-placeholder")
    html should include("Loading 5")
    html should include("left: 400px")
  }

  it should "render crawlable windows from query params" in {
    val items = ListProperty[String]()
    items.setAll((0 until 20).map(index => s"Member $index"))

    val html = Ssr.renderToString {
      router(Seq(
        asyncRoute("/") {
          page {
            dataGrid(items, itemWidthPx = 400, itemHeightPx = 100, gapPx = 0, overscanRows = 0, crawlable = true) { (item, index) =>
              div {
                text = s"$index:${Option(item).getOrElse("loading")}"
              }
            }
          }
        }
      ), "/?offset=5&limit=4")
    }

    html should not include "4:Member 4"
    html should include("5:Member 5")
    html should include("8:Member 8")
    html should not include "9:Member 9"
    html should include("top: 200px")
    html should include("href=\"?offset=9&amp;limit=4\"")
  }
}
