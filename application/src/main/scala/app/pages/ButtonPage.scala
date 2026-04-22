package app.pages

import jfx.action.Button.button
import jfx.core.component.Component.*
import app.DemoI18n
import jfx.i18n.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import org.scalajs.dom
import app.components.Showcase.*

object ButtonPage {
  def render() = {
    showcasePage(i18n"Button", i18n"The pulse of your app.") {
      vbox {
        style { gap = "34px" }

        sectionIntro(
          i18n"Interaction",
          i18n"A button is small, but it carries responsibility.",
          i18n"In JFX2 the action stays visible in the template: label, event, and surrounding context sit next to each other. That keeps simple buttons easy to read and leaves room for more complex workflows later."
        )

        componentShowcase(
          i18n"Standard button",
          i18n"A focused click target with direct event binding. Ideal for clear, self-contained actions."
        ) {
          div { 
            style { marginBottom = "12px"; opacity = "0.8" }
            text = DemoI18n.text(i18n"Buttons are the heart of interaction. They are not just click targets; they bring your app to life.")
          }
          button(DemoI18n.text(i18n"Click me and bring me to life")) {
            onClick { _ => dom.window.alert(DemoI18n.resolveNow(i18n"I was clicked! The magic begins.")) }
          }
        }

        componentShowcase(
          i18n"Action group",
          i18n"Several buttons may sit close together as long as their intent remains distinguishable."
        ) {
          hbox {
            classes = "showcase-action-row"
            button(DemoI18n.text(i18n"Save")) {
              onClick { _ => dom.window.alert(DemoI18n.resolveNow(i18n"Saved.")) }
            }
            button(DemoI18n.text(i18n"Check")) {
              onClick { _ => dom.window.alert(DemoI18n.resolveNow(i18n"Checked.")) }
            }
            button(DemoI18n.text(i18n"Reset")) {
              onClick { _ => dom.window.alert(DemoI18n.resolveNow(i18n"Reset.")) }
            }
          }
        }

        insightGrid(
          (i18n"State", i18n"The button says what happens", i18n"A good label describes the next action, not the technical implementation behind it."),
          (i18n"Event", i18n"onClick stays local", i18n"The DSL keeps trigger and reaction visible in the same place."),
          (i18n"Feedback", i18n"Actions need a response", i18n"After the click, the interface should show something visible: a message, status, navigation, or data update.")
        )

        apiSection(
          i18n"The simplicity of the DSL",
          i18n"The core stays intentionally small: create the button, bind the handler, done."
        ) {
          codeBlock("scala", "button(\"Click me\") {\n  onClick { _ => println(\"Clicked\") }\n}")
        }
      }
    }
  }
}
