/**
 * アプリケーションログ出力パッケージ。
 *
 * <p>プロジェクト全体で統一されたログ出力を提供する。
 * 直接 SLF4J の {@code Logger} を取得せず、
 * {@link com.example.demo.common.logging.AppLogger} を使用すること。</p>
 *
 * <h3>使用例</h3>
 * <pre>{@code
 * public class OrderApplicationService {
 *     private static final AppLogger log = AppLogger.of(OrderApplicationService.class);
 *
 *     public OrderId placeOrder(PlaceOrderCommand command) {
 *         log.info("注文を受け付けました: テーブル{}", command.tableNumber());
 *         // ...
 *         log.warn("在庫残りわずか: {}", menuItemId);
 *     }
 * }
 * }</pre>
 *
 * <h3>なぜ共通ロガーを使うのか</h3>
 * <ul>
 *   <li>ログフォーマットの統一（トレースID・操作コンテキストの自動付与）</li>
 *   <li>ログレベルポリシーの一元管理</li>
 *   <li>将来的な監視基盤との連携を容易にする</li>
 * </ul>
 *
 * @see com.example.demo.common.logging.AppLogger
 */
package com.example.demo.common.logging;
