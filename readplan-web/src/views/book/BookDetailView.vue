<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getBookDetail } from '@/api/book/book';
import { addReadingPlan } from '@/api/plan/plan';
import AppPage from '@/components/common/AppPage.vue';
import PublicNoteList from '@/components/note/PublicNoteList.vue';
import { useUserStore } from '@/stores/user';
import type { BookDetail } from '@/types/readplan';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = shallowRef(false);
const joining = shallowRef(false);
const planStatus = shallowRef<0 | 1 | 2>(1);
const detail = shallowRef<BookDetail | null>(null);

const isAdmin = computed(() => userStore.profile?.roles.includes('ADMIN'));

const goToAdmin = () => {
  void router.push('/admin/books');
};

const isFullscreen = ref(false);

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value;
  if (isFullscreen.value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
};

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isFullscreen.value) {
    toggleFullscreen();
  }
};

const loadDetail = async () => {
  loading.value = true;

  try {
    detail.value = await getBookDetail(String(route.params.id ?? '1'));
  } finally {
    loading.value = false;
  }
};

const joinPlan = async () => {
  if (!detail.value) {
    return;
  }

  joining.value = true;

  try {
    await addReadingPlan(detail.value.id, planStatus.value);
    ElMessage.success('已加入阅读计划。');
    await loadDetail();
  } finally {
    joining.value = false;
  }
};

onMounted(() => {
  void loadDetail();
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
  document.body.style.overflow = '';
});
</script>

<template>
  <AppPage
    :title="detail?.title ?? '书籍详情'"
    :description="detail?.description ?? '展示书籍信息、公开笔记和后续评论入口。'"
  >
    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item style="width: 100%; height: 320px" variant="rect" />
      </template>

      <template #default>
        <div v-if="detail" class="detail-layout">
          <section class="detail-panel">
            <img :alt="detail.title" :src="detail.cover" class="detail-panel__cover" />
            <div class="detail-panel__body">
              <div class="detail-panel__metrics">
                <el-tag effect="dark" type="warning">阅读计划 {{ detail.planCount }}</el-tag>
                <el-tag>公开笔记 {{ detail.noteCount }}</el-tag>
                <el-tag v-if="detail.fileType" type="success">{{ detail.fileType }}</el-tag>
              </div>
              <div v-if="userStore.isAuthenticated" class="detail-panel__actions">
                <el-select v-model="planStatus" style="width: 160px">
                  <el-option :value="0" label="未开始" />
                  <el-option :value="1" label="阅读中" />
                  <el-option :value="2" label="已读完" />
                </el-select>
                <el-button :loading="joining" type="primary" @click="joinPlan">加入阅读计划</el-button>
              </div>
              <div v-if="detail.fileUrl" class="detail-panel__actions">
                <el-button :href="detail.fileUrl" tag="a" target="_blank" type="success">
                  打开原文件
                </el-button>
              </div>
              <p><strong>作者：</strong>{{ detail.author }}</p>
              <p><strong>出版年份：</strong>{{ detail.publishYear }}</p>
              <p><strong>ISBN：</strong>{{ detail.isbn }}</p>
              <p><strong>Open Library ID：</strong>{{ detail.openLibraryId }}</p>
              <el-alert
                :title="userStore.isAuthenticated ? '已登录，后续可在加入计划并标记阅读状态后写笔记。' : '游客可以浏览书籍与公开笔记，登录后才能加入计划。'"
                :type="userStore.isAuthenticated ? 'success' : 'info'"
                :closable="false"
                show-icon
              />
            </div>
          </section>

          <section v-if="detail.fileUrl && detail.fileType === 'PDF'" class="detail-pdf-viewer">
            <header class="detail-pdf-viewer__header">
              <div class="detail-pdf-viewer__title-row">
                <h2>在线阅读</h2>
                <div class="detail-pdf-viewer__actions">
                  <el-button type="primary" @click="toggleFullscreen">
                    <template #icon>
                      <icon-ep-full-screen />
                    </template>
                    网页全屏
                  </el-button>
                  <el-button :href="detail.fileUrl" tag="a" target="_blank" type="info">
                    <template #icon>
                      <icon-ep-share />
                    </template>
                    在新窗口打开
                  </el-button>
                </div>
              </div>
              <p>您可以在下方直接预览与阅读该 PDF 书籍。</p>
            </header>
            <div :class="['pdf-iframe-container', { 'is-fullscreen': isFullscreen }]">
              <iframe :src="detail.fileUrl" class="pdf-iframe" title="PDF Reader"></iframe>
              <el-button
                v-if="isFullscreen"
                class="fullscreen-exit-btn"
                type="danger"
                circle
                @click="toggleFullscreen"
              >
                <template #icon>
                  <icon-ep-close />
                </template>
              </el-button>
            </div>
          </section>

          <section v-else class="detail-pdf-viewer detail-pdf-viewer--empty">
            <header class="detail-pdf-viewer__header">
              <h2>在线阅读</h2>
              <p>该书籍暂未提供在线阅读支持。</p>
            </header>
            <el-empty description="此书籍暂未上传关联的 PDF 电子书文件">
              <div class="empty-actions">
                <el-button v-if="isAdmin" type="primary" @click="goToAdmin">
                  去后台上传关联 PDF
                </el-button>
              </div>
              <p class="detail-empty-tip">当前已不再提供全网自动爬取 PDF，请使用后台资源搜索或本地上传。</p>
            </el-empty>
          </section>

          <section class="detail-notes">
            <header class="detail-notes__header">
              <div>
                <h2>关联公开笔记</h2>
                <p>书籍详情页默认公开展示所有用户笔记，后续可继续接入评论流。</p>
              </div>
            </header>

            <PublicNoteList :notes="detail.notes" />
          </section>
        </div>
      </template>
    </el-skeleton>
  </AppPage>
