package app.pages

import app.DemoI18n
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.ComboBox
import jfx.form.ComboBox.*
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import app.components.Showcase.*

case class Member(name: String, role: String, avatarColor: String)

object ComboBoxPage {
  def render() = {
    showcasePage(i18n"ComboBox", i18n"The elegant choice from a set of possibilities.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Selection",
          i18n"A ComboBox must provide orientation, not just hide options.",
          i18n"The showcase uses real data, custom renderers, and a footer action. That makes it clear how value display, row rendering, and identity work together."
        )

        metricStrip(
          i18n"items" -> i18n"The available set remains a clearly passed sequence.",
          i18n"converter" -> i18n"The text value can be derived independently from the object model.",
          i18n"identityBy" -> i18n"Selection stays stable even when objects are delivered again."
        )

        componentShowcase(
          i18n"Team member selector",
          i18n"A realistic renderer with avatar, role, selection state, and footer link."
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
                text = DemoI18n.text(i18n"Choose the project owner")
              }

            comboBox[Member]("team-selector") {
              placeholder = "Search member..."
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
                    text = DemoI18n.text(i18n"check")
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
                  text = DemoI18n.text(i18n"Team settings")
                }
              }
            }
          }
        }

        insightGrid(
          (i18n"Renderer", i18n"Row and value may differ", i18n"The dropdown row can be rich while the closed value stays compact."),
          (i18n"Cursor", i18n"Stable identity protects selection", i18n"identityBy describes when an entry remains the same domain entry."),
          (i18n"Readability", i18n"Configuration stays inside the block", i18n"Placeholder, data, heights, and renderers live together in one component.")
        )

        apiSection(
          i18n"Usage",
          i18n"The important decisions stay right inside the comboBox block."
        ) {
          codeBlock("scala", """|comboBox[Member]("team-selector") {
                                |  placeholder = "Choose member..."
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
