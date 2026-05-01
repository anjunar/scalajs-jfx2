import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import tailwindcss from "@tailwindcss/vite";
import { existsSync } from "node:fs";
import { dirname, normalize, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

const scalaJsFastOptMain = resolve(
    __dirname,
    "application",
    "target",
    "scala-3.3.7",
    "scalajs-jfx2-demo-fastopt",
    "main.js"
);

const scalaJsFullOptMain = resolve(
    __dirname,
    "application",
    "target",
    "scala-3.3.7",
    "scalajs-jfx2-demo-opt",
    "main.js"
);

export default defineConfig(({ command }) => ({
    base: "/scalajs-jfx2/",
    root: "application/src/main/webapp",
    server: {
        fs: {
            allow: [resolve(__dirname, ".")],
        },
        clearScreen: false,
    },
    build: {
        outDir: resolve(__dirname, "docs"),
        emptyOutDir: true,
    },
    plugins: [
        tailwindcss(),
        scalaJsProductionBundle(scalaJsFullOptMain),
        scalaJsDevFallback(scalaJsFastOptMain),
        command === "serve" && scalaJSPlugin({
            cwd: ".",
            projectID: "scalajs-jfx2-demo",
        }),
    ].filter(Boolean)
}));

function scalaJsProductionBundle(scalaJsMainFile) {
    const moduleLabel = "scalajs:main.js";
    let isBuild = false;

    return {
        name: "jfx:scalajs-production-bundle",
        enforce: "pre",

        configResolved(config) {
            isBuild = config.command === "build";
        },

        resolveId(source) {
            if (isBuild && source === moduleLabel) {
                return scalaJsMainFile;
            }
            return null;
        },
    };
}

function scalaJsDevFallback(scalaJsMainFile) {
    const virtualId = "\0jfx:missing-scalajs-main";
    const normalizedMainFile = normalizePath(scalaJsMainFile);
    const moduleLabel = "scalajs:main.js";
    let isServe = false;

    return {
        name: "jfx:scalajs-dev-fallback",

        configResolved(config) {
            isServe = config.command === "serve";
        },

        configureServer(server) {
            server.watcher.add(dirname(scalaJsMainFile));
            server.watcher.on("add", changedPath => {
                if (normalizePath(changedPath) === normalizedMainFile) {
                    const module = server.moduleGraph.getModuleById(virtualId);
                    if (module) server.moduleGraph.invalidateModule(module);
                    server.ws.send({ type: "full-reload" });
                }
            });
        },

        resolveId(source) {
            if (isServe && source === moduleLabel && !existsSync(scalaJsMainFile)) {
                return virtualId;
            }
            return null;
        },

        load(id) {
            if (!isServe) return null;

            const normalizedId = normalizePath(id);
            const isScalaJsMain =
                id === virtualId || normalizedId === normalizedMainFile;

            if (isScalaJsMain && !existsSync(scalaJsMainFile)) {
                return missingScalaJsModule(scalaJsMainFile);
            }

            return null;
        },
    };
}

function missingScalaJsModule(scalaJsMainFile) {
    const message =
        "Scala.js bundle fehlt. Wahrscheinlich ist fastLinkJS wegen Compilefehlern nicht fertig geworden.";
    const hint =
        "Fehler in Scala beheben und dann sbtn-x86_64-pc-win32.exe scalajs-jfx2-demo/fastLinkJS laufen lassen.";
    const escapedMessage = JSON.stringify(message);
    const escapedHint = JSON.stringify(hint);
    const escapedPath = JSON.stringify(scalaJsMainFile);

    return `
const message = ${escapedMessage};
const hint = ${escapedHint};
const bundlePath = ${escapedPath};

export function boot() {
  console.warn(message + " " + hint, bundlePath);
  const root = globalThis.document?.getElementById("root");
  if (!root) return;
  root.innerHTML =
    '<div style="font: 14px/1.45 system-ui, sans-serif; margin: 24px; max-width: 760px;">' +
    '<h1 style="font-size: 20px; margin: 0 0 8px;">Scala.js bundle fehlt</h1>' +
    '<p style="margin: 0 0 8px;">' + message + '</p>' +
    '<p style="margin: 0 0 8px;">' + hint + '</p>' +
    '<code style="display: block; white-space: pre-wrap;">' + bundlePath + '</code>' +
    '</div>';
}

export function renderSsr() {
  return '<div class="jfx-scalajs-missing"><strong>Scala.js bundle fehlt.</strong> ' + message + '</div>';
}
`;
}

function normalizePath(path) {
    return normalize(path).replace(/\\/g, "/");
}
