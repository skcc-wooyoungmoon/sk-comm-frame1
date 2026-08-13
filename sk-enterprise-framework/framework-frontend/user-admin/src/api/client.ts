import type { ApiResponse } from '../types';

/**
 * 공통 API 클라이언트.
 * - 백엔드 표준 응답(ApiResponse<T>)을 언랩하여 data를 반환한다.
 * - success=false 이거나 HTTP 오류면 ApiError를 던진다.
 */

const BASE_URL = '/api';

export class ApiError extends Error {
  code: string;
  status: number;
  constructor(message: string, code: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
    ...options,
  });

  let body: ApiResponse<T> | null = null;
  const text = await res.text();
  if (text) {
    try {
      body = JSON.parse(text) as ApiResponse<T>;
    } catch {
      // JSON이 아니면 무시 (아래에서 상태로 처리)
    }
  }

  if (!res.ok || (body && !body.success)) {
    const message = body?.message ?? `요청 실패 (HTTP ${res.status})`;
    const code = body?.code ?? String(res.status);
    throw new ApiError(message, code, res.status);
  }

  return (body ? body.data : (undefined as unknown)) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(data) }),
  put: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(data) }),
  patch: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
