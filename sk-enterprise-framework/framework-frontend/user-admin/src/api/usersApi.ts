import { api } from './client';
import type {
  PageResponse,
  User,
  UserCreateRequest,
  UserRole,
  UserSearch,
  UserStatus,
  UserUpdateRequest,
} from '../types';

/** 사용자 관리 API (백엔드 UserController 와 1:1 대응) */
export const usersApi = {
  list(search: UserSearch, page = 0, size = 20): Promise<PageResponse<User>> {
    const params = new URLSearchParams();
    if (search.username) params.set('username', search.username);
    if (search.email) params.set('email', search.email);
    if (search.status) params.set('status', search.status);
    params.set('page', String(page));
    params.set('size', String(size));
    return api.get<PageResponse<User>>(`/users?${params.toString()}`);
  },

  get(id: number): Promise<User> {
    return api.get<User>(`/users/${id}`);
  },

  create(req: UserCreateRequest): Promise<User> {
    return api.post<User>('/users', req);
  },

  update(id: number, req: UserUpdateRequest): Promise<User> {
    return api.put<User>(`/users/${id}`, req);
  },

  changeStatus(id: number, status: UserStatus): Promise<User> {
    return api.patch<User>(`/users/${id}/status`, { status });
  },

  changeRole(id: number, role: UserRole): Promise<User> {
    return api.patch<User>(`/users/${id}/role`, { role });
  },

  remove(id: number): Promise<void> {
    return api.delete<void>(`/users/${id}`);
  },
};
