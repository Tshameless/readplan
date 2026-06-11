import request from '@/utils/request';
import { getCurrentUserInfo } from '@/api/user/user';
import type { ApiResponse } from '@/types/api';
import type { LoginForm, RegisterForm, UserProfile } from '@/types/auth';

export interface LoginResult {
  token: string;
  user: UserProfile;
}

interface AuthTokenPayload {
  token: string;
}

const buildLoginResult = async (token: string): Promise<LoginResult> => {
  const user = await getCurrentUserInfo(token);
  return { token, user };
};

export const loginWithPassword = async (payload: LoginForm): Promise<LoginResult> => {
  const { data } = await request.post<ApiResponse<AuthTokenPayload>>('/auth/login', payload);
  return buildLoginResult(data.data.token);
};

export const registerAccount = async (payload: RegisterForm): Promise<LoginResult> => {
  const { data } = await request.post<ApiResponse<AuthTokenPayload>>('/auth/register', payload);
  return buildLoginResult(data.data.token);
};
