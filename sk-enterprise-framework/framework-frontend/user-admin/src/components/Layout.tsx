import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { Permission } from '../types';

const TITLES: Record<string, string> = {
  '/': '대시보드',
  '/users': '사용자 관리',
  '/products': '상품 관리',
  '/orders': '주문',
  '/monitoring': '모니터링',
  '/logs': '로그',
};

interface NavItem {
  to: string;
  label: string;
  icon: string;
  permission?: Permission;
  end?: boolean;
}

const NAV: NavItem[] = [
  { to: '/', label: '대시보드', icon: '📊', permission: 'dashboard:view', end: true },
  { to: '/users', label: '사용자 관리', icon: '👤', permission: 'user:view' },
  { to: '/products', label: '상품 관리', icon: '📦', permission: 'product:view' },
  { to: '/orders', label: '주문', icon: '🧾', permission: 'order:view' },
  { to: '/monitoring', label: '모니터링', icon: '📈', permission: 'monitoring:view' },
  { to: '/logs', label: '로그', icon: '📋', permission: 'log:view' },
];

export function Layout() {
  const { pathname } = useLocation();
  const { user, logout, can } = useAuth();
  const title = TITLES[pathname] ?? '관리자 콘솔';

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="dot" />
          <div>
            SK Framework
            <small>User Admin Console</small>
          </div>
        </div>
        <nav className="nav">
          {NAV.filter((n) => !n.permission || can(n.permission)).map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end}>
              {n.icon} {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="foot">
          SK Enterprise Framework
          <br />v1.0.0 · Transaction Edition
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <h1>{title}</h1>
          <div className="row" style={{ alignItems: 'center', gap: 12 }}>
            {user && (
              <span className="muted">
                {user.username} · <b>{user.role}</b>
              </span>
            )}
            <button className="btn sm" onClick={logout}>
              로그아웃
            </button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
