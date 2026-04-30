import axios from 'axios';

const STORAGE_KEY = 'buildsmart.token';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8081';

const apiClient = axios.create({ baseURL: API_BASE, timeout: 15000 });

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(STORAGE_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

// Every service is reached through the same API gateway. The named exports
// are kept so feature modules read clearly ("iamClient.post(...)") and so
// each domain can be re-pointed independently if you ever bypass the gateway.
export const iamClient = apiClient;
export const projectClient = apiClient;
export const resourceClient = apiClient;
export const safetyClient = apiClient;
export const financeClient = apiClient;
export const vendorClient = apiClient;
export const siteopsClient = apiClient;
export const reportClient = apiClient;
export const notificationClient = apiClient;

export default apiClient;
