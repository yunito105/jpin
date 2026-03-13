# アーキテクチャ方針書 — 海外販売システム

> **プロジェクト規模**: 25億円  
> **業態**: 小売業（IKEA/ニトリ/カインズ型）  
> **作成日**: 2026-03-13  
> **ステータス**: ドラフト（テックリードレビュー用）

---

## 1. 開発環境コンテナ分割方針

### 1.1 コンテナ一覧

| コンテナ名 | イメージ | ポート | 役割 |
|---|---|---|---|
| **frontend** | node:20-alpine (Next.js) | 3000 | フロントエンド (SSR/CSR) |
| **backend** | eclipse-temurin:17 (Spring Boot) | 8080 | バックエンドAPI |
| **db** | gvenzl/oracle-free:23-slim | 1521 | Oracle Database |
| **keycloak** | quay.io/keycloak/keycloak:24 | 8180 | 認証・認可 (OIDC/OAuth2) |
| **mock-server** | wiremock/wiremock:3x | 9090 | 外部API モックサーバー |
| **mailhog** | mailhog/mailhog | 1025/8025 | メール送信テスト用 |

### 1.2 分割の判断基準

```
分割する基準:
  ✅ 独立したライフサイクル（起動・停止・再起動が独立）
  ✅ チーム間の並行開発を阻害しない
  ✅ 本番環境のトポロジーに近い構成
  ✅ リソース（CPU/Mem）の独立管理

統合する基準:
  ❌ コンテナ間通信のオーバーヘッドが大きすぎる
  ❌ 起動順序の依存が複雑すぎる
```

### 1.3 コンテナ分割パターン比較

#### パターンA: 最小構成（現状）
```
[backend (ローカル)] → [db (Docker)]
```
- **メリット**: シンプル、リソース消費少
- **デメリット**: 認証テスト不可、外部連携テスト不可、フロント別途立ち上げ必要

#### パターンB: 推奨構成
```
[frontend] → [backend] → [db]
                ├→ [keycloak]
                ├→ [mock-server]
                └→ [mailhog]
```
- **メリット**: 本番に近い構成、チーム並行開発可、E2Eテスト可能
- **デメリット**: メモリ8GB以上推奨、初回構築にやや時間

#### パターンC: フル構成（大規模チーム向け）
```
[frontend] → [api-gateway] → [backend] → [db]
                                ├→ [keycloak]
                                ├→ [mock-server]
                                ├→ [redis (cache)]
                                ├→ [mailhog]
                                └→ [elasticsearch (検索)]
```
- **メリット**: 本番同一構成、パフォーマンステスト可能
- **デメリット**: メモリ16GB以上必要、構築/維持コスト高

### 1.4 推奨: パターンB

25億規模のPJであれば、パターンBを基本とし、開発が進むにつれてパターンCに拡張する段階的アプローチを推奨。

---

## 2. ソフトウェアアーキテクチャ: ヘキサゴナル + 簡易CQRS

### 2.1 なぜヘキサゴナルアーキテクチャか

| 観点 | 従来の3層アーキテクチャ | ヘキサゴナルアーキテクチャ |
|---|---|---|
| **依存方向** | UI → Service → Repository (上→下) | 外側 → ポート → ドメイン (外→内) |
| **テスタビリティ** | △ DBやHTTPに依存 | ◎ ポート経由でモック差替え容易 |
| **外部システム変更** | △ 影響が広範囲 | ◎ アダプタのみ変更 |
| **ドメインロジック保護** | △ サービス層に分散しがち | ◎ ドメイン層に凝集 |
| **学習コスト** | ◎ 低い | △ やや高い |
| **小売業での適合性** | △ 複雑な在庫/価格計算で破綻 | ◎ ドメインモデルで表現力高い |

### 2.2 小売業特有のドメイン分類

```
海外販売システム 主要ドメイン
├── 商品管理 (Product Catalog)
│   ├── 商品マスタ (SKU/JANコード管理)
│   ├── カテゴリ・属性管理
│   └── 多言語・多通貨対応
├── 在庫管理 (Inventory)
│   ├── 倉庫在庫 (Warehouse Stock)
│   ├── 店舗在庫 (Store Stock)
│   └── 在庫引当 (Reservation)
├── 受注管理 (Order)
│   ├── カート → 注文確定
│   ├── 出荷指示
│   └── 返品・交換
├── 価格管理 (Pricing)
│   ├── 通常価格・セール価格
│   ├── 通貨換算
│   └── 税計算 (国別)
├── 顧客管理 (Customer)
│   ├── 会員・ゲスト購入
│   └── 配送先管理
└── 外部連携 (Integration)
    ├── 決済ゲートウェイ
    ├── 物流業者API
    └── 基幹システム (ERP)
```

