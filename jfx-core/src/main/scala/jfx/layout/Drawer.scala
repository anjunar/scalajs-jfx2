package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime, StyleProxy}
import scala.compiletime.uninitialized

class Drawer extends Box("div") {
  val $openProperty = Property(false)
  val $drawerWidthProperty = Property("280px")
  val $sideProperty = Property(Drawer.Side.Start)
  val $closeOnScrimClickProperty = Property(true)

  private var _navigationHost: Box = uninitialized
  private var _contentHost: Box = uninitialized

  def $navigationHost: Box = _navigationHost
  def $contentHost: Box = _contentHost

  private val panelShellWidthProperty =
    $openProperty.flatMap { open =>
      $drawerWidthProperty.map { width =>
        if (open) s"min(92vw, $width)" else "0px"
      }
    }

  override def compose(): Unit = {
    given Component = this
    addBaseClass("jfx-drawer")
    classIf("jfx-drawer--open", $openProperty)
    classIf("jfx-drawer--start", $sideProperty.map(_ == Drawer.Side.Start))
    classIf("jfx-drawer--end", $sideProperty.map(_ == Drawer.Side.End))
    
    style { 
      display = "flex"
      width = "100%"
      height = "100%"
      position = "relative"
    }

    Box.box("div") {
      addClass("jfx-drawer__panel-shell")
      style { 
        height = "100%"
        position = "relative"
        width_=(panelShellWidthProperty)
      }

      Box.box("div") {
        addClass("jfx-drawer__panel")
        style { 
          height = "100%"
          overflow = "hidden"
          width_=($drawerWidthProperty)
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

    Box.box("div") {
      addClass("jfx-drawer__scrim")
      onClick { _ =>
        if ($closeOnScrimClickProperty.get && $openProperty.get) {
          $openProperty.set(false)
        }
      }
    }

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

    onWindowKeyDown { event =>
      if (event.key == "Escape" && $openProperty.get) {
        $openProperty.set(false)
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
    DslRuntime.withComponentScope(d.$navigationHost) {
      init
    }
  }

  def drawerContent(init: => Unit)(using d: Drawer): Unit = {
    DslRuntime.withComponentScope(d.$contentHost) {
      init
    }
  }

  def open(using d: Drawer): Boolean = d.$openProperty.get
  def open_=(v: Boolean)(using d: Drawer): Unit = d.$openProperty.set(v)
  def openProperty(using d: Drawer): Property[Boolean] = d.$openProperty
  
  def side(using d: Drawer): Side = d.$sideProperty.get
  def side_=(s: Side)(using d: Drawer): Unit = d.$sideProperty.set(s)

  def drawerWidth(using d: Drawer): String = d.$drawerWidthProperty.get
  def drawerWidth_=(w: String)(using d: Drawer): Unit = d.$drawerWidthProperty.set(w)

  def toggle()(using d: Drawer): Unit = d.$openProperty.set(!d.$openProperty.get)
}
