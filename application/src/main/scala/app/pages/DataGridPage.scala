package app.pages

import app.DemoI18n
import app.components.Showcase.*
import jfx.action.Button.button
import jfx.control.DataGrid.*
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty, RemoteListProperty}
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters.*

object DataGridPage {

  final case class ShowcaseTile(title: String, category: String, summary: String, accent: String)
  final case class ShowcaseTileQuery(index: Int, limit: Int)

  object ShowcaseTileQuery {
    def first(limit: Int): ShowcaseTileQuery =
      ShowcaseTileQuery(index = 0, limit = math.max(1, limit))
  }

  private val showcaseTileCatalog: Vector[(String, String, String, String)] = Vector(
    ("Atlas Memo", "Research", "Dense notes, references, and open questions in one card.", "#2563eb"),
    ("Northwind", "Commerce", "A product teaser with enough structure for catalog browsing.", "#0f766e"),
    ("Signal Room", "Operations", "Metrics and ownership should still feel quiet at scale.", "#9333ea"),
    ("Amber Draft", "Editorial", "A story card with room for title, deck, and routing metadata.", "#ea580c"),
    ("Mint Ledger", "Finance", "Stable dimensions make grids predictable for dashboards too.", "#059669"),
    ("Violet Tape", "Archive", "Even long collections stay light when only the window is rendered.", "#7c3aed")
  )

  def buildShowcaseTiles(count: Int): Seq[ShowcaseTile] = {
    val catalog = showcaseTileCatalog
    val size = catalog.length
    (0 until count).map { index =>
      val (title, category, summary, accent) = catalog(index % size)
      ShowcaseTile(
        title = s"$title ${index + 1}",
        category = category,
        summary = summary,
        accent = accent
      )
    }
  }

  def createRemoteTiles(
    pageSize: Int = 24
  )(using executionContext: ExecutionContext): RemoteListProperty[ShowcaseTile, ShowcaseTileQuery] = {
    val normalizedPageSize = math.max(1, pageSize)

    ListProperty.remote[ShowcaseTile, ShowcaseTileQuery](
      loader = ListProperty.RemoteLoader { query =>
        Future {
          val tiles = buildShowcaseTiles(180).toVector
          val rows = tiles.slice(query.index, query.index + query.limit)
          val nextIndex = query.index + rows.length

          ListProperty.RemotePage[ShowcaseTile, ShowcaseTileQuery](
            items = rows,
            offset = Some(query.index),
            nextQuery =
              if (nextIndex < tiles.length) Some(query.copy(index = nextIndex, limit = normalizedPageSize))
              else None,
            totalCount = Some(tiles.length),
            hasMore = Some(nextIndex < tiles.length)
          )
        }.toJSPromise
      },
      initialQuery = ShowcaseTileQuery.first(normalizedPageSize),
      executionContext = executionContext,
      rangeQueryUpdater = Some((query, index, limit) =>
        query.copy(index = index, limit = math.max(1, limit))
      )
    )
  }

