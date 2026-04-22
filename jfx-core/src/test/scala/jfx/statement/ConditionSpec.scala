package jfx.statement

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.ssr.Ssr
import jfx.statement.Condition.*

class ConditionSpec extends AnyFlatSpec with Matchers {

  "Condition" should "render the correct initial branch" in {
    val prop = Property(true)
    val html = Ssr.renderToString {
      condition(prop) {
        thenDo { div { text = "TRUE" } }
        elseDo { div { text = "FALSE" } }
      }
    }
    html shouldBe "<div>TRUE</div>"
  }

  it should "switch branches when property changes" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}
    val prop = Property(true)
    var cond: Condition = null
    val backend = new SsrRenderBackend()

    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        cond = condition(prop) {
          thenDo { div { classes = Seq("t"); text = "T" } }
          elseDo { div { classes = Seq("f"); text = "F" } }
        }
        cond
      }

      cond.children should have size 1
      cond.children.head.asInstanceOf[jfx.core.component.Box].classes should contain ("t")

      // Act: Switch
      prop.set(false)
    }

    cond.children should have size 1
    cond.children.head.asInstanceOf[jfx.core.component.Box].classes should contain ("f")
  }

  it should "handle nested conditions and maintain offsets" in {
    import jfx.core.render.{RenderBackend, SsrRenderBackend}
    val outer = Property(true)
    val inner = Property(false)
    var root: jfx.core.component.Box = null
    val backend = new SsrRenderBackend()
    
    RenderBackend.withBackend(backend) {
      Ssr.renderToString {
        root = div {
          div { text = "1" }
          condition(outer) {
            thenDo {
              condition(inner) {
                thenDo { div { text = "Inner-T" } }
                elseDo { div { text = "Inner-F" } }
              }
            }
          }
          div { text = "3" }
        }
        root
      }

      root.children(2).calculateDomOffset shouldBe 2 // [0]=Div(1), [1]=Div(Inner-F), [2]=Div(3)
      
      // Act: Switch inner
      inner.set(true)
      root.children(2).calculateDomOffset shouldBe 2
      
      // Act: Switch outer to empty (no elseDo)
      outer.set(false)
    }
    
    root.children(2).calculateDomOffset shouldBe 1 // [0]=Div(1), [1]=Div(3)
  }
}
