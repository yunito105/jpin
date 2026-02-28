package com.example.demo.common.exception;

import com.example.demo.common.SharedUtility;

/**
 * 業務例外の基底クラス。
 *
 * <p>ビジネスルール違反やバリデーションエラーなど、
 * 業務上想定される例外を表現する。
 * HTTP 400系のレスポンスに変換される。</p>
 *
 * <h3>使用例</h3>
 * <pre>{@code
 * if (!menuItem.isAvailable()) {
 *     throw new BusinessException("MENU_UNAVAILABLE",
 *         "このメニュー項目は現在提供できません: " + menuItem.getName());
 * }
 * }</pre>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@SharedUtility(
        description = "業務例外の基底クラス（HTTP 400系）",
        targetLayers = {"domain", "application"},
        category = "例外"
)
public class BusinessException extends RuntimeException {

    /** エラーコード */
    private final String errorCode;

    /**
     * 業務例外を生成する。
     *
     * @param errorCode エラーコード（例: "ORDER_NOT_FOUND"）
     * @param message   エラーメッセージ
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 業務例外を生成する（原因例外付き）。
     *
     * @param errorCode エラーコード
     * @param message   エラーメッセージ
     * @param cause     原因例外
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * エラーコードを取得する。
     *
     * @return エラーコード
     */
    public String getErrorCode() {
        return errorCode;
    }
}
