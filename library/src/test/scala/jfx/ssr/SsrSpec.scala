package jfx.ssr

import jfx.action.Button.*
import jfx.core.component.ClientOnly.*
import jfx.core.component.ElementComponent.*
import jfx.form.Input.*
import jfx.layout.Div.div
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

}
