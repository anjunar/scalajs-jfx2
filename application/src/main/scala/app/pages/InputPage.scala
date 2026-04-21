package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.form.Input.{input, standaloneInput, stringValueProperty, errorsProperty, validators}
import jfx.form.InputContainer.inputContainer
import jfx.form.validators.{EmailValidator, NotBlankValidator}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.layout.HBox.hbox
import jfx.form.Form
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
          "Domain-Bound Form",
          "Formulare können direkt an Domänen-Objekte gebunden werden. Reflection verbindet Feldnamen mit Properties."
        ) {
          import jfx.form.Form.form
          import jfx.form.SubForm.subForm
          import jfx.form.Formular
          import app.domain.{User, Address}

          val user = new User()
          user.name.set("Max Mustermann")
          user.address.get.city.set("Musterstadt")

          form(user) {
            val f = summon[Form[User]]
            vbox {
              style { gap = "15px" }

              inputContainer("Vollständiger Name") {
                input("name") {}
              }

              inputContainer("E-Mail") {
                input("email") {}
              }

              subForm("address") {
                vbox {
                  style { gap = "10px"; padding = "10px"; border = "1px solid #eee"; borderRadius = "4px" }
                  div { text = "Adresse (SubForm)"; style { fontWeight = "bold" } }
                  
                  inputContainer("Straße") {
                    input("street") {}
                  }
                  inputContainer("Stadt") {
                    input("city") {}
                  }
                }
              }

              hbox {
                style { gap = "10px" }
                button("Validieren") {
                  onClick { _ =>
                    import jfx.form.Form.controls
                    val hasErrors = controls(using f).get.map(_.validate(forceVisible = true)).exists(_.nonEmpty)
                    if (hasErrors) {
                      dom.window.alert("Formular enthält Fehler!")
                    } else {
                      dom.window.alert("Alles ok!")
                    }
                  }
                }
              }

              div {
                classes = "showcase-result"
                val info = user.name.map(n => s"Aktueller User: $n (${user.email.get}) wohnt in ${user.address.get.city.get}")
                text = info
              }
            }
          }
        }
        apiSection(
          "Domain Binding",
          "Durch die Übergabe eines Objekts an 'form' suchen sich die Inputs automatisch die passenden Properties."
        ) {
          codeBlock("scala", """val user = new User()
form(user) {
  input("name") // Bindet an user.name (Property[String])
  
  subForm("address") {
    input("city") // Bindet an user.address.city
  }
}""")
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
