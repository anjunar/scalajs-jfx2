package jfx.statement

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.core.state.ListProperty
import jfx.ssr.Ssr
import jfx.statement.ForEach.forEach
import scala.scalajs.js

class ForEachSpec extends AnyFlatSpec with Matchers {

  "ForEach" should "render initial items" in {
    val items = new ListProperty[String](js.Array("A", "B"))
    val html = Ssr.renderToString {
      div {
        forEach(items) { (item: String) =>
          div { text = item }
        }
      }
    }
    html shouldBe "<div><div>A</div><div>B</div></div>"
  }

  it should "update DOM on addition" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}
    val items = new ListProperty[String](js.Array("A"))
    var root: jfx.core.component.Box = null
    val backend = new SsrRenderBackend()
    
    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        root = div {
          forEach(items) { (item: String) =>
            div { 
              classes = Seq(s"item-$item")
              text = item 
            }
          }
        }
        root
      }

      // Act: Add item inside backend context
      items += "B"
    }

    // Verify Tree
    root.children.head.children should have size 2
    root.children.head.children(1).asInstanceOf[jfx.core.component.Box].classes should contain ("item-B")
  }

  it should "maintain correct DOM offsets after middle insertion" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}
    val items = new ListProperty[String](js.Array("A", "C"))
    var fe: ForEach[String] = null
    val backend = new SsrRenderBackend()
    
    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        div {
          div { text = "Before" }
          fe = forEach(items) { (item: String) =>
            div { text = item }
          }
          div { text = "After" }
        }
      }

      // Initial offsets: Before=0, A=1, C=2, After=3
      fe.children(1).calculateDomOffset shouldBe 2
      
      // Act: Insert "B" at index 1
      items.insert(1, "B")
    }

    // New offsets: Before=0, A=1, B=2, C=3, After=4
    fe.children should have size 3
    fe.children(0).calculateDomOffset shouldBe 1 // A
    fe.children(1).calculateDomOffset shouldBe 2 // B
    fe.children(2).calculateDomOffset shouldBe 3 // C
    
    // Verify sibling after ForEach
    fe.parent.get.children(2).calculateDomOffset shouldBe 4 // After
  }

  it should "handle removals correctly" in {
    val items = new ListProperty[String](js.Array("A", "B", "C"))
    var fe: ForEach[String] = null
    
    Ssr.renderToString {
      fe = forEach(items) { (item: String) => div { text = item } }
      fe
    }

    items.remove(1) // Remove "B"
    fe.children should have size 2
    fe.children(0).calculateDomOffset shouldBe 0 // A
    fe.children(1).calculateDomOffset shouldBe 1 // C
  }
}
