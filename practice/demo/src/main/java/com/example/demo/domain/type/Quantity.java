package com.example.demo.domain.type;

import java.util.Objects;

/**
 * 数量を表す値オブジェクト。
 *
 * <p>注文の商品数量を表現する。1以上の整数のみ許可する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public class Quantity {

    /** 数量 */
    private final int value;

    /**
     * 数量を生成する。
     *
     * @param value 数量（1以上）
     * @throws IllegalArgumentException 数量が1未満の場合
     */
    public Quantity(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("数量は1以上である必要があります: " + value);
        }
        this.value = value;
    }

    /**
     * 数量を生成するファクトリメソッド。
     *
     * @param value 数量（1以上）
     * @return 数量オブジェクト
     */
    public static Quantity of(int value) {
        return new Quantity(value);
    }

    /**
     * 数量の値を取得する。
     *
     * @return 数量
     */
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
