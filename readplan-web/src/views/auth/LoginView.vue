<script setup lang="ts">
import { reactive, shallowRef } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { loginWithPassword } from '@/api/auth/auth';
import { ROUTE_NAMES } from '@/constants/routeNames';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';
import type { LoginForm } from '@/types/auth';

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();
const userStore = useUserStore();

const form = reactive<LoginForm>({
  username: 'reader',
  password: '123456',
});

const loading = shallowRef(false);

const handleSubmit = async () => {
  loading.value = true;

  try {
    const result = await loginWithPassword(form);
    userStore.setSession(result.token, result.user);
    permissionStore.setPermissions(result.user.permissions);
    ElMessage.success('登录成功');

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : undefined;
    await router.push(redirect ?? { name: ROUTE_NAMES.home });
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="auth-page">
    <section class="auth-copy">
      <p class="auth-copy__eyebrow">ReadPlan</p>
      <h1>把阅读计划、笔记和讨论放进一个系统里。</h1>
      <p class="auth-copy__body">
        当前是前端业务骨架，已经接好登录态、权限守卫和业务路由。你可以先用演示账号进入不同角色页面。
      </p>
      <el-space wrap>
        <el-tag>普通用户：reader / 123456</el-tag>
        <el-tag type="warning">管理员：admin / 123456</el-tag>
      </el-space>
    </section>

    <el-card class="auth-card" shadow="never">
      <template #header>
        <div class="auth-card__header">
          <div>
            <h2>登录</h2>
            <p>进入个人阅读计划与笔记系统。</p>
          </div>
          <RouterLink :to="{ name: ROUTE_NAMES.register }">去注册</RouterLink>
        </div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="reader 或 admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-button :loading="loading" class="auth-card__submit" type="primary" @click="handleSubmit">
          登录系统
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 420px);
  min-height: 100vh;
  gap: 32px;
  align-items: center;
  padding: 48px;
}

.auth-copy {
  max-width: 620px;
}

.auth-copy__eyebrow {
  margin: 0 0 14px;
  color: #7a4d11;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.auth-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.06;
}

.auth-copy__body {
  margin: 18px 0 24px;
  max-width: 560px;
  color: var(--page-muted);
  font-size: 16px;
  line-height: 1.7;
}

.auth-card {
  border: 1px solid var(--page-border);
  border-radius: 8px;
  background: var(--page-surface);
  box-shadow: var(--page-shadow);
  backdrop-filter: blur(18px);
}

.auth-card__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.auth-card__header h2 {
  margin: 0;
  font-size: 28px;
}

.auth-card__header p {
  margin: 8px 0 0;
  color: var(--page-muted);
}

.auth-card__submit {
  width: 100%;
  margin-top: 8px;
}

@media (width <= 900px) {
  .auth-page {
    grid-template-columns: 1fr;
    padding: 24px;
  }
}
</style>
