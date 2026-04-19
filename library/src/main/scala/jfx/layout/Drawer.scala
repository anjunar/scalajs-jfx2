package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime, StyleProxy}
import org.scalajs.dom.{Event, KeyboardEvent, window}
import scala.compiletime.uninitialized

class Drawer extends Box("div") {
  val openProperty = Property(false)
  val drawerWidthProperty = Property("280px")
  val sideProperty = Property(Drawer.Side.Start)
  val closeOnScrimClickProperty = Property(true)

  private var _navigationHost: Box = uninitialized
  private var _contentHost: Box = uninitialized

  def navigationHost: Box = _navigationHost
  def contentHost: Box = _contentHost

  override def compose(): Unit = {
    given Component = this
    addBaseClass("jfx-drawer")
    
    // Hardcode layout styles to prevent DSL collisions
    host.setStyle("display", "flex")
    host.setStyle("width", "100%")
    host.setStyle("height", "100%")
    host.setStyle("position", "relative")

    addDisposable(openProperty.observe(syncOpenState))
    addDisposable(drawerWidthProperty.observe(_ => syncPanelWidth()))
    addDisposable(sideProperty.observe(syncSideState))

    // 1. Scrim
    Box.box("div") {
      addClass("jfx-drawer__scrim")
      addDisposable(host.addEventListener("click", _ => {
        if (closeOnScrimClickProperty.get && openProperty.get) openProperty.set(false)
      }))
    }

    // 2. Navigation Panel
    Box.box("div") {
      addClass("jfx-drawer__panel-shell")
      style { 
        height = "100%"
        position = "relative"
        zIndex = "100"
      }
      
      addDisposable(drawerWidthProperty.observe(_ => syncPanelWidth()))
      addDisposable(openProperty.observe(_ => syncPanelWidth()))

      Box.box("div") {
        addClass("jfx-drawer__panel")
        style { 
          height = "100%"
          overflow = "hidden"
        }
        
        _navigationHost = Box.box("div") {
          addClass("jfx-drawer__navigation")
          style { 
            height = "100%" 
            display = "flex"
            flexDirection = "column"
          }
        }
      }
    }

    // 3. Main Content Area
    _contentHost = Box.box("div") {
      addClass("jfx-drawer__content")
      style { 
        height = "100%"
        flex = "1"
        display = "flex"
        flexDirection = "column"
        minWidth = "0" 
      }
    }

    if (!jfx.core.render.RenderBackend.current.isServer) {
      addDisposable({
        val listener: KeyboardEvent => Unit = e => {
          if (e.key == "Escape" && openProperty.get) openProperty.set(false)
        }
        window.addEventListener("keydown", listener)
        () => window.removeEventListener("keydown", listener)
      })
    }

    syncOpenState(openProperty.get)
    syncSideState(sideProperty.get)
    syncPanelWidth()
  }

  private def syncOpenState(open: Boolean): Unit = {
    if (open) addBaseClass("jfx-drawer--open")
    else removeBaseClass("jfx-drawer--open")
  }

  private def syncSideState(side: Drawer.Side): Unit = {
    removeBaseClass("jfx-drawer--start")
    removeBaseClass("jfx-drawer--end")
    side match {
      case Drawer.Side.Start => addBaseClass("jfx-drawer--start")
      case Drawer.Side.End => addBaseClass("jfx-drawer--end")
    }
  }

  private def syncPanelWidth(): Unit = {
    children.collectFirst { case b: Box if b.baseClasses.contains("jfx-drawer__panel-shell") => b }.foreach { shell =>
      val widthValue = drawerWidthProperty.get
      val responsiveWidth = s"min(92vw, $widthValue)"
      shell.host.setStyle("width", if (openProperty.get) responsiveWidth else "0px")
      
      // Also sync the inner panel
      shell.children.collectFirst { case b: Box if b.baseClasses.contains("jfx-drawer__panel") => b }.foreach { panel =>
         panel.host.setStyle("width", widthValue)
      }
    }
  }
}

object Drawer {
  enum Side { case Start, End }

  def drawer(init: Drawer ?=> Unit): Drawer = {
    DslRuntime.build(new Drawer())(init)
  }

  def drawerNavigation(init: => Unit)(using d: Drawer): Unit = {
    DslRuntime.withComponentScope(d.navigationHost) {
      init
    }
  }

  def drawerContent(init: => Unit)(using d: Drawer): Unit = {
    DslRuntime.withComponentScope(d.contentHost) {
      init
    }
  }

  def open(using d: Drawer): Boolean = d.openProperty.get
  def open_=(v: Boolean)(using d: Drawer): Unit = d.openProperty.set(v)
  def openProperty(using d: Drawer): Property[Boolean] = d.openProperty
  
  def side(using d: Drawer): Side = d.sideProperty.get
  def side_=(s: Side)(using d: Drawer): Unit = d.sideProperty.set(s)

  def drawerWidth(using d: Drawer): String = d.drawerWidthProperty.get
  def drawerWidth_=(w: String)(using d: Drawer): Unit = d.drawerWidthProperty.set(w)

  def toggle()(using d: Drawer): Unit = d.openProperty.set(!d.openProperty.get)
}
