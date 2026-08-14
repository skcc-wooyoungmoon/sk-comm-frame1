/**
 * 멱등키/주문번호 생성 유틸.
 * 백엔드의 @Idempotent(key = orderNo)와 짝을 이루어, 클라이언트가 재시도 시
 * 동일한 orderNo를 재사용하면 중복 생성이 방지된다.
 */

/** RFC4122 v4 UUID (crypto 우선, 폴백 포함) */
export function uuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/** 사람이 식별 가능한 주문번호(prefix + 날짜 + 랜덤) */
export function generateOrderNo(prefix = 'ORD'): string {
  const now = new Date();
  const ymd =
    now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, '0') +
    String(now.getDate()).padStart(2, '0');
  const rand = Math.random().toString(36).slice(2, 8).toUpperCase();
  return `${prefix}-${ymd}-${rand}`;
}
