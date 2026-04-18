package jfx.ssr

import jfx.action.Button.*
import jfx.core.component.ClientOnly.*
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.control.Link.*
import jfx.form.Editor.editor
import jfx.form.editor.plugins.*
import jfx.form.Input.*
import jfx.layout.Div.div
import jfx.router.Route
import jfx.router.RouteContext.routeContext
import jfx.router.Router.router
import jfx.statement.Conditional.*
import jfx.statement.ForEach.*
import jfx.statement.ObserveRender.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

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

  it should "render router content through a native child slot" in {
    val routes = js.Array(
      Route.scoped("/") {
        div {
          text = "Home"
        }
      },
      Route.scoped("/people/:name") {
        val context = routeContext

        div {
          text = s"${context.pathParams("name")}:${context.queryParams("tab")}"
        }
      }
    )

    val html =
      Ssr.renderToStringFor(Ssr.Request(path = "/people/ada?tab=notes")) {
        div {
          div {
            text = "Before"
          }

          router(routes)

          div {
            text = "After"
          }
        }
      }

    html.shouldBe("<div><div>Before</div><div>ada:notes</div><div>After</div></div>")
  }

  it should "render internal router links with an SSR base path" in {
    val html =
      Ssr.renderToStringFor(Ssr.Request(path = "/", attributes = Map("basePath" -> "/base"))) {
        div {
          link("/people/ada?tab=notes") {
            text = "Ada"
          }
        }
      }

    html.shouldBe("""<div><a href="/base/people/ada?tab=notes">Ada</a></div>""")
  }

  it should "render lexical editor fallback on the server" in {
    val html =
      Ssr.renderToString {
        div {
          val lexicalEditor = editor("body") {
            defaultPlugins()
          }
          lexicalEditor.value = "Client-only value"
        }
      }

    html.shouldBe(
      """<div><div data-jfx-client-only="LexicalEditor" data-jfx-client-only-state="fallback" class="jfx-editor-host" data-jfx-control-name="body"><div class="jfx-editor-readonly"><p class="lexical-paragraph">Client-only value</p></div></div></div>"""
    )
  }

  it should "render highlighted CodeMirror nodes in the editor SSR fallback" in {
    val state =
      """{"root":{"children":[{"type":"codemirror","code":"val answer = 42\n// ok","language":"scala","version":1}],"type":"root","version":1}}"""

    val html =
      Ssr.renderToString {
        div {
          val lexicalEditor = editor("body")
          lexicalEditor.value = state
        }
      }

    html.should(include("""<pre class="jfx-editor-code language-scala">"""))
    html.should(include("""<span class="hljs-keyword">val</span>"""))
    html.should(include("""<span class="hljs-number">42</span>"""))
    html.should(include("""<span class="hljs-comment">// ok</span>"""))
  }

}
