package jfx.control

import jfx.core.state.{CompositeDisposable, Disposable, Property, ReadOnlyProperty}
import jfx.dsl.{DslRuntime, Scope}

import scala.collection.mutable

class TableColumn[S, T](initialText: String = "") {

  val textProperty: Property[String] = Property(initialText)
  val prefWidthProperty: Property[Double] = Property(160.0)
  val minWidthProperty: Property[Double] = Property(40.0)
  val maxWidthProperty: Property[Double] = Property(Double.PositiveInfinity)
  val sortableProperty: Property[Boolean] = Property(false)
  val sortKeyProperty: Property[String | Null] = Property(null)
  val resizableProperty: Property[Boolean] = Property(true)
  val cellValueFactoryProperty: Property[TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null] =
    Property(null)
  val cellFactoryProperty: Property[TableColumn[S, T] => TableCell[S, T] | Null] =
    Property(null)

  def getText: String = textProperty.get
  def setText(value: String): Unit = textProperty.set(if (value == null) "" else value)
  def text: String = getText
  def text_=(value: String): Unit = setText(value)

  def getPrefWidth: Double = prefWidthProperty.get
  def setPrefWidth(value: Double): Unit = prefWidthProperty.set(value)
  def prefWidth: Double = getPrefWidth
  def prefWidth_=(value: Double): Unit = setPrefWidth(value)

  def getMinWidth: Double = minWidthProperty.get
  def setMinWidth(value: Double): Unit = minWidthProperty.set(value)

  def getMaxWidth: Double = maxWidthProperty.get
  def setMaxWidth(value: Double): Unit = maxWidthProperty.set(value)

  def isSortable: Boolean = sortableProperty.get
  def setSortable(value: Boolean): Unit = sortableProperty.set(value)
  def getSortKey: String | Null = sortKeyProperty.get
  def setSortKey(value: String | Null): Unit =
    sortKeyProperty.set(
      if (value == null) null
      else {
        val trimmed = value.trim
        if (trimmed.isEmpty) null else trimmed
      }
    )

  def isResizable: Boolean = resizableProperty.get
  def setResizable(value: Boolean): Unit = resizableProperty.set(value)

  def getCellValueFactory: TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null =
    cellValueFactoryProperty.get

  def setCellValueFactory(factory: TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null): Unit =
    cellValueFactoryProperty.set(factory)

  def getCellFactory: TableColumn[S, T] => TableCell[S, T] | Null =
    cellFactoryProperty.get

  def setCellFactory(factory: TableColumn[S, T] => TableCell[S, T] | Null): Unit =
    cellFactoryProperty.set(factory)

  private[control] def effectiveWidth: Double = {
    val maxWidth = maxWidthProperty.get
    val minWidth = minWidthProperty.get
    val preferredWidth = prefWidthProperty.get
    val boundedMax = math.max(minWidth, maxWidth)
    math.max(minWidth, math.min(boundedMax, preferredWidth))
  }

  private[control] def resolveCellValue(
    tableView: TableView[S],
    rowValue: S,
    rowIndex: Int
  ): ReadOnlyProperty[T] | Null = {
    val factory = cellValueFactoryProperty.get
    if (factory == null) null
    else factory(TableColumn.CellDataFeatures(tableView, this, rowValue, rowIndex))
  }

  private[control] def createCell(): TableCell[S, T] = {
    val factory = cellFactoryProperty.get
    if (factory == null) new TableCell[S, T]()
    else {
      val cell = factory(this)
      if (cell == null) new TableCell[S, T]() else cell
    }
  }

