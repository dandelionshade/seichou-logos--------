import { useToastStore } from '@/store/toastStore';

const BASE_URL = '/api';

export async function apiFetch(endpoint: string, options: RequestInit = {}) {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...((options.headers as any) || {}),
  };

  try {
    const url = `${BASE_URL}${endpoint}`;
    console.log(`[API] Fetching: ${url}`, options.method || 'GET');
    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const errorMsg = errorData.error || `API Error: ${response.status}`;
      
      // Hook into the toast store
      const toastStore = useToastStore();
      if (response.status === 401) {
        toastStore.addToast("Session expired. Please log in again.", "error");
      } else {
        toastStore.addToast(errorMsg, "error");
      }
      
      throw new Error(errorMsg);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    return response.text();
  } catch (err: any) {
    if (err.name === 'TypeError' && err.message === 'Failed to fetch') {
      console.error(`Network Error fetching ${endpoint}. Is the server running?`);
      const toastStore = useToastStore();
      toastStore.addToast(`Network Error connecting to server. Please check your connection.`, 'error');
    }
    throw err;
  }
}
