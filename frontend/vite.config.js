import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  css: { preprocessorOptions: { scss: { api: 'modern', silenceDeprecations: ['legacy-js-api'] } } },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
      '/webase': {
        target: 'http://175.178.120.23:5002',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/webase/, '/WeBASE-Front'),
      },
    },
  },
})
