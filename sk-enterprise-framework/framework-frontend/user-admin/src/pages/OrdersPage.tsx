import { useState } from 'react';
import { ordersApi } from '../api/ordersApi';
import { ApiError } from '../api/client';
import { useToast } from '../components/Toast';
import { generateOrderNo } from '../lib/idempotency';
import { formatNumber } from '../lib/format';
import type { Order, PlaceOrderRequest } from '../types';

export function OrdersPage() {
  const toast = useToast();
  const [form, setForm] = useState<PlaceOrderRequest>({
    orderNo: generateOrderNo(),
    customerId: 'C1',
    productId: 'P1',
    quantity: 1,
    amount: 10000,
  });
  const [last, setLast] = useState<Order | null>(null);
  const [busy, setBusy] = useState(false);
  const [lookupNo, setLookupNo] = useState('');
  const [looked, setLooked] = useState<Order | null>(null);

  const set = (patch: Partial<PlaceOrderRequest>) => setForm((p) => ({ ...p, ...patch }));

  const place = async () => {
    setBusy(true);
    try {
      const order = await ordersApi.place(form);
      setLast(order);
      toast.success(`주문 처리됨 (id=${order.id})`);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : '주문 실패');
    } finally {
      setBusy(false);
    }
  };

  const lookup = async () => {
    if (!lookupNo) return;
    try {
      setLooked(await ordersApi.get(lookupNo));
    } catch (e) {
      setLooked(null);
      toast.error(e instanceof ApiError ? e.message : '주문을 찾을 수 없습니다.');
    }
  };

  return (
    <div className="row" style={{ alignItems: 'flex-start' }}>
      <div className="card grow" style={{ minWidth: 340 }}>
        <div className="card-head">
          <h2>주문 생성 (멱등)</h2>
          <button className="btn sm" onClick={() => set({ orderNo: generateOrderNo() })}>
            새 주문번호
          </button>
        </div>
        <div className="modal-body">
          <label className="field">
            주문번호 (멱등키)
            <input value={form.orderNo} onChange={(e) => set({ orderNo: e.target.value })} />
          </label>
          <div className="row">
            <label className="field grow">
              고객 ID
              <input value={form.customerId} onChange={(e) => set({ customerId: e.target.value })} />
            </label>
            <label className="field grow">
              상품 ID
              <input value={form.productId} onChange={(e) => set({ productId: e.target.value })} />
            </label>
          </div>
          <div className="row">
            <label className="field grow">
              수량
              <input type="number" value={form.quantity} onChange={(e) => set({ quantity: Number(e.target.value) })} />
            </label>
            <label className="field grow">
              금액
              <input type="number" value={form.amount} onChange={(e) => set({ amount: Number(e.target.value) })} />
            </label>
          </div>
          <button className="btn primary" onClick={place} disabled={busy}>
            {busy ? '처리 중…' : '주문 생성'}
          </button>
          <button className="btn" onClick={place} disabled={busy} title="같은 주문번호로 재전송 → 멱등 확인">
            🔁 같은 주문번호로 재전송 (멱등 테스트)
          </button>
          <p className="muted" style={{ fontSize: 12 }}>
            같은 주문번호로 여러 번 눌러도 새 주문이 생기지 않고 동일한 id가 반환됩니다.(백엔드 @Idempotent)
          </p>

          {last && (
            <div className="result-box">
              <b>최근 응답</b>
              <div>id: {last.id}</div>
              <div>주문번호: {last.orderNo}</div>
              <div>금액: ₩{formatNumber(last.amount)}</div>
              <div>상태: {last.status}</div>
            </div>
          )}
        </div>
      </div>

      <div className="card grow" style={{ minWidth: 300 }}>
        <div className="card-head">
          <h2>주문 조회</h2>
        </div>
        <div className="modal-body">
          <div className="row">
            <input
              className="grow"
              placeholder="주문번호"
              value={lookupNo}
              onChange={(e) => setLookupNo(e.target.value)}
            />
            <button className="btn" onClick={lookup}>
              조회
            </button>
          </div>
          {looked && (
            <div className="result-box">
              <div>id: {looked.id}</div>
              <div>주문번호: {looked.orderNo}</div>
              <div>고객: {looked.customerId}</div>
              <div>상품: {looked.productId} × {looked.quantity}</div>
              <div>금액: ₩{formatNumber(looked.amount)}</div>
              <div>상태: {looked.status}</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
