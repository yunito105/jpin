/**
 * APIクライアントモジュール。
 *
 * バックエンドREST APIとの通信を抽象化する。
 * 各エンドポイントごとに型安全なメソッドを提供し、
 * コンポーネントからは `menuApi` / `orderApi` をインポートして使用する。
 *
 * @example
 * ```ts
 * import { menuApi, orderApi } from "@/lib/api";
 *
 * // メニュー一覧取得
 * const items = await menuApi.getAll();
 *
 * // 注文作成
 * const result = await orderApi.place({ tableNumber: 1, items: [...] });
 * ```
 *
 * @module api
 */
import axios from "axios";
import {
  MenuItemView,
  OrderSummary,
  OrderDetailView,
  PlaceOrderRequest,
} from "@/types";

/**
 * Axios インスタンス。
 *
 * ベースURLと共通ヘッダーを設定済みのHTTPクライアント。
 * 全てのAPI呼び出しはこのインスタンスを経由する。
 */
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * メニューAPI。
 *
 * メニュー項目の取得に関するAPI呼び出しを提供する。
 *
 * @example
 * ```ts
 * const all = await menuApi.getAll();
 * const drinks = await menuApi.getByCategory("DRINK");
 * ```
 */
export const menuApi = {
  /**
   * 全メニュー取得。
   * @returns 全メニュー項目の配列
   */
  getAll: () => api.get<MenuItemView[]>("/menu").then((res) => res.data),

  /**
   * 提供可能メニュー取得。
   * `available === true` のメニューのみ返却される。
   * @returns 提供可能なメニュー項目の配列
   */
  getAvailable: () =>
    api.get<MenuItemView[]>("/menu/available").then((res) => res.data),

  /**
   * カテゴリ別メニュー取得。
   * @param category - カテゴリコード（例: "MAIN_COURSE"）
   * @returns 指定カテゴリのメニュー項目の配列
   */
  getByCategory: (category: string) =>
    api
      .get<MenuItemView[]>("/menu", { params: { category } })
      .then((res) => res.data),
};

/**
 * 注文API。
 *
 * 注文の作成・取得・ステータス更新・キャンセルに関するAPI呼び出しを提供する。
 *
 * @example
 * ```ts
 * // 注文作成
 * const result = await orderApi.place({ tableNumber: 1, items: [{ menuItemId: "xxx", quantity: 2 }] });
 *
 * // 注文一覧取得（ステータス指定）
 * const orders = await orderApi.getAll({ status: "PLACED" });
 * ```
 */
export const orderApi = {
  /**
   * 注文作成。
   * @param request - テーブル番号と注文明細を含むリクエスト
   * @returns 作成された注文のorderId
   */
  place: (request: PlaceOrderRequest) =>
    api
      .post<{ orderId: string }>("/orders", request)
      .then((res) => res.data),

  /**
   * 注文一覧取得。
   * @param params - 絞り込み条件（任意）
   * @param params.status - ステータスで絞り込み（例: "PLACED"）
   * @param params.tableNumber - テーブル番号で絞り込み
   * @returns 注文サマリーの配列
   */
  getAll: (params?: { status?: string; tableNumber?: number }) =>
    api.get<OrderSummary[]>("/orders", { params }).then((res) => res.data),

  /**
   * 注文詳細取得。
   * @param orderId - 注文ID（UUID形式）
   * @returns 注文の詳細情報（明細含む）
   */
  getDetail: (orderId: string) =>
    api
      .get<OrderDetailView>(`/orders/${orderId}`)
      .then((res) => res.data),

  /**
   * 注文ステータス更新。
   * @param orderId - 注文ID
   * @param status - 新しいステータス（例: "PREPARING"）
   */
  updateStatus: (orderId: string, status: string) =>
    api.patch(`/orders/${orderId}/status`, { status }),

  /**
   * 注文キャンセル。
   * @param orderId - キャンセルする注文のID
   */
  cancel: (orderId: string) => api.post(`/orders/${orderId}/cancel`),
};
