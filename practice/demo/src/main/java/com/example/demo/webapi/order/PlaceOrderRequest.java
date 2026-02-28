package com.example.demo.webapi.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 注文作成リクエスト。
 *
 * <p>フロントエンドから送信される注文作成のリクエストボディ。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@Schema(description = "注文作成リクエスト")
public record PlaceOrderRequest(

        @Schema(description = "テーブル番号", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "テーブル番号は1以上である必要があります")
        int tableNumber,

        @Schema(description = "注文明細リスト")
        @NotEmpty(message = "注文明細は1件以上必要です")
        @Valid
        List<OrderItemRequest> items
) {

    /**
     * 注文明細リクエスト。
     */
    @Schema(description = "注文明細リクエスト")
    public record OrderItemRequest(

            @Schema(description = "メニュー項目ID", example = "main-001", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "メニュー項目IDは必須です")
            String menuItemId,

            @Schema(description = "数量", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @Min(value = 1, message = "数量は1以上である必要があります")
            int quantity
    ) {
    }
}
