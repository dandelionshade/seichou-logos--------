<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useBoardStore } from '@/store/boardStore';
import { useLogStore } from '@/store/logStore';
import { apiFetch } from '@/api';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { 
  TrendingUp, Activity, Sparkles, 
  Brain, Shield, Zap, Heart, Star, Loader2, FileText
} from 'lucide-vue-next';
import * as echarts from 'echarts';

const { t } = useI18n();
const boardStore = useBoardStore();
const logStore = useLogStore();

const lineChartRef = ref<HTMLElement | null>(null);
const barChartRef = ref<HTMLElement | null>(null);
const radarChartRef = ref<HTMLElement | null>(null);
const heatmapChartRef = ref<HTMLElement | null>(null);

const isGeneratingReport = ref(false);
const weeklyReportHtml = ref('');

let radarChart: echarts.ECharts | null = null;
let lineChart: echarts.ECharts | null = null;
let barChart: echarts.ECharts | null = null;
let heatmapChart: echarts.ECharts | null = null;

const synergyArea = ref(0);

const generateWeeklyReport = async () => {
  if (isGeneratingReport.value) return;
  isGeneratingReport.value = true;
  weeklyReportHtml.value = '';
  try {
    const response = await apiFetch('/reports/weekly', {
      method: 'POST',
      body: JSON.stringify({
        logs: logStore.logs.slice(0, 20), // Send recent logs
        cards: boardStore.completedHistory.slice(0, 30) // and recently completed cards
      })
    });
    
    if (response && response.markdown) {
      const rawHtml = await marked.parse(response.markdown);
      weeklyReportHtml.value = DOMPurify.sanitize(rawHtml as string);
    }
  } catch (err) {
    console.error(err);
    weeklyReportHtml.value = '<p class="text-red-400">Failed to generate report. Please try again later.</p>';
  } finally {
    isGeneratingReport.value = false;
  }
};

const initCharts = () => {
  initRadarChart();
  initLineChart();
  initBarChart();
  initHeatmapChart();
};

onMounted(async () => {
  if (boardStore.cards.length === 0) await boardStore.fetchCards();
  if (logStore.logs.length === 0) await logStore.fetchLogs();
  
  initCharts();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});

const handleResize = () => {
  radarChart?.resize();
  lineChart?.resize();
  barChart?.resize();
  heatmapChart?.resize();
};

const initRadarChart = () => {
  if (!radarChartRef.value) return;
  radarChart = echarts.init(radarChartRef.value);
  
  const v = Math.max(10, boardStore.stats.vitalityExp % 100);
  const f = Math.max(10, boardStore.stats.flowExp % 100);
  const s = Math.max(10, (boardStore.stats.sparkExp || 20) % 100); 
  const e = Math.max(10, (boardStore.stats.echoExp || 10) % 100);
  const r = Math.max(10, (boardStore.stats.resilienceExp || 30) % 100);

  // 物理面积计算法: 0.5 * sin(72) * sum(r[i]*r[i+1])
  const calcArea = Math.round(0.5 * Math.sin(72 * Math.PI / 180) * 
    (v*f + f*e + e*s + s*r + r*v));
  synergyArea.value = calcArea;

  radarChart.setOption({
    backgroundColor: 'transparent',
    radar: {
      indicator: [
        { name: t('dimension.health.label'), max: 100 },
        { name: t('dimension.mind.label'), max: 100 },
        { name: t('dimension.social.label'), max: 100 },
        { name: t('dimension.career.label'), max: 100 },
        { name: t('reports.traits.resilience'), max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#8E9299', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(212, 175, 55, 0.1)', type: 'dashed' } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: 'rgba(212, 175, 55, 0.2)' } }
    },
    series: [{
      name: 'Soul Portrait',
      type: 'radar',
      data: [
        {
          value: [v, f, e, s, r],
          name: 'Current State',
          itemStyle: { color: '#d4af37' },
          areaStyle: {
            color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
              { offset: 0, color: 'rgba(212, 175, 55, 0.6)' },
              { offset: 1, color: 'rgba(212, 175, 55, 0.1)' }
            ])
          },
          lineStyle: { width: 2 }
        }
      ]
    }]
  });
};

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return;
  heatmapChart = echarts.init(heatmapChartRef.value);

  // Generate 6 months of mock activity data
  const data = [];
  const today = new Date();
  const startDate = new Date();
  startDate.setMonth(startDate.getMonth() - 6);
  
  for (let d = new Date(startDate); d <= today; d.setDate(d.getDate() + 1)) {
    const val = Math.random();
    // Simulate some active and inactive days
    const exp = val > 0.6 ? Math.floor(Math.random() * 50) + 10 : (val > 0.4 ? Math.floor(Math.random() * 15) : 0);
    const dateStr = [d.getFullYear(), String(d.getMonth() + 1).padStart(2,'0'), String(d.getDate()).padStart(2,'0')].join('-');
    data.push([dateStr, exp]);
  }

  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: function (p: any) {
        const value = p.data[1];
        return `${p.data[0]}: ${value > 0 ? value + ' EXP Gained' : 'Rest Day'}`;
      }
    },
    visualMap: {
      min: 0,
      max: 60,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      top: 0,
      itemWidth: 10,
      itemHeight: 100,
      inRange: {
        color: ['#1e1e1e', '#324a35', '#4d7c4f', '#6db33f', '#88D66C']
      },
      show: false
    },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      cellSize: ['auto', 14],
      range: [startDate.toISOString().split('T')[0], today.toISOString().split('T')[0]],
      itemStyle: {
        color: '#1a1a1a',
        borderWidth: 2,
        borderColor: '#111'
      },
      yearLabel: { show: false },
      dayLabel: { color: '#8E9299', fontSize: 10 },
      monthLabel: { color: '#8E9299', fontSize: 10 },
      splitLine: { show: false }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: data,
      itemStyle: { borderRadius: 3 }
    }]
  });
};

