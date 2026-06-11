<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createBook,
  deleteBook,
  getAdminBooks,
  importBooksFromOpenLibrary,
  searchOpenLibraryBooks,
  updateBook,
  type SaveBookPayload,
} from '@/api/book/book';
import AppPage from '@/components/common/AppPage.vue';
import type { AdminImportCandidate, BookSummary } from '@/types/readplan';

const loading = shallowRef(false);
const importLoading = shallowRef(false);
const saveLoading = shallowRef(false);
const deletingId = shallowRef('');
const dialogOpen = shallowRef(false);
const importKeyword = shallowRef('java');
const candidates = ref<AdminImportCandidate[]>([]);
const localBooks = ref<BookSummary[]>([]);
const editingBook = shallowRef<BookSummary | null>(null);
const form = reactive<SaveBookPayload>({
  title: '',
  author: '',
  cover: '',
  publishYear: new Date().getFullYear(),
  isbn: '',
  olId: '',
  description: '',
});

const selectedCount = computed(() => candidates.value.filter((item) => item.selected).length);

const loadLocalBooks = async () => {
  localBooks.value = await getAdminBooks();
};

const searchCandidates = async () => {
  loading.value = true;

  try {
    candidates.value = await searchOpenLibraryBooks(importKeyword.value);
  } finally {
    loading.value = false;
  }
};

const importSelectedBooks = async () => {
  importLoading.value = true;

  try {
    const importedBooks = await importBooksFromOpenLibrary(candidates.value);
    if (importedBooks.length === 0) {
      ElMessage.warning('请先选择要导入的书籍。');
      return;
    }

    await loadLocalBooks();
    candidates.value = candidates.value.map((item) => ({
      ...item,
      selected: false,
    }));
    ElMessage.success(`已导入 ${importedBooks.length} 本书。`);
  } finally {
    importLoading.value = false;
  }
};

const openCreateDialog = () => {
  editingBook.value = null;
  Object.assign(form, {
    title: '',
    author: '',
    cover: '',
    publishYear: new Date().getFullYear(),
    isbn: '',
    olId: '',
    description: '',
  });
  dialogOpen.value = true;
};

const openEditDialog = (book: BookSummary) => {
  editingBook.value = book;
  Object.assign(form, {
    title: book.title,
    author: book.author,
    cover: book.cover,
    publishYear: book.publishYear,
    isbn: book.isbn ?? '',
    olId: book.olId ?? '',
    description: book.description,
  });
  dialogOpen.value = true;
};

const handleSaveBook = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('书名不能为空。');
    return;
  }

  saveLoading.value = true;

  try {
    if (editingBook.value) {
      await updateBook(editingBook.value.id, form);
      ElMessage.success('书籍信息已更新。');
    } else {
      await createBook(form);
      ElMessage.success('书籍已新增。');
    }

    dialogOpen.value = false;
    await loadLocalBooks();
  } finally {
    saveLoading.value = false;
  }
};

const handleDeleteBook = async (bookId: string) => {
  await ElMessageBox.confirm('下架后书籍不会物理删除，只会从前台隐藏，确定继续吗？', '下架书籍', {
    type: 'warning',
  });

  deletingId.value = bookId;
  try {
    await deleteBook(bookId);
    await loadLocalBooks();
    ElMessage.success('书籍已下架。');
  } finally {
    deletingId.value = '';
  }
};

onMounted(() => {
  void Promise.all([searchCandidates(), loadLocalBooks()]);
});
</script>

<template>
  <AppPage title="后台管理" description="管理员可以检索导入候选书籍，并维护本地书库。">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">手动新增书籍</el-button>
    </template>

    <el-card class="admin-section" shadow="never">
      <template #header>
        <div class="admin-section__header">
          <div>
            <h2>Open Library 导入</h2>
            <p>先搜索，再选择候选书籍导入到本地书库。</p>
          </div>
          <el-tag type="warning">已选 {{ selectedCount }}</el-tag>
        </div>
      </template>

      <div class="admin-toolbar">
        <el-input
          v-model="importKeyword"
          clearable
          placeholder="输入关键字搜索待导入书籍"
          @keyup.enter="searchCandidates"
        />
        <el-button :loading="loading" type="primary" @click="searchCandidates">搜索</el-button>
        <el-button :loading="importLoading" @click="importSelectedBooks">导入所选</el-button>
      </div>

      <div class="candidate-grid">
        <label v-for="candidate in candidates" :key="candidate.olId" class="candidate-card">
          <input v-model="candidate.selected" class="candidate-card__checkbox" type="checkbox" />
          <img :alt="candidate.title" :src="candidate.cover" class="candidate-card__cover" />
          <div>
            <h3>{{ candidate.title }}</h3>
            <p>{{ candidate.author }}</p>
            <span>{{ candidate.firstPublishYear }} · {{ candidate.olId }}</span>
          </div>
        </label>
      </div>
    </el-card>

    <el-card class="admin-section" shadow="never">
      <template #header>
        <div class="admin-section__header">
          <div>
            <h2>本地书库</h2>
            <p>支持编辑书籍信息和软删除下架。</p>
          </div>
        </div>
      </template>

      <el-table :data="localBooks" border>
        <el-table-column label="书名" min-width="220" prop="title" />
        <el-table-column label="作者" min-width="180" prop="author" />
        <el-table-column label="年份" min-width="100" prop="publishYear" />
        <el-table-column label="简介" min-width="260" prop="description" />
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button :loading="deletingId === row.id" link type="danger" @click="handleDeleteBook(row.id)">
                下架
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :model-value="dialogOpen"
      :title="editingBook ? '编辑书籍' : '手动新增书籍'"
      width="700px"
      @close="dialogOpen = false"
    >
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="书名">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="form.author" />
          </el-form-item>
          <el-form-item label="出版年份">
            <el-input-number v-model="form.publishYear" :max="2100" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="ISBN">
            <el-input v-model="form.isbn" />
          </el-form-item>
          <el-form-item label="Open Library ID">
            <el-input v-model="form.olId" />
          </el-form-item>
          <el-form-item label="封面链接">
            <el-input v-model="form.cover" />
          </el-form-item>
        </div>
        <el-form-item label="简介">
          <el-input v-model="form.description" :rows="4" type="textarea" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button :loading="saveLoading" type="primary" @click="handleSaveBook">保存</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<style scoped lang="scss">
.admin-section {
  border: 1px solid var(--page-border);
  border-radius: 24px;
  background: rgb(255 255 255 / 88%);
  box-shadow: var(--page-shadow);
}

.admin-section__header h2 {
  margin: 0;
}

.admin-section__header p {
  margin: 8px 0 0;
  color: var(--page-muted);
}

.admin-toolbar {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 12px;
}

.candidate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.candidate-card {
  position: relative;
  display: grid;
  gap: 12px;
  border: 1px solid var(--page-border);
  border-radius: 20px;
  padding: 16px;
}

.candidate-card__checkbox {
  position: absolute;
  top: 14px;
  right: 14px;
}

.candidate-card__cover {
  width: 100%;
  height: 220px;
  border-radius: 14px;
  object-fit: cover;
}

.candidate-card h3,
.candidate-card p,
.candidate-card span {
  margin: 0;
}

.candidate-card p,
.candidate-card span {
  color: var(--page-muted);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (width <= 760px) {
  .admin-toolbar,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
