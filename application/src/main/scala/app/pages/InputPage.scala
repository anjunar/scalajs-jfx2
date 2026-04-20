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
    showcasePage("Formulare & Input", "Der fließende Dialog mit deinen Nutzern.") {
      vbox {
        style { gap = "24px" }
        div {
          style { opacity = "0.8"; fontSize = "14px"; marginBottom = "8px" }
          text = "Dateneingabe sollte sich wie ein natürliches Gespräch anfühlen. Erlebe die perfekte Symbiose aus typsicherem Binding, automatischer Dependency Injection und reaktiver Validierung – so macht Fehlerbehandlung sogar Spaß."
        }
        componentShowcase("Einfache Texteingabe") {
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
        apiSection("Standalone Usage") {
          codeBlock("scala", """val name = Property("")
inputContainer("Name eingeben...") {
  standaloneInput("name") {
    addDisposable(stringValueProperty.observe(name.set))
  }
}""")
        }
        componentShowcase("DI-Bound Form") {
          import jfx.form.Form.{form, controls, clearErrors}
          form {
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
                  val hasErrors = controls.get.map(_.validate(forceVisible = true)).exists(_.nonEmpty)
                  if (hasErrors) {
                     dom.window.alert("Formular enthält Fehler!")
                  } else {
                     val data = controls.get.map(c => s"${c.name}: ${c.valueProperty.get}").mkString(", ")
                     dom.window.alert(s"Form Data: $data")
                  }
                }
              }
              
              button("Clear") {
                onClick { _ =>
                  controls.foreach(_.valueProperty.asInstanceOf[Property[String]].set(""))
                  clearErrors()
                }
              }
            }
          }
        }
        apiSection("Form & DI Usage") {
          codeBlock("scala", """import jfx.form.Form.{form, controls}
form {
  inputContainer("E-Mail") {
    input("email") { 
      validators += EmailValidator()
    }
  }
  
  button("Submit") {
    onClick { _ => 
      if (!controls.get.exists(_.validate(true).nonEmpty)) {
         println("Success!")
      }
    }
  }
}""")
        }
      }
    }
  }
}
