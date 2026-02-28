/**
 * メニュー項目ビュー。
 *
 * バックエンド `/api/menu` から返却されるメニュー1件分の表示用データ。
 *
 * @see {@link MenuCategory} カテゴリ区分
 */
export interface MenuItemView {
  /** メニューID（UUID形式） */
  menuItemId: string;
  /** メニュー名（例: "マルゲリータ"） */
  name: string;
  /** 税抜価格（円） */
  price: number;
  /** 税込価格（円・小数点以下切り捨て） */
  priceWithTax: number;
  /** カテゴリコード（例: "MAIN_COURSE"） */
  category: string;
  /** カテゴリ表示名（例: "メイン料理"） */
  categoryDisplayName: string;
  /** 提供可能フラグ */
  available: boolean;
}

/**
 * 注文サマリー。
 *
 * 注文一覧画面で表示する要約情報。バックエンド `/api/orders` から返却される。
 *
 * @see {@link OrderDetailView} 詳細ビュー
 * @see {@link OrderStatus} ステータス区分
 */
export interface OrderSummary {
  /** 注文ID（UUID形式） */
  orderId: string;
  /** テーブル番号（1以上の整数） */
  tableNumber: number;
  /** ステータスコード（例: "PLACED"） */
  status: string;
  /** ステータス表示名（例: "注文受付済み"） */
  statusDisplayName: string;
  /** 合計金額（税抜・円） */
  totalAmount: number;
  /** 合計金額（税込・円） */
  totalAmountWithTax: number;
  /** 注文明細の件数 */
  itemCount: number;
  /** 注文日時（ISO 8601形式） */
  orderedAt: string;
}

/**
 * 注文詳細ビュー。
 *
 * 注文の全情報（明細含む）。バックエンド `/api/orders/{orderId}` から返却される。
 *
 * @see {@link OrderItemView} 明細ビュー
 * @see {@link OrderSummary} サマリービュー
 */
export interface OrderDetailView {
  /** 注文ID（UUID形式） */
  orderId: string;
  /** テーブル番号 */
  tableNumber: number;
  /** ステータスコード */
  status: string;
  /** ステータス表示名 */
  statusDisplayName: string;
  /** 注文明細の一覧 */
  items: OrderItemView[];
  /** 合計金額（税抜・円） */
  totalAmount: number;
  /** 合計金額（税込・円） */
  totalAmountWithTax: number;
  /** 注文日時（ISO 8601形式） */
  orderedAt: string;
}

/**
 * 注文明細ビュー。
 *
 * 注文に含まれる個々のメニュー項目の情報。
 */
export interface OrderItemView {
  /** メニューID */
  menuItemId: string;
  /** メニュー名 */
  menuItemName: string;
  /** 単価（税込・円） */
  unitPrice: number;
  /** 数量 */
  quantity: number;
  /** 小計（税込・円） = unitPrice × quantity */
  subtotal: number;
}

/**
 * 注文作成リクエスト。
 *
 * `POST /api/orders` に送信するリクエストボディ。
 *
 * @see {@link OrderItemRequest} 明細リクエスト
 */
export interface PlaceOrderRequest {
  /** テーブル番号（1以上の整数） */
  tableNumber: number;
  /** 注文する商品のリスト（1件以上必須） */
  items: OrderItemRequest[];
}

/**
 * 注文明細リクエスト。
 *
 * 注文に含める個々の商品と数量。
 */
export interface OrderItemRequest {
  /** メニューID（UUID形式） */
  menuItemId: string;
  /** 数量（1以上の整数） */
  quantity: number;
}

/**
 * 注文ステータス。
 *
 * 注文のライフサイクルを表す区分値。
 *
 * | 値 | 意味 |
 * |---|---|
 * | PLACED | 注文受付済み |
 * | PREPARING | 調理中 |
 * | READY | 提供準備完了 |
 * | SERVED | 提供済み |
 * | CANCELLED | キャンセル |
 */
export type OrderStatus = "PLACED" | "PREPARING" | "READY" | "SERVED" | "CANCELLED";

/**
 * メニューカテゴリ。
 *
 * メニュー項目の分類を表す区分値。
 *
 * | 値 | 表示名 |
 * |---|---|
 * | APPETIZER | 前菜 |
 * | MAIN_COURSE | メイン料理 |
 * | SIDE_DISH | サイドメニュー |
 * | DRINK | ドリンク |
 * | DESSERT | デザート |
 */
export type MenuCategory = "APPETIZER" | "MAIN_COURSE" | "SIDE_DISH" | "DRINK" | "DESSERT";

/**
 * カテゴリ表示名マップ。
 *
 * {@link MenuCategory} コードから日本語表示名へ変換するための定数マップ。
 *
 * @example
 * ```ts
 * CATEGORY_LABELS["MAIN_COURSE"] // → "メイン料理"
 * ```
 */
export const CATEGORY_LABELS: Record<MenuCategory, string> = {
  APPETIZER: "前菜",
  MAIN_COURSE: "メイン料理",
  SIDE_DISH: "サイドメニュー",
  DRINK: "ドリンク",
  DESSERT: "デザート",
};

/**
 * ステータス表示名マップ。
 *
 * {@link OrderStatus} コードから日本語表示名へ変換するための定数マップ。
 *
 * @example
 * ```ts
 * STATUS_LABELS["PLACED"] // → "注文受付済み"
 * ```
 */
export const STATUS_LABELS: Record<OrderStatus, string> = {
  PLACED: "注文受付済み",
  PREPARING: "調理中",
  READY: "提供準備完了",
  SERVED: "提供済み",
  CANCELLED: "キャンセル",
};
