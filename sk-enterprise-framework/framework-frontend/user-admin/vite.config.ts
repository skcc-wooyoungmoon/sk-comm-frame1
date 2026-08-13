import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 서버(5173)에서 /api 요청을 백엔드(8080)로 프록시한다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
