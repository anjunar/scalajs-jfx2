package app.pages

import jfx.action.Button.button
import jfx.control.TableColumn.column
import jfx.control.TableView.*
import jfx.control.{TableColumn, TableView}
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.DemoI18n
import app.components.Showcase.*
import jfx.i18n.*

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters.*

object TableViewPage {

  final case class ShowcaseBook(title: String, author: String, year: Int)
  final case class ShowcaseBookQuery(
    index: Int,
    limit: Int,
    sorting: Vector[ListProperty.RemoteSort] = Vector.empty
  )

  object ShowcaseBookQuery {
    def first(limit: Int, sorting: Vector[ListProperty.RemoteSort] = Vector.empty): ShowcaseBookQuery =
      ShowcaseBookQuery(index = 0, limit = math.max(1, limit), sorting = sorting)
  }

  private val showcaseBookCatalog: Vector[(String, String, Int)] = Vector(
    ("Der Hobbit", "J. R. R. Tolkien", 1937),
    ("Der Herr der Ringe", "J. R. R. Tolkien", 1954),
    ("1984", "George Orwell", 1949),
    ("Farm der Tiere", "George Orwell", 1945),
    ("Faust. Eine Tragödie", "Johann Wolfgang von Goethe", 1808),
    ("Siddhartha", "Hermann Hesse", 1922),
    ("Der Steppenwolf", "Hermann Hesse", 1927),
    ("Der Prozess", "Franz Kafka", 1925),
    ("Das Schloss", "Franz Kafka", 1926),
    ("Die Verwandlung", "Franz Kafka", 1915),
    ("Im Westen nichts Neues", "Erich Maria Remarque", 1929),
    ("Arc de Triomphe", "Erich Maria Remarque", 1945),
    ("Buddenbrooks", "Thomas Mann", 1901),
    ("Der Zauberberg", "Thomas Mann", 1924),
    ("Doktor Faustus", "Thomas Mann", 1947),
    ("Der Tod in Venedig", "Thomas Mann", 1912),
    ("Die Verlobung im St. Domingo", "Heinrich von Kleist", 1811),
    ("Michael Kohlhaas", "Heinrich von Kleist", 1810),
    ("Kabale und Liebe", "Friedrich Schiller", 1784),
    ("Die Räuber", "Friedrich Schiller", 1781),
    ("Wallenstein", "Friedrich Schiller", 1799),
    ("Emilia Galotti", "Gotthold Ephraim Lessing", 1772),
    ("Nathan der Weise", "Gotthold Ephraim Lessing", 1779),
    ("Minna von Barnhelm", "Gotthold Ephraim Lessing", 1767),
    ("Effi Briest", "Theodor Fontane", 1895),
    ("Der Stechlin", "Theodor Fontane", 1899),
    ("Irrungen, Wirrungen", "Theodor Fontane", 1888),
    ("Die Leiden des jungen Werther", "Johann Wolfgang von Goethe", 1774),
    ("Wilhelm Meisters Lehrjahre", "Johann Wolfgang von Goethe", 1795),
    ("Wahlverwandtschaften", "Johann Wolfgang von Goethe", 1809),
    ("Ansichten eines Clowns", "Heinrich Böll", 1963),
    ("Die verlorene Ehre der Katharina Blum", "Heinrich Böll", 1974),
    ("Gruppenbild mit Dame", "Heinrich Böll", 1971),
    ("Das Parfum", "Patrick Süskind", 1985),
    ("Homo faber", "Max Frisch", 1957),
    ("Mein Name sei Gantenbein", "Max Frisch", 1964),
    ("Andorra", "Max Frisch", 1961),
    ("Die Physiker", "Friedrich Dürrenmatt", 1962),
    ("Der Besuch der alten Dame", "Friedrich Dürrenmatt", 1956),
    ("Justiz", "Friedrich Dürrenmatt", 1985),
    ("Mutter Courage und ihre Kinder", "Bertolt Brecht", 1941),
    ("Der kaukasische Kreidekreis", "Bertolt Brecht", 1948),
    ("Die Dreigroschenoper", "Bertolt Brecht", 1928),
    ("Der Richter und sein Henker", "Friedrich Dürrenmatt", 1950),
    ("Das Versprechen", "Friedrich Dürrenmatt", 1958),
    ("Katz und Maus", "Günter Grass", 1961),
    ("Die Blechtrommel", "Günter Grass", 1959),
    ("Hundejahre", "Günter Grass", 1963),
    ("Ein weites Feld", "Günter Grass", 1995),
    ("Jakob der Lügner", "Jurek Becker", 1969),
    ("Die neuen Leiden des jungen W.", "Ulrich Plenzdorf", 1972),
    ("Crazy", "Benjamin Lebert", 1999),
    ("Tschick", "Wolfgang Herrndorf", 2010),
    ("Schachnovelle", "Stefan Zweig", 1942),
    ("Beware of Pity", "Stefan Zweig", 1939),
    ("Simplicissimus Teutsch", "Hans Jakob Christoffel von Grimmelshausen", 1668),
    ("Das Urteil", "Franz Kafka", 1912),
    ("Betrachtung", "Franz Kafka", 1913),
    ("Ein Landarzt", "Franz Kafka", 1919),
    ("Sturmhöhe", "Emily Brontë", 1847),
    ("Stolz und Vorurteil", "Jane Austen", 1813),
    ("Jane Eyre", "Charlotte Brontë", 1847),
    ("Moby-Dick", "Herman Melville", 1851),
    ("On the Road", "Jack Kerouac", 1957),
    ("Der große Gatsby", "F. Scott Fitzgerald", 1925),
    ("Ulysses", "James Joyce", 1922),
    ("Krieg und Frieden", "Lew Tolstoi", 1869),
    ("Anna Karenina", "Lew Tolstoi", 1877),
    ("Schuld und Sühne", "Fjodor Dostojewski", 1866),
    ("Die Brüder Karamasow", "Fjodor Dostojewski", 1880),
    ("Der Fremde", "Albert Camus", 1942),
    ("Die Pest", "Albert Camus", 1947),
    ("Ein Hungerkünstler", "Franz Kafka", 1924),
    ("Brief an den Vater", "Franz Kafka", 1919),
    ("Amerika", "Franz Kafka", 1927),
    ("Narziss und Goldmund", "Hermann Hesse", 1930),
    ("Demian", "Hermann Hesse", 1919),
    ("Unterm Rad", "Hermann Hesse", 1906),
    ("Peter Camenzind", "Hermann Hesse", 1904),
    ("Gerusalemme liberata", "Torquato Tasso", 1581),
    ("Don Quijote", "Miguel de Cervantes", 1605),
    ("Robinson Crusoe", "Daniel Defoe", 1719),
    ("Gullivers Reisen", "Jonathan Swift", 1726),
    ("Frankenstein", "Mary Shelley", 1818),
    ("Dracula", "Bram Stoker", 1897),
    ("Sherlock Holmes – Studie in Scharlachrot", "Arthur Conan Doyle", 1887),
    ("Kleiner Prinz", "Antoine de Saint-Exupéry", 1943),
    ("Der alchimist", "Paulo Coelho", 1988),
    ("Eine kurze Geschichte der Zeit", "Stephen Hawking", 1988),
    ("Sapiens", "Yuval Noah Harari", 2011),
    ("Gödel, Escher, Bach", "Douglas Hofstadter", 1979),
    ("Clean Code", "Robert C. Martin", 2008),
    ("Design Patterns", "Gang of Four", 1994),
    ("Structure and Interpretation of Computer Programs", "Abelson & Sussman", 1985),
    ("Programming in Scala", "Odersky, Spoon, Venners", 2008),
    ("Scala for the Impatient", "Cay Horstmann", 2016),
    ("Hands-on Scala.js", "Li Haoyi", 2020)
  )

