import { useCallback, useEffect, useRef, useState } from 'react';
import { monitoringApi } from '../api/adminApi';
import { ApiError } from '../api/client';
import { useToast } from '../components/Toast';
import { LineChart } from '../components/LineChart';
import { formatBytes, formatDuration, formatNumber, formatPercent } from '../lib/format';
import type { MonitoringSummary } from '../types';

const REFRESH_MS = 5000;
const MAX_POINTS = 30;

interface History {
  mem: number[];
  cpu: number[];
  http: number[];
}

export function MonitoringPage() {
  const toast = useToast();
  const [data, setData] = useState<MonitoringSummary | null>(null);
  const [history, setHistory] = useState<History>({ mem: [], cpu: [], http: [] });
  const [auto, setAuto] = useState(true);
  const [updatedAt, setUpdatedAt] = useState<string>('');
  const timer = useRef<number | null>(null);

  const load = useCallback(async () => {
    try {
      const summary = await monitoringApi.summary();
      setData(summary);
      setUpdatedAt(new Date().toLocaleTimeString('ko-KR'));

      const m = summary.metrics;
      const memPct = m.jvmMemoryUsed != null && m.jvmMemoryMax ? (m.jvmMemoryUsed / m.jvmMemoryMax) * 100 : 0;
      const cpuPct = (m.processCpuUsage ?? 0) * 100;
      const http = m.httpRequestCount ?? 0;
      setHistory((prev) => ({
        mem: [...prev.mem, memPct].slice(-MAX_POINTS),
        cpu: [...prev.cpu, cpuPct].slice(-MAX_POINTS),
        http: [...prev.http, http].slice(-MAX_POINTS),
      }));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '모니터링 조회 실패');
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (auto) {
      timer.current = window.setInterval(load, REFRESH_MS);
      return () => {
        if (timer.current) window.clearInterval(timer.current);
      };
    }
  }, [auto, load]);

  const m = data?.metrics;
  const memRatio = m && m.jvmMemoryUsed != null && m.jvmMemoryMax ? m.jvmMemoryUsed / m.jvmMemoryMax : null;
  const up = data?.status === 'UP';

  return (
    <>
      <div className="card-head" style={{ background: 'transparent', border: 'none', padding: '0 0 16px' }}>
        <h2 style={{ margin: 0 }}>
          시스템 상태{' '}
          <span className={`badge ${up ? 'green' : 'red'}`}>{data?.status ?? '...'}</span>
        </h2>
        <div className="toolbar">
          <span className="muted">갱신 {updatedAt}</span>
          <label className="row" style={{ alignItems: 'center', gap: 6 }}>
            <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} style={{ height: 16 }} />
            자동 새로고침(5s)
          </label>
          <button className="btn sm" onClick={load}>새로고침</button>
        </div>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <div className="label">JVM 메모리 사용</div>
          <div className="value">{formatBytes(m?.jvmMemoryUsed)}</div>
          <div className="muted">/ {formatBytes(m?.jvmMemoryMax)}</div>
          <div className="meter">
            <span style={{ width: `${Math.min(100, (memRatio ?? 0) * 100)}%` }} />
          </div>
        </div>
        <div className="stat">
          <div className="label">프로세스 CPU</div>
          <div className="value">{formatPercent(m?.processCpuUsage)}</div>
          <div className="muted">시스템 {formatPercent(m?.cpuUsage)}</div>
        </div>
        <div className="stat">
          <div className="label">가동 시간</div>
          <div className="value" style={{ fontSize: 22 }}>{formatDuration(m?.uptimeSeconds)}</div>
        </div>
        <div className="stat">
          <div className="label">활성 스레드</div>
          <div className="value">{formatNumber(m?.liveThreads)}</div>
        </div>
        <div className="stat">
          <div className="label">HTTP 요청 수</div>
          <div className="value">{formatNumber(m?.httpRequestCount)}</div>
        </div>
        <div className="stat">
          <div className="label">DB 활성 커넥션</div>
          <div className="value">{formatNumber(m?.dbConnectionsActive)}</div>
        </div>
      </div>

      <div className="row" style={{ alignItems: 'stretch' }}>
        <div className="card grow" style={{ minWidth: 320 }}>
          <div className="card-head"><h2>리소스 사용률 추이 (%)</h2><span className="muted">최근 {history.mem.length}회</span></div>
          <div style={{ padding: '12px 12px 16px' }}>
            <LineChart
              unit="%"
              yMax={100}
              series={[
                { label: 'JVM 메모리', color: '#ea002c', data: history.mem },
                { label: 'CPU', color: '#ff7a00', data: history.cpu },
              ]}
            />
          </div>
        </div>
        <div className="card grow" style={{ minWidth: 320 }}>
          <div className="card-head"><h2>HTTP 요청 수 추이 (누적)</h2></div>
          <div style={{ padding: '12px 12px 16px' }}>
            <LineChart series={[{ label: 'HTTP 요청', color: '#1e5fbf', data: history.http }]} />
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-head"><h2>안내</h2></div>
        <div className="modal-body">
          <p className="muted" style={{ margin: 0 }}>
            이 지표는 백엔드 <code>/api/admin/monitoring/summary</code>가 Spring Boot Actuator의
            Health/Metrics를 요약해 제공합니다. 차트는 폴링 시점의 값을 누적해 시계열로 표시하며,
            Prometheus 연동은 <code>/actuator/prometheus</code>에서 수집할 수 있습니다.
          </p>
        </div>
      </div>
    </>
  );
}
