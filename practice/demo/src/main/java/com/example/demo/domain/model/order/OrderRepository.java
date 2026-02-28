package com.example.demo.domain.model.order;

import com.example.demo.domain.type.TableNumber;

import java.util.List;
import java.util.Optional;

/**
 * 注文リポジトリインターフェース。
 *
 * <p>注文集約の永続化を抽象化するリポジトリ。
 * ヘキサゴナルアーキテクチャにおけるポート（出力ポート）として機能する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see Order
 */
public interface OrderRepository {

    /**
     * 注文を保存する。
     *
     * <p>新規の場合は追加、既存の場合は更新する。</p>
     *
     * @param order 保存する注文
     */
    void save(Order order);

    /**
     * 注文IDで検索する。
     *
     * @param id 注文ID
     * @return 注文（存在しない場合は空）
     */
    Optional<Order> findById(OrderId id);

    /**
     * 全注文を取得する。
     *
     * @return 注文一覧
     */
    List<Order> findAll();

    /**
     * テーブル番号でアクティブな注文を検索する。
     *
     * <p>提供済み・キャンセル済みを除く未完了の注文を返す。</p>
     *
     * @param tableNumber テーブル番号
     * @return アクティブな注文一覧
     */
    List<Order> findActiveByTableNumber(TableNumber tableNumber);

    /**
     * ステータスで注文を検索する。
     *
     * @param status 注文ステータス
     * @return 該当する注文一覧
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 注文を削除する。
     *
     * @param id 削除対象の注文ID
     */
    void deleteById(OrderId id);
}
