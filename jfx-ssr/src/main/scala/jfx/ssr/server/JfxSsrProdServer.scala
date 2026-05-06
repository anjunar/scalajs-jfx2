package jfx.ssr.server

import jfx.ssr.context.SsrRequestContext
import jfx.ssr.node.express.*
import jfx.ssr.node.fs.fsPromises
import jfx.ssr.node.path.nodePath

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Thenable.Implicits.*

object JfxSsrProdServer {

  trait Config extends js.Object {
    val port: Int
    val bindAddress: js.UndefOr[String]
    val resolveTheme: js.Function1[String, String]
    val applyThemeToHtml: js.Function2[String, String, String]
    val injectRoot: js.Function2[String, String, String]
  }

  object Config {
    def apply(
      port: Int,
      bindAddress: js.UndefOr[String] = js.undefined,
      resolveTheme: js.Function1[String, String] = (_: String) => "dark",
      applyThemeToHtml: js.Function2[String, String, String] = (html: String, _: String) => html,
      injectRoot: js.Function2[String, String, String] = (html: String, appHtml: String) =>
        html.replace("<div id=\"root\"></div>", s"<div id=\"root\">$appHtml</div>")
    ): Config =
      js.Dynamic.literal(
        port = port,
        bindAddress = bindAddress,
        resolveTheme = resolveTheme,
        applyThemeToHtml = applyThemeToHtml,
        injectRoot = injectRoot
      ).asInstanceOf[Config]
  }

  /**
   * Starts an express-based SSR server that accepts JSON POST payloads and returns HTML.
   *
   * Payload shape (kept intentionally small and generic):
   * `{ staticPath: string, routePath: string, origin: string, cookie?: string }`
   */
  def start(renderer: JfxSsrRenderer, config: Config): Unit = {
    val app = express()
    app.disable("x-powered-by")
    app.use(express.json())

    // We keep request processing deterministic by queueing render promises.
    var renderQueue: js.Promise[Unit] = js.Promise.resolve[Unit](())

    var cachedTemplate: String | Null = null
    var cachedStaticPath: String | Null = null

    app.post("/", (req: Request, res: Response, next: NextFunction) => {
        val body = req.body.getOrElse(js.undefined)
        val payload = if (js.isUndefined(body) || body == null) js.Dynamic.literal() else body.asInstanceOf[js.Dynamic]

        val staticPath = payload.selectDynamic("staticPath").asInstanceOf[js.UndefOr[String]].getOrElse("")
        val routePath = payload.selectDynamic("routePath").asInstanceOf[js.UndefOr[String]].getOrElse("")
        val origin = payload.selectDynamic("origin").asInstanceOf[js.UndefOr[String]].getOrElse("")
        val cookie = payload.selectDynamic("cookie").asInstanceOf[js.UndefOr[String]]

        if (staticPath.isEmpty || routePath.isEmpty || origin.isEmpty) {
          res.status(400).end("Missing SSR parameters")
        } else {
          val normalizedPath = normalizeRenderPath(routePath)
          val cookieHeader = cookie.getOrElse("")
          val theme = config.resolveTheme(cookieHeader)
          val ctx = SsrRequestContext(origin = origin, path = normalizedPath, cookie = cookie)

          def ensureTemplate(): js.Promise[String] = {
            if (cachedTemplate != null && cachedStaticPath == staticPath) {
              js.Promise.resolve(cachedTemplate.asInstanceOf[String])
            } else {
              val indexPath = nodePath.join(staticPath, "index.html")
              fsPromises.readFile(indexPath, "utf8").`then`[String] { html =>
                cachedTemplate = html
                cachedStaticPath = staticPath
                html
              }
            }
          }

          val current = renderQueue.`then`[String] { _ =>
            ensureTemplate().`then`[String] { template =>
              val themedTemplate = config.applyThemeToHtml(template, theme)
              renderer.renderSsrWithTheme(normalizedPath, theme, ctx).`then`[String] { appHtml =>
                config.injectRoot(themedTemplate, appHtml)
              }
            }
          }
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

  private def normalizeRenderPath(value: String): String = {
    val raw = Option(value).filter(_.nonEmpty).getOrElse("/")
    val withoutOrigin = raw.replaceAll("^https?://[^/]+", "")
    val pathWithSearch = withoutOrigin.split("#").headOption.getOrElse("/")
    if (pathWithSearch.startsWith("/")) pathWithSearch else s"/$pathWithSearch"
  }
}
