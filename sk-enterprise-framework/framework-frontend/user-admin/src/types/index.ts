// 백엔드 표준 응답/도메인 타입 정의 (framework-common 의 ApiResponse/PageResponse 와 대응)

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type UserRole = 'ADMIN' | 'MANAGER' | 'USER';

export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  status: UserStatus;
  role: UserRole;
  version: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserCreateRequest {
  username: string;
  email: string;
  phone?: string;
  role?: UserRole;
}

export interface UserUpdateRequest {
  username?: string;
  email?: string;
  phone?: string;
  role?: UserRole;
}

export interface UserSearch {
  username?: string;
  email?: string;
  status?: UserStatus | '';
}

/** framework-common ApiResponse<T> */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

/** framework-common PageResponse<T> */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
