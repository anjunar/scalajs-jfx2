package jfx.form

import jfx.core.component.{Box, Component}
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.core.component.Component.*
import org.scalajs.dom

import jfx.layout.Div.div
import jfx.layout.HorizontalLine.horizontalLine

object InputContainer {
  def inputContainer(placeholderText: String)(init: => Unit): Box = {
    div {
      addClass("jfx-input-container")
      val container = summon[Box]

      val labelDiv = div {
        addClass("jfx-input-container__label")
        div {
          addClass("placeholder")
          text = placeholderText
        }
      }

      val contentSlot = div {
        addClass("jfx-input-container__control")
        init // Render child control here
      }

      val divider = horizontalLine {
        addClass("jfx-input-container__divider")
      }

      val errorsTextProp = Property("")
      val errorsDiv = div {
        addClass("jfx-input-container__errors")
        text = errorsTextProp
      }

      // Bind states
      val controls = contentSlot.children.collect { case c: Control[?] => c }
      if (controls.nonEmpty) {
        val control = controls.head
        
        if (control.placeholderProperty.get.trim.isEmpty && placeholderText.trim.nonEmpty) {
          control.placeholderProperty.set(placeholderText)
        }

        container.addDisposable(control.valueProperty.observe { value =>
          val empty = value == null || value.toString.trim.isEmpty
          if (empty) container.addBaseClass("empty") else container.removeBaseClass("empty")
        })

        container.addDisposable(control.focusedProperty.observe { focused =>
          if (focused) labelDiv.addBaseClass("focus") else labelDiv.removeBaseClass("focus")
          if (focused) divider.addBaseClass("focus") else divider.removeBaseClass("focus")
        })

        container.addDisposable(control.dirtyProperty.observe { dirty =>
          if (dirty) labelDiv.addBaseClass("dirty") else labelDiv.removeBaseClass("dirty")
          if (dirty) divider.addBaseClass("dirty") else divider.removeBaseClass("dirty")
        })

        container.addDisposable(control.invalidProperty.observe { invalid =>
          if (invalid) labelDiv.addBaseClass("invalid") else labelDiv.removeBaseClass("invalid")
          if (invalid) divider.addBaseClass("invalid") else divider.removeBaseClass("invalid")
        })

        container.addDisposable(control.errorsProperty.observe { errList =>
          errorsTextProp.set(errList.mkString(", "))
        })
      }
    }
  }
}
