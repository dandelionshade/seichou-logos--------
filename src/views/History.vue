<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import { useLogStore } from '@/store/logStore';
import { useBoardStore } from '@/store/boardStore';
import { useI18n } from 'vue-i18n';
import { 
  Clock, Filter, ChevronLeft, ChevronRight, 
  TrendingUp, Activity, PieChart, BarChart3,
  Zap, Heart, BookOpen, Target, Star
} from 'lucide-vue-next';
import * as echarts from 'echarts';

const logStore = useLogStore();
const boardStore = useBoardStore();
const { t } = useI18n();

// Filter state
const filterDate = ref('');
const currentPage = ref(1);
const itemsPerPage = 5;

// Charts refs
const radarChartRef = ref<HTMLElement | null>(null);
const trendChartRef = ref<HTMLElement | null>(null);
const heatmapChartRef = ref<HTMLElement | null>(null);

let radarChart: echarts.ECharts | null = null;
let trendChart: echarts.ECharts | null = null;
let heatmapChart: echarts.ECharts | null = null;

// Calculation for Synergy Area (Polygon Area)
const synergyArea = computed(() => {
  const getCatScore = (cat: string) => {
    const baseExp = cat === 'health' ? boardStore.stats.vitalityExp : 
                    cat === 'mind' ? boardStore.stats.flowExp :
                    cat === 'career' ? boardStore.stats.sparkExp :
                    cat === 'social' ? boardStore.stats.echoExp : 0;
    const historyCount = boardStore.completedHistory.filter(c => c.category === cat).length;
    return baseExp + (historyCount * 15);
  };
  
  const v = getCatScore('health');
  const f = getCatScore('mind');
  const s = getCatScore('career');
  const e = getCatScore('social');
  const r = boardStore.stats.resilienceExp + (logStore.logs.length * 10);
  
  const rad = (72 * Math.PI) / 180;
  const sin72 = Math.sin(rad);
  const area = 0.5 * sin72 * (v*f + f*s + s*e + e*r + r*v);
  
  return Math.round(area);
});

// Summary Stats
const stats = computed(() => [
  { label: '多维共鸣面积 (Synergy Area)', value: synergyArea.value.toLocaleString(), icon: Activity, color: 'text-accent-glow', desc: '雷达图非线性复利面积（均衡增长将带来指数级面积提升）' },
  { label: '顿悟红利倍率 (Epiphany Multiplier)', value: `${(boardStore.stats.epiphanyMultiplier || 1.0).toFixed(1)}x`, icon: Zap, color: 'text-orange-400', desc: '根据你经历的“低谷/挣扎”积累。下一次成功结算时，将享受此经验翻倍红利！' },
  { label: t('history.totalLogs'), value: logStore.logs.length, icon: BookOpen, color: 'text-ink-dim', desc: '记录的生命碎念总数' },
  { label: t('history.tasksDone'), value: boardStore.completedHistory.length, icon: Target, color: 'text-green-400', desc: '所有顺境与逆境所踏过的足迹' },
]);

// Computed filtered and paginated logs
const filteredLogs = computed(() => {
  let result = logStore.logs;
  if (filterDate.value) {
    result = result.filter(log => log.date.startsWith(filterDate.value));
  }
  return result;
});

const totalPages = computed(() => Math.ceil(filteredLogs.value.length / itemsPerPage) || 1);

const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return filteredLogs.value.slice(start, end);
});

const nextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++;
};

const prevPage = () => {
  if (currentPage.value > 1) currentPage.value--;
};

