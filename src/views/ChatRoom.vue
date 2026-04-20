<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useBoardStore } from '../store/boardStore';
import { useChatStore } from '../store/chatStore';
import { 
  Send, User, Bot, Sparkles, PlusCircle, 
  History as HistoryIcon, Plus, Trash2, ChevronLeft, MessageSquare,
  Pencil
} from 'lucide-vue-next';

const { t } = useI18n();
const boardStore = useBoardStore();
const chatStore = useChatStore();

const newMessage = ref('');
const searchQuery = ref('');
const isTyping = ref(false);
const chatContainer = ref<HTMLElement | null>(null);
const showSideBar = ref(true);
const editingSessionId = ref<string | null>(null);
const editTitleValue = ref('');

// Confirmation States
const showConfirmDelete = ref(false);
const sessionToDelete = ref<{ id: string, title: string } | null>(null);
const showConfirmClearAll = ref(false);

const filteredSessions = computed(() => {
  if (!searchQuery.value.trim()) return chatStore.sessions;
  const q = searchQuery.value.toLowerCase();
  return chatStore.sessions.filter(s => 
    s.title.toLowerCase().includes(q) || 
    s.dimensions?.some(d => d.toLowerCase().includes(q))
  );
});

const messages = computed(() => {
  const activeSession = chatStore.sessions.find(s => s.id === chatStore.activeSessionId);
  if (!activeSession) return [{
    id: 0,
    role: 'assistant' as const,
    content: t('chat.welcome'),
    timestamp: new Date().toISOString()
  }];
  return activeSession.messages;
});

const scrollToBottom = async () => {
  await nextTick();
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};

onMounted(() => {
  chatStore.loadSessions();
  scrollToBottom();
});

watch(() => chatStore.activeSessionId, () => {
  scrollToBottom();
  editingSessionId.value = null;
});

const startNewChat = () => {
  chatStore.activeSessionId = null;
  newMessage.value = '';
  editingSessionId.value = null;
};

const selectSession = (id: string) => {
  if (editingSessionId.value) return; // Don't switch if editing
  chatStore.activeSessionId = id;
  if (window.innerWidth < 768) showSideBar.value = false;
};

const startEditing = (session: any) => {
  editingSessionId.value = session.id;
  editTitleValue.value = session.title;
};

const saveTitle = (id: string) => {
  const trimmed = editTitleValue.value.trim();
  if (trimmed && trimmed !== chatStore.sessions.find(s => s.id === id)?.title) {
    chatStore.updateSessionTitle(id, trimmed);
  }
  editingSessionId.value = null;
};

const cancelEditing = () => {
  editingSessionId.value = null;
};

const openDeleteConfirm = (session: any) => {
  sessionToDelete.value = { id: session.id, title: session.title };
  showConfirmDelete.value = true;
};

const confirmDelete = () => {
  if (sessionToDelete.value) {
    chatStore.deleteSession(sessionToDelete.value.id);
  }
  showConfirmDelete.value = false;
  sessionToDelete.value = null;
};

const openClearAllConfirm = () => {
  showConfirmClearAll.value = true;
};

const confirmClearAll = () => {
  chatStore.clearAll();
  showConfirmClearAll.value = false;
};

const sendMessage = async () => {
  if (!newMessage.value.trim() || isTyping.value) return;

  const text = newMessage.value;
  newMessage.value = '';

  if (!chatStore.activeSessionId) {
    chatStore.createNewSession(text.slice(0, 30) + '...');
  }

  chatStore.addMessageToActiveSession({
    id: Date.now(),
    role: 'user',
    content: text
  });
  
  scrollToBottom();
  isTyping.value = true;
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 25000);

  try {
    const history = messages.value
      .filter(m => m.id !== 0 && !m.isAction)
      .map(m => ({
        role: m.role,
        content: m.content
      }));

    const userContext = {
      stats: boardStore.stats,
      activeCards: boardStore.cards.filter(c => c.status === 'todo').map(c => ({
        title: c.title,
        dimension: c.category,
        tags: c.tags
      }))
    };

    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(localStorage.getItem('token') ? { 'Authorization': `Bearer ${localStorage.getItem('token')}` } : {})
      },
      signal: controller.signal,
      body: JSON.stringify({
        messages: history,
        userContext,
        therapyMode: 'adlerian' 
      })
    });

    if (response.ok) {
      const data = await response.json();
      
      chatStore.addMessageToActiveSession({
        id: Date.now() + 1,
        role: 'assistant',
        content: data.reply || "I'm listening. Tell me more about that."
      });

      if (data.action && data.action.type === 'CREATE_TASK') {
        const taskData = data.action.data;
        const dimension = taskData.dimension || taskData.category || 'health';

        boardStore.addCard({
          title: taskData.title,
          description: taskData.description,
          category: dimension as any,
          tags: taskData.tags || ['AI-Created']
        });

        // Tag the session with the dimension of the new task
        chatStore.tagActiveSessionWithDimension(dimension);

        chatStore.addMessageToActiveSession({
          id: Date.now() + 2,
          role: 'assistant',
          isAction: true,
          content: `✨ [${t('chat.systemAction')}] ${t('chat.taskAdded', { title: taskData.title })}`
        });
      }
    } else {
      throw new Error('Failed to get AI response');
    }
  } catch (error) {
    const isTimeout = error instanceof DOMException && error.name === 'AbortError';
    chatStore.addMessageToActiveSession({
      id: Date.now() + 1,
      role: 'assistant',
      content: isTimeout
        ? '请求有点慢，我先接住你这句话。你可以继续输入下一句，或者稍后再试。'
        : t('chat.error')
    });
  } finally {
    window.clearTimeout(timeoutId);
    isTyping.value = false;
    scrollToBottom();
  }
};
</script>

