<script setup lang="ts">
import { useBoardStore } from '@/store/boardStore';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import TreeOfLogos from '@/components/TreeOfLogos.vue';
import { Leaf, Zap, Heart, TrendingUp, Milestone, Sprout } from 'lucide-vue-next';

const boardStore = useBoardStore();
const { t } = useI18n();

const stats = computed(() => boardStore.stats);

// Calculate percentages for bars
const getPercent = (value: number) => Math.min(100, Math.round((value / stats.value.maxExp) * 100));

const dimensions = computed(() => [
  { id: 'vitality', label: t('dimension.health.label'), value: stats.value.vitalityExp, icon: Zap, color: 'bg-red-400' },
  { id: 'flow', label: t('dimension.mind.label'), value: stats.value.flowExp, icon: Heart, color: 'bg-blue-400' },
  { id: 'spark', label: t('dimension.career.label'), value: stats.value.sparkExp, icon: TrendingUp, color: 'bg-amber-400' },
  { id: 'echo', label: t('dimension.social.label'), value: stats.value.echoExp, icon: Milestone, color: 'bg-green-400' },
]);
</script>

<template>
  <div class="h-full flex flex-col p-6 animate-fade-in-up overflow-y-auto">
    <h2 class="text-2xl font-serif text-ink mb-6">{{ t('nav.treeOfLogos') }}</h2>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 树的展示区 -->
      <div class="h-96 border border-border/50 rounded-3xl overflow-hidden bg-card-bg/30 relative">
        <TreeOfLogos />
      </div>

      <!-- 维度协调区 -->
      <div class="flex flex-col gap-6">
        <div class="bg-card-bg/30 p-6 rounded-3xl border border-border/50">
          <h3 class="text-sm font-bold text-ink uppercase tracking-widest mb-6">{{ t('dashboard.statsOverview') }}</h3>
          <div class="space-y-6">
            <div v-for="dim in dimensions" :key="dim.id" class="group">
              <div class="flex justify-between items-center mb-2">
                <div class="flex items-center gap-2">
                  <component :is="dim.icon" :size="14" :class="dim.color.replace('bg-', 'text-')" />
                  <span class="text-xs text-ink-dim">{{ dim.label }}</span>
                </div>
                <span class="text-sm font-mono text-ink">{{ dim.value }} EXP</span>
              </div>
              <div class="h-2 w-full bg-bg rounded-full overflow-hidden">
                <div :class="`h-full ${dim.color}`" :style="{ width: `${getPercent(dim.value)}%` }" />
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 rounded-3xl border border-accent-glow/20 bg-accent-glow/5">
          <h3 class="text-sm font-bold text-accent-glow uppercase tracking-widest mb-2">{{ t('dashboard.levelUpSummary') }}</h3>
          <p class="text-sm text-ink leading-relaxed">
            Level {{ stats.level }}: {{ t('dashboard.treeGrowthHint') }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
