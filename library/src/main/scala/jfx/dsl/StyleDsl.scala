package jfx.dsl

import jfx.control.{TableColumn, TableView}
import jfx.core.component.ElementComponent
import jfx.core.render.HostElement
import jfx.core.state.{ListProperty, ReadOnlyProperty}
import org.scalajs.dom

import scala.Conversion
import scala.annotation.targetName

private[jfx] final case class StyleTarget(
  component: ElementComponent[?],
  host: HostElement
)

final class StyleProperty private[jfx] (
  private val currentValue: () => String,
  private val assignValue: String => Unit,
  private val bindValue: ReadOnlyProperty[String] => Unit
) {

  def value: String =
    currentValue()

  def apply(): String =
    currentValue()

  def :=(value: String): Unit =
    assignValue(value)

  @targetName("bindFromProperty")
  def <--(property: ReadOnlyProperty[String]): Unit =
    bindValue(property)

  override def toString: String =
    currentValue()

}

given Conversion[StyleProperty, String] with {
  override def apply(value: StyleProperty): String =
    value.value
}

def style(init: StyleTarget ?=> Unit)(using component: ElementComponent[?]): Unit =
  component.addStyle(init)

def css(using component: ElementComponent[?]): dom.CSSStyleDeclaration =
  component.css

def setProperty(name: String, value: String)(using target: StyleTarget): Unit = {
  target.component.clearStylePropertyBinding(name)
  target.host.setStyleProperty(name, value)
}

def removeProperty(name: String)(using target: StyleTarget): String = {
  target.component.clearStylePropertyBinding(name)
  target.host.removeStyleProperty(name)
}

def getPropertyValue(name: String)(using target: StyleTarget): String =
  target.host.styleProperty(name)

private[dsl] def styleProperty(bindingKey: String)(using target: StyleTarget): StyleProperty =
  new StyleProperty(
    currentValue = () => target.host.styleProperty(bindingKey),
    assignValue = value => {
      target.component.clearStylePropertyBinding(bindingKey)
      target.host.setStyleProperty(bindingKey, value)
    },
    bindValue = property =>
      target.component.bindStyleProperty(bindingKey, property)(value => target.host.setStyleProperty(bindingKey, value))
  )

def display(using target: StyleTarget): StyleProperty =
  styleProperty("display")

def display_=(value: String)(using target: StyleTarget): Unit =
  display := value

def color(using target: StyleTarget): StyleProperty =
  styleProperty("color")

def color_=(value: String)(using target: StyleTarget): Unit =
  color := value

def background(using target: StyleTarget): StyleProperty =
  styleProperty("background")

def background_=(value: String)(using target: StyleTarget): Unit =
  background := value

def backgroundColor(using target: StyleTarget): StyleProperty =
  styleProperty("background-color")

def backgroundColor_=(value: String)(using target: StyleTarget): Unit =
  backgroundColor := value

def padding(using target: StyleTarget): StyleProperty =
  styleProperty("padding")

def padding_=(value: String)(using target: StyleTarget): Unit =
  padding := value

def margin(using target: StyleTarget): StyleProperty =
  styleProperty("margin")

def margin_=(value: String)(using target: StyleTarget): Unit =
  margin := value

def border(using target: StyleTarget): StyleProperty =
  styleProperty("border")

def border_=(value: String)(using target: StyleTarget): Unit =
  border := value

def borderRadius(using target: StyleTarget): StyleProperty =
  styleProperty("border-radius")

def borderRadius_=(value: String)(using target: StyleTarget): Unit =
  borderRadius := value

def width(using target: StyleTarget): StyleProperty =
  styleProperty("width")

def width_=(value: String)(using target: StyleTarget): Unit =
  width := value

def height(using target: StyleTarget): StyleProperty =
  styleProperty("height")

def height_=(value: String)(using target: StyleTarget): Unit =
  height := value

def minWidth(using target: StyleTarget): StyleProperty =
  styleProperty("min-width")

def minWidth_=(value: String)(using target: StyleTarget): Unit =
  minWidth := value

def minHeight(using target: StyleTarget): StyleProperty =
  styleProperty("min-height")

def minHeight_=(value: String)(using target: StyleTarget): Unit =
  minHeight := value

def maxWidth(using target: StyleTarget): StyleProperty =
  styleProperty("max-width")

def maxWidth_=(value: String)(using target: StyleTarget): Unit =
  maxWidth := value

def maxHeight(using target: StyleTarget): StyleProperty =
  styleProperty("max-height")

def maxHeight_=(value: String)(using target: StyleTarget): Unit =
  maxHeight := value

def flex(using target: StyleTarget): StyleProperty =
  styleProperty("flex")

def flex_=(value: String)(using target: StyleTarget): Unit =
  flex := value

def gap(using target: StyleTarget): StyleProperty =
  styleProperty("gap")

def gap_=(value: String)(using target: StyleTarget): Unit =
  gap := value

def alignItems(using target: StyleTarget): StyleProperty =
  styleProperty("align-items")

def alignItems_=(value: String)(using target: StyleTarget): Unit =
  alignItems := value

def justifyContent(using target: StyleTarget): StyleProperty =
  styleProperty("justify-content")

def justifyContent_=(value: String)(using target: StyleTarget): Unit =
  justifyContent := value

def columns[S](using tableView: TableView[S]): ListProperty[TableColumn[S, ?]] =
  tableView.columnsProperty

def columns_=[S](value: IterableOnce[TableColumn[S, ?]])(using tableView: TableView[S]): Unit = {
  tableView.columnsProperty.clear()
  value.iterator.foreach(column => tableView.columnsProperty += column)
}

def minWidth(using tableColumn: TableColumn[?, ?]): Double =
  tableColumn.getMinWidth

def minWidth_=(value: Double)(using tableColumn: TableColumn[?, ?]): Unit =
  tableColumn.setMinWidth(value)

def maxWidth_=(value: Double)(using tableColumn: TableColumn[?, ?]): Unit =
  tableColumn.setMaxWidth(value)
