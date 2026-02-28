package com.example.demo.domain.model.order;

import com.example.demo.domain.type.Money;
import com.example.demo.domain.type.TableNumber;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 注文を表す集約ルートエンティティ。
 *
 * <p>実店舗におけるテーブル単位の注文を管理する。
 * 注文は複数の注文明細（{@link OrderItem}）を持ち、
 * ステータスに応じた状態遷移のビジネスルールを保持する。</p>
 *
 * <h3>集約の不変条件</h3>
 * <ul>
 *   <li>注文には1つ以上の注文明細が必要</li>
 *   <li>ステータス遷移は {@link OrderStatus} の遷移規則に従う</li>
 *   <li>キャンセル済み・提供済みの注文は変更不可</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see OrderId
 * @see OrderItem
 * @see OrderStatus
 */
public class Order {

    /** 注文ID */
    private final OrderId id;

    /** テーブル番号 */
    private final TableNumber tableNumber;

    /** 注文明細リスト */
    private final List<OrderItem> items;

    /** 注文ステータス */
    private OrderStatus status;

    /** 注文日時 */
    private final LocalDateTime orderedAt;

    /**
     * 新規注文を作成する。
     *
     * <p>注文は「注文受付済み（PLACED）」状態で作成される。</p>
     *
     * @param id          注文ID
     * @param tableNumber テーブル番号
     * @param items       注文明細リスト（1件以上必須）
     * @throws IllegalArgumentException 注文明細が空の場合
     */
    public Order(OrderId id, TableNumber tableNumber, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("注文には1つ以上の注文明細が必要です");
        }
        this.id = Objects.requireNonNull(id, "注文IDは必須です");
        this.tableNumber = Objects.requireNonNull(tableNumber, "テーブル番号は必須です");
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PLACED;
        this.orderedAt = LocalDateTime.now();
    }

    /**
     * 永続化データから注文を復元する（リコンストラクタ）。
     *
     * @param id          注文ID
     * @param tableNumber テーブル番号
     * @param items       注文明細リスト
     * @param status      注文ステータス
     * @param orderedAt   注文日時
     * @return 復元された注文
     */
    public static Order reconstruct(OrderId id, TableNumber tableNumber,
                                     List<OrderItem> items, OrderStatus status,
                                     LocalDateTime orderedAt) {
        Order order = new Order(id, tableNumber, items);
        order.status = status;
        return order;
    }

    /**
     * 注文ステータスを更新する。
     *
     * <p>ステータスの遷移は {@link OrderStatus#canTransitionTo(OrderStatus)} の
     * ルールに従う。不正な遷移を行った場合は例外がスローされる。</p>
     *
     * @param newStatus 新しいステータス
     * @throws IllegalStateException 不正なステータス遷移の場合
     */
    public void changeStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("注文ステータスを %s から %s に変更できません",
                            this.status.getDisplayName(), newStatus.getDisplayName()));
        }
        this.status = newStatus;
    }

    /**
     * 注文をキャンセルする。
     *
     * @throws IllegalStateException キャンセルできないステータスの場合
     */
    public void cancel() {
        changeStatus(OrderStatus.CANCELLED);
    }

    /**
     * 合計金額（税抜）を計算する。
     *
     * @return 合計金額（税抜）
     */
    public Money totalAmount() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(), Money::add);
    }

    /**
     * 合計金額（税込）を計算する。
     *
     * @return 合計金額（税込）
     */
    public Money totalAmountWithTax() {
        return totalAmount().withTax();
    }

    /**
     * この注文がアクティブ（未完了）かどうかを判定する。
     *
     * @return アクティブな場合 {@code true}
     */
    public boolean isActive() {
        return status != OrderStatus.SERVED && status != OrderStatus.CANCELLED;
    }

    // --- Getter ---

    /**
     * 注文IDを取得する。
     *
     * @return 注文ID
     */
    public OrderId getId() {
        return id;
    }

    /**
     * テーブル番号を取得する。
     *
     * @return テーブル番号
     */
    public TableNumber getTableNumber() {
        return tableNumber;
    }

    /**
     * 注文明細リスト（不変）を取得する。
     *
     * @return 注文明細リスト
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * 注文ステータスを取得する。
     *
     * @return 注文ステータス
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * 注文日時を取得する。
     *
     * @return 注文日時
     */
    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", tableNumber=" + tableNumber +
                ", status=" + status +
                ", items=" + items.size() + "件" +
                ", totalAmount=" + totalAmount() +
                '}';
    }
}
