import { defineConfig } from 'vite'; // 引入 Vite 的配置定义函数，提供类型提示
import vue from '@vitejs/plugin-vue'; // 引入 Vite 的 Vue 插件，用于编译 .vue 单文件组件
import tailwindcss from '@tailwindcss/vite'; // 引入 Tailwind CSS 的 Vite 插件
import { VitePWA } from 'vite-plugin-pwa';
import path from 'path'; // 引入 Node.js 的 path 模块，用于处理路径

// 导出 Vite 的配置对象
export default defineConfig({
  // 配置 Vite 使用的插件列表
  plugins: [
    vue(), // 启用 Vue 支持
    tailwindcss(), // 启用 Tailwind CSS 支持
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['tree.svg'],
      manifest: {
        name: 'Seichou-Logos',
        short_name: 'Logos',
        description: 'AI Quantification Life-RPG',
        theme_color: '#0C0C0E',
        background_color: '#0C0C0E',
        display: 'standalone',
        icons: [
          {
            src: '/tree.svg',
            sizes: '192x192 512x512',
            type: 'image/svg+xml'
          }
        ]
      }
    })
  ],
  
  // 路径解析配置
  resolve: {
    alias: {
      // 配置 '@' 别名指向 'src' 目录，方便在代码中引入模块 (例如: import xxx from '@/components/xxx')
      '@': path.resolve(__dirname, './src'),
    },
  },
  
  // 定义全局常量替换
  define: {
    // 将 process.env.GEMINI_API_KEY 替换为实际的环境变量值
    // 注意：在实际的生产环境中，敏感的 API Key 不应该直接暴露给前端代码。
    // 这里主要是为了兼容某些纯前端直接调用 API 的演示场景。
    // 在我们当前的全栈架构中，DeepSeek API 的调用是在后端 (server.ts / Spring Boot) 完成的，所以前端不需要这个 Key。
    'process.env.GEMINI_API_KEY': JSON.stringify(process.env.GEMINI_API_KEY)
  }
});
