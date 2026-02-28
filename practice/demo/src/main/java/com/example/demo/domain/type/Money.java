package com.example.demo.domain.type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額を表す値オブジェクト。
 *
 * <p>金額は日本円の整数値として扱い、税込・税抜の計算をサポートする。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public class Money {

    /** 消費税率（10%） */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    /** 金額（税抜） */
    private final BigDecimal amount;

    /**
     * 金額を生成する。
     *
     * @param amount 金額（税抜、0以上）
     * @throws IllegalArgumentException 金額が負の場合
     */
    public Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額は0以上である必要があります: " + amount);
        }
        this.amount = amount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 整数値から金額を生成する。
     *
     * @param amount 金額（税抜、0以上）
     * @return 金額オブジェクト
     */
    public static Money of(int amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    /**
     * ゼロ円を返す。
     *
     * @return ゼロ円の金額
     */
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    /**
     * 金額を加算する。
     *
     * @param other 加算する金額
     * @return 加算後の金額
     */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /**
     * 金額を数量で乗算する。
     *
     * @param quantity 数量
     * @return 乗算後の金額
     */
    public Money multiply(Quantity quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity.getValue())));
    }

    /**
     * 税込金額を計算する。
     *
     * @return 税込金額
     */
    public Money withTax() {
        BigDecimal taxIncluded = amount.add(amount.multiply(TAX_RATE));
        return new Money(taxIncluded.setScale(0, RoundingMode.HALF_UP));
    }

    /**
     * 金額の数値を取得する。
     *
     * @return 金額
     */
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return "¥" + amount.toPlainString();
    }
}
