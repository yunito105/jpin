package com.example.demo.common.exception;

import com.example.demo.common.SharedUtility;

/**
 * システム例外の基底クラス。
 *
 * <p>外部システム連携の障害やインフラ層の予期しないエラーなど、
 * システム起因の例外を表現する。
 * HTTP 500系のレスポンスに変換される。</p>
 *
 * <h3>使用例</h3>
 * <pre>{@code
 * try {
 *     // 外部API呼び出し
 * } catch (IOException e) {
 *     throw new SystemException("EXTERNAL_API_ERROR",
 *         "外部システムとの通信に失敗しました", e);
 * }
 * }</pre>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@SharedUtility(
        description = "システム例外の基底クラス（HTTP 500系）",
        targetLayers = {"infrastructure"},
        category = "例外"
)
public class SystemException extends RuntimeException {

    /** エラーコード */
    private final String errorCode;

    /**
     * システム例外を生成する。
     *
     * @param errorCode エラーコード（例: "DB_CONNECTION_ERROR"）
     * @param message   エラーメッセージ
     * @param cause     原因例外
     */
    public SystemException(String errorCode, String message, Throwable cause) {
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
