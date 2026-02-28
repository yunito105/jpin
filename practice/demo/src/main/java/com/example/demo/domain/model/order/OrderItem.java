package com.example.demo.domain.model.order;

import com.example.demo.domain.model.menu.MenuItemId;
import com.example.demo.domain.type.Money;
import com.example.demo.domain.type.Quantity;

import java.util.Objects;

/**
 * 注文明細を表す値オブジェクト。
 *
 * <p>注文に含まれる個々のメニュー項目と数量の組み合わせを表現する。
 * 小計金額の計算ロジックを持つ。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see Order
 */
public class OrderItem {

    /** 注文対象のメニュー項目ID */
    private final MenuItemId menuItemId;

    /** メニュー名（表示用スナップショット） */
    private final String menuItemName;

    /** 注文時の単価（税抜） */
    private final Money unitPrice;

    /** 数量 */
    private final Quantity quantity;

    /**
     * 注文明細を生成する。
     *
     * @param menuItemId   メニュー項目ID
     * @param menuItemName メニュー名
     * @param unitPrice    注文時の単価（税抜）
     * @param quantity     数量
     */
    public OrderItem(MenuItemId menuItemId, String menuItemName, Money unitPrice, Quantity quantity) {
        this.menuItemId = Objects.requireNonNull(menuItemId, "メニュー項目IDは必須です");
        this.menuItemName = Objects.requireNonNull(menuItemName, "メニュー名は必須です");
        this.unitPrice = Objects.requireNonNull(unitPrice, "単価は必須です");
        this.quantity = Objects.requireNonNull(quantity, "数量は必須です");
    }

    /**
     * 小計金額（税抜）を計算する。
     *
     * <p>単価 × 数量で算出する。</p>
     *
     * @return 小計金額（税抜）
     */
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }

    /**
     * メニュー項目IDを取得する。
     *
     * @return メニュー項目ID
     */
    public MenuItemId getMenuItemId() {
        return menuItemId;
    }

    /**
     * メニュー名を取得する。
     *
     * @return メニュー名
     */
    public String getMenuItemName() {
        return menuItemName;
    }

    /**
     * 注文時の単価（税抜）を取得する。
     *
     * @return 単価
     */
    public Money getUnitPrice() {
        return unitPrice;
    }

    /**
     * 数量を取得する。
     *
     * @return 数量
     */
    public Quantity getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return menuItemId.equals(orderItem.menuItemId) &&
                unitPrice.equals(orderItem.unitPrice) &&
                quantity.equals(orderItem.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItemId, unitPrice, quantity);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "menuItemName='" + menuItemName + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                ", subtotal=" + subtotal() +
                '}';
    }
}
