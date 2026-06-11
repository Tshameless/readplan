<script setup lang="ts">
import { computed } from 'vue';
import type { ReadingStatus } from '@/types/readplan';

const props = defineProps<{
  status: ReadingStatus;
}>();

const statusMeta = computed(() => {
  if (props.status === 2) {
    return { label: '已读完', type: 'success' as const };
  }

  if (props.status === 1) {
    return { label: '阅读中', type: 'warning' as const };
  }

  return { label: '未开始', type: 'info' as const };
});
</script>

<template>
  <el-tag :type="statusMeta.type" round>
    {{ statusMeta.label }}
  </el-tag>
</template>
