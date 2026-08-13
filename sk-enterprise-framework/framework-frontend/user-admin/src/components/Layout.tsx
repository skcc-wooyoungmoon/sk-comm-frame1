import { NavLink, Outlet, useLocation } from 'react-router-dom';

const TITLES: Record<string, string> = {
  '/': '대시보드',
  '/users': '사용자 관리',
};

export function Layout() {
  const { pathname } = useLocation();
  const title = TITLES[pathname] ?? '사용자 관리';

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
          <NavLink to="/" end>
            📊 대시보드
          </NavLink>
          <NavLink to="/users">👤 사용자 관리</NavLink>
        </nav>
        <div className="foot">
          SK Enterprise Framework
          <br />v1.0.0 · Transaction Edition
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <h1>{title}</h1>
          <span className="muted">framework-team@sk.com</span>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
