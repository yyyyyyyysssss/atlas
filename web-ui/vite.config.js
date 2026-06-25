import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import http from 'node:http';
import https from 'node:https';

export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) }
  const apiUrl = process.env.VITE_API_BASE_URL
  const isHttps = apiUrl.startsWith('https')
  const agent = isHttps
    ? new https.Agent({ keepAlive: true, keepAliveMsecs: 5000 })
    : new http.Agent({ keepAlive: true, keepAliveMsecs: 5000 })
  return {
    build: {
      outDir: 'build',
    },
    plugins: [react()],
    server: {
      port: 3000,
      host: '0.0.0.0',
      open: true,
      allowedHosts: [
        'atlas.ys0921.sbs'
      ],
      proxy: {
        '/api': {
          target: apiUrl,
          changeOrigin: true, // 修改请求头中的Origin字段
          secure: isHttps, // 如果使用的是 HTTPS，需要设置为 true
          // rewrite: (path) => path.replace(/^\/api/, ''), // 重写路径
          agent: agent
        },
        '/file': {
          target: apiUrl,
          changeOrigin: true,
          secure: isHttps,
          agent: agent
        },
      }
    }
  };
})