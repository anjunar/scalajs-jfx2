import express from 'express'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const rootDir = resolve(__dirname, '..')
const docsDir = resolve(rootDir, 'docs')
const templatePath = resolve(docsDir, 'index.html')
const scalaJsServerBundle = resolve(
  rootDir,
  'application',
  'target',
  'scala-3.8.3',
  'scalajs-jfx2-demo-opt',
  'main.js'
)

const app = express()
const port = Number.parseInt(process.env.PORT ?? '4174', 10)
const basePath = '/scalajs-jfx2'

assertBuilt()

const template = readFileSync(templatePath, 'utf8')
const { renderSsr } = await import(pathToFileURL(scalaJsServerBundle).href)

app.disable('x-powered-by')

app.use(
  basePath,
  express.static(docsDir, {
    index: false,
    immutable: true,
    maxAge: '1y',
  })
)

app.get('/', (_request, response) => {
  response.redirect(302, `${basePath}/`)
})

app.get(`${basePath}/__ssr`, async (request, response, next) => {
  try {
    const path = normalizeRenderPath(request.query.path)
    response.type('html').send(await renderSsr(path))
  } catch (error) {
    next(error)
  }
})

app.get(`${basePath}/*`, (request, response, next) => {
  if (isAssetRequest(request.path)) {
    next()
    return
  }

  renderPage(request, response, next)
})

app.use((request, response, next) => {
  if (request.method === 'GET' && !isAssetRequest(request.path)) {
    renderPage(request, response, next)
    return
  }

  response.status(404).type('text').send('Not found')
})

app.listen(port, () => {
  console.log(`JFX2 SSR demo running at http://localhost:${port}${basePath}/`)
  console.log(`Raw SSR fragment: http://localhost:${port}${basePath}/__ssr?path=/`)
})

async function renderPage(request, response, next) {
  try {
    const renderPath = normalizeRenderPath(request.originalUrl)
    const ssrHtml = await renderSsr(renderPath)
    const html = injectRoot(template, ssrHtml)

    response
      .status(200)
      .set('Cache-Control', 'no-store')
      .type('html')
      .send(html)
  } catch (error) {
    next(error)
  }
}

function injectRoot(html, ssrHtml) {
  return html.replace('<div id="root"></div>', `<div id="root">${ssrHtml}</div>`)
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

function isAssetRequest(path) {
  return /\.[a-z0-9]+$/i.test(path)
}

function assertBuilt() {
  const missing = [
    [templatePath, 'docs/index.html'],
    [scalaJsServerBundle, 'Scala.js fullOptJS bundle'],
  ].filter(([path]) => !existsSync(path))

  if (missing.length === 0) return

  const labels = missing.map(([, label]) => `- ${label}`).join('\n')
  throw new Error(
    `SSR server needs a production build first.\nMissing:\n${labels}\n\nRun: npm run build`
  )
}
