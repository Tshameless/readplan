<script setup lang="ts">
import NoteCommentPanel from '@/components/comment/NoteCommentPanel.vue';
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
      <p class="note-card__content">{{ note.contentPreview }}</p>
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
  border-radius: 22px;
  background: rgb(255 255 255 / 86%);
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
</style>