const initLineChart = () => {
  if (!lineChartRef.value) return;
  lineChart = echarts.init(lineChartRef.value);
  
  const last30Days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (29 - i));
    return d.toISOString().split('T')[0].slice(5);
  });

  // Mock a resilient growth curve with a significant dip
  const vitalityData = last30Days.map((_, i) => 40 + Math.sin(i / 3) * 15 + i);
  // Add a significant "low battery / burnout" valley around day 18-20
  vitalityData[18] -= 20;
  vitalityData[19] -= 25;
  vitalityData[20] -= 15;
  // Epiphany multiplier kicks in at day 21, skyrocketing flow
  const flowData = last30Days.map((_, i) => 30 + Math.cos(i / 4) * 10 + i * 1.5);
  flowData[19] -= 20;
  flowData[20] -= 10;
  flowData[21] += 30; // Epiphany trigger
  flowData[22] += 35;

  lineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { 
      data: ['Vitality', 'Flow'], 
      textStyle: { color: '#8E9299' },
      bottom: 0 
    },
    grid: { top: 30, bottom: 40, left: 30, right: 30 },
    xAxis: {
      type: 'category',
      data: last30Days,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#8E9299', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      show: false,
    },
    series: [
      {
        name: 'Vitality',
        type: 'line',
        smooth: true,
        data: vitalityData.map(v => Math.max(0, Math.round(v))),
        itemStyle: { color: '#F87171' },
      },
      {
        name: 'Flow',
        type: 'line',
        smooth: true,
        data: flowData.map(f => Math.max(0, Math.round(f))),
        itemStyle: { color: '#60A5FA' },
        markPoint: {
          data: [
            { 
              coord: [19, Math.max(0, flowData[19])],
              name: 'Low Point',
              value: 'Burnout / Low Battery',
              itemStyle: { color: '#ef4444' }
            },
            {
              coord: [22, flowData[22]],
              name: 'Epiphany',
              value: 'Epiphany x1.5',
              itemStyle: { color: '#88D66C' }
            }
          ]
        }
      }
    ]
  });
};

