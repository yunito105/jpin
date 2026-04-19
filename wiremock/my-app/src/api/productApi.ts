import type { Product, ProductListResponse } from '../types/product';

const BASE_URL = '/api/v1';

export async function fetchProducts(): Promise<ProductListResponse> {
  const res = await fetch(`${BASE_URL}/products`);
  if (!res.ok) throw new Error(`商品一覧の取得に失敗しました (${res.status})`);
  return res.json();
}

export async function fetchProductDetail(id: string): Promise<Product> {
  const res = await fetch(`${BASE_URL}/products/${id}`);
  if (!res.ok) throw new Error(`商品詳細の取得に失敗しました (${res.status})`);
  return res.json();
}
