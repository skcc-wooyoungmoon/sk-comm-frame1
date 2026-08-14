import { api } from './client';
import type { Product, ProductRequest } from '../types';

/** 상품 관리 API (백엔드 ProductController 대응) */
export const productsApi = {
  list(keyword?: string): Promise<Product[]> {
    const q = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    return api.get<Product[]>(`/products${q}`);
  },
  get(id: number): Promise<Product> {
    return api.get<Product>(`/products/${id}`);
  },
  create(req: ProductRequest): Promise<Product> {
    return api.post<Product>('/products', req);
  },
  update(id: number, req: ProductRequest): Promise<Product> {
    // 낙관적 락 충돌 시 클라이언트도 최대 3회 재시도
    return api.put<Product>(`/products/${id}`, req, { retryOnConflict: 3 });
  },
  purchase(id: number, quantity: number): Promise<Product> {
    return api.post<Product>(`/products/${id}/purchase`, { quantity }, { retryOnConflict: 3 });
  },
  remove(id: number): Promise<void> {
    return api.delete<void>(`/products/${id}`);
  },
};
