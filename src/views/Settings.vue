<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import {
  Moon, Sun, Bell, Shield, Save, Loader2, 
  Languages, Brain, Download, Trash2, CheckCircle2, KeyRound
} from 'lucide-vue-next';
import { usePreferenceStore } from '@/store/preferenceStore';

const { t } = useI18n();
const route = useRoute();
const prefStore = usePreferenceStore();

const isSaving = ref(false);
const showSuccess = ref(false);
const deepseekApiKeyInput = ref('');
const isSavingApiKey = ref(false);
const apiKeyMessage = ref('');

onMounted(async () => {
  if (prefStore.loading) return;
  if (!prefStore.hasDeepseekApiKey) {
    await prefStore.fetchPreferences();
  }
  if (route.query.llm === 'setup') {
    apiKeyMessage.value = '请先完成 DeepSeek API Key 绑定，再继续使用 AI 能力。';
  }
});

const saveSettings = async () => {
  isSaving.value = true;
  // The store handles updates via watchers, but we can call an explicit sync if needed
  // For now, just simulate a save for UI feedback
  await new Promise(resolve => setTimeout(resolve, 500));
  isSaving.value = false;
  showSuccess.value = true;
  setTimeout(() => { showSuccess.value = false; }, 3000);
};

const exportData = () => {
  const data = {
    logs: localStorage.getItem('logStore') || '[]',
    cards: localStorage.getItem('boardStore') || '[]',
    preferences: localStorage.getItem('seichou_settings') || '{}'
  };
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `seichou-data-${new Date().toISOString().split('T')[0]}.json`;
  a.click();
};

const clearData = () => {
  if (confirm('Are you sure you want to clear all local data? This action cannot be undone.')) {
    localStorage.clear();
    window.location.reload();
  }
};

