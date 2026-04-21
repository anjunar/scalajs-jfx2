import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import tailwindcss from "@tailwindcss/vite";
import { resolve } from "node:path";

export default defineConfig({
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
        scalaJSPlugin({
            cwd: ".",
            projectID: "scalajs-jfx2-demo",
        }),
    ]
});