### 2.3 簡易CQRSの適用方針

#### なぜ「簡易」CQRSか

| 方式 | 説明 | 複雑度 | 適用判断 |
|---|---|---|---|
| **フルCQRS + Event Sourcing** | Command/Query完全分離 + イベントストア | 非常に高 | ❌ 過剰。運用難易度が高すぎる |
| **簡易CQRS（同一DB）** | Command/Queryのモデル分離のみ。DBは共有 | 中 | ✅ **推奨** |
| **CQRS なし** | 単一モデルでCRUD | 低 | ❌ 検索画面と更新画面で最適化が困難 |

#### 簡易CQRSの実装方針

```
Command側（書き込み）:
  - JPA Entity + Hibernate でドメインモデルを永続化
  - ビジネスルールはドメイン層で検証
  - トランザクション管理あり

Query側（読み取り）:
  - Native SQL / JPQL で READ最適化
  - DTOに直接マッピング（ドメインモデルを経由しない）
  - Oracle Materialized View を活用
  - ページング・ソート・フィルタ対応
```

**小売業での具体例:**

| 操作 | 側 | 理由 |
|---|---|---|
| 商品一覧検索 | Query | 複数テーブルJOIN + 検索条件多 → Native SQL最適 |
| 商品詳細表示 | Query | 多言語/多通貨の結合表示 |
| 注文確定 | Command | 在庫引当 + 決済 + ドメインルール検証 |
| 在庫更新 | Command | 排他制御 + ビジネスルール |
| 売上ダッシュボード | Query | 集計クエリ → Materialized View |
| カート操作 | Command | 在庫チェック + 価格計算 |

### 2.4 パッケージ構成

```
com.example.sales/
├── adapter/
│   ├── in/
│   │   ├── web/              # REST Controller
│   │   │   ├── product/      # 商品API
│   │   │   ├── order/        # 注文API
│   │   │   └── inventory/    # 在庫API
│   │   └── batch/            # バッチジョブ
│   └── out/
│       ├── persistence/      # JPA Repository 実装
│       │   ├── entity/       # JPA Entity (DBマッピング用)
│       │   └── mapper/       # Domain ↔ Entity 変換
│       ├── external/         # 外部APIクライアント
│       │   ├── payment/      # 決済連携
│       │   └── logistics/    # 物流連携
│       └── auth/             # Keycloak連携
│
├── application/
│   ├── port/
│   │   ├── in/               # Input Port (UseCase Interface)
│   │   │   ├── command/      # Command系 UseCase
│   │   │   └── query/        # Query系 UseCase
│   │   └── out/              # Output Port (Repository Interface等)
│   ├── command/              # Command UseCase 実装
│   └── query/                # Query UseCase 実装
│
├── domain/
│   ├── model/
│   │   ├── product/          # 商品 Aggregate
│   │   ├── order/            # 注文 Aggregate
│   │   ├── inventory/        # 在庫 Aggregate
│   │   └── shared/           # 共有 Value Object (Money, Quantity等)
│   ├── service/              # Domain Service
│   └── event/                # Domain Event
│
└── config/                   # Spring設定
    ├── SecurityConfig.java
    ├── JpaConfig.java
    └── WebConfig.java
```

### 2.5 依存ルール（絶対遵守）

```
┌─────────────────────────────────────────────┐
│  依存は常に「外 → 内」方向のみ               │
│                                              │
│  adapter → application → domain              │
│                                              │
│  ❌ domain が adapter に依存してはならない    │
│  ❌ application が adapter に依存してはならない │
│  ✅ domain は外部ライブラリに依存しない       │
│     (JPA Entity はadapter層に置く)            │
└─────────────────────────────────────────────┘
```

### 2.6 ArchUnit による依存ルール自動検証

```java
// ArchUnit テスト例
@AnalyzeClasses(packages = "com.example.sales")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapter =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapter =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..");
}
```

---

## 3. 技術スタック詳細

