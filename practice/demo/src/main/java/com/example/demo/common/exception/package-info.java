/**
 * 共通例外パッケージ。
 *
 * <p>プロジェクト全体で統一された例外階層を提供する。</p>
 *
 * <h3>例外階層</h3>
 * <pre>
 *   RuntimeException
 *     ├── BusinessException    ... 業務例外（入力不正・ルール違反等）
 *     └── SystemException      ... システム例外（外部連携障害・想定外エラー等）
 * </pre>
 *
 * @see com.example.demo.common.exception.BusinessException
 * @see com.example.demo.common.exception.SystemException
 */
package com.example.demo.common.exception;
