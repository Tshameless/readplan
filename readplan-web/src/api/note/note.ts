import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';
import type { NoteEditorForm, PublicNote, UserNoteSummary } from '@/types/readplan';

interface UserNoteSummaryPayload {
  id: number;
  bookId: number;
  bookTitle: string;
  title: string;
  excerpt: string;
  createdAt: string;
  commentCount: number;
}

interface PublicNotePayload {
  id: number;
  title: string;
  authorName: string;
  contentPreview: string;
  createdAt: string;
  commentCount: number;
}

export const getMyNotes = async (): Promise<UserNoteSummary[]> => {
  const { data } = await request.get<ApiResponse<UserNoteSummaryPayload[]>>('/notes/me');
  return data.data.map((item) => ({
    id: String(item.id),
    bookId: String(item.bookId),
    bookTitle: item.bookTitle,
    title: item.title,
    excerpt: item.excerpt,
    createdAt: item.createdAt,
    commentCount: item.commentCount,
  }));
};

export const getBookNotes = async (bookId: string): Promise<PublicNote[]> => {
  const { data } = await request.get<ApiResponse<PublicNotePayload[]>>(`/books/${bookId}/notes`);
  return data.data.map((item) => ({
    id: String(item.id),
    title: item.title,
    authorName: item.authorName,
    contentPreview: item.contentPreview,
    createdAt: item.createdAt,
    commentCount: item.commentCount,
  }));
};

export const createNote = async (payload: NoteEditorForm): Promise<UserNoteSummary> => {
  const { data } = await request.post<ApiResponse<UserNoteSummaryPayload>>('/notes', {
    bookId: Number(payload.bookId),
    title: payload.title,
    content: payload.content,
  });

  return normalizeUserNote(data.data);
};

export const updateNote = async (noteId: string, payload: NoteEditorForm): Promise<UserNoteSummary> => {
  const { data } = await request.put<ApiResponse<UserNoteSummaryPayload>>(`/notes/${noteId}`, {
    bookId: Number(payload.bookId),
    title: payload.title,
    content: payload.content,
  });

  return normalizeUserNote(data.data);
};

export const deleteNote = async (noteId: string): Promise<void> => {
  await request.delete(`/notes/${noteId}`);
};

const normalizeUserNote = (item: UserNoteSummaryPayload): UserNoteSummary => ({
  id: String(item.id),
  bookId: String(item.bookId),
  bookTitle: item.bookTitle,
  title: item.title,
  excerpt: item.excerpt,
  createdAt: item.createdAt,
  commentCount: item.commentCount,
});
