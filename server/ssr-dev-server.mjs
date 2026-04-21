import express from 'express'
import { readFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer as createViteServer } from 'vite'

const __dirname = dirname(fileURLToPath(import.meta.url))
const rootDir = resolve(__dirname, '..')
const webappDir = resolve(rootDir, 'application', 'src', 'main', 'webapp')
const templatePath = resolve(webappDir, 'index.html')

const app = express()
const port = Number.parseInt(process.env.PORT ?? '5174', 10)
const basePath = '/scalajs-jfx2'

const vite = await createViteServer({
  appType: 'custom',
  server: {
    middlewareMode: true,
  },
})

app.disable('x-powered-by')

app.get('/', (_request, response) => {
  response.redirect(302, `${basePath}/`)
})

app.get(`${basePath}/__ssr`, async (request, response, next) => {
  try {
    const { renderSsr } = await vite.ssrLoadModule('/src/main.js')
    const path = normalizeRenderPath(request.query.path)
    response.type('html').send(await renderSsr(path))
  } catch (error) {
    vite.ssrFixStacktrace(error)
    next(error)
  }
})

app.use(async (request, response, next) => {
  if (!shouldRenderHtml(request)) {
    next()
    return
  }

  try {
    const requestUrl = request.originalUrl
    const renderPath = normalizeRenderPath(requestUrl)
    const sourceTemplate = await readFile(templatePath, 'utf8')
    const transformedTemplate = await vite.transformIndexHtml(requestUrl, sourceTemplate)
    const { renderSsr } = await vite.ssrLoadModule('/src/main.js')
    const appHtml = await renderSsr(renderPath)
    const html = injectRoot(transformedTemplate, appHtml)

    response
      .status(200)
      .set('Cache-Control', 'no-store')
      .type('html')
      .send(html)
  } catch (error) {
    vite.ssrFixStacktrace(error)
    next(error)
  }
})

app.use(vite.middlewares)

app.listen(port, () => {
  console.log(`JFX2 Vite SSR dev server running at http://localhost:${port}${basePath}/`)
  console.log(`Raw SSR fragment: http://localhost:${port}${basePath}/__ssr?path=/`)
})

function injectRoot(html, appHtml) {
  return html.replace('<div id="root"></div>', `<div id="root">${appHtml}</div>`)
}

function normalizeRenderPath(value) {
  const raw = typeof value === 'string' && value.length > 0 ? value : '/'
  const withoutOrigin = raw.replace(/^https?:\/\/[^/]+/i, '')
  const withoutBase = withoutOrigin.startsWith(basePath)
    ? withoutOrigin.slice(basePath.length) || '/'
    : withoutOrigin
  const pathWithSearch = withoutBase.split('#')[0] || '/'

  return pathWithSearch.startsWith('/') ? pathWithSearch : `/${pathWithSearch}`
}

function isAssetRequest(url) {
  const path = url.split('?')[0]
  return /\.[a-z0-9]+$/i.test(path)
}

function isViteInternalRequest(url) {
  const path = url.split('?')[0]
  return path.includes('/@vite/') || path.includes('/@fs/') || path.includes('/@id/')
}

function shouldRenderHtml(request) {
  if (request.method !== 'GET') return false
  if (isAssetRequest(request.originalUrl)) return false
  if (isViteInternalRequest(request.originalUrl)) return false

  const accept = request.headers.accept ?? ''
  return accept.includes('text/html') || accept.includes('*/*') || accept.length === 0
}