  def render(remoteTiles: RemoteListProperty[ShowcaseTile, ShowcaseTileQuery]) = {
    showcasePage(i18n"DataGrid", i18n"Virtual cards with a calm scroll story.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Grid virtualization",
          i18n"Card collections should stay light even when they grow.",
          i18n"DataGrid is for predictable tile grids: media cards, article teasers, catalog entries. It derives the column count from a preferred card width, stretches the rendered cards to the viewport, and keeps the row height deterministic."
        )

        metricStrip(
          i18n"320x180" -> i18n"Preferred footprint with viewport-scaled width.",
          i18n"16px" -> i18n"Gap keeps density readable.",
          i18n"Prefetch" -> i18n"Remote windows load before the user reaches them."
        )

        componentShowcase(
          i18n"Local DataGrid",
          i18n"Selection, keyboard access, click, double click, and local mutations in one interactive grid."
        ) {
          val localItems = ListProperty[ShowcaseTile]()
          localItems.setAll(buildShowcaseTiles(18))
          val cardCount = Property(localItems.length)
          val selectedIndex = Property(-1)
          val interactionText = Property(DemoI18n.resolveNow(i18n"Click a card or double click it."))

          vbox {
            style { gap = "16px" }

            div {
              style {
                height = "520px"
                border = "1px solid var(--aj-line)"
                borderRadius = "8px"
                overflow = "hidden"
              }

              dataGrid(localItems, itemWidthPx = 220, itemHeightPx = 180, gapPx = 16, overscanRows = 1) { (item, index) =>
                renderTile(
                  item,
                  index,
                  selected = selectedIndex.map(_ == index),
                  onTileClick = tile => {
                    selectedIndex.set(index)
                    interactionText.set(
                      DemoI18n.resolveNow(i18n"Click on ${I18n.named("title", tile.title)} at index ${I18n.named("index", index + 1)}")
                    )
                  },
                  onTileDoubleClick = tile => {
                    selectedIndex.set(index)
                    interactionText.set(
                      DemoI18n.resolveNow(i18n"Double click on ${I18n.named("title", tile.title)} at index ${I18n.named("index", index + 1)}")
                    )
                  }
                )
              }
            }

            hbox {
              classes = Seq("showcase-action-row")
              button(DemoI18n.text(i18n"Add card")) {
                onClick { _ =>
                  val next = buildShowcaseTiles(localItems.length + 1).last
                  localItems += next
                  cardCount.set(localItems.length)
                }
              }
              button(DemoI18n.text(i18n"Reset grid")) {
                onClick { _ =>
                  localItems.setAll(buildShowcaseTiles(18))
                  cardCount.set(localItems.length)
                  selectedIndex.set(-1)
                  interactionText.set(DemoI18n.resolveNow(i18n"Click a card or double click it."))
                }
              }
            }

            div {
              classes = Seq("showcase-result")
              text =
                interactionText.flatMap { interaction =>
                  cardCount.map { count =>
                    val base = DemoI18n.resolveNow(i18n"Total cards: ${I18n.named("count", count)}")
                    s"$base | $interaction"
                  }
                }
            }
          }
        }

        apiSection(
          i18n"DataGrid DSL",
          i18n"The control owns virtualization; the renderer only describes one card."
        ) {
          codeBlock("scala", """val posts = ListProperty[Post]()
posts.setAll(seedPosts)

dataGrid(posts, itemWidthPx = 320, itemHeightPx = 180, gapPx = 16, crawlable = true) { (post, index) =>
  div {
    classes = Seq("post-card")
    text = if (post == null) s"Loading $index" else post.title
  }
}""")
        }

        componentShowcase(
          i18n"Range-aware grid",
          i18n"RemoteListProperty can hydrate the visible window and keep SSR crawl pages honest."
        ) {
          div {
            style {
              height = "520px"
              border = "1px solid var(--aj-line)"
              borderRadius = "8px"
              overflow = "hidden"
            }

            dataGrid(
              remoteTiles,
              itemWidthPx = 240,
              itemHeightPx = 196,
              gapPx = 16,
              overscanRows = 1,
              prefetchItems = 24,
              crawlable = true
            ) { (item, index) =>
              renderTile(item, index)
            }
          }
        }

        insightGrid(
          (i18n"Shared sizing", i18n"Same row math, flexible width", i18n"Preferred width chooses the column count; rendered width stretches to fill the viewport."),
          (i18n"Windowed loading", i18n"Prefetch stays close to view", i18n"The grid can request nearby ranges before the user hits empty space."),
          (i18n"Crawlable SSR", i18n"Real pagination for crawlers", i18n"offset and limit describe a repeatable HTML window while the client still upgrades to free scrolling.")
        )

        componentShowcase(
          i18n"Shared dimensions are the contract",
          i18n"DataGrid keeps height fixed and distributes the card width across the measured viewport."
        ) {
          div {
            style { display = "flex"; gap = "14px"; flexWrap = "wrap" }
            logicCard(
              "contentWidth / contentHeight",
              i18n"Derived from column count, rendered tile width, fixed height, and gap to size the scroll surface."
            )
            logicCard(
              "visibleRange",
              i18n"Calculates start and end from scrollTop, viewportHeight, column count, and overscan rows."
            )
            logicCard(
              "prefetchItems",
              i18n"Defines how far remote loading reaches beyond the visible cards."
            )
            logicCard(
              "crawlable",
              i18n"In SSR the grid uses offset/limit and renders a real More link for the next window."
            )
          }
        }

        apiSection(
          i18n"Remote loader",
          i18n"The loader only needs to answer index/limit requests with totalCount and nextQuery."
        ) {
          codeBlock("scala", """ListProperty.remote[Post, PageQuery](
  loader = ListProperty.RemoteLoader { query =>
    fetchPosts(query.index, query.limit)
  },
  initialQuery = PageQuery(index = 0, limit = 24),
  rangeQueryUpdater = Some((query, index, limit) =>
    query.copy(index = index, limit = limit)
  )
)""")
        }

        apiSection(
          i18n"Crawlable window",
          i18n"The server renders a stable slice, while the browser restores scroll position from the same query."
        ) {
          codeBlock("text", """/data-grid?offset=0&limit=24
  renders items 0 to 23

/data-grid?offset=24&limit=24
  renders items 24 to 47

Browser:
  scrollTop starts at the row that contains offset

SSR:
  More link points to offset + limit""")
        }
      }
    }
  }

  private def renderTile(
    item: ShowcaseTile | Null,
    index: Int,
    selected: ReadOnlyProperty[Boolean] = Property(false),
    onTileClick: ShowcaseTile => Unit = _ => (),
    onTileDoubleClick: ShowcaseTile => Unit = _ => ()
  ): Unit = {
    val tile = Option(item)
    val accent = tile.map(_.accent).getOrElse("var(--aj-line)")

    vbox {
      classes = Seq("data-grid-showcase-card")
      classIf("data-grid-showcase-card--selected", selected)

      tile.foreach { value =>
        role = "button"
        tabIndex = 0
        attribute("aria-selected", selected.map(_.toString))
        onClick { _ => onTileClick(value) }
        onDoubleClick { _ => onTileDoubleClick(value) }
        onKeyDown { event =>
          event.key match {
            case "Enter" =>
              event.preventDefault()
              onTileClick(value)
            case " " | "Spacebar" =>
              event.preventDefault()
              onTileClick(value)
            case _ =>
          }
        }
      }

      style {
        height = "100%"
        gap = "12px"
        padding = "16px"
        borderRadius = "8px"
        boxSizing = "border-box"
        overflow = "hidden"
        cursor = if (tile.nonEmpty) "pointer" else "default"
      }

      div {
        style {
          height = "4px"
          borderRadius = "999px"
          background = accent
        }
      }

      div {
        style {
          display = "flex"
          justifyContent = "space-between"
          alignItems = "center"
          gap = "10px"
        }

        div {
          style { fontSize = "0.74rem"; fontWeight = "800"; color = "var(--aj-ink-muted)" }
          text = selected.map { isSelected =>
            if (isSelected) DemoI18n.resolveNow(i18n"Selected")
            else tile.map(_.category).getOrElse(DemoI18n.resolveNow(i18n"Loading..."))
          }
        }

        div {
          style { fontSize = "0.8rem"; color = "var(--aj-ink-faint)" }
          text = s"#${index + 1}"
        }
      }

      div {
        style { fontSize = "1.12rem"; fontWeight = "820" }
        text = tile.map(_.title).getOrElse(s"Loading tile ${index + 1}")
      }

      div {
        style {
          color = "var(--aj-ink-soft)"
          flex = "1 1 auto"
          minHeight = "0"
          overflow = "hidden"
        }
        text = tile.map(_.summary).getOrElse(DemoI18n.resolveNow(i18n"Loading..."))
      }

      div {
        style {
          flex = "0 0 auto"
          marginTop = "auto"
          color = accent
          fontWeight = "700"
          overflow = "hidden"
        }
        text = DemoI18n.resolveNow(i18n"Open window")
      }
    }
  }

  private def logicCard(title: String, body: RuntimeMessage): Unit = {
    vbox {
      classes = Seq("showcase-result")
      style { gap = "8px"; flex = "1 1 260px"; marginTop = "0" }
      div {
        style { fontWeight = "800" }
        text = title
      }
      div {
        style { color = "var(--aj-ink-muted)" }
        text = DemoI18n.text(body)
      }
    }
  }
}