const initBarChart = () => {
  if (!barChartRef.value) return;
  barChart = echarts.init(barChartRef.value);
  barChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 10, bottom: 20, left: 80, right: 20 },
    xAxis: {
      type: 'value',
      splitLine: { show: false },
      axisLabel: { show: false }
    },
    yAxis: {
      type: 'category',
      data: [
        t('dimension.health.label'), 
        t('dimension.mind.label'), 
        t('dimension.career.label'), 
        t('dimension.social.label')
      ],
      axisLine: { show: false },
      axisLabel: { color: '#8E9299', fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data: [
        boardStore.completedHistory.filter(c => c.category === 'health').length,
        boardStore.completedHistory.filter(c => c.category === 'mind').length,
        boardStore.completedHistory.filter(c => c.category === 'career').length,
        boardStore.completedHistory.filter(c => c.category === 'social').length
      ],
      barWidth: 10,
      itemStyle: {
        color: '#d4af37',
        borderRadius: 5
      }
    }]
  });
};

const characterTraits = computed(() => [
  { name: t('reports.traits.resilience'), level: boardStore.stats.level > 5 ? t('reports.traits.master') : t('reports.traits.adept'), icon: Shield, iconColor: 'text-blue-400', desc: t('ai.assets.stressMetabolism') },
  { name: t('dimension.career.label'), level: t('reports.traits.adept'), icon: Zap, iconColor: 'text-orange-400', desc: t('ai.assets.reflectiveCapacity') },
  { name: t('reports.traits.selfAwareness'), level: t('reports.traits.expert'), icon: Brain, iconColor: 'text-purple-400', desc: t('ai.assets.selfAwareness') },
]);

watch([() => boardStore.stats, () => logStore.logs], () => {
  initCharts();
}, { deep: true });
</script>

