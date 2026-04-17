import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { apiFetch } from '../api';

// 定义卡片的数据维度 (健康、心流/学习、事业/灵感、社交/回音)
export type Category = 'health' | 'mind' | 'career' | 'social';
// 定义卡片的生命周期状态 (待办、已完成、挣扎过、已作为肥料分解)
export type CardStatus = 'todo' | 'done' | 'struggled' | 'composted';

// 里程碑/检查点接口：用于将大任务拆分为小步骤
export interface Checkpoint {
  id: string;
  title: string;          // 步骤名称
  completed: boolean;     // 是否已打勾
}

// 核心看板卡片接口：对应后端的 BoardCard Entity
export interface BoardCard {
  id: string;
  title: string;
  description?: string;
  category: Category;     // 所属维度
  tags: string[];         // 标签阵列
  status: CardStatus;     // 当前状态
  checkpoints?: Checkpoint[]; // 子任务
  progress?: number;      // 完成进度百分比
}

// 用户游戏化能力值雷达：对应后端的 UserStats Entity
export interface UserStats {
  level: number;          // 当前灵魂树等级
  vitalityExp: number;    // 生理能量值 (健康维度)
  flowExp: number;        // 心流能量值 (学习维度)
  sparkExp: number;       // 灵感能量值 (事业维度)
  echoExp: number;        // 社交共振值 (社交维度)
  resilienceExp: number;  // 韧性护盾值 (特殊经验，主要来源于失败/挫折后的沉淀)
  maxExp: number;         // 升级所需的单项经验阈值
  epiphanyMultiplier?: number; // 顿悟倍率（AI判定触发）
  unlockedBadges?: string[];   // 解锁的成就徽章集合
}

// 声明全局响应式状态树：Pinia Store
// 采用 Composition API (Setup Store) 模式
export const useBoardStore = defineStore('board', () => {
  // === 状态声明 (State) ===
  const cards = ref<BoardCard[]>([]);       // 活跃的卡片列表
  const stats = ref<UserStats>({            // 个人大盘数据初始化
    level: 1, 
    vitalityExp: 0, 
    flowExp: 0, 
    sparkExp: 0,
    echoExp: 0,
    resilienceExp: 0,
    maxExp: 100,
    epiphanyMultiplier: 1.0,
    unlockedBadges: []
  });
  const loading = ref(false);               // 全局加载状态控制

  // 历史完成记录 (通常用于查看档案库)
  const completedHistory = ref<BoardCard[]>([]);

  // === 派生属性 (Getters) ===
  // 计算连续成长天数 (目前为 Mock，后端实现后可从 Stats 返回)
  const streak = computed(() => {
    return 5; 
  });

  // === 动作集 (Actions) ===

  // 1. 获取所有当前看板内的任务卡片
  const fetchCards = async () => {
    loading.value = true;
    try {
      const data = await apiFetch('/board/cards');
      cards.value = data;
    } catch (e) {
      console.error('Failed to fetch cards', e);
    } finally {
      loading.value = false;
    }
  };

  // 2. 拉取游戏化雷达大盘数据
  const fetchStats = async () => {
    try {
      const data = await apiFetch('/board/stats');
      stats.value = data;
    } catch (e) {
      console.error('Failed to fetch stats', e);
    }
  };

  // 3. 发布新的行动卡片 (POST)
  const addCard = async (card: Omit<BoardCard, 'id' | 'status'>) => {
    const newCard = await apiFetch('/board/cards', {
      method: 'POST',
      body: JSON.stringify(card),
    });
    cards.value.push(newCard); // 本地响应式推入，前端立刻显示
    return newCard;
  };

  // 4. 更新卡片内容 (PUT) (如：改变标题，勾选某一个 Checkpoint 等)
  const updateCard = async (id: string, updates: Partial<BoardCard>) => {
    const updatedCard = await apiFetch(`/board/cards/${id}`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    });
    // 找到本地旧卡片并替换，保证视图同步更新
    const index = cards.value.findIndex(c => c.id === id);
    if (index !== -1) {
      cards.value[index] = updatedCard;
    }
  };

  const moveCard = async (id: string, category: Category) => {
    await updateCard(id, { category });
  };

  const completeCard = async (id: string) => {
    await updateCard(id, { status: 'done' });
  };

  const markStruggled = async (id: string) => {
    await updateCard(id, { status: 'struggled' });
  };

  const markComposted = async (id: string) => {
    await updateCard(id, { status: 'composted' });
  };

  const toggleCheckpoint = async (cardId: string, checkpointId: string) => {
    const card = cards.value.find(c => c.id === cardId);
    if (!card || !card.checkpoints) return;
    
    const cpIndex = card.checkpoints.findIndex(cp => cp.id === checkpointId);
    if (cpIndex === -1) return;
    
    const updatedCheckpoints = [...card.checkpoints];
    updatedCheckpoints[cpIndex].completed = !updatedCheckpoints[cpIndex].completed;
    
    await updateCard(cardId, { checkpoints: updatedCheckpoints });
  };

  const settleExp = async (feeling: 'smooth' | 'struggled' | 'composted' = 'smooth') => {
    loading.value = true;
    try {
      // Get all cards that are not 'todo' (i.e., done, struggled, composted)
      // If feeling is composted, we might want to settle all 'today' cards regardless of status?
      // Let's just settle the ones the user interacted with or all 'done' ones, plus we can pass the feeling.
      // Actually, if they select 'composted', maybe we convert all 'todo' from today to 'composted'.
      if (feeling === 'composted') {
        cards.value.forEach(c => {
          if (c.status === 'todo') { // change all remaining to composted? Or only feeling bad? Better to just set what user clicked
            c.status = 'composted';
          }
        });
      } else if (feeling === 'struggled') {
         cards.value.forEach(c => {
          if (c.status === 'todo') {
            c.status = 'struggled';
          }
        });
      }

      const cardsToSettle = cards.value.filter(c => c.status !== 'todo');
      
      if (cardsToSettle.length === 0 && feeling === 'smooth') {
        return; // Nothing to settle
      }

      const data = await apiFetch('/settle', {
        method: 'POST',
        body: JSON.stringify({ cards: cardsToSettle, feeling }),
      });
      
      // Update stats after settlement
      await fetchStats();
      
      // Clear local settled cards
      cards.value = cards.value.filter(c => c.status === 'todo');
      completedHistory.value.push(...cardsToSettle);
      
      return data;
    } finally {
      loading.value = false;
    }
  };

  const triggerLowBattery = async () => {
    loading.value = true;
    try {
      const data = await apiFetch('/low-battery', {
        method: 'POST',
      });
      await fetchStats();
      return data;
    } finally {
      loading.value = false;
    }
  };

  // 获取特定维度的卡片
  const getCardsByCategory = (category: Category) => {
    return computed(() => cards.value.filter(c => c.category === category && c.status === 'todo'));
  };

  const getDoneCards = computed(() => cards.value.filter(c => c.status !== 'todo'));

  return { 
    cards, stats, completedHistory, streak, loading,
    fetchCards, fetchStats, addCard, updateCard, moveCard, completeCard, markStruggled, markComposted, toggleCheckpoint, settleExp, triggerLowBattery,
    getCardsByCategory, getDoneCards
  };
});
