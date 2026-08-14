import type { ApiResponse } from '../types';

/**
 * 공통 API 클라이언트.
 * - 백엔드 표준 응답(ApiResponse<T>)을 언랩하여 data를 반환한다.
 * - 저장된 인증 토큰(Authorization: Bearer)을 자동 첨부한다.
 * - 409(동시성/멱등 충돌) 응답에 대해 지수 백오프로 재시도한다.(백엔드 @RetryOnConflict 대응)
 * - success=false 이거나 HTTP 오류면 ApiError를 던진다.
 */

const BASE_URL = '/api';
const TOKEN_KEY = 'sk.admin.token';

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

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

interface Options extends RequestInit {
  /** 409 충돌 시 재시도 횟수 (기본 0) */
  retryOnConflict?: number;
}

async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { retryOnConflict = 0, headers, ...rest } = options;
  const token = tokenStore.get();

  let attempt = 0;
  let backoff = 100;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const res = await fetch(`${BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(headers ?? {}),
      },
      ...rest,
    });

    let body: ApiResponse<T> | null = null;
    const text = await res.text();
    if (text) {
      try {
        body = JSON.parse(text) as ApiResponse<T>;
      } catch {
        /* JSON 아님 */
      }
    }

    const failed = !res.ok || (body && !body.success);
    if (failed) {
      // 409 충돌이면 재시도
      if (res.status === 409 && attempt < retryOnConflict) {
        attempt++;
        await sleep(backoff);
        backoff *= 2;
        continue;
      }
      // 401(미인증/만료)이면 토큰을 폐기하고 로그아웃 이벤트를 알린다.(403 권한부족은 유지)
      if (res.status === 401) {
        tokenStore.clear();
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
      const message = body?.message ?? (res.status === 401 ? '세션이 만료되었습니다. 다시 로그인하세요.' : `요청 실패 (HTTP ${res.status})`);
      const code = body?.code ?? String(res.status);
      throw new ApiError(message, code, res.status);
    }

    return (body ? body.data : (undefined as unknown)) as T;
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, data: unknown, opts?: Options) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(data), ...opts }),
  put: <T>(path: string, data: unknown, opts?: Options) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(data), ...opts }),
  patch: <T>(path: string, data: unknown, opts?: Options) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(data), ...opts }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
