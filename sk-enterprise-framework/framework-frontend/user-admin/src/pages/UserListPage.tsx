import { useCallback, useEffect, useState } from 'react';
import { usersApi } from '../api/usersApi';
import { ApiError } from '../api/client';
import { RoleBadge, StatusBadge } from '../components/Badges';
import { UserFormModal, type UserFormValues } from '../components/UserFormModal';
import { useToast } from '../components/Toast';
import { useAuth } from '../auth/AuthContext';
import type { User, UserRole, UserSearch, UserStatus } from '../types';

const STATUS_OPTIONS: UserStatus[] = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];
const ROLE_OPTIONS: UserRole[] = ['ADMIN', 'MANAGER', 'USER'];
const PAGE_SIZE = 10;

export function UserListPage() {
  const toast = useToast();
  const { can } = useAuth();
  const manage = can('user:manage');
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [search, setSearch] = useState<UserSearch>({ username: '', email: '', status: '' });
  const [modal, setModal] = useState<{ mode: 'create' | 'edit'; user?: User } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await usersApi.list(search, page, PAGE_SIZE);
      setUsers(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [search, page, toast]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const handleSubmit = async (values: UserFormValues) => {
    setSubmitting(true);
    try {
      if (modal?.mode === 'create') {
        await usersApi.create(values);
        toast.success('사용자가 등록되었습니다.');
      } else if (modal?.user) {
        await usersApi.update(modal.user.id, values);
        toast.success('사용자가 수정되었습니다.');
      }
      setModal(null);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '저장에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const changeStatus = async (u: User, status: UserStatus) => {
    try {
      await usersApi.changeStatus(u.id, status);
      toast.success(`'${u.username}' 상태를 변경했습니다.`);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '상태 변경 실패');
    }
  };

  const changeRole = async (u: User, role: UserRole) => {
    try {
      await usersApi.changeRole(u.id, role);
      toast.success(`'${u.username}' 권한을 변경했습니다.`);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '권한 변경 실패');
    }
  };

  const remove = async (u: User) => {
    if (!confirm(`'${u.username}' 사용자를 삭제할까요?`)) return;
    try {
      await usersApi.remove(u.id);
      toast.success('삭제되었습니다.');
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '삭제 실패');
    }
  };

  return (
    <>
      <div className="card">
        <div className="card-head">
          <h2>사용자 목록 {totalElements > 0 && <span className="muted">· 총 {totalElements}명</span>}</h2>
          <div className="toolbar">
            <form className="toolbar" onSubmit={handleSearch}>
              <input
                placeholder="사용자명"
                value={search.username}
                onChange={(e) => setSearch((s) => ({ ...s, username: e.target.value }))}
              />
              <input
                placeholder="이메일"
                value={search.email}
                onChange={(e) => setSearch((s) => ({ ...s, email: e.target.value }))}
              />
              <select
                value={search.status}
                onChange={(e) => setSearch((s) => ({ ...s, status: e.target.value as UserStatus | '' }))}
              >
                <option value="">전체 상태</option>
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              <button className="btn" type="submit">
                🔍 검색
              </button>
            </form>
            {manage && (
              <button className="btn primary" onClick={() => setModal({ mode: 'create' })}>
                + 사용자 등록
              </button>
            )}
          </div>
        </div>

        {loading ? (
          <div className="spinner">불러오는 중…</div>
        ) : users.length === 0 ? (
          <div className="empty">조건에 맞는 사용자가 없습니다.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>사용자명</th>
                  <th>이메일</th>
                  <th>전화번호</th>
                  <th>상태</th>
                  <th>권한</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="muted">{u.id}</td>
                    <td>{u.username}</td>
                    <td>{u.email}</td>
                    <td className="muted">{u.phone ?? '-'}</td>
                    <td>
                      <StatusBadge status={u.status} />
                    </td>
                    <td>
                      <RoleBadge role={u.role} />
                    </td>
                    <td>
                      {!manage ? (
                        <span className="muted">읽기 전용</span>
                      ) : (
                      <div className="actions">
                        <button className="btn sm" onClick={() => setModal({ mode: 'edit', user: u })}>
                          수정
                        </button>
                        <select
                          className="btn sm"
                          value=""
                          onChange={(e) => e.target.value && changeStatus(u, e.target.value as UserStatus)}
                        >
                          <option value="">상태변경</option>
                          {STATUS_OPTIONS.map((s) => (
                            <option key={s} value={s}>
                              {s}
                            </option>
                          ))}
                        </select>
                        <select
                          className="btn sm"
                          value=""
                          onChange={(e) => e.target.value && changeRole(u, e.target.value as UserRole)}
                        >
                          <option value="">권한변경</option>
                          {ROLE_OPTIONS.map((r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ))}
                        </select>
                        <button className="btn sm danger" onClick={() => remove(u)}>
                          삭제
                        </button>
                      </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination">
            <span className="muted">
              {page + 1} / {totalPages} 페이지
            </span>
            <div className="row">
              <button className="btn sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                이전
              </button>
              <button
                className="btn sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                다음
              </button>
            </div>
          </div>
        )}
      </div>

      {modal && (
        <UserFormModal
          mode={modal.mode}
          initial={modal.user}
          submitting={submitting}
          onSubmit={handleSubmit}
          onClose={() => setModal(null)}
        />
      )}
    </>
  );
}
