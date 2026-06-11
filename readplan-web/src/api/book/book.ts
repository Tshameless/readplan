import request from '@/utils/request';
import type { ApiResponse, PageResult } from '@/types/api';
import type { AdminImportCandidate, BookDetail, BookSummary } from '@/types/readplan';

interface BookSummaryPayload {
  id: number;
  title: string;
  author: string;
  cover: string;
  publishYear: number;
  description: string;
  tags: string[];
  imported: boolean;
  isbn?: string;
  olId?: string;
}

interface PublicNotePayload {
  id: number;
  title: string;
  authorName: string;
  contentPreview: string;
  createdAt: string;
  commentCount: number;
}

interface BookDetailPayload extends BookSummaryPayload {
  isbn: string;
  openLibraryId: string;
  noteCount: number;
  planCount: number;
  notes: PublicNotePayload[];
}

interface AdminImportCandidatePayload {
  olId: string;
  title: string;
  author: string;
  firstPublishYear: number;
  cover: string;
  selected: boolean;
}

const normalizeBookSummary = (payload: BookSummaryPayload): BookSummary => ({
  id: String(payload.id),
  title: payload.title,
  author: payload.author,
  cover: payload.cover,
  publishYear: payload.publishYear,
  description: payload.description,
  tags: payload.tags,
  imported: payload.imported,
  isbn: payload.isbn,
  olId: payload.olId,
});

const normalizeImportCandidate = (payload: AdminImportCandidatePayload): AdminImportCandidate => ({
  ...payload,
  selected: payload.selected ?? false,
});

const normalizeBookDetail = (payload: BookDetailPayload): BookDetail => ({
  ...normalizeBookSummary(payload),
  isbn: payload.isbn,
  openLibraryId: payload.openLibraryId,
  noteCount: payload.noteCount,
  planCount: payload.planCount,
  notes: payload.notes.map((note) => ({
    id: String(note.id),
    title: note.title,
    authorName: note.authorName,
    contentPreview: note.contentPreview,
    createdAt: note.createdAt,
    commentCount: note.commentCount,
  })),
});

export const getBookCatalog = async (keyword = ''): Promise<BookSummary[]> => {
  const { data } = await request.get<ApiResponse<PageResult<BookSummaryPayload>>>('/books', {
    params: {
      keyword,
      pageNum: 1,
      pageSize: 20,
    },
  });

  return data.data.records.map(normalizeBookSummary);
};

export const getBookDetail = async (bookId: string): Promise<BookDetail> => {
  const { data } = await request.get<ApiResponse<BookDetailPayload>>(`/books/${bookId}`);
  return normalizeBookDetail(data.data);
};

export const getAdminBooks = async (): Promise<BookSummary[]> => {
  const { data } = await request.get<ApiResponse<BookSummaryPayload[]>>('/admin/books');
  return data.data.map(normalizeBookSummary);
};

export const searchOpenLibraryBooks = async (keyword: string): Promise<AdminImportCandidate[]> => {
  const { data } = await request.get<ApiResponse<AdminImportCandidatePayload[]>>('/admin/books/import/candidates', {
    params: { keyword },
  });
  return data.data.map(normalizeImportCandidate);
};

export const importBooksFromOpenLibrary = async (
  candidates: AdminImportCandidate[],
): Promise<BookSummary[]> => {
  const olIds = candidates.filter((item) => item.selected).map((item) => item.olId);
  const { data } = await request.post<ApiResponse<BookSummaryPayload[]>>('/admin/books/import', { olIds });
  return data.data.map(normalizeBookSummary);
};

export interface SaveBookPayload {
  title: string;
  author: string;
  cover: string;
  publishYear: number;
  isbn: string;
  olId: string;
  description: string;
}

export const createBook = async (payload: SaveBookPayload): Promise<BookSummary> => {
  const { data } = await request.post<ApiResponse<BookSummaryPayload>>('/admin/books', payload);
  return normalizeBookSummary(data.data);
};

export const updateBook = async (bookId: string, payload: SaveBookPayload): Promise<BookSummary> => {
  const { data } = await request.put<ApiResponse<BookSummaryPayload>>(`/admin/books/${bookId}`, payload);
  return normalizeBookSummary(data.data);
};

export const deleteBook = async (bookId: string): Promise<void> => {
  await request.delete(`/admin/books/${bookId}`);
};