| レイヤ | 技術 | バージョン | 選定理由 |
|---|---|---|---|
| Frontend | Next.js + TypeScript | 14+ | SSR/CSRハイブリッド、SEO対応 |
| Backend | Spring Boot + Java | 3.x / 17 | エンタープライズ実績、人材確保容易 |
| DB | Oracle Database | 23ai | 大規模トランザクション、既存資産活用 |
| 認証 | Keycloak | 24+ | OIDC/OAuth2標準、多言語対応 |
| API仕様管理 | OpenAPI (Swagger) | 3.x | FE/BE/モバイルの契約を1ファイルで管理 |
| API Mock | WireMock | 3.x | 外部API開発前にFE/BE開発着手可能 |
| ビルド | Gradle | 8.x | マルチモジュール対応、ビルドキャッシュ |
| テスト | JUnit5 + ArchUnit | - | 依存ルール自動検証 |

---

## 4. API定義ファースト（OpenAPI）

### 4.1 なぜOpenAPIを使うか

25億円規模でFE・BE・モバイルのチームが分かれる場合、**「どんなAPIを作るか」を全員が共通認識として持つ仕組み**が必要です。

```
 OpenAPI なし（手書きモック）:                OpenAPI あり:

  BEが口頭でAPIを説明                         openapi.yml を git で管理
       ↓                                           ↓
  FEが手書きでWireMockの                       FEはコマンド1つでモック自動生成
  JSONを1つずつ作る                            BEはYAMLからコード雛形を自動生成
       ↓                                      モバイルも同じYAMLを参照
  仕様変更のたびに全員バラバラに修正           仕様変更は openapi.yml の1箇所だけ
```

### 4.2 WireMockのファイル管理（手書き vs 自動生成）

**ファイルの単位はAPIごとではなく「ドメインごと」に整理します。**

```
docker/wiremock/mappings/
  ├── products.json     ← 商品関連のAPI（一覧・詳細・検索）をまとめて定義
  ├── orders.json       ← 注文関連のAPI
  ├── inventory.json    ← 在庫関連のAPI
  ├── payment.json      ← 外部の決済会社APIの代役
  └── logistics.json    ← 外部の物流会社APIの代役
```

OpenAPIを使えば、これらのJSONは**自動生成**できるので手書きは不要になります。

```bash
# OpenAPI仕様書からWireMockのモックを自動生成するコマンド例
npx @stoplight/prism-cli mock openapi.yml --port 9090
# → openapi.yml に書いた全APIが自動的にモックとして使えるようになる
```

### 4.3 OpenAPI定義ファーストの開発フロー

```
① 設計フェーズ（FE・BE・モバイルで合意）
   openapi.yml にAPIの入出力を定義する
   例: GET /api/products → id・name・price・category の配列を返す

② 各チームが並行して開発開始
   FEチーム  → openapi.yml からモックを自動生成 → 画面を作り込む
   BEチーム  → openapi.yml からAPIの雛形コードを自動生成 → ロジックを実装
   モバイル  → 同じ openapi.yml を参照して実装

③ BE実装完了後
   FEの接続先を mock-server から backend に切り替えるだけ
   → openapi.yml で約束した通りのレスポンスが返るので結合がスムーズ
```

### 4.4 openapi.yml のサンプル（商品API）

```yaml
openapi: "3.0.3"
info:
  title: 海外販売システム API
  version: "1.0.0"

paths:
  /api/products:
    get:
      summary: 商品一覧取得
      parameters:
        - name: category
          in: query
          schema: { type: string }
          description: カテゴリで絞り込み（省略可）
      responses:
        '200':
          description: 商品一覧
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Product'

  /api/products/{id}:
    get:
      summary: 商品詳細取得
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: integer }
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Product'
        '404':
          description: 商品が見つからない

components:
  schemas:
    Product:
      type: object
      properties:
        id:       { type: integer,  example: 1 }
        name:     { type: string,   example: "KALLAX Shelf" }
        price:    { type: integer,  example: 7990 }
        category: { type: string,   example: "Furniture" }
```

### 4.5 開発タイムライン比較

#### コードファースト（OpenAPIなし）— FE・BEが順番待ちになる

```
           Week1    Week2    Week3    Week4    Week5    Week6    Week7
           ─────────────────────────────────────────────────────────
BEチーム   ████████████████████████████████
                                            ↓ BEが終わってからFEが開始
FEチーム                                    ████████████████████████
モバイル                                             ████████████████
                                                                    ↓
結合テスト                                                           ██
```

