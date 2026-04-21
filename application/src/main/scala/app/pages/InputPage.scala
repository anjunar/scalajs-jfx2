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
        style { gap = "34px" }

        sectionIntro(
          "Formfluss",
          "Dateneingabe soll sich wie ein geordnetes Gespräch anfühlen.",
          "InputContainer, Input und Properties trennen Darstellung, Wert und Validierung, bleiben im Template aber direkt zusammen lesbar. So sieht man sofort, welche Felder existieren und wie sie reagieren."
        )

        metricStrip(
          "Property" -> "Der sichtbare Wert kann reaktiv weitergegeben werden.",
          "Validator" -> "Fehlerregeln bleiben nah am Feld.",
          "Form" -> "Mehrere Controls teilen einen Kontext für Submit und Reset."
        )

        componentShowcase(
          "Einfache Texteingabe",
          "Standalone-Inputs sind ideal für Suchfelder, kurze Filter oder kleine Dialoge ohne kompletten Form-Kontext."
        ) {
          val name = Property("")
          vbox {
            style { maxWidth = "420px" }
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
        apiSection(
          "Standalone Usage",
          "Das Property bleibt explizit. Dadurch ist sofort klar, wohin der Wert fliesst."
        ) {
          codeBlock("scala", """val name = Property("")
inputContainer("Name eingeben...") {
  standaloneInput("name") {
    addDisposable(stringValueProperty.observe(name.set))
  }
}""")
        }
        componentShowcase(
          "DI-Bound Form",
          "Im Form-Kontext melden sich Controls selbst an. Submit-Logik kann dadurch alle Felder gemeinsam validieren."
        ) {
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

        insightGrid(
          ("Binding", "Wertfluss bleibt sichtbar", "Das Feld schreibt in ein Property oder registriert sich im Form-Kontext."),
          ("Validierung", "Fehler gehören ans Feld", "Regeln sitzen dort, wo ein Leser sie erwartet, statt in einer entfernten Submit-Methode."),
          ("Submit", "Formen sammeln Verhalten", "Der Formular-Kontext macht Validieren, Leeren und Auslesen als gemeinsame Operation lesbar.")
        )

        apiSection(
          "Form & DI Usage",
          "Für größere Formulare ist der Form-Kontext die ruhigere Struktur."
        ) {
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
