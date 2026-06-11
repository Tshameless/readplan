import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';
import type { CommentItem } from '@/types/readplan';

interface CommentItemPayload {
  id: number;
  username: string;
  content: string;
  createdAt: string;
  canDelete: boolean;
}

const normalizeComment = (item: CommentItemPayload): CommentItem => ({
  id: String(item.id),
  username: item.username,
  content: item.content,
  createdAt: item.createdAt,
  canDelete: item.canDelete,
});

export const getNoteComments = async (noteId: string): Promise<CommentItem[]> => {
  const { data } = await request.get<ApiResponse<CommentItemPayload[]>>(`/notes/${noteId}/comments`);
  return data.data.map(normalizeComment);
};

export const createComment = async (noteId: string, content: string): Promise<CommentItem> => {
  const { data } = await request.post<ApiResponse<CommentItemPayload>>('/comments', {
    noteId: Number(noteId),
    content,
  });
  return normalizeComment(data.data);
};

export const deleteComment = async (commentId: string): Promise<void> => {
  await request.delete(`/comments/${commentId}`);
};