const saveDeepseekApiKey = async () => {
  const trimmed = deepseekApiKeyInput.value.trim();
  if (!trimmed) {
    apiKeyMessage.value = '请输入有效的 DeepSeek API Key。';
    return;
  }

  isSavingApiKey.value = true;
  apiKeyMessage.value = '';
  try {
    await prefStore.updateDeepseekApiKey(trimmed);
    deepseekApiKeyInput.value = '';
    apiKeyMessage.value = 'DeepSeek API Key 已绑定到当前账户。';
  } catch (error: any) {
    apiKeyMessage.value = error?.message || '绑定失败，请稍后重试。';
  } finally {
    isSavingApiKey.value = false;
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto pb-20 animate-fade-in-up">
    <!-- Header -->
    <div class="flex justify-between items-center mb-8 border-b border-border pb-4">
      <div>
        <h2 class="text-2xl font-serif text-ink">{{ t('settings.title') }}</h2>
        <p class="text-xs text-ink-dim mt-1">{{ t('settings.subtitle') }}</p>
      </div>
      <button 
        @click="saveSettings"
        :disabled="isSaving"
        class="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-accent-glow text-bg hover:opacity-90 transition-all text-sm font-bold uppercase tracking-widest disabled:opacity-70 shadow-lg shadow-accent-glow/20"
      >
        <Loader2 v-if="isSaving" class="animate-spin" :size="16" />
        <Save v-else :size="16" />
        {{ t('settings.save') }}
      </button>
    </div>

    <!-- Success Message -->
    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="transform -translate-y-4 opacity-0"
      enter-to-class="transform translate-y-0 opacity-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="showSuccess" class="mb-6 p-4 bg-green-500/10 border border-green-500/30 text-green-400 text-sm rounded-xl flex items-center gap-3">
        <CheckCircle2 :size="18" />
        {{ t('settings.success') }}
      </div>
    </Transition>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- LLM API Setup -->
      <div class="md:col-span-2 bg-card-bg border border-border rounded-2xl overflow-hidden shadow-sm p-6">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-4 flex items-center gap-2">
          <KeyRound :size="14" class="text-accent-glow" /> LLM API Access
        </h3>

        <p class="text-xs text-ink-dim mb-4">
          绑定后会优先使用你账户下的 DeepSeek API Key（按用户生效，可后续覆盖更新）。
        </p>

        <div class="flex flex-col md:flex-row gap-3">
          <input
            v-model="deepseekApiKeyInput"
            type="password"
            placeholder="sk-..."
            class="flex-1 bg-bg border border-border rounded-lg px-3 py-2 text-xs text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none"
          />
          <button
            @click="saveDeepseekApiKey"
            :disabled="isSavingApiKey"
            class="px-4 py-2 rounded-lg bg-accent-glow text-bg text-xs font-bold uppercase tracking-widest hover:opacity-90 transition-colors disabled:opacity-70"
          >
            <Loader2 v-if="isSavingApiKey" class="animate-spin" :size="14" />
            <span v-else>绑定 Key</span>
          </button>
        </div>

        <p class="mt-3 text-[11px]" :class="prefStore.hasDeepseekApiKey ? 'text-green-400' : 'text-amber-400'">
          {{ prefStore.hasDeepseekApiKey ? '当前账户已绑定 DeepSeek API Key。' : '当前账户尚未绑定 DeepSeek API Key。' }}
        </p>
        <p v-if="apiKeyMessage" class="mt-2 text-[11px] text-ink-dim">{{ apiKeyMessage }}</p>
      </div>

      <!-- Appearance -->
      <div class="bg-card-bg border border-border rounded-2xl overflow-hidden shadow-sm p-6">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-6 flex items-center gap-2">
          <Moon :size="14" class="text-accent-glow" /> {{ t('settings.appearance') }}
        </h3>
        
        <div class="space-y-6">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-ink">{{ t('settings.theme') }}</div>
            </div>
            <div class="flex bg-bg border border-border rounded-lg p-1">
              <button 
                @click="prefStore.theme = 'light'"
                class="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium transition-colors"
                :class="prefStore.theme === 'light' ? 'bg-card-bg text-ink shadow-sm' : 'text-ink-dim hover:text-ink'"
              >
                <Sun :size="14" /> {{ t('settings.light') }}
              </button>
              <button 
                @click="prefStore.theme = 'dark'"
                class="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium transition-colors"
                :class="prefStore.theme === 'dark' ? 'bg-card-bg text-ink shadow-sm' : 'text-ink-dim hover:text-ink'"
              >
                <Moon :size="14" /> {{ t('settings.dark') }}
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-ink">{{ t('settings.language') }}</div>
            </div>
            <select 
              v-model="prefStore.language"
              class="bg-bg border border-border rounded-lg px-3 py-1.5 text-xs text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none"
            >
              <option value="en">English</option>
              <option value="zh">简体中文</option>
              <option value="ja">日本語</option>
            </select>
          </div>
        </div>
      </div>

      <!-- AI Personality -->
      <div class="bg-card-bg border border-border rounded-2xl overflow-hidden shadow-sm p-6">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-6 flex items-center gap-2">
          <Brain :size="14" class="text-accent-glow" /> {{ t('settings.aiPersonality') }}
        </h3>
        
        <div class="space-y-6">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-ink">{{ t('settings.mentorStyle') }}</div>
            </div>
            <select 
              v-model="prefStore.aiPersonality"
              class="bg-bg border border-border rounded-lg px-3 py-1.5 text-xs text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none"
            >
              <option value="empathetic">{{ t('settings.empathetic') }}</option>
              <option value="strict">{{ t('settings.strict') }}</option>
              <option value="philosophical">{{ t('settings.philosophical') }}</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Notifications -->
      <div class="bg-card-bg border border-border rounded-2xl overflow-hidden shadow-sm p-6">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-6 flex items-center gap-2">
          <Bell :size="14" class="text-accent-glow" /> {{ t('settings.notifications') }}
        </h3>
        
        <div class="space-y-4">
          <label class="flex items-center justify-between cursor-pointer group">
            <div>
              <div class="text-sm font-medium text-ink group-hover:text-accent-glow transition-colors">{{ t('settings.dailyReminders') }}</div>
            </div>
            <div class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" v-model="prefStore.notifications" class="sr-only peer">
              <div class="w-10 h-5 bg-bg border border-border peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-ink-dim after:border-ink-dim after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-accent-glow peer-checked:after:bg-bg"></div>
            </div>
          </label>
        </div>
      </div>

      <!-- Privacy & Data -->
      <div class="bg-card-bg border border-border rounded-2xl overflow-hidden shadow-sm p-6">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-6 flex items-center gap-2">
          <Shield :size="14" class="text-accent-glow" /> {{ t('settings.privacy') }}
        </h3>
        
        <div class="space-y-6">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium text-ink">{{ t('settings.dataProcessing') }}</div>
            </div>
            <select 
              v-model="prefStore.dataPrivacy"
              class="bg-bg border border-border rounded-lg px-3 py-1.5 text-xs text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none"
            >
              <option value="strict">Strict (Anonymized)</option>
              <option value="standard">Standard</option>
            </select>
          </div>

          <div class="flex gap-3">
            <button 
              @click="exportData"
              class="flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-lg border border-border text-[10px] font-bold uppercase tracking-widest text-ink hover:bg-bg transition-colors"
            >
              <Download :size="14" /> {{ t('settings.export') }}
            </button>
            <button 
              @click="clearData"
              class="flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-lg border border-red-500/30 text-[10px] font-bold uppercase tracking-widest text-red-500 hover:bg-red-500/5 transition-colors"
            >
              <Trash2 :size="14" /> {{ t('settings.clearAll') }}
            </button>
          </div>
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
}
</style>
