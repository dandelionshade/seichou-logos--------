import { defineStore } from 'pinia';
import { ref, watch } from 'vue';

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  isAction?: boolean;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
  dimensions?: string[]; // Dimensions discussed in this session
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([]);
  const activeSessionId = ref<string | null>(null);

  // Initialize from localStorage
  const loadSessions = () => {
    const saved = localStorage.getItem('wisdom_chat_sessions');
    if (saved) {
      try {
        sessions.value = JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse chat sessions', e);
        sessions.value = [];
      }
    }
  };

  const saveSessions = () => {
    localStorage.setItem('wisdom_chat_sessions', JSON.stringify(sessions.value));
  };

  // Watch sessions for changes to auto-save
  watch(sessions, () => {
    saveSessions();
  }, { deep: true });

  const createNewSession = (title: string = 'New Reflection') => {
    const id = Date.now().toString();
    const newSession: ChatSession = {
      id,
      title,
      messages: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    sessions.value.unshift(newSession);
    activeSessionId.value = id;
    return newSession;
  };

  const addMessageToActiveSession = (message: Omit<ChatMessage, 'timestamp'>) => {
    if (!activeSessionId.value) {
      createNewSession(message.content.slice(0, 30) + '...');
    }
    
    const session = sessions.value.find(s => s.id === activeSessionId.value);
    if (session) {
      const fullMessage: ChatMessage = {
        ...message,
        timestamp: new Date().toISOString()
      };
      session.messages.push(fullMessage);
      session.updatedAt = new Date().toISOString();
      
      // Update title if it's the first user message
      if (session.messages.filter(m => m.role === 'user').length === 1 && message.role === 'user') {
        session.title = message.content.slice(0, 30) + (message.content.length > 30 ? '...' : '');
      }
    }
  };

  const tagActiveSessionWithDimension = (dimension: string) => {
    const session = sessions.value.find(s => s.id === activeSessionId.value);
    if (session) {
      if (!session.dimensions) session.dimensions = [];
      if (!session.dimensions.includes(dimension)) {
        session.dimensions.push(dimension);
      }
    }
  };

  const updateSessionTitle = (id: string, newTitle: string) => {
    const session = sessions.value.find(s => s.id === id);
    if (session) {
      session.title = newTitle;
      session.updatedAt = new Date().toISOString();
    }
  };

  const deleteSession = (id: string) => {
    sessions.value = sessions.value.filter(s => s.id !== id);
    if (activeSessionId.value === id) {
      activeSessionId.value = sessions.value[0]?.id || null;
    }
  };

  const clearAll = () => {
    sessions.value = [];
    activeSessionId.value = null;
    localStorage.removeItem('wisdom_chat_sessions');
  };

  return {
    sessions,
    activeSessionId,
    loadSessions,
    createNewSession,
    addMessageToActiveSession,
    deleteSession,
    clearAll,
    updateSessionTitle,
    tagActiveSessionWithDimension
  };
});
