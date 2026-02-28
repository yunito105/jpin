package com.example.demo.application.order;

import com.example.demo.common.logging.AppLogger;
import com.example.demo.domain.model.menu.MenuItem;
import com.example.demo.domain.model.menu.MenuItemId;
import com.example.demo.domain.model.menu.MenuItemRepository;
import com.example.demo.domain.model.order.*;
import com.example.demo.domain.type.Money;
import com.example.demo.domain.type.Quantity;
import com.example.demo.domain.type.TableNumber;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 注文アプリケーションサービス（Command側）。
 *
 * <p>注文に関するコマンド（作成・更新・キャンセル）を処理するサービス。
 * ヘキサゴナルアーキテクチャにおける入力ポートのアダプタとして機能し、
 * ドメインモデルを呼び出してビジネスロジックを実行する。</p>
 *
 * <h3>責務</h3>
 * <ul>
 *   <li>コマンドオブジェクトの受付とバリデーション</li>
 *   <li>ドメインオブジェクトへの変換と操作の委譲</li>
 *   <li>リポジトリを介した永続化</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see PlaceOrderCommand
 * @see UpdateOrderStatusCommand
 */
@Service
public class OrderApplicationService {

    private static final AppLogger log = AppLogger.of(OrderApplicationService.class);

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * コンストラクタ。
     *
     * @param orderRepository    注文リポジトリ
     * @param menuItemRepository メニュー項目リポジトリ
     */
    public OrderApplicationService(OrderRepository orderRepository,
                                    MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
    }

    /**
     * 注文を作成する。
     *
     * <p>メニュー項目の存在確認・提供可否チェックを行った上で、
     * ドメインモデルの注文を作成し永続化する。</p>
     *
     * @param command 注文作成コマンド
     * @return 作成された注文ID
     * @throws IllegalArgumentException メニュー項目が存在しないまたは提供不可の場合
     */
    public OrderId placeOrder(PlaceOrderCommand command) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (PlaceOrderCommand.OrderItemCommand itemCommand : command.items()) {
            MenuItemId menuItemId = MenuItemId.of(itemCommand.menuItemId());
            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "メニュー項目が見つかりません: " + itemCommand.menuItemId()));

            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException(
                        "このメニュー項目は現在提供できません: " + menuItem.getName());
            }

            OrderItem orderItem = new OrderItem(
                    menuItem.getId(),
                    menuItem.getName(),
                    menuItem.getPrice(),
                    Quantity.of(itemCommand.quantity())
            );
            orderItems.add(orderItem);
        }

        OrderId orderId = OrderId.generate();
        TableNumber tableNumber = TableNumber.of(command.tableNumber());
        Order order = new Order(orderId, tableNumber, orderItems);

        orderRepository.save(order);

        log.info("注文を受け付けました: 注文ID={}, テーブル={}, 明細数={}",
                orderId.getValue(), command.tableNumber(), orderItems.size());

        return orderId;
    }

    /**
     * 注文ステータスを更新する。
     *
     * <p>ドメインモデルのステータス遷移ルールに従い、
     * 注文のステータスを変更する。</p>
     *
     * @param command ステータス更新コマンド
     * @throws IllegalArgumentException 注文が存在しない場合
     * @throws IllegalStateException    不正なステータス遷移の場合
     */
    public void updateOrderStatus(UpdateOrderStatusCommand command) {
        OrderId orderId = OrderId.of(command.orderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "注文が見つかりません: " + command.orderId()));

        OrderStatus newStatus = OrderStatus.valueOf(command.newStatus());
        order.changeStatus(newStatus);
        orderRepository.save(order);

        log.info("注文ステータスを更新しました: 注文ID={}, 新ステータス={}",
                command.orderId(), newStatus.getDisplayName());
    }

    /**
     * 注文をキャンセルする。
     *
     * @param orderId キャンセル対象の注文ID
     * @throws IllegalArgumentException 注文が存在しない場合
     * @throws IllegalStateException    キャンセルできないステータスの場合
     */
    public void cancelOrder(String orderId) {
        OrderId id = OrderId.of(orderId);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "注文が見つかりません: " + orderId));

        order.cancel();
        orderRepository.save(order);

        log.info("注文をキャンセルしました: 注文ID={}", orderId);
    }
}
