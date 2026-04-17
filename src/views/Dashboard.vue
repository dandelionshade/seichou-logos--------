<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useBoardStore, type Category, type BoardCard } from '../store/boardStore';
import { soundFx } from '../utils/audio';
import TreeOfLogos from '../components/TreeOfLogos.vue';
import { 
  Plus, Zap, Heart, CheckCircle, Loader2, Sparkles, 
  Kanban, List, Minimize2, Maximize2, Check, X,
  TrendingUp, Milestone, Star, BatteryWarning, Sprout, Award
} from 'lucide-vue-next';

const { t } = useI18n();
const boardStore = useBoardStore();

onMounted(async () => {
  if (boardStore.cards.length === 0) {
    await boardStore.fetchCards();
  }
  await boardStore.fetchStats();
});

const categories = computed(() => [
  { id: 'health', label: t('dimension.health.label'), icon: Zap, sub: t('dimension.health.sub'), color: 'text-red-400' },
  { id: 'mind', label: t('dimension.mind.label'), icon: Heart, sub: t('dimension.mind.sub'), color: 'text-blue-400' },
  { id: 'career', label: t('dimension.career.label'), icon: TrendingUp, sub: t('dimension.career.sub'), color: 'text-amber-400' },
  { id: 'social', label: t('dimension.social.label'), icon: Milestone, sub: t('dimension.social.sub'), color: 'text-green-400' }
]);

// 视图模式切换
const viewMode = ref<'kanban' | 'list'>('kanban');

// 看板折叠状态
const collapsedCategories = ref<Set<Category>>(new Set());
const toggleCollapse = (cat: Category) => {
  if (collapsedCategories.value.has(cat)) {
    collapsedCategories.value.delete(cat);
  } else {
    collapsedCategories.value.add(cat);
  }
};

// 拖拽状态
const draggedCardId = ref<string | null>(null);

const onDragStart = (e: DragEvent, cardId: string) => {
  draggedCardId.value = cardId;
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', cardId);
  }
};

const onDrop = (e: DragEvent, category: Category) => {
  const cardId = draggedCardId.value || e.dataTransfer?.getData('text/plain');
  if (cardId) {
    boardStore.moveCard(cardId, category);
  }
  draggedCardId.value = null;
};

// 完成卡片动画与音效
const completingCards = ref<Set<string>>(new Set());

const playPopSound = () => {
  try {
    const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioContext) return;
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.type = 'sine';
    osc.frequency.setValueAtTime(600, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(1200, ctx.currentTime + 0.1);
    gain.gain.setValueAtTime(0, ctx.currentTime);
    gain.gain.linearRampToValueAtTime(0.2, ctx.currentTime + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.1);
    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + 0.1);
  } catch (e) {
    console.error('Audio playback failed', e);
  }
};

// 卡片结算弹窗逻辑
const settlingCardId = ref<string | null>(null);

const confirmCardSettle = (status: 'done' | 'struggled' | 'composted') => {
  if (!settlingCardId.value) return;
  const id = settlingCardId.value;
  showCardSettleModal.value = false;
  showAddModal.value = false; // Close the unified drawer
  
  completingCards.value.add(id);
  if (status === 'done') playPopSound();
  
  setTimeout(() => {
    if (status === 'done') boardStore.completeCard(id);
    else if (status === 'struggled') boardStore.markStruggled(id);
    else if (status === 'composted') boardStore.markComposted(id);
    
    completingCards.value.delete(id);
    settlingCardId.value = null;
  }, 400);
};

// 快捷操作 (为了兼容列表视图或Hover菜单，可选保留)
const handleComplete = (id: string) => confirmCardSettle('done');
const handleStruggled = (id: string) => confirmCardSettle('struggled');
const handleComposted = (id: string) => confirmCardSettle('composted');

// 新增/编辑卡片模态框
const showAddModal = ref(false);
const editingCardId = ref<string | null>(null);
const newCard = ref({ 
  title: '', 
  description: '', 
  category: 'health' as Category, 
  tags: [] as string[],
  checkpoints: [] as { id: string, title: string, completed: boolean }[]
});
const tagInput = ref('');
const checkpointInput = ref('');

const openAddModal = (category: Category = 'health') => {
  editingCardId.value = null;
  newCard.value = { title: '', description: '', category, tags: [], checkpoints: [] };
  tagInput.value = '';
  checkpointInput.value = '';
  showAddModal.value = true;
};

