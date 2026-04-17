import { createApp } from 'vue'; // 从 Vue 核心库引入 createApp 函数，用于创建应用实例
import { createPinia } from 'pinia'; // 引入 Pinia 状态管理
import router from './router'; // 引入 Vue Router 路由配置
import './index.css'; // 引入全局 CSS 样式文件（包含 Tailwind CSS 的配置）
import App from './App.vue'; // 引入根组件 App.vue
import { i18n } from './i18n'; // 引入配置好的国际化 (i18n) 实例

// 注册 PWA Service Worker
import { registerSW } from 'virtual:pwa-register';
registerSW({ immediate: true });

// 创建 Vue 应用实例
const app = createApp(App);
// 创建 Pinia 实例
const pinia = createPinia();

// 注册插件
app.use(pinia); // 使用状态管理
app.use(router); // 使用路由
app.use(i18n); // 使用多语言

// 将 Vue 应用挂载到 index.html 中 id 为 'root' 的 DOM 节点上
app.mount('#root');
