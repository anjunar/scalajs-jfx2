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
        style { gap = "48px" }

        componentShowcase("Team-Mitglieder Selektor") {
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

        apiSection("Usage") {
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
