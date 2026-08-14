import { useCallback, useEffect, useRef, useState } from 'react';
import { logsApi } from '../api/adminApi';
import { ApiError } from '../api/client';
import { useToast } from '../components/Toast';
import { formatTime } from '../lib/format';
import type { LogEntry } from '../types';

const LEVELS = ['', 'ERROR', 'WARN', 'INFO', 'DEBUG'] as const;
const LEVEL_CLASS: Record<string, string> = {
  ERROR: 'red',
  WARN: 'role-manager',
  INFO: 'role-user',
  DEBUG: 'gray',
  TRACE: 'gray',
};

export function LogsPage() {
  const toast = useToast();
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [level, setLevel] = useState('');
  const [auto, setAuto] = useState(false);
  const timer = useRef<number | null>(null);

  const load = useCallback(async () => {
    try {
      setLogs(await logsApi.recent(level || undefined, 200));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '로그 조회 실패');
    }
  }, [level, toast]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (auto) {
      timer.current = window.setInterval(load, 3000);
      return () => {
        if (timer.current) window.clearInterval(timer.current);
      };
    }
  }, [auto, load]);

  return (
    <div className="card">
      <div className="card-head">
        <h2>애플리케이션 로그 <span className="muted">· {logs.length}건</span></h2>
        <div className="toolbar">
          <select value={level} onChange={(e) => setLevel(e.target.value)}>
            {LEVELS.map((l) => (
              <option key={l} value={l}>
                {l || '전체 레벨'}
              </option>
            ))}
          </select>
          <label className="row" style={{ alignItems: 'center', gap: 6 }}>
            <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} style={{ height: 16 }} />
            자동(3s)
          </label>
          <button className="btn sm" onClick={load}>새로고침</button>
        </div>
      </div>
      <div className="log-view">
        {logs.length === 0 ? (
          <div className="empty">로그가 없습니다.</div>
        ) : (
          logs.map((l, i) => (
            <div className="log-row" key={i}>
              <span className="log-time">{formatTime(l.timestamp)}</span>
              <span className={`badge ${LEVEL_CLASS[l.level] ?? 'gray'} log-level`}>{l.level}</span>
              <span className="log-logger">{l.logger}</span>
              <span className="log-msg">{l.message}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
