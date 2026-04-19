import { useEffect, useState } from 'react';
import type { Product } from '../types/product';
import { fetchProductDetail } from '../api/productApi';

interface Props {
  productId: string;
  onBack: () => void;
}

const STATUS_LABEL: Record<string, string> = {
  AVAILABLE: '販売中',
  OUT_OF_STOCK: '在庫なし',
  DISCONTINUED: '廃番',
};

export function ProductDetail({ productId, onBack }: Props) {
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetchProductDetail(productId)
      .then(setProduct)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [productId]);

  if (loading) return <p className="state-msg">読み込み中...</p>;
  if (error) return <p className="state-msg error">エラー: {error}</p>;
  if (!product) return null;

  return (
    <div className="product-detail">
      <button className="back-btn" onClick={onBack}>
        ← 一覧に戻る
      </button>

      <div className="detail-card">
        <div className="detail-header">
          <span className="detail-id">{product.id}</span>
          <h2>{product.name}</h2>
          <span className="detail-category">{product.category}</span>
        </div>

        <div className="detail-body">
          <div className="detail-row">
            <span className="label">価格</span>
            <span className="value price-large">¥{product.price.toLocaleString()}</span>
          </div>
          <div className="detail-row">
            <span className="label">在庫数</span>
            <span className="value">{product.stock} 個</span>
          </div>
          <div className="detail-row">
            <span className="label">ステータス</span>
            <span className="value">{STATUS_LABEL[product.status] ?? product.status}</span>
          </div>
          {product.description && (
            <div className="detail-row description-row">
              <span className="label">説明</span>
              <span className="value">{product.description}</span>
            </div>
          )}
          {product.dimensions && (
            <div className="detail-row">
              <span className="label">サイズ (cm)</span>
              <span className="value">
                幅 {product.dimensions.width} × 奥行 {product.dimensions.depth} × 高さ {product.dimensions.height}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
