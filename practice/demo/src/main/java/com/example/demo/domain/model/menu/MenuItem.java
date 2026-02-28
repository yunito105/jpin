package com.example.demo.domain.model.menu;

import com.example.demo.domain.type.Money;

import java.util.Objects;

/**
 * メニュー項目を表すエンティティ。
 *
 * <p>店舗で提供する一つのメニュー項目（料理やドリンク）を表現する。
 * メニュー項目は名前、価格、カテゴリ、提供可否状態を持つ。</p>
 *
 * <h3>ビジネスルール</h3>
 * <ul>
 *   <li>メニュー項目は一意のIDを持つ</li>
 *   <li>価格は0円以上</li>
 *   <li>提供不可のメニュー項目は注文できない</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see MenuItemId
 * @see MenuCategory
 */
public class MenuItem {

    /** メニュー項目ID */
    private final MenuItemId id;

    /** メニュー名 */
    private final String name;

    /** 単価（税抜） */
    private final Money price;

    /** カテゴリ */
    private final MenuCategory category;

    /** 提供可否 */
    private boolean available;

    /**
     * メニュー項目を生成する。
     *
     * @param id       メニュー項目ID
     * @param name     メニュー名（null・空文字不可）
     * @param price    単価（税抜）
     * @param category カテゴリ
     * @throws IllegalArgumentException メニュー名が空の場合
     */
    public MenuItem(MenuItemId id, String name, Money price, MenuCategory category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("メニュー名は空にできません");
        }
        this.id = Objects.requireNonNull(id, "メニュー項目IDは必須です");
        this.name = name;
        this.price = Objects.requireNonNull(price, "価格は必須です");
        this.category = Objects.requireNonNull(category, "カテゴリは必須です");
        this.available = true;
    }

    /**
     * メニュー項目を提供可能にする。
     */
    public void enable() {
        this.available = true;
    }

    /**
     * メニュー項目を提供不可にする。
     */
    public void disable() {
        this.available = false;
    }

    /**
     * このメニュー項目が注文可能かどうかを判定する。
     *
     * @return 注文可能な場合 {@code true}
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * メニュー項目IDを取得する。
     *
     * @return メニュー項目ID
     */
    public MenuItemId getId() {
        return id;
    }

    /**
     * メニュー名を取得する。
     *
     * @return メニュー名
     */
    public String getName() {
        return name;
    }

    /**
     * 単価（税抜）を取得する。
     *
     * @return 単価
     */
    public Money getPrice() {
        return price;
    }

    /**
     * カテゴリを取得する。
     *
     * @return カテゴリ
     */
    public MenuCategory getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return id.equals(menuItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MenuItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category=" + category +
                ", available=" + available +
                '}';
    }
}
