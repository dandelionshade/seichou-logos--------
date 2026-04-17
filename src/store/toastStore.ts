import { defineStore } from 'pinia';
import { ref } from 'vue';
import { soundFx } from '@/utils/audio';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: number;
  message: string;
  type: ToastType;
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastMessage[]>([]);
  let nextId = 0;

  const addToast = (message: string, type: ToastType = 'info') => {
    const id = nextId++;
    toasts.value.push({ id, message, type });
    
    // Play subtle audio feedback based on toast type
    if (type === 'error' || type === 'warning') {
      soundFx.error();
    } else if (type === 'success') {
      soundFx.success();
    } else {
      soundFx.click();
    }

    setTimeout(() => {
      removeToast(id);
    }, 4000);
  };

  const removeToast = (id: number) => {
    toasts.value = toasts.value.filter(t => t.id !== id);
  };

  return {
    toasts,
    addToast,
    removeToast
  };
});
