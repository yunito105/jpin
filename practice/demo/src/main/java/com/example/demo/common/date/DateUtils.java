package com.example.demo.common.date;

import com.example.demo.common.SharedUtility;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日付操作ユーティリティ。
 *
 * <p>プロジェクト全体で統一的に使用する日付・時刻の汎用関数を提供する。
 * すべてのメソッドは {@link java.time} APIベースで、スレッドセーフである。</p>
 *
 * <h3>使用方法</h3>
 * <pre>{@code
 * // 今日の日付を取得
 * LocalDate today = DateUtils.today();
 *
 * // 現在日時を取得
 * LocalDateTime now = DateUtils.now();
 *
 * // 日付フォーマット
 * String formatted = DateUtils.formatDate(today);         // "2026/02/28"
 * String withTime  = DateUtils.formatDateTime(now);       // "2026/02/28 14:30:00"
 *
 * // 営業日判定
 * boolean open = DateUtils.isBusinessDay(today);          // 平日ならtrue
 *
 * // 日付の差分
 * long days = DateUtils.daysBetween(startDate, endDate);  // 2つの日付間の日数
 *
 * // 期間判定
 * boolean within = DateUtils.isWithinDays(targetDate, 7); // 7日以内か判定
 * }</pre>
 *
 * <h3>設計方針</h3>
 * <ul>
 *   <li>すべて static メソッドとして提供（インスタンス化不要）</li>
 *   <li>タイムゾーンは {@code Asia/Tokyo} を標準とする</li>
 *   <li>null引数に対しては {@link IllegalArgumentException} をスローする</li>
 *   <li>旧来の {@code java.util.Date} や {@code SimpleDateFormat} は使用しない</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see java.time.LocalDate
 * @see java.time.LocalDateTime
 */
@SharedUtility(
        description = "日付・時刻操作の汎用関数群",
        targetLayers = {"all"},
        category = "日付"
)
public final class DateUtils {

    /** 標準タイムゾーン（Asia/Tokyo） */
    private static final ZoneId ZONE_TOKYO = ZoneId.of("Asia/Tokyo");

    /** 日付フォーマット（yyyy/MM/dd） */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 日時フォーマット（yyyy/MM/dd HH:mm:ss） */
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    /** ISO日付フォーマット（yyyy-MM-dd） */
    private static final DateTimeFormatter ISO_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {
        // ユーティリティクラスのためインスタンス化を禁止
    }

    // ========================================
    // 現在日時の取得
    // ========================================

    /**
     * 本日の日付を取得する。
     *
     * @return 本日の日付（Asia/Tokyo基準）
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE_TOKYO);
    }

    /**
     * 現在日時を取得する。
     *
     * @return 現在日時（Asia/Tokyo基準）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_TOKYO);
    }

    // ========================================
    // フォーマット
    // ========================================

    /**
     * 日付を標準フォーマット（yyyy/MM/dd）に変換する。
     *
     * <p>使用例:</p>
     * <pre>{@code
     * String s = DateUtils.formatDate(LocalDate.of(2026, 2, 28));
     * // → "2026/02/28"
     * }</pre>
     *
     * @param date 日付（nullの場合は例外）
     * @return フォーマットされた日付文字列
     * @throws IllegalArgumentException dateがnullの場合
     */
    public static String formatDate(LocalDate date) {
        requireNonNull(date, "date");
        return date.format(DATE_FORMAT);
    }

    /**
     * 日時を標準フォーマット（yyyy/MM/dd HH:mm:ss）に変換する。
     *
     * @param dateTime 日時（nullの場合は例外）
     * @return フォーマットされた日時文字列
     * @throws IllegalArgumentException dateTimeがnullの場合
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        requireNonNull(dateTime, "dateTime");
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * 日付をISO形式（yyyy-MM-dd）に変換する。
     *
     * <p>APIレスポンスやログ出力で使用する。</p>
     *
     * @param date 日付（nullの場合は例外）
     * @return ISOフォーマットの日付文字列
     * @throws IllegalArgumentException dateがnullの場合
     */
    public static String formatIsoDate(LocalDate date) {
        requireNonNull(date, "date");
        return date.format(ISO_DATE_FORMAT);
    }

    // ========================================
    // パース
    // ========================================

    /**
     * 文字列（yyyy/MM/dd）を日付に変換する。
     *
     * @param dateStr 日付文字列（例: "2026/02/28"）
     * @return パースされた日付
     * @throws IllegalArgumentException dateStrがnullの場合
     * @throws java.time.format.DateTimeParseException フォーマットが不正な場合
     */
    public static LocalDate parseDate(String dateStr) {
        requireNonNull(dateStr, "dateStr");
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }

