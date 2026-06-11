export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

export interface PageResult<T> {
  total: number;
  pages: number;
  records: T[];
}
