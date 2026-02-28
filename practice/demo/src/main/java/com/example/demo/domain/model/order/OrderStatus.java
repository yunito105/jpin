package com.example.demo.domain.model.order;

/**
 * 注文ステータスを表す列挙型。
 *
 * <p>注文のライフサイクルにおける状態遷移を定義する。</p>
 *
 * <h3>状態遷移図</h3>
 * <pre>
 *   PLACED → PREPARING → READY → SERVED
 *     ↓         ↓
 *   CANCELLED  CANCELLED
 * </pre>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public enum OrderStatus {

    /** 注文受付済み：注文が入った状態 */
    PLACED("注文受付済み"),

    /** 調理中：キッチンで調理が開始された状態 */
    PREPARING("調理中"),

    /** 提供準備完了：料理が完成し提供を待っている状態 */
    READY("提供準備完了"),

    /** 提供済み：お客様に料理が届けられた状態 */
    SERVED("提供済み"),

    /** キャンセル：注文がキャンセルされた状態 */
    CANCELLED("キャンセル");

    /** ステータスの表示名 */
    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * ステータスの表示名を取得する。
     *
     * @return 表示名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * このステータスから指定されたステータスへの遷移が有効かどうかを判定する。
     *
     * @param next 遷移先のステータス
     * @return 遷移可能な場合 {@code true}
     */
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY || next == CANCELLED;
            case READY -> next == SERVED;
            case SERVED, CANCELLED -> false;
        };
    }
}