  def buildShowcaseBooks(rowCount: Int): Seq[ShowcaseBook] = {
    val cat = showcaseBookCatalog
    val n = cat.length
    (0 until rowCount).map { i =>
      val (title, author, year) = cat(i % n)
      ShowcaseBook(title, author, year)
    }
  }

  def createRemoteBooks(
    pageSize: Int = 50
  )(using executionContext: ExecutionContext): RemoteListProperty[ShowcaseBook, ShowcaseBookQuery] = {
    val normalizedPageSize = math.max(1, pageSize)

    ListProperty.remote[ShowcaseBook, ShowcaseBookQuery](
      loader = ListProperty.RemoteLoader { query =>
        Future {
          val sorted = sortBooks(buildShowcaseBooks(1000).toVector, query.sorting)
          val rows = sorted.slice(query.index, query.index + query.limit)
          val nextIndex = query.index + rows.length

          ListProperty.RemotePage[ShowcaseBook, ShowcaseBookQuery](
            items = rows,
            offset = Some(query.index),
            nextQuery =
              if (nextIndex < sorted.length) Some(query.copy(index = nextIndex, limit = normalizedPageSize))
              else None,
            totalCount = Some(sorted.length),
            hasMore = Some(nextIndex < sorted.length)
          )
        }.toJSPromise
      },
      initialQuery = ShowcaseBookQuery.first(normalizedPageSize),
      executionContext = executionContext,
      sortUpdater = Some((query, sorting) =>
        query.copy(
          index = 0,
          limit = normalizedPageSize,
          sorting = sorting.toVector
        )
      ),
      rangeQueryUpdater = Some((query, index, limit) =>
        query.copy(
          index = index,
          limit = math.max(1, limit)
        )
      )
    )
  }

