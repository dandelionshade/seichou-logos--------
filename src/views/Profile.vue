<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { User, Mail, Calendar, Edit2, Save, X, Loader2, LogOut, Lock, Award, Shield, Heart, Milestone, Flame, Sun } from 'lucide-vue-next';
import { useAuthStore } from '@/store/authStore';
import { useBoardStore } from '@/store/boardStore';
import { soundFx } from '@/utils/audio';
import { useRouter } from 'vue-router';

const { t } = useI18n();
const authStore = useAuthStore();
const boardStore = useBoardStore();
const router = useRouter();

const isLoading = ref(true);
const isSaving = ref(false);
const isEditing = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const profile = ref({
  id: '',
  email: '',
  name: '',
  bio: '',
  joinedAt: ''
});

const editForm = ref({
  name: '',
  bio: ''
});

// Password change state
const isChangingPwd = ref(false);
const pwdError = ref('');
const pwdSuccess = ref('');
const pwdForm = ref({
  current: '',
  new: '',
  confirm: ''
});

const fetchProfile = async () => {
  if (!authStore.token) {
    authStore.logout();
    return;
  }

  try {
    const res = await fetch('/api/users/me', {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    });
    
    if (!res.ok) {
      if (res.status === 401) {
        authStore.logout();
        return;
      }
      throw new Error('Failed to fetch profile');
    }
    
    const data = await res.json();
    profile.value = data;
    editForm.value = { name: data.name, bio: data.bio };
  } catch (error: any) {
    errorMessage.value = error.message;
  } finally {
    isLoading.value = false;
  }
};

const saveProfile = async () => {
  isSaving.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const res = await fetch('/api/users/me', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify(editForm.value)
    });
    
    if (!res.ok) throw new Error('Failed to update profile');
    
    const data = await res.json();
    profile.value = data;
    isEditing.value = false;
    successMessage.value = 'Profile updated successfully';
    soundFx.success();
    setTimeout(() => { successMessage.value = ''; }, 3000);
  } catch (error: any) {
    errorMessage.value = error.message;
    soundFx.error();
  } finally {
    isSaving.value = false;
  }
};

const changePassword = async () => {
  pwdError.value = '';
  pwdSuccess.value = '';

  if (pwdForm.value.new !== pwdForm.value.confirm) {
    pwdError.value = 'New passwords do not match';
    soundFx.error();
    return;
  }

  if (pwdForm.value.new.length < 6) {
    pwdError.value = 'New password must be at least 6 characters';
    soundFx.error();
    return;
  }

  isChangingPwd.value = true;

  try {
    const res = await fetch('/api/users/me/password', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({
        currentPassword: pwdForm.value.current,
        newPassword: pwdForm.value.new
      })
    });
    
    const data = await res.json();
    
    if (!res.ok) {
      throw new Error(data.error || 'Failed to update password');
    }
    
    pwdSuccess.value = 'Password updated successfully';
    pwdForm.value = { current: '', new: '', confirm: '' };
    soundFx.success();
    setTimeout(() => { pwdSuccess.value = ''; }, 3000);
  } catch (error: any) {
    pwdError.value = error.message;
    soundFx.error();
  } finally {
    isChangingPwd.value = false;
  }
};

const handleLogout = () => {
  authStore.logout();
};

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'long', day: 'numeric' }).format(date);
};

const MASTER_BADGES = [
  { id: 'early_bird', name: 'Dawn Guardian', desc: 'Completed a task before 7 AM.', icon: Sun, color: 'text-amber-400', bg: 'bg-amber-400/10', border: 'border-amber-400/30' },
  { id: 'resilience_init', name: 'Composter', desc: 'Used the "Compost" feature to process a setback.', icon: Shield, color: 'text-emerald-400', bg: 'bg-emerald-400/10', border: 'border-emerald-400/30' },
  { id: 'flow_master', name: 'Zen Master', desc: 'Reached 100 Flow EXP in a single week.', icon: Heart, color: 'text-blue-400', bg: 'bg-blue-400/10', border: 'border-blue-400/30' },
  { id: 'social_butterfly', name: 'Resonant Echo', desc: 'Completed 5 social tasks.', icon: Milestone, color: 'text-purple-400', bg: 'bg-purple-400/10', border: 'border-purple-400/30' },
  { id: 'streak_7', name: 'Unbroken Thread', desc: 'Maintained a 7-day logging streak.', icon: Flame, color: 'text-red-400', bg: 'bg-red-400/10', border: 'border-red-400/30' },
];

