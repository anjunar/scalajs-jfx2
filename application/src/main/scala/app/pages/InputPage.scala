package app.pages

import app.DemoI18n
import jfx.action.Button.button
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.i18n.*
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
    showcasePage(i18n"Forms & input", i18n"The flowing dialog with your users.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Form flow",
          i18n"Data entry should feel like a structured conversation.",
          i18n"InputContainer, Input, and Properties separate presentation, value, and validation while staying readable together in the template. You can see immediately which fields exist and how they react."
        )

        metricStrip(
          i18n"Property" -> i18n"The visible value can flow reactively.",
          i18n"Validator" -> i18n"Error rules stay close to the field.",
          i18n"Form" -> i18n"Several controls share one context for submit and reset."
        )

        componentShowcase(
          i18n"Simple text input",
          i18n"Standalone inputs are ideal for search fields, short filters, or small dialogs without a full form context."
        ) {
          val name = Property("")
          vbox {
            style { maxWidth = "420px" }
            inputContainer(DemoI18n.text(i18n"Enter name...")) {
              standaloneInput("name") {
                addDisposable(stringValueProperty.observe(name.set))
              }
            }
            div {
              classes = "showcase-result"
              val labelText = name.flatMap(v => DemoI18n.text(i18n"Input: $v"))
              text = labelText
            }
          }
        }
        apiSection(
          i18n"Standalone usage",
          i18n"The property remains explicit. That makes it immediately clear where the value flows."
        ) {
          codeBlock("scala", """val name = Property("")
inputContainer("Name eingeben...") {
  standaloneInput("name") {
    addDisposable(stringValueProperty.observe(name.set))
  }
}""")
        }
        componentShowcase(
          i18n"Domain-bound form",
          i18n"Forms can bind directly to domain objects. Reflection connects field names with properties."
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

              inputContainer(DemoI18n.text(i18n"Full name")) {
                input("name") {}
              }

              inputContainer(DemoI18n.text(i18n"E-mail")) {
                input("email") {}
              }

              subForm("address") {
                vbox {
                  style { gap = "10px"; padding = "10px"; border = "1px solid #eee"; borderRadius = "4px" }
                  div { text = DemoI18n.text(i18n"Address (SubForm)"); style { fontWeight = "bold" } }
                  
                  inputContainer(DemoI18n.text(i18n"Street")) {
                    input("street") {}
                  }
                  inputContainer(DemoI18n.text(i18n"City")) {
                    input("city") {}
                  }
                }
              }

              hbox {
                style { gap = "10px" }
                button(DemoI18n.text(i18n"Validate")) {
                  onClick { _ =>
                    import jfx.form.Form.controls
                    val hasErrors = controls(using f).get.map(_.validate(forceVisible = true)).exists(_.nonEmpty)
                    if (hasErrors) {
                      dom.window.alert(DemoI18n.resolveNow(i18n"The form contains errors."))
                    } else {
                      dom.window.alert(DemoI18n.resolveNow(i18n"Everything is fine."))
                    }
                  }
                }
              }

              div {
                classes = "showcase-result"
                val info = user.name.flatMap(n => DemoI18n.text(i18n"Current user: ${I18n.named("name", n)} (${I18n.named("email", user.email.get)}) lives in ${I18n.named("city", user.address.get.city.get)}"))
                text = info
              }
            }
          }
        }
        apiSection(
          i18n"Domain binding",
          i18n"Passing an object to form lets inputs find the matching properties automatically."
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
          (i18n"Binding", i18n"Value flow stays visible", i18n"The field writes into a Property or registers itself in the form context."),
          (i18n"Validation", i18n"Errors belong to the field", i18n"Rules sit where a reader expects them, not in a distant submit method."),
          (i18n"Submit", i18n"Forms collect behavior", i18n"The form context makes validation, clearing, and reading visible as shared operations.")
        )

        apiSection(
          i18n"Form and DI usage",
          i18n"For larger forms, the form context is the calmer structure."
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
