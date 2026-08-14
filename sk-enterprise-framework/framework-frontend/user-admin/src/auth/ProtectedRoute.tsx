import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import type { Permission } from '../types';

interface Props {
  children: ReactNode;
  /** 접근에 필요한 권한(미지정 시 로그인만 필요) */
  permission?: Permission;
}

/** 인증/권한 기반 라우트 가드 */
export function ProtectedRoute({ children, permission }: Props) {
  const { user, loading, can } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="spinner">인증 확인 중…</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (permission && !can(permission)) {
    return (
      <div className="card">
        <div className="empty">
          🔒 이 화면에 접근할 권한이 없습니다.
          <br />
          <span className="muted">현재 권한: {user.role}</span>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}
