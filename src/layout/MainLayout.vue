<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useBoardStore } from '../store/boardStore';
import { usePreferenceStore } from '../store/preferenceStore';
import { useAuthStore } from '../store/authStore';
import { useToastStore } from '../store/toastStore';
import { soundFx } from '../utils/audio';
import TreeOfLogos from '../components/TreeOfLogos.vue';
import {
  LayoutDashboard, BookOpen, TrendingUp, Settings,
  ChevronRight, Globe, User, Bell, Zap, Heart, MessageSquare, Sprout, Milestone, Volume2, VolumeX, RefreshCw
} from 'lucide-vue-next';

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const goToGarden = () => router.push('/garden');
const boardStore = useBoardStore();
const preferenceStore = usePreferenceStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

type BackendOrigin = 'node' | 'spring' | 'unknown';
const BACKEND_ORIGIN_CACHE_KEY = 'backend_origin_indicator_v1';
const backendOrigin = ref<BackendOrigin>('unknown');
const showBackendIndicator = computed(() => import.meta.env.DEV);

const backendOriginLabel = computed(() => {
  if (backendOrigin.value === 'node') return t('header.backendNode');
  if (backendOrigin.value === 'spring') return t('header.backendSpring');
  return t('header.backendUnknown');
});

const backendOriginClass = computed(() => {
  if (backendOrigin.value === 'node') return 'text-blue-400';
  if (backendOrigin.value === 'spring') return 'text-green-400';
  return 'text-ink-dim';
});

const deleteRouteLabel = computed(() => {
  if (boardStore.deleteRouteStatus === 'available') return t('header.deleteRouteAvailable');
  if (boardStore.deleteRouteStatus === 'unavailable') return t('header.deleteRouteUnavailable');
  return t('header.deleteRouteUnknown');
});

const deleteRouteClass = computed(() => {
  if (boardStore.deleteRouteStatus === 'available') return 'text-green-400';
  if (boardStore.deleteRouteStatus === 'unavailable') return 'text-red-400';
  return 'text-ink-dim';
});

const deleteModeLabel = computed(() => {
  return boardStore.localOnlyDeleteMode
    ? t('header.deleteModeLocalOnly')
    : t('header.deleteModeNormal');
});

