package com.example.demo.application.order;

/**
 * 注文ステータス更新コマンド。
 *
 * <p>注文のステータスを次のステータスに遷移させるためのコマンドオブジェクト。
 * CQRSのCommand側で使用される。</p>
 *
 * @param orderId   対象の注文ID
 * @param newStatus 新しいステータス（PLACED, PREPARING, READY, SERVED, CANCELLED）
 * @author store-order-system
 * @since 1.0.0
 */
public record UpdateOrderStatusCommand(
        String orderId,
        String newStatus
) {
}
