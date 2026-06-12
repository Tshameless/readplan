<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { deleteReadingPlan, getMyReadingPlans, updateReadingPlanStatus } from '@/api/plan/plan';
import AppPage from '@/components/common/AppPage.vue';
import PlanStatusTag from '@/components/plan/PlanStatusTag.vue';
import type { ReadingPlanItem } from '@/types/readplan';

const loading = shallowRef(false);
const updatingPlanId = shallowRef('');
const deletingPlanId = shallowRef('');
const plans = ref<ReadingPlanItem[]>([]);

const loadPlans = async () => {
  loading.value = true;

  try {
    plans.value = await getMyReadingPlans();
  } finally {
    loading.value = false;
  }
};

const handleStatusChange = async (planId: string, status: 0 | 1 | 2) => {
  updatingPlanId.value = planId;

  try {
    const updatedPlan = await updateReadingPlanStatus(planId, status);
    plans.value = plans.value.map((item) => (item.id === planId ? updatedPlan : item));
    ElMessage.success('阅读状态已更新。');
  } finally {
    updatingPlanId.value = '';
  }
};

const handleDeletePlan = async (planId: string) => {
  await ElMessageBox.confirm('移出后该书会从你的阅读计划中消失，确定继续吗？', '移除计划', {
    type: 'warning',
  });

  deletingPlanId.value = planId;
  try {
    await deleteReadingPlan(planId);
    plans.value = plans.value.filter((item) => item.id !== planId);
    ElMessage.success('阅读计划已移除。');
  } finally {
    deletingPlanId.value = '';
  }
};

onMounted(() => {
  void loadPlans();
});
</script>

<template>
  <AppPage title="我的阅读计划" description="这里预留了阅读状态切换、软删除展示和写笔记前置校验位置。">
    <el-table :data="plans" border :loading="loading">
      <el-table-column label="书籍" min-width="240">
        <template #default="{ row }">
          <div class="plan-book">
            <img :alt="row.book.title" :src="row.book.cover" class="plan-book__cover" />
            <div>
              <strong>{{ row.book.title }}</strong>
              <p>{{ row.book.author }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="阅读状态" min-width="120">
        <template #default="{ row }">
          <div class="plan-status-cell">
            <PlanStatusTag :status="row.status" />
            <el-select
              :disabled="updatingPlanId === row.id"
              :model-value="row.status"
              size="small"
              style="width: 110px"
              @change="handleStatusChange(row.id, $event)"
            >
              <el-option :value="0" label="未开始" />
              <el-option :value="1" label="阅读中" />
              <el-option :value="2" label="已读完" />
            </el-select>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="书籍状态" min-width="120">
        <template #default="{ row }">
          <el-tag :type="row.shelfState === 'ACTIVE' ? 'success' : 'danger'" round>
            {{ row.shelfState === 'ACTIVE' ? '正常展示' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="写笔记条件" min-width="160">
        <template #default="{ row }">
          <el-tag :type="row.allowNote ? 'success' : 'info'" effect="plain">
            {{ row.allowNote ? '允许写笔记' : '需先开始阅读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近更新时间" min-width="180" prop="updatedAt" />
      <el-table-column label="操作" min-width="120">
        <template #default="{ row }">
          <el-button :loading="deletingPlanId === row.id" link type="danger" @click="handleDeletePlan(row.id)">
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </AppPage>
</template>

<style scoped lang="scss">
.plan-book {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plan-book__cover {
  width: 52px;
  height: 72px;
  border-radius: 4px;
  object-fit: cover;
}

.plan-book p {
  margin: 6px 0 0;
  color: var(--page-muted);
}

.plan-status-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
