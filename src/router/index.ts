import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/authStore';

// 创建路由实例
const router = createRouter({
  history: createWebHistory(), // 使用 HTML5 History 模式
  routes: [
    {
      path: '/auth',
      name: 'Auth',
      component: () => import('@/views/Auth.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'), // 根路径使用 MainLayout 作为布局
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') }, // 默认子路由：工作台
        { path: 'chat', name: 'ChatRoom', component: () => import('@/views/ChatRoom.vue') }, // 智聊室
        { path: 'history', name: 'History', component: () => import('@/views/History.vue') }, // 历史记录页
        { path: 'reports', name: 'Reports', component: () => import('@/views/Reports.vue') }, // AI 报告页
        { path: 'garden', name: 'TreeGarden', component: () => import('@/views/TreeGarden.vue') }, // 养成花园页
        { path: 'settings', name: 'Settings', component: () => import('@/views/Settings.vue') }, // 设置页
        { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') }, // 个人主页
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
