import request from '@/utils/request';
import type { ApiResponse } from '@/types/api';
import type { UserProfile } from '@/types/auth';

interface UserInfoPayload {
  id: number;
  username: string;
  nickname: string;
  roles: Array<'USER' | 'ADMIN'>;
  permissions: string[];
}

const normalizeUserProfile = (payload: UserInfoPayload): UserProfile => ({
  id: String(payload.id),
  username: payload.username,
  nickname: payload.nickname,
  roles: payload.roles,
  permissions: payload.permissions,
});

export const getCurrentUserInfo = async (token?: string): Promise<UserProfile> => {
  const { data } = await request.get<ApiResponse<UserInfoPayload>>('/user/info', {
    headers: token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : undefined,
  });

  return normalizeUserProfile(data.data);
};
