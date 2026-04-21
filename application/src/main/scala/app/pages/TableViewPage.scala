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
import app.components.Showcase.*

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
    showcasePage("TableView", "Datenmengen, die atmen und fließen.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Datenansicht",
          "Eine Tabelle ist erst gut, wenn sie große Daten ruhig macht.",
          "Die JFX2 TableView vereint reaktive Virtualisierung mit voller SEO-Tauglichkeit. Im Browser scrollt der Benutzer flüssig, während Crawler über echte HTML-Links die gesamte Datenmenge erreichen."
        )

        metricStrip(
          "ListProperty" -> "Lokale Daten mutieren reaktiv.",
          "RemoteList" -> "Remote-Daten laden seiten- oder bereichsweise nach.",
          "Virtual Rows" -> "Nur sichtbare Zeilen werden physisch gerendert."
        )

        componentShowcase(
          "Lokale TableView",
          "Die Control selbst: Items, Columns, RowHeight, Selection und reaktive ListProperty-Mutationen."
        ) {
          val localItems = ListProperty[ShowcaseBook]()
          localItems.setAll(buildShowcaseBooks(8))
          val selectedText = Property("Noch keine Zeile ausgewählt.")
          val selectionRequest = Property(-1)

          vbox {
            style { gap = "16px" }

            tableView[ShowcaseBook] {
              val localTable = summon[TableView[ShowcaseBook]]
              style { height = "300px" }
              rowHeight = 42.0

              column[ShowcaseBook, String]("Titel") { item =>
                text = item.title
              }.prefWidthProperty.set(260.0)

              column[ShowcaseBook, String]("Autor") { item =>
                text = item.author
              }.prefWidthProperty.set(220.0)

              column[ShowcaseBook, Int]("Jahr") { item =>
                text = item.year.toString
              }.prefWidthProperty.set(90.0)

              TableView.items = localItems

              addDisposable(selectionRequest.observeWithoutInitial(localTable.select))
              addDisposable(localTable.selectedItemProperty.observe { item =>
                selectedText.set(
                  if (item == null) "Noch keine Zeile ausgewählt."
                  else s"Ausgewählt: ${item.title} von ${item.author}"
                )
              })
            }

            hbox {
              style { gap = "10px"; flexWrap = "wrap" }
              button("Erste Zeile auswählen") {
                onClick { _ =>
                  selectionRequest.setAlways(0)
                }
              }
              button("Zeile hinzufügen") {
                onClick { _ =>
                  val next = buildShowcaseBooks(localItems.length + 1).last
                  localItems += next
                }
              }
              button("Zurücksetzen") {
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
          "TableView DSL",
          "Das ist die eigentliche Control-Nutzung, unabhängig davon, ob die Daten lokal oder remote kommen."
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
          "TableView Control Logic",
          "Die wichtigsten Zustände in der TableView selbst, nicht in der Route."
        ) {
          div {
            style { display = "flex"; gap = "14px"; flexWrap = "wrap" }
            logicCard(
              "itemsRefProperty",
              "Hält die aktuelle ListProperty. Beim Wechsel werden Observer neu verdrahtet und sichtbare Rows neu berechnet."
            )
            logicCard(
              "visibleRowsProperty",
              "Enthält nur die Row-Slots, die für ScrollTop, ViewportHeight und Overscan sichtbar sein müssen."
            )
            logicCard(
              "renderedWidthsProperty",
              "Verteilt Spaltenbreiten anhand von PrefWidth und ViewportWidth stabil auf Header und Body."
            )
            logicCard(
              "RemoteListProperty",
              "Liefert Loading/Error/Sorting/Range-State. Die TableView triggert lazy load oder range load aus dem sichtbaren Bereich."
            )
            logicCard(
              "crawlable",
              "Im SSR wird der sichtbare Bereich über offset/limit bestimmt und ein echter More-Link erzeugt."
            )
            logicCard(
              "selection",
              "Zeilenklick setzt selectedIndexProperty; daraus wird selectedItemProperty aktualisiert."
            )
          }
        }

        apiSection(
          "Virtualisierung Intern",
          "Die Control rendert nicht die ganze Liste, sondern berechnet aus Scroll- und Viewport-State den sichtbaren Bereich."
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
          "1000" -> "Remote geladene Buchzeilen im Beispiel.",
          "50" -> "Zeilen pro initialer Remote-Page.",
          "3" -> "Sortierbare Spalten mit stabilen Sort-Keys."
        )

        componentShowcase(
          "Crawlbarer Bücher-Katalog",
          "Remote-Daten, virtuelle Zeilen, sortierbare Spalten und crawlbare Navigation in einem Beispiel."
        ) {
          tableView[ShowcaseBook] {
            style { height = "500px" }
            rowHeight = 40.0
            crawlable = true

            val titleColumn = column[ShowcaseBook, String]("Titel") { item =>
               text = item.title
            }
            titleColumn.sortableProperty.set(true)
            titleColumn.sortKeyProperty.set(Some("title"))

            val authorColumn = column[ShowcaseBook, String]("Autor") { item =>
               text = item.author
            }
            authorColumn.sortableProperty.set(true)
            authorColumn.sortKeyProperty.set(Some("author"))

            val yearColumn = column[ShowcaseBook, Int]("Jahr") { item =>
               text = item.year.toString
            }
            yearColumn.sortableProperty.set(true)
            yearColumn.sortKeyProperty.set(Some("year"))

            TableView.items = books
          }
        }

        insightGrid(
          ("Remote", "Die Query ist explizit", "Index, Limit und Sorting bilden zusammen den wiederholbaren Zustand der Datenanfrage."),
          ("Cursor", "Virtuelle Zeilen brauchen stabile DOM-Pfade", "SSR, Hydration und ForEach müssen dieselben physischen Knoten meinen."),
          ("SEO", "Crawlable bleibt eine fachliche Option", "Die Tabelle kann echte Links anbieten, ohne die Browser-Performance aufzugeben.")
        )

        apiSection(
          "RemoteListProperty",
          "Sortierung und Lazy Loading gehören zur Datenquelle. Die TableView fragt diese Fähigkeiten nur ab."
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
          "Async Route Usage",
          "Die Route ist nur die SSR-Hülle: Sie lädt die erste Page vor dem Rendern, damit SSR und Hydration denselben Anfangszustand teilen."
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

  private def logicCard(title: String, body: String): Unit = {
    vbox {
      classes = "showcase-result"
      style { gap = "8px"; flex = "1 1 260px" }
      div {
        style { fontWeight = "800" }
        text = title
      }
      div {
        style { color = "var(--aj-ink-muted)" }
        text = body
      }
    }
  }
}
