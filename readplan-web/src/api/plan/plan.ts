import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';
import type { ReadingPlanItem } from '@/types/readplan';

interface BookSummaryPayload {
  id: number;
  title: string;
  author: string;
  cover: string;
  publishYear: number;
  description: string;
  tags: string[];
  imported: boolean;
}

interface ReadingPlanItemPayload {
  id: number;
  book: BookSummaryPayload;
  status: 0 | 1 | 2;
  shelfState: 'ACTIVE' | 'OFFLINE';
  updatedAt: string;
  allowNote: boolean;
}

export const getMyReadingPlans = async (): Promise<ReadingPlanItem[]> => {
  const { data } = await request.get<ApiResponse<ReadingPlanItemPayload[]>>('/plans/me');
  return data.data.map(normalizeReadingPlanItem);
};

export const addReadingPlan = async (bookId: string, status: 0 | 1 | 2): Promise<ReadingPlanItem> => {
  const { data } = await request.post<ApiResponse<ReadingPlanItemPayload>>('/plans', {
    bookId: Number(bookId),
    status,
  });
  return normalizeReadingPlanItem(data.data);
};

export const updateReadingPlanStatus = async (
  planId: string,
  status: 0 | 1 | 2,
): Promise<ReadingPlanItem> => {
  const { data } = await request.put<ApiResponse<ReadingPlanItemPayload>>(`/plans/${planId}/status`, {
    status,
  });
  return normalizeReadingPlanItem(data.data);
};

export const deleteReadingPlan = async (planId: string): Promise<void> => {
  await request.delete(`/plans/${planId}`);
};

const normalizeReadingPlanItem = (item: ReadingPlanItemPayload): ReadingPlanItem => ({
    id: String(item.id),
    book: {
      id: String(item.book.id),
      title: item.book.title,
      author: item.book.author,
      cover: item.book.cover,
      publishYear: item.book.publishYear,
      description: item.book.description,
      tags: item.book.tags,
      imported: item.book.imported,
    },
    status: item.status,
    shelfState: item.shelfState,
    updatedAt: item.updatedAt,
    allowNote: item.allowNote,
  });
