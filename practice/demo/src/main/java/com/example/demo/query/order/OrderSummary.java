package com.example.demo.query.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 注文サマリー（Query用DTO）。
 *
 * <p>注文一覧画面で表示する注文の概要情報を表現する。
 * CQRSのQuery側で使用される読み取り専用のデータ構造。</p>
 *
 * @param orderId          注文ID
 * @param tableNumber      テーブル番号
 * @param status           注文ステータス
 * @param statusDisplayName ステータス表示名
 * @param totalAmount      合計金額（税抜）
 * @param totalAmountWithTax 合計金額（税込）
 * @param itemCount        注文明細数
 * @param orderedAt        注文日時
 * @author store-order-system
 * @since 1.0.0
 */
public record OrderSummary(
        String orderId,
        int tableNumber,
        String status,
        String statusDisplayName,
        BigDecimal totalAmount,
        BigDecimal totalAmountWithTax,
        int itemCount,
        LocalDateTime orderedAt
) {
}
