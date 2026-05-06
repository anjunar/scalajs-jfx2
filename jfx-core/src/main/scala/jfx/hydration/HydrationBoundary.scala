package jfx.hydration

import jfx.core.component.{ClientSideComponent, Component}
import jfx.core.render.{AsyncRenderPending, BrowserRenderBackend, HydrationCursor, HydrationRenderBackend, RenderBackend}
import jfx.dsl.{ComponentContext, DslRuntime}
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

sealed trait HydrationStrategy

object HydrationStrategy {
  case object Immediate extends HydrationStrategy
  case object Idle extends HydrationStrategy
  case object Manual extends HydrationStrategy
  final case class Visible(rootMargin: String = "200px") extends HydrationStrategy
  final case class Interaction(events: Seq[String] = Seq("pointerdown", "focusin", "keydown")) extends HydrationStrategy
}

private[jfx] trait DeferredHydrationBoundary {
  def shouldDeferHydrationChildren: Boolean
  def markDeferredHydrationClaimed(): Unit
}

final class HydrationBoundary private[hydration] (
  val strategy: HydrationStrategy,
  renderContent: () => Unit
) extends Component with DeferredHydrationBoundary {

  private var claimed = false
  private var scheduled = false
  private var hydrated = false
  private var running: js.Promise[Unit] | Null = null

  override def tagName: String = "div"

  override def initialize(): Unit = {
    host.setAttribute("data-jfx-hydration-boundary", "true")
    host.setAttribute("data-jfx-hydration-state", if (RenderBackend.current.isServer) "server" else "pending")
  }

  override def compose(): Unit = {
    if (RenderBackend.current.isServer || !DslRuntime.isDeferredHydrationSuspended) {
      renderContent()
    }
  }

  override def shouldDeferHydrationChildren: Boolean =
    !RenderBackend.current.isServer && !hydrated

  override def markDeferredHydrationClaimed(): Unit = {
    claimed = true
    host.setAttribute("data-jfx-hydration-state", "pending")
  }

  def hydrateBoundary(): js.Promise[Unit] = {
    if (hydrated) {
      js.Promise.resolve(())
    } else if (running != null) {
      running
    } else {
      running = runBoundaryHydration()
      running
    }
  }

  private[jfx] def scheduleHydration(): Unit = {
    if (!claimed || scheduled || hydrated || RenderBackend.current.isServer) {
      return
    }

    scheduled = true
    strategy match {
      case HydrationStrategy.Manual =>
        ()
      case HydrationStrategy.Immediate =>
        scheduleTimeout(0)(hydrateBoundary())
      case HydrationStrategy.Idle =>
        scheduleIdle(hydrateBoundary())
      case visible: HydrationStrategy.Visible =>
        scheduleVisible(visible)(hydrateBoundary())
      case interaction: HydrationStrategy.Interaction =>
        scheduleInteraction(interaction)(hydrateBoundary())
    }
  }

  private def runBoundaryHydration(): js.Promise[Unit] = {
    val element = host.domNode.collect { case value: dom.Element => value }.orNull
    if (element == null) {
      return js.Promise.resolve(())
    }

    host.setAttribute("data-jfx-hydration-state", "hydrating")

    val contentRoot =
      DslRuntime.withClientSideActivationSuspended {
        DslRuntime.withDeferredHydrationSuspended {
          RenderBackend.withBackend(BrowserRenderBackend) {
            DslRuntime.withCursor(BrowserRenderBackend.nextCursor(None)) {
              DslRuntime.withContext(ComponentContext(None, registry = registry)) {
                DslRuntime.build(new HydrationBoundaryContentRoot(renderContent)) {}
              }
            }
          }
        }
      }

    AsyncRenderPending.awaitPending(contentRoot).map { _ =>
      val cursor = new HydrationCursor(element)
      val boundaryChildren = contentRoot.children
      contentRoot._children.clear()

      _children.clear()
      boundaryChildren.foreach { child =>
        child.setParent(Some(this))
        addChild(child)
      }

      RenderBackend.withBackend(HydrationRenderBackend.root(element)) {
        boundaryChildren.foreach(child => DslRuntime.rehydrate(child, cursor))
      }

      cursor.pruneRemaining()
      boundaryChildren.foreach(ClientSideComponent.activateTree)
      boundaryChildren.foreach(HydrationBoundary.scheduleTree)

      hydrated = true
      host.setAttribute("data-jfx-hydration-state", "hydrated")
    }.recover { case error =>
      running = null
      host.setAttribute("data-jfx-hydration-state", "failed")
      throw error
    }.toJSPromise
  }

  private def scheduleTimeout(delay: Int)(action: => Unit): Unit =
    dom.window.setTimeout(() => action, delay)

  private def scheduleIdle(action: => Unit): Unit = {
    val idleCallback = js.Dynamic.global.selectDynamic("requestIdleCallback")
    if (js.typeOf(idleCallback) == "function") {
      idleCallback((_: js.Any) => action)
    } else {
      scheduleTimeout(1)(action)
    }
  }

  private def scheduleVisible(strategy: HydrationStrategy.Visible)(action: => Unit): Unit = {
    val element = host.domNode.collect { case value: dom.Element => value }.orNull
    val observerCtor = js.Dynamic.global.selectDynamic("IntersectionObserver")

    if (element == null) {
      ()
    } else if (js.typeOf(observerCtor) == "function") {
      val observer = js.Dynamic.newInstance(observerCtor)(
        (entries: js.Array[js.Dynamic], observer: js.Dynamic) => {
          if (entries.exists(entry => entry.selectDynamic("isIntersecting").asInstanceOf[Boolean])) {
            observer.disconnect()
            action
          }
        },
        js.Dynamic.literal(rootMargin = strategy.rootMargin)
      )

      observer.observe(element)
      addDisposable(() => observer.disconnect())
    } else {
      scheduleIdle(action)
    }
  }

  private def scheduleInteraction(strategy: HydrationStrategy.Interaction)(action: => Unit): Unit = {
    val element = host.domNode.collect { case value: dom.Element => value }.orNull
    if (element == null) {
      return
    }

    var disposables = Seq.empty[() => Unit]
    lazy val activate: dom.Event => Unit = _ => {
      disposables.foreach(_.apply())
      disposables = Seq.empty
      action
    }

    disposables = strategy.events.map { eventName =>
      element.addEventListener(eventName, activate)
      () => element.removeEventListener(eventName, activate)
    }

    disposables.foreach(dispose => addDisposable(() => dispose()))
  }
}

object HydrationBoundary {
  def hydrationBoundary(strategy: HydrationStrategy = HydrationStrategy.Visible())(content: => Unit): HydrationBoundary =
    DslRuntime.build(new HydrationBoundary(strategy, () => content)) {}

  def hydrateNow(boundary: HydrationBoundary): js.Promise[Unit] =
    boundary.hydrateBoundary()

  private[jfx] def scheduleTree(root: Component): Unit = {
    root match {
      case boundary: HydrationBoundary =>
        boundary.scheduleHydration()
      case _ =>
        root.children.foreach(scheduleTree)
    }
  }
}

private final class HydrationBoundaryContentRoot(renderContent: () => Unit) extends Component {
  override def tagName: String = "div"

  override def compose(): Unit =
    renderContent()
}
