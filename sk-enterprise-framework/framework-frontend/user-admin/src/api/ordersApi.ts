import { api } from './client';
import type { Order, PlaceOrderRequest } from '../types';

/** 주문 API (백엔드 OrderController 대응) - 멱등 생성 */
export const ordersApi = {
  place(req: PlaceOrderRequest): Promise<Order> {
    return api.post<Order>('/orders', req);
  },
  get(orderNo: string): Promise<Order> {
    return api.get<Order>(`/orders/${encodeURIComponent(orderNo)}`);
  },
};
