<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { NoteEditorForm, ReadingPlanItem } from '@/types/readplan';

const props = defineProps<{
  loading: boolean;
  modelValue: boolean;
  availablePlans: ReadingPlanItem[];
  initialValue?: NoteEditorForm | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  submit: [payload: NoteEditorForm];
}>();

const form = reactive<NoteEditorForm>({
  bookId: '',
  title: '',
  content: '',
});

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return;
    }

    form.bookId = props.initialValue?.bookId ?? props.availablePlans[0]?.book.id ?? '';
    form.title = props.initialValue?.title ?? '';
    form.content = props.initialValue?.content ?? '';
  },
  { immediate: true },
);

const handleSubmit = () => {
  emit('submit', {
    bookId: form.bookId,
    title: form.title.trim(),
    content: form.content.trim(),
  });
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="initialValue ? '编辑笔记' : '写读书笔记'"
    width="620px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="所属书籍">
        <el-select v-model="form.bookId" style="width: 100%">
          <el-option
            v-for="plan in availablePlans"
            :key="plan.id"
            :label="plan.book.title"
            :value="plan.book.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="80" placeholder="例如：第一章感悟" show-word-limit />
      </el-form-item>
      <el-form-item label="内容">
        <el-input
          v-model="form.content"
          :rows="8"
          maxlength="2000"
          placeholder="可以直接填写 Markdown 文本。"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button :loading="loading" type="primary" @click="handleSubmit">保存笔记</el-button>
    </template>
  </el-dialog>
</template>
