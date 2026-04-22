package jfx.core.component

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.layout.Div.div
import jfx.core.component.Component.*
import jfx.ssr.Ssr

class ComponentTreeSpec extends AnyFlatSpec with Matchers {

  "Component Tree" should "establish correct parent-child relationships" in {
    // We use SSR context to build the tree without a real DOM
    Ssr.renderToString {
      val root = div {
        div {
          classes = Seq("child")
          text = "I am a child"
        }
      }

      root.children should have size 1
      val child = root.children.head
      child.parent shouldBe Some(root)
      child.children should have size 1 // The TextComponent
      child.children.head.parent shouldBe Some(child)
      
      root
    }
  }

  it should "calculate DOM offsets correctly for mixed structures" in {
     Ssr.renderToString {
       val root = div {
         div { text = "1" }
         div { text = "2" }
         div { text = "3" }
       }
       
       root.children(0).calculateDomOffset shouldBe 0
       root.children(1).calculateDomOffset shouldBe 1
       root.children(2).calculateDomOffset shouldBe 2
       
       root
     }
  }

  it should "calculate offsets correctly for virtual fragments" in {
    import jfx.statement.Condition.condition
    import jfx.core.state.Property

    Ssr.renderToString {
      val root = div {
        div { text = "Before" }
        condition(Property(true)) {
          jfx.statement.Condition.thenDo {
            div { text = "Inside 1" }
            div { text = "Inside 2" }
          }
        }
        div { text = "After" }
      }

      // root children: [Div(Before), Condition, Div(After)]
      root.children should have size 3
      
      val before = root.children(0)
      val cond = root.children(1)
      val after = root.children(2)
      
      before.calculateDomOffset shouldBe 0
      cond.calculateDomOffset shouldBe 1
      
      // Inside Condition (virtual):
      cond.children should have size 2
      cond.children(0).calculateDomOffset shouldBe 1 // Starts at Condition's offset
      cond.children(1).calculateDomOffset shouldBe 2 // Next node in physical DOM
      
      // After Condition:
      // "Before" (1 node) + "Inside 1" (1 node) + "Inside 2" (1 node) = 3 nodes before "After"
      after.calculateDomOffset shouldBe 3
      
      root
    }
  }
}
