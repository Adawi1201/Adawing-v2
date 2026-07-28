import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Self-host Vditor lazy-loaded assets (lute parser, KaTeX, highlight.js, ...)
// under /vditor so `cdn: '/vditor'` resolves to /vditor/dist/js/... served by
// our own nginx instead of the external unpkg CDN. These are runtime static
// files, not part of the JS bundle, so they add zero JS chunk weight.
const VDITOR_DIST = fileURLToPath(new URL('./node_modules/vditor/dist', import.meta.url))
const MIME = {
  '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.wasm': 'application/wasm', '.map': 'application/json',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf',
  '.svg': 'image/svg+xml', '.png': 'image/png'
}

function vditorSelfHost() {
  return {
    name: 'vditor-self-host',
    // Production: copy the runtime assets into the build output at /vditor/dist.
    writeBundle(options) {
      const outDir = options.dir || fileURLToPath(new URL('./dist', import.meta.url))
      const target = path.join(outDir, 'vditor', 'dist')
      for (const sub of ['js', 'css']) {
        const from = path.join(VDITOR_DIST, sub)
        if (fs.existsSync(from)) {
          fs.cpSync(from, path.join(target, sub), { recursive: true })
        }
      }
    },
    // Dev server: serve /vditor/dist/* straight from node_modules/vditor/dist.
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url || !req.url.startsWith('/vditor/dist/')) return next()
        const rel = decodeURIComponent(req.url.split('?')[0].replace('/vditor/dist/', ''))
        const filePath = path.join(VDITOR_DIST, rel)
        if (!filePath.startsWith(VDITOR_DIST) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
          return next()
        }
        res.setHeader('Content-Type', MIME[path.extname(filePath)] || 'application/octet-stream')
        fs.createReadStream(filePath).pipe(res)
      })
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), vditorSelfHost()],
  server: {
    proxy: {
      '/api/v2': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
