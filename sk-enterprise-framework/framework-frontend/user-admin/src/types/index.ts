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

// ===== 상품(Product) =====
export interface Product {
  id: number;
  name: string;
  price: number;
  stock: number;
  version: number;
  createdAt?: string;
}

export interface ProductRequest {
  name: string;
  price: number;
  stock: number;
}

// ===== 주문(Order) =====
export interface Order {
  id: number;
  orderNo: string;
  customerId: string;
  productId: string;
  quantity: number;
  amount: number;
  status: string;
  createdAt?: string;
}

export interface PlaceOrderRequest {
  orderNo: string;
  customerId: string;
  productId: string;
  quantity: number;
  amount: number;
}

// ===== 인증(Auth) =====
export interface AuthUser extends User {}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

// 권한(퍼미션) - 화면/기능 접근 제어 키
export type Permission =
  | 'dashboard:view'
  | 'user:view'
  | 'user:manage'
  | 'product:view'
  | 'product:manage'
  | 'order:view'
  | 'order:manage'
  | 'monitoring:view'
  | 'log:view';

// ===== 모니터링/로그 =====
export interface MonitoringSummary {
  status: string;
  metrics: {
    jvmMemoryUsed: number | null;
    jvmMemoryMax: number | null;
    cpuUsage: number | null;
    processCpuUsage: number | null;
    uptimeSeconds: number | null;
    liveThreads: number | null;
    httpRequestCount: number | null;
    dbConnectionsActive: number | null;
  };
}

export interface LogEntry {
  timestamp: number;
  level: 'ERROR' | 'WARN' | 'INFO' | 'DEBUG' | 'TRACE';
  logger: string;
  thread: string;
  message: string;
  traceId?: string | null;
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
