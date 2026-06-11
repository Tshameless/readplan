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
  fileType?: string;
  fileUrl?: string;
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
  fileType: string;
  fileUrl: string;
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

const buildFallbackCover = (title: string): string => {
  const safeTitle = title.trim() || 'ReadPlan';
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 440">
      <rect width="320" height="440" rx="28" fill="#f0ede7" />
      <rect x="24" y="24" width="272" height="392" rx="22" fill="#d8b36a" />
      <rect x="48" y="54" width="224" height="10" rx="5" fill="#8a5a18" opacity="0.45" />
      <text x="44" y="160" fill="#3c2a12" font-family="Georgia, serif" font-size="24" font-weight="700">
        ${safeTitle.slice(0, 28)}
      </text>
      <text x="44" y="214" fill="#5f4320" font-family="Georgia, serif" font-size="18">
        Local Book File
      </text>
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
};

const normalizeBookSummary = (payload: BookSummaryPayload): BookSummary => ({
  id: String(payload.id),
  title: payload.title,
  author: payload.author,
  cover: payload.cover || buildFallbackCover(payload.title),
  publishYear: payload.publishYear,
  description: payload.description,
  tags: payload.tags,
  imported: payload.imported,
  isbn: payload.isbn,
  olId: payload.olId,
  fileType: payload.fileType,
  fileUrl: payload.fileUrl,
});

const normalizeImportCandidate = (payload: AdminImportCandidatePayload): AdminImportCandidate => ({
  ...payload,
  selected: payload.selected ?? false,
});

const normalizeBookDetail = (payload: BookDetailPayload): BookDetail => ({
  ...normalizeBookSummary(payload),
  isbn: payload.isbn,
  openLibraryId: payload.openLibraryId,
  fileType: payload.fileType,
  fileUrl: payload.fileUrl,
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

export const getBookCatalog = async (
  keyword = '',
  pageNum = 1,
  pageSize = 8,
): Promise<PageResult<BookSummary>> => {
  const { data } = await request.get<ApiResponse<PageResult<BookSummaryPayload>>>('/books', {
    params: {
      keyword,
      pageNum,
      pageSize,
    },
  });

  return {
    total: data.data.total,
    pages: data.data.pages,
    records: data.data.records.map(normalizeBookSummary),
  };
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

export const crawlBooksFromOpenLibrary = async (keyword: string): Promise<AdminImportCandidate[]> => {
  const { data } = await request.get<ApiResponse<AdminImportCandidatePayload[]>>('/admin/books/crawl', {
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
  tags: string[];
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

export const uploadBooksFromFile = async (file: File, tags: string[]): Promise<BookSummary[]> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('tags', tags.join(','));
  const { data } = await request.post<ApiResponse<BookSummaryPayload[]>>('/admin/books/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return data.data.map(normalizeBookSummary);
};

export const uploadBookFile = async (bookId: string, file: File): Promise<BookSummary> => {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await request.post<ApiResponse<BookSummaryPayload>>(`/admin/books/${bookId}/file`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return normalizeBookSummary(data.data);
};
