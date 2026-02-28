package com.example.demo.domain.model.order;

import java.util.Objects;
import java.util.UUID;

/**
 * 注文IDを表す値オブジェクト。
 *
 * <p>注文を一意に識別するためのIDを表現する。
 * UUIDベースで生成される。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public class OrderId {

    /** ID値 */
    private final String value;

    /**
     * 注文IDを生成する。
     *
     * @param value ID値（null・空文字不可）
     * @throws IllegalArgumentException ID値がnullまたは空文字の場合
     */
    public OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("注文IDは空にできません");
        }
        this.value = value;
    }

    /**
     * 新しい注文IDを自動生成する。
     *
     * @return 新しい注文ID
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    /**
     * 文字列から注文IDを復元する。
     *
     * @param value ID文字列
     * @return 注文ID
     */
    public static OrderId of(String value) {
        return new OrderId(value);
    }

    /**
     * ID値を取得する。
     *
     * @return ID値
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return value.equals(orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
