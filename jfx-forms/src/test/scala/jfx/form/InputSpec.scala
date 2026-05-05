package jfx.form

import jfx.core.render.{BrowserRenderBackend, Cursor, HostElement, HostNode, RenderBackend}
import jfx.core.state.Disposable
import jfx.dsl.DslRuntime
import jfx.form.Input.*
import jfx.form.SubForm.{editable as subFormEditable, subForm}
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

class InputSpec extends AnyFlatSpec with Matchers {

  "Input" should "treat an undefined native value as empty during input events" in {
    var control: Input = null
    var host: FakeHostElement = null

    RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(new FakeCursor(created => host = created)) {
        control = input("email", standalone = true) {}
      }
    }

    host.setDynamicProperty("value", js.undefined)

    noException should be thrownBy {
      host.fire("input")
    }

    control.$valueProperty.get shouldBe ""
  }

  it should "disable the native fieldset when a subform becomes non-editable" in {
    val createdHosts = mutable.ArrayBuffer.empty[FakeHostElement]

    RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(new FakeCursor(created => createdHosts += created)) {
        subForm[String]("info") {
          input("firstName") {}
          subFormEditable = false
        }
      }
    }

    val fieldsetHost = createdHosts.find(_.tagName == "fieldset").orNull
    fieldsetHost should not be null
    fieldsetHost.property[Boolean]("disabled") shouldBe Some(true)
  }
}

private final class FakeCursor(onCreate: FakeHostElement => Unit) extends Cursor {
  override def claimElement(tagName: String): HostElement = {
    val host = new FakeHostElement(tagName)
    onCreate(host)
    host
  }

  override def claimText(initial: String): HostNode =
    new HostNode {
      override def renderHtml(indent: Int): String = initial
      override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])
    }

  override def subCursor(element: HostElement): Cursor =
    this
}

private final class FakeHostElement(override val tagName: String) extends HostElement {
  private val attributes = mutable.Map.empty[String, String]
  private val styles = mutable.Map.empty[String, String]
  private val dynamicProperties = mutable.Map.empty[String, js.Any]
  private val listeners = mutable.Map.empty[String, dom.Event => Unit]

  override def renderHtml(indent: Int): String = s"<$tagName></$tagName>"
  override def domNode: Option[dom.Node] = Some(js.Dynamic.literal().asInstanceOf[dom.Node])

  override def setAttribute(name: String, value: String): Unit = attributes.update(name, value)
  override def attribute(name: String): Option[String] = attributes.get(name)
  override def setProperty(name: String, value: Any): Unit = dynamicProperties.update(name, value.asInstanceOf[js.Any])
  override def property[T](name: String): Option[T] = dynamicProperties.get(name).map(_.asInstanceOf[T])
  override def setClassNames(classes: Seq[String]): Unit = setAttribute("class", classes.mkString(" "))
  override def getStyle(name: String): String = styles.getOrElse(name, "")
  override def setStyle(name: String, value: String): Unit = styles.update(name, value)
  override def clientHeight: Int = 0
  override def clientWidth: Int = 0

  override def addEventListener(name: String, listener: dom.Event => Unit): Disposable = {
    listeners.update(name, listener)
    () => listeners.remove(name)
  }

  override def clearChildren(): Unit = ()
  override def insertChild(index: Int, child: HostNode): Unit = ()
  override def removeChild(child: HostNode): Unit = ()

  def setDynamicProperty(name: String, value: js.Any): Unit =
    dynamicProperties.update(name, value)

  def fire(name: String): Unit =
    listeners(name)(js.Dynamic.literal(`type` = name).asInstanceOf[dom.Event])
}
