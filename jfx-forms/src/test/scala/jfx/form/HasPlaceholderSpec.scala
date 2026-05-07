package jfx.form

import jfx.core.render.{BrowserRenderBackend, Cursor, HostElement, HostNode, RenderBackend}
import jfx.core.state.Disposable
import jfx.dsl.DslRuntime
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

class HasPlaceholderSpec extends AnyFlatSpec with Matchers {

  "HasPlaceholder" should "provide a shared placeholder DSL for inputs" in {
    var control: Input = null

    RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(new PlaceholderCursor(_ => ())) {
        import HasPlaceholder.{placeholder, placeholder_=}

        control = Input.input("title", standalone = true) {
          placeholder = "Title"
        }
      }
    }

    control.$placeholder shouldBe "Title"
  }

  it should "provide a shared placeholder DSL for combo boxes" in {
    var control: ComboBox[String] = null

    RenderBackend.withBackend(BrowserRenderBackend) {
      DslRuntime.withCursor(new PlaceholderCursor(_ => ())) {
        import HasPlaceholder.{placeholder, placeholder_=}

        control = ComboBox.comboBox[String]("visibility", standalone = true) {
          placeholder = "Only you"
        }
      }
    }

    control.$placeholder shouldBe "Only you"
  }
}

private final class PlaceholderCursor(onCreate: PlaceholderHostElement => Unit) extends Cursor {
  override def claimElement(tagName: String): HostElement = {
    val host = new PlaceholderHostElement(tagName)
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

private final class PlaceholderHostElement(override val tagName: String) extends HostElement {
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
}
