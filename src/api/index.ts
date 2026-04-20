import { useToastStore } from '@/store/toastStore';

const BASE_URL = '/api';

export interface ApiFetchOptions extends RequestInit {
  silentError?: boolean;
}

export async function apiFetch(endpoint: string, options: ApiFetchOptions = {}) {
  const { silentError = false, ...requestOptions } = options;
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...((requestOptions.headers as any) || {}),
  };

  try {
    const url = `${BASE_URL}${endpoint}`;
    console.log(`[API] Fetching: ${url}`, requestOptions.method || 'GET');
    const response = await fetch(url, {
      ...requestOptions,
      headers,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const errorMsg = errorData.error || `API Error: ${response.status}`;
      
      // Hook into the toast store
      if (!silentError) {
        const toastStore = useToastStore();
        if (response.status === 401) {
          toastStore.addToast("Session expired. Please log in again.", "error");
        } else {
          toastStore.addToast(errorMsg, "error");
        }
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
