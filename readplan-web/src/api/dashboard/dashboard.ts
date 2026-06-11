import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';
import type { DashboardStat } from '@/types/readplan';

export const getDashboardStats = async (): Promise<DashboardStat[]> => {
  const { data } = await request.get<ApiResponse<DashboardStat[]>>('/dashboard/stats');
  return data.data;
};
