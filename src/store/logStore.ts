import { defineStore } from 'pinia';
import { apiFetch } from '../api';

// 定义日志条目的接口类型
export interface LogEntry {
  id: string;
  date: string;
  content: string;
  analysis: any; // 存储 AI 分析结果
}

// 创建并导出 Pinia Store
export const useLogStore = defineStore('logStore', {
  state: () => ({
    // 存储所有的历史日志
    logs: [] as LogEntry[],
    loading: false,
  }),
  actions: {
    async fetchLogs() {
      this.loading = true;
      try {
        const data = await apiFetch('/logs');
        this.logs = data.map((log: any) => ({
          id: log.logId,
          date: log.logDate,
          content: log.content,
          analysis: log.emotionAnalysis,
        }));
      } finally {
        this.loading = false;
      }
    },
    async reframeEmotion(content: string, therapyMode: string = 'adlerian') {
      this.loading = true;
      try {
        const analysis = await apiFetch('/reframe', {
          method: 'POST',
          body: JSON.stringify({ content, therapyMode }),
        });
        
        // After reframing, we should probably fetch logs again or just push the new one
        await this.fetchLogs();
        
        return analysis;
      } finally {
        this.loading = false;
      }
    },
    // 添加新日志到列表开头 (Local only, usually used after API call)
    addLog(log: LogEntry) {
      this.logs.unshift(log);
    }
  }
});
