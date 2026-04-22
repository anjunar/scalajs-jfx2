package jfx.statement

import jfx.core.component.Component.*
import jfx.core.state.Property
import jfx.layout.Div.div
import jfx.ssr.Ssr
import jfx.statement.Conditional.{conditional, elseDo, thenDo}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConditionalSpec extends AnyFlatSpec with Matchers {

  "Conditional" should "provide the JFX1 conditional import surface" in {
    val show = Property(false)

    val html = Ssr.renderToString {
      conditional(show) {
        thenDo {
          div { text = "Then" }
        }
        elseDo {
          div { text = "Else" }
        }
      }
    }

    html shouldBe "<div>Else</div>"
  }
}
