package com.example.demo.domain.model.menu;

import java.util.List;
import java.util.Optional;

/**
 * メニュー項目リポジトリインターフェース。
 *
 * <p>メニュー項目の永続化を抽象化するリポジトリ。
 * ヘキサゴナルアーキテクチャにおけるポート（出力ポート）として機能する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see MenuItem
 */
public interface MenuItemRepository {

    /**
     * メニュー項目を保存する。
     *
     * <p>新規の場合は追加、既存の場合は更新する。</p>
     *
     * @param menuItem 保存するメニュー項目
     */
    void save(MenuItem menuItem);

    /**
     * メニュー項目IDで検索する。
     *
     * @param id メニュー項目ID
     * @return メニュー項目（存在しない場合は空）
     */
    Optional<MenuItem> findById(MenuItemId id);

    /**
     * 全メニュー項目を取得する。
     *
     * @return メニュー項目一覧
     */
    List<MenuItem> findAll();

    /**
     * カテゴリでメニュー項目を検索する。
     *
     * @param category カテゴリ
     * @return 該当するメニュー項目一覧
     */
    List<MenuItem> findByCategory(MenuCategory category);

    /**
     * 提供可能なメニュー項目のみを取得する。
     *
     * @return 提供可能なメニュー項目一覧
     */
    List<MenuItem> findAvailable();

    /**
     * メニュー項目を削除する。
     *
     * @param id 削除対象のメニュー項目ID
     */
    void deleteById(MenuItemId id);
}