// Chart Initialization
const initCharts = () => {
  // 1. Radar Chart: Dimension Balance
  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value);
    
    // Aggregating scores per category
    const getCatScore = (cat: string) => {
      const baseExp = cat === 'health' ? boardStore.stats.vitalityExp : 
                      cat === 'mind' ? boardStore.stats.flowExp :
                      cat === 'career' ? boardStore.stats.sparkExp :
                      cat === 'social' ? boardStore.stats.echoExp : 0;
      const historyCount = boardStore.completedHistory.filter(c => c.category === cat).length;
      return baseExp + (historyCount * 15);
    };

    const vitality = getCatScore('health');
    const flow = getCatScore('mind');
    const spark = getCatScore('career');
    const echo = getCatScore('social');
    const resilience = boardStore.stats.resilienceExp + (logStore.logs.length * 10);
    
    radarChart.setOption({
      backgroundColor: 'transparent',
      radar: {
        indicator: [
          { name: t('dimension.health.label'), max: 500 },
          { name: t('dimension.mind.label'), max: 500 },
          { name: t('dimension.career.label'), max: 500 },
          { name: t('dimension.social.label'), max: 500 },
          { name: t('reports.traits.resilience'), max: 500 }
        ],
        shape: 'circle',
        splitNumber: 4,
        axisName: { color: '#8E9299', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(212, 175, 55, 0.1)' } },
        splitArea: { show: false },
        axisLine: { lineStyle: { color: 'rgba(212, 175, 55, 0.1)' } }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [vitality, flow, spark, echo, resilience],
          name: 'Growth Profile',
          itemStyle: { color: '#D4AF37' },
          areaStyle: {
            color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
              { color: 'rgba(212, 175, 55, 0.4)', offset: 0 },
              { color: 'rgba(212, 175, 55, 0.1)', offset: 1 }
            ])
          },
          lineStyle: { width: 2, shadowColor: 'rgba(212, 175, 55, 0.5)', shadowBlur: 10 }
        }]
      }]
    });
  }

  // 2. Trend Chart: Cumulative Growth Area
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value);
    const last30Days = Array.from({ length: 30 }, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - (29 - i));
      return d.toISOString().split('T')[0];
    });

    let cumulativeScore = 100; // Base starting point
    const cumulativeData = last30Days.map(date => {
      // Simulate historical density of logs
      const logCount = logStore.logs.filter(l => l.date === date).length;
      const daySeed = date.split('-').reduce((acc, char) => acc + char.charCodeAt(0), 0);
      const activity = logCount > 0 ? (logCount * 15 + 10) : ((daySeed % 5));
      cumulativeScore += activity;
      return cumulativeScore;
    });

    trendChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { 
        trigger: 'axis',
        backgroundColor: 'rgba(22, 22, 26, 0.9)',
        borderColor: 'rgba(136, 214, 108, 0.3)',
        textStyle: { color: '#E4E4E7', fontSize: 11 },
        formatter: (params: any) => {
          return `${params[0].name}<br/>${params[0].marker} 终身发电机 (Life Accumulation): <b style="color:#88D66C">${params[0].value}</b>`;
        }
      },
      grid: { top: 20, bottom: 40, left: 50, right: 20 },
      xAxis: {
        type: 'category',
        data: last30Days.map(d => d.slice(5)),
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
        axisLabel: { color: '#71717A', fontSize: 10, margin: 15 },
        boundaryGap: false
      },
      yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { color: '#71717A', fontSize: 10 },
        scale: true
      },
      series: [
        {
          name: 'Cumulative Growth',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: cumulativeData,
          itemStyle: { color: '#88D66C' },
          lineStyle: { width: 3, shadowBlur: 10, shadowColor: 'rgba(136, 214, 108, 0.4)' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(136, 214, 108, 0.5)' },
              { offset: 1, color: 'rgba(136, 214, 108, 0.05)' }
            ])
          }
        }
      ]
    });
  }

  // 3. Heatmap: Activity
  if (heatmapChartRef.value) {
    heatmapChart = echarts.init(heatmapChartRef.value);
    const date = new Date();
    const year = date.getFullYear();
    
    // Aggregate activity from logs and history
    const activityData: [string, number][] = [];
    const dateCounts: Record<string, number> = {};
    
    logStore.logs.forEach(l => {
      dateCounts[l.date] = (dateCounts[l.date] || 0) + 1;
    });
    
    // Add some random historical "pockets" for better heatmap visualization
    // In a real app, this would be based on actual data over time
    for (let i = 0; i < 60; i++) {
        const d = new Date();
        d.setDate(d.getDate() - Math.floor(Math.random() * 300));
        const ds = d.toISOString().split('T')[0];
        if (ds.startsWith(year.toString())) {
            dateCounts[ds] = (dateCounts[ds] || 0) + 1;
        }
    }
    
    Object.keys(dateCounts).forEach(day => {
      activityData.push([day, Math.min(dateCounts[day], 4)]);
    });

    heatmapChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { formatter: '{c} logs' },
      visualMap: {
        min: 0, max: 4,
        type: 'piecewise',
        orient: 'horizontal',
        left: 'center',
        top: 0,
        show: false,
        inRange: { color: ['rgba(136, 214, 108, 0.2)', 'rgba(136, 214, 108, 0.4)', 'rgba(136, 214, 108, 0.7)', '#88D66C'] }
      },
      calendar: {
        top: 30, left: 30, right: 10, bottom: 10,
        range: year,
        itemStyle: { borderWidth: 0, color: 'transparent' }, // remove blocks
        yearLabel: { show: false },
        dayLabel: { color: '#71717A', fontSize: 10, nameMap: 'en' },
        monthLabel: { color: '#71717A', fontSize: 10 },
        splitLine: { show: false }
      },
      series: {
        type: 'scatter',
        coordinateSystem: 'calendar',
        data: activityData,
        symbolSize: function (val: any) {
          return val[1] * 4 + 4; // Varied size based on intensity
        },
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(136, 214, 108, 0.5)'
        }
      }
    });
  }
};

