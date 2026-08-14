import { useCallback, useEffect, useState } from 'react';
import { productsApi } from '../api/productsApi';
import { ApiError } from '../api/client';
import { useToast } from '../components/Toast';
import { Modal } from '../components/Modal';
import { useAuth } from '../auth/AuthContext';
import { useDebounce } from '../lib/useDebounce';
import { exportCsv } from '../lib/csv';
import { formatNumber } from '../lib/format';
import type { Product, ProductRequest } from '../types';

const EMPTY: ProductRequest = { name: '', price: 0, stock: 0 };

export function ProductsPage() {
  const toast = useToast();
  const { can } = useAuth();
  const manage = can('product:manage');

  const [items, setItems] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const debounced = useDebounce(keyword, 300);
  const [editing, setEditing] = useState<{ id?: number; form: ProductRequest } | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await productsApi.list(debounced));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '상품 목록 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [debounced, toast]);

  useEffect(() => {
    load();
  }, [load]);

  const save = async () => {
    if (!editing) return;
    setBusy(true);
    try {
      if (editing.id) {
        await productsApi.update(editing.id, editing.form);
        toast.success('상품이 수정되었습니다.');
      } else {
        await productsApi.create(editing.form);
        toast.success('상품이 등록되었습니다.');
      }
      setEditing(null);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '저장 실패');
    } finally {
      setBusy(false);
    }
  };

  const purchase = async (p: Product) => {
    const input = prompt(`'${p.name}' 구매 수량 (현재고 ${p.stock})`, '1');
    if (!input) return;
    const qty = Number(input);
    if (!Number.isInteger(qty) || qty < 1) return toast.error('수량이 올바르지 않습니다.');
    try {
      const updated = await productsApi.purchase(p.id, qty);
      toast.success(`구매 완료. 남은 재고 ${updated.stock}`);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '구매 실패');
    }
  };

  const remove = async (p: Product) => {
    if (!confirm(`'${p.name}'을(를) 삭제할까요?`)) return;
    try {
      await productsApi.remove(p.id);
      toast.success('삭제되었습니다.');
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '삭제 실패');
    }
  };

  const setForm = (patch: Partial<ProductRequest>) =>
    setEditing((prev) => (prev ? { ...prev, form: { ...prev.form, ...patch } } : prev));

  return (
    <>
      <div className="card">
        <div className="card-head">
          <h2>상품 목록 <span className="muted">· {items.length}건</span></h2>
          <div className="toolbar">
            <input placeholder="상품명 검색" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
            <button
              className="btn"
              onClick={() =>
                exportCsv(
                  items,
                  [
                    { key: 'id', header: 'ID' },
                    { key: 'name', header: '상품명' },
                    { key: 'price', header: '가격' },
                    { key: 'stock', header: '재고' },
                  ],
                  'products.csv',
                )
              }
            >
              ⬇ CSV
            </button>
            {manage && (
              <button className="btn primary" onClick={() => setEditing({ form: { ...EMPTY } })}>
                + 상품 등록
              </button>
            )}
          </div>
        </div>

        {loading ? (
          <div className="spinner">불러오는 중…</div>
        ) : items.length === 0 ? (
          <div className="empty">상품이 없습니다.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>상품명</th>
                  <th>가격</th>
                  <th>재고</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.id}>
                    <td className="muted">{p.id}</td>
                    <td>{p.name}</td>
                    <td>₩{formatNumber(p.price)}</td>
                    <td>
                      <span className={`badge ${p.stock === 0 ? 'red' : p.stock < 5 ? 'role-manager' : 'green'}`}>
                        {p.stock}
                      </span>
                    </td>
                    <td>
                      <div className="actions">
                        <button className="btn sm" onClick={() => purchase(p)} disabled={!manage}>
                          구매
                        </button>
                        {manage && (
                          <button
                            className="btn sm"
                            onClick={() => setEditing({ id: p.id, form: { name: p.name, price: p.price, stock: p.stock } })}
                          >
                            수정
                          </button>
                        )}
                        {manage && (
                          <button className="btn sm danger" onClick={() => remove(p)}>
                            삭제
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {editing && (
        <Modal
          title={editing.id ? '상품 수정' : '상품 등록'}
          onClose={() => setEditing(null)}
          footer={
            <>
              <button className="btn" onClick={() => setEditing(null)} disabled={busy}>
                취소
              </button>
              <button className="btn primary" onClick={save} disabled={busy}>
                {busy ? '저장 중…' : '저장'}
              </button>
            </>
          }
        >
          <label className="field">
            상품명
            <input value={editing.form.name} onChange={(e) => setForm({ name: e.target.value })} />
          </label>
          <label className="field">
            가격
            <input
              type="number"
              value={editing.form.price}
              onChange={(e) => setForm({ price: Number(e.target.value) })}
            />
          </label>
          <label className="field">
            재고
            <input
              type="number"
              value={editing.form.stock}
              onChange={(e) => setForm({ stock: Number(e.target.value) })}
            />
          </label>
        </Modal>
      )}
    </>
  );
}
