package app.pages

import jfx.control.TableColumn.column
import jfx.control.TableView.tableView
import jfx.control.{TableColumn, TableView}
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object TableViewPage {

  final case class ShowcaseBook(title: String, author: String, year: Int)

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
    ("Der Alchimist", "Paulo Coelho", 1988),
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

  def render() = {
    showcasePage("TableView", "Einfache Tabellen-Struktur.") {
      vbox {
        style { gap = "24px" }
        componentShowcase("Bücher (virtuelle Liste mit 1000 Einträgen)") {
          val books = new ListProperty[ShowcaseBook]()
          books.setAll(buildShowcaseBooks(1000))
          tableView[ShowcaseBook] {
            val table = summon[TableView[ShowcaseBook]]
            style { height = "400px" }
            table.rowHeightProperty.set(40.0)

            column[ShowcaseBook, String]("Titel") { item =>
               text = item.title
            }
            column[ShowcaseBook, String]("Autor") { item =>
               text = item.author
            }

            table.items.setAll(books.get)
          }
        }
      }
    }
  }
}
