<script setup lang="ts">
import type { BookSummary } from '@/types/readplan';

defineProps<{
  books: BookSummary[];
}>();

const emit = defineEmits<{
  select: [bookId: string];
}>();
</script>

<template>
  <div class="book-grid">
    <article v-for="book in books" :key="book.id" class="book-card">
      <img :alt="book.title" :src="book.cover" class="book-card__cover" />

      <div class="book-card__body">
        <div class="book-card__meta">
          <span>{{ book.author }}</span>
          <span>{{ book.publishYear }}</span>
        </div>
        <h3>{{ book.title }}</h3>
        <p>{{ book.description }}</p>

        <div class="book-card__tags">
          <el-tag v-for="tag in book.tags" :key="tag" effect="plain" round>
            {{ tag }}
          </el-tag>
        </div>

        <div class="book-card__actions">
          <el-tag :type="book.imported ? 'success' : 'info'">
            {{ book.imported ? '已入库' : '待导入' }}
          </el-tag>
          <el-button link type="primary" @click="emit('select', book.id)">查看详情</el-button>
        </div>
      </div>
    </article>
  </div>
</template>

<style scoped lang="scss">
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.book-card {
  overflow: hidden;
  border: 1px solid var(--page-border);
  border-radius: 24px;
  background: rgb(255 255 255 / 84%);
  box-shadow: var(--page-shadow);
}

.book-card__cover {
  display: block;
  width: 100%;
  height: 280px;
  object-fit: cover;
  background: #f3f4f6;
}

.book-card__body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.book-card__meta,
.book-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.book-card__meta {
  color: var(--page-muted);
  font-size: 13px;
}

.book-card h3 {
  margin: 0;
  font-size: 20px;
}

.book-card p {
  margin: 0;
  color: var(--page-muted);
  line-height: 1.6;
}

.book-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
