import type { Router } from 'vue-router';
import { LOGIN_PATH } from '@/constants/auth';
import { ROUTE_NAMES } from '@/constants/routeNames';
import { pinia } from '@/stores';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';

export const applyRouterGuards = (router: Router) => {
  router.beforeEach((to) => {
    const userStore = useUserStore(pinia);
    const permissionStore = usePermissionStore(pinia);
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true);

    if (
      (to.name === ROUTE_NAMES.login || to.name === ROUTE_NAMES.register) &&
      userStore.isAuthenticated
    ) {
      return { name: ROUTE_NAMES.home };
    }

    if (requiresAuth && !userStore.isAuthenticated) {
      return {
        path: LOGIN_PATH,
        query: {
          redirect: to.fullPath,
        },
      };
    }

    if (to.meta.requiredPermission && !permissionStore.hasPermission(to.meta.requiredPermission)) {
      return { name: ROUTE_NAMES.forbidden };
    }

    return true;
  });
};
