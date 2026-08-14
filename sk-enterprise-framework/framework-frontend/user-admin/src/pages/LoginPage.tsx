import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ApiError } from '../api/client';

const DEMO_ACCOUNTS = [
  { email: 'admin@sk.com', role: '관리자(ADMIN)' },
  { email: 'manager@sk.com', role: '매니저(MANAGER)' },
  { email: 'user1@sk.com', role: '일반(USER)' },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('admin@sk.com');
  const [password, setPassword] = useState('demo');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const from = (location.state as { from?: string })?.from ?? '/';

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="login-brand">
          <span className="dot" /> SK Framework
        </div>
        <div className="login-sub">User Admin Console · 로그인</div>

        {error && <div className="badge red" style={{ padding: '10px 12px' }}>{error}</div>}

        <label className="field">
          이메일
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="admin@sk.com" />
        </label>
        <label className="field">
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="데모: 아무 값"
          />
        </label>

        <button className="btn primary" type="submit" disabled={busy} style={{ width: '100%' }}>
          {busy ? '로그인 중…' : '로그인'}
        </button>

        <div className="login-demo">
          <div className="muted" style={{ marginBottom: 6 }}>데모 계정 (비밀번호 미검증)</div>
          {DEMO_ACCOUNTS.map((a) => (
            <button
              type="button"
              key={a.email}
              className="btn sm"
              onClick={() => setEmail(a.email)}
              style={{ marginRight: 6, marginBottom: 6 }}
            >
              {a.role}
            </button>
          ))}
        </div>
      </form>
    </div>
  );
}
