package com.example.demo.common;

import java.lang.annotation.*;

/**
 * 共通ユーティリティであることを明示するマーカーアノテーション。
 *
 * <p>このアノテーションが付与されたクラスは、プロジェクト全体で再利用可能な
 * 共通基盤機能を提供する。新規参画者は {@code @SharedUtility} で検索することで
 * 利用可能な共通ユーティリティを一覧できる。</p>
 *
 * <h3>検索方法</h3>
 * <ul>
 *   <li>IntelliJ: {@code Ctrl+N} → "SharedUtility" で検索 → 利用箇所を辿る</li>
 *   <li>IntelliJ: {@code Ctrl+Shift+F} → "@SharedUtility" でプロジェクト全体検索</li>
 *   <li>VS Code: {@code Ctrl+Shift+F} → "@SharedUtility" で検索</li>
 *   <li>grep: {@code grep -r "@SharedUtility" src/}</li>
 * </ul>
 *
 * <h3>使用ルール</h3>
 * <ul>
 *   <li>このアノテーションは {@code common} パッケージ配下のクラスにのみ付与すること</li>
 *   <li>ArchUnit テストでこのルールが強制されている</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SharedUtility {

    /**
     * このユーティリティの説明。
     *
     * @return 説明文
     */
    String description();

    /**
     * 利用を推奨するレイヤー。
     * デフォルトは全レイヤー。
     *
     * @return 対象レイヤー名の配列
     */
    String[] targetLayers() default {"all"};

    /**
     * このユーティリティの分類カテゴリ。
     *
     * @return カテゴリ名（例: "ログ", "例外", "日付", "文字列"）
     */
    String category() default "";
}
