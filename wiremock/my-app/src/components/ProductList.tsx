import { useEffect, useState } from 'react';
import type { Product } from '../types/product';
import { fetchProducts } from '../api/productApi';

interface Props {
  onSelect: (id: string) => void;
}

const STATUS_LABEL: Record<string, string> = {
  AVAILABLE: '販売中',
  OUT_OF_STOCK: '在庫なし',
  DISCONTINUED: '廃番',
};

const STATUS_CLASS: Record<string, string> = {
  AVAILABLE: 'status-available',
  OUT_OF_STOCK: 'status-out',
  DISCONTINUED: 'status-discontinued',
};

export function ProductList({ onSelect }: Props) {
  const [products, setProducts] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProducts()
      .then((data) => {
        setProducts(data.products);
        setTotal(data.total);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="state-msg">読み込み中...</p>;
  if (error) return <p className="state-msg error">エラー: {error}</p>;

  return (
    <div className="product-list">
      <div className="list-header">
        <h2>商品一覧</h2>
        <span className="total-badge">{total} 件</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>商品ID</th>
            <th>商品名</th>
            <th>カテゴリ</th>
            <th>価格</th>
            <th>在庫数</th>
            <th>ステータス</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id} onClick={() => onSelect(p.id)} className="clickable-row">
              <td className="product-id">{p.id}</td>
              <td>{p.name}</td>
              <td>{p.category}</td>
              <td className="price">¥{p.price.toLocaleString()}</td>
              <td className="stock">{p.stock}</td>
              <td>
                <span className={`status-badge ${STATUS_CLASS[p.status] ?? ''}`}>
                  {STATUS_LABEL[p.status] ?? p.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
