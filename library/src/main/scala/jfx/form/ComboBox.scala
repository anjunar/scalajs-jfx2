package jfx.form

import jfx.control.TableView.*
import jfx.control.TableColumn.*
import jfx.control.TableColumn
import jfx.control.TableView
import jfx.core.component.Component.*
import jfx.core.component.Component
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.layout.Overlay.*
import jfx.layout.Overlay
import jfx.statement.Condition.*

class ComboBox[T](override val name: String, override val standalone: Boolean = false) extends Component with Control[T] {
  override def tagName: String = "div"

  override val valueProperty: Property[T] = Property(null.asInstanceOf[T])
  val openProperty: Property[Boolean] = Property(false)
  val itemsProperty: ListProperty[T] = new ListProperty[T]()
  val dropdownWidthProperty: Property[Option[Double]] = Property(None)
  
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
        overlay(dropdownWidthProperty.get) {
          addClass("jfx-combo-box__dropdown")
          
          tableView[T] {
            addClass("jfx-combo-box__table")
            showHeader = false
            rowHeight = 36.0
            TableView.prefWidth = Overlay.effectiveWidth
            
            TableView.items = ComboBox.this.itemsProperty
            
            column[T, Any]("") { (col: TableColumn[T, Any]) ?=>
              TableColumn.prefWidth = Overlay.effectiveWidth
              
              col.setCellRenderer { item =>
                div {
                  addClass("jfx-combo-box__item")
                  div {
                    addClass("jfx-combo-box__item-text")
                    text = item.toString
                  }
                  
                  onClick { _ =>
                    valueProperty.set(item)
                    openProperty.set(false)
                  }
                }
              }
            }
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

  def items[T](using c: ComboBox[T]): ListProperty[T] = c.itemsProperty
  def items_=[T](v: scala.collection.IterableOnce[T])(using c: ComboBox[T]): Unit = c.itemsProperty.setAll(v)

  def dropdownWidth[T](using c: ComboBox[T]): Option[Double] = c.dropdownWidthProperty.get
  def dropdownWidth_=[T](value: Double)(using c: ComboBox[T]): Unit = c.dropdownWidthProperty.set(Some(value))
  def dropdownWidth_=[T](value: Option[Double])(using c: ComboBox[T]): Unit = c.dropdownWidthProperty.set(value)
}
