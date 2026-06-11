export type ReadingStatus = 0 | 1 | 2;

export interface DashboardStat {
  label: string;
  value: string;
  hint: string;
}

export interface PublicNote {
  id: string;
  title: string;
  authorName: string;
  contentPreview: string;
  createdAt: string;
  commentCount: number;
}

export interface BookSummary {
  id: string;
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

export interface BookDetail extends BookSummary {
  isbn: string;
  openLibraryId: string;
  noteCount: number;
  planCount: number;
  notes: PublicNote[];
}

export interface ReadingPlanItem {
  id: string;
  book: BookSummary;
  status: ReadingStatus;
  shelfState: 'ACTIVE' | 'OFFLINE';
  updatedAt: string;
  allowNote: boolean;
}

export interface UserNoteSummary {
  id: string;
  bookId: string;
  bookTitle: string;
  title: string;
  excerpt: string;
  createdAt: string;
  commentCount: number;
}

export interface CommentItem {
  id: string;
  username: string;
  content: string;
  createdAt: string;
  canDelete: boolean;
}

export interface NoteEditorForm {
  bookId: string;
  title: string;
  content: string;
}

export interface AdminImportCandidate {
  olId: string;
  title: string;
  author: string;
  firstPublishYear: number;
  cover: string;
  selected: boolean;
}
