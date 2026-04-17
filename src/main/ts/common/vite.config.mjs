import { defineConfig } from 'vite'
import { resolve } from 'path'
import eslint from 'vite-plugin-eslint';

export default defineConfig({
  plugins: [
    eslint(),
  ],
  build: {
    outDir: '../../../../target/classes/META-INF/resources/js/',
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'ReposisCommon',
      formats: ['iife'],
      fileName: () => `reposis-common.js`,
    },
  },
})
