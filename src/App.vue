<script setup lang="ts">
// App.vue 现在作为根组件，只负责渲染路由视图 (router-view)
// 具体的页面布局和内容已经拆分到 src/layout/MainLayout.vue 和 src/views/ 下
import ToastContainer from './components/ToastContainer.vue';
</script>

<template>
  <router-view />
  <ToastContainer />
</template>

<style>
/* 定义全局 CSS 动画关键帧 */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes fill85 {
  from { width: 0; }
  to { width: 85%; }
}
@keyframes fill40 {
  from { width: 0; }
  to { width: 40%; }
}

/* 将动画应用到对应的 CSS 类上 */
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
}
.animate-fade-in-right {
  animation: fadeInRight 0.5s ease-out forwards;
}
.animate-fill-85 {
  animation: fill85 1s ease-out forwards;
}
.animate-fill-40 {
  animation: fill40 1s ease-out forwards;
}
</style>
