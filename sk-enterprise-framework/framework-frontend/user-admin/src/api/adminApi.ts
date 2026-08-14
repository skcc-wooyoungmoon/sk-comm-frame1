import { api } from './client';
import type { LogEntry, MonitoringSummary } from '../types';

/** 모니터링 API (백엔드 MonitoringController 대응) */
export const monitoringApi = {
  summary(): Promise<MonitoringSummary> {
    return api.get<MonitoringSummary>('/admin/monitoring/summary');
  },
};

/** 로그 API (백엔드 LogController 대응) */
export const logsApi = {
  recent(level?: string, limit = 100): Promise<LogEntry[]> {
    const params = new URLSearchParams();
    if (level) params.set('level', level);
    params.set('limit', String(limit));
    return api.get<LogEntry[]>(`/admin/logs?${params.toString()}`);
  },
};
