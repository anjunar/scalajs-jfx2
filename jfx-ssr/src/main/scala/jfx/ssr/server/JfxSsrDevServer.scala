package jfx.ssr.server

import jfx.ssr.context.SsrRequestContext
import jfx.ssr.node.express.*
import jfx.ssr.node.fs.fsPromises
import jfx.ssr.node.vite.vite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*

object JfxSsrDevServer {

  trait Config extends js.Object {
    val port: Int
    val backendOrigin: String
    val templatePath: String
    val viteInlineConfig: js.Object

    /**
     * Decide if a given request should be server-rendered as HTML.
     * If false, request falls through to Vite middlewares.
     */
    val shouldRenderHtml: js.Function1[Request, Boolean]

    /** Derive a theme string from the raw cookie header. */
    val resolveTheme: js.Function1[String, String]

    /** Inject SSR HTML into the template. */
    val injectRoot: js.Function2[String, String, String]
  }

  object Config {
    def apply(
      port: Int,
      backendOrigin: String,
      templatePath: String,
      viteInlineConfig: js.Object,
      shouldRenderHtml: js.Function1[Request, Boolean],
      resolveTheme: js.Function1[String, String] = (cookie: String) => "dark",
      injectRoot: js.Function2[String, String, String] = (html: String, appHtml: String) =>
        html.replace("<div id=\"root\"></div>", s"<div id=\"root\">$appHtml</div>")
    ): Config =
      js.Dynamic.literal(
        port = port,
        backendOrigin = backendOrigin,
        templatePath = templatePath,
        viteInlineConfig = viteInlineConfig,
        shouldRenderHtml = shouldRenderHtml,
        resolveTheme = resolveTheme,
        injectRoot = injectRoot
      ).asInstanceOf[Config]
  }

  def start(renderer: JfxSsrRenderer, config: Config): js.Promise[Unit] = {
    val app = express()
    app.disable("x-powered-by")

    vite.createServer(config.viteInlineConfig).`then`[Unit] { viteServer =>
      // Raw SSR fragment endpoint (useful for debugging)
      app.get(
        "/__ssr",
        (req: Request, res: Response, next: NextFunction) => {
          val cookie = req.headers.selectDynamic("cookie").asInstanceOf[js.UndefOr[String]].getOrElse("")
          val theme = config.resolveTheme(cookie)
          val pathValue = req.query.selectDynamic("path").asInstanceOf[js.UndefOr[String]].getOrElse("/")
          val path = normalizeRenderPath(pathValue)

          val ctx = SsrRequestContext(
            origin = config.backendOrigin,
            path = path,
            cookie = cookie
          )

          renderer.renderSsrWithTheme(path, theme, ctx).toFuture
            .map { html => res.`type`("html").send(html) }
            .recover { case err =>
              viteServer.ssrFixStacktrace(err.asInstanceOf[js.Any])
              next(err.asInstanceOf[js.Any])
            }

          ()
        }
      )

      // Main HTML handler
      app.use((req: Request, res: Response, next: NextFunction) => {
        if (!config.shouldRenderHtml(req)) {
          next(js.undefined)
        } else {
          val cookie = req.headers.selectDynamic("cookie").asInstanceOf[js.UndefOr[String]].getOrElse("")
          val theme = config.resolveTheme(cookie)

          val url = req.originalUrl
          fsPromises.readFile(config.templatePath, "utf8").toFuture
            .flatMap { source =>
              viteServer.transformIndexHtml(url, source).toFuture
            }
            .flatMap { transformed =>
              val path = normalizeRenderPath(url)
              val ctx = SsrRequestContext(
                origin = config.backendOrigin,
                path = path,
                cookie = cookie
              )
              renderer.renderSsrWithTheme(path, theme, ctx).toFuture
                .map(appHtml => config.injectRoot(transformed, appHtml))
            }
            .map { finalHtml =>
              res.status(200).set("Cache-Control", "no-store").`type`("html").send(finalHtml)
            }
            .recover { case err =>
              viteServer.ssrFixStacktrace(err.asInstanceOf[js.Any])
              next(err.asInstanceOf[js.Any])
            }

          ()
        }
      })

      // Vite middleware (HMR, static assets, etc.)
      app.use(viteServer.middlewares.asInstanceOf[RequestHandler])

      app.listen(config.port, () => ())
      ()
    }.asInstanceOf[js.Promise[Unit]]
  }

  private def normalizeRenderPath(value: String): String = {
    val raw = Option(value).filter(_.nonEmpty).getOrElse("/")
    val withoutOrigin = raw.replaceAll("^https?://[^/]+", "")
    val pathWithSearch = withoutOrigin.split("#").headOption.getOrElse("/")
    if (pathWithSearch.startsWith("/")) pathWithSearch else s"/$pathWithSearch"
  }
}
