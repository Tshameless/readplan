import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { PROFILE_STORAGE_KEY } from '@/constants/auth';
import type { UserProfile } from '@/types/auth';
import { clearAuthToken, getAuthToken, setAuthToken } from '@/utils/auth';

const getStoredProfile = (): UserProfile | null => {
  const rawProfile = localStorage.getItem(PROFILE_STORAGE_KEY);

  if (!rawProfile) {
    return null;
  }

  try {
    return JSON.parse(rawProfile) as UserProfile;
  } catch {
    localStorage.removeItem(PROFILE_STORAGE_KEY);
    return null;
  }
};

export const useUserStore = defineStore('user', () => {
  const token = ref(getAuthToken());
  const profile = ref<UserProfile | null>(getStoredProfile());

  const isAuthenticated = computed(() => Boolean(token.value));

  const setSession = (nextToken: string, nextProfile: UserProfile) => {
    token.value = nextToken;
    profile.value = nextProfile;
    setAuthToken(nextToken);
    localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(nextProfile));
  };

  const clearSession = () => {
    token.value = '';
    profile.value = null;
    clearAuthToken();
    localStorage.removeItem(PROFILE_STORAGE_KEY);
  };

  return {
    token,
    profile,
    isAuthenticated,
    setSession,
    clearSession,
  };
});
