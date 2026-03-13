-- ============================================
-- デモ用: 商品テーブル + サンプルデータ
-- コンテナ初回起動時に自動実行される
-- ============================================

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    price       INTEGER NOT NULL,
    category    VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- IKEA/ニトリ/カインズ風のサンプルデータ
INSERT INTO products (name, price, category) VALUES
    ('KALLAX Shelf Unit',       7990,  'Furniture'),
    ('MALM Bed Frame',         29990,  'Furniture'),
    ('LED Desk Lamp',           2990,  'Lighting'),
    ('Cotton Bath Towel Set',   1990,  'Textile'),
    ('Stainless Kitchen Rack',  4990,  'Kitchen');
