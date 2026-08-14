import type { UserRole, UserStatus } from '../types';

const STATUS_LABEL: Record<UserStatus, { text: string; cls: string }> = {
  ACTIVE: { text: '활성', cls: 'green' },
  INACTIVE: { text: '비활성', cls: 'gray' },
  SUSPENDED: { text: '정지', cls: 'red' },
};

const ROLE_LABEL: Record<UserRole, { text: string; cls: string }> = {
  ADMIN: { text: '관리자', cls: 'role-admin' },
  MANAGER: { text: '매니저', cls: 'role-manager' },
  USER: { text: '일반', cls: 'role-user' },
};

export function StatusBadge({ status }: { status: UserStatus }) {
  const s = STATUS_LABEL[status];
  return <span className={`badge ${s.cls}`}>{s.text}</span>;
}

export function RoleBadge({ role }: { role: UserRole }) {
  const r = ROLE_LABEL[role];
  return <span className={`badge ${r.cls}`}>{r.text}</span>;
}