const openEditModal = (card: BoardCard) => {
  editingCardId.value = card.id;
  settlingCardId.value = card.id; // Also set for settlement
  newCard.value = { 
    title: card.title, 
    description: card.description || '', 
    category: card.category, 
    tags: [...card.tags],
    checkpoints: card.checkpoints ? card.checkpoints.map(c => ({ ...c })) : []
  };
  tagInput.value = '';
  checkpointInput.value = '';
  showAddModal.value = true;
};

const addCheckpoint = () => {
  if (checkpointInput.value.trim()) {
    newCard.value.checkpoints.push({
      id: Date.now().toString(),
      title: checkpointInput.value.trim(),
      completed: false
    });
    checkpointInput.value = '';
    soundFx.click();
  }
};

const removeCheckpoint = (index: number) => {
  newCard.value.checkpoints.splice(index, 1);
};

const toggleCheckpoint = (index: number) => {
  newCard.value.checkpoints[index].completed = !newCard.value.checkpoints[index].completed;
  if (newCard.value.checkpoints[index].completed) {
    soundFx.success();
  } else {
    soundFx.click();
  }
};

const addTag = () => {
  const tag = tagInput.value.trim().replace(/,/g, '');
  if (tag && !newCard.value.tags.includes(tag)) {
    newCard.value.tags.push(tag);
    soundFx.click();
  }
  tagInput.value = '';
};

const removeTag = (index: number) => {
  newCard.value.tags.splice(index, 1);
};

const submitCard = () => {
  if (!newCard.value.title.trim()) return;
  
  const cardData = {
    title: newCard.value.title,
    description: newCard.value.description,
    category: newCard.value.category,
    tags: newCard.value.tags,
    checkpoints: newCard.value.checkpoints
  };

  if (editingCardId.value) {
    boardStore.updateCard(editingCardId.value, cardData);
  } else {
    boardStore.addCard(cardData);
  }
  
  showAddModal.value = false;
};

// 结算逻辑 (AI Settlement)
const isSettling = ref(false);
const showSettleModal = ref(false);
const settlementResult = ref<{ summary: string; vitality: number; flow: number; spark: number; echo: number; resilience: number, newlyUnlockedBadges?: string[] } | null>(null);

const openSettleModal = () => {
  showSettleModal.value = true;
};

const handleSettle = async (feeling: 'smooth' | 'struggled' | 'composted') => {
  showSettleModal.value = false;
  isSettling.value = true;
  settlementResult.value = null;

  try {
    const data = await boardStore.settleExp(feeling);
    
    if (data) {
      // 显示结算结果
      settlementResult.value = {
        summary: data.compassionate_summary,
        vitality: data.vitality_exp || data.physical_exp || 0,
        flow: data.flow_exp || data.mental_exp || 0,
        spark: data.spark_exp || 0,
        echo: data.echo_exp || 0,
        resilience: data.resilience_exp || 0,
        newlyUnlockedBadges: data.newlyUnlockedBadges || []
      };
      
      if (data.newlyUnlockedBadges && data.newlyUnlockedBadges.length > 0) {
        soundFx.unlock();
      } else {
        soundFx.success();
      }
    }
  } catch (error) {
    console.error(error);
    alert("结算失败，请检查网络或 API Key。");
  } finally {
    isSettling.value = false;
  }
};

// 低电量模式
const isLowBatteryLoading = ref(false);
const lowBatteryResult = ref<{ message: string; expGained: number } | null>(null);

const handleLowBattery = async () => {
  isLowBatteryLoading.value = true;
  lowBatteryResult.value = null;
  try {
    const data = await boardStore.triggerLowBattery();
    lowBatteryResult.value = {
      message: data.message,
      expGained: data.expGained
    };
  } catch (error) {
    console.error(error);
    alert("请求失败，请重试。");
  } finally {
    isLowBatteryLoading.value = false;
  }
};

const getCategoryIcon = (catId: Category) => {
  return categories.value.find(d => d.id === catId)?.icon;
};
const getCategoryColor = (catId: Category) => {
  return categories.value.find(d => d.id === catId)?.color;
};
</script>

