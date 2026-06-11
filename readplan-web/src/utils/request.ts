import axios from 'axios';
import { LOGIN_PATH } from '@/constants/auth';
import router from '@/router';
import { pinia } from '@/stores';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';
import { clearAuthToken, getAuthToken } from '@/utils/auth';
import { message } from '@/utils/message';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  timeout: 8000,
});

request.interceptors.request.use((config) => {
  const token = getAuthToken();

  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

request.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status as number | undefined;
    const backendMessage = error.response?.data?.message as string | undefined;

    if (status === 401) {
      clearAuthToken();
      useUserStore(pinia).clearSession();
      usePermissionStore(pinia).clearPermissions();
      message.warning(backendMessage ?? '登录已失效，请重新登录。');
      if (router.currentRoute.value.path !== LOGIN_PATH) {
        await router.push(LOGIN_PATH);
      }
      return Promise.reject(error);
    }

    if (status === 403) {
      message.warning(backendMessage ?? '当前账号没有访问权限。');
      await router.push({ name: 'Forbidden' });
      return Promise.reject(error);
    }

    if (status === 404) {
      message.error(backendMessage ?? '接口不存在，请检查请求地址。');
      return Promise.reject(error);
    }

    message.error(backendMessage ?? error.message ?? '请求失败，请稍后重试。');
    return Promise.reject(error);
  },
);

export default request;
