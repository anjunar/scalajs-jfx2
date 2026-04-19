package app

import jfx.action.Button.*
import jfx.browser.Browser
import jfx.control.Link.*
import jfx.control.TableColumn.column
import jfx.control.TableView.tableView
import jfx.control.{TableColumn, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.Input.*
import jfx.form.Editor.editor
import jfx.form.editor.plugins.*
import jfx.form.InputContainer.inputContainer
import jfx.hydration.Hydration
import jfx.layout.Div.div
import jfx.layout.Drawer.*
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.layout.HorizontalLine.horizontalLine
import jfx.router.Route
import jfx.router.RouteContext.routeContext
import jfx.router.Router.router
import jfx.ssr.Ssr
import jfx.statement.Conditional.*
import jfx.statement.ForEach.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object Main {

  private final case class ShowcaseBook(title: String, author: String, year: Int)

  /** Viele verschiedene Titel, werden für große Tabellen mehrfach durchlaufen. */
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

  private def buildShowcaseBooks(rowCount: Int): js.Array[ShowcaseBook] = {
    val cat = showcaseBookCatalog
    val n = cat.length
    js.Array(
      (0 until rowCount).map { i =>
        val (title, author, year) = cat(i % n)
        ShowcaseBook(title, author, year)
      }*
    )
  }

  def main(args: Array[String]): Unit =
    boot()

  @JSExportTopLevel("boot")
  def boot(): Unit = {
    val maybeRoot = Option(dom.document.getElementById("root")).map(_.asInstanceOf[HTMLElement])

    maybeRoot match {
      case Some(root) if root.children.length > 0 =>
        Hydration.hydrateInto(root) {
          demo()
        }

      case Some(root) =>
        Browser.mount(root) {
          demo()
        }

      case None =>
        dom.console.error("JFX2 demo could not find #root.")
    }
  }

  @JSExportTopLevel("renderSsr")
  def renderSsr(path: String): String =
    Ssr.renderToStringFor(Ssr.Request(path = path, attributes = Map("basePath" -> "/scalajs-jfx2"))) {
      demo()
    }

  private def demo() = {
    val drawerOpen = Property(true)

    val routes = js.Array(
      Route.scoped("/") {
        showcasePage("Overview", "Willkommen zur JFX2 API Dokumentation.") {
          vbox {
            classes = "showcase-intro"
            div {
              classes = "showcase-intro__text"
              text = "JFX2 ist ein reaktives UI-Framework für Scala.js mit Fokus auf SSR-Kompatibilität, Typisierung und architektonische Klarheit."
            }
            horizontalLine()
            div {
              classes = "showcase-intro__links"
              text = "Wähle eine Komponente aus der Sidebar, um Details und Live-Beispiele zu sehen."
            }
          }
        }
      },
      Route.scoped("/button") {
        showcasePage("Button", "Die primäre Aktions-Komponente.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Standard Button") {
              button("Klick mich") {
                onClick { _ => dom.window.alert("Button geklickt!") }
              }
            }
            apiSection("Usage") {
              codeBlock("scala", "button(\"Klick mich\") {\n  onClick { _ => println(\"Geklickt\") }\n}")
            }
          }
        }
      },
      Route.scoped("/input") {
        showcasePage("Input", "Texteingabe-Felder für Formulare.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Text Input") {
              val name = Property("")
              vbox {
                input("name") {
                  placeholder = "Name eingeben..."
                  stringValueProperty.observe(name.set)
                }
                val label = div {
                  classes = "showcase-result"
                  text = s"Eingabe: ${name.get}"
                }
                label.addDisposable(name.observe(v => label.textContent = s"Eingabe: $v"))
              }
            }
            apiSection("Usage") {
              codeBlock("scala", "input(\"username\") {\n  placeholder = \"Benutzername\"\n  stringValueProperty.observe(v => println(v))\n}")
            }
          }
        }
      },
      Route.scoped("/editor") {
        showcasePage("Rich Text Editor", "Ein leistungsfähiger WYSIWYG Editor basierend auf Lexical.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Live Editor") {
              val e = editor("demo-editor") {
                defaultPlugins()
              }
              e.value = "Dies ist ein **fetter** Text im Editor."
            }
            apiSection("Usage") {
              codeBlock("scala", "editor(\"body\") {\n  defaultPlugins()\n  value = \"Startinhalt\"\n}")
            }
          }
        }
      },
      Route.scoped("/layout") {
        showcasePage("Layout Components", "HBox, VBox und Div zur Strukturierung.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("HBox & VBox") {
              vbox {
                style { gap = "10px" }
                hbox {
                  style { gap = "10px" }
                  div { classes = "demo-box"; text = "H1" }
                  div { classes = "demo-box"; text = "H2" }
                }
                vbox {
                  style { gap = "5px" }
                  div { classes = "demo-box"; text = "V1" }
                  div { classes = "demo-box"; text = "V2" }
                }
              }
            }
          }
        }
      },
      Route.scoped("/table-view") {
        showcasePage("TableView", "Virtuelle Zeilen, Spalten mit ListProperty und optionaler Mehrfachauswahl.") {
          vbox {
            style { gap = "24px" }
            componentShowcase("Bücher — 500 Zeilen (lokale ListProperty)") {
              val books = ListProperty(buildShowcaseBooks(500))
              div {
                classes = "component-doc__mini-table"
                tableView[ShowcaseBook] {
                  val table = summon[TableView[ShowcaseBook]]
                  table.items = books
                  table.setFixedCellSize(36.0)
                  style {
                    height = "420px"
                  }

                  column[ShowcaseBook, String]("Titel") {
                    val col = summon[TableColumn[ShowcaseBook, String]]
                    col.setCellValueFactory(f => Property(f.getValue.title))
                    col.prefWidth = 220
                  }
                  column[ShowcaseBook, String]("Autor") {
                    val col = summon[TableColumn[ShowcaseBook, String]]
                    col.setCellValueFactory(f => Property(f.getValue.author))
                    col.prefWidth = 200
                  }
                  column[ShowcaseBook, Int]("Jahr") {
                    val col = summon[TableColumn[ShowcaseBook, Int]]
                    col.setCellValueFactory(f => Property(f.getValue.year))
                    col.prefWidth = 72
                  }
                }
              }
            }
            apiSection("Usage") {
              codeBlock(
                "scala",
                """import jfx.control.TableView.tableView
import jfx.control.TableColumn.column
import jfx.core.state.{ListProperty, Property}

val books = ListProperty(buildShowcaseBooks(500))

tableView[ShowcaseBook] {
  val table = summon[TableView[ShowcaseBook]]
  table.items = books
  table.setFixedCellSize(36.0)
  style { height = "420px" }
  column[ShowcaseBook, String]("Titel") {
    val col = summon[TableColumn[ShowcaseBook, String]]
    col.setCellValueFactory(f => Property(f.getValue.title))
  }
}"""
              )
            }
          }
        }
      }
    )

    div {
      classes = "app-shell"

      val component = drawer {
        drawerNavigation {
          div {
            classes = "app-sidebar"
            
            div {
              classes = "app-sidebar__header"
              div { classes = "app-sidebar__logo"; text = "JFX2 API" }
            }

            div {
              classes = "app-sidebar__nav"
              sidebarSection("General")
              navLink("/", "Overview", "Introduction")
              
              sidebarSection("Actions")
              navLink("/button", "Button", "Trigger actions")
              
              sidebarSection("Forms")
              navLink("/input", "Input", "Text fields")
              navLink("/editor", "Editor", "Rich text")
              
              sidebarSection("Layout")
              navLink("/layout", "Layouts", "HBox, VBox, Div")

              sidebarSection("Data")
              navLink("/table-view", "TableView", "Listen & Spalten")
            }
            
            div {
              classes = "app-sidebar__footer"
              text = "Built with JFX2"
            }
          }
        }

        drawerContent {
          div {
            classes = "app-main"
            
            div {
              classes = "app-toolbar"
              button("menu") {
                classes = "app-toolbar__menu-toggle material-icons"
                text = "menu"
                onClick { _ => drawerOpen.set(!drawerOpen.get) }
              }
              div {
                classes = "app-toolbar__title"
                text = "Live Documentation"
              }
              div { classes = "spacer" }
              div {
                classes = "app-toolbar__version"
                text = "v2.0.0-alpha"
              }
            }

            div {
              classes = "app-content-viewport"
              router(routes)
            }

            div {
              classes = "app-footer"
              div {
                classes = "app-footer__text"
                text = s"┬⌐ ${new js.Date().getFullYear()} Anjunar. Pure Scala.js Architecture."
              }
            }
          }
        }
      }

      component.addDisposable(drawerOpen.observe(component.isOpen = _))
    }
  }

  private def showcasePage(title: String, subtitle: String)(content: => Unit) = {
    vbox {
      classes = "showcase-page"
      vbox {
        classes = "showcase-page__header"
        div { classes = "showcase-page__title"; text = title }
        div { classes = "showcase-page__subtitle"; text = subtitle }
      }
      div {
        classes = "showcase-page__content"
        content
      }
    }
  }

  private def componentShowcase(title: String)(content: => Unit) = {
    vbox {
      classes = "component-showcase"
      div { classes = "component-showcase__title"; text = title }
      div {
        classes = "component-showcase__render"
        content
      }
    }
  }

  private def apiSection(title: String)(content: => Unit) = {
    vbox {
      classes = "api-section"
      div { classes = "api-section__title"; text = title }
      div {
        classes = "api-section__content"
        content
      }
    }
  }

  private def codeBlock(lang: String, code: String) = {
    div {
      classes = "code-block"
      div {
        classes = "code-block__content"
        text = code
      }
    }
  }

  private def sidebarSection(title: String) = {
    div {
      classes = "app-sidebar__section-title"
      text = title
    }
  }

  private def navLink(path: String, label: String, sub: String) = {
    link(path) {
      classes = "app-nav-link"
      div {
        classes = "app-nav-link__label"
        text = label
      }
      div {
        classes = "app-nav-link__sub"
        text = sub
      }
    }
  }

}
