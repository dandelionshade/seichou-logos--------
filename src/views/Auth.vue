<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/authStore';
import { Mail, Lock, AlertCircle, Loader2, ArrowRight } from 'lucide-vue-next';

const router = useRouter();
const authStore = useAuthStore();

const isLogin = ref(true); // Toggle between Login and Register modes
const email = ref('');
const password = ref('');
const confirmPassword = ref('');

const errorMessage = ref('');
const fieldErrors = ref({ email: '', password: '', confirmPassword: '' });

const validateForm = () => {
  let isValid = true;
  fieldErrors.value = { email: '', password: '', confirmPassword: '' };

  // Email validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!email.value) {
    fieldErrors.value.email = 'Email is required';
    isValid = false;
  } else if (!emailRegex.test(email.value)) {
    fieldErrors.value.email = 'Invalid email format';
    isValid = false;
  }

  // Password validation
  if (!password.value) {
    fieldErrors.value.password = 'Password is required';
    isValid = false;
  } else if (password.value.length < 6) {
    fieldErrors.value.password = 'Password must be at least 6 characters';
    isValid = false;
  }

  // Confirm password validation (only for registration)
  if (!isLogin.value && password.value !== confirmPassword.value) {
    fieldErrors.value.confirmPassword = 'Passwords do not match';
    isValid = false;
  }

  return isValid;
};

const handleSubmit = async () => {
  errorMessage.value = '';
  
  if (!validateForm()) return;

  try {
    if (isLogin.value) {
      await authStore.login(email.value, password.value);
    } else {
      await authStore.register(email.value, password.value);
    }
    // Redirect to dashboard on success
    router.push('/');
  } catch (error: any) {
    errorMessage.value = error.message || 'Authentication failed. Please try again.';
  }
};

const toggleMode = () => {
  isLogin.value = !isLogin.value;
  errorMessage.value = '';
  fieldErrors.value = { email: '', password: '', confirmPassword: '' };
};

const modeText = computed(() => {
  return isLogin.value ? 
    { title: 'Welcome Back', subtitle: 'Continue your journey of growth', action: 'Sign in', switchText: 'New here?', switchLink: 'Create an account' } :
    { title: 'Create account', subtitle: 'Start your journey of growth and reflection', action: 'Sign up', switchText: 'Already have an account?', switchLink: 'Sign in' }
});

</script>

<template>
  <div class="min-h-screen bg-bg flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans text-ink selection:bg-accent-glow/30">
    <div class="sm:mx-auto sm:w-full sm:max-w-md animate-fade-in-up">
      <div class="flex justify-center items-center gap-3 mb-6">
        <div class="w-10 h-10 rounded-full bg-accent-glow/20 flex items-center justify-center">
          <div class="w-4 h-4 rounded-full bg-accent-glow"></div>
        </div>
      </div>
      <h2 class="text-center text-3xl font-serif font-bold tracking-tight text-ink transition-all">
        {{ modeText.title }}
      </h2>
      <p class="mt-2 text-center text-sm text-ink-dim transition-all">
        {{ modeText.subtitle }}
      </p>
    </div>

    <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md animate-fade-in-up" style="animation-delay: 0.1s;">
      <div class="bg-bg-surface py-8 px-4 shadow sm:rounded-2xl sm:px-10 border border-ink-dim/10 relative overflow-hidden">
        <form class="space-y-6 relative z-10" @submit.prevent="handleSubmit">
          
          <!-- Email Field -->
          <div>
            <label for="email" class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px]">Email address</label>
            <div class="mt-1 relative rounded-md shadow-sm">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Mail class="h-4 w-4 text-ink-dim" />
              </div>
              <input 
                id="email" 
                v-model="email" 
                type="email" 
                class="block w-full pl-10 bg-black/20 border border-ink-dim/20 rounded-lg py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
                :class="{'border-negative-tint focus:border-negative-tint focus:ring-negative-tint': fieldErrors.email}"
                placeholder="you@example.com" 
              />
            </div>
            <p v-if="fieldErrors.email" class="mt-1 text-xs text-negative-tint">{{ fieldErrors.email }}</p>
          </div>

          <!-- Password Field -->
          <div>
            <label for="password" class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px]">Password</label>
            <div class="mt-1 relative rounded-md shadow-sm">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock class="h-4 w-4 text-ink-dim" />
              </div>
              <input 
                id="password" 
                v-model="password" 
                type="password" 
                class="block w-full pl-10 bg-black/20 border border-ink-dim/20 rounded-lg py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
                :class="{'border-negative-tint focus:border-negative-tint focus:ring-negative-tint': fieldErrors.password}"
                placeholder="••••••••" 
              />
            </div>
            <p v-if="fieldErrors.password" class="mt-1 text-xs text-negative-tint">{{ fieldErrors.password }}</p>
          </div>

          <!-- Confirm Password Field (Only for Register) -->
          <div v-if="!isLogin" class="animate-fade-in-up" style="animation-duration: 0.3s;">
            <label for="confirmPassword" class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px]">Confirm Password</label>
            <div class="mt-1 relative rounded-md shadow-sm">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock class="h-4 w-4 text-ink-dim" />
              </div>
              <input 
                id="confirmPassword" 
                v-model="confirmPassword" 
                type="password" 
                class="block w-full pl-10 bg-black/20 border border-ink-dim/20 rounded-lg py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
                :class="{'border-negative-tint focus:border-negative-tint focus:ring-negative-tint': fieldErrors.confirmPassword}"
                placeholder="••••••••" 
              />
            </div>
            <p v-if="fieldErrors.confirmPassword" class="mt-1 text-xs text-negative-tint">{{ fieldErrors.confirmPassword }}</p>
          </div>

          <!-- Global Error Message -->
          <div v-if="errorMessage" class="p-3 bg-negative-tint/10 border border-negative-tint/30 text-negative-tint text-xs rounded-lg flex items-center gap-2">
            <AlertCircle :size="14" class="shrink-0" />
            {{ errorMessage }}
          </div>

          <!-- Submit Button -->
          <div class="mt-2">
            <!-- 默认补充一个提示语告诉用户可以自由把玩 -->
            <p v-if="isLogin" class="text-[10px] text-ink-dim/50 italic text-center mb-4">
              Tip: Local mocks wipe on code edits. <br/> Use <span class="font-bold text-ink-dim">admin@seichou.com</span> / <span class="font-bold text-ink-dim">password123</span>
            </p>

            <button 
              type="submit" 
              :disabled="authStore.loading"
              class="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-bg bg-accent-glow hover:bg-accent-glow/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-accent-glow focus:ring-offset-bg transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
            >
              <Loader2 v-if="authStore.loading" class="animate-spin h-4 w-4" />
              <span v-else>{{ modeText.action }}</span>
              <ArrowRight v-if="!authStore.loading" class="h-4 w-4" />
            </button>
          </div>
        </form>
        
        <div class="mt-6 text-center text-xs text-ink-dim relative z-10 transition-colors">
          {{ modeText.switchText }} 
          <button @click="toggleMode" type="button" class="text-accent-glow hover:underline transition-colors focus:outline-none">
            {{ modeText.switchLink }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}
</style>
