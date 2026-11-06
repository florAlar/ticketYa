import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// base '/' — ya no gh-pages; front habla con API Gateway :8080
export default defineConfig({
  plugins: [react()],
  base: '/',
  server: {
    port: 5173,
  },
})
