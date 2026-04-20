package jfx.form

import jfx.core.component.Component.*
import jfx.core.component.Component
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.layout.Overlay.overlay
import jfx.statement.Condition.*

class ComboBox[T](override val name: String, override val standalone: Boolean = false) extends Component with Control[T] {
  override def tagName: String = "div"

  override val valueProperty: Property[T] = Property(null.asInstanceOf[T])
  val openProperty: Property[Boolean] = Property(false)
  
  private val displayText = valueProperty.flatMap { v =>
    if (v == null) placeholderProperty else Property(v.toString)
  }

  private val isPlaceholder = valueProperty.map(_ == null)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-combo-box")
    classIf("jfx-combo-box-open", openProperty)

    onClick { _ => openProperty.set(!openProperty.get) }

    div {
      addClass("jfx-combo-box__value")

      div {
        addClass("jfx-combo-box__value-text")
        classIf("is-placeholder", isPlaceholder)
        text = displayText
      }
    }

    div {
      addClass("jfx-combo-box__indicator")
      addClass("material-icons")
      text = "arrow_drop_down"
    }
    
    condition(openProperty) {
      thenDo {
        overlay {
          addClass("jfx-combo-box__dropdown")
          div {
            style { 
              padding = "12px"
              color = "var(--aj-ink-muted)"
              fontSize = "13px"
              textAlign = "center"
            }
            text = "Liste wird in Kürze implementiert..."
          }
        }
      }
    }
  }
}

object ComboBox {
  def comboBox[T](name: String)(init: ComboBox[T] ?=> Unit): ComboBox[T] = {
    DslRuntime.build(new ComboBox[T](name))(init)
  }

  def placeholder[T](using c: ComboBox[T]): String = c.placeholder
  def placeholder_=[T](value: String)(using c: ComboBox[T]): Unit = c.placeholder = value
  
  def valueProperty[T](using c: ComboBox[T]): Property[T] = c.valueProperty
  
  def open[T](using c: ComboBox[T]): Boolean = c.openProperty.get
  def open_=[T](value: Boolean)(using c: ComboBox[T]): Unit = c.openProperty.set(value)
}
