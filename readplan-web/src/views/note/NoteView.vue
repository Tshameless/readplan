<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createNote, deleteNote, getMyNotes, updateNote } from '@/api/note/note';
import { getMyReadingPlans } from '@/api/plan/plan';
import AppPage from '@/components/common/AppPage.vue';
import NoteEditorDialog from '@/components/note/NoteEditorDialog.vue';
import type { NoteEditorForm, ReadingPlanItem, UserNoteSummary } from '@/types/readplan';

const loading = shallowRef(false);
const saving = shallowRef(false);
const deletingId = shallowRef('');
const editorOpen = shallowRef(false);
const notes = ref<UserNoteSummary[]>([]);
const availablePlans = ref<ReadingPlanItem[]>([]);
const editingNote = shallowRef<UserNoteSummary | null>(null);

const writablePlans = computed(() => availablePlans.value.filter((plan) => plan.allowNote));

const loadPageData = async () => {
  loading.value = true;

  try {
    const [noteList, plans] = await Promise.all([getMyNotes(), getMyReadingPlans()]);
    notes.value = noteList;
    availablePlans.value = plans;
  } finally {
    loading.value = false;
  }
};

const openCreateDialog = () => {
  if (writablePlans.value.length === 0) {
    ElMessage.warning('请先在阅读计划中将书籍标记为阅读中或已读完。');
    return;
  }

  editingNote.value = null;
  editorOpen.value = true;
};

const openEditDialog = (note: UserNoteSummary) => {
  editingNote.value = note;
  editorOpen.value = true;
};

const handleSaveNote = async (payload: NoteEditorForm) => {
  if (!payload.bookId || !payload.title || !payload.content) {
    ElMessage.warning('请填写完整笔记信息。');
    return;
  }

  saving.value = true;

  try {
    if (editingNote.value) {
      const updatedNote = await updateNote(editingNote.value.id, payload);
      notes.value = notes.value.map((item) => (item.id === editingNote.value?.id ? updatedNote : item));
      ElMessage.success('笔记已更新。');
    } else {
      const createdNote = await createNote(payload);
      notes.value = [createdNote, ...notes.value];
      ElMessage.success('笔记已创建。');
    }

    editorOpen.value = false;
  } finally {
    saving.value = false;
  }
};

const handleDeleteNote = async (noteId: string) => {
  await ElMessageBox.confirm('删除后不可恢复，确定继续吗？', '删除笔记', {
    type: 'warning',
  });

  deletingId.value = noteId;

  try {
    await deleteNote(noteId);
    notes.value = notes.value.filter((item) => item.id !== noteId);
    ElMessage.success('笔记已删除。');
  } finally {
    deletingId.value = '';
  }
};

onMounted(() => {
  void loadPageData();
});
</script>

<template>
  <AppPage title="我的笔记" description="已接通真实后端接口，可直接创建、编辑和删除个人读书笔记。">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">写笔记</el-button>
    </template>

    <el-alert
      v-if="writablePlans.length === 0"
      :closable="false"
      show-icon
      title="当前还没有可写笔记的书籍"
      type="info"
      description="先去“我的阅读计划”把书籍标记为“阅读中”或“已读完”，再回来写笔记。"
    />

    <div v-loading="loading" class="note-stack">
      <article v-for="note in notes" :key="note.id" class="note-card">
        <div class="note-card__header">
          <div>
            <el-tag effect="plain">{{ note.bookTitle }}</el-tag>
            <h2>{{ note.title }}</h2>
          </div>
          <span>{{ note.createdAt }}</span>
        </div>
        <p>{{ note.excerpt }}</p>
        <div class="note-card__footer">
          <span>评论 {{ note.commentCount }}</span>
          <div class="note-card__actions">
            <el-button link type="primary" @click="openEditDialog(note)">编辑</el-button>
            <el-button :loading="deletingId === note.id" link type="danger" @click="handleDeleteNote(note.id)">
              删除
            </el-button>
          </div>
        </div>
      </article>

      <el-empty v-if="!loading && notes.length === 0" description="你还没有写过笔记。" />
    </div>

    <NoteEditorDialog
      v-model="editorOpen"
      :available-plans="writablePlans"
      :initial-value="editingNote ? { bookId: editingNote.bookId, title: editingNote.title, content: editingNote.excerpt } : null"
      :loading="saving"
      @submit="handleSaveNote"
    />
  </AppPage>
</template>

<style scoped lang="scss">
.note-stack {
  display: grid;
  gap: 16px;
}

.note-card {
  border: 1px solid var(--page-border);
  border-radius: 24px;
  background: rgb(255 255 255 / 88%);
  padding: 22px;
  box-shadow: var(--page-shadow);
}

.note-card__header,
.note-card__footer,
.note-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.note-card__header h2 {
  margin: 12px 0 0;
  font-size: 22px;
}

.note-card__header span,
.note-card p,
.note-card__footer span {
  color: var(--page-muted);
}

.note-card p {
  margin: 16px 0;
  line-height: 1.7;
}
</style>