<template>
  <div class="h-full flex flex-col animate-fade-in-up">
    <!-- 顶部控制栏 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-serif text-ink flex items-center gap-3">
          {{ t('dashboard.title') }}
          <!-- 视图切换器 -->
          <div class="flex bg-bg border border-border rounded-lg p-1">
            <button 
              @click="viewMode = 'kanban'"
              class="p-1.5 rounded-md transition-colors"
              :class="viewMode === 'kanban' ? 'bg-card-bg text-accent-glow shadow-sm' : 'text-ink-dim hover:text-ink'"
              :title="t('dashboard.kanbanView')"
            >
              <Kanban :size="16" />
            </button>
            <button 
              @click="viewMode = 'list'"
              class="p-1.5 rounded-md transition-colors"
              :class="viewMode === 'list' ? 'bg-card-bg text-accent-glow shadow-sm' : 'text-ink-dim hover:text-ink'"
              :title="t('dashboard.listView')"
            >
              <List :size="16" />
            </button>
          </div>
        </h1>
        <p class="text-xs text-ink-dim mt-1">{{ t('dashboard.subtitle') }}</p>
      </div>
      
      <div class="flex items-center gap-3">
        <button 
          @click="handleLowBattery"
          :disabled="isLowBatteryLoading"
          class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-card-bg text-ink-dim hover:text-ink hover:border-ink-dim transition-colors text-sm font-medium disabled:opacity-50"
          title="今天搞砸了 / 不想努力了"
        >
          <Loader2 v-if="isLowBatteryLoading" class="animate-spin" :size="16" />
          <BatteryWarning v-else :size="16" />
          <span class="hidden sm:inline">{{ t('dashboard.lowBattery') }}</span>
        </button>
        <button 
          @click="openAddModal('health')"
          class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-card-bg text-ink hover:border-accent-glow transition-colors text-sm font-medium"
        >
          <Plus :size="16" /> {{ t('dashboard.add') }}
        </button>
        <button 
          @click="openSettleModal"
          :disabled="isSettling"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-accent-glow text-bg hover:bg-accent-glow/90 transition-colors text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Loader2 v-if="isSettling" class="animate-spin" :size="16" />
          <Sparkles v-else :size="16" />
          {{ t('dashboard.settleDay') }}
        </button>
      </div>
    </div>

    <!-- 骨架屏 (Loading Skeleton) -->
    <div v-if="boardStore.loading" class="flex-1 flex flex-col md:flex-row gap-4 overflow-hidden">
      <div v-for="i in 4" :key="i" class="flex-1 min-w-[280px] bg-card-bg/30 border border-border/50 rounded-2xl flex flex-col overflow-hidden animate-pulse">
        <div class="p-4 border-b border-border/30">
          <div class="h-4 bg-border/50 rounded w-24 mb-2"></div>
          <div class="h-2 bg-border/30 rounded w-16"></div>
        </div>
        <div class="flex-1 p-4 space-y-4">
          <div class="h-20 bg-card-bg/50 rounded-xl border border-border/30"></div>
          <div class="h-28 bg-card-bg/50 rounded-xl border border-border/30"></div>
          <div class="h-16 bg-card-bg/50 rounded-xl border border-border/30"></div>
        </div>
      </div>
    </div>
    
    <template v-else>
      <!-- 低电量结果弹窗 -->
      <div v-if="lowBatteryResult" class="mb-6 p-5 bg-card-bg border border-green-500/50 rounded-xl shadow-lg relative overflow-hidden">
      <div class="absolute top-0 left-0 w-1 h-full bg-green-500"></div>
      <h3 class="text-lg font-serif text-green-500 flex items-center gap-2 mb-2">
        <Sprout :size="18" /> {{ t('dashboard.selfCareMode') }}
      </h3>
      <p class="text-sm text-ink leading-relaxed mb-4">{{ lowBatteryResult.message }}</p>
      <div class="flex gap-4 text-xs font-medium">
        <span class="flex items-center gap-1 text-green-500 bg-green-500/10 px-2 py-1 rounded"><Sprout :size="12"/> +{{ lowBatteryResult.expGained }} {{ t('dashboard.resilienceExp') }}</span>
      </div>
      <button @click="lowBatteryResult = null" class="absolute top-4 right-4 text-ink-dim hover:text-ink">✕</button>
    </div>

    <!-- 结算结果弹窗 -->
    <div v-if="settlementResult" class="mb-6 p-5 bg-card-bg border border-accent-glow/50 rounded-xl shadow-lg relative overflow-hidden">
      <div class="absolute top-0 left-0 w-1 h-full bg-accent-glow"></div>
      <h3 class="text-lg font-serif text-accent-glow flex items-center gap-2 mb-2">
        <Sparkles :size="18" /> {{ t('dashboard.levelUpSummary') }}
      </h3>
      <p class="text-sm text-ink leading-relaxed mb-4">{{ settlementResult.summary }}</p>
      <div class="flex flex-wrap gap-4 text-[10px] font-bold uppercase tracking-widest mt-4">
        <span class="flex items-center gap-1 text-red-400 bg-red-400/10 px-2 py-1 rounded"><Zap :size="12"/> +{{ settlementResult.vitality }} {{ t('dashboard.physicalExp') }}</span>
        <span class="flex items-center gap-1 text-blue-400 bg-blue-400/10 px-2 py-1 rounded"><Heart :size="12"/> +{{ settlementResult.flow }} {{ t('dashboard.mentalExp') }}</span>
        <span v-if="settlementResult.spark > 0" class="flex items-center gap-1 text-orange-400 bg-orange-400/10 px-2 py-1 rounded"><Star :size="12"/> +{{ settlementResult.spark }} {{ t('reports.traits.skill') }}</span>
        <span v-if="settlementResult.echo > 0" class="flex items-center gap-1 text-purple-400 bg-purple-400/10 px-2 py-1 rounded"><TrendingUp :size="12"/> +{{ settlementResult.echo }} {{ t('reports.traits.social') }}</span>
        <span v-if="settlementResult.resilience > 0" class="flex items-center gap-1 text-green-500 bg-green-500/10 px-2 py-1 rounded"><Sprout :size="12"/> +{{ settlementResult.resilience }} {{ t('dashboard.resilienceExp') }}</span>
      </div>

      <div v-if="settlementResult.newlyUnlockedBadges && settlementResult.newlyUnlockedBadges.length > 0" class="mt-6 pt-4 border-t border-accent-glow/20">
        <h4 class="text-xs font-bold text-accent-glow uppercase tracking-widest flex items-center gap-2 mb-3">
          <Award :size="14" /> Achievement Unlocked!
        </h4>
        <div class="flex flex-wrap gap-2">
          <span 
            v-for="badge in settlementResult.newlyUnlockedBadges" 
            :key="badge"
            class="bg-accent-glow/10 border border-accent-glow text-accent-glow px-3 py-1.5 rounded-lg text-xs font-semibold shadow-[0_0_15px_-3px_rgba(136,214,108,0.4)] animate-pulse"
          >
            🏆 {{ badge.replace('_', ' ').toUpperCase() }}
          </span>
        </div>
      </div>

      <button @click="settlementResult = null" class="absolute top-4 right-4 text-ink-dim hover:text-ink">✕</button>
    </div>

    <!-- ========================================== -->
    <!-- 看板视图 (Kanban View) -->
    <!-- ========================================== -->
    <div v-if="viewMode === 'kanban'" class="flex-1 flex flex-col md:flex-row gap-4 overflow-x-auto md:overflow-y-hidden overflow-y-auto pb-6">
      
      <template v-for="cat in categories" :key="cat.id">
        <!-- 折叠状态 -->
        <div 
          v-if="collapsedCategories.has(cat.id)" 
          @click="toggleCollapse(cat.id)"
          class="w-12 shrink-0 bg-card-bg/30 border border-border rounded-xl flex flex-col items-center py-4 cursor-pointer hover:bg-card-bg transition-colors h-full"
        >
          <button class="mb-6 text-ink-dim hover:text-ink"><Maximize2 :size="14"/></button>
          <span style="writing-mode: vertical-rl;" class="text-xs font-medium tracking-widest text-ink-dim uppercase">{{ cat.label }}</span>
        </div>

        <!-- 展开状态 -->
        <div 
          v-else 
          class="flex-1 min-w-[280px] flex flex-col h-full transition-all rounded-2xl overflow-hidden border border-border"
        >
          <div 
            class="flex justify-between items-center border-b p-4 transition-colors sticky top-0 bg-bg/90 backdrop-blur-md z-10 border-border"
          >
            <div class="flex items-center gap-2">
              <component :is="cat.icon" :size="16" :class="cat.color" />
              <div class="flex flex-col">
                <h2 class="text-sm font-bold text-ink uppercase tracking-widest leading-tight">{{ cat.label }}</h2>
                <span class="text-[9px] text-ink-dim uppercase tracking-tighter">{{ cat.sub }}</span>
              </div>
            </div>
            <button @click="toggleCollapse(cat.id)" class="text-ink-dim hover:text-ink transition-colors" :title="t('dashboard.kanbanView')">
              <Minimize2 :size="14"/>
            </button>
          </div>

          <!-- 列内容滚动区域 -->
          <div class="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar scroll-smooth">
            <div 
              class="flex flex-col min-h-[400px] h-full bg-card-bg/40 border border-border border-dashed rounded-xl p-4 transition-colors hover:bg-card-bg/60 relative"
              @dragover.prevent
              @dragenter.prevent
              @drop="onDrop($event, cat.id)"
            >
              <div class="flex justify-end mb-4 absolute top-2 right-2">
                <button @click="openAddModal(cat.id)" class="text-ink-dim hover:text-ink transition-colors p-1 rounded-md hover:bg-white/5" :title="t('dashboard.add')">
                  <Plus :size="14" />
                </button>
              </div>

                <!-- 卡片列表 -->
                <div class="flex-1 space-y-3 mt-4">
                  <div 
                    v-for="card in boardStore.getCardsByCategory(cat.id).value" 
                    :key="card.id"
                    draggable="true"
                    @dragstart="onDragStart($event, card.id)"
                    @click="openEditModal(card)"
                    class="bg-bg/50 border border-border rounded-xl p-4 cursor-grab active:cursor-grabbing hover:border-accent-glow/40 transition-all group relative overflow-hidden"
                    :class="{ 
                      'opacity-50 grayscale-[0.5]': completingCards.has(card.id),
                      'ring-1 ring-accent-glow/30 bg-accent-glow/[0.03] shadow-[0_8px_20px_-12px_rgba(136,214,108,0.2)]': card.checkpoints?.length > 0,
                      'border-accent-glow/30': card.checkpoints?.length > 0 && cat.id === 'career'
                    }"
                  >
                    <!-- 为有进度的卡片添加背景装饰 -->
                    <div v-if="card.checkpoints?.length > 0" class="absolute -right-4 -top-4 w-12 h-12 bg-accent-glow/5 rounded-full blur-2xl group-hover:bg-accent-glow/10 transition-colors"></div>
                    <div v-if="card.checkpoints?.length > 0" class="absolute top-0 right-0 p-1">
                       <div class="px-1.5 py-0.5 bg-bg/80 backdrop-blur-md text-accent-glow text-[7px] font-black uppercase tracking-[0.15em] rounded-md border border-accent-glow/20 shadow-sm flex items-center gap-1">
                          <Milestone :size="8" /> {{ t('dashboard.progress') }}
                       </div>
                    </div>

                    <div class="absolute top-0 left-0 w-1 h-full bg-accent-glow opacity-0 group-hover:opacity-100 transition-opacity"></div>
                    
                    <div class="flex items-start gap-1">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-1.5 mb-0.5">
                          <TrendingUp v-if="card.checkpoints?.length > 0 && cat.id === 'career'" :size="10" class="text-accent-glow" />
                          <p class="text-sm text-ink leading-relaxed transition-all duration-300 font-medium" :class="{'line-through text-ink-dim opacity-50': completingCards.has(card.id)}">{{ card.title }}</p>
                        </div>
                      </div>
                    </div>
                
                <!-- 进度条 (针对有子任务的长期目标) -->
                <div v-if="card.checkpoints && card.checkpoints.length > 0" class="mt-4 pl-6 pr-2">
                  <div class="flex justify-between items-center text-[10px] text-ink-dim mb-1.5 px-0.5">
                    <span class="font-medium tracking-tight opacity-70">{{ t('dashboard.progress') }}</span>
                    <span class="font-bold text-accent-glow">{{ Math.round((card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100) }}%</span>
                  </div>
                  <div class="h-2 bg-bg border border-border/40 rounded-full overflow-hidden shadow-inner relative group/bar">
                    <div 
                      class="h-full bg-accent-glow transition-all duration-700 ease-out relative shadow-[0_0_10px_#88D66C66]" 
                      :style="{ width: `${(card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100}%` }"
                    >
                      <!-- 光亮流光效果 -->
                      <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent w-full -translate-x-full animate-[shimmer_2s_infinite]"></div>
                    </div>
                  </div>
                </div>

                <!-- 迷你子任务列表 -->
                <div v-if="card.checkpoints && card.checkpoints.length > 0" class="mt-2 pl-6 space-y-1">
                  <div 
                    v-for="cp in card.checkpoints.slice(0, 3)" 
                    :key="cp.id"
                    class="flex items-center gap-2 group/cp"
                    @click.stop="boardStore.toggleCheckpoint(card.id, cp.id)"
                  >
                    <div 
                      class="w-2.5 h-2.5 rounded-sm border border-ink-dim/50 flex items-center justify-center transition-colors"
                      :class="cp.completed ? 'bg-accent-glow border-accent-glow' : 'group-hover/cp:border-accent-glow'"
                    >
                      <Check v-if="cp.completed" :size="6" class="text-bg" />
                    </div>
                    <span class="text-[10px] text-ink-dim truncate" :class="{ 'line-through opacity-50': cp.completed }">{{ cp.title }}</span>
                  </div>
                  <div v-if="card.checkpoints.length > 3" class="text-[9px] text-ink-dim pl-4 italic">
                    {{ t('dashboard.more', { count: card.checkpoints.length - 3 }) }}
                  </div>
                </div>

                <div v-if="card.tags.length > 0" class="flex flex-wrap gap-1 mt-2 pl-6">
                  <span v-for="tag in card.tags" :key="tag" class="text-[10px] px-1.5 py-0.5 bg-card-bg border border-border rounded text-ink-dim">
                    #{{ tag }}
                  </span>
                </div>
              </div>
              
              <!-- 空状态占位 -->
              <div v-if="boardStore.getCardsByCategory(cat.id).value.length === 0" class="h-full flex items-center justify-center text-xs text-ink-dim/50 italic pointer-events-none mt-10">
                {{ t('dashboard.noEntries') }}
              </div>
            </div>
          </div>
        </div>
        </div>
      </template>

    </div>

    <!-- ========================================== -->
    <!-- 列表视图 (List View) -->
    <!-- ========================================== -->
    <div v-else class="flex-1 overflow-y-auto pr-2 space-y-8">
      <div v-for="cat in categories" :key="cat.id" class="space-y-4">
        <h3 class="text-lg font-serif text-ink flex items-center gap-3 border-b border-border pb-2">
          <component :is="cat.icon" :size="20" :class="cat.color" />
          <div class="flex flex-col">
            <span class="leading-tight">{{ cat.label }}</span>
            <span class="text-[10px] text-ink-dim uppercase tracking-widest">{{ cat.sub }}</span>
          </div>
        </h3>
        
        <div v-if="boardStore.getCardsByCategory(cat.id).value.length === 0" class="text-xs text-ink-dim italic py-2">
          {{ t('dashboard.noEntries') }}
        </div>

        <div 
          v-for="card in boardStore.getCardsByCategory(cat.id).value" 
          :key="card.id"
          @click="openEditModal(card)"
          class="flex items-center gap-4 bg-card-bg p-3 rounded-xl border border-border hover:border-accent-glow/50 transition-colors cursor-pointer group relative overflow-hidden"
        >
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-0.5">
              <TrendingUp v-if="card.checkpoints?.length > 0 && cat.id === 'career'" :size="12" class="text-accent-glow shrink-0" />
              <h4 class="text-sm text-ink truncate transition-all duration-300 font-medium" :class="{'line-through text-ink-dim opacity-50': completingCards.has(card.id)}">{{ card.title }}</h4>
              <div v-if="card.checkpoints?.length > 0" class="px-1.5 py-0.5 bg-accent-glow/10 text-accent-glow text-[7px] font-bold uppercase tracking-widest rounded-md border border-accent-glow/20 flex items-center gap-1">
                <Milestone :size="8" /> {{ t('dashboard.progress') }}
              </div>
            </div>
            <p v-if="card.description" class="text-xs text-ink-dim truncate mt-0 transition-all duration-300" :class="{'opacity-50': completingCards.has(card.id)}">{{ card.description }}</p>
            
            <!-- 列表视图进度条 -->
            <div v-if="card.checkpoints && card.checkpoints.length > 0" class="mt-3 w-40">
              <div class="flex justify-between items-center text-[9px] text-ink-dim mb-1 px-0.5">
                <span>{{ Math.round((card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100) }}%</span>
              </div>
              <div class="h-1.5 bg-bg rounded-full overflow-hidden border border-border/30 shadow-inner">
                <div 
                  class="h-full bg-accent-glow transition-all duration-700 ease-out shadow-[0_0_8px_#88D66C4D]" 
                  :style="{ width: `${(card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100}%` }"
                ></div>
              </div>
            </div>
          </div>
          
          <div class="flex items-center gap-3 shrink-0">
            <div v-if="card.tags.length > 0" class="hidden sm:flex gap-1">
              <span v-for="tag in card.tags.slice(0, 2)" :key="tag" class="text-[10px] px-1.5 py-0.5 bg-bg border border-border rounded text-ink-dim">
                #{{ tag }}
              </span>
              <span v-if="card.tags.length > 2" class="text-[10px] text-ink-dim">...</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- 侧边栏抽屉 (Drawer) 替代原来的模态框 -->
    <!-- ========================================== -->
    <div v-if="showAddModal" class="fixed inset-0 z-50 flex justify-end">
      <!-- 背景遮罩 (使用显式的 rgba 确保透明度) -->
      <div 
        class="absolute inset-0 transition-opacity"
        style="background-color: rgba(0, 0, 0, 0.4);"
        @click="showAddModal = false"
      ></div>
      
      <!-- 抽屉面板 -->
      <div class="relative w-full max-w-md h-full bg-card-bg border-l border-border shadow-2xl flex flex-col animate-slide-in-right">
        <!-- 抽屉头部 -->
        <div class="flex items-center justify-between p-6 border-b border-border">
          <h3 class="text-lg font-serif">{{ editingCardId ? t('edit.title') : t('dashboard.add') }}</h3>
          <button @click="showAddModal = false" class="text-ink-dim hover:text-ink transition-colors">
            <X :size="20" />
          </button>
        </div>
        
        <!-- 抽屉内容 -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6">
          <div>
            <label class="block text-xs text-ink-dim mb-1.5">{{ t('edit.title') }}</label>
            <input v-model="newCard.title" type="text" class="w-full bg-bg border border-border rounded-lg px-3 py-2.5 text-sm text-ink focus:border-accent-glow outline-none transition-colors" :placeholder="t('edit.title')">
          </div>
          <div>
            <label class="block text-xs text-ink-dim mb-1.5">{{ t('edit.description') }}</label>
            <textarea v-model="newCard.description" class="w-full h-32 bg-bg border border-border rounded-lg px-3 py-2.5 text-sm text-ink focus:border-accent-glow outline-none resize-none transition-colors" :placeholder="t('edit.description')"></textarea>
          </div>
          <div class="space-y-4">
            <div>
              <label class="block text-xs text-ink-dim mb-1.5">{{ t('edit.category') }}</label>
              <select v-model="newCard.category" class="w-full bg-bg border border-border rounded-lg px-3 py-2.5 text-sm text-ink focus:border-accent-glow outline-none transition-colors">
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.label }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-xs text-ink-dim mb-1.5">{{ t('edit.tags') }}</label>
            <div class="flex flex-wrap gap-2 mb-2">
              <span 
                v-for="(tag, index) in newCard.tags" 
                :key="index"
                class="flex items-center gap-1 px-2 py-1 bg-accent-glow/10 border border-accent-glow/30 rounded text-xs text-accent-glow"
              >
                #{{ tag }}
                <button @click="removeTag(index)" class="hover:text-ink transition-colors">
                  <X :size="12" />
                </button>
              </span>
            </div>
            <div class="relative">
              <input 
                v-model="tagInput" 
                type="text" 
                class="w-full bg-bg border border-border rounded-lg px-3 py-2.5 text-sm text-ink focus:border-accent-glow outline-none transition-colors pr-10" 
                :placeholder="t('edit.tags')" 
                @keydown.enter.prevent="addTag"
                @keydown.comma.prevent="addTag"
                @blur="addTag"
              >
              <button 
                @click="addTag"
                class="absolute right-2 top-1/2 -translate-y-1/2 text-ink-dim hover:text-accent-glow transition-colors"
                :title="t('edit.tags')"
              >
                <Plus :size="18" />
              </button>
            </div>
          </div>

          <!-- Checkpoints (Sub-tasks) -->
          <div>
            <label class="block text-xs text-ink-dim mb-1.5">{{ t('edit.checkpoints') }}</label>
            <div class="space-y-2 mb-3">
              <div 
                v-for="(cp, index) in newCard.checkpoints" 
                :key="cp.id"
                class="flex items-center gap-3 bg-bg border border-border rounded-lg p-2 group"
              >
                <button 
                  @click="toggleCheckpoint(index)"
                  class="w-4 h-4 rounded border border-ink-dim flex items-center justify-center transition-colors"
                  :class="cp.completed ? 'bg-accent-glow border-accent-glow' : 'hover:border-accent-glow'"
                >
                  <Check v-if="cp.completed" :size="10" class="text-bg" />
                </button>
                <span class="flex-1 text-sm text-ink" :class="{ 'line-through text-ink-dim': cp.completed }">{{ cp.title }}</span>
                <button @click="removeCheckpoint(index)" class="text-ink-dim hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all">
                  <X :size="14" />
                </button>
              </div>
            </div>
            <div class="relative">
              <input 
                v-model="checkpointInput" 
                type="text" 
                class="w-full bg-bg border border-border rounded-lg px-3 py-2.5 text-sm text-ink focus:border-accent-glow outline-none transition-colors pr-10" 
                :placeholder="t('edit.addCheckpoint')" 
                @keydown.enter.prevent="addCheckpoint"
              >
              <button 
                @click="addCheckpoint"
                class="absolute right-2 top-1/2 -translate-y-1/2 text-ink-dim hover:text-accent-glow transition-colors"
                :title="t('edit.addCheckpoint')"
              >
                <Plus :size="18" />
              </button>
            </div>
          </div>

          <!-- 记录体验 (仅在编辑现有任务时显示) -->
          <div v-if="editingCardId" class="pt-6 border-t border-border">
            <label class="block text-xs text-ink-dim mb-4 uppercase tracking-widest">{{ t('experience.record') }}</label>
            <div class="grid grid-cols-1 gap-3">
              <button 
                @click="confirmCardSettle('done')" 
                class="flex items-center justify-between p-4 rounded-xl border border-border bg-bg hover:border-accent-glow hover:bg-accent-glow/5 transition-all group"
              >
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-accent-glow/10 flex items-center justify-center text-accent-glow group-hover:scale-110 transition-transform">
                    <CheckCircle :size="16" />
                  </div>
                  <span class="font-medium text-ink">{{ t('experience.smooth') }}</span>
                </div>
                <span class="text-[10px] text-ink-dim uppercase tracking-wider">{{ t('dashboard.standardSettle') }}</span>
              </button>

              <button 
                @click="confirmCardSettle('struggled')" 
                class="flex items-center justify-between p-4 rounded-xl border border-border bg-bg hover:border-blue-400/50 hover:bg-blue-400/5 transition-all group"
              >
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-blue-400/10 flex items-center justify-center text-blue-400 group-hover:scale-110 transition-transform">
                    🧗
                  </div>
                  <span class="font-medium text-ink">{{ t('experience.struggled') }}</span>
                </div>
                <span class="text-[10px] text-blue-400/70 uppercase tracking-wider">{{ t('dashboard.persistenceAward') }}</span>
              </button>

              <button 
                @click="confirmCardSettle('composted')" 
                class="flex items-center justify-between p-4 rounded-xl border border-border bg-bg hover:border-green-500/50 hover:bg-green-500/5 transition-all group"
              >
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-green-500/10 flex items-center justify-center text-green-500 group-hover:scale-110 transition-transform">
                    🌱
                  </div>
                  <span class="font-medium text-ink">{{ t('experience.composted') }}</span>
                </div>
                <span class="text-[10px] text-green-500/70 uppercase tracking-wider">{{ t('dashboard.acceptanceAward') }}</span>
              </button>
            </div>
          </div>
        </div>
        
        <!-- 抽屉底部 -->
        <div class="p-6 border-t border-border bg-card-bg/50 flex justify-end gap-3">
          <button @click="showAddModal = false" class="px-5 py-2.5 text-sm text-ink-dim hover:text-ink transition-colors">{{ t('edit.cancel') }}</button>
          <button @click="submitCard" class="px-5 py-2.5 text-sm bg-accent-glow text-bg rounded-lg font-medium hover:bg-accent-glow/90 transition-colors shadow-lg shadow-accent-glow/20">
            {{ editingCardId ? t('edit.save') : t('edit.addToKanban') }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- 结算白天抽屉 (Settle Day Drawer) -->
    <!-- ========================================== -->
    </template>
  </div>
</template>

<style scoped>
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in-up {
  animation: fadeInUp 0.4s ease-out forwards;
}

@keyframes slideInRight {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.animate-slide-in-right {
  animation: slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
</style>
