import { existsSync } from "node:fs";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const rootDir = resolve(__dirname, "..");
const docsDir = resolve(rootDir, "docs");
const templatePath = resolve(docsDir, "index.html");
const scalaJsBundle = resolve(
  rootDir,
  "application",
  "target",
  "scala-3.8.3",
  "scalajs-jfx2-demo-opt",
  "main.js"
);

const siteUrl = "https://anjunar.github.io/scalajs-jfx2";
const basePath = "/scalajs-jfx2";

const routes = [
  route("/", "scalajs-jfx2 | Scala.js UI Library", "A Scala.js UI library for declarative components, SSR, hydration, typed controls, runtime metadata, and source-first I18n."),
  route("/button", "Buttons | scalajs-jfx2", "Action controls in the JFX2 component DSL."),
  route("/input", "Inputs And Forms | scalajs-jfx2", "Typed input controls, form context, model binding, and validation."),
  route("/combo-box", "ComboBox | scalajs-jfx2", "Typed selection with stable identity and reactive state."),
  route("/table-view", "TableView | scalajs-jfx2", "Reactive table rendering with remote loading, sorting, and crawlable SSR slices."),
  route("/virtual-list", "VirtualList | scalajs-jfx2", "Virtualized list rendering with route-aware crawlable SSR."),
  route("/layout", "Layout | scalajs-jfx2", "Declarative layout primitives for Scala.js UI composition."),
  route("/window", "Windows | scalajs-jfx2", "Overlay and window components with persistent page state."),
  route("/domain", "Domain Metadata | scalajs-jfx2", "Runtime class descriptors, reflected properties, validators, forms, and JSON mapping."),
  route("/image", "Images | scalajs-jfx2", "Image components and visual content handling in JFX2."),
  route("/image-cropper", "ImageCropper | scalajs-jfx2", "Client-side image cropper control for form workflows."),
  route("/editor", "Editor | scalajs-jfx2", "Lexical-backed editor integration as a normal JFX2 form control.")
];

assertBuilt();

const template = await readFile(templatePath, "utf8");
const { renderSsr } = await import(pathToFileURL(scalaJsBundle).href);

for (const entry of routes) {
  const appHtml = await renderSsr(entry.path);
  const html = prepareHtml(template, appHtml, entry);
  const outputPath = outputPathFor(entry.path);
  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, html, "utf8");
  console.log(`prerendered ${entry.path} -> ${relativeToDocs(outputPath)}`);
}

await writeFile(resolve(docsDir, "404.html"), fallback404(template), "utf8");
await writeFile(resolve(docsDir, ".nojekyll"), "", "utf8");
await writeFile(resolve(docsDir, "sitemap.xml"), sitemap(), "utf8");
await writeFile(resolve(docsDir, "robots.txt"), robots(), "utf8");

function route(path, title, description) {
  return {
    path,
    title,
    description,
    canonical: `${siteUrl}${path === "/" ? "/" : `${path}/`}`
  };
}

function prepareHtml(html, appHtml, entry) {
  return injectRoot(applyMeta(html, entry), appHtml);
}

function injectRoot(html, appHtml) {
  return html.replace('<div id="root"></div>', `<div id="root">${appHtml}</div>`);
}

function applyMeta(html, entry) {
  return html
    .replace(/<title>.*?<\/title>/, `<title>${escapeHtml(entry.title)}</title>`)
    .replace(/<link rel="canonical" href="[^"]*" \/>/, `<link rel="canonical" href="${entry.canonical}" />`)
    .replace(/<meta\s+name="description"\s+content="[^"]*"\s*\/>/, `<meta name="description" content="${escapeHtml(entry.description)}" />`)
    .replace(/<meta property="og:title" content="[^"]*" \/>/, `<meta property="og:title" content="${escapeHtml(entry.title)}" />`)
    .replace(/<meta\s+property="og:description"\s+content="[^"]*"\s*\/>/, `<meta property="og:description" content="${escapeHtml(entry.description)}" />`)
    .replace(/<meta property="og:url" content="[^"]*" \/>/, `<meta property="og:url" content="${entry.canonical}" />`)
    .replace(/<meta name="twitter:title" content="[^"]*" \/>/, `<meta name="twitter:title" content="${escapeHtml(entry.title)}" />`)
    .replace(/<meta\s+name="twitter:description"\s+content="[^"]*"\s*\/>/, `<meta name="twitter:description" content="${escapeHtml(entry.description)}" />`);
}

function fallback404(html) {
  const entry = route("/", "scalajs-jfx2", "Scala.js UI library for declarative, server-renderable application interfaces.");
  return applyMeta(html, entry)
    .replace('<meta name="robots" content="index, follow" />', '<meta name="robots" content="noindex" />')
    .replace('<div id="root"></div>', '<div id="root"></div>');
}

function outputPathFor(path) {
  if (path === "/") return resolve(docsDir, "index.html");
  return resolve(docsDir, path.slice(1), "index.html");
}

function relativeToDocs(path) {
  return path.replace(docsDir, "docs").replace(/\\/g, "/");
}

function sitemap() {
  const today = new Date().toISOString().slice(0, 10);
  const urls = routes.map(entry => `  <url>
    <loc>${entry.canonical}</loc>
    <lastmod>${today}</lastmod>
    <changefreq>weekly</changefreq>
    <priority>${entry.path === "/" ? "1.0" : "0.8"}</priority>
  </url>`).join("\n");

  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>`;
}

function robots() {
  return `User-agent: *
Allow: /

Sitemap: ${siteUrl}/sitemap.xml
`;
}

function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function assertBuilt() {
  const missing = [
    [templatePath, "docs/index.html"],
    [scalaJsBundle, "Scala.js fullOptJS bundle"]
  ].filter(([path]) => !existsSync(path));

  if (missing.length === 0) return;

  const labels = missing.map(([, label]) => `- ${label}`).join("\n");
  throw new Error(
    `Build-time SSR needs a production build first.\nMissing:\n${labels}\n\nRun: npm run build:pages`
  );
}
