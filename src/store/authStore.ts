import { defineStore } from 'pinia';
import { apiFetch } from '../api';
import router from '../router';

interface User {
  id: string;
  email: string;
  name?: string;
  bio?: string;
}

export const useAuthStore = defineStore('authStore', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: null as User | null,
    loading: false,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
  },
  actions: {
    async register(email: string, password: string) {
      this.loading = true;
      try {
        const data = await apiFetch('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        });
        this.setAuth(data.token, data.user);
        return data;
      } finally {
        this.loading = false;
      }
    },
    async login(email: string, password: string) {
      this.loading = true;
      try {
        const data = await apiFetch('/auth/login', { 
          method: 'POST',
          body: JSON.stringify({ email, password }),
        });
        this.setAuth(data.token, data.user);
        return data;
      } finally {
        this.loading = false;
      }
    },
    async fetchMe() {
      if (!this.token) return;
      try {
        const user = await apiFetch('/users/me');
        this.user = user;
      } catch (e) {
        this.logout();
      }
    },
    async updateProfile(updates: Partial<User>) {
      const user = await apiFetch('/users/me', {
        method: 'PUT',
        body: JSON.stringify(updates),
      });
      this.user = user;
    },
    setAuth(token: string, user: User) {
      this.token = token;
      this.user = user;
      localStorage.setItem('token', token);
    },
    logout() {
      this.token = null;
      this.user = null;
      localStorage.removeItem('token');
      router.push('/auth');
    }
  }
});