  private def sortBooks(
    books: Vector[ShowcaseBook],
    sorting: Vector[ListProperty.RemoteSort]
  ): Vector[ShowcaseBook] =
    sorting.headOption match {
      case Some(sort) =>
        val sorted =
          sort.field match {
            case "title"  => books.sortBy(_.title.toLowerCase)
            case "author" => books.sortBy(_.author.toLowerCase)
            case "year"   => books.sortBy(_.year)
            case _        => books
          }
        if (sort.ascending) sorted else sorted.reverse
      case None =>
        books
    }

  def render(books: RemoteListProperty[ShowcaseBook, ShowcaseBookQuery]) = {
    showcasePage(i18n"TableView", i18n"Large data sets that breathe and flow.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Data view",
          i18n"A table is good when it makes large data feel calm.",
          i18n"The JFX2 TableView combines reactive virtualization with crawlable SSR. In the browser users scroll smoothly, while crawlers can reach the full data set through real HTML links."
        )

        metricStrip(
          i18n"ListProperty" -> i18n"Local data mutates reactively.",
          i18n"RemoteList" -> i18n"Remote data loads by page or by range.",
          i18n"Virtual rows" -> i18n"Only visible rows are physically rendered."
        )

        componentShowcase(
          i18n"Local TableView",
          i18n"The control itself: items, columns, row height, selection, and reactive ListProperty mutations."
        ) {
          val localItems = ListProperty[ShowcaseBook]()
          localItems.setAll(buildShowcaseBooks(8))
          val selectedText = Property(DemoI18n.resolveNow(i18n"No row selected yet."))
          val selectionRequest = Property(-1)

          vbox {
            style { gap = "16px" }

            tableView[ShowcaseBook] {
              val localTable = summon[TableView[ShowcaseBook]]
              style { height = "300px" }
              rowHeight = 42.0

              column[ShowcaseBook, String](DemoI18n.resolveNow(i18n"Title")) { item =>
                text = item.title
              }.prefWidthProperty.set(260.0)

              column[ShowcaseBook, String](DemoI18n.resolveNow(i18n"Author")) { item =>
                text = item.author
              }.prefWidthProperty.set(220.0)

              column[ShowcaseBook, Int](DemoI18n.resolveNow(i18n"Year")) { item =>
                text = item.year.toString
              }.prefWidthProperty.set(90.0)

              TableView.items = localItems

              addDisposable(selectionRequest.observeWithoutInitial(localTable.select))
              addDisposable(localTable.selectedItemProperty.observe { item =>
                selectedText.set(
                  if (item == null) DemoI18n.resolveNow(i18n"No row selected yet.")
                  else DemoI18n.resolveNow(i18n"Selected: ${I18n.named("title", item.title)} by ${I18n.named("author", item.author)}")
                )
              })
            }

            hbox {
              style { gap = "10px"; flexWrap = "wrap" }
              button(DemoI18n.text(i18n"Select first row")) {
                onClick { _ =>
                  selectionRequest.setAlways(0)
                }
              }
              button(DemoI18n.text(i18n"Add row")) {
                onClick { _ =>
                  val next = buildShowcaseBooks(localItems.length + 1).last
                  localItems += next
                }
              }
              button(DemoI18n.text(i18n"Reset")) {
                onClick { _ =>
                  localItems.setAll(buildShowcaseBooks(8))
                  selectionRequest.setAlways(-1)
                }
              }
            }

            div {
              classes = "showcase-result"
              text = selectedText
            }
          }
        }

        apiSection(
          i18n"TableView DSL",
          i18n"This is the actual control usage, independent of whether the data is local or remote."
        ) {
          codeBlock("scala", """val books = ListProperty[Book]()
books.setAll(seedBooks)

tableView[Book] {
  rowHeight = 42.0
  showHeader = true

  column[Book, String]("Titel") { book =>
    text = book.title
  }

  column[Book, String]("Autor") { book =>
    text = book.author
  }

  column[Book, Int]("Jahr") { book =>
    text = book.year.toString
  }

  TableView.items = books
}""")
        }

        componentShowcase(
          i18n"TableView control logic",
          i18n"The important state lives in the TableView itself, not in the route."
        ) {
          div {
            style { display = "flex"; gap = "14px"; flexWrap = "wrap" }
            logicCard(
              "itemsRefProperty",
              i18n"Holds the current ListProperty. When it changes, observers are rewired and visible rows are recalculated."
            )
            logicCard(
              "visibleRowsProperty",
              i18n"Contains only the row slots that must be visible for scrollTop, viewportHeight, and overscan."
            )
            logicCard(
              "renderedWidthsProperty",
              i18n"Distributes column widths from prefWidth and viewportWidth for header and body."
            )
            logicCard(
              "RemoteListProperty",
              i18n"Provides loading, error, sorting, and range state. The TableView triggers lazy or range loading from the visible area."
            )
            logicCard(
              "crawlable",
              i18n"In SSR, the visible area is determined through offset and limit and a real More link is created."
            )
            logicCard(
              "selection",
              i18n"Row clicks set selectedIndexProperty; selectedItemProperty is updated from it."
            )
          }
        }

        apiSection(
          i18n"Virtualization internals",
          i18n"The control does not render the whole list, but calculates the visible range from scroll and viewport state."
        ) {
          codeBlock("scala", """visibleRange(total):
  firstVisible = floor(scrollTop / rowHeight)
  visibleCount = ceil(viewportHeight / rowHeight) + 1
  start = max(0, firstVisible - overscanRows)
  end = min(total, firstVisible + visibleCount + overscanRows)

recomputeVisibleRows():
  visibleRows = start.until(end).map { index =>
    VisibleRow(index, itemAt(index))
  }

onScroll:
  scrollTopProperty = viewport.scrollTop
  scrollLeftProperty = viewport.scrollLeft
  viewportHeightProperty = viewport.clientHeight
  viewportWidthProperty = viewport.clientWidth""")
        }

        metricStrip(
          i18n"1000" -> i18n"Remotely loaded book rows in the example.",
          i18n"50" -> i18n"Rows per initial remote page.",
          i18n"3" -> i18n"Sortable columns with stable sort keys."
        )

        componentShowcase(
          i18n"Crawlable book catalog",
          i18n"Remote data, virtual rows, sortable columns, and crawlable navigation in one example."
        ) {
          tableView[ShowcaseBook] {
            style { height = "500px" }
            rowHeight = 40.0
            crawlable = true

            val titleColumn = column[ShowcaseBook, String](DemoI18n.resolveNow(i18n"Title")) { item =>
               text = item.title
            }
            titleColumn.sortableProperty.set(true)
            titleColumn.sortKeyProperty.set(Some("title"))

            val authorColumn = column[ShowcaseBook, String](DemoI18n.resolveNow(i18n"Author")) { item =>
               text = item.author
            }
            authorColumn.sortableProperty.set(true)
            authorColumn.sortKeyProperty.set(Some("author"))

            val yearColumn = column[ShowcaseBook, Int](DemoI18n.resolveNow(i18n"Year")) { item =>
               text = item.year.toString
            }
            yearColumn.sortableProperty.set(true)
            yearColumn.sortKeyProperty.set(Some("year"))

            TableView.items = books
          }
        }

        insightGrid(
          (i18n"Remote", i18n"The query is explicit", i18n"Index, limit, and sorting together form the repeatable state of the data request."),
          (i18n"Cursor", i18n"Virtual rows need stable DOM paths", i18n"SSR, hydration, and ForEach must refer to the same physical nodes."),
          (i18n"SEO", i18n"Crawlable remains a product option", i18n"The table can offer real links without giving up browser performance.")
        )

        apiSection(
          i18n"RemoteListProperty",
          i18n"Sorting and lazy loading belong to the data source. The TableView only asks for those capabilities."
        ) {
          codeBlock("scala", """ListProperty.remote[Book, BookQuery](
  loader = ListProperty.RemoteLoader { query =>
    fetchBooks(query)
  },
  initialQuery = BookQuery.first(limit = 50),
  sortUpdater = Some((query, sorting) =>
    query.copy(index = 0, sorting = sorting.toVector)
  ),
  rangeQueryUpdater = Some((query, index, limit) =>
    query.copy(index = index, limit = limit)
  )
)""")
        }

        apiSection(
          i18n"Async route usage",
          i18n"The route is only the SSR shell: it loads the first page before rendering so SSR and hydration share the same initial state."
        ) {
          codeBlock("scala", """asyncRoute("/table-view") {
  val books = TableViewPage.createRemoteBooks(pageSize = 50)
  
  books.reload(TableViewPage.ShowcaseBookQuery.first(50)).toFuture.map { _ =>
    Route.factory {
      TableViewPage.render(books)
    }
  }.toJSPromise
}""")
        }
      }
    }
  }

  private def logicCard(title: String, body: RuntimeMessage): Unit = {
    vbox {
      classes = "showcase-result"
      style { gap = "8px"; flex = "1 1 260px" }
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