<template>
  <div class="flex flex-col gap-8 pb-20 animate-fade-in-up">
    <!-- Header -->
    <div class="flex justify-between items-end border-b border-border pb-4">
      <div>
        <h2 class="text-2xl font-serif text-ink">{{ t('reports.title') }}</h2>
        <p class="text-xs text-ink-dim mt-1">{{ t('reports.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-[10px] uppercase tracking-widest text-ink-dim">{{ t('reports.lastSync', { time: 'Today 10:30' }) }}</span>
        <Sparkles :size="14" class="text-accent-glow animate-pulse" />
      </div>
    </div>

    <!-- AI Character Sheet -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Radar Chart: Growth Balance -->
      <div class="lg:col-span-1 bg-card-bg border border-border rounded-2xl p-6 shadow-sm flex flex-col relative overflow-hidden">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-2 flex items-center gap-2">
          <Activity :size="14" class="text-accent-glow" /> {{ t('reports.growthBalance') }}
        </h3>
        <p class="text-[10px] text-ink-dim/80 mb-2">Total Synergy Area: <span class="text-accent-glow font-mono font-bold">{{ synergyArea }} px²</span></p>
        <div ref="radarChartRef" class="w-full h-56 relative z-10"></div>
        <div class="p-3 bg-bg/80 rounded-xl border border-border/50 backdrop-blur-md">
          <p class="text-[10px] text-ink-dim leading-relaxed">
            <span class="text-accent-glow font-bold">{{ t('reports.aiInsight') }}:</span> 
            Your Total Synergy Area shows exponential intrinsic value. Avoid zero values in any dimension to maximize the area metric.
          </p>
        </div>
      </div>

      <!-- Character Traits -->
      <div class="lg:col-span-2 space-y-4">
        <div class="bg-card-bg border border-border rounded-2xl p-6 shadow-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-6 flex items-center gap-2">
            <Star :size="14" class="text-vision" /> {{ t('reports.coreTraits') }}
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div v-for="trait in characterTraits" :key="trait.name" class="p-4 bg-bg border border-border rounded-xl hover:border-accent-glow/30 transition-colors group">
              <div class="flex items-center justify-between mb-3">
                <component :is="trait.icon" :size="20" :class="trait.iconColor || 'text-accent-glow'" />
                <span class="text-[10px] uppercase font-bold text-accent-glow">{{ trait.level }}</span>
              </div>
              <h4 class="text-sm font-serif text-ink mb-1">{{ trait.name }}</h4>
              <p class="text-[10px] text-ink-dim leading-tight opacity-70">{{ trait.desc }}</p>
            </div>
          </div>
        </div>

        <!-- Growth Assets Bar Chart -->
        <div class="bg-card-bg border border-border rounded-2xl p-6 shadow-sm flex flex-col">
          <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-4 flex items-center gap-2">
            <TrendingUp :size="14" class="text-accent-glow" /> {{ t('reports.growthAssetsGained') }}
          </h3>
          <div ref="barChartRef" class="w-full h-48"></div>
        </div>
      </div>
    </div>

    <!-- Weekly AI Growth Report -->
    <div class="bg-accent-glow/5 border border-accent-glow/20 rounded-2xl p-8 relative overflow-hidden">
      <div class="absolute -right-4 -bottom-4 opacity-5 rotate-12">
        <Brain :size="160" />
      </div>
      <div class="flex items-start gap-6 relative z-10">
        <div class="w-16 h-16 rounded-full bg-accent-glow/10 flex items-center justify-center shrink-0 border border-accent-glow/20">
          <Sparkles :size="32" class="text-accent-glow" />
        </div>
        <div class="flex-1">
          <h3 class="text-xl font-serif text-accent-glow mb-3">周期性“灵魂画像”报告</h3>
          
          <div v-if="!weeklyReportHtml" class="space-y-4 text-sm text-ink-dim leading-relaxed max-w-4xl">
            <p>
              Based on your {{ logStore.logs.length }} logs and {{ boardStore.completedHistory.length }} completed tasks, the AI is ready to synthesize your weekly soul profile.
              Click the button below to generate a deep reflection on your emotional keywords, cognitive patterns, and weekly highlights.
            </p>
            <div class="mt-6 flex gap-4">
              <button 
                @click="generateWeeklyReport"
                :disabled="isGeneratingReport"
                class="flex items-center gap-2 px-6 py-2.5 bg-accent-glow text-bg rounded-lg text-xs font-bold uppercase tracking-widest hover:bg-accent-glow/90 transition-all disabled:opacity-50"
              >
                <Loader2 v-if="isGeneratingReport" class="animate-spin" :size="16" />
                <FileText v-else :size="16" />
                {{ isGeneratingReport ? 'Synthesizing...' : '一键生成周报 (Generate Report)' }}
              </button>
            </div>
          </div>
          
          <div v-else class="space-y-6">
            <div 
              class="markdown-body prose prose-sm prose-invert max-w-none prose-p:text-ink-dim prose-headings:text-ink prose-a:text-accent-glow prose-strong:text-accent-glow"
              v-html="weeklyReportHtml"
            ></div>
            <div class="pt-4 border-t border-accent-glow/20 mt-6 flex gap-4">
              <button class="px-4 py-2 border border-accent-glow/30 text-accent-glow rounded-lg text-xs font-bold uppercase tracking-widest hover:bg-accent-glow/5 transition-colors">
                {{ t('reports.downloadReport') }}
              </button>
              <button class="px-4 py-2 border border-accent-glow/30 text-accent-glow rounded-lg text-xs font-bold uppercase tracking-widest hover:bg-accent-glow/5 transition-colors">
                {{ t('reports.shareInsights') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Github-style Contribution Heatmap -->
    <div class="bg-card-bg border border-border rounded-2xl p-6 shadow-sm overflow-x-auto">
      <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-2 flex items-center gap-2">
        <Brain :size="14" class="text-accent-glow" /> Activity Heatmap (Last 6 Months)
      </h3>
      <p class="text-[10px] text-ink-dim/70 mb-4 tracking-wide">Consistency builds compound interest. Every block represents a day of life logged.</p>
      <div class="min-w-[700px] h-32">
        <div ref="heatmapChartRef" class="w-full h-full"></div>
      </div>
    </div>

    <!-- Emotion vs Growth Trend Data Visualization -->
    <div class="bg-card-bg border border-border rounded-2xl p-6 shadow-sm relative">
      <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim mb-2 flex items-center gap-2">
        <Activity :size="14" class="text-accent-glow" /> 30-Day Subconscious Trend
      </h3>
      <p class="text-[10px] text-ink-dim/70 mb-4 tracking-wide">Notice how the "Low Battery" burnout phase (dip) organically paved the way for an "Epiphany x1.5" breakthrough. Pain is composted.</p>
      <div ref="lineChartRef" class="w-full h-64"></div>
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
