import { readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { brotliCompressSync, constants, gzipSync } from "node:zlib";
import react from "@vitejs/plugin-react";
import { defineConfig, type Plugin } from "vite";

const compressible = /\.(js|css|html|svg|json|ico)$/;

function precompressAssets(): Plugin {
  let outputDirectory = "";

  return {
    name: "precompress-assets",
    apply: "build",
    configResolved(config) {
      outputDirectory = resolve(config.root, config.build.outDir);
    },
    closeBundle() {
      const entries = readdirSync(outputDirectory, {
        recursive: true,
        withFileTypes: true,
      });

      for (const entry of entries) {
        if (!entry.isFile() || !compressible.test(entry.name)) continue;

        const file = join(entry.parentPath, entry.name);
        const source = readFileSync(file);

        writeFileSync(`${file}.gz`, gzipSync(source, { level: 9 }));
        writeFileSync(
          `${file}.br`,
          brotliCompressSync(source, {
            params: { [constants.BROTLI_PARAM_QUALITY]: 11 },
          }),
        );
      }
    },
  };
}

export default defineConfig(({ command }) => ({
  plugins: [react(), precompressAssets()],
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              test: /node_modules[\\/](react|react-dom|scheduler)[\\/]/,
              name: "react",
            },
            { test: /node_modules[\\/]@navikt[\\/]/, name: "aksel" },
            { test: /node_modules[\\/]/, name: "vendor" },
          ],
        },
      },
    },
  },
  css: {
    modules: {
      generateScopedName:
        command === "build" ? "[hash:base64:6]" : "[name]__[local]___[hash:base64:5]",
    },
  },
  server: {
    host: "localhost",
    strictPort: true,
    proxy:
      command === "serve"
        ? {
            "/api/lab": {
              target: "http://localhost:3000",
              changeOrigin: true,
            },
          }
        : undefined,
  },
}));
