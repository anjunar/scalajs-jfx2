package app.pages

import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.ComboBox
import jfx.form.ComboBox.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

case class Member(name: String, role: String, avatarColor: String)

object ComboBoxPage {
  def render() = {
    showcasePage("ComboBox", "Die elegante Auswahl aus einer Menge von Möglichkeiten.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          "Auswahl",
          "Eine ComboBox muss Orientierung geben, nicht nur Optionen verstecken.",
          "Der Showcase nutzt echte Daten, eigene Renderer und eine Footer-Aktion. So wird sichtbar, wie Wertdarstellung, Listenzeile und Identität zusammenspielen."
        )

        metricStrip(
          "items" -> "Die verfügbare Menge bleibt eine klar übergebene Sequenz.",
          "converter" -> "Der Textwert kann unabhängig vom Objektmodell entstehen.",
          "identityBy" -> "Selektion bleibt stabil, auch wenn Objekte neu geliefert werden."
        )

        componentShowcase(
          "Team-Mitglieder Selektor",
          "Ein realistischer Renderer mit Avatar, Rolle, Auswahlzustand und Footer-Link."
        ) {
          val members = Seq(
            Member("Alice Scala", "Software Architect", "#6366f1"),
            Member("Bob Kotlin", "Product Owner", "#ec4899"),
            Member("Charlie Rust", "DevOps Engineer", "#10b981"),
            Member("Diana Java", "Backend Lead", "#f59e0b")
          )

          vbox {
            style { gap = "16px"; maxWidth = "400px" }
            
            div {
              style { fontWeight = "600"; fontSize = "14px"; color = "var(--aj-ink-soft)" }
              text = "Projekt-Verantwortlichen wählen"
            }

            comboBox[Member]("team-selector") {
              placeholder = "Mitglied suchen..."
              items = members
              rowHeight = 60.0
              dropdownHeight = 300.0
              
              converter = _.name
              identityBy = _.name

              itemRenderer { (member, isSelected) =>
                hbox {
                  style { 
                    width = "100%"
                    alignItems = "center"
                    padding = "8px 12px"
                    gap = "12px"
                  }
                  
                  div {
                    style { 
                      width = "32px"; height = "32px"
                      borderRadius = "50%"
                      background = member.avatarColor
                      display = "flex"; alignItems = "center"; justifyContent = "center"
                      color = "white"; fontSize = "12px"; fontWeight = "bold"
                    }
                    text = member.name.take(1)
                  }

                  vbox {
                    style { flex = "1" }
                    div {
                      style { fontWeight = "500"; fontSize = "14px" }
                      text = member.name
                    }
                    div {
                      style { fontSize = "12px"; opacity = "0.6" }
                      text = member.role
                    }
                  }

                  div {
                    addClass("material-icons")
                    style { color = "var(--aj-accent)"; fontSize = "20px" }
                    text = "check"
                    visible = isSelected
                  }
                }
              }

              valueRenderer { member =>
                hbox {
                  style { alignItems = "center"; gap = "8px" }
                  div {
                    style { 
                      width = "20px"; height = "20px"; borderRadius = "50%"
                      background = member.avatarColor
                    }
                  }
                  div {
                    style { fontWeight = "500" }
                    text = member.name
                  }
                }
              }

              footerRenderer {
                div {
                  addClass("jfx-combo-box__footer-link")
                  text = "Team-Einstellungen"
                }
              }
            }
          }
        }

        insightGrid(
          ("Renderer", "Zeile und Wert dürfen verschieden sein", "Die Dropdown-Zeile kann reich sein, während der geschlossene Wert kompakt bleibt."),
          ("Cursor", "Stabile Identität schützt die Auswahl", "identityBy beschreibt, wann ein Eintrag derselbe fachliche Eintrag bleibt."),
          ("Lesbarkeit", "Konfiguration bleibt im Block", "Placeholder, Daten, Höhen und Renderer liegen in einer Komponente zusammen.")
        )

        apiSection(
          "Usage",
          "Die wichtigsten Entscheidungen stehen direkt im comboBox-Block."
        ) {
          codeBlock("scala", """|comboBox[Member]("team-selector") {
                                |  placeholder = "Mitglied wählen..."
                                |  items = myMemberList
                                |
                                |  itemRenderer { (member, isSelected) =>
                                |    hbox {
                                |       // Avatar, Name, Checkmark...
                                |    }
                                |  }
                                |}""".stripMargin)
        }
      }
    }
  }
}
