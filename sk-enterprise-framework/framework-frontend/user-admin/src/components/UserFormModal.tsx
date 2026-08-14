import { useState } from 'react';
import { Modal } from './Modal';
import type { User, UserRole } from '../types';

export interface UserFormValues {
  username: string;
  email: string;
  phone: string;
  role: UserRole;
}

interface Props {
  mode: 'create' | 'edit';
  initial?: User;
  submitting: boolean;
  onSubmit: (values: UserFormValues) => void;
  onClose: () => void;
}

const ROLES: UserRole[] = ['ADMIN', 'MANAGER', 'USER'];

export function UserFormModal({ mode, initial, submitting, onSubmit, onClose }: Props) {
  const [values, setValues] = useState<UserFormValues>({
    username: initial?.username ?? '',
    email: initial?.email ?? '',
    phone: initial?.phone ?? '',
    role: initial?.role ?? 'USER',
  });
  const [error, setError] = useState<string | null>(null);

  const set = (k: keyof UserFormValues, v: string) =>
    setValues((prev) => ({ ...prev, [k]: v }));

  const handleSubmit = () => {
    if (!values.username.trim()) return setError('사용자명을 입력하세요.');
    if (!values.email.trim()) return setError('이메일을 입력하세요.');
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(values.email)) return setError('이메일 형식이 올바르지 않습니다.');
    setError(null);
    onSubmit(values);
  };

  return (
    <Modal
      title={mode === 'create' ? '사용자 등록' : '사용자 수정'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button className="btn primary" onClick={handleSubmit} disabled={submitting}>
            {submitting ? '저장 중…' : '저장'}
          </button>
        </>
      }
    >
      {error && <div className="badge red" style={{ padding: '8px 12px' }}>{error}</div>}
      <label className="field">
        사용자명
        <input value={values.username} onChange={(e) => set('username', e.target.value)} placeholder="홍길동" />
      </label>
      <label className="field">
        이메일
        <input value={values.email} onChange={(e) => set('email', e.target.value)} placeholder="user@sk.com" />
      </label>
      <label className="field">
        전화번호
        <input value={values.phone} onChange={(e) => set('phone', e.target.value)} placeholder="010-1234-5678" />
      </label>
      <label className="field">
        권한
        <select value={values.role} onChange={(e) => set('role', e.target.value)}>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      </label>
    </Modal>
  );
}
