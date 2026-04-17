<script setup lang="ts">
import { useToastStore } from '@/store/toastStore';
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-vue-next';

const toastStore = useToastStore();
</script>

<template>
  <div class="fixed bottom-6 right-6 z-50 flex flex-col gap-3 pointer-events-none">
    <TransitionGroup name="toast">
      <div 
        v-for="toast in toastStore.toasts" 
        :key="toast.id"
        class="toast-item pointer-events-auto flex items-start gap-3 min-w-[300px] max-w-md p-4 rounded-xl shadow-xl border bg-bg/95 backdrop-blur-md"
        :class="{
          'border-accent-glow/50 shadow-[0_4px_20px_-10px_rgba(136,214,108,0.3)]': toast.type === 'success',
          'border-negative-tint/50 shadow-[0_4px_20px_-10px_rgba(255,107,107,0.3)]': toast.type === 'error',
          'border-vision/50 shadow-[0_4px_20px_-10px_rgba(250,204,21,0.3)]': toast.type === 'warning',
          'border-blue-400/50 shadow-[0_4px_20px_-10px_rgba(96,165,250,0.3)]': toast.type === 'info',
        }"
      >
        <!-- Icon logic -->
        <div class="shrink-0 mt-0.5">
          <CheckCircle v-if="toast.type === 'success'" class="text-accent-glow" :size="18" />
          <AlertCircle v-else-if="toast.type === 'error'" class="text-negative-tint" :size="18" />
          <AlertTriangle v-else-if="toast.type === 'warning'" class="text-vision" :size="18" />
          <Info v-else class="text-blue-400" :size="18" />
        </div>
        
        <!-- Message -->
        <div class="flex-1 text-sm text-ink font-medium leading-tight pr-4">
          {{ toast.message }}
        </div>
        
        <!-- Close Button -->
        <button 
          @click="toastStore.removeToast(toast.id)" 
          class="shrink-0 text-ink-dim hover:text-ink transition-colors focus:outline-none"
        >
          <X :size="14" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(50px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: scale(0.95);
  margin-bottom: -50px;
}
</style>
