import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    proxy: {
      '/rekol-api': {
        target: 'https://wefyx.pro',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
