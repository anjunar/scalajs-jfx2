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
import jfx.statement.ForEach
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
  val identityProperty: Property[T => Any] = Property((v: T) => v.asInstanceOf[Any])
  
  type Renderer = (T, ReadOnlyProperty[Boolean]) => Unit
  val itemRendererProperty: Property[Option[Renderer]] = Property(None)
  val valueRendererProperty: Property[Option[T => Unit]] = Property(None)
  val footerRendererProperty: Property[Option[() => Unit]] = Property(None)

  // -- Identity Logic --
  private def isSame(a: T, b: T): Boolean = {
    val aAny = a.asInstanceOf[Any]
    val bAny = b.asInstanceOf[Any]
    if (aAny == null || bAny == null) aAny == bAny
    else identityProperty.get(a) == identityProperty.get(b)
  }

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
          // Wir nutzen ForEach für die selektierten Elemente.
          // Das ist viel sauberer und handhabt das Entfernen alter Komponenten automatisch.
          ForEach.forEach(selectionProperty) { item =>
            valueRendererProperty.get.foreach(_(item))
          }
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
    
    // -- Dropdown Overlay --
    condition(openProperty) {
      thenDo {
        overlay(dropdownWidthProperty.get) {
          div {
            addClass("jfx-combo-box__dropdown")
            val h = dropdownHeightProperty.get
            style { maxHeight = s"${h}px"; display = "flex"; flexDirection = "column" }
            
            tableView[T] {
              addClass("jfx-combo-box__table")
              showHeader = false
              rowHeight = rowHeightProperty.get
              style { height = s"${h}px"; width = "100%" }
              
              TableView.prefWidth = Overlay.effectiveWidth.map(_ - 2.0)
              
              TableView.items = ComboBox.this.itemsProperty
              
              column[T, Any]("") { (col: TableColumn[T, Any]) ?=>
                TableColumn.prefWidth = Overlay.effectiveWidth.map(_ - 4.0)
                
                col.setCellRenderer { item =>
                  div {
                    addClass("jfx-combo-box__item")
                    classIf("is-selected", selectionProperty.asProperty.map(list => list.toSeq.exists(isSame(_, item))))
                    
                    onClick { _ => selectItem(item) }

                    condition(itemRendererProperty.map(_.isDefined)) {
                      thenDo { itemRendererProperty.get.foreach(_(item, selectionProperty.asProperty.map(list => list.toSeq.exists(isSame(_, item))))) }
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
      val idx = selectionProperty.toSeq.indexWhere(isSame(_, item))
      if (idx >= 0) selectionProperty.remove(idx)
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
  def items_=[T](using c: ComboBox[T])(v: scala.collection.IterableOnce[T]): Unit = c.itemsProperty.setAll(v)

  def placeholder(using c: ComboBox[?]): String = c.placeholderProperty.get
  def placeholder_=(using c: ComboBox[?])(v: String): Unit = c.placeholderProperty.set(v)

  def converter[T](using c: ComboBox[T]): T => String = c.converterProperty.get
  def converter_=[T](using c: ComboBox[T])(v: T => String): Unit = c.converterProperty.set(v)

  def identityBy[T](using c: ComboBox[T]): T => Any = c.identityProperty.get
  def identityBy_=[T](using c: ComboBox[T])(v: T => Any): Unit = c.identityProperty.set(v)

  def rowHeight(using c: ComboBox[?]): Double = c.rowHeightProperty.get
  def rowHeight_=(using c: ComboBox[?])(v: Double): Unit = c.rowHeightProperty.set(v)

  def dropdownHeight(using c: ComboBox[?]): Double = c.dropdownHeightProperty.get
  def dropdownHeight_=(using c: ComboBox[?])(v: Double): Unit = c.dropdownHeightProperty.set(v)

  def multiSelect(using c: ComboBox[?]): Boolean = c.multipleSelectionProperty.get
  def multiSelect_=(using c: ComboBox[?])(v: Boolean): Unit = c.multipleSelectionProperty.set(v)

  def itemRenderer[T](using c: ComboBox[T])(v: (T, ReadOnlyProperty[Boolean]) => Unit): Unit = 
    c.itemRendererProperty.set(Some(v))
    
  def valueRenderer[T](using c: ComboBox[T])(v: T => Unit): Unit = 
    c.valueRendererProperty.set(Some(v))
    
  def footerRenderer(using c: ComboBox[?])(v: => Unit): Unit = 
    c.footerRendererProperty.set(Some(() => v))
}