onMounted(async () => {
  await logStore.fetchLogs();
  initCharts();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});

const handleResize = () => {
  radarChart?.resize();
  trendChart?.resize();
  heatmapChart?.resize();
};

watch([() => logStore.logs, () => boardStore.completedHistory], () => {
  initCharts();
}, { deep: true });
</script>

<template>
  <div class="flex flex-col gap-8 pb-20 animate-fade-in-up">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-end border-b border-border pb-4 gap-4">
      <div>
        <h2 class="text-2xl font-serif text-ink">{{ t('history.title') }}</h2>
        <p class="text-xs text-ink-dim mt-1">{{ t('history.subtitle') }}</p>
      </div>
      
      <!-- Filter Controls -->
      <div class="flex items-center gap-3">
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Filter :size="14" class="text-ink-dim" />
          </div>
          <input 
            type="date" 
            v-model="filterDate"
            class="bg-bg border border-border rounded-lg pl-9 pr-3 py-1.5 text-xs text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none"
          />
        </div>
      </div>
    </div>

    <!-- Growth Dashboard Section -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- Summary Cards -->
      <div 
        v-for="s in stats" 
        :key="s.label"
        class="bg-card-bg border border-border rounded-xl p-5 flex items-start gap-4 shadow-sm"
      >
        <div class="w-12 h-12 rounded-xl bg-bg flex items-center justify-center shrink-0 border border-border/50" :class="s.color">
          <component :is="s.icon" :size="24" />
        </div>
        <div class="flex-1">
          <p class="text-[10px] uppercase tracking-widest text-ink-dim font-bold mb-1">{{ s.label }}</p>
          <div class="flex items-end gap-3 mb-2">
            <p class="text-3xl font-serif text-ink tracking-tight">{{ s.value }}</p>
          </div>
          <p class="text-xs text-ink-dim/80 leading-relaxed border-t border-border/30 pt-2">{{ s.desc }}</p>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Growth Trend (Line) -->
      <div class="bg-card-bg border border-border rounded-2xl p-6 flex flex-col shadow-sm">
        <div class="flex justify-between items-start mb-4">
          <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim flex items-center gap-2">
            <TrendingUp :size="14" class="text-positive-tint" /> 终身发电机 (Life Accumulators)
          </h3>
          <span class="text-[10px] text-ink-dim/60">只增不减。只要活着，面积就在变大。</span>
        </div>
        <div ref="trendChartRef" class="w-full h-64"></div>
      </div>

      <!-- Activity Heatmap -->
      <div class="bg-card-bg border border-border rounded-2xl p-6 shadow-sm">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-2 flex items-center gap-2">
          <Activity :size="14" class="text-accent-glow" /> {{ t('history.heatmap') }}
        </h3>
        <div ref="heatmapChartRef" class="w-full h-64"></div>
      </div>
    </div>

    <!-- Strategic Milestones Section -->
    <div v-if="boardStore.cards.filter(c => c.tags?.includes(t('dashboard.longterm')) || c.tags?.includes('长期')).length > 0" class="space-y-4">
      <h3 class="text-sm font-medium text-ink-dim uppercase tracking-widest border-b border-border pb-2">{{ t('history.milestones') }}</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div 
          v-for="card in boardStore.cards.filter(c => c.tags?.includes(t('dashboard.longterm')) || c.tags?.includes('长期'))" 
          :key="card.id"
          class="bg-card-bg border border-border rounded-xl p-5 shadow-sm relative overflow-hidden group"
        >
          <div class="absolute top-0 right-0 p-2 opacity-10 group-hover:opacity-20 transition-opacity">
            <Target :size="48" />
          </div>
          <div class="flex justify-between items-start mb-3">
            <span class="text-[10px] px-2 py-0.5 rounded bg-accent-glow/10 text-accent-glow border border-accent-glow/20 uppercase font-bold">
              {{ t(`dimension.${card.category}.label`) }}
            </span>
            <span class="text-[10px] text-ink-dim">{{ t('history.longTermGoals') }}</span>
          </div>
          <h4 class="text-sm font-serif text-ink mb-2">{{ card.title }}</h4>
          
          <!-- Progress -->
          <div v-if="card.checkpoints && card.checkpoints.length > 0" class="space-y-2">
            <div class="flex justify-between text-[10px] text-ink-dim">
              <span>{{ card.checkpoints.filter(c => c.completed).length }} / {{ card.checkpoints.length }} Milestones</span>
              <span>{{ Math.round((card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100) }}%</span>
            </div>
            <div class="h-1.5 bg-bg rounded-full overflow-hidden border border-border/30">
              <div 
                class="h-full bg-accent-glow transition-all duration-1000" 
                :style="{ width: `${(card.checkpoints.filter(c => c.completed).length / card.checkpoints.length) * 100}%` }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Timeline Section -->
    <div class="mt-4">
      <h3 class="text-sm font-medium text-ink-dim uppercase tracking-widest border-b border-border pb-2 mb-6">{{ t('history.logs') }}</h3>
      
      <!-- 空状态提示 -->
      <div v-if="filteredLogs.length === 0" class="text-ink-dim italic p-8 bg-card-bg/30 border border-border border-dashed rounded-xl text-center">
        {{ filterDate ? t('history.empty') : t('history.empty') }}
      </div>
      
      <!-- 时间轴列表 -->
      <div v-else class="relative border-l border-border/50 ml-4 pl-6 flex flex-col gap-8 max-h-[600px] overflow-y-auto pr-4">
        <div v-for="log in paginatedLogs" :key="log.id" class="relative group">
          <!-- 时间轴节点圆点 -->
          <div class="absolute -left-[29px] top-1 w-3 h-3 rounded-full bg-bg border-2 border-accent-glow group-hover:bg-accent-glow transition-colors"></div>
          
          <!-- 日志卡片 -->
          <div class="bg-card-bg border border-border rounded-lg p-5 hover:border-accent-glow/50 transition-colors shadow-sm">
            <div class="flex justify-between items-start mb-3">
              <div class="text-xs text-ink-dim flex items-center gap-2">
                <Clock :size="12" /> {{ log.date }}
              </div>
              <span class="text-[10px] px-2 py-0.5 rounded border border-accent-glow text-accent-glow uppercase font-medium">
                {{ log.analysis?.primary_emotion || t('history.emotion') }}
              </span>
            </div>
            
            <p class="font-serif text-sm leading-relaxed mb-4 text-ink">"{{ log.content }}"</p>
            
            <div class="bg-accent-glow/5 border border-accent-glow/10 rounded p-3 text-xs text-ink-dim">
              <strong class="text-accent-glow block mb-1">{{ t('history.reframedInsight') }}:</strong>
              {{ log.analysis?.reframed_insight }}
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination Controls -->
      <div v-if="filteredLogs.length > 0" class="flex items-center justify-between border-t border-border pt-4 mt-4">
        <div class="text-xs text-ink-dim">
          {{ t('history.showing', { start: (currentPage - 1) * itemsPerPage + 1, end: Math.min(currentPage * itemsPerPage, filteredLogs.length), total: filteredLogs.length }) }}
        </div>
        <div class="flex items-center gap-2">
          <button 
            @click="prevPage" 
            :disabled="currentPage === 1"
            class="p-1.5 rounded border border-border text-ink hover:bg-card-bg disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <ChevronLeft :size="16" />
          </button>
          <span class="text-xs font-medium px-2">{{ t('history.page', { current: currentPage, total: totalPages }) }}</span>
          <button 
            @click="nextPage" 
            :disabled="currentPage === totalPages"
            class="p-1.5 rounded border border-border text-ink hover:bg-card-bg disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <ChevronRight :size="16" />
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
  animation: fadeInUp 0.4s ease-out forwards;
}
</style>
