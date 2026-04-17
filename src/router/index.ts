import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '@/layout/MainLayout.vue';
import Dashboard from '@/views/Dashboard.vue';
import History from '@/views/History.vue';
import Reports from '@/views/Reports.vue';
import Settings from '@/views/Settings.vue';
import Profile from '@/views/Profile.vue';
import Auth from '@/views/Auth.vue';
import ChatRoom from '@/views/ChatRoom.vue';
import TreeGarden from '@/views/TreeGarden.vue';
import { useAuthStore } from '@/store/authStore';

// 创建路由实例
const router = createRouter({
  history: createWebHistory(), // 使用 HTML5 History 模式
  routes: [
    {
      path: '/auth',
      name: 'Auth',
      component: Auth,
      meta: { requiresGuest: true }
    },
    {
      path: '/',
      component: MainLayout, // 根路径使用 MainLayout 作为布局
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: Dashboard }, // 默认子路由：工作台
        { path: 'chat', name: 'ChatRoom', component: ChatRoom }, // 智聊室
        { path: 'history', name: 'History', component: History }, // 历史记录页
        { path: 'reports', name: 'Reports', component: Reports }, // AI 报告页
        { path: 'garden', name: 'TreeGarden', component: TreeGarden }, // 养成花园页
        { path: 'settings', name: 'Settings', component: Settings }, // 设置页
        { path: 'profile', name: 'Profile', component: Profile }, // 个人主页
      ]
    }
  ]
});

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.isAuthenticated;

  if (to.meta.requiresAuth && !isAuthenticated) {
    // 目标路由需要登录，但用户未登录，跳转到 Auth 页
    next('/auth');
  } else if (to.meta.requiresGuest && isAuthenticated) {
    // 目标路由需要未登录（如登录页），但用户已登录，跳转到首页
    next('/');
  } else {
    // 其他情况正常放行
    next();
  }
});

export default router;
