package jfx.core.component

import jfx.core.render.{BrowserRenderBackend, RenderBackend}
import jfx.core.render.{Cursor, HostElement, HostNode}
import jfx.core.state.Disposable
import jfx.dsl.DslRuntime
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

class ClientSideComponentSpec extends AnyFlatSpec with Matchers {

  "ClientSideComponent" should "activate automatically for live browser builds" in {
    val probe = buildProbe()

    probe.fallbackComposed shouldBe true
    probe.clientMounted shouldBe true
  }

  it should "stay inactive while client activation is suspended" in {
    val probe =
      DslRuntime.withClientSideActivationSuspended {
        buildProbe()
      }

    probe.fallbackComposed shouldBe true
    probe.clientMounted shouldBe false

    ClientSideComponent.activateTree(probe)
    probe.clientMounted shouldBe true
  }

  private def buildProbe(): ProbeClientSideComponent = {
    val backend = BrowserRenderBackend

    RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(new FakeCursor()) {
        ProbeClientSideComponent()
      }
    }
  }
}

private final class FakeCursor extends Cursor {
  override def claimElement(tagName: String): HostElement =
    new FakeHostElement(tagName)

  override def claimText(initial: String): HostNode =
    new HostNode {
      override def renderHtml(indent: Int): String = initial
      override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])
    }

  override def subCursor(element: HostElement): Cursor =
    new FakeCursor()
}

private final class FakeHostElement(override val tagName: String) extends HostElement {
  private val attributes = mutable.Map.empty[String, String]
  private val styles = mutable.Map.empty[String, String]

  override def renderHtml(indent: Int): String = s"<$tagName></$tagName>"
  override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])

  override def setAttribute(name: String, value: String): Unit = attributes.update(name, value)
  override def attribute(name: String): Option[String] = attributes.get(name)
  override def setClassNames(classes: Seq[String]): Unit = setAttribute("class", classes.mkString(" "))
  override def getStyle(name: String): String = styles.getOrElse(name, "")
  override def setStyle(name: String, value: String): Unit = styles.update(name, value)
  override def clientHeight: Int = 0
  override def clientWidth: Int = 0
  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = () => ()
  override def clearChildren(): Unit = ()
  override def insertChild(index: Int, child: HostNode): Unit = ()
  override def removeChild(child: HostNode): Unit = ()
}

private final class ProbeClientSideComponent extends ClientSideComponent {
  var fallbackComposed = false
  var clientMounted = false

  override def tagName: String = "div"

  override protected def composeFallback(): Unit =
    fallbackComposed = true

  override protected def mountClient(): Unit =
    clientMounted = true
}

private object ProbeClientSideComponent {
  def apply(): ProbeClientSideComponent =
    DslRuntime.build(new ProbeClientSideComponent) {}
}
