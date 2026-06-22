package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.dsl.{ComponentContext, DslRuntime, StyleProxy}
import scala.scalajs.js
import scala.compiletime.uninitialized

class Drawer extends Box("div") {
  val $openProperty = Property(false)
  val $drawerWidthProperty = Property("280px")
  val $sideProperty = Property(Drawer.Side.Start)
  val $modeProperty = Property(Drawer.Mode.Push)
  val $closeOnScrimClickProperty = Property(true)

  private var _navigationHost: Box = uninitialized
  private var _contentHost: Box = uninitialized

  def $navigationHost: Box = _navigationHost
  def $contentHost: Box = _contentHost

  private val resolvedDrawerWidthProperty =
    $drawerWidthProperty.map { width =>
      s"min(92vw, $width)"
    }

  private val drawerFlexDirectionProperty =
    $sideProperty.map {
      case Drawer.Side.Start => "row"
      case Drawer.Side.End   => "row-reverse"
    }

  private val panelShellWidthProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $openProperty.flatMap { open =>
          resolvedDrawerWidthProperty.map { width =>
            if (open) width else "0px"
          }
        }

      case Drawer.Mode.Overlay =>
        resolvedDrawerWidthProperty
    }

  private val panelShellPositionProperty =
    $modeProperty.map {
      case Drawer.Mode.Push    => "relative"
      case Drawer.Mode.Overlay => "absolute"
    }

  private val panelShellLeftProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $sideProperty.map(_ => "auto")

      case Drawer.Mode.Overlay =>
        $sideProperty.map {
          case Drawer.Side.Start => "0"
          case Drawer.Side.End   => "auto"
        }
    }

  private val panelShellRightProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $sideProperty.map(_ => "auto")

      case Drawer.Mode.Overlay =>
        $sideProperty.map {
          case Drawer.Side.Start => "auto"
          case Drawer.Side.End   => "0"
        }
    }

  private val panelShellTransformProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $openProperty.map(_ => "translateX(0)")

      case Drawer.Mode.Overlay =>
        $openProperty.flatMap { open =>
          $sideProperty.map {
            case Drawer.Side.Start =>
              if (open) "translateX(0)" else "translateX(-100%)"

            case Drawer.Side.End =>
              if (open) "translateX(0)" else "translateX(100%)"
          }
        }
    }

  private val panelShellPointerEventsProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $openProperty.map(_ => "auto")

      case Drawer.Mode.Overlay =>
        $openProperty.map { open =>
          if (open) "auto" else "none"
        }
    }

  private val scrimOpacityProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $openProperty.map(_ => "0")

      case Drawer.Mode.Overlay =>
        $openProperty.map { open =>
          if (open) "1" else "0"
        }
    }

  private val scrimPointerEventsProperty =
    $modeProperty.flatMap {
      case Drawer.Mode.Push =>
        $openProperty.map(_ => "none")

      case Drawer.Mode.Overlay =>
        $openProperty.map { open =>
          if (open) "auto" else "none"
        }
    }

  override def compose(): Unit = {
    given Component = this

    addBaseClass("jfx-drawer")
    classIf("jfx-drawer--open", $openProperty)
    classIf("jfx-drawer--start", $sideProperty.map(_ == Drawer.Side.Start))
    classIf("jfx-drawer--end", $sideProperty.map(_ == Drawer.Side.End))
    classIf("jfx-drawer--push", $modeProperty.map(_ == Drawer.Mode.Push))
    classIf("jfx-drawer--overlay", $modeProperty.map(_ == Drawer.Mode.Overlay))

    style {
      display = "flex"
      flexDirection =(drawerFlexDirectionProperty)
      width = "100%"
      height = "100%"
      position = "relative"
      overflow = "hidden"
    }

    Box.box("div") {
      addClass("jfx-drawer__panel-shell")

      style {
        height = "100%"
        position =(panelShellPositionProperty)
        top = "0"
        bottom = "0"
        left =(panelShellLeftProperty)
        right =(panelShellRightProperty)
        width =(panelShellWidthProperty)
        overflow = "hidden"
        transform =(panelShellTransformProperty)
        pointerEvents =(panelShellPointerEventsProperty)
        transition = "width 180ms ease, transform 180ms ease"
        zIndex = "20"
      }

      Box.box("div") {
        addClass("jfx-drawer__panel")

        style {
          height = "100%"
          overflow = "hidden"
          width_=(resolvedDrawerWidthProperty)
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

      style {
        position = "absolute"
        top = "0"
        right = "0"
        bottom = "0"
        left = "0"
        opacity =  scrimOpacityProperty
        pointerEvents = scrimPointerEventsProperty
        backgroundColor = "rgba(0, 0, 0, 0.32)"
        transition = "opacity 180ms ease"
        zIndex = "10"
      }

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
      val key =
        Option(event.asInstanceOf[js.Dynamic].selectDynamic("key").asInstanceOf[js.Any])
          .filter(value => value != null && !js.isUndefined(value))
          .map(_.toString)
          .getOrElse("")

      if (key == "Escape" && $openProperty.get) {
        $openProperty.set(false)
      }
    }
  }
}

object Drawer {
  enum Side { case Start, End }

  enum Mode {
    case Push
    case Overlay
  }

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

  def mode(using d: Drawer): Mode = d.$modeProperty.get
  def mode_=(m: Mode)(using d: Drawer): Unit = d.$modeProperty.set(m)
  def modeProperty(using d: Drawer): Property[Mode] = d.$modeProperty

  def drawerWidth(using d: Drawer): String = d.$drawerWidthProperty.get
  def drawerWidth_=(w: String)(using d: Drawer): Unit = d.$drawerWidthProperty.set(w)

  def closeOnScrimClick(using d: Drawer): Boolean = d.$closeOnScrimClickProperty.get
  def closeOnScrimClick_=(v: Boolean)(using d: Drawer): Unit =
    d.$closeOnScrimClickProperty.set(v)

  def toggle()(using d: Drawer): Unit =
    d.$openProperty.set(!d.$openProperty.get)
}