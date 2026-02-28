package com.example.demo.domain.model.menu;

/**
 * メニューカテゴリを表す列挙型。
 *
 * <p>店舗で提供するメニュー項目の分類を定義する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
public enum MenuCategory {

    /** 前菜 */
    APPETIZER("前菜"),

    /** メイン料理 */
    MAIN_COURSE("メイン料理"),

    /** サイドメニュー */
    SIDE_DISH("サイドメニュー"),

    /** ドリンク */
    DRINK("ドリンク"),

    /** デザート */
    DESSERT("デザート");

    /** カテゴリの表示名 */
    private final String displayName;

    MenuCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * カテゴリの表示名を取得する。
     *
     * @return 表示名
     */
    public String getDisplayName() {
        return displayName;
    }
}
