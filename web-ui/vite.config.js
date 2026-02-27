import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) }
  const apiUrl = process.env.VITE_API_BASE_URL
  return {
    build: {
      outDir: 'build',
    },
    plugins: [react()],
    server: {
      port: 3000,
      open: true,
      proxy: {
        '/api': {
          target: apiUrl,
          changeOrigin: true, // 修改请求头中的Origin字段
          secure: false, // 如果使用的是 HTTPS，需要设置为 true
          // rewrite: (path) => path.replace(/^\/api/, ''), // 重写路径
        },
        '/file': {
          target: apiUrl,
          changeOrigin: true,
          secure: false,
        },
      }
    }
  };
})