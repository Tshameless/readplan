<script setup lang="ts">
import { reactive, shallowRef } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { registerAccount } from '@/api/auth/auth';
import { ROUTE_NAMES } from '@/constants/routeNames';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';
import type { RegisterForm } from '@/types/auth';

const router = useRouter();
const permissionStore = usePermissionStore();
const userStore = useUserStore();

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
});

const loading = shallowRef(false);

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写完整注册信息。');
    return;
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致。');
    return;
  }

  loading.value = true;

  try {
    const result = await registerAccount(form);
    userStore.setSession(result.token, result.user);
    permissionStore.setPermissions(result.user.permissions);
    ElMessage.success('注册成功，已自动登录。');
    await router.push({ name: ROUTE_NAMES.home });
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="register-page">
    <el-card class="register-card" shadow="never">
      <template #header>
        <div class="register-card__header">
          <div>
            <h1>创建账号</h1>
            <p>注册后即可加入阅读计划、写笔记和参与评论。</p>
          </div>
          <RouterLink :to="{ name: ROUTE_NAMES.login }">返回登录</RouterLink>
        </div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            show-password
            type="password"
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <el-button :loading="loading" class="register-card__submit" type="primary" @click="handleSubmit">
          完成注册
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.register-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
}

.register-card {
  width: min(100%, 460px);
  border: 1px solid var(--page-border);
  border-radius: 8px;
  background: var(--page-surface);
  box-shadow: var(--page-shadow);
}

.register-card__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.register-card__header h1 {
  margin: 0;
  font-size: 30px;
}

.register-card__header p {
  margin: 8px 0 0;
  color: var(--page-muted);
}

.register-card__submit {
  width: 100%;
}
</style>
