import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { usersApi } from '../api/usersApi';
import { ApiError } from '../api/client';
import { useToast } from '../components/Toast';
import type { User } from '../types';

interface Stats {
  total: number;
  active: number;
  suspended: number;
  admins: number;
}

export function DashboardPage() {
  const toast = useToast();
  const [stats, setStats] = useState<Stats | null>(null);
  const [recent, setRecent] = useState<User[]>([]);

  useEffect(() => {
    (async () => {
      try {
        // 대시보드 집계용으로 넉넉한 페이지 크기로 조회
        const res = await usersApi.list({}, 0, 200);
        const all = res.content;
        setStats({
          total: res.totalElements,
          active: all.filter((u) => u.status === 'ACTIVE').length,
          suspended: all.filter((u) => u.status === 'SUSPENDED').length,
          admins: all.filter((u) => u.role === 'ADMIN').length,
        });
        setRecent(all.slice(0, 5));
      } catch (e) {
        toast.error(e instanceof ApiError ? e.message : '대시보드 로딩 실패');
      }
    })();
  }, [toast]);

  return (
    <>
      <div className="stat-grid">
        <div className="stat">
          <div className="label">전체 사용자</div>
          <div className="value">{stats?.total ?? '—'}</div>
        </div>
        <div className="stat">
          <div className="label">활성 사용자</div>
          <div className="value">{stats?.active ?? '—'}</div>
        </div>
        <div className="stat">
          <div className="label">정지 사용자</div>
          <div className="value red">{stats?.suspended ?? '—'}</div>
        </div>
        <div className="stat">
          <div className="label">관리자</div>
          <div className="value">{stats?.admins ?? '—'}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <h2>최근 사용자</h2>
          <Link className="btn sm" to="/users">
            전체 보기 →
          </Link>
        </div>
        {recent.length === 0 ? (
          <div className="empty">데이터가 없습니다. 백엔드(8080)가 실행 중인지 확인하세요.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>사용자명</th>
                <th>이메일</th>
                <th>권한</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((u) => (
                <tr key={u.id}>
                  <td className="muted">{u.id}</td>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
