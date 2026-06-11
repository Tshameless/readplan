<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue';
import { ElMessage } from 'element-plus';
import { createComment, deleteComment, getNoteComments } from '@/api/comment/comment';
import { useUserStore } from '@/stores/user';
import type { CommentItem } from '@/types/readplan';

const props = defineProps<{
  noteId: string;
}>();

const comments = shallowRef<CommentItem[]>([]);
const loading = shallowRef(false);
const submitting = shallowRef(false);
const deletingId = shallowRef('');
const userStore = useUserStore();
const form = reactive({
  content: '',
});

const loadComments = async () => {
  loading.value = true;
  try {
    comments.value = await getNoteComments(props.noteId);
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请输入评论内容。');
    return;
  }

  submitting.value = true;
  try {
    const comment = await createComment(props.noteId, form.content.trim());
    comments.value = [comment, ...comments.value];
    form.content = '';
    ElMessage.success('评论已发布。');
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (commentId: string) => {
  deletingId.value = commentId;
  try {
    await deleteComment(commentId);
    comments.value = comments.value.filter((item) => item.id !== commentId);
    ElMessage.success('评论已删除。');
  } finally {
    deletingId.value = '';
  }
};

onMounted(() => {
  void loadComments();
});
</script>

<template>
  <section class="comment-panel">
    <div class="comment-panel__header">
      <h4>评论互动</h4>
      <el-button :loading="loading" link type="primary" @click="loadComments">刷新</el-button>
    </div>

    <div v-if="userStore.isAuthenticated" class="comment-panel__editor">
      <el-input
        v-model="form.content"
        :rows="3"
        maxlength="300"
        placeholder="写下你的想法"
        show-word-limit
        type="textarea"
      />
      <div class="comment-panel__actions">
        <el-button :loading="submitting" size="small" type="primary" @click="handleSubmit">发表评论</el-button>
      </div>
    </div>

    <p v-else class="comment-panel__hint">登录后可以参与评论。</p>

    <div class="comment-stack">
      <article v-for="comment in comments" :key="comment.id" class="comment-card">
        <div class="comment-card__header">
          <strong>{{ comment.username }}</strong>
          <div class="comment-card__actions">
            <span>{{ comment.createdAt }}</span>
            <el-button
              v-if="comment.canDelete"
              :loading="deletingId === comment.id"
              link
              type="danger"
              @click="handleDelete(comment.id)"
            >
              删除
            </el-button>
          </div>
        </div>
        <p>{{ comment.content }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped lang="scss">
.comment-panel {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px dashed var(--page-border);
}

.comment-panel__header,
.comment-card__header,
.comment-panel__actions,
.comment-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-panel__header h4 {
  margin: 0;
  font-size: 16px;
}

.comment-panel__hint,
.comment-card p,
.comment-card__actions span {
  margin: 0;
  color: var(--page-muted);
  line-height: 1.6;
}

.comment-stack {
  display: grid;
  gap: 10px;
}

.comment-card {
  padding: 14px;
  border-radius: 16px;
  background: rgb(20 33 61 / 4%);
}
</style>
