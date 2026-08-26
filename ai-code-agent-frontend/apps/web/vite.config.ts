import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

const repoRoot = fileURLToPath(new URL('../../', import.meta.url))
const webSrc = fileURLToPath(new URL('./src', import.meta.url))

export default defineConfig({
    plugins: [react(), babel({ presets: [reactCompilerPreset()] })],
    resolve: {
        alias: {
            '@': webSrc,
            '@root': repoRoot,
            // 包内自引用别名：packages/*/src 内部以包名子路径互相导入时由 vite 解析。
            // 带尾斜杠，只匹配子路径（如 @ai-code-agent/shared/auth），
            // 不影响 apps/web 里的裸包名导入（走 pnpm workspace 的 node_modules 符号链接）。
            '@ai-code-agent/shared/': fileURLToPath(
                new URL('../../packages/shared/src/', import.meta.url),
            ),
            '@ai-code-agent/canvas/': fileURLToPath(
                new URL('../../packages/canvas/src/', import.meta.url),
            ),
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    build: {
        // Vite 8 在重建前会以「安全删除（trash）」方式清空 dist，
        // 而当前沙箱环境的 trash 操作被拦截，会导致 build 直接失败。
        // 关闭自动清空，依赖部署流程/CI 清理产物目录即可（index.html 引用带 hash 的文件名，残留旧文件不会被加载）。
        emptyOutDir: false,
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (!id.includes('node_modules')) {
                        return
                    }
                    if (id.includes('@xyflow') || id.includes('reactflow')) {
                        return 'vendor-xyflow'
                    }
                    if (
                        id.includes('antd') ||
                        id.includes('@ant-design') ||
                        id.includes('rc-') ||
                        id.includes('@rc-component') ||
                        id.includes('dayjs')
                    ) {
                        return 'vendor-antd'
                    }
                    if (
                        id.includes('/react') ||
                        id.includes('react-dom') ||
                        id.includes('scheduler')
                    ) {
                        return 'vendor-react'
                    }
                    return 'vendor'
                },
            },
        },
    },
})
