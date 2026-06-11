export const PERMISSION_KEYS = {
  noteWrite: 'note:write',
  commentModerate: 'comment:moderate',
  bookManage: 'book:manage',
} as const;

export type PermissionKey = (typeof PERMISSION_KEYS)[keyof typeof PERMISSION_KEYS];
