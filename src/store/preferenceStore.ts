import { defineStore } from 'pinia';
import { ref, watch } from 'vue';
import { i18n } from '../i18n';
import { apiFetch } from '../api';

export type Theme = 'light' | 'dark';
export type AIPersonality = 'empathetic' | 'strict' | 'philosophical';
export type DataPrivacy = 'strict' | 'standard';

export const usePreferenceStore = defineStore('preference', () => {
  const theme = ref<Theme>((localStorage.getItem('seichou_theme') as Theme) || 'dark');
  const language = ref(localStorage.getItem('seichou_lang') || 'en');
  const notifications = ref(localStorage.getItem('seichou_notifications') !== 'false');
  const aiPersonality = ref<AIPersonality>((localStorage.getItem('seichou_ai_personality') as AIPersonality) || 'empathetic');
  const dataPrivacy = ref<DataPrivacy>((localStorage.getItem('seichou_data_privacy') as DataPrivacy) || 'standard');
  const loading = ref(false);

  // Apply theme on init
  const applyTheme = (t: Theme) => {
    if (t === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };
  applyTheme(theme.value);

  const fetchPreferences = async () => {
    loading.value = true;
    try {
      const data = await apiFetch('/preferences');
      theme.value = data.theme;
      language.value = data.language;
      notifications.value = data.notificationsEnabled;
      aiPersonality.value = data.aiPersonality;
      dataPrivacy.value = data.dataPrivacy;
      
      // Update local storage and UI
      localStorage.setItem('seichou_theme', data.theme);
      localStorage.setItem('seichou_lang', data.language);
      applyTheme(data.theme);
      i18n.global.locale.value = data.language as any;
    } catch (e) {
      console.error('Failed to fetch preferences', e);
    } finally {
      loading.value = false;
    }
  };

  const updatePreferences = async (updates: any) => {
    try {
      await apiFetch('/preferences', {
        method: 'PUT',
        body: JSON.stringify(updates),
      });
    } catch (e) {
      console.error('Failed to update preferences', e);
    }
  };

  // Watchers for persistence & API sync
  watch(theme, (newTheme) => {
    localStorage.setItem('seichou_theme', newTheme);
    applyTheme(newTheme);
    updatePreferences({ theme: newTheme });
  });

  watch(language, (newLang) => {
    localStorage.setItem('seichou_lang', newLang);
    i18n.global.locale.value = newLang as any;
    updatePreferences({ language: newLang });
  });

  watch(notifications, (val) => {
    localStorage.setItem('seichou_notifications', String(val));
    updatePreferences({ notificationsEnabled: val });
  });

  watch(aiPersonality, (val) => {
    localStorage.setItem('seichou_ai_personality', val);
    updatePreferences({ aiPersonality: val });
  });

  watch(dataPrivacy, (val) => {
    localStorage.setItem('seichou_data_privacy', val);
    updatePreferences({ dataPrivacy: val });
  });

  return {
    theme,
    language,
    notifications,
    aiPersonality,
    dataPrivacy,
    loading,
    fetchPreferences
  };
});
