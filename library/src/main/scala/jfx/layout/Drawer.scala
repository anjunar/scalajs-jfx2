package jfx.layout

import jfx.core.component.{Box, Component}
import jfx.core.state.Property
import jfx.dsl.Dsl.*
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom.{Event, KeyboardEvent, window}
import scala.compiletime.uninitialized

class Drawer extends Box("div") {
  val openProperty = Property(false)
  val widthProperty = Property("280px")
  val sideProperty = Property(Drawer.Side.Start)
  val closeOnScrimClickProperty = Property(true)

  private var _navigationHost: Box = uninitialized
  private var _contentHost: Box = uninitialized

  def navigationHost: Box = _navigationHost
  def contentHost: Box = _contentHost

  override def compose(): Unit = {
    classes = Seq("jfx-drawer")

    addDisposable(openProperty.observe(syncOpenState))
    addDisposable(widthProperty.observe(_ => syncPanelWidth()))
    addDisposable(sideProperty.observe(syncSideState))

    Box.box("div") {
      classes = Seq("jfx-drawer__scrim")
      addDisposable(host.addEventListener("click", _ => {
        if (closeOnScrimClickProperty.get && openProperty.get) openProperty.set(false)
      }))
    }

    Box.box("div") {
      classes = Seq("jfx-drawer__panel-shell")
      style { height = "100%" }
      
      addDisposable(widthProperty.observe(_ => syncPanelWidth()))
      addDisposable(openProperty.observe(_ => syncPanelWidth()))

      Box.box("div") {
        classes = Seq("jfx-drawer__panel")
        style { height = "100%" }
        addDisposable(widthProperty.observe(w => host.setStyle("width", w)))
        style { width = widthProperty.get }

        _navigationHost = Box.box("div") {
          classes = Seq("jfx-drawer__navigation")
          style { height = "100%" }
        }
      }
    }

    _contentHost = Box.box("div") {
      classes = Seq("jfx-drawer__content")
      style { height = "100%" }
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
    val current = classes.filterNot(_ == "jfx-drawer--open")
    classes = if (open) current :+ "jfx-drawer--open" else current
  }

  private def syncSideState(side: Drawer.Side): Unit = {
    val current = classes.filterNot(c => c == "jfx-drawer--start" || c == "jfx-drawer--end")
    classes = side match {
      case Drawer.Side.Start => current :+ "jfx-drawer--start"
      case Drawer.Side.End => current :+ "jfx-drawer--end"
    }
  }

  private def syncPanelWidth(): Unit = {
    children.collectFirst { case b: Box if b.host.attribute("class").exists(_.contains("jfx-drawer__panel-shell")) => b }.foreach { shell =>
      val widthValue = widthProperty.get
      val responsiveWidth = s"min(92vw, $widthValue)"
      shell.host.setStyle("width", if (openProperty.get) responsiveWidth else "0px")
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
  
  def side(using d: Drawer): Side = d.sideProperty.get
  def side_=(s: Side)(using d: Drawer): Unit = d.sideProperty.set(s)

  def width(using d: Drawer): String = d.widthProperty.get
  def width_=(w: String)(using d: Drawer): Unit = d.widthProperty.set(w)

  def toggle()(using d: Drawer): Unit = d.openProperty.set(!d.openProperty.get)
}
