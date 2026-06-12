<script setup lang="ts">
import NoteCommentPanel from '@/components/comment/NoteCommentPanel.vue';
import { renderMarkdown } from '@/utils/markdown';
import type { PublicNote } from '@/types/readplan';

defineProps<{
  notes: PublicNote[];
}>();
</script>

<template>
  <div class="note-list">
    <article v-for="note in notes" :key="note.id" class="note-card">
      <div class="note-card__header">
        <div>
          <h3>{{ note.title }}</h3>
          <p>{{ note.authorName }} · {{ note.createdAt }}</p>
        </div>
        <el-tag type="warning">评论 {{ note.commentCount }}</el-tag>
      </div>
      <div class="note-card__content markdown-content" v-html="renderMarkdown(note.contentPreview)"></div>
      <NoteCommentPanel :note-id="note.id" />
    </article>

    <el-empty v-if="notes.length === 0" description="当前书籍下还没有公开笔记。" />
  </div>
</template>

<style scoped lang="scss">
.note-list {
  display: grid;
  gap: 16px;
}

.note-card {
  border: 1px solid var(--page-border);
  border-radius: 8px;
  background: var(--page-surface);
  padding: 20px;
}

.note-card__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.note-card__header h3 {
  margin: 0;
  font-size: 18px;
}

.note-card__header p,
.note-card__content {
  margin: 8px 0 0;
  color: var(--page-muted);
  line-height: 1.7;
}

.markdown-content {
  margin: 12px 0;
  color: #374151;
  font-size: 15px;

  :deep(p) {
    margin: 6px 0;
  }
}
</style>
