<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import { getBookCatalog } from '@/api/book/book';
import { getDashboardStats } from '@/api/dashboard/dashboard';
import BookCardGrid from '@/components/book/BookCardGrid.vue';
import AppPage from '@/components/common/AppPage.vue';
import { ROUTE_NAMES } from '@/constants/routeNames';
import type { BookSummary, DashboardStat } from '@/types/readplan';

const router = useRouter();
const keyword = shallowRef('');
const loading = shallowRef(false);
const books = ref<BookSummary[]>([]);
const statsList = ref<DashboardStat[]>([]);

const pageNum = ref(1);
const pageSize = ref(8);
const total = ref(0);

const visibleBooksLabel = computed(() => `${total.value} 本书`);

const loadStats = async () => {
  try {
    statsList.value = await getDashboardStats();
  } catch (error) {
    console.error('Failed to load dashboard stats:', error);
  }
};

const loadBooks = async () => {
  loading.value = true;

  try {
    const res = await getBookCatalog(keyword.value, pageNum.value, pageSize.value);
    books.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
  pageNum.value = page;
  void loadBooks();
};

const handleSearch = () => {
  pageNum.value = 1;
  void loadBooks();
};

const openBookDetail = async (bookId: string) => {
  await router.push({
    name: ROUTE_NAMES.bookDetail,
    params: { id: bookId },
  });
};

onMounted(() => {
  void loadStats();
  void loadBooks();
});
</script>

<template>
  <AppPage title="书籍广场" description="浏览已入库书籍、查看公开笔记，并继续扩展阅读计划与评论互动。">
    <template #actions>
      <el-tag effect="dark" type="warning">{{ visibleBooksLabel }}</el-tag>
    </template>

    <section class="hero-stats">
      <article v-for="stat in statsList" :key="stat.label" class="hero-stats__card">
        <p>{{ stat.label }}</p>
        <strong>{{ stat.value }}</strong>
        <span>{{ stat.hint }}</span>
      </article>
    </section>

    <el-card class="filter-card" shadow="never">
      <div class="filter-card__inner">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索书名、作者或标签"
          @keyup.enter="handleSearch"
        />
        <el-button :loading="loading" type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </el-card>

    <BookCardGrid v-if="books.length" :books="books" @select="openBookDetail" />
    <el-empty v-else description="当前没有匹配的书籍。" />

    <div v-if="total > pageSize" class="pagination-container">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
  </AppPage>
</template>

<style scoped lang="scss">
.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.hero-stats__card,
.filter-card {
  border: 1px solid var(--page-border);
  border-radius: 8px;
  background: var(--page-surface);
  box-shadow: var(--page-shadow);
}

.hero-stats__card {
  display: grid;
  gap: 10px;
  padding: 20px;
}

.hero-stats__card p,
.hero-stats__card span {
  margin: 0;
  color: var(--page-muted);
}

.hero-stats__card strong {
  font-size: 34px;
}

.filter-card__inner {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (width <= 640px) {
  .filter-card__inner {
    grid-template-columns: 1fr;
  }
}
</style>
