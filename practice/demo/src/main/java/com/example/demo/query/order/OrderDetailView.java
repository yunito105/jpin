package com.example.demo.query.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 注文詳細ビュー（Query用DTO）。
 *
 * <p>注文詳細画面で表示する注文の全情報を表現する。
 * CQRSのQuery側で使用される読み取り専用のデータ構造。</p>
 *
 * @param orderId            注文ID
 * @param tableNumber        テーブル番号
 * @param status             注文ステータス
 * @param statusDisplayName  ステータス表示名
 * @param items              注文明細リスト
 * @param totalAmount        合計金額（税抜）
 * @param totalAmountWithTax 合計金額（税込）
 * @param orderedAt          注文日時
 * @author store-order-system
 * @since 1.0.0
 */
public record OrderDetailView(
        String orderId,
        int tableNumber,
        String status,
        String statusDisplayName,
        List<OrderItemView> items,
        BigDecimal totalAmount,
        BigDecimal totalAmountWithTax,
        LocalDateTime orderedAt
) {

    /**
     * 注文明細ビュー。
     *
     * @param menuItemId   メニュー項目ID
     * @param menuItemName メニュー名
     * @param unitPrice    単価（税抜）
     * @param quantity     数量
     * @param subtotal     小計（税抜）
     */
    public record OrderItemView(
            String menuItemId,
            String menuItemName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
    }
}
