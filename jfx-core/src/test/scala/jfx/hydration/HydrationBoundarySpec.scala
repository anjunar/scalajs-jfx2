package jfx.hydration

import jfx.core.component.Component.*
import jfx.core.component.Component
import jfx.core.render.{BrowserRenderBackend, Cursor, HostElement, HostNode, RenderBackend}
import jfx.core.state.Disposable
import jfx.dsl.DslRuntime
import jfx.hydration.HydrationBoundary.hydrationBoundary
import jfx.hydration.HydrationStrategy.Manual
import jfx.layout.Div.div
import jfx.ssr.Ssr
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

class HydrationBoundarySpec extends AnyFlatSpec with Matchers {

  "HydrationBoundary" should "render its content during SSR" in {
    val html = Ssr.renderToString {
      hydrationBoundary(Manual) {
        div {
          text = "server content"
        }
      }
    }

    html should include("""data-jfx-hydration-boundary="true"""")
    html should include("""data-jfx-hydration-state="server"""")
    html should include("server content")
  }

  it should "defer child composition during the hydration dry run" in {
    val boundary =
      DslRuntime.withClientSideActivationSuspended {
        DslRuntime.withDeferredHydrationSuspended {
          RenderBackend.withBackend(BrowserRenderBackend) {
            DslRuntime.withCursor(new BoundarySpecCursor) {
              hydrationBoundary(Manual) {
                div {
                  text = "client content"
                }
              }
            }
          }
        }
      }

    boundary.children shouldBe empty
    boundary.host.attribute("data-jfx-hydration-boundary") shouldBe Some("true")
    boundary.host.attribute("data-jfx-hydration-state") shouldBe Some("pending")
  }

  it should "keep boundary composition separate from client-side activation suspension" in {
    val boundary =
      DslRuntime.withClientSideActivationSuspended {
        RenderBackend.withBackend(BrowserRenderBackend) {
          DslRuntime.withCursor(new BoundarySpecCursor) {
            hydrationBoundary(Manual) {
              div {
                text = "boundary content"
              }
            }
          }
        }
      }

    boundary.children should have size 1
  }

  it should "skip child claiming during the global hydration pass" in {
    val boundary =
      RenderBackend.withBackend(BrowserRenderBackend) {
        DslRuntime.withCursor(new BoundarySpecCursor) {
          hydrationBoundary(Manual) {
            div {
              text = "still server owned"
            }
          }
        }
      }

    boundary.children should have size 1

    val cursor = new BoundarySpecCursor
    DslRuntime.withClientSideActivationSuspended {
      RenderBackend.withBackend(BrowserRenderBackend) {
        DslRuntime.rehydrate(boundary, cursor)
      }
    }

    cursor.subCursorCalls shouldBe 0
    boundary.host.attribute("data-jfx-hydration-state") shouldBe Some("pending")
  }
}

private final class BoundarySpecCursor extends Cursor {
  var subCursorCalls = 0

  override def claimElement(tagName: String): HostElement =
    new BoundarySpecHostElement(tagName)

  override def claimText(initial: String): HostNode =
    new HostNode {
      override def renderHtml(indent: Int): String = initial
      override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])
    }

  override def subCursor(element: HostElement): Cursor = {
    subCursorCalls += 1
    new BoundarySpecCursor()
  }
}

private final class BoundarySpecHostElement(override val tagName: String) extends HostElement {
  private val attributes = mutable.Map.empty[String, String]

  override def renderHtml(indent: Int): String = s"<$tagName></$tagName>"
  override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])

  override def setAttribute(name: String, value: String): Unit =
    attributes.update(name, value)

  override def attribute(name: String): Option[String] =
    attributes.get(name)

  override def setProperty(name: String, value: Any): Unit = ()
  override def property[T](name: String): Option[T] = None
  override def setClassNames(classes: Seq[String]): Unit = ()
  override def getStyle(name: String): String = ""
  override def setStyle(name: String, value: String): Unit = ()
  override def clientHeight: Int = 0
  override def clientWidth: Int = 0
  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = () => ()
  override def clearChildren(): Unit = ()
  override def insertChild(index: Int, child: HostNode): Unit = ()
  override def removeChild(child: HostNode): Unit = ()
}
