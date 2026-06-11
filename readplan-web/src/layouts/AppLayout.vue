<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { LOGIN_PATH } from '@/constants/auth';
import { ROUTE_NAMES } from '@/constants/routeNames';
import { constantRoutes } from '@/router/routes';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const permissionStore = usePermissionStore();
const userStore = useUserStore();

const menuItems = computed(() => {
  const rootRoute = constantRoutes.find((item) => item.path === '/');

  return (rootRoute?.children ?? []).filter((item) => {
    if (item.meta?.hidden) {
      return false;
    }

    if (item.meta?.requiresAuth && !userStore.isAuthenticated) {
      return false;
    }

    return permissionStore.hasPermission(item.meta?.requiredPermission);
  });
});

const displayName = computed(() => userStore.profile?.nickname ?? '游客');

const handleLogout = async () => {
  userStore.clearSession();
  permissionStore.clearPermissions();
  await router.push(LOGIN_PATH);
};
</script>

<template>
  <div class="app-shell">
    <header class="shell-header">
      <RouterLink class="shell-brand" :to="{ name: ROUTE_NAMES.home }">ReadPlan</RouterLink>

      <el-menu class="shell-menu" mode="horizontal" router>
        <el-menu-item v-for="item in menuItems" :key="item.name" :index="`/${item.path}`">
          {{ item.meta?.title }}
        </el-menu-item>
      </el-menu>

      <div class="shell-actions">
        <template v-if="userStore.isAuthenticated">
          <el-tag round>{{ displayName }}</el-tag>
          <el-button link type="primary" @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <RouterLink :to="{ name: ROUTE_NAMES.login }">登录</RouterLink>
          <RouterLink :to="{ name: ROUTE_NAMES.register }">注册</RouterLink>
        </template>
      </div>
    </header>

    <main class="shell-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
}

.shell-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--page-border);
  background: rgb(255 255 255 / 76%);
  backdrop-filter: blur(18px);
}

.shell-brand {
  color: #7a4d11;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.shell-menu {
  min-width: 0;
  border-bottom: 0;
  background: transparent;
}

.shell-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell-main {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 40px;
}

@media (width <= 760px) {
  .shell-header {
    grid-template-columns: 1fr;
  }

  .shell-main {
    width: min(100% - 24px, 1200px);
  }
}
</style>
