package app.pages

import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.form.ComboBox.{comboBox, placeholder, valueProperty}
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
          text = "Eine ComboBox bietet eine platzsparende Möglichkeit, einen Wert aus einer Liste auszuwählen. In dieser ersten Iteration konzentrieren wir uns auf das visuelle Erscheinungsbild des Eingabefeldes mit seinem Placeholder."
        }

        componentShowcase("Basis ComboBox") {
          vbox {
            style { gap = "16px" }
            
            div {
              text = "ComboBox ohne Auswahl (zeigt Placeholder):"
            }
            
            comboBox[String]("demo-combo") {
              placeholder = "Bitte wählen..."
            }

            div {
              text = "ComboBox mit gesetztem Wert:"
            }

            comboBox[String]("demo-combo-filled") {
              placeholder = "Wird nicht angezeigt"
              valueProperty.set("Ausgewählter Wert")
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
