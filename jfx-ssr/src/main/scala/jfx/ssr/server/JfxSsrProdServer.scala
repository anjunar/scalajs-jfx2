package jfx.ssr.server

import jfx.ssr.context.SsrRequestContext
import jfx.ssr.node.express.*

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Thenable.Implicits.*

object JfxSsrProdServer {

  trait Config extends js.Object {
    val port: Int
    val bindAddress: js.UndefOr[String]
  }

  object Config {
    def apply(port: Int, bindAddress: js.UndefOr[String] = js.undefined): Config =
      js.Dynamic.literal(
        port = port,
        bindAddress = bindAddress
      ).asInstanceOf[Config]
  }

  /**
   * Starts an express-based SSR server that accepts JSON POST payloads and returns HTML.
   *
   * Payload shape (kept intentionally small and generic):
   * `{ routePath: string, origin: string, cookie?: string, theme?: string }`
   */
  def start(renderer: JfxSsrRenderer, config: Config): Unit = {
    val app = express()
    app.disable("x-powered-by")
    app.use(express.json())

    // We keep request processing deterministic by queueing render promises.
    var renderQueue: js.Promise[Unit] = js.Promise.resolve[Unit](())

    app.post("/", (req: Request, res: Response, next: NextFunction) => {
        val body = req.body.getOrElse(js.undefined)
        val payload = if (js.isUndefined(body) || body == null) js.Dynamic.literal() else body.asInstanceOf[js.Dynamic]

        val routePath = payload.selectDynamic("routePath").asInstanceOf[js.UndefOr[String]].getOrElse("")
        val origin = payload.selectDynamic("origin").asInstanceOf[js.UndefOr[String]].getOrElse("")
        val cookie = payload.selectDynamic("cookie").asInstanceOf[js.UndefOr[String]]
        val theme = payload.selectDynamic("theme").asInstanceOf[js.UndefOr[String]].getOrElse("dark")

        if (routePath.isEmpty || origin.isEmpty) {
          res.status(400).end("Missing SSR parameters")
        } else {
          val ctx = SsrRequestContext(origin = origin, path = routePath, cookie = cookie)

          val current = renderQueue.`then`[String](_ => renderer.renderSsrWithTheme(routePath, theme, ctx))
          renderQueue = current.`then`[Unit](_ => ()).`catch`[Unit](_ => ())

          current.`then`[Unit](
            html => {
              res.status(200).set("Content-Type", "text/html; charset=utf-8").send(html.asInstanceOf[String])
            },
            err => {
              val message = try JSON.stringify(err.asInstanceOf[js.Any]) catch { case _: Throwable => String.valueOf(err) }
              res.status(500).end(message)
            }
          )
        }
      })

    app.listen(config.port, () => ())
  }
}
