import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { PROFILE_STORAGE_KEY } from '@/constants/auth';
import type { UserProfile } from '@/types/auth';
import { hasRequiredPermission } from '@/utils/permission';

const getStoredPermissions = () => {
  const rawProfile = localStorage.getItem(PROFILE_STORAGE_KEY);

  if (!rawProfile) {
    return [];
  }

  try {
    return (JSON.parse(rawProfile) as UserProfile).permissions ?? [];
  } catch {
    return [];
  }
};

export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref<string[]>(getStoredPermissions());

  const setPermissions = (nextPermissions: string[]) => {
    permissions.value = nextPermissions;
  };

  const clearPermissions = () => {
    permissions.value = [];
  };

  const hasPermission = (requiredPermission?: string | string[]) =>
    hasRequiredPermission(permissions.value, requiredPermission);

  const permissionCount = computed(() => permissions.value.length);

  return {
    permissions,
    permissionCount,
    setPermissions,
    clearPermissions,
    hasPermission,
  };
});
