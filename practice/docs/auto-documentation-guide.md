# 自動ドキュメント生成ツール ガイド

> **目的**: Javadoc / OpenAPI / JiG の3つの自動ドキュメントツールについて、  
> 使い方・わかること・レベル感を整理し、レガシー言語からJavaへ移行する現場での活用方針を定める。

---

## 目次

1. [全体サマリー（比較表）](#1-全体サマリー比較表)
2. [Javadoc](#2-javadoc)
3. [OpenAPI（Swagger）](#3-openapiswagger)
4. [JiG](#4-jig)
5. [3ツール組み合わせの推奨運用](#5-3ツール組み合わせの推奨運用)
6. [横断的関心事の管理（ArchUnit + common パッケージ）](#6-横断的関心事の管理archunit--common-パッケージ)
7. [フロントエンド自動ドキュメント（TypeDoc + JSDoc）](#7-フロントエンド自動ドキュメントtypedoc--jsdoc)

---

## 1. 全体サマリー（比較表）

| 観点 | Javadoc | OpenAPI (Swagger) | JiG | TypeDoc |
|------|---------|-------------------|-----|---------|
| **対象** | Javaソースコード全体 | REST APIエンドポイント | ドメインモデル（DDD向け） | TypeScript/TSXソースコード |
| **読み手** | 開発者（バックエンド） | 開発者（フロント/バック両方）、QA | 開発者、ドメインエキスパート、設計レビュー | 開発者（フロントエンド） |
| **出力形式** | HTML（静的サイト） | HTML（Swagger UI）/ JSON / YAML | HTML / SVG図 | HTML（静的サイト） |
| **自動化レベル** | コメント記述が必要 | アノテーション記述が必要 | **コメント不要**（コード構造から自動生成） | JSDocコメント記述が必要（型は自動） |
| **わかること** | クラス/メソッドの仕様・引数・戻り値 | APIのURL・パラメータ・レスポンス・試行 | パッケージ関連図、列挙型一覧、ビジネスルール概要 | 型定義・API関数・コンポーネントの仕様 |
| **実行方法** | `./gradlew javadoc` | アプリ起動 → ブラウザアクセス | `./gradlew jigReports` | `npm run doc` |
| **メンテコスト** | 中（コメント維持が必要） | 中（アノテーション維持が必要） | **低**（コード変更に自動追従） | 低〜中（型は自動、説明文のみ記述） |

---

## 2. Javadoc

### 2.1 概要

Java標準のドキュメント生成ツール。ソースコード内の `/** ... */` コメントからHTML形式のAPI仕様書を生成する。

### 2.2 本プロジェクトでの設定

**build.gradle:**
```groovy
javadoc {
    options.encoding = 'UTF-8'
    options.charSet = 'UTF-8'
    options.locale = 'ja'
    options.addStringOption('Xdoclint:none', '-quiet')
    options.links('https://docs.oracle.com/en/java/javase/17/docs/api/')
    options.links('https://docs.spring.io/spring-framework/docs/current/javadoc-api/')
}
```

### 2.3 使い方

```bash
# ドキュメント生成
./gradlew javadoc

# 出力先
build/docs/javadoc/index.html
```

ブラウザで `build/docs/javadoc/index.html` を開くと閲覧できる。

### 2.4 わかること

| レベル | 内容 | 例 |
|--------|------|-----|
| **パッケージ構成** | パッケージ一覧と各パッケージの概要 | `com.example.demo.domain.model.order` |
| **クラス仕様** | クラスの役割・責務の説明 | `Order` = 注文を表す集約ルートエンティティ |
| **メソッド仕様** | 引数(`@param`)、戻り値(`@return`)、例外(`@throws`) | `placeOrder(PlaceOrderCommand) → OrderId` |
| **フィールド説明** | 定数・フィールドの意味 | `TAX_RATE = 消費税率（10%）` |
| **相互参照** | `@see`によるクラス間の関連リンク | `Order` → `OrderId`, `OrderItem`, `OrderStatus` |
| **バージョン情報** | `@since`, `@author` | `@since 1.0.0`, `@author store-order-system` |

### 2.5 ソースコード記述例

```java
/**
 * 注文を表す集約ルートエンティティ。
 *
 * <p>実店舗におけるテーブル単位の注文を管理する。</p>
 *
 * <h3>集約の不変条件</h3>
 * <ul>
 *   <li>注文には1つ以上の注文明細が必要</li>
 *   <li>ステータス遷移は {@link OrderStatus} の遷移規則に従う</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see OrderId
 * @see OrderItem
 */
public class Order { ... }
```

### 2.6 わかること・わからないこと

**✅ わかる：**
- 個々のクラス・メソッドの仕様（引数・戻り値・例外）
- パッケージ構成と階層
- クラス間のリンク関係（`@see`を書いた場合）
- 継承関係

**❌ わからない：**
- クラス間の依存関係の図（ダイアグラム）
- ビジネスフロー全体の流れ
- REST APIのエンドポイント仕様（URLやHTTPメソッド）
- ドメインモデル間の関連を俯瞰する図

**📝 注意点：**
- **コメントを書かないと何も生成されない**（自動化と言いつつ記述コストがある）
- コメントとコードの乖離が起きやすい（メンテナンスが必要）

---

## 3. OpenAPI（Swagger）

### 3.1 概要

REST APIの仕様をOpenAPI仕様（旧Swagger仕様）に基づいて自動生成する。Swagger UIで対話的にAPIを試行（Try it out）できる。

本プロジェクトでは **springdoc-openapi v2** を使用。

### 3.2 本プロジェクトでの設定

**build.gradle（依存関係）:**
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'
```

**application.properties:**
```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
```

**OpenApiConfig.java（メタ情報定義）:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("店舗ご注文システム API")
                .version("1.0.0")
                .description("実店舗で使用する注文管理システムのREST API仕様書。"))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("ローカル開発環境")
            ));
    }
}
```

### 3.3 使い方

```bash
# Spring Bootアプリケーションを起動
./gradlew bootRun
```

| URL | 内容 |
|-----|------|
| http://localhost:8080/swagger-ui.html | **Swagger UI**（ブラウザで対話的に操作） |
| http://localhost:8080/v3/api-docs | **OpenAPI JSON仕様**（機械読み取り用） |
| http://localhost:8080/v3/api-docs.yaml | **OpenAPI YAML仕様** |

### 3.4 わかること

| レベル | 内容 | 例 |
|--------|------|-----|
| **エンドポイント一覧** | 全REST APIのURL・HTTPメソッド | `POST /api/orders`, `GET /api/menu` |
| **リクエスト仕様** | リクエストボディのスキーマ・バリデーション | `tableNumber: int (min: 1)`, `items: array` |
| **レスポンス仕様** | ステータスコード別のレスポンスボディ | `201: {orderId: string}`, `400: エラー詳細` |
| **パラメータ仕様** | クエリパラメータ・パスパラメータ | `?status=PLACED`, `/{orderId}` |
| **Try it out** | ブラウザ上でAPIを実際に実行 | カール不要で動作確認可能 |
| **スキーマ定義** | DTO/Recordクラスのフィールド構造 | `OrderSummary`のプロパティ一覧 |

### 3.5 アノテーション記述例

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "注文API", description = "注文の作成・取得・ステータス管理を行うAPI")
public class OrderController {

    @PostMapping
    @Operation(summary = "注文を作成する", 
               description = "テーブル番号とメニュー項目を指定して新しい注文を作成する")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "注文作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト不正")
    })
    public ResponseEntity<Map<String, String>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) { ... }
}
```

```java
@Schema(description = "注文作成リクエスト")
public record PlaceOrderRequest(
    @Schema(description = "テーブル番号", example = "5", requiredMode = REQUIRED)
    @Min(value = 1, message = "テーブル番号は1以上である必要があります")
    int tableNumber,

    @Schema(description = "注文明細リスト")
    @NotEmpty(message = "注文明細は1件以上必要です")
    List<OrderItemRequest> items
) { }
```

### 3.6 わかること・わからないこと

**✅ わかる：**
- APIの全エンドポイント一覧（URL + HTTPメソッド）
- リクエスト/レスポンスのJSONスキーマ
- バリデーションルール（`@Min`, `@NotBlank` 等が反映される）
- 実際にAPIを叩いて動作確認可能（Try it out）
- フロントエンド開発者が見るべきAPI契約

**❌ わからない：**
- 内部のビジネスロジック（ドメイン層の処理内容）
- クラスの設計意図やアーキテクチャ構造
- ドメインモデルの関連
- 非APIクラス（サービス層・リポジトリ層）の仕様

**📝 活用ポイント：**
- **フロントエンド開発者との契約書**として機能する
- OpenAPI JSONから**TypeScript型定義を自動生成**できる（`openapi-typescript` 等）
- CI/CDでOpenAPI仕様の差分チェックが可能

---

## 4. JiG

### 4.1 概要

**JiG**（Japan-Inspired-Glossary）は、DDDスタイルのJavaプロジェクトにおいて**ソースコードの構造を解析**し、ドメインモデルに関するドキュメント（図・一覧）を**Javadocコメントなしでも**自動生成するツール。

作者は増田亨さん。DDDの文脈でドメインモデルの「可視化」を目的としている。

GitHub: https://github.com/dddjava/jig

### 4.2 セットアップ方法

**build.gradle に追加:**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.12-SNAPSHOT'
    id 'io.spring.dependency-management' version '1.1.7'
    // ↓ JiGプラグインを追加
    id 'org.dddjava.jig-gradle-plugin' version '2024.7.1'
}

// JiG設定（任意）
jig {
    // 出力ディレクトリ
    outputDirectory = file("${buildDir}/jig")
    // ドメイン層のパッケージパターン
    domainPattern = '.+\\.domain\\..+'
}
```

### 4.3 使い方

```bash
# JiGレポートを生成
./gradlew jigReports

# 出力先
build/jig/
```

`build/jig/` 配下にHTML・SVG形式のドキュメントが出力される。

### 4.4 わかること（JiGが出力するレポート）

JiGは以下のような**ダイアグラムと一覧**を自動生成する：

#### 📊 ダイアグラム系（SVG図）

| レポート名 | 内容 | 本プロジェクトでわかること |
|------------|------|--------------------------|
| **パッケージ関連図** | パッケージ間の依存関係 | `webapi` → `application` → `domain` の依存方向 |
| **ビジネスルール関連図** | ドメインモデル間の関連図 | `Order` → `OrderItem` → `Money`, `Quantity` |
| **カテゴリ図** | 列挙型の区分一覧 | `OrderStatus`（5種）、`MenuCategory`（5種） |
| **サービスメソッド関連図** | Serviceクラスのメソッド呼び出し関係 | `OrderApplicationService` → `OrderRepository`, `MenuItemRepository` |

#### 📋 一覧系（HTML表）

| レポート名 | 内容 | 本プロジェクトでわかること |
|------------|------|--------------------------|
| **ビジネスルール一覧** | ドメイン層のクラス一覧（値オブジェクト/エンティティ/列挙型） | `Money`, `Quantity`, `TableNumber`, `Order`, `MenuItem` 等 |
| **列挙型の区分値一覧** | Enumの全定数とフィールド | `OrderStatus`: PLACED/PREPARING/READY/SERVED/CANCELLED |
| **コレクション一覧** | Listを持つクラス | `Order` → `List<OrderItem>` |
| **文字列比較箇所** | `equals`で文字列比較している箇所 | 意図せぬ文字列比較の検出 |
| **注意メソッド** | 引数が多すぎるメソッド、Boolean返却等 | 設計改善ポイントの自動検出 |

### 4.5 JiGがドメインモデルコードから読み取る情報

JiGはコメントではなく**コード自体の構造**を解析する：

```
■ クラスの種類を自動判別
  - フィールドが1つのクラス → 値オブジェクト（Money, Quantity, TableNumber）
  - enumクラス → 区分オブジェクト（OrderStatus, MenuCategory）
  - 他のエンティティを持つクラス → 集約（Order ← OrderItem）
  
■ 依存関係を自動追跡
  - import文やフィールド型から関連を構築
  - Order → OrderItem → MenuItemId, Money, Quantity

■ 日本語ラベルの取得
  - Javadocの1行目を名前として利用
  - 例: "金額を表す値オブジェクト" → Moneyの表示名になる
```

### 4.6 JiGの最大の特徴

| 特徴 | 説明 |
|------|------|
| **コメント不要** | ソースコードの構造自体からドキュメントを生成（Javadocコメントは補助） |
| **嘘をつかない** | コードの実態がそのまま反映される（コメントの乖離が起きない） |
| **DDD向き** | 値オブジェクト・エンティティ・列挙型を自動分類 |
| **設計の歪み検出** | 「大きすぎるクラス」「引数が多いメソッド」等を自動警告 |

### 4.7 わかること・わからないこと

**✅ わかる：**
- ドメインモデルの全体像（クラス関連図）
- パッケージ間の依存方向（アーキテクチャ違反の検出）
- 列挙型の区分値一覧（ビジネスの用語集として機能）
- 値オブジェクト・エンティティの分類
- 設計上の改善ポイント（コードの匂い検出）

**❌ わからない：**
- REST APIのエンドポイント仕様
- メソッドの引数・戻り値の詳細説明
- ビジネスロジックの具体的なアルゴリズム
- フロントエンドとの連携仕様

---

## 5. 3ツール組み合わせの推奨運用

### 5.1 対象読者別・使うべきツール

| 読者 | 主に見るべきドキュメント | 理由 |
|------|--------------------------|------|
| **フロントエンド開発者** | OpenAPI (Swagger UI) | APIの契約（URL・リクエスト・レスポンス）を確認 |
| **バックエンド開発者（新規参画）** | JiG → Javadoc の順 | まずJiGで全体像を掴み、詳細はJavadocで確認 |
| **設計レビュー** | JiG | パッケージ依存の方向やドメインモデルの構造を確認 |
| **ドメインエキスパート** | JiG（列挙型一覧） | ビジネスの用語・区分が正しく表現されているか確認 |
| **QA／テスト担当** | OpenAPI | APIの仕様をもとにテストケースを設計 |

### 5.2 開発フローへの組み込み

```
コーディング
    ↓
┌──────────────────────────────────────┐
│ CI/CDパイプラインで自動生成            │
│                                      │
│  ./gradlew javadoc      → Javadoc    │
│  ./gradlew jigReports   → JiG        │
│  アプリ起動時に自動      → OpenAPI    │
└──────────────────────────────────────┘
    ↓
成果物を社内Wiki等に自動デプロイ
```

### 5.3 本プロジェクトでの実行コマンドまとめ

```bash
# ① Javadoc生成
./gradlew javadoc
# → build/docs/javadoc/index.html

# ② OpenAPI確認（アプリ起動が前提）
./gradlew bootRun
# → http://localhost:8080/swagger-ui.html    （Swagger UI）
# → http://localhost:8080/v3/api-docs        （JSON仕様）

# ③ JiGレポート生成（プラグイン追加後）
./gradlew jigReports
# → build/jig/

# ④ 全ドキュメント一括生成
./gradlew javadoc jigReports
```

### 5.4 ドキュメントのカバー範囲マッピング

```
┌─────────────────────────────────────────────────────────┐
│                    システム全体                           │
│                                                         │
│  ┌──────────────┐     ┌───────────────┐                 │
│  │ webapi層      │────→│ application層  │    ← Javadoc   │
│  │              │     │               │                 │
│  │ ← OpenAPI    │     │ (Command/Query)│                │
│  │ ← Javadoc    │     │ ← Javadoc     │                │
│  └──────────────┘     └───────┬───────┘                 │
│                               │                         │
│                       ┌───────▼───────┐                 │
│                       │  domain層      │    ← JiG       │
│                       │               │    ← Javadoc   │
│                       │ model / type   │                │
│                       └───────┬───────┘                 │
│                               │                         │
│                       ┌───────▼───────┐                 │
│                       │infrastructure │    ← Javadoc   │
│                       │ 層             │                │
│                       └───────────────┘                 │
└─────────────────────────────────────────────────────────┘
```

### 5.5 結論：自動ドキュメントでカバーできるレベル

| ドキュメント種別 | 自動生成でカバーできるか | 補足 |
|------------------|--------------------------|------|
| API仕様書 | **◎ 完全にカバー** | OpenAPIで自動生成 + Try it out |
| クラス仕様書 | **○ 概ねカバー** | Javadoc記述が前提 |
| ドメインモデル関連図 | **◎ 完全にカバー** | JiGがコードから自動生成 |
| 用語集（区分値一覧） | **◎ 完全にカバー** | JiGのEnum一覧 |
| アーキテクチャ図 | **○ パッケージ関連はカバー** | JiGのパッケージ関連図 |
| 画面仕様書 | **× カバー不可** | 別途作成が必要 |
| 業務フロー図 | **× カバー不可** | 別途作成が必要 |
| テーブル定義書 | **△ 部分的** | JPA使用時はある程度自動化可能 |

---

> **レガシー言語からの移行において、この3ツールを併用することで  
> 「書いたコードがそのままドキュメントになる」状態を高いレベルで実現できます。**

---

## 6. 横断的関心事の管理（ArchUnit + common パッケージ）

### 6.1 課題：自動ドキュメントで「見えない」もの

Javadoc / OpenAPI / JiG の3ツールは優れたドキュメントを自動生成しますが、  
以下のような **横断的関心事（Cross-cutting Concerns）** は発見しにくいという課題があります。

| 横断的関心事 | 問題点 |
|---|---|
| ログ出力（AppLogger） | 共通ユーティリティの存在を知らず、各自が独自実装してしまう |
| 例外ハンドリング | BusinessException / SystemException の使い分けが統一されない |
| バリデーション | 同じチェックロジックが複数箇所に散在する |

**結論**: Javadoc/OpenAPI/JiG の3ツールだけでは不十分。**ArchUnit**（アーキテクチャテスト）を「第4のツール」として導入し、**発見性** と **強制力** を両立させます。

### 6.2 3層アプローチ

```
┌─────────────────────────────────────────────────────────┐
│  第1層: 発見性（Discoverability）                        │
│  → common パッケージ + package-info.java                 │
│  → Javadoc に一覧として表示される                         │
├─────────────────────────────────────────────────────────┤
│  第2層: 強制力（Enforcement）                             │
│  → ArchUnit テストで違反を検出                            │
│  → CI/CD で自動チェック                                  │
├─────────────────────────────────────────────────────────┤
│  第3層: 実装（Implementation）                           │
│  → 既存コードに共通ユーティリティを適用                    │
│  → 使用例がコードベース内に存在する状態を作る              │
└─────────────────────────────────────────────────────────┘
```

### 6.3 第1層: common パッケージと package-info.java

`com.example.demo.common` パッケージに横断的ユーティリティを集約し、  
`package-info.java` で用途・利用ルールを明文化します。

**common/package-info.java の例:**
```java
/**
 * <h2>共通ユーティリティ一覧</h2>
 * <table>
 *   <tr><th>分類</th><th>クラス</th><th>用途</th><th>利用必須レイヤー</th></tr>
 *   <tr><td>ログ</td><td>AppLogger</td><td>業務ログ出力</td><td>application, webapi</td></tr>
 *   <tr><td>例外</td><td>BusinessException</td><td>業務エラー</td><td>全レイヤー</td></tr>
 *   <tr><td>例外</td><td>SystemException</td><td>システムエラー</td><td>全レイヤー</td></tr>
 * </table>
 */
package com.example.demo.common;
```

→ `./gradlew javadoc` を実行すると、**共通ユーティリティの一覧表がHTMLに表示**されます。

### 6.4 第2層: ArchUnit によるアーキテクチャルール

**build.gradle に追加:**
```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```

**ArchitectureRuleTest.java の例:**
```java
@AnalyzeClasses(packages = "com.example.demo")
public class ArchitectureRuleTest {

    @ArchTest
    static final ArchRule SLF4Jの直接利用を禁止 =
            noClasses()
                .that().resideOutsideOfPackage("..common.logging..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.slf4j.LoggerFactory")
                .because("ログ出力は AppLogger を経由してください");

    @ArchTest
    static final ArchRule System_out_errの使用を禁止 =
            noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.io.PrintStream")
                .because("System.out/err ではなく AppLogger を使用してください");
}
```

→ `./gradlew test` を実行すると、ルール違反があれば **テストが失敗** します。  
→ CI/CDに組み込むことで、共通ユーティリティの利用を自動的に強制できます。

### 6.5 各ツールの役割まとめ（横断的関心事の観点）

| 観点 | Javadoc | OpenAPI | JiG | ArchUnit |
|------|---------|---------|-----|----------|
| 共通ユーティリティの一覧 | ○ package-info | × | △ パッケージ関連図 | × |
| 利用ルールの明文化 | ○ コメント内に記述 | × | × | **◎ ルールをコードで定義** |
| 違反の自動検出 | × | × | × | **◎ テスト実行時に検出** |
| CI/CD統合 | ○ 生成のみ | ○ 生成のみ | ○ 生成のみ | **◎ 違反でビルド失敗** |
| レイヤー間依存の制御 | × | × | △ 可視化のみ | **◎ 違反でビルド失敗** |

### 6.6 実行方法

```bash
# ArchUnit テスト（通常のテストに含まれる）
./gradlew test

# テストレポート確認
# build/reports/tests/test/index.html
```

本プロジェクトでは **9件のテスト**（うち7件がArchUnitルール）がすべてパスしています。

---

## 7. フロントエンド自動ドキュメント（TypeDoc + JSDoc）

### 7.1 概要

バックエンドのJavadocに対応する**フロントエンド版のAPIドキュメント生成ツール**。  
TypeScript / React のソースコードに記述した **JSDocコメント**（`/** ... */`）から、  
HTML形式のドキュメントを自動生成する。

| 観点 | TypeDoc |
|------|---------|
| **対象** | TypeScript / TSXソースコード全体 |
| **読み手** | フロントエンド開発者、バックエンド開発者（型定義の確認） |
| **出力形式** | HTML（静的サイト） |
| **自動化レベル** | JSDocコメント記述が必要（型情報はTypeScriptから自動取得） |
| **わかること** | 型定義（interface/type）、API関数の仕様、コンポーネントの役割 |
| **実行方法** | `npm run doc` |
| **メンテコスト** | 低〜中（TypeScriptの型は自動反映、説明文のみ記述） |

### 7.2 本プロジェクトでのセットアップ

**インストール済みパッケージ:**
```bash
npm install --save-dev typedoc
```

**typedoc.json（設定ファイル）:**
```json
{
  "$schema": "https://typedoc.org/schema.json",
  "entryPoints": [
    "src/types/index.ts",
    "src/lib/api.ts",
    "src/lib/theme.ts",
    "src/components/AppLayout.tsx",
    "src/components/ThemeRegistry.tsx",
    "src/app/layout.tsx",
    "src/app/page.tsx",
    "src/app/order/page.tsx",
    "src/app/orders/page.tsx",
    "src/app/menu/page.tsx"
  ],
  "out": "docs/typedoc",
  "name": "店舗ご注文システム フロントエンド API",
  "skipErrorChecking": true
}
```

**package.json（スクリプト）:**
```json
{
  "scripts": {
    "doc": "typedoc",
    "doc:watch": "typedoc --watch"
  }
}
```

### 7.3 使い方

```bash
# ドキュメント生成
cd frontend
npm run doc

# 出力先
frontend/docs/typedoc/index.html

# 開発中のリアルタイム更新（ファイル監視モード）
npm run doc:watch
```

ブラウザで `frontend/docs/typedoc/index.html` を開くと閲覧できる。

### 7.4 わかること

| レベル | 内容 | 本プロジェクトの例 |
|--------|------|-------------------|
| **型定義（interface）** | プロパティ名・型・説明 | `MenuItemView`, `OrderSummary`, `PlaceOrderRequest` |
| **型エイリアス（type）** | ユニオン型の区分値一覧 | `OrderStatus`, `MenuCategory` |
| **定数マップ** | コード→表示名の対応表 | `CATEGORY_LABELS`, `STATUS_LABELS` |
| **API関数** | メソッドの引数・戻り値・使用例 | `menuApi.getAll()`, `orderApi.place()` |
| **コンポーネント** | 役割・Props・使い方 | `AppLayout`, `ThemeRegistry` |
| **ページ** | 各画面の機能・ルート | `OrderPage` (`/order`), `MenuPage` (`/menu`) |
| **相互参照** | `@see` によるリンク | `OrderSummary` → `OrderDetailView`, `OrderStatus` |

### 7.5 JSDocコメントの書き方（TypeScript版）

JavadocとJSDocは同じ `/** ... */` 記法を使う。  
**TypeScriptでは型情報がコードに含まれるため、`@param {string}` のような型注釈は不要**。

#### 基本的なタグ一覧

| タグ | 用途 | Javadocとの対応 |
|------|------|-----------------|
| `@param name - 説明` | 引数の説明 | `@param name 説明` と同じ |
| `@returns 説明` | 戻り値の説明 | `@return` と同じ |
| `@throws {ErrorType} 説明` | 例外の説明 | `@throws` と同じ |
| `@example` | 使用例（コードブロック） | Javadocには無い（独自） |
| `@see {@link ClassName}` | 関連クラスへのリンク | `@see` と同じ |
| `@since 1.0.0` | 追加バージョン | `@since` と同じ |
| `@deprecated 説明` | 非推奨マーク | `@deprecated` と同じ |
| `@module name` | モジュール全体の説明 | `package-info.java` に相当 |
| `@internal` | ドキュメントから除外 | `@hidden` に相当 |
| `@route /path` | ページのルート（カスタム） | N/A |

#### 型定義の例（Javadocのクラス定義に相当）

```typescript
/**
 * メニュー項目ビュー。
 *
 * バックエンド `/api/menu` から返却されるメニュー1件分の表示用データ。
 *
 * @see {@link MenuCategory} カテゴリ区分
 */
export interface MenuItemView {
  /** メニューID（UUID形式） */
  menuItemId: string;
  /** 税込価格（円・小数点以下切り捨て） */
  priceWithTax: number;
  /** 提供可能フラグ */
  available: boolean;
}
```

#### API関数の例（JavadocのServiceクラスに相当）

```typescript
/**
 * 注文API。
 *
 * 注文の作成・取得・ステータス更新・キャンセルに関するAPI呼び出しを提供する。
 *
 * @example
 * ```ts
 * const result = await orderApi.place({
 *   tableNumber: 1,
 *   items: [{ menuItemId: "xxx", quantity: 2 }]
 * });
 * ```
 */
export const orderApi = {
  /**
   * 注文作成。
   * @param request - テーブル番号と注文明細を含むリクエスト
   * @returns 作成された注文のorderId
   */
  place: (request: PlaceOrderRequest) => { ... },
};
```

#### コンポーネントの例（JavadocのUI層に相当）

```tsx
/**
 * アプリケーション共通レイアウトコンポーネント。
 *
 * 上部の `AppBar` と左側の `Drawer`（ナビゲーション）を含むレイアウト。
 * レスポンシブ対応済みで、モバイルではドロワーがトグル表示される。
 *
 * @param props.children - メインコンテンツエリアに表示される子要素
 */
export default function AppLayout({ children }: { children: React.ReactNode }) {
```

### 7.6 Javadocとの比較

| 観点 | Javadoc（Java） | TypeDoc（TypeScript） |
|------|-----------------|----------------------|
| コメント記法 | `/** ... */` | `/** ... */`（同じ） |
| 型情報の記述 | コメント不要（Java型あり） | コメント不要（TS型あり） |
| 実行コマンド | `./gradlew javadoc` | `npm run doc` |
| 出力先 | `build/docs/javadoc/` | `docs/typedoc/` |
| HTML閲覧 | `index.html` | `index.html` |
| パッケージ概要 | `package-info.java` | `@module` タグ |
| 相互参照 | `@see`, `{@link}` | `@see`, `{@link}`（同じ） |
| コード例 | `<pre>{@code ...}</pre>` | `` @example ```ts ... ``` `` |
| 設定ファイル | `build.gradle` の `javadoc` タスク | `typedoc.json` |

### 7.7 わかること・わからないこと

**✅ わかる：**
- 型定義（interface/type）のプロパティ名・型・説明
- API関数の引数・戻り値・使用例
- コンポーネントの役割とProps
- 区分値（OrderStatus, MenuCategory）の一覧と意味
- TypeScriptの型レベルの継承・関連

**❌ わからない：**
- 画面のビジュアルデザイン・レイアウト（→ Storybookが必要）
- ユーザー操作のフロー（画面遷移図など）
- バックエンドAPIとの通信シーケンス
- 状態管理の詳細フロー

**📝 注意点：**
- TypeScriptの型情報は自動的にドキュメントに反映される（Javadocと同じ利点）
- JSDocコメントを書かなくても型情報だけは出力されるが、**説明文を書くことで価値が大きく向上する**
- `@example` タグでコードサンプルを記述すると、使い方が直接わかるため特に有用

### 7.8 開発フローへの組み込み

```bash
# バックエンド + フロントエンド 全ドキュメント一括生成
cd demo && ./gradlew javadoc jigReports && cd ../frontend && npm run doc
```

| ドキュメント | 生成コマンド | 出力先 |
|---|---|---|
| Javadoc（バックエンド） | `./gradlew javadoc` | `demo/build/docs/javadoc/index.html` |
| OpenAPI（バックエンド） | `./gradlew bootRun` | http://localhost:8080/swagger-ui.html |
| JiG（バックエンド） | `./gradlew jigReports` | `demo/build/jig/index.html` |
| **TypeDoc（フロントエンド）** | `npm run doc` | `frontend/docs/typedoc/index.html` |

---

> **バックエンドの Javadoc / OpenAPI / JiG に加えて、フロントエンドの TypeDoc を導入することで、  
> システム全体の「書いたコードがそのままドキュメントになる」状態がフルスタックで実現できます。**
