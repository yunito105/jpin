package com.example.demo.query.menu;

import java.math.BigDecimal;

/**
 * メニュー項目ビュー（Query用DTO）。
 *
 * <p>メニュー一覧画面で表示するメニュー項目の情報を表現する。
 * CQRSのQuery側で使用される読み取り専用のデータ構造。</p>
 *
 * @param menuItemId  メニュー項目ID
 * @param name        メニュー名
 * @param price       単価（税抜）
 * @param priceWithTax 単価（税込）
 * @param category    カテゴリ
 * @param categoryDisplayName カテゴリ表示名
 * @param available   提供可否
 * @author store-order-system
 * @since 1.0.0
 */
public record MenuItemView(
        String menuItemId,
        String name,
        BigDecimal price,
        BigDecimal priceWithTax,
        String category,
        String categoryDisplayName,
        boolean available
) {
}
