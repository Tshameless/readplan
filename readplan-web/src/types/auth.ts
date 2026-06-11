export type RoleKey = 'USER' | 'ADMIN';

export interface LoginForm {
  username: string;
  password: string;
}

export interface RegisterForm extends LoginForm {
  confirmPassword: string;
}

export interface UserProfile {
  id: string;
  username: string;
  nickname: string;
  roles: RoleKey[];
  permissions: string[];
}
