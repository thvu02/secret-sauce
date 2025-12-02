import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  base: '/',
  plugins: [
    react(),
    tailwindcss(),
  ],
  build: {
    outDir: '../build'
  },
  // @ts-expect-error - test config is valid but not in base Vite types
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: './src/test-setup.ts',
  },
})
