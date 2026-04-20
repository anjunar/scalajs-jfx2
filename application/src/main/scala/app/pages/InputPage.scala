package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.form.Input.{input, standaloneInput, stringValueProperty, errorsProperty, validators}
import jfx.form.InputContainer.inputContainer
import jfx.form.validators.{EmailValidator, NotBlankValidator}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import org.scalajs.dom
import scala.scalajs.js
import app.components.Showcase.*

object InputPage {
  def render() = {
    showcasePage("Form & Input", "Texteingabe-Felder und Formulare.") {
      vbox {
        style { gap = "24px" }
        componentShowcase("Standalone Text Input") {
          val name = Property("")
          vbox {
            inputContainer("Name eingeben...") {
              standaloneInput("name") {
                addDisposable(stringValueProperty.observe(name.set))
              }
            }
            div {
              classes = "showcase-result"
              val labelText = name.map(v => s"Eingabe: $v")
              text = labelText
            }
          }
        }
        componentShowcase("DI-Bound Form") {
          import jfx.form.Form.form
          form {
            val myForm = summon[jfx.form.Form]
            
            vbox {
              style { gap = "10px" }
              
              inputContainer("Vorname") {
                input("firstName") { 
                  validators += NotBlankValidator("Vorname darf nicht leer sein")
                }
              }

              inputContainer("E-Mail") {
                input("email") { 
                  validators += EmailValidator()
                }
              }
              
              button("Submit (Siehe Konsole)") {
                onClick { _ => 
                  // Validate all controls before submit
                  val hasErrors = myForm.controls.get.map(_.validate(forceVisible = true)).exists(_.nonEmpty)
                  if (hasErrors) {
                     dom.window.alert("Formular enthält Fehler!")
                  } else {
                     val data = myForm.controls.get.map(c => s"${c.name}: ${c.valueProperty.get}").mkString(", ")
                     dom.window.alert(s"Form Data: $data")
                  }
                }
              }
              
              button("Clear") {
                onClick { _ =>
                  myForm.controls.foreach(_.valueProperty.asInstanceOf[Property[String]].set(""))
                  myForm.clearErrors()
                }
              }
            }
          }
        }
      }
    }
  }
}
