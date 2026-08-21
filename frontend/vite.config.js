import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        port: 5173,
        host: '0.0.0.0',
        proxy: {
            // 末尾斜杠避免把 /api/user-report 误匹配为认证接口。
            '/api/user/': {
                target: 'http://localhost:18099',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            '/api/auth': {
                target: 'http://localhost:18099',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            '/api/signup': {
                target: 'http://localhost:18099',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            // 仪表盘趋势与用水占比直接由 IoT 服务提供。
            '/api/Data': {
                target: 'http://localhost:18016',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            // 明日用水预测由预测服务基于近七天记录计算。
            '/api/ai': {
                target: 'http://localhost:18017',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            // 报修服务在本地开发时直接转发，避免网关鉴权配置掩盖业务错误。
            '/api/user-report': {
                target: 'http://localhost:20000',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            '/api/operations': {
                target: 'http://localhost:20000',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
            '/api': {
                target: 'http://localhost:18080',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/api/, ''); },
            },
        },
    },
});
