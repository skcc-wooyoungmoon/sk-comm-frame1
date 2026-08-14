/** 표시용 포매팅 유틸리티 */

/** 바이트를 사람이 읽기 쉬운 단위로 변환 */
export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null) return '—';
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}

/** 0~1 비율을 백분율 문자열로 */
export function formatPercent(ratio: number | null | undefined, digits = 1): string {
  if (ratio == null) return '—';
  return `${(ratio * 100).toFixed(digits)}%`;
}

/** 초 단위 시간을 d h m s 로 */
export function formatDuration(seconds: number | null | undefined): string {
  if (seconds == null) return '—';
  const s = Math.floor(seconds);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const parts: string[] = [];
  if (d) parts.push(`${d}d`);
  if (h) parts.push(`${h}h`);
  if (m) parts.push(`${m}m`);
  parts.push(`${sec}s`);
  return parts.join(' ');
}

/** epoch millis 를 HH:mm:ss.SSS 로 */
export function formatTime(ts: number): string {
  const d = new Date(ts);
  const pad = (n: number, l = 2) => String(n).padStart(l, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
}

/** ISO 문자열을 yyyy-MM-dd HH:mm 로 (간단 표시) */
export function formatDateTime(iso?: string): string {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

/** 숫자 천단위 콤마 */
export function formatNumber(n: number | null | undefined): string {
  if (n == null) return '—';
  return n.toLocaleString('ko-KR');
}
