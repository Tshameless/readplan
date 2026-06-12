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
  timeout: 20000,
});

request.interceptors.request.use((config) => {
  const token = getAuthToken();

  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

const handleAuthError = async (code: number, msg: string) => {
  if (code === 401) {
    clearAuthToken();
    useUserStore(pinia).clearSession();
    usePermissionStore(pinia).clearPermissions();
    message.warning(msg || '登录已失效，请重新登录。');
    if (router.currentRoute.value.path !== LOGIN_PATH) {
      await router.push(LOGIN_PATH);
    }
  } else if (code === 403) {
    message.warning(msg || '当前账号没有访问权限。');
    await router.push({ name: 'Forbidden' });
  } else {
    message.error(msg || '请求失败，请稍后重试。');
  }
};

request.interceptors.response.use(
  async (response) => {
    const res = response.data;
    if (res && typeof res.code === 'number' && res.code !== 200) {
      await handleAuthError(res.code, res.message);
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return response;
  },
  async (error) => {
    const status = error.response?.status as number | undefined;
    const backendMessage = error.response?.data?.message as string | undefined;

    await handleAuthError(status || 500, backendMessage || error.message);
    return Promise.reject(error);
  },
);

export default request;