**合計: 7週間**、FEはBEが終わるまで待ちが発生する。

---

#### OpenAPI定義ファースト（推奨）— 全チームが並行で動く

```
           Week1    Week2    Week3    Week4    Week5
           ─────────────────────────────────────────
設計会議   ████
openapi.yml作成   ████
                     ↓ 全員スタート
BEチーム             ████████████████████
FEチーム             ████████████████████  ← モックで先行開発
モバイル                 ████████████████
                                         ↓ 接続先を切り替えるだけ
結合テスト                               ██
```

**合計: 5週間**、設計2週間の投資で開発期間を短縮できる。

---

#### 各フェーズで何をするか

| フェーズ | 誰が | 何をするか |
|---|---|---|
| 設計（Week1〜2） | テックリード + FE/BEリード | `openapi.yml` に全APIの入出力を定義・レビュー |
| FE開発（Week3〜4） | FEチーム | `openapi.yml` からモックを自動生成して画面を作り込む |
| BE開発（Week3〜4） | BEチーム | `openapi.yml` からコード雛形を生成しビジネスロジックを実装 |
| 結合（Week5） | 全員 | FEの接続先を `mock-server → backend` に切り替えてテスト |

### 4.6 メリット・デメリット

| メリット | デメリット |
|---|---|
| API仕様の認識ズレをゼロにできる | openapi.ymlの書き方を覚える必要がある |
| FE・BE・モバイルが並行開発できる | 設計フェーズで仕様を決める議論コストがかかる |
| モックの手書きが不要 | 仕様変更時にyamlと実装の両方を更新する規律が必要 |
| Swagger UIで自動的にAPI仕様書ができあがる | - |

---

## 5. メリット・デメリットまとめ

### ヘキサゴナルアーキテクチャ

| メリット | デメリット |
|---|---|
| ドメインロジックが外部技術から独立 | 初期のファイル数・パッケージ数が多い |
| テストが高速（DB不要でユニットテスト可能） | 学習コストがやや高い |
| Oracle→別DB、REST→gRPC等の技術変更が容易 | マッピング層のコードが増える |
| チーム分担しやすい（adapter/domain/application） | 小規模CRUDには過剰に感じる場合あり |
| ArchUnitで依存ルール自動検証可能 | - |

### 簡易CQRS

| メリット | デメリット |
|---|---|
| 読み取り性能を独立最適化できる | Command/Queryの境界判断が必要 |
| 複雑な検索画面とドメインモデルを分離 | 同一DBなので結果整合性の問題は少ないが、モデルの二重管理 |
| Oracle Materialized View との親和性高 | Query側のSQLが手書きになりがち |
| 画面最適化DTOで表示パフォーマンス向上 | - |

### コンテナ分割（パターンB）

| メリット | デメリット |
|---|---|
| `docker compose up` のみで環境構築完了 | 開発マシンに8GB以上のメモリが必要 |
| 新メンバーのオンボーディングが極めて高速 | Oracle Free版のイメージサイズが大きい (~1.5GB) |
| CI/CDパイプラインと同じ構成でローカルテスト可 | 初回のコンテナビルドに時間がかかる |
| 外部依存をmockで完全制御 | - |

---

## 6. リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| ヘキサゴナルの学習コスト | 開発初期の生産性低下 | サンプル実装 + ペアプロ + ArchUnitで自動検証 |
| Oracle Free版と本番Oracleの差異 | データ型や関数の違いで障害 | CI環境ではOracle本番同等版を使用 |
| CQRSの過度な適用 | 単純CRUDまで分離して工数増 | 判断基準を明文化（検索条件3つ以上ならQuery側で最適化等） |
| コンテナ構成の肥大化 | 開発マシンの負荷増大 | プロファイル分割（core/full）で選択的起動 |
| Keycloak設定の複雑さ | 認証周りの開発停滞 | Realm設定をJSON export/importで自動化 |

---

## 7. 次のアクション

- [ ] テックリードレビュー
- [ ] OpenAPI定義ファーストの採用可否を決定
- [ ] openapi.yml の初版作成（商品・注文・在庫API）
- [ ] サンプル実装（商品管理ドメインで1機能をE2E実装）
- [ ] ArchUnit テスト導入
- [ ] docker-compose.yml 全体構成の確定
- [ ] CI/CDパイプライン設計
- [ ] フロントエンド ディレクトリ構成の策定
