/**
 * 共通基盤パッケージ。
 *
 * <p>本パッケージは、ドメインに依存しない横断的な共通機能を提供する。
 * <strong>全レイヤーで利用すべき基盤機能</strong>がここに集約されている。</p>
 *
 * <h2>提供する共通機能一覧</h2>
 * <table>
 *   <tr><th>クラス</th><th>役割</th><th>利用必須レイヤー</th></tr>
 *   <tr>
 *     <td>{@link com.example.demo.common.logging.AppLogger}</td>
 *     <td>アプリケーションログ出力</td>
 *     <td>全レイヤー（webapi / application / query / infrastructure）</td>
 *   </tr>
 *   <tr>
 *     <td>{@link com.example.demo.common.exception.BusinessException}</td>
 *     <td>業務例外の基底クラス</td>
 *     <td>domain / application</td>
 *   </tr>
 *   <tr>
 *     <td>{@link com.example.demo.common.exception.SystemException}</td>
 *     <td>システム例外の基底クラス</td>
 *     <td>infrastructure</td>
 *   </tr>
 *   <tr>
 *     <td>{@link com.example.demo.common.date.DateUtils}</td>
 *     <td>日付・時刻操作の汎用関数群</td>
 *     <td>全レイヤー</td>
 *   </tr>
 * </table>
 *
 * <h2>新規参画者向け</h2>
 * <p>本プロジェクトでは、ログ出力に {@code System.out.println} や
 * 素の {@code LoggerFactory.getLogger} を直接使用せず、
 * 必ず {@link com.example.demo.common.logging.AppLogger} を使用すること。
 * これは ArchUnit テストで強制されている。</p>
 *
 * <h2>共通ユーティリティの検索方法</h2>
 * <p>全ての共通ユーティリティには {@link com.example.demo.common.SharedUtility @SharedUtility}
 * アノテーションが付与されている。
 * IDEで「{@code @SharedUtility}」を検索すれば、利用可能な共通機能を一覧できる。</p>
 *
 * @see com.example.demo.common.logging
 * @see com.example.demo.common.exception
 * @see com.example.demo.common.date
 */
package com.example.demo.common;
