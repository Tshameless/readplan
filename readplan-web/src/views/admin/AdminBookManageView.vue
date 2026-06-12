<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef, useTemplateRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  crawlBooksFromOpenLibrary,
  createBook,
  deleteBook,
  getAdminBooks,
  importBooksFromLegalResources,
  importBooksFromOpenLibrary,
  searchOpenLibraryBooks,
  searchLegalBookResources,
  updateBook,
  uploadBooksFromFile,
  uploadBookFile,
  type SaveBookPayload,
} from '@/api/book/book';
import AppPage from '@/components/common/AppPage.vue';
import type { AdminImportCandidate, BookSummary, LegalBookResourceCandidate } from '@/types/readplan';

const loading = shallowRef(false);
const crawlLoading = shallowRef(false);
const importLoading = shallowRef(false);
const legalLoading = shallowRef(false);
const legalImportLoading = shallowRef(false);
const uploadLoading = shallowRef(false);
const saveLoading = shallowRef(false);
const deletingId = shallowRef('');
const dialogOpen = shallowRef(false);
const importKeyword = shallowRef('java');
const uploadTagText = shallowRef('');
const legalTagText = shallowRef('公版资源');
const selectedUploadFile = shallowRef<File | null>(null);
const candidates = ref<AdminImportCandidate[]>([]);
const legalResources = ref<LegalBookResourceCandidate[]>([]);
const localBooks = ref<BookSummary[]>([]);
const editingBook = ref<BookSummary | null>(null);
const dialogUploadLoading = shallowRef(false);
const selectedDialogFile = shallowRef<File | null>(null);
const form = reactive<SaveBookPayload>({
  title: '',
  author: '',
  cover: '',
  publishYear: new Date().getFullYear(),
  isbn: '',
  olId: '',
  description: '',
  tags: [],
});
const uploadSectionRef = useTemplateRef<HTMLElement>('uploadSection');

const selectedCount = computed(() => candidates.value.filter((item) => item.selected).length);
const selectedLegalCount = computed(() => legalResources.value.filter((item) => item.selected).length);
const tagOptions = computed(() => Array.from(new Set(localBooks.value.flatMap((book) => book.tags))));
const hasLegalResults = computed(() => legalResources.value.length > 0);
const isLegalResultSparse = computed(() => legalResources.value.length > 0 && legalResources.value.length < 6);

const parseTagInput = (value: string): string[] =>
  value
    .split(/[,|/;，；、]/)
    .map((item) => item.trim())
    .filter(Boolean);

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

const crawlCandidates = async () => {
  crawlLoading.value = true;

  try {
    candidates.value = await crawlBooksFromOpenLibrary(importKeyword.value);
    ElMessage.success(`已抓取 ${candidates.value.length} 条候选书籍。`);
  } finally {
    crawlLoading.value = false;
  }
};

