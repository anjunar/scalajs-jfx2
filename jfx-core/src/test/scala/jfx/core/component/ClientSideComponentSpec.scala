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

  it should "pass SSR content to client activation" in {
    val probe = buildWithFakeCursor(new SsrAwareProbeClientSideComponent)

    probe.ssrContentPassed shouldBe true
  }

  it should "release fallback children without removing adopted DOM" in {
    val probe = buildWithFakeCursor(new AdoptingProbeClientSideComponent)

    probe.childrenBeforeRelease shouldBe 1
    probe.childrenAfterRelease shouldBe 0
    probe.domChildrenBeforeRelease shouldBe 1
    probe.domChildrenAfterRelease shouldBe 1
  }

  private def buildProbe(): ProbeClientSideComponent = {
    val backend = BrowserRenderBackend

    RenderBackend.withBackend(backend) {
      DslRuntime.withCursor(new FakeCursor()) {
        ProbeClientSideComponent()
      }
    }
  }

  private def buildWithFakeCursor[A <: ClientSideComponent](component: => A): A =
    RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(new FakeCursor()) {
        DslRuntime.build(component) {}
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
  private var childCountValue = 0

  def childCount: Int = childCountValue

  override def renderHtml(indent: Int): String = s"<$tagName></$tagName>"
  override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])

  override def setAttribute(name: String, value: String): Unit = attributes.update(name, value)
  override def attribute(name: String): Option[String] = attributes.get(name)
  override def setProperty(name: String, value: Any): Unit = ()
  override def property[T](name: String): Option[T] = None
  override def setClassNames(classes: Seq[String]): Unit = setAttribute("class", classes.mkString(" "))
  override def getStyle(name: String): String = styles.getOrElse(name, "")
  override def setStyle(name: String, value: String): Unit = styles.update(name, value)
  override def clientHeight: Int = 0
  override def clientWidth: Int = 0
  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = () => ()
  override def clearChildren(): Unit = childCountValue = 0
  override def insertChild(index: Int, child: HostNode): Unit = childCountValue += 1
  override def removeChild(child: HostNode): Unit = childCountValue -= 1
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

private final class SsrAwareProbeClientSideComponent extends ClientSideComponent {
  var ssrContentPassed = false

  override def tagName: String = "div"

  override protected def composeFallback(): Unit = ()

  override protected def mountClient(): Unit = ()

  override protected def activateClientSideContent(ssrContent: ClientSideSsrContent): Unit =
    ssrContentPassed = true
}

private final class AdoptingProbeClientSideComponent extends ClientSideComponent {
  var childrenBeforeRelease = -1
  var childrenAfterRelease = -1
  var domChildrenBeforeRelease = -1
  var domChildrenAfterRelease = -1

  override def tagName: String = "div"

  override protected def composeFallback(): Unit =
    DslRuntime.build(new ProbeFallbackChild) {}

  override protected def mountClient(): Unit = ()

  override protected def activateClientSideContent(ssrContent: ClientSideSsrContent): Unit = {
    val fakeHost = hostNode.asInstanceOf[FakeHostElement]
    childrenBeforeRelease = children.size
    domChildrenBeforeRelease = fakeHost.childCount
    releaseFallbackChildren()
    childrenAfterRelease = children.size
    domChildrenAfterRelease = fakeHost.childCount
  }
}

private final class ProbeFallbackChild extends Component {
  override def tagName: String = "span"
}