    /**
     * 文字列（yyyy/MM/dd HH:mm:ss）を日時に変換する。
     *
     * @param dateTimeStr 日時文字列（例: "2026/02/28 14:30:00"）
     * @return パースされた日時
     * @throws IllegalArgumentException dateTimeStrがnullの場合
     * @throws java.time.format.DateTimeParseException フォーマットが不正な場合
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        requireNonNull(dateTimeStr, "dateTimeStr");
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMAT);
    }

    // ========================================
    // 日付計算
    // ========================================

    /**
     * 2つの日付間の日数を計算する。
     *
     * <p>使用例:</p>
     * <pre>{@code
     * long days = DateUtils.daysBetween(
     *     LocalDate.of(2026, 2, 1),
     *     LocalDate.of(2026, 2, 28)
     * );
     * // → 27
     * }</pre>
     *
     * @param from 開始日（nullの場合は例外）
     * @param to   終了日（nullの場合は例外）
     * @return 日数差（from &lt; to の場合は正、from &gt; to の場合は負）
     * @throws IllegalArgumentException from/toがnullの場合
     */
    public static long daysBetween(LocalDate from, LocalDate to) {
        requireNonNull(from, "from");
        requireNonNull(to, "to");
        return ChronoUnit.DAYS.between(from, to);
    }

    /**
     * 指定日が今日から指定日数以内かどうかを判定する。
     *
     * <p>使用例: 注文の有効期限チェック</p>
     * <pre>{@code
     * boolean valid = DateUtils.isWithinDays(order.getOrderDate(), 30);
     * }</pre>
     *
     * @param targetDate 対象日付（nullの場合は例外）
     * @param days       日数（正の値）
     * @return 今日を基準に±days日以内の場合true
     * @throws IllegalArgumentException targetDateがnull、またはdaysが負の場合
     */
    public static boolean isWithinDays(LocalDate targetDate, int days) {
        requireNonNull(targetDate, "targetDate");
        if (days < 0) {
            throw new IllegalArgumentException("daysは正の値を指定してください: " + days);
        }
        long diff = Math.abs(ChronoUnit.DAYS.between(today(), targetDate));
        return diff <= days;
    }

    // ========================================
    // 営業日判定
    // ========================================

    /**
     * 指定日が営業日（平日）かどうかを判定する。
     *
     * <p>土曜・日曜を非営業日とする簡易判定。
     * 祝日判定は現在の実装には含まれない。</p>
     *
     * @param date 判定対象の日付（nullの場合は例外）
     * @return 平日の場合true、土日の場合false
     * @throws IllegalArgumentException dateがnullの場合
     */
    public static boolean isBusinessDay(LocalDate date) {
        requireNonNull(date, "date");
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /**
     * 指定日から次の営業日を取得する。
     *
     * <p>指定日が平日であればそのまま返し、
     * 土日であれば翌週月曜日を返す。</p>
     *
     * @param date 基準日（nullの場合は例外）
     * @return 次の営業日
     * @throws IllegalArgumentException dateがnullの場合
     */
    public static LocalDate nextBusinessDay(LocalDate date) {
        requireNonNull(date, "date");
        LocalDate next = date;
        while (!isBusinessDay(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    // ========================================
    // 期間判定
    // ========================================

    /**
     * 指定の日時が今日の営業時間内（開店〜閉店）かどうかを判定する。
     *
     * <p>店舗の営業時間（デフォルト: 10:00〜22:00）内かを判定する。</p>
     *
     * @param dateTime  判定対象の日時（nullの場合は例外）
     * @param openHour  開店時刻（0-23）
     * @param closeHour 閉店時刻（0-23、openHourより大きいこと）
     * @return 営業時間内の場合true
     * @throws IllegalArgumentException dateTimeがnull、または時刻が不正な場合
     */
    public static boolean isWithinBusinessHours(LocalDateTime dateTime, int openHour, int closeHour) {
        requireNonNull(dateTime, "dateTime");
        if (openHour < 0 || openHour > 23 || closeHour < 0 || closeHour > 23) {
            throw new IllegalArgumentException("時刻は0〜23の範囲で指定してください");
        }
        int hour = dateTime.getHour();
        return hour >= openHour && hour < closeHour;
    }

    /**
     * 指定の日付が今月かどうかを判定する。
     *
     * @param date 判定対象の日付（nullの場合は例外）
     * @return 今月の場合true
     * @throws IllegalArgumentException dateがnullの場合
     */
    public static boolean isCurrentMonth(LocalDate date) {
        requireNonNull(date, "date");
        LocalDate now = today();
        return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
    }

    // ========================================
    // 内部ユーティリティ
    // ========================================

    private static void requireNonNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + "はnullにできません");
        }
    }
}
