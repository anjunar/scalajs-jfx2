package app.pages

import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.ComboBox.{comboBox, placeholder, valueProperty, items}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import app.components.Showcase.*

object ComboBoxPage {
  def render() = {
    showcasePage("ComboBox", "Die elegante Auswahl aus einer Menge von Möglichkeiten.") {
      vbox {
        style { gap = "24px" }
        div {
          style { opacity = "0.8"; fontSize = "14px"; marginBottom = "8px" }
          text = "Eine ComboBox bietet eine platzsparende Möglichkeit, einen Wert aus einer Liste auszuwählen. In dieser Iteration haben wir die TableView ohne Header integriert, um echte Daten anzuzeigen."
        }

        componentShowcase("ComboBox mit Items") {
          val selected = Property[String](null)
          vbox {
            style { gap = "16px" }
            
            comboBox[String]("language-selector") {
              placeholder = "Programmiersprache wählen..."
              items = Seq("Scala", "Kotlin", "Java", "TypeScript", "Rust", "Go", "Swift")
              addDisposable(valueProperty.observe(selected.set))
            }

            div {
              classes = "showcase-result"
              text = selected.map(v => s"Auswahl: ${if (v == null) "Keine" else v}")
            }
          }
        }

        apiSection("Usage") {
          codeBlock("scala", """comboBox[String]("my-combo") {
  placeholder = "Bitte wählen..."
}""")
        }
      }
    }
  }
}
