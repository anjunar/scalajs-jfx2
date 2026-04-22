package jfx.control

import jfx.control.TableColumn.*
import jfx.control.TableView.*
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.router.Route.{asyncRoute, page}
import jfx.router.Router.router
import jfx.ssr.Ssr
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class TableViewRemoteListSpec extends AnyFlatSpec with Matchers {

  private final case class PageQuery(
    index: Int,
    limit: Int,
    sorting: Vector[ListProperty.RemoteSort] = Vector.empty
  )

  private def remoteMembers(pageSize: Int): jfx.core.state.RemoteListProperty[String, PageQuery] = {
    val members = (0 until 20).map(index => s"Member $index")

    ListProperty.remote[String, PageQuery](
      loader = ListProperty.RemoteLoader { query =>
        val sorted =
          query.sorting.headOption match {
            case Some(sort) if sort.field == "name" && !sort.ascending => members.reverse
            case _                                                      => members
          }
        val items = sorted.slice(query.index, query.index + query.limit)
        val nextIndex = query.index + items.length

        js.Promise.resolve(
          ListProperty.RemotePage[String, PageQuery](
            items = items,
            offset = Some(query.index),
            nextQuery =
              if (nextIndex < sorted.length) Some(query.copy(index = nextIndex, limit = pageSize))
              else None,
            totalCount = Some(sorted.length),
            hasMore = Some(nextIndex < sorted.length)
          )
        )
      },
      initialQuery = PageQuery(0, pageSize),
      sortUpdater = Some((query, sorting) => query.copy(index = 0, limit = pageSize, sorting = sorting.toVector)),
      rangeQueryUpdater = Some((query, index, limit) => query.copy(index = index, limit = limit))
    )
  }

  "TableView with RemoteListProperty" should "render unloaded remote ranges as placeholder rows in SSR" in {
    val remote = remoteMembers(pageSize = 5)
    remote.totalCountProperty.set(Some(20))
    remote.hasMoreProperty.set(true)

    val html = Ssr.renderToString {
      router(Seq(
        asyncRoute("/") {
          page {
            tableView[String] {
              crawlable = true
              items = remote
              column[String, String]("Name") { item =>
                text = item
              }
            }
          }
        }
      ), "/?offset=5&limit=5")
    }

    html should include("jfx-table-cell-loading-placeholder")
    html should include("top: 160px")
    html should include("href=\"?offset=10&amp;limit=5\"")
  }

  it should "reflect remote sorting state in the header" in {
    val remote = remoteMembers(pageSize = 5)
    remote.sortingProperty.set(Vector(ListProperty.RemoteSort("name", ascending = false)))

    val html = Ssr.renderToString {
      tableView[String] {
        items = remote
        column[String, String]("Name")((current: TableColumn[String, String]) ?=>
          current.sortableProperty.set(true)
          current.sortKeyProperty.set(Some("name"))
        )
      }
    }

    html should include("jfx-table-header-cell-sortable")
    html should include("jfx-table-header-cell-sorted")
    html should include("jfx-table-header-cell-sorted-desc")
  }
}
