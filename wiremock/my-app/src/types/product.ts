export type ProductStatus = 'AVAILABLE' | 'OUT_OF_STOCK' | 'DISCONTINUED';

export interface ProductDimensions {
  width: number;
  depth: number;
  height: number;
}

export interface Product {
  id: string;
  name: string;
  category: string;
  price: number;
  stock: number;
  status: ProductStatus;
  description?: string;
  dimensions?: ProductDimensions;
}

export interface ProductListResponse {
  products: Product[];
  total: number;
}
