<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { useI18n } from 'vue-i18n';

// 接收父组件传入的成长资产数据 (例如: { "自我觉察力": 10, "压力代谢值": 5 })
const props = defineProps<{ data: Record<string, number> }>();
const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
const { t } = useI18n();

// 初始化 ECharts 实例
const initChart = () => {
  if (!chartRef.value) return;
  chartInstance = echarts.init(chartRef.value);
  updateChart();
};

// 更新图表数据和配置
const updateChart = () => {
  if (!chartInstance || !props.data) return;
  
  const keys = Object.keys(props.data);
  const values = Object.values(props.data);
  // 动态计算雷达图的最大值，确保图形不会顶边
  const maxVal = Math.max(...values, 10); 

  const option = {
    radar: {
      indicator: keys.map(key => ({ name: key, max: maxVal + 5 })),
      splitArea: { 
        areaStyle: { color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)'] } 
      },
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisName: { color: '#71717A', fontSize: 10, fontFamily: 'serif' }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: 'Growth Assets',
        itemStyle: { color: '#88D66C' }, // 主题色 accent-glow
        areaStyle: { color: 'rgba(136, 214, 108, 0.2)' },
        lineStyle: { color: '#88D66C', width: 2 }
      }]
    }]
  };
  chartInstance.setOption(option);
};

// 组件挂载时初始化图表，并监听窗口大小变化以自适应
onMounted(() => {
  initChart();
  window.addEventListener('resize', () => chartInstance?.resize());
});

// 监听 props.data 的变化，当 AI 分析出新结果时自动更新图表
watch(() => props.data, () => {
  updateChart();
}, { deep: true });

// 组件卸载时清理资源
onUnmounted(() => {
  window.removeEventListener('resize', () => chartInstance?.resize());
  chartInstance?.dispose();
});
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="font-serif italic text-sm text-ink-dim">{{ t('chart.title') }}</div>
    <!-- ECharts 渲染容器 -->
    <div ref="chartRef" class="w-full h-64 bg-card-bg/30 border border-border rounded-lg"></div>
  </div>
</template>
