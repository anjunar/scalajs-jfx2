package jfx.layout

import jfx.core.component.NativeComponent
import jfx.core.render.RenderBackend
import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.{Event, HTMLDivElement, KeyboardEvent, window}

final class Drawer extends NativeComponent[HTMLDivElement]("div") {

  val openProperty: Property[Boolean] = Property(false)
  val widthProperty: Property[String] = Property("280px")
  val sideProperty: Property[Drawer.Side] = Property(Drawer.Side.Start)
  val closeOnScrimClickProperty: Property[Boolean] = Property(true)

  private val scrim = DslRuntime.withComponentContext(ComponentContext(Some(this))) { new Div() }
  private val panelShell = DslRuntime.withComponentContext(ComponentContext(Some(this))) { new Div() }
  private val panel = DslRuntime.withComponentContext(ComponentContext(Some(panelShell))) { new Div() }
  private val navigationHost = DslRuntime.withComponentContext(ComponentContext(Some(panel))) { new Div() }
  private val contentHost = DslRuntime.withComponentContext(ComponentContext(Some(this))) { new Div() }

  private val structureSlot = reserveChildSlot()

  private var structureInitialized = false

  classProperty += "jfx-drawer"

  ensureStructure()

  override protected def mountContent(): Unit = {
    super.mountContent()
  }

  private val openObserver = openProperty.observe(syncOpenState)
  addDisposable(openObserver)

  private val widthObserver = widthProperty.observe(_ => syncPanelWidth())
  addDisposable(widthObserver)

  private val sideObserver = sideProperty.observe(syncSideState)
  addDisposable(sideObserver)

  private val scrimClickListener: Event => Unit = _ => {
    if (closeOnScrimClickProperty.get && openProperty.get) {
      close()
    }
  }

  scrim.addEventListener("click", scrimClickListener)
  // ElementComponent.addEventListener already handles disposable

  private val keyDownListener: KeyboardEvent => Unit = event => {
    if (event.key == "Escape" && openProperty.get) {
      close()
    }
  }

  if (!RenderBackend.current.isServer) {
    window.addEventListener("keydown", keyDownListener)
    addDisposable(() => window.removeEventListener("keydown", keyDownListener))
  }

  def isOpen: Boolean =
    openProperty.get

  def isOpen_=(value: Boolean): Unit =
    openProperty.set(value)

  def width: String =
    widthProperty.get

  def width_=(value: String): Unit =
    widthProperty.set(value)

  def closeOnScrimClick: Boolean =
    closeOnScrimClickProperty.get

  def closeOnScrimClick_=(value: Boolean): Unit =
    closeOnScrimClickProperty.set(value)

  def side: Drawer.Side =
    sideProperty.get

  def side_=(value: Drawer.Side): Unit =
    sideProperty.set(value)

  def open(): Unit =
    openProperty.set(true)

  def close(): Unit =
    openProperty.set(false)

  def toggle(): Unit =
    openProperty.set(!openProperty.get)

  private def ensureStructure(): Unit =
    if (!structureInitialized) {
      structureInitialized = true

      scrim.classProperty += "jfx-drawer__scrim"
      panelShell.classProperty += "jfx-drawer__panel-shell"
      panel.classProperty += "jfx-drawer__panel"
      navigationHost.classProperty += "jfx-drawer__navigation"
      contentHost.classProperty += "jfx-drawer__content"

      panelShell.hostElement.setStyleProperty("height", "100%")
      panel.hostElement.setStyleProperty("height", "100%")
      navigationHost.hostElement.setStyleProperty("height", "100%")
      contentHost.hostElement.setStyleProperty("height", "100%")

      structureSlot.replace(Vector(scrim, panelShell, contentHost))

      panelShell.addChild(panel)
      panel.addChild(navigationHost)

      syncPanelWidth()
      syncSideState(sideProperty.get)
    }

  private[jfx] def navigationHostComponent: Div =
    navigationHost

  private[jfx] def contentHostComponent: Div =
    contentHost

  private def syncOpenState(isOpen: Boolean): Unit =
    if (isOpen) {
      classProperty += "jfx-drawer--open"
      syncPanelWidth()
    } else {
      classProperty -= "jfx-drawer--open"
      syncPanelWidth()
    }

  private def syncPanelWidth(): Unit = {
    val widthValue = widthProperty.get
    val responsiveWidth = s"min(92vw, $widthValue)"
    panel.hostElement.setStyleProperty("width", widthValue)
    panelShell.hostElement.setStyleProperty("width", if (openProperty.get) responsiveWidth else "0px")
  }

  private def syncSideState(side: Drawer.Side): Unit = {
    classProperty -= "jfx-drawer--start"
    classProperty -= "jfx-drawer--end"
    classProperty += (
      side match {
        case Drawer.Side.Start => "jfx-drawer--start"
        case Drawer.Side.End => "jfx-drawer--end"
      }
    )
  }
}

object Drawer {

  enum Side {
    case Start, End
  }

  def drawer(init: Drawer ?=> Unit = {}): Drawer =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new Drawer()

      DslRuntime.withComponentContext(ComponentContext(Some(component))) {
        given Scope = currentScope
        given Drawer = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }

  def drawerNavigation(init: => Unit)(using drawer: Drawer): Unit =
    renderSection(drawer.navigationHostComponent)(init)

  def drawerContent(init: => Unit)(using drawer: Drawer): Unit =
    renderSection(drawer.contentHostComponent)(init)

  def drawerOpen(using drawer: Drawer): Boolean =
    drawer.isOpen

  def drawerOpen_=(value: Boolean)(using drawer: Drawer): Unit =
    drawer.isOpen = value

  def drawerWidth(using drawer: Drawer): String =
    drawer.width

  def drawerWidth_=(value: String)(using drawer: Drawer): Unit =
    drawer.width = value

  def closeOnScrimClick(using drawer: Drawer): Boolean =
    drawer.closeOnScrimClick

  def closeOnScrimClick_=(value: Boolean)(using drawer: Drawer): Unit =
    drawer.closeOnScrimClick = value

  def drawerSide(using drawer: Drawer): Drawer.Side =
    drawer.side

  def drawerSide_=(value: Drawer.Side)(using drawer: Drawer): Unit =
    drawer.side = value

  def openDrawer(using drawer: Drawer): Unit =
    drawer.open()

  def closeDrawer(using drawer: Drawer): Unit =
    drawer.close()

  def toggleDrawer(using drawer: Drawer): Unit =
    drawer.toggle()

  private def renderSection(host: Div)(init: => Unit): Unit =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      DslRuntime.withComponentContext(ComponentContext(Some(host))) {
        given Scope = currentScope
        given Div = host
        init
      }
    }
}
