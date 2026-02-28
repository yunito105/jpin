package com.example.demo.application.order;

import java.util.List;

/**
 * 注文作成コマンド。
 *
 * <p>フロントエンドからの注文リクエストをアプリケーション層で受け取るためのコマンドオブジェクト。
 * CQRSのCommand側で使用される。</p>
 *
 * @param tableNumber テーブル番号
 * @param items       注文明細リスト
 * @author store-order-system
 * @since 1.0.0
 */
public record PlaceOrderCommand(
        int tableNumber,
        List<OrderItemCommand> items
) {

    /**
     * 注文明細コマンド。
     *
     * @param menuItemId メニュー項目ID
     * @param quantity   数量
     */
    public record OrderItemCommand(
            String menuItemId,
            int quantity
    ) {
    }
}
