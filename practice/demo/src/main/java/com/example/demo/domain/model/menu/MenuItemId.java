package com.example.demo.domain.model.menu;

import java.util.Objects;
import java.util.UUID;

/**
 * メニュー項目IDを表す値オブジェクト。
 *
 * <p>メニュー項目を一意に識別するためのIDを表現する。
 * UUIDベースで生成される。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public class MenuItemId {

    /** ID値 */
    private final String value;

    /**
     * メニュー項目IDを生成する。
     *
     * @param value ID値（null・空文字不可）
     * @throws IllegalArgumentException ID値がnullまたは空文字の場合
     */
    public MenuItemId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("メニュー項目IDは空にできません");
        }
        this.value = value;
    }

    /**
     * 新しいメニュー項目IDを自動生成する。
     *
     * @return 新しいメニュー項目ID
     */
    public static MenuItemId generate() {
        return new MenuItemId(UUID.randomUUID().toString());
    }

    /**
     * 文字列からメニュー項目IDを復元する。
     *
     * @param value ID文字列
     * @return メニュー項目ID
     */
    public static MenuItemId of(String value) {
        return new MenuItemId(value);
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
        MenuItemId that = (MenuItemId) o;
        return value.equals(that.value);
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