  private[control] def observeColumnState(listener: () => Unit): Disposable = {
    val composite = new CompositeDisposable()
    composite.add(observeWithoutInitial(textProperty)(_ => listener()))
    composite.add(observeWithoutInitial(prefWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(minWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(maxWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(sortableProperty)(_ => listener()))
    composite.add(observeWithoutInitial(sortKeyProperty)(_ => listener()))
    composite.add(observeWithoutInitial(resizableProperty)(_ => listener()))
    composite.add(observeWithoutInitial(cellValueFactoryProperty)(_ => listener()))
    composite.add(observeWithoutInitial(cellFactoryProperty)(_ => listener()))
    composite
  }

  private def observeWithoutInitial[V](property: Property[V])(listener: V => Unit): Disposable = {
    var first = true
    property.observe { value =>
      if (first) first = false
      else listener(value)
    }
  }
}

object TableColumn {

  private val enclosingTableViewStack = mutable.ArrayBuffer.empty[TableView[?]]

  def tableColumn[S, T](text: String): TableColumn[S, T] =
    tableColumn(text)({})

  def tableColumn[S, T](text: String)(init: TableColumn[S, T] ?=> Unit): TableColumn[S, T] =
    DslRuntime.currentScope { currentScope =>
      val component = new TableColumn[S, T](text)
      given Scope = currentScope
      given TableColumn[S, T] = component
      init
      currentEnclosingTableView[S]().foreach(_.columnsProperty += component)
      component
    }

  def column[S, T](text: String): TableColumn[S, T] =
    tableColumn(text)

  def column[S, T](text: String)(init: TableColumn[S, T] ?=> Unit): TableColumn[S, T] =
    tableColumn(text)(init)

  def header(using component: TableColumn[?, ?]): String =
    component.getText

  def header_=(value: String)(using component: TableColumn[?, ?]): Unit =
    component.setText(value)

  def prefWidth(using tableColumn: TableColumn[?, ?]): Double =
    tableColumn.getPrefWidth

  def prefWidth_=(value: Double)(using tableColumn: TableColumn[?, ?]): Unit =
    tableColumn.setPrefWidth(value)

  def columnMaxWidth(using tableColumn: TableColumn[?, ?]): Double =
    tableColumn.getMaxWidth

  def sortable(using tableColumn: TableColumn[?, ?]): Boolean =
    tableColumn.isSortable

  def sortable_=(value: Boolean)(using tableColumn: TableColumn[?, ?]): Unit =
    tableColumn.setSortable(value)

  def sortKey(using tableColumn: TableColumn[?, ?]): String | Null =
    tableColumn.getSortKey

  def sortKey_=(value: String | Null)(using tableColumn: TableColumn[?, ?]): Unit =
    tableColumn.setSortKey(value)

  def resizable(using tableColumn: TableColumn[?, ?]): Boolean =
    tableColumn.isResizable

  def resizable_=(value: Boolean)(using tableColumn: TableColumn[?, ?]): Unit =
    tableColumn.setResizable(value)

  def cellValueFactory[S, T](using tableColumn: TableColumn[S, T]): TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null =
    tableColumn.getCellValueFactory

  def cellValueFactory_=[S, T](
    factory: TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null
  )(using tableColumn: TableColumn[S, T]): Unit =
    tableColumn.setCellValueFactory(factory)

  def cellFactory[S, T](using tableColumn: TableColumn[S, T]): TableColumn[S, T] => TableCell[S, T] | Null =
    tableColumn.getCellFactory

  def cellFactory_=[S, T](
    factory: TableColumn[S, T] => TableCell[S, T] | Null
  )(using tableColumn: TableColumn[S, T]): Unit =
    tableColumn.setCellFactory(factory)

  private[control] def withEnclosingTableView[S, A](tableView: TableView[S])(block: => A): A = {
    enclosingTableViewStack += tableView
    try block
    finally enclosingTableViewStack.remove(enclosingTableViewStack.length - 1)
  }

  private def currentEnclosingTableView[S](): Option[TableView[S]] =
    enclosingTableViewStack.lastOption.map(_.asInstanceOf[TableView[S]])

  final case class CellDataFeatures[S, T](
    tableView: TableView[S],
    tableColumn: TableColumn[S, T],
    value: S,
    index: Int
  ) {
    def getTableView: TableView[S] = tableView
    def getTableColumn: TableColumn[S, T] = tableColumn
    def getValue: S = value
    def getIndex: Int = index
  }
}