<template>
  <div class="max-w-6xl mx-auto flex gap-6 h-[750px] animate-fade-in relative pb-10">
    <!-- Sidebar for History -->
    <div 
      class="fixed inset-0 z-40 bg-black/50 transition-opacity md:hidden"
      v-if="showSideBar"
      @click="showSideBar = false"
    ></div>

    <aside 
      class="absolute md:relative z-50 w-72 h-full bg-card-bg border border-border rounded-2xl flex flex-col transition-transform duration-300 shadow-xl"
      :class="showSideBar ? 'translate-x-0' : '-translate-x-full md:translate-x-0 md:opacity-0 md:w-0 overflow-hidden border-none'"
    >
      <div class="p-4 border-b border-border flex items-center justify-between">
        <h3 class="text-xs font-bold uppercase tracking-widest text-ink-dim flex items-center gap-2">
          <HistoryIcon :size="14" /> {{ t('chat.history') }}
        </h3>
        <button 
          @click="startNewChat"
          class="p-1.5 rounded-lg bg-accent-glow/10 text-accent-glow hover:bg-accent-glow/20 transition-colors"
          :title="t('chat.newChat')"
        >
          <Plus :size="16" />
        </button>
      </div>

      <!-- Search Bar -->
      <div class="p-2 border-b border-border/50">
        <div class="relative">
          <input 
            v-model="searchQuery"
            type="text" 
            placeholder="Search reflections..."
            class="w-full bg-bg/50 border border-border rounded-lg px-8 py-2 text-[10px] focus:ring-1 focus:ring-accent-glow outline-none"
          />
          <HistoryIcon :size="12" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-ink-dim opacity-40" />
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
        <div v-if="filteredSessions.length === 0" class="p-6 text-center mt-10">
          <MessageSquare :size="32" class="mx-auto text-ink-dim/20 mb-3" />
          <p class="text-[10px] text-ink-dim leading-relaxed uppercase tracking-tighter opacity-60">
            {{ searchQuery ? 'No matching reflections' : t('chat.noHistory') }}
          </p>
        </div>

        <div 
          v-for="session in filteredSessions" 
          :key="session.id"
          @click="selectSession(session.id)"
          class="group p-3 rounded-xl cursor-pointer transition-all border border-transparent flex flex-col gap-1.5"
          :class="chatStore.activeSessionId === session.id ? 'bg-accent-glow/10 border-accent-glow/20 shadow-sm' : 'hover:bg-bg/50'"
        >
          <div class="flex items-center gap-3">
            <MessageSquare :size="16" class="shrink-0" :class="chatStore.activeSessionId === session.id ? 'text-accent-glow' : 'text-ink-dim opacity-40'" />
            <div class="flex-1 min-w-0">
              <input 
                v-if="editingSessionId === session.id"
                v-model="editTitleValue"
                @click.stop
                @keyup.enter="saveTitle(session.id)"
                @keyup.esc="cancelEditing"
                @blur="saveTitle(session.id)"
                class="w-full bg-bg border border-accent-glow rounded px-1.5 py-0.5 text-xs focus:outline-none"
                autofocus
              />
              <h4 v-else class="text-xs font-medium truncate" :class="chatStore.activeSessionId === session.id ? 'text-accent-glow' : 'text-ink'">
                {{ session.title }}
              </h4>
            </div>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button 
                v-if="editingSessionId !== session.id"
                @click.stop="startEditing(session)"
                class="p-1 hover:text-accent-glow transition-colors"
                title="Rename"
              >
                <Pencil :size="12" />
              </button>
              <button 
                @click.stop="openDeleteConfirm(session)"
                class="p-1 hover:text-red-400 transition-colors"
                title="Delete"
              >
                <Trash2 :size="12" />
              </button>
            </div>
          </div>
          
          <div class="flex items-center justify-between pl-7">
            <span class="text-[9px] text-ink-dim opacity-60">{{ new Date(session.updatedAt).toLocaleDateString() }}</span>
            
            <!-- Dimension Tags -->
            <div class="flex gap-1" v-if="session.dimensions?.length">
              <div 
                v-for="dim in session.dimensions" 
                :key="dim"
                class="w-1.5 h-1.5 rounded-full"
                :class="{
                  'bg-vitality': dim === 'health',
                  'bg-flow': dim === 'mind',
                  'bg-spark': dim === 'career',
                  'bg-echo': dim === 'social'
                }"
                :title="dim"
              ></div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="p-4 border-t border-border">
        <button 
          @click="openClearAllConfirm"
          :disabled="chatStore.sessions.length === 0"
          class="w-full py-2 text-[10px] uppercase tracking-widest text-ink-dim hover:text-red-400 transition-colors border border-border rounded-lg hover:border-red-400/30 disabled:opacity-30 disabled:hover:text-ink-dim disabled:hover:border-border"
        >
          {{ t('settings.clearAll') }}
        </button>
      </div>
    </aside>

    <!-- Modals for Confirmation -->
    <Teleport to="body">
      <!-- Delete Confirmation Modal -->
      <div v-if="showConfirmDelete" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
        <div class="bg-card-bg border border-border rounded-2xl p-6 max-w-sm w-full shadow-2xl animate-scale-in">
          <h3 class="text-lg font-serif mb-2">{{ t('chat.deleteConfirm') }}</h3>
          <p class="text-sm text-ink-dim mb-6 italic">"{{ sessionToDelete?.title }}"</p>
          <div class="flex gap-3 justify-end">
            <button @click="showConfirmDelete = false" class="px-4 py-2 text-sm text-ink-dim hover:text-ink transition-colors">
              {{ t('edit.cancel') }}
            </button>
            <button @click="confirmDelete" class="px-4 py-2 text-sm bg-red-500/10 text-red-400 border border-red-500/30 rounded-lg hover:bg-red-500/20 transition-colors">
              {{ t('chat.deleteConfirm') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Clear All Confirmation Modal -->
      <div v-if="showConfirmClearAll" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
        <div class="bg-card-bg border border-border rounded-2xl p-6 max-w-sm w-full shadow-2xl animate-scale-in">
          <h3 class="text-lg font-serif mb-2">Clear All Reflections?</h3>
          <p class="text-sm text-ink-dim mb-6">{{ t('settings.clearConfirm') }}</p>
          <div class="flex gap-3 justify-end">
            <button @click="showConfirmClearAll = false" class="px-4 py-2 text-sm text-ink-dim hover:text-ink transition-colors">
              {{ t('edit.cancel') }}
            </button>
            <button @click="confirmClearAll" class="px-4 py-2 text-sm bg-red-500 text-bg rounded-lg font-bold hover:bg-red-600 transition-colors shadow-lg shadow-red-500/20">
              Clear Everything
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Main Chat Container -->
    <div class="flex-1 flex flex-col bg-card-bg border border-border rounded-2xl overflow-hidden shadow-2xl relative">
      <!-- Toggle Sidebar Button (Mobile) -->
      <button 
        @click="showSideBar = !showSideBar"
        class="absolute left-4 top-4 z-20 md:flex items-center gap-2 p-2 rounded-xl bg-bg/80 border border-border text-ink-dim hover:text-accent-glow transition-all backdrop-blur-md shadow-sm"
        :class="showSideBar ? 'md:hidden' : 'flex'"
      >
        <ChevronLeft :size="18" :class="!showSideBar ? 'rotate-180' : ''" />
        <span class="text-[10px] uppercase font-bold tracking-widest hidden sm:inline">{{ t('chat.history') }}</span>
      </button>

      <!-- Chat Header -->
      <div class="p-4 pl-16 md:pl-4 border-b border-border bg-bg/50 backdrop-blur-sm flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-accent-glow/20 flex items-center justify-center text-accent-glow border border-accent-glow/30">
            <Bot :size="20" />
          </div>
          <div>
            <h2 class="text-sm font-serif font-bold text-ink uppercase tracking-widest">{{ t('nav.wisdomChat') }}</h2>
            <div class="flex items-center gap-1.5 mt-0.5">
              <div class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
              <span class="text-[10px] text-ink-dim uppercase tracking-tighter">{{ t('chat.mentorOnline') }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <Sparkles :size="16" class="text-accent-glow opacity-50" />
        </div>
      </div>

      <!-- Messages Area -->
      <div ref="chatContainer" class="flex-1 overflow-y-auto p-6 space-y-6 scroll-smooth custom-scrollbar">
        <div 
          v-for="msg in messages" 
          :key="msg.id"
          class="flex flex-col"
          :class="msg.role === 'user' ? 'items-end' : 'items-start'"
        >
          <div class="flex items-start gap-3 max-w-[85%]" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
            <div 
              class="w-8 h-8 rounded-full shrink-0 flex items-center justify-center text-xs border transition-transform duration-300 hover:scale-110"
              :class="msg.role === 'user' ? 'bg-bg border-border text-ink-dim' : 'bg-accent-glow/10 border-accent-glow/30 text-accent-glow shadow-[0_0_10px_rgba(255,255,255,0.05)]'"
            >
              <User v-if="msg.role === 'user'" :size="14" />
              <Bot v-else :size="14" />
            </div>
            
            <div 
              class="p-4 rounded-2xl text-sm leading-relaxed shadow-sm transition-all duration-300"
              :class="[
                msg.role === 'user' 
                  ? 'bg-accent-glow text-bg rounded-tr-none font-medium' 
                  : 'bg-bg/80 border border-border/80 text-ink rounded-tl-none backdrop-blur-sm',
                msg.isAction ? 'border-dashed border-accent-glow/50 bg-accent-glow/5 italic text-accent-glow' : ''
              ]"
            >
              <div v-if="msg.isAction" class="flex items-center gap-2 mb-1">
                <PlusCircle :size="14" />
                <span class="font-bold uppercase tracking-tighter text-[10px]">{{ t('chat.systemAction') }}</span>
              </div>
              {{ msg.content }}
            </div>
          </div>
          <span class="text-[10px] text-ink-dim mt-1 px-11 opacity-50">
            {{ new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}
          </span>
        </div>

        <!-- Typing Indicator -->
        <div v-if="isTyping" class="flex items-start gap-3">
          <div class="w-8 h-8 rounded-full bg-accent-glow/10 border border-accent-glow/30 flex items-center justify-center text-accent-glow">
            <Bot :size="14" />
          </div>
          <div class="bg-bg/50 backdrop-blur-sm border border-border p-4 rounded-2xl rounded-tl-none flex gap-1 shadow-sm">
            <div class="w-1.5 h-1.5 bg-accent-glow rounded-full animate-bounce"></div>
            <div class="w-1.5 h-1.5 bg-accent-glow rounded-full animate-bounce [animation-delay:0.2s]"></div>
            <div class="w-1.5 h-1.5 bg-accent-glow rounded-full animate-bounce [animation-delay:0.4s]"></div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="p-4 bg-bg/80 backdrop-blur-md border-t border-border mt-auto">
        <form @submit.prevent="sendMessage" class="relative flex items-center gap-2 group">
          <div class="absolute inset-0 bg-accent-glow/5 rounded-xl opacity-0 group-focus-within:opacity-100 transition-opacity blur-xl"></div>
          <input 
            v-model="newMessage"
            type="text" 
            :placeholder="t('chat.placeholder')"
            class="relative flex-1 bg-card-bg/80 border border-border rounded-xl px-4 py-3 text-sm focus:ring-1 focus:ring-accent-glow focus:border-accent-glow outline-none transition-all pr-12"
          />
          <button 
            type="submit"
            class="relative absolute right-2 p-2 rounded-lg bg-accent-glow text-bg hover:shadow-[0_0_15px_rgba(var(--accent-glow-rgb),0.5)] transition-all disabled:opacity-50 disabled:shadow-none"
            :disabled="!newMessage.trim() || isTyping"
          >
            <Send :size="18" />
          </button>
        </form>
        <p class="text-[10px] text-center text-ink-dim mt-3 uppercase tracking-widest opacity-40">
          AI Mentor is weaving your growth tapestry session by session
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in {
  animation: fadeIn 0.5s ease-out forwards;
}
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(var(--accent-glow-rgb), 0.1);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--accent-glow-rgb), 0.2);
}
</style>
