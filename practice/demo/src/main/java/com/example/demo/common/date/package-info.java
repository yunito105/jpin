/**
 * 日付操作ユーティリティパッケージ。
 *
 * <p>本パッケージは、プロジェクト全体で統一的に使用する日付・時刻操作機能を提供する。
 * Java標準の {@link java.time} APIをラップし、業務で頻出する日付操作を簡素化する。</p>
 *
 * <h3>提供クラス</h3>
 * <ul>
 *   <li>{@link com.example.demo.common.date.DateUtils} - 日付操作の汎用関数群</li>
 * </ul>
 *
 * <h3>使用ルール</h3>
 * <ul>
 *   <li>日付操作は必ずこのパッケージのユーティリティを使用すること</li>
 *   <li>{@code new Date()} や {@code SimpleDateFormat} は使用禁止</li>
 *   <li>タイムゾーンは {@code Asia/Tokyo} が標準</li>
 * </ul>
 *
 * @see com.example.demo.common.date.DateUtils
 */
package com.example.demo.common.date;
