/** 간단 CSV 내보내기 유틸 (클라이언트 다운로드) */

function escapeCell(value: unknown): string {
  const s = value == null ? '' : String(value);
  if (/[",\n]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

/**
 * 객체 배열을 CSV로 변환해 브라우저 다운로드를 트리거한다.
 *
 * @param rows    데이터 배열
 * @param columns 컬럼 정의(키 + 헤더)
 * @param filename 저장 파일명
 */
export function exportCsv<T>(
  rows: T[],
  columns: { key: keyof T; header: string }[],
  filename: string,
): void {
  const head = columns.map((c) => escapeCell(c.header)).join(',');
  const body = rows
    .map((row) => columns.map((c) => escapeCell(row[c.key])).join(','))
    .join('\n');
  const csv = `﻿${head}\n${body}`; // BOM: Excel 한글 깨짐 방지

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
