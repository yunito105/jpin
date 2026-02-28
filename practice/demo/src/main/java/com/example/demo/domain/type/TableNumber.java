package com.example.demo.domain.type;

import java.util.Objects;

/**
 * テーブル番号を表す値オブジェクト。
 *
 * <p>実店舗におけるテーブル（座席）の識別番号を表現する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public class TableNumber {

    /** テーブル番号 */
    private final int value;

    /**
     * テーブル番号を生成する。
     *
     * @param value テーブル番号（1以上）
     * @throws IllegalArgumentException テーブル番号が1未満の場合
     */
    public TableNumber(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("テーブル番号は1以上である必要があります: " + value);
        }
        this.value = value;
    }

    /**
     * テーブル番号を生成するファクトリメソッド。
     *
     * @param value テーブル番号（1以上）
     * @return テーブル番号オブジェクト
     */
    public static TableNumber of(int value) {
        return new TableNumber(value);
    }

    /**
     * テーブル番号の値を取得する。
     *
     * @return テーブル番号
     */
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableNumber that = (TableNumber) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "テーブル" + value;
    }
}