const checkUnlocked = (id: string) => {
  return boardStore.stats.unlockedBadges?.includes(id) || false;
};

onMounted(() => {
  fetchProfile();
  if (boardStore.cards.length === 0) {
    boardStore.fetchStats();
  }
});
</script>

<template>
  <div class="max-w-3xl mx-auto animate-fade-in-up">
    <div class="flex justify-between items-center mb-8">
      <h2 class="text-2xl font-serif text-ink">{{ t('nav.profile') }}</h2>
      <button 
        @click="handleLogout"
        class="flex items-center gap-2 px-4 py-2 rounded-lg border border-negative-tint/30 text-negative-tint hover:bg-negative-tint/10 transition-colors text-sm font-medium"
      >
        <LogOut :size="16" />
        Sign Out
      </button>
    </div>

    <div v-if="isLoading" class="flex justify-center items-center py-20">
      <Loader2 class="animate-spin text-accent-glow" :size="32" />
    </div>

    <div v-else class="space-y-8">
      <!-- Messages -->
      <div v-if="errorMessage" class="p-4 bg-negative-tint/10 border border-negative-tint/30 text-negative-tint text-sm rounded-xl">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="p-4 bg-accent-glow/10 border border-accent-glow/30 text-accent-glow text-sm rounded-xl">
        {{ successMessage }}
      </div>

      <!-- Profile Card -->
      <div class="bg-bg-surface border border-ink-dim/10 rounded-2xl overflow-hidden shadow-sm">
        <!-- Header / Cover -->
        <div class="h-32 bg-gradient-to-r from-accent-glow/20 to-bg-surface relative">
          <div class="absolute -bottom-12 left-8">
            <div class="w-24 h-24 rounded-full bg-bg border-4 border-bg-surface flex items-center justify-center text-accent-glow overflow-hidden shadow-sm">
              <User :size="40" />
            </div>
          </div>
          
          <div class="absolute top-4 right-4">
            <button 
              v-if="!isEditing"
              @click="isEditing = true"
              class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-bg/50 backdrop-blur-sm border border-ink-dim/20 hover:border-accent-glow/50 hover:text-accent-glow transition-colors text-sm"
            >
              <Edit2 :size="14" />
              Edit Profile
            </button>
            <div v-else class="flex gap-2">
              <button 
                @click="isEditing = false"
                class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-bg/50 backdrop-blur-sm border border-ink-dim/20 hover:border-ink-dim hover:text-ink-dim transition-colors text-sm"
              >
                <X :size="14" />
                Cancel
              </button>
              <button 
                @click="saveProfile"
                :disabled="isSaving"
                class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-accent-glow text-bg hover:bg-accent-glow/90 transition-colors text-sm font-medium disabled:opacity-70"
              >
                <Loader2 v-if="isSaving" class="animate-spin" :size="14" />
                <Save v-else :size="14" />
                Save
              </button>
            </div>
          </div>
        </div>

        <!-- Body -->
        <div class="pt-16 p-8">
          <div v-if="!isEditing" class="space-y-6">
            <div>
              <h3 class="text-2xl font-serif font-medium text-ink">{{ profile.name }}</h3>
              <p class="text-ink-dim mt-2 leading-relaxed max-w-2xl">{{ profile.bio }}</p>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-6 border-t border-ink-dim/10">
              <div class="flex items-center gap-3 text-ink-dim">
                <Mail :size="18" />
                <span>{{ profile.email }}</span>
              </div>
              <div class="flex items-center gap-3 text-ink-dim">
                <Calendar :size="18" />
                <span>Joined {{ formatDate(profile.joinedAt) }}</span>
              </div>
            </div>
          </div>

          <div v-else class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">Display Name</label>
              <input 
                v-model="editForm.name" 
                type="text" 
                class="block w-full bg-black/20 border border-ink-dim/20 rounded-lg px-4 py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
                placeholder="Your name" 
              />
            </div>
            
            <div>
              <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">Bio</label>
              <textarea 
                v-model="editForm.bio" 
                rows="4"
                class="block w-full bg-black/20 border border-ink-dim/20 rounded-lg px-4 py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none resize-none" 
                placeholder="Tell us about yourself..." 
              ></textarea>
            </div>
            
            <div class="pt-6 border-t border-ink-dim/10">
              <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">Email Address (Read Only)</label>
              <input 
                :value="profile.email" 
                type="email" 
                disabled
                class="block w-full bg-black/40 border border-ink-dim/10 rounded-lg px-4 py-2.5 text-sm text-ink-dim cursor-not-allowed" 
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Achievements & Badges -->
      <div class="bg-bg-surface border border-ink-dim/10 rounded-2xl overflow-hidden shadow-sm p-8">
        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-accent-glow/10 flex items-center justify-center text-accent-glow">
              <Award :size="20" />
            </div>
            <div>
              <h3 class="text-lg font-serif font-medium text-ink">Achievements</h3>
              <p class="text-xs text-ink-dim mt-1">Unlock badges by growing your Tree of Logos.</p>
            </div>
          </div>
          <div class="text-sm font-mono text-ink-dim">
            {{ boardStore.stats.unlockedBadges?.length || 0 }} / {{ MASTER_BADGES.length }} Unlocked
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div 
            v-for="badge in MASTER_BADGES" 
            :key="badge.id"
            class="relative p-4 rounded-xl border transition-all duration-300 flex flex-col items-center text-center group overflow-hidden"
            :class="checkUnlocked(badge.id) ? `${badge.bg} ${badge.border}` : 'bg-black/20 border-ink-dim/10 opacity-60 grayscale'"
          >
            <div 
              class="w-12 h-12 rounded-full flex items-center justify-center mb-3 shadow-sm transition-transform duration-500 group-hover:scale-110"
              :class="checkUnlocked(badge.id) ? `bg-bg-surface border ${badge.border} ${badge.color}` : 'bg-black/40 text-ink-dim'"
            >
              <component :is="badge.icon" :size="20" />
            </div>
            <h4 class="font-serif text-sm font-medium mb-1" :class="checkUnlocked(badge.id) ? 'text-ink' : 'text-ink-dim'">{{ badge.name }}</h4>
            <p class="text-[10px] text-ink-dim leading-relaxed">{{ badge.desc }}</p>
            
            <div v-if="!checkUnlocked(badge.id)" class="absolute top-2 right-2 text-ink-dim/50">
              <Lock :size="12" />
            </div>
          </div>
        </div>
      </div>

      <!-- Change Password Section -->
      <div class="bg-bg-surface border border-ink-dim/10 rounded-2xl overflow-hidden shadow-sm p-8">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-10 h-10 rounded-full bg-ink-dim/10 flex items-center justify-center text-ink">
            <Lock :size="20" />
          </div>
          <div>
            <h3 class="text-lg font-serif font-medium text-ink">Change Password</h3>
            <p class="text-xs text-ink-dim mt-1">Ensure your account is using a long, random password to stay secure.</p>
          </div>
        </div>

        <div v-if="pwdError" class="mb-6 p-3 bg-negative-tint/10 border border-negative-tint/30 text-negative-tint text-sm rounded-xl">
          {{ pwdError }}
        </div>
        <div v-if="pwdSuccess" class="mb-6 p-3 bg-accent-glow/10 border border-accent-glow/30 text-accent-glow text-sm rounded-xl">
          {{ pwdSuccess }}
        </div>

        <form @submit.prevent="changePassword" class="space-y-5 max-w-md">
          <div>
            <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">Current Password</label>
            <input 
              v-model="pwdForm.current" 
              type="password" 
              required 
              class="block w-full bg-black/20 border border-ink-dim/20 rounded-lg px-4 py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">New Password</label>
            <input 
              v-model="pwdForm.new" 
              type="password" 
              required 
              minlength="6" 
              class="block w-full bg-black/20 border border-ink-dim/20 rounded-lg px-4 py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-ink-dim uppercase tracking-wider text-[10px] mb-2">Confirm New Password</label>
            <input 
              v-model="pwdForm.confirm" 
              type="password" 
              required 
              minlength="6" 
              class="block w-full bg-black/20 border border-ink-dim/20 rounded-lg px-4 py-2.5 text-sm text-ink focus:ring-1 focus:ring-accent-glow focus:border-accent-glow transition-colors outline-none" 
            />
          </div>
          <div class="pt-2">
            <button 
              type="submit" 
              :disabled="isChangingPwd" 
              class="flex items-center justify-center gap-2 w-full py-2.5 px-4 rounded-lg bg-ink-dim/10 text-ink hover:bg-ink-dim/20 transition-colors text-sm font-medium disabled:opacity-70"
            >
              <Loader2 v-if="isChangingPwd" class="animate-spin" :size="16" />
              <Lock v-else :size="16" />
              Update Password
            </button>
          </div>
        </form>
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
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}
</style>
