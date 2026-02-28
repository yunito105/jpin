package com.example.demo.common.logging;

import com.example.demo.common.SharedUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * アプリケーション共通ロガー。
 *
 * <p>プロジェクト全体でログ出力を統一するためのラッパークラス。
 * 全てのログ出力はこのクラスを経由して行うこと。</p>
 *
 * <h3>使用方法</h3>
 * <pre>{@code
 * // クラスフィールドとして宣言
 * private static final AppLogger log = AppLogger.of(MyClass.class);
 *
 * // ログ出力
 * log.info("処理を開始します: {}", param);
 * log.warn("想定外の状態: {}", state);
 * log.error("処理に失敗しました", exception);
 * }</pre>
 *
 * <h3>ログレベルポリシー</h3>
 * <ul>
 *   <li>{@code DEBUG} - 開発時のデバッグ情報（本番では出力しない）</li>
 *   <li>{@code INFO} - 業務処理の正常な実行記録（注文受付、ステータス変更等）</li>
 *   <li>{@code WARN} - 想定内だが注意が必要な事象（在庫不足、リトライ等）</li>
 *   <li>{@code ERROR} - システムエラー・予期しない例外</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@SharedUtility(
        description = "アプリケーション共通ロガー（SLF4Jラッパー）",
        targetLayers = {"webapi", "application", "query", "infrastructure"},
        category = "ログ"
)
public class AppLogger {

    private final Logger logger;

    private AppLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 指定されたクラス用のロガーを生成する。
     *
     * @param clazz ログ出力元のクラス
     * @return AppLoggerインスタンス
     */
    public static AppLogger of(Class<?> clazz) {
        return new AppLogger(LoggerFactory.getLogger(clazz));
    }

    /**
     * DEBUGレベルのログを出力する。
     *
     * @param message メッセージ（SLF4Jプレースホルダ対応）
     * @param args    メッセージ引数
     */
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * INFOレベルのログを出力する。
     *
     * <p>業務処理の正常な実行記録に使用する。</p>
     *
     * @param message メッセージ（SLF4Jプレースホルダ対応）
     * @param args    メッセージ引数
     */
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * WARNレベルのログを出力する。
     *
     * <p>想定内だが注意が必要な事象に使用する。</p>
     *
     * @param message メッセージ（SLF4Jプレースホルダ対応）
     * @param args    メッセージ引数
     */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * ERRORレベルのログを出力する。
     *
     * @param message メッセージ
     * @param throwable 例外
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * ERRORレベルのログを出力する。
     *
     * @param message メッセージ（SLF4Jプレースホルダ対応）
     * @param args    メッセージ引数
     */
    public void error(String message, Object... args) {
        logger.error(message, args);
    }
}
