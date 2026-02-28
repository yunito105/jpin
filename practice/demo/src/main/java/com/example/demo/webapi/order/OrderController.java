package com.example.demo.webapi.order;

import com.example.demo.application.order.OrderApplicationService;
import com.example.demo.application.order.PlaceOrderCommand;
import com.example.demo.application.order.UpdateOrderStatusCommand;
import com.example.demo.domain.model.order.OrderId;
import com.example.demo.query.order.OrderDetailView;
import com.example.demo.query.order.OrderQueryService;
import com.example.demo.query.order.OrderSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 注文API コントローラ。
 *
 * <p>注文に関するREST APIエンドポイントを提供する。
 * ヘキサゴナルアーキテクチャにおける入力アダプタ（プレゼンテーション層）として、
 * HTTPリクエストを受け取り、アプリケーション層に処理を委譲する。</p>
 *
 * <h3>エンドポイント一覧</h3>
 * <ul>
 *   <li>{@code POST /api/orders} - 注文作成</li>
 *   <li>{@code GET /api/orders} - 注文一覧取得</li>
 *   <li>{@code GET /api/orders/{orderId}} - 注文詳細取得</li>
 *   <li>{@code PATCH /api/orders/{orderId}/status} - 注文ステータス更新</li>
 *   <li>{@code POST /api/orders/{orderId}/cancel} - 注文キャンセル</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "注文API", description = "注文の作成・取得・ステータス管理を行うAPI")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private final OrderApplicationService orderApplicationService;
    private final OrderQueryService orderQueryService;

    /**
     * コンストラクタ。
     *
     * @param orderApplicationService 注文アプリケーションサービス（Command）
     * @param orderQueryService       注文クエリサービス（Query）
     */
    public OrderController(OrderApplicationService orderApplicationService,
                            OrderQueryService orderQueryService) {
        this.orderApplicationService = orderApplicationService;
        this.orderQueryService = orderQueryService;
    }

    /**
     * 注文を作成する。
     *
     * @param request 注文作成リクエスト
     * @return 作成された注文ID
     */
    @PostMapping
    @Operation(summary = "注文を作成する", description = "テーブル番号とメニュー項目を指定して新しい注文を作成する")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注文作成成功"),
            @ApiResponse(responseCode = "400", description = "リクエスト不正（メニュー不存在・提供不可等）")
    })
    public ResponseEntity<Map<String, String>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        PlaceOrderCommand command = new PlaceOrderCommand(
                request.tableNumber(),
                request.items().stream()
                        .map(item -> new PlaceOrderCommand.OrderItemCommand(
                                item.menuItemId(), item.quantity()))
                        .collect(Collectors.toList())
        );

        OrderId orderId = orderApplicationService.placeOrder(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("orderId", orderId.getValue()));
    }

    /**
     * 注文一覧を取得する。
     *
     * @param status ステータスフィルタ（任意）
     * @param tableNumber テーブル番号フィルタ（任意）
     * @return 注文サマリー一覧
     */
    @GetMapping
    @Operation(summary = "注文一覧を取得する", description = "ステータスやテーブル番号で絞り込み可能な注文一覧を取得する")
    @ApiResponse(responseCode = "200", description = "取得成功")
    public ResponseEntity<List<OrderSummary>> getOrders(
            @Parameter(description = "ステータスフィルタ", example = "PLACED")
            @RequestParam(required = false) String status,
            @Parameter(description = "テーブル番号フィルタ", example = "5")
            @RequestParam(required = false) Integer tableNumber) {

        List<OrderSummary> orders;

        if (status != null) {
            orders = orderQueryService.findOrdersByStatus(status);
        } else if (tableNumber != null) {
            orders = orderQueryService.findActiveOrdersByTable(tableNumber);
        } else {
            orders = orderQueryService.findAllOrders();
        }

        return ResponseEntity.ok(orders);
    }

    /**
     * 注文詳細を取得する。
     *
     * @param orderId 注文ID
     * @return 注文詳細ビュー
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "注文詳細を取得する", description = "指定された注文IDの詳細情報を取得する")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取得成功"),
            @ApiResponse(responseCode = "404", description = "注文が見つからない")
    })
    public ResponseEntity<OrderDetailView> getOrderDetail(
            @Parameter(description = "注文ID", required = true)
            @PathVariable String orderId) {

        return orderQueryService.findOrderDetail(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 注文ステータスを更新する。
     *
     * @param orderId 注文ID
     * @param request ステータス更新リクエスト
     * @return 204 No Content
     */
    @PatchMapping("/{orderId}/status")
    @Operation(summary = "注文ステータスを更新する", description = "注文のステータスを次の段階に遷移させる")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "不正なステータス遷移"),
            @ApiResponse(responseCode = "404", description = "注文が見つからない")
    })
    public ResponseEntity<Void> updateOrderStatus(
            @Parameter(description = "注文ID", required = true)
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(orderId, request.status());
        orderApplicationService.updateOrderStatus(command);

        return ResponseEntity.noContent().build();
    }

    /**
     * 注文をキャンセルする。
     *
     * @param orderId キャンセル対象の注文ID
     * @return 204 No Content
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "注文をキャンセルする", description = "指定された注文をキャンセルする")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "キャンセル成功"),
            @ApiResponse(responseCode = "400", description = "キャンセルできないステータス"),
            @ApiResponse(responseCode = "404", description = "注文が見つからない")
    })
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "注文ID", required = true)
            @PathVariable String orderId) {

        orderApplicationService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
