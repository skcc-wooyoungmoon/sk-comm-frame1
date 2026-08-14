import { useEffect, useState } from 'react';

/**
 * 값 디바운스 훅. 검색 입력 등에서 과도한 API 호출을 줄인다.
 *
 * @param value 원본 값
 * @param delay 지연(ms)
 * @returns delay 동안 변화가 없을 때 반영되는 값
 */
export function useDebounce<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
