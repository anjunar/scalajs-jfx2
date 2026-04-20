package jfx.form

import jfx.control.TableView.*
import jfx.control.TableColumn.*
import jfx.control.TableColumn
import jfx.control.TableView
import jfx.core.component.Component.*
import jfx.core.component.Component
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.dsl.DslRuntime
import jfx.layout.Div.div
import jfx.layout.Overlay.*
import jfx.layout.Overlay
import jfx.statement.Condition
import jfx.statement.Condition.*
import org.scalajs.dom
import scala.scalajs.js

class ComboBox[T](override val name: String, override val standalone: Boolean = false) extends Component with Control[T] {
  override def tagName: String = "div"

  // -- Properties --
  override val valueProperty: Property[T] = Property(null.asInstanceOf[T])
  val selectionProperty: ListProperty[T] = new ListProperty[T]()
  val itemsProperty: ListProperty[T] = new ListProperty[T]()
  
  val openProperty: Property[Boolean] = Property(false)
  val multipleSelectionProperty: Property[Boolean] = Property(false)
  val dropdownWidthProperty: Property[Option[Double]] = Property(None)
  val dropdownHeightProperty: Property[Double] = Property(240.0)
  val rowHeightProperty: Property[Double] = Property(36.0)
  
  val converterProperty: Property[T => String] = Property((v: T) => if (v == null) "" else v.toString)
  
  type Renderer = T => Unit
  val itemRendererProperty: Property[Option[Renderer]] = Property(None)
  val valueRendererProperty: Property[Option[Renderer]] = Property(None)
  val footerRendererProperty: Property[Option[() => Unit]] = Property(None)

  // -- Reactive Helpers --
  private val displayText = selectionProperty.asProperty.flatMap { list =>
    val first = list.headOption.getOrElse(null.asInstanceOf[T])
    if (first == null) placeholderProperty else Property(converterProperty.get(first))
  }

  private val isPlaceholder = selectionProperty.asProperty.map(_.isEmpty)

  override def compose(): Unit = {
    given Component = this
    addClass("jfx-combo-box")
    classIf("jfx-combo-box-open", openProperty)
    
    tabIndex = 0
    role = "combobox"
    attribute("aria-expanded", openProperty.map(_.toString))

    onClick { _ => toggle() }
    
    onKeyDown { e =>
      e.key match {
        case "ArrowDown" | "ArrowUp" => 
          e.preventDefault()
          if (!openProperty.get) openProperty.set(true)
        case "Enter" | " " => 
          e.preventDefault()
          toggle()
        case "Escape" => 
          if (openProperty.get) {
            e.preventDefault()
            openProperty.set(false)
          }
        case _ => 
      }
    }

    div {
      addClass("jfx-combo-box__value")
      
      condition(valueRendererProperty.map(_.isDefined)) {
        thenDo {
          val cond = summon[Condition]
          addDisposable(selectionProperty.observe { list =>
            DslRuntime.updateBranch(cond) {
               val selected = list.headOption.getOrElse(null.asInstanceOf[T])
               if (selected != null) valueRendererProperty.get.foreach(_(selected))
            }
          })
        }
        elseDo {
          div {
            addClass("jfx-combo-box__value-text")
            classIf("is-placeholder", isPlaceholder)
            text = displayText
          }
        }
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
          div {
            addClass("jfx-combo-box__dropdown")
            style { maxHeight = s"${dropdownHeightProperty.get}px" }
            
            tableView[T] {
              addClass("jfx-combo-box__table")
              showHeader = false
              rowHeight = rowHeightProperty.get
              TableView.prefWidth = Overlay.effectiveWidth.map(_ - 2.0)
              
              TableView.items = ComboBox.this.itemsProperty
              
              column[T, Any]("") { (col: TableColumn[T, Any]) ?=>
                TableColumn.prefWidth = Overlay.effectiveWidth.map(_ - 4.0)
                
                col.setCellRenderer { item =>
                  div {
                    addClass("jfx-combo-box__item")
                    classIf("is-selected", selectionProperty.asProperty.map(_.contains(item)))
                    
                    onClick { _ => selectItem(item) }

                    condition(itemRendererProperty.map(_.isDefined)) {
                      thenDo { itemRendererProperty.get.foreach(_(item)) }
                      elseDo {
                        div {
                          addClass("jfx-combo-box__item-text")
                          text = converterProperty.get(item)
                        }
                      }
                    }
                  }
                }
              }
            }
            
            condition(footerRendererProperty.map(_.isDefined)) {
              thenDo {
                div {
                  addClass("jfx-combo-box__footer")
                  footerRendererProperty.get.foreach(_())
                }
              }
            }
          }
        }
      }
    }
    
    addDisposable(selectionProperty.observe { list =>
       valueProperty.set(list.headOption.getOrElse(null.asInstanceOf[T]))
    })
  }

  def toggle(): Unit = openProperty.set(!openProperty.get)

  def selectItem(item: T): Unit = {
    if (multipleSelectionProperty.get) {
      if (selectionProperty.contains(item)) selectionProperty -= item
      else selectionProperty += item
    } else {
      selectionProperty.setAll(Seq(item))
      openProperty.set(false)
    }
    setDirty(true)
  }
}

object ComboBox {
  def comboBox[T](name: String)(init: ComboBox[T] ?=> Unit): ComboBox[T] = {
    DslRuntime.build(new ComboBox[T](name))(init)
  }

  def items[T](using c: ComboBox[T]): ListProperty[T] = c.itemsProperty
  def items_=[T](v: scala.collection.IterableOnce[T])(using c: ComboBox[T]): Unit = c.itemsProperty.setAll(v)

  def placeholder[T](using c: ComboBox[T]): String = c.placeholder
  def placeholder_=[T](value: String)(using c: ComboBox[T]): Unit = c.placeholder = value

  def selection[T](using c: ComboBox[T]): ListProperty[T] = c.selectionProperty
  def valueProperty[T](using c: ComboBox[T]): Property[T] = c.valueProperty

  def multiSelect(using c: ComboBox[?]): Boolean = c.multipleSelectionProperty.get
  def multiSelect_=(v: Boolean)(using c: ComboBox[?]): Unit = c.multipleSelectionProperty.set(v)

  def itemRenderer[T](renderer: T => Unit)(using c: ComboBox[T]): Unit = c.itemRendererProperty.set(Some(renderer))
  def valueRenderer[T](renderer: T => Unit)(using c: ComboBox[T]): Unit = c.valueRendererProperty.set(Some(renderer))
  def footerRenderer(renderer: => Unit)(using c: ComboBox[?]): Unit = c.footerRendererProperty.set(Some(() => renderer))

  def converter[T](using c: ComboBox[T]): T => String = c.converterProperty.get
  def converter_=[T](v: T => String)(using c: ComboBox[T]): Unit = c.converterProperty.set(v)
}
