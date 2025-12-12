import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server:{
    proxy:{
      '/api':{
        // target:'https://m1.apifoxmock.com/m1/6436769-6134289-default',
        //target:'http://localhost:8080',
        target:'http://192.168.1.35:8000',
        changeOrigin:true,
        //rewrite:(path)=>path.replace(/^\/api/,'')
      }
    }
  }
})