const detectBackendOrigin = async () => {
  if (!import.meta.env.DEV) return;

  const cached = sessionStorage.getItem(BACKEND_ORIGIN_CACHE_KEY);
  if (cached === 'node' || cached === 'spring' || cached === 'unknown') {
    backendOrigin.value = cached;
    return;
  }

  try {
    const token = localStorage.getItem('token');
    const response = await fetch('/api/board/cards/__backend_probe__', {
      method: 'DELETE',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    const contentType = response.headers.get('content-type') || '';
    const poweredBy = (response.headers.get('x-powered-by') || '').toLowerCase();

    if (response.status === 400 || response.status === 401 || response.status === 403) {
      backendOrigin.value = 'spring';
    } else if (response.status === 204) {
      backendOrigin.value = 'node';
    } else if (response.status === 404) {
      backendOrigin.value = contentType.includes('application/json') || poweredBy.includes('express')
        ? 'node'
        : 'unknown';
    } else {
      backendOrigin.value = 'unknown';
    }
  } catch {
    backendOrigin.value = 'unknown';
  } finally {
    sessionStorage.setItem(BACKEND_ORIGIN_CACHE_KEY, backendOrigin.value);
  }
};

const retryingDeleteSync = ref(false);

const retryPermanentDelete = async () => {
  if (retryingDeleteSync.value) return;
  if (boardStore.deletedCardCount <= 0) {
    toastStore.addToast(t('header.retryDeleteNothing'), 'info');
    return;
  }

  retryingDeleteSync.value = true;
  try {
    const result = await boardStore.retryPermanentDelete();
    if (!result.apiReady) {
      toastStore.addToast(t('header.retryDeleteNeedBackend'), 'warning');
      return;
    }

    if (result.removed > 0) {
      toastStore.addToast(t('header.retryDeleteSuccess', { count: result.removed }), 'success');
    } else {
      toastStore.addToast(t('header.retryDeleteNothing'), 'info');
    }
  } catch (e) {
    console.error('Retry permanent delete failed', e);
    toastStore.addToast(t('header.retryDeleteFailed'), 'error');
  } finally {
    retryingDeleteSync.value = false;
  }
};

const soundEnabled = ref(true);
const toggleSound = () => {
  soundEnabled.value = !soundEnabled.value;
  soundFx.setEnabled(soundEnabled.value);
  if (soundEnabled.value) soundFx.click();
};

// 语言切换逻辑
const changeLanguage = (lang: string) => { 
  preferenceStore.language = lang;
  locale.value = lang;
};
const showLangMenu = ref(false);
const toggleLangMenu = () => { showLangMenu.value = !showLangMenu.value; };
const closeLangMenu = (e: Event) => {
  if (!(e.target as HTMLElement).closest('.lang-selector')) {
    showLangMenu.value = false;
  }
};

onMounted(async () => {
  document.addEventListener('click', closeLangMenu);

  await detectBackendOrigin();
  await boardStore.refreshDeleteRouteHealth();

  // Fetch all initial data
  if (authStore.isAuthenticated) {
    await Promise.all([
      authStore.fetchMe(),
      preferenceStore.fetchPreferences(),
      boardStore.fetchCards(),
      boardStore.fetchStats()
    ]);
  }
});

// 导航项定义
const navItems = [
  { name: 'nav.dailyLogs', path: '/', icon: LayoutDashboard },
  { name: 'nav.wisdomChat', path: '/chat', icon: MessageSquare },
  { name: 'nav.growthAssets', path: '/history', icon: TrendingUp },
  { name: 'nav.aiReports', path: '/reports', icon: BookOpen },
  { name: 'nav.settings', path: '/settings', icon: Settings },
];

// 计算经验条百分比
const physicalPercent = computed(() => Math.min(100, (boardStore.stats.vitalityExp / boardStore.stats.maxExp) * 100));
const mentalPercent = computed(() => Math.min(100, (boardStore.stats.flowExp / boardStore.stats.maxExp) * 100));
const sparkPercent = computed(() => Math.min(100, ((boardStore.stats.sparkExp || 0) / boardStore.stats.maxExp) * 100));
const echoPercent = computed(() => Math.min(100, ((boardStore.stats.echoExp || 0) / boardStore.stats.maxExp) * 100));
const resiliencePercent = computed(() => Math.min(100, ((boardStore.stats.resilienceExp || 0) / boardStore.stats.maxExp) * 100));
</script>

<template>
  <div class="flex h-screen bg-bg text-ink font-sans selection:bg-accent-glow/30 overflow-hidden">
    
    <!-- 左侧边栏 (Sidebar) - 仅桌面端显示 -->
    <aside class="hidden lg:flex w-64 border-r border-border p-6 flex-col gap-10 shrink-0 bg-bg z-40">
      <div class="logo font-serif text-xl font-bold tracking-wider border-b border-border pb-5">
        SEICHOU-LOGOS
        <span class="block mt-1 text-xs font-normal opacity-60">成長の軌跡</span>
      </div>

      <nav class="flex flex-col gap-3">
        <router-link 
          v-for="item in navItems" 
          :key="item.path"
          :to="item.path" 
          class="flex items-center gap-3 py-2 cursor-pointer transition-all group" 
          :class="route.path === item.path ? 'text-ink border-l-2 border-accent-glow pl-3 -ml-3' : 'text-ink-dim hover:text-ink'"
        >
          <div :class="route.path === item.path ? 'text-accent-glow' : 'group-hover:text-ink'">
            <component :is="item.icon" :size="18" />
          </div>
          <div class="flex flex-col">
            <span class="text-[11px] uppercase tracking-widest font-medium">{{ t(item.name) }}</span>
          </div>
          <ChevronRight v-if="route.path === item.path" :size="14" class="ml-auto text-accent-glow/50" />
        </router-link>
      </nav>

      <div class="mt-auto h-48 border-t border-border pt-6">
        <TreeOfLogos interactive @click="goToGarden" />
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="flex-1 flex flex-col h-screen overflow-hidden relative">
      
      <!-- 顶部 Header (Top Navigation) -->
      <header class="h-16 lg:h-20 grid grid-cols-3 items-center border-b border-border px-4 lg:px-10 shrink-0 bg-bg/80 backdrop-blur-md z-20">
        <!-- 移动端 Logo (Left) -->
        <div class="flex items-center justify-start">
          <div class="lg:hidden font-serif text-lg font-bold tracking-wider">
            SEICHOU
          </div>
        </div>

        <!-- 游戏化状态条 (Gamification EXP Bars - Center) -->
        <div class="flex items-center justify-center gap-6">
          <div class="hidden lg:flex items-center justify-center w-10 h-10 rounded-full bg-card-bg border border-border text-accent-glow font-bold font-serif shadow-[0_0_15px_#88D66C33]">
            L{{ boardStore.stats.level }}
          </div>
          
          <!-- Unified 4 Energies matching the Board -->
          <div class="flex flex-col gap-1 w-36 sm:w-48">
            <!-- Vitality (Health) -->
            <div class="flex items-center gap-2 group cursor-help" :title="t('dimension.health.label')">
              <Zap :size="10" class="text-red-400 shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
              <div class="flex-1 h-1.5 bg-card-bg rounded-full overflow-hidden border border-border/50">
                <div class="h-full bg-red-400 transition-all duration-700 ease-out shadow-[0_0_8px_#F87171]" :style="{ width: `${physicalPercent}%` }"></div>
              </div>
            </div>
            <!-- Flow (Mind) -->
            <div class="flex items-center gap-2 group cursor-help" :title="t('dimension.mind.label')">
              <Heart :size="10" class="text-blue-400 shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
              <div class="flex-1 h-1.5 bg-card-bg rounded-full overflow-hidden border border-border/50">
                <div class="h-full bg-blue-400 transition-all duration-700 ease-out shadow-[0_0_8px_#60A5FA]" :style="{ width: `${mentalPercent}%` }"></div>
              </div>
            </div>
            <!-- Spark (Career/Skill) -->
            <div class="flex items-center gap-2 group cursor-help" :title="t('dimension.career.label')">
              <TrendingUp :size="10" class="text-amber-400 shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
              <div class="flex-1 h-1.5 bg-card-bg rounded-full overflow-hidden border border-border/50">
                <div class="h-full bg-amber-400 transition-all duration-700 ease-out shadow-[0_0_8px_#FBBF24]" :style="{ width: `${sparkPercent}%` }"></div>
              </div>
            </div>
            <!-- Echo (Social) -->
            <div class="flex items-center gap-2 group cursor-help" :title="t('dimension.social.label')">
              <Milestone :size="10" class="text-green-400 shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
              <div class="flex-1 h-1.5 bg-card-bg rounded-full overflow-hidden border border-border/50">
                <div class="h-full bg-green-400 transition-all duration-700 ease-out shadow-[0_0_8px_#4ADE80]" :style="{ width: `${echoPercent}%` }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧操作区 (Right) -->
        <div class="flex gap-4 sm:gap-6 text-[10px] uppercase tracking-widest text-ink-dim items-center justify-end">
          <div
            v-if="showBackendIndicator"
            class="hidden lg:flex flex-col gap-1 px-2.5 py-1.5 rounded-lg border border-border/60 bg-card-bg/60 normal-case tracking-normal text-[10px] leading-tight"
            :title="t('header.deleteHealth')"
          >
            <div class="text-ink-dim/80 uppercase tracking-wider text-[9px]">{{ t('header.deleteHealth') }}</div>
            <div class="flex items-center gap-1.5">
              <span class="text-ink-dim">{{ t('header.deleteRoute') }}:</span>
              <span :class="deleteRouteClass">{{ deleteRouteLabel }}</span>
            </div>
            <div class="flex items-center gap-1.5">
              <span class="text-ink-dim">{{ t('header.deleteMode') }}:</span>
              <span>{{ deleteModeLabel }}</span>
            </div>
            <div class="flex items-center gap-1.5">
              <span class="text-ink-dim">{{ t('header.deleteRetryCount') }}:</span>
              <span class="text-vision">{{ boardStore.deletedCardCount }}</span>
            </div>
          </div>
          <button
            v-if="showBackendIndicator && boardStore.deletedCardCount > 0"
            type="button"
            class="hidden sm:flex items-center gap-1.5 px-2 py-1 rounded border border-vision/40 text-vision hover:border-vision hover:bg-vision/10 transition-colors"
            :disabled="retryingDeleteSync"
            :title="t('header.retryDeletePermanent', { count: boardStore.deletedCardCount })"
            @click="retryPermanentDelete"
          >
            <RefreshCw :size="12" :class="retryingDeleteSync ? 'animate-spin' : ''" />
            <span>{{ t('header.retryDeletePermanent', { count: boardStore.deletedCardCount }) }}</span>
          </button>
          <span
            v-if="showBackendIndicator"
            class="hidden sm:flex items-center gap-1.5"
            :title="t('header.backendSource')"
          >
            <div class="w-1.5 h-1.5 rounded-full" :class="backendOriginClass === 'text-ink-dim' ? 'bg-ink-dim' : (backendOriginClass === 'text-blue-400' ? 'bg-blue-400' : 'bg-green-400')" />
            <span>{{ t('header.backendSource') }}: </span>
            <span :class="backendOriginClass">{{ backendOriginLabel }}</span>
          </span>
          <span class="hidden sm:flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-accent-glow" /> {{ t('header.system') }}</span>
          <span class="hidden sm:inline">{{ t('header.database') }}</span>
          
          <!-- 语言切换器 -->
          <div class="relative lang-selector">
            <button @click="toggleLangMenu" class="flex items-center gap-1.5 hover:text-ink transition-colors px-2 py-1 rounded border border-border/50 hover:border-border">
              <Globe :size="12" />
              <span>{{ locale.toUpperCase() }}</span>
            </button>
            <div v-if="showLangMenu" class="absolute top-full right-0 mt-1 bg-card-bg border border-border rounded shadow-lg py-1 z-10 min-w-[100px]">
              <button @click="changeLanguage('en')" class="block w-full text-left px-3 py-1.5 hover:bg-white/5 transition-colors" :class="{ 'text-accent-glow': locale === 'en' }">English</button>
              <button @click="changeLanguage('ja')" class="block w-full text-left px-3 py-1.5 hover:bg-white/5 transition-colors" :class="{ 'text-accent-glow': locale === 'ja' }">日本語</button>
              <button @click="changeLanguage('zh')" class="block w-full text-left px-3 py-1.5 hover:bg-white/5 transition-colors" :class="{ 'text-accent-glow': locale === 'zh' }">中文</button>
            </div>
          </div>

          <!-- 音效控制 -->
          <button @click="toggleSound" class="hover:text-ink transition-colors hidden sm:block delay-75" title="Toggle Sound">
            <Volume2 v-if="soundEnabled" :size="16" />
            <VolumeX v-else :size="16" class="text-ink-dim/50" />
          </button>

          <!-- 通知图标 -->
          <button class="hover:text-ink transition-colors relative hidden sm:block">
            <Bell :size="16" />
            <span class="absolute -top-1 -right-1 w-1.5 h-1.5 rounded-full bg-accent-glow"></span>
          </button>

          <!-- 个人主页入口 -->
          <router-link to="/profile" class="hover:text-accent-glow transition-colors" :class="route.path === '/profile' ? 'text-accent-glow' : ''">
            <User :size="18" />
          </router-link>
        </div>
      </header>

      <!-- 滚动主内容 -->
      <main class="flex-1 overflow-y-auto px-4 lg:px-10 pb-24 lg:pb-10 pt-6 scroll-smooth">
        <!-- 移动端显示的小树 -->
        <div class="lg:hidden h-24 border border-border/50 rounded-2xl overflow-hidden mb-6 relative shadow-sm">
          <TreeOfLogos interactive @click="goToGarden" />
        </div>
        
        <div class="max-w-6xl mx-auto h-full">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>

      <!-- 底部导航栏 (Bottom Navigation) - 仅移动端显示 -->
      <nav class="lg:hidden fixed bottom-0 w-full h-16 bg-bg/95 backdrop-blur-md border-t border-border flex justify-around items-center px-2 pb-safe z-30">
        <router-link 
          v-for="item in navItems" 
          :key="item.path" 
          :to="item.path"
          class="flex flex-col items-center justify-center w-16 h-full gap-1 transition-colors"
          :class="route.path === item.path ? 'text-accent-glow' : 'text-ink-dim hover:text-ink'"
        >
          <component :is="item.icon" :size="20" />
          <span class="text-[9px] font-medium truncate w-full text-center uppercase tracking-wider">{{ t(item.name) }}</span>
        </router-link>
      </nav>

    </div>
  </div>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(5px);
}

/* 适配 iOS 底部安全区 */
.pb-safe {
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
