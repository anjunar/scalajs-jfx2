package jfx.form

import jfx.core.component.{ClientSideComponent, Component}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div

import scala.scalajs.js

class Editor(val name: String, override val standalone: Boolean = false)
    extends ClientSideComponent
    with Control[js.Any | Null] {

  override def tagName: String = "div"

  override val valueProperty: Property[js.Any | Null] = Property(null)

  override protected def composeFallback(): Unit = {
    given Component = this
    addClass("editor")
    addClass("jfx-editor-host")
    attribute("data-client-side", "fallback")

    div {
      addClass("jfx-editor-fallback")
      text = "Editor wird im Browser aktiviert"
    }

    addDisposable(valueProperty.observe(_ => validate()))
    addDisposable(validators.observe(_ => validate()))
    addDisposable(dirtyProperty.observe(_ => validate()))

    if (!standalone) {
      try {
        val formContext = DslRuntime.service[FormContext]
        formContext.registerControl(this)
        addDisposable(() => formContext.unregisterControl(this))
      } catch {
        case _: Exception =>
      }
    }
  }

  override protected def mountClient(): Unit = {
    renderClient {
      given Component = this

      div {
        addClass("jfx-editor")
        div {
          addClass("jfx-editor__shell")
          div {
            addClass("jfx-editor__toolbar")
            div {
              addClass("jfx-editor__toolbar-group")
              div {
                addClass("jfx-editor__toolbar-group-label")
                text = "Client"
              }
              div {
                addClass("jfx-editor__toolbar-buttons")
                div {
                  addClass("jfx-editor__toolbar-button")
                  text = "Lexical Mount vorbereitet"
                }
              }
            }
          }

          div {
            addClass("jfx-editor__surface-wrap")
            div {
              addClass("jfx-editor__surface")
              text = placeholderProperty.map { value =>
                Option(value).map(_.trim).filter(_.nonEmpty).getOrElse("Clientseitiger Editor-Mount")
              }
            }
          }
        }
      }
    }
  }
}

object Editor {
  def editor(name: String, standalone: Boolean = false)(init: Editor ?=> Unit = {}): Editor =
    DslRuntime.build(new Editor(name, standalone))(init)

  def value(using e: Editor): js.Any | Null =
    e.valueProperty.get

  def value_=(using e: Editor)(nextValue: js.Any | Null): Unit =
    e.valueProperty.set(nextValue)

  def valueProperty(using e: Editor): Property[js.Any | Null] =
    e.valueProperty

  def placeholder(using e: Editor): String =
    e.placeholder

  def placeholder_=(using e: Editor)(value: String): Unit =
    e.placeholder = value
}