</template>

<style scoped lang="scss">
.detail-layout {
  display: grid;
  gap: 20px;
}

.detail-panel,
.detail-notes {
  border: 1px solid var(--page-border);
  border-radius: 28px;
  background: rgb(255 255 255 / 86%);
  box-shadow: var(--page-shadow);
}

.detail-panel {
  display: grid;
  grid-template-columns: minmax(220px, 300px) 1fr;
  gap: 24px;
  padding: 24px;
}

.detail-panel__cover {
  width: 100%;
  height: 100%;
  min-height: 360px;
  border-radius: 20px;
  object-fit: cover;
  background: #f3f4f6;
}

.detail-panel__body {
  display: grid;
  gap: 14px;
}

.detail-panel__body p,
.detail-notes__header p {
  margin: 0;
  color: var(--page-muted);
  line-height: 1.7;
}

.detail-panel__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-notes {
  padding: 24px;
}

.detail-pdf-viewer {
  border: 1px solid var(--page-border);
  border-radius: 28px;
  background: rgb(255 255 255 / 86%);
  box-shadow: var(--page-shadow);
  padding: 24px;
}

.detail-pdf-viewer__title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-pdf-viewer__actions {
  display: flex;
  gap: 10px;
}

.detail-pdf-viewer__header h2 {
  margin: 0;
  font-size: 24px;
}

.detail-pdf-viewer__header p {
  margin-top: 8px;
  margin-bottom: 16px;
  color: var(--page-muted);
}

.pdf-iframe-container {
  width: 100%;
  height: 680px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 9999;
    border-radius: 0;
    border: none;
    background: #000;
  }
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.fullscreen-exit-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  font-size: 20px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-notes__header h2 {
  margin: 0;
  font-size: 24px;
}

.detail-notes__header p {
  margin-top: 8px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.detail-empty-tip {
  margin: 16px 0 0;
  color: var(--page-muted);
  text-align: center;
}

@media (width <= 900px) {
  .detail-panel {
    grid-template-columns: 1fr;
  }
}
</style>
