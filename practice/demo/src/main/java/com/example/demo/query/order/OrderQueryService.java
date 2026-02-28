package com.example.demo.query.order;

import com.example.demo.domain.model.order.Order;
import com.example.demo.domain.model.order.OrderId;
import com.example.demo.domain.model.order.OrderRepository;
import com.example.demo.domain.model.order.OrderStatus;
import com.example.demo.domain.type.TableNumber;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 注文クエリサービス（Query側）。
 *
 * <p>注文に関する参照系の操作を提供するサービス。
 * CQRSのQuery側として、読み取り専用のDTOを返却する。
 * ドメインモデルを直接返さず、Queryに特化したビュー用DTOに変換して返す。</p>
 *
 * <h3>責務</h3>
 * <ul>
 *   <li>注文一覧の取得（フィルタリング対応）</li>
 *   <li>注文詳細の取得</li>
 *   <li>ドメインモデルからQuery用DTOへの変換</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see OrderSummary
 * @see OrderDetailView
 */
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    /**
     * コンストラクタ。
     *
     * @param orderRepository 注文リポジトリ
     */
    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 全注文のサマリー一覧を取得する。
     *
     * @return 注文サマリー一覧
     */
    public List<OrderSummary> findAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * ステータスで絞り込んだ注文サマリー一覧を取得する。
     *
     * @param status 注文ステータス
     * @return 該当する注文サマリー一覧
     */
    public List<OrderSummary> findOrdersByStatus(String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status);
        return orderRepository.findByStatus(orderStatus).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * テーブル番号でアクティブな注文サマリー一覧を取得する。
     *
     * @param tableNumber テーブル番号
     * @return アクティブな注文サマリー一覧
     */
    public List<OrderSummary> findActiveOrdersByTable(int tableNumber) {
        return orderRepository.findActiveByTableNumber(TableNumber.of(tableNumber)).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * 注文詳細を取得する。
     *
     * @param orderId 注文ID
     * @return 注文詳細ビュー（存在しない場合は空）
     */
    public Optional<OrderDetailView> findOrderDetail(String orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .map(this::toDetailView);
    }

    /**
     * Order集約をOrderSummaryに変換する。
     */
    private OrderSummary toSummary(Order order) {
        return new OrderSummary(
                order.getId().getValue(),
                order.getTableNumber().getValue(),
                order.getStatus().name(),
                order.getStatus().getDisplayName(),
                order.totalAmount().getAmount(),
                order.totalAmountWithTax().getAmount(),
                order.getItems().size(),
                order.getOrderedAt()
        );
    }

    /**
     * Order集約をOrderDetailViewに変換する。
     */
    private OrderDetailView toDetailView(Order order) {
        List<OrderDetailView.OrderItemView> itemViews = order.getItems().stream()
                .map(item -> new OrderDetailView.OrderItemView(
                        item.getMenuItemId().getValue(),
                        item.getMenuItemName(),
                        item.getUnitPrice().getAmount(),
                        item.getQuantity().getValue(),
                        item.subtotal().getAmount()
                ))
                .collect(Collectors.toList());

        return new OrderDetailView(
                order.getId().getValue(),
                order.getTableNumber().getValue(),
                order.getStatus().name(),
                order.getStatus().getDisplayName(),
                itemViews,
                order.totalAmount().getAmount(),
                order.totalAmountWithTax().getAmount(),
                order.getOrderedAt()
        );
    }
}
