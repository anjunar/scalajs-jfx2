package jfx.ssr

import jfx.action.Button.*
import jfx.core.component.ClientOnly.*
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.form.Input.*
import jfx.layout.Div.div
import jfx.statement.Conditional.*
import jfx.statement.ForEach.*
import jfx.statement.ObserveRender.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class SsrSpec extends AnyFlatSpec with Matchers {

  "Ssr" should "render the minimal core components without a browser document" in {
    val html =
      Ssr.renderToString {
        div {
          classes = "panel featured"
          text = "Hello <World>"

          button("Save") {
            buttonType = "submit"
          }

          input("email") {
            placeholder = "Email"
            stringValueProperty.set("ada@example.test")
          }
        }
      }

    html.shouldBe(
      """<div class="panel featured">Hello &lt;World&gt;<button type="submit">Save</button><input name="email" placeholder="Email" value="ada@example.test"></div>"""
    )
  }

  it should "render client-only fallbacks without evaluating the client component" in {
    var clientEvaluated = false

    val html =
      Ssr.renderToString {
        div {
          clientOnly("LexicalEditor")(
            div {
              classes = "editor-fallback"
              text = "Editor fallback"
            }
          ) {
            clientEvaluated = true

            div {
              classes = "editor-client"
              text = "Client editor"
            }
          }
        }
      }

    clientEvaluated.shouldBe(false)
    html.shouldBe(
      """<div><div data-jfx-client-only="LexicalEditor" data-jfx-client-only-state="fallback"><div class="editor-fallback">Editor fallback</div></div></div>"""
    )
  }

  it should "render conditional slots without statement anchors" in {
    val showDetails = Property(true)

    val html =
      Ssr.renderToString {
        div {
          div {
            text = "A"
          }

          conditional(showDetails) {
            thenDo {
              div {
                text = "B"
              }
            }

            elseDo {
              div {
                text = "Else"
              }
            }
          }

          div {
            text = "C"
          }
        }
      }

    html.shouldBe("<div><div>A</div><div>B</div><div>C</div></div>")
  }

  it should "render forEach slots as native children" in {
    val items = ListProperty[String]()
    items += "Ada"
    items += "Grace"

    val html =
      Ssr.renderToString {
        div {
          div {
            text = "People"
          }

          forEach(items) { (item, index) =>
            div {
              text = s"$index:$item"
            }
          }
        }
      }

    html.shouldBe("<div><div>People</div><div>0:Ada</div><div>1:Grace</div></div>")
  }

  it should "render observeRender slots from the current property value" in {
    val name = Property("Ada")

    val html =
      Ssr.renderToString {
        div {
          observeRender(name) { value =>
            div {
              text = s"Hello $value"
            }
          }
        }
      }

    html.shouldBe("<div><div>Hello Ada</div></div>")
  }

}