const searchLegalResources = async () => {
  legalLoading.value = true;
  try {
    legalResources.value = await searchLegalBookResources(importKeyword.value);
    if (legalResources.value.length === 0) {
      ElMessage.info('没有找到合适资源，可以直接上传本地文件或手动建书。');
      return;
    }
    if (legalResources.value.length < 6) {
      ElMessage.info('当前结果较少，建议同时准备本地文件上传作为补充。');
    }
  } finally {
    legalLoading.value = false;
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

const importSelectedLegalResources = async () => {
  legalImportLoading.value = true;

  try {
    const importedBooks = await importBooksFromLegalResources(
      legalResources.value,
      parseTagInput(legalTagText.value),
    );
    if (importedBooks.length === 0) {
      ElMessage.warning('请先选择要导入的公开资源。');
      return;
    }

    await loadLocalBooks();
    legalResources.value = legalResources.value.map((item) => ({
      ...item,
      selected: false,
    }));
    ElMessage.success(`已导入 ${importedBooks.length} 本公开资源书籍。`);
  } finally {
    legalImportLoading.value = false;
  }
};

const openCreateDialog = () => {
  selectedDialogFile.value = null;
  editingBook.value = null;
  Object.assign(form, {
    title: '',
    author: '',
    cover: '',
    publishYear: new Date().getFullYear(),
    isbn: '',
    olId: '',
    description: '',
    tags: [],
  });
  dialogOpen.value = true;
};

const openCreateDialogFromKeyword = () => {
  selectedDialogFile.value = null;
  editingBook.value = null;
  Object.assign(form, {
    title: importKeyword.value.trim(),
    author: '',
    cover: '',
    publishYear: new Date().getFullYear(),
    isbn: '',
    olId: '',
    description: `根据搜索词“${importKeyword.value.trim() || '未命名书籍'}”手动补录书籍信息。`,
    tags: parseTagInput(legalTagText.value),
  });
  dialogOpen.value = true;
};

const scrollToUploadSection = () => {
  uploadSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const openEditDialog = (book: BookSummary) => {
  selectedDialogFile.value = null;
  editingBook.value = book;
  Object.assign(form, {
    title: book.title,
    author: book.author,
    cover: book.cover,
    publishYear: book.publishYear,
    isbn: book.isbn ?? '',
    olId: book.olId ?? '',
    description: book.description,
    tags: [...book.tags],
  });
  dialogOpen.value = true;
};

const handleDialogFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  selectedDialogFile.value = input.files?.[0] ?? null;
};

const handleUploadDialogFile = async () => {
  if (!editingBook.value || !selectedDialogFile.value) {
    return;
  }

  dialogUploadLoading.value = true;
  try {
    const updatedBook = await uploadBookFile(editingBook.value.id, selectedDialogFile.value);
    localBooks.value = localBooks.value.map((book) => book.id === updatedBook.id ? updatedBook : book);
    editingBook.value = updatedBook;
    selectedDialogFile.value = null;
    ElMessage.success('PDF 文件已成功关联。');
  } catch (error) {
    console.error(error);
  } finally {
    dialogUploadLoading.value = false;
  }
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

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  selectedUploadFile.value = input.files?.[0] ?? null;
};

const handleUploadBooks = async () => {
  if (!selectedUploadFile.value) {
    ElMessage.warning('请先选择 csv、json 或 pdf 文件。');
    return;
  }

  uploadLoading.value = true;
  try {
    const imported = await uploadBooksFromFile(selectedUploadFile.value, parseTagInput(uploadTagText.value));
    await loadLocalBooks();
    selectedUploadFile.value = null;
    uploadTagText.value = '';
    ElMessage.success(`已导入 ${imported.length} 本书。`);
  } finally {
    uploadLoading.value = false;
  }
};

onMounted(() => {
  void Promise.all([searchCandidates(), loadLocalBooks()]);
});
</script>

<template>
  <AppPage title="后台管理" description="管理员可以抓取、上传并维护本地书库。">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">手动新增书籍</el-button>
    </template>

    <el-card class="admin-section" shadow="never">
      <template #header>
        <div class="admin-section__header">
          <div>
            <h2>抓取候选书籍</h2>
            <p>支持读取本地候选数据，也支持在线抓取 Open Library 结果后再导入。</p>
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
        <el-button :loading="loading" type="primary" @click="searchCandidates">读取候选</el-button>
        <el-button :loading="crawlLoading" @click="crawlCandidates">在线抓取</el-button>
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
            <h2>合法资源搜索</h2>
            <p>按书名搜索合法公开资源、可预览资源和开放书目信息，目前接入 Open Library、Project Gutenberg、Google Books。</p>
          </div>
          <el-tag type="success">已选 {{ selectedLegalCount }}</el-tag>
        </div>
      </template>

      <div class="admin-toolbar legal-toolbar">
        <el-input
          v-model="importKeyword"
          clearable
          placeholder="输入书名搜索合法资源或书目信息"
          @keyup.enter="searchLegalResources"
        />
        <el-input
          v-model="legalTagText"
          clearable
          placeholder="导入时追加标签，例如：公版资源,公开书源"
        />
        <el-button :loading="legalLoading" type="primary" @click="searchLegalResources">搜索公开资源</el-button>
        <el-button :loading="legalImportLoading" @click="importSelectedLegalResources">导入所选资源</el-button>
      </div>

      <div v-if="hasLegalResults" class="candidate-grid legal-grid">
        <label v-for="resource in legalResources" :key="resource.sourceId" class="candidate-card">
          <input v-model="resource.selected" class="candidate-card__checkbox" type="checkbox" />
          <img :alt="resource.title" :src="resource.cover || form.cover" class="candidate-card__cover" />
          <div class="candidate-card__body">
            <h3>{{ resource.title }}</h3>
            <p>{{ resource.author || '作者待补充' }}</p>
            <span>{{ resource.sourceName }} · {{ resource.resourceType }}</span>
            <span>{{ resource.publishYear || '年份未知' }}</span>
            <p class="candidate-card__description">{{ resource.description }}</p>
            <el-link :href="resource.resourceUrl" target="_blank" type="primary">打开资源页</el-link>
          </div>
        </label>
      </div>

      <el-empty v-else-if="!legalLoading" description="没有找到可直接使用的资源结果">
        <div class="empty-actions">
          <el-button type="primary" @click="scrollToUploadSection">上传本地文件</el-button>
          <el-button @click="openCreateDialogFromKeyword">按当前书名手动建书</el-button>
        </div>
      </el-empty>

      <div v-if="isLegalResultSparse" class="resource-tip-panel">
        <p class="resource-tip-panel__title">结果偏少时的建议</p>
        <p class="resource-tip-panel__text">这类书通常没有稳定公开全文资源。你可以直接上传本地 PDF，或者先建书再补充文件与标签。</p>
        <div class="resource-tip-panel__actions">
          <el-button type="primary" plain @click="scrollToUploadSection">去上传区</el-button>
          <el-button plain @click="openCreateDialogFromKeyword">快速建书</el-button>
        </div>
      </div>
    </el-card>

    <el-card ref="uploadSection" class="admin-section" shadow="never">
      <template #header>
        <div class="admin-section__header">
          <div>
            <h2>本地文件上传导入</h2>
            <p>支持上传 `csv`、`json` 或 `pdf` 文件，导入时可追加全局标签。</p>
          </div>
        </div>
      </template>

      <div class="upload-panel">
        <input accept=".csv,.json,.pdf,application/json,text/csv,application/pdf" type="file" @change="handleFileChange" />
        <el-input
          v-model="uploadTagText"
          clearable
          placeholder="可选：输入全局标签，使用逗号分隔"
        />
        <el-button :loading="uploadLoading" type="primary" @click="handleUploadBooks">上传并导入</el-button>
      </div>
      <p class="upload-help">
        CSV 表头示例：`title,author,cover,publishYear,isbn,olId,description,tags`
      </p>
      <p class="upload-help">
        JSON 示例：`[{ "title": "DDD", "author": "Eric Evans", "tags": ["架构","设计"] }]`
      </p>
      <p class="upload-help">
        PDF 会直接作为本地书籍文件入库，默认用文件名生成书名，之后可在下方继续补充作者和简介。
      </p>
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
        <el-table-column label="标签" min-width="200">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="tag in row.tags" :key="tag" effect="plain" size="small">{{ tag }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="原文件" min-width="140">
          <template #default="{ row }">
            <el-link v-if="row.fileUrl" :href="row.fileUrl" target="_blank" type="primary">
              {{ row.fileType || '查看文件' }}
            </el-link>
            <span v-else class="file-empty">无</span>
          </template>
        </el-table-column>
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
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            allow-create
            clearable
            default-first-option
            filterable
            multiple
            style="width: 100%"
          >
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" :rows="4" type="textarea" />
        </el-form-item>
        <el-form-item v-if="editingBook" label="附加 PDF 书籍文件">
          <div class="dialog-file-upload">
            <div class="current-file">
              <span v-if="editingBook.fileUrl">
                已关联文件:
                <el-link :href="editingBook.fileUrl" target="_blank" type="success">
                  {{ editingBook.fileType || '查看文件' }}
                </el-link>
              </span>
              <span v-else class="no-file">当前未关联文件</span>
            </div>
            <div class="upload-actions">
              <input type="file" accept=".pdf" @change="handleDialogFileChange" />
              <el-button
                v-if="selectedDialogFile"
                :loading="dialogUploadLoading"
                size="small"
                type="success"
                @click="handleUploadDialogFile"
              >
                上传并关联 PDF
              </el-button>
            </div>
          </div>
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
  grid-template-columns: 1fr auto auto auto;
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

.candidate-card__body {
  display: grid;
  gap: 6px;
}

.candidate-card p,
.candidate-card span {
  color: var(--page-muted);
}

.candidate-card__description {
  min-height: 40px;
}

.legal-toolbar {
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) auto auto;
}

.legal-grid .candidate-card {
  align-content: start;
}

.empty-actions,
.resource-tip-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.resource-tip-panel {
  margin-top: 20px;
  border: 1px dashed var(--page-border);
  border-radius: 18px;
  padding: 16px;
  background: rgb(250 247 240 / 90%);
}

.resource-tip-panel__title,
.resource-tip-panel__text {
  margin: 0;
}

.resource-tip-panel__title {
  font-weight: 700;
  color: var(--page-text);
}

.resource-tip-panel__text {
  margin-top: 8px;
  color: var(--page-muted);
  line-height: 1.7;
}

.upload-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.upload-help {
  margin: 12px 0 0;
  color: var(--page-muted);
  font-size: 13px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-empty {
  color: var(--page-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.dialog-file-upload {
  border: 1px dashed var(--page-border);
  padding: 14px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.02);
  display: grid;
  gap: 10px;

  .current-file {
    font-size: 13px;
    color: var(--page-muted);
  }

  .upload-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
}

@media (width <= 760px) {
  .admin-toolbar,
  .upload-panel,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
