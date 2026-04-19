package jfx.integration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.core.component.Component.*
import jfx.core.state.{ListProperty, Property}
import jfx.ssr.Ssr
import jfx.statement.Condition.*
import jfx.statement.ForEach.forEach
import scala.scalajs.js

class ComplexTreeSpec extends AnyFlatSpec with Matchers {

  "A complex JFX2 tree" should "render correctly in SSR" in {
    val showExtra = Property(true)
    val items = new ListProperty[String](js.Array("A", "B"))

    val html = Ssr.renderToString {
      vbox {
        classes = "root-vbox"
        div { classes = "header"; text = "Title" }
        
        condition(showExtra) {
          thenDo {
            div { classes = "extra-1"; text = "Extra 1" }
            div { classes = "extra-2"; text = "Extra 2" }
          }
        }

        vbox {
          classes = "list-container"
          forEach(items) { item =>
            div { classes = s"item-$item"; text = item }
          }
        }

        div { classes = "footer"; text = "End" }
      }
    }

    // Expected Structure:
    // <div class="vbox root-vbox">
    //   <div class="header">Title</div>
    //   <div class="extra-1">Extra 1</div>
    //   <div class="extra-2">Extra 2</div>
    //   <div class="vbox list-container">
    //     <div class="item-A">A</div>
    //     <div class="item-B">B</div>
    //   </div>
    //   <div class="footer">End</div>
    // </div>

    html should include ("root-vbox")
    html should include ("header")
    html should include ("extra-1")
    html should include ("extra-2")
    html should include ("list-container")
    html should include ("item-A")
    html should include ("item-B")
    html should include ("footer")
  }

  it should "maintain stability during deep reactive updates" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}
    val showSection = Property(false)
    val listItems = new ListProperty[Int](js.Array(1))
    var root: jfx.core.component.Box = null
    val backend = new SsrRenderBackend()

    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        root = div {
          div { classes = "pre"; text = "Pre" }
          
          condition(showSection) {
            thenDo {
              vbox {
                classes = "inner-section"
                forEach(listItems) { i =>
                  div { classes = s"i-$i"; text = i.toString }
                }
              }
            }
          }

          div { classes = "post"; text = "Post" }
        }
        root
      }

      val pre = root.children(0)
      val cond = root.children(1)
      val post = root.children(2)

      // 1. Initial State: [Pre, Cond(empty), Post]
      pre.calculateDomOffset shouldBe 0
      cond.domNodeCount shouldBe 0
      post.calculateDomOffset shouldBe 1

      // 2. Act: Show Section
      showSection.set(true)
      // Tree: [Pre, Cond([VBox(inner-section)]), Post]
      // VBox: [ForEach([Div(i-1)])]
      cond.domNodeCount shouldBe 1 // The VBox
      post.calculateDomOffset shouldBe 2 // Pre(1) + VBox(1)

      // 3. Act: Add to inner list
      listItems += 2
      // Tree remains same, but VBox content changes? 
      // No, VBox is physical, so its domNodeCount is always 1.
      cond.domNodeCount shouldBe 1
      post.calculateDomOffset shouldBe 2

      // 4. Act: Remove outer condition
      showSection.set(false)
      cond.domNodeCount shouldBe 0
      post.calculateDomOffset shouldBe 1
    }
  }

  it should "handle deeply nested virtual fragments" in {
    val p1 = Property(true)
    val p2 = Property(true)
    val items = new ListProperty[String](js.Array("X"))

    val html = Ssr.renderToString {
      div {
        condition(p1) {
          thenDo {
            condition(p2) {
              thenDo {
                forEach(items) { item =>
                  div { text = item }
                }
              }
            }
          }
        }
      }
    }

    html shouldBe "<div><div>X</div></div>"
  }
}
