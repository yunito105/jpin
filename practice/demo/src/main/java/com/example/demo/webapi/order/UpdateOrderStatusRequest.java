package com.example.demo.webapi.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 注文ステータス更新リクエスト。
 *
 * <p>フロントエンドから送信される注文ステータス更新のリクエストボディ。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@Schema(description = "注文ステータス更新リクエスト")
public record UpdateOrderStatusRequest(

        @Schema(description = "新しいステータス", example = "PREPARING",
                allowableValues = {"PLACED", "PREPARING", "READY", "SERVED", "CANCELLED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ステータスは必須です")
        String status
) {
}
