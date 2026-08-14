import { api } from './client';
import type { AuthUser, LoginRequest, LoginResponse } from '../types';

/** 인증 API (백엔드 AuthController 대응) */
export const authApi = {
  login(req: LoginRequest): Promise<LoginResponse> {
    return api.post<LoginResponse>('/auth/login', req);
  },
  me(): Promise<AuthUser> {
    return api.get<AuthUser>('/auth/me');
  },
};
