package jfx.form

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.layout.Div.div
import jfx.layout.HorizontalLine.horizontalLine

object InputContainer {
  def inputContainer(placeholderText: String)(init: => Unit): Box =
    inputContainer(Property(placeholderText))(init)

  def inputContainer(placeholderText: ReadOnlyProperty[String])(init: => Unit): Box = {
    div {
      addClass("jfx-input-container")
      val container = summon[Box]

      val labelDiv = div {
        addClass("jfx-input-container__label")
        Box.box("span") {
          addClass("placeholder")
          addClass("jfx-input-container__placeholder")
          text = placeholderText
        }
      }

      val contentSlot = div {
        addClass("jfx-input-container__control")
        init
      }

      val divider = horizontalLine {
        addClass("jfx-input-container__divider")
      }

      val errorsTextProp = Property("")
      val errorsDiv = div {
        addClass("jfx-input-container__errors")
        text = errorsTextProp
      }

      val controls = collectControls(contentSlot)
      if (controls.nonEmpty) {
        val control = controls.head

        container.addDisposable(placeholderText.observe { value =>
          control.$placeholderProperty.set(value)
        })

        container.addDisposable(control.$valueProperty.observe { value =>
          val empty = value == null || value.toString.trim.isEmpty
          if (empty) container.addBaseClass("empty") else container.removeBaseClass("empty")
        })

        container.addDisposable(control.$focusedProperty.observe { focused =>
          if (focused) labelDiv.addBaseClass("focus") else labelDiv.removeBaseClass("focus")
          if (focused) divider.addBaseClass("focus") else divider.removeBaseClass("focus")
        })

        container.addDisposable(control.$dirtyProperty.observe { dirty =>
          if (dirty) labelDiv.addBaseClass("dirty") else labelDiv.removeBaseClass("dirty")
          if (dirty) divider.addBaseClass("dirty") else divider.removeBaseClass("dirty")
        })

        container.addDisposable(control.$invalidProperty.observe { invalid =>
          if (invalid) labelDiv.addBaseClass("invalid") else labelDiv.removeBaseClass("invalid")
          if (invalid) divider.addBaseClass("invalid") else divider.removeBaseClass("invalid")
        })

        container.addDisposable(control.$errorsProperty.observe { errList =>
          errorsTextProp.set(errList.mkString(", "))
        })
      }
    }
  }

  private def collectControls(component: Component): Seq[Control[?]] =
    component.children.flatMap {
      case control: Control[?] => Seq(control)
      case child               => collectControls(child)
    }
}
