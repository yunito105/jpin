# feature/ 機能モジュール構成 改訂版

> **目的**: 元提案のフラット構成から **ドメイン → サブドメイン** の階層構成に改訂する。
>
> **変更の動機**: 「sales → order, working-plan, stock のようにしたい」という要望に対応。

---

## 目次

1. [元提案の問題点](#1-元提案の問題点)
2. [改訂方針](#2-改訂方針)
3. [改訂後のディレクトリ構成図](#3-改訂後のディレクトリ構成図)
4. [settings.gradle.kts 改訂版](#4-settingsgradlekts-改訂版)
5. [ドメイン/サブドメイン定義一覧](#5-ドメインサブドメイン定義一覧)
6. [shared モジュールの詳細設計](#6-shared-モジュールの詳細設計)
7. [モジュール依存関係（改訂版）](#7-モジュール依存関係改訂版)
8. [Gradle Convention Plugin の追加](#8-gradle-convention-plugin-の追加)
9. [ドメイン追加時の手順](#9-ドメイン追加時の手順)
10. [よくある質問](#10-よくある質問)

---

## 1. 元提案の問題点

元の提案では feature 直下にフラットに機能を配置していた:

```
feature/
├── product-catalog/     # 商品カタログ
├── product-detail/      # 商品詳細
├── order-list/          # 受注一覧
├── order-detail/        # 受注詳細
├── order-create/        # 受注作成
├── inventory-list/      # 在庫一覧
├── inventory-detail/    # 在庫詳細
├── shipment-list/       # 出荷一覧
├── shipment-detail/     # 出荷詳細
├── shipment-tracking/   # 出荷追跡
├── store-list/          # 店舗一覧
├── store-detail/        # 店舗詳細
└── ...
```

### 問題

| # | 問題 | 説明 |
|---|------|------|
| 1 | **ドメイン境界が不明瞭** | フォルダ名だけでどのドメインに属するか判別しにくい |
| 2 | **サブドメイン間の共有が困難** | 受注一覧と受注作成が共通モデル (`Order`) を使いたい場合、core/model に置く以外に方法がない |
| 3 | **バックエンドのDDDと不整合** | バックエンドは `sales`, `logistics`, `master` 等のドメイン単位で管理している |
| 4 | **スケーラビリティ** | 画面が増えるとフラット構造が破綻する（50+モジュールが並ぶ） |

---

## 2. 改訂方針

### 階層構造

```
feature/
├── {domain}/           # ドメイン（ビジネス領域）
│   ├── shared/         # ドメイン内共有（モデル、API、Repository）
│   ├── {subdomain}/    # サブドメイン（画面単位の機能）
│   │   ├── api/        # ナビゲーションキーのみ（外部公開用）
│   │   └── impl/       # 画面実装（Screen, ViewModel, UiState）
│   └── ...
└── ...
```

### 3段構成のルール

| レベル | 例 | 含むもの |
|-------|-----|---------|
| **ドメイン** | `sales/`, `logistics/`, `master/` | ビジネス領域の括り |
| **shared** | `sales/shared/` | ドメイン内共有モデル・API・Repository |
| **サブドメイン** | `sales/order-list/`, `sales/order-create/` | 画面単位の機能実装 |

---

## 3. 改訂後のディレクトリ構成図

```
feature/
│
│  ┌─────────────────────────────────────────────────────────┐
│  │ ドメイン: 販売管理 (Sales)                                │
│  │ バックエンド: sales-service                               │
│  └─────────────────────────────────────────────────────────┘
├── sales/
│   ├── shared/                          # ── 販売ドメイン共有 ──
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../feature/sales/shared/
│   │       ├── model/                   # ドメイン共有モデル
│   │       │   ├── Order.kt                 # 受注モデル
│   │       │   ├── OrderLine.kt             # 受注明細
│   │       │   ├── OrderStatus.kt           # 受注ステータス enum
│   │       │   ├── WorkingPlan.kt           # 作業計画モデル
│   │       │   └── SalesStock.kt            # 販売在庫（引当在庫）
│   │       ├── api/                     # Retrofit API インターフェース
│   │       │   └── OrderApi.kt              # @GET, @POST 等
│   │       ├── repository/              # Repository
│   │       │   ├── OrderRepository.kt       # インターフェース
│   │       │   └── DefaultOrderRepository.kt
│   │       └── mapper/                  # DTO → Model 変換
│   │           └── OrderMapper.kt
│   │
│   ├── order-list/                      # ── 受注一覧 ──
│   │   ├── api/
│   │   │   ├── build.gradle.kts
│   │   │   └── src/main/kotlin/.../
│   │   │       └── OrderListNavKey.kt       # ナビゲーションキー
│   │   └── impl/
│   │       ├── build.gradle.kts
│   │       └── src/main/kotlin/.../
│   │           ├── OrderListScreen.kt       # 受注一覧画面
│   │           ├── OrderListViewModel.kt    # 状態管理
│   │           ├── model/
│   │           │   └── OrderListUiState.kt  # 画面固有UIステート
│   │           └── navigation/
│   │               └── OrderListNavigation.kt
│   │
│   ├── order-detail/                    # ── 受注詳細 ──
│   │   ├── api/
│   │   │   └── ...OrderDetailNavKey.kt
│   │   └── impl/
│   │       └── ...OrderDetailScreen.kt, ...ViewModel.kt
│   │
│   ├── order-create/                    # ── 受注作成 ──
│   │   ├── api/
│   │   └── impl/
│   │       └── src/main/kotlin/.../
│   │           ├── OrderCreateScreen.kt
│   │           ├── OrderCreateViewModel.kt
│   │           ├── model/
│   │           │   ├── OrderCreateUiState.kt
│   │           │   └── OrderFormValidation.kt    # 表示ルールバリデーション
│   │           └── navigation/
│   │
│   ├── working-plan/                    # ── 作業計画 ──
│   │   ├── api/
│   │   └── impl/
│   │       └── ...WorkingPlanScreen.kt, ...ViewModel.kt
│   │
│   └── stock/                           # ── 販売在庫照会 ──
│       ├── api/
│       └── impl/
│           └── ...SalesStockScreen.kt, ...ViewModel.kt
│
│  ┌─────────────────────────────────────────────────────────┐
│  │ ドメイン: 物流管理 (Logistics)                            │
│  │ バックエンド: logistics-service                           │
│  └─────────────────────────────────────────────────────────┘
├── logistics/
│   ├── shared/
│   │   └── src/main/kotlin/.../feature/logistics/shared/
│   │       ├── model/
│   │       │   ├── Shipment.kt              # 出荷モデル
│   │       │   ├── ShipmentStatus.kt        # 出荷ステータス enum
│   │       │   ├── DeliveryRoute.kt         # 配送ルート
│   │       │   └── Warehouse.kt             # 倉庫
│   │       ├── api/
│   │       │   └── ShipmentApi.kt
│   │       ├── repository/
│   │       │   ├── ShipmentRepository.kt
│   │       │   └── DefaultShipmentRepository.kt
│   │       └── mapper/
│   │           └── ShipmentMapper.kt
│   │
│   ├── shipment-list/
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── shipment-detail/
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── shipment-tracking/               # 出荷追跡（地図表示等）
│   │   ├── api/
│   │   └── impl/
│   │
│   └── inventory-transfer/              # 倉庫間在庫移動
│       ├── api/
│       └── impl/
│
│  ┌─────────────────────────────────────────────────────────┐
│  │ ドメイン: マスタ管理 (Master)                             │
│  │ バックエンド: master-service                              │
│  └─────────────────────────────────────────────────────────┘
├── master/
│   ├── shared/
│   │   └── src/main/kotlin/.../feature/master/shared/
│   │       ├── model/
│   │       │   ├── Product.kt               # 商品マスタ
│   │       │   ├── Category.kt              # カテゴリ
│   │       │   ├── Store.kt                 # 店舗マスタ
│   │       │   └── Supplier.kt              # 仕入先
│   │       ├── api/
│   │       │   ├── ProductApi.kt
│   │       │   └── StoreApi.kt
│   │       ├── repository/
│   │       │   ├── ProductRepository.kt
│   │       │   ├── DefaultProductRepository.kt
│   │       │   ├── StoreRepository.kt
│   │       │   └── DefaultStoreRepository.kt
│   │       └── mapper/
│   │           ├── ProductMapper.kt
│   │           └── StoreMapper.kt
│   │
│   ├── product-catalog/                 # 商品カタログ（一覧/検索）
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── product-detail/                  # 商品詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── store-list/                      # 店舗一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   └── store-detail/                    # 店舗詳細
│       ├── api/
│       └── impl/
│
│  ┌─────────────────────────────────────────────────────────┐
│  │ ドメイン: 在庫管理 (Inventory)                            │
│  │ バックエンド: inventory-service                           │
│  └─────────────────────────────────────────────────────────┘
├── inventory/
│   ├── shared/
│   │   └── src/main/kotlin/.../feature/inventory/shared/
│   │       ├── model/
│   │       │   ├── Inventory.kt             # 在庫モデル
│   │       │   ├── InventoryAdjustment.kt   # 在庫調整
│   │       │   └── StockLevel.kt            # 在庫水準
│   │       ├── api/
│   │       │   └── InventoryApi.kt
│   │       ├── repository/
│   │       │   ├── InventoryRepository.kt
│   │       │   └── DefaultInventoryRepository.kt
│   │       └── mapper/
│   │           └── InventoryMapper.kt
│   │
│   ├── inventory-list/                  # 在庫一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── inventory-detail/                # 在庫詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   └── inventory-adjustment/            # 在庫調整入力
│       ├── api/
│       └── impl/
│
│  ┌─────────────────────────────────────────────────────────┐
│  │ 共通機能（ドメイン横断）                                    │
│  └─────────────────────────────────────────────────────────┘
├── auth/                                # 認証
│   └── impl/
│       └── src/main/kotlin/.../
│           ├── LoginScreen.kt
│           ├── LoginViewModel.kt
│           └── navigation/
│
├── dashboard/                           # ダッシュボード（ホーム画面）
│   ├── api/
│   └── impl/
│
├── search/                              # 横断検索
│   ├── api/
│   └── impl/
│
├── settings/                            # アプリ設定
│   └── impl/
│
└── notifications/                       # 通知一覧
    ├── api/
    └── impl/
```

---

## 4. settings.gradle.kts 改訂版

```kotlin
// settings.gradle.kts
include(":app")
include(":app-catalog")
include(":lint")
include(":ui-test-hilt-manifest")

// ─── Core modules ───
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:analytics")
include(":core:testing")
include(":core:screenshot-testing")

// ─── Feature: Sales（販売管理）───
include(":feature:sales:shared")
include(":feature:sales:order-list:api")
include(":feature:sales:order-list:impl")
include(":feature:sales:order-detail:api")
include(":feature:sales:order-detail:impl")
include(":feature:sales:order-create:api")
include(":feature:sales:order-create:impl")
include(":feature:sales:working-plan:api")
include(":feature:sales:working-plan:impl")
include(":feature:sales:stock:api")
include(":feature:sales:stock:impl")

// ─── Feature: Logistics（物流管理）───
include(":feature:logistics:shared")
include(":feature:logistics:shipment-list:api")
include(":feature:logistics:shipment-list:impl")
include(":feature:logistics:shipment-detail:api")
include(":feature:logistics:shipment-detail:impl")
include(":feature:logistics:shipment-tracking:api")
include(":feature:logistics:shipment-tracking:impl")
include(":feature:logistics:inventory-transfer:api")
include(":feature:logistics:inventory-transfer:impl")

// ─── Feature: Master（マスタ管理）───
include(":feature:master:shared")
include(":feature:master:product-catalog:api")
include(":feature:master:product-catalog:impl")
include(":feature:master:product-detail:api")
include(":feature:master:product-detail:impl")
include(":feature:master:store-list:api")
include(":feature:master:store-list:impl")
include(":feature:master:store-detail:api")
include(":feature:master:store-detail:impl")

// ─── Feature: Inventory（在庫管理）───
include(":feature:inventory:shared")
include(":feature:inventory:inventory-list:api")
include(":feature:inventory:inventory-list:impl")
include(":feature:inventory:inventory-detail:api")
include(":feature:inventory:inventory-detail:impl")
include(":feature:inventory:inventory-adjustment:api")
include(":feature:inventory:inventory-adjustment:impl")

// ─── Feature: Common（共通機能）───
include(":feature:auth:impl")
include(":feature:dashboard:api")
include(":feature:dashboard:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:settings:impl")
include(":feature:notifications:api")
include(":feature:notifications:impl")
```

---

## 5. ドメイン/サブドメイン定義一覧

| ドメイン | Gradle パス | サブドメイン | 画面数 | バックエンド対応 |
|---------|------------|------------|-------|-----------------|
| **Sales** (販売) | `:feature:sales:*` | order-list, order-detail, order-create, working-plan, stock | 5 | sales-service |
| **Logistics** (物流) | `:feature:logistics:*` | shipment-list, shipment-detail, shipment-tracking, inventory-transfer | 4 | logistics-service |
| **Master** (マスタ) | `:feature:master:*` | product-catalog, product-detail, store-list, store-detail | 4 | master-service |
| **Inventory** (在庫) | `:feature:inventory:*` | inventory-list, inventory-detail, inventory-adjustment | 3 | inventory-service |
| **共通** | `:feature:*` | auth, dashboard, search, settings, notifications | 5 | 各種 |

**合計**: 4 ドメイン + 共通 = **21 サブドメイン（画面）**

> ドメイン分割はバックエンドの Bounded Context に対応させる。  
> バックエンド側のサービス分割が変わった場合は、モバイル側のドメインも追随する。

---

## 6. shared モジュールの詳細設計

### 役割

**同じドメイン内の複数サブドメイン（画面）が共有する**コードを置く場所。

### shared に置くもの / 置かないもの

| 置くもの | 例 | 理由 |
|---------|-----|------|
| ドメインモデル | `Order.kt`, `OrderStatus.kt` | 受注一覧・詳細・作成が全て使う |
| Retrofit API IF | `OrderApi.kt` | 受注一覧・作成が API を呼ぶ |
| Repository IF + 実装 | `OrderRepository.kt` | 複数画面がデータ取得に使う |
| DTO → Model マッパー | `OrderMapper.kt` | API レスポンスの変換は共通 |

| 置かないもの | 例 | 理由 |
|-------------|-----|------|
| 画面固有 UiState | `OrderCreateUiState.kt` | 受注作成画面でしか使わない |
| Screen Composable | `OrderListScreen.kt` | 各画面固有 |
| ViewModel | `OrderListViewModel.kt` | 各画面固有 |

### build.gradle.kts の例

```kotlin
// feature/sales/shared/build.gradle.kts
plugins {
    alias(libs.plugins.retail.android.library)
    alias(libs.plugins.retail.hilt)
}

dependencies {
    implementation(projects.core.model)      // 全アプリ共通モデル
    implementation(projects.core.network)    // Retrofit 基盤
    implementation(projects.core.common)     // Result, Dispatchers
    
    // このドメイン固有の API, Repository, Model を提供
}
```

### impl モジュールからの依存

```kotlin
// feature/sales/order-list/impl/build.gradle.kts
plugins {
    alias(libs.plugins.retail.android.feature.impl)
    alias(libs.plugins.retail.hilt)
}

dependencies {
    implementation(projects.feature.sales.shared)          // ← ドメイン共有
    implementation(projects.feature.sales.orderList.api)   // ← 自身のナビキー
    
    // 他ドメインの画面に遷移する場合
    implementation(projects.feature.master.productDetail.api)  // ← 商品詳細への遷移キー
}
```

---

## 7. モジュール依存関係（改訂版）

```
app
 ├── feature:sales:order-list:impl
 ├── feature:sales:order-detail:impl
 ├── feature:sales:order-create:impl
 ├── feature:sales:working-plan:impl
 ├── feature:sales:stock:impl
 ├── feature:logistics:shipment-list:impl
 ├── feature:logistics:shipment-detail:impl
 ├── feature:logistics:shipment-tracking:impl
 ├── feature:logistics:inventory-transfer:impl
 ├── feature:master:product-catalog:impl
 ├── feature:master:product-detail:impl
 ├── feature:master:store-list:impl
 ├── feature:master:store-detail:impl
 ├── feature:inventory:inventory-list:impl
 ├── feature:inventory:inventory-detail:impl
 ├── feature:inventory:inventory-adjustment:impl
 ├── feature:auth:impl
 ├── feature:dashboard:impl
 ├── feature:search:impl
 ├── feature:settings:impl
 ├── feature:notifications:impl
 ├── core:designsystem
 ├── core:ui
 ├── core:navigation
 ├── core:data
 ├── core:model
 ├── core:analytics
 └── core:common

feature:{domain}:{subdomain}:api
 └── core:navigation

feature:{domain}:{subdomain}:impl
 ├── feature:{domain}:shared          ← ★ドメイン共有モジュール
 ├── feature:{domain}:{subdomain}:api
 ├── feature:(他ドメイン):(遷移先):api （必要な場合のみ）
 ├── core:designsystem
 ├── core:ui
 ├── core:navigation
 ├── core:analytics
 └── core:common

feature:{domain}:shared
 ├── core:model
 ├── core:network
 ├── core:common
 └── core:data  （共通Repository を使う場合のみ）

core:data
 ├── core:network
 ├── core:database
 ├── core:datastore
 ├── core:model
 └── core:common

core:model
 └── (kotlinx-datetime のみ)
```

### 依存方向の図解

```
┌─────────────────────────────────────────────────────────┐
│                         app                              │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────┴─────┐   ┌────┴─────┐   ┌────┴─────┐
    │ sales:   │   │logistics:│   │ master:  │
    │ order-   │   │shipment- │   │ product- │
    │ list:impl│   │ list:impl│   │ catalog: │
    └────┬─────┘   └────┬─────┘   │ impl    │
         │               │         └────┬─────┘
    ┌────┴─────┐   ┌────┴─────┐   ┌────┴─────┐
    │ sales:   │   │logistics:│   │ master:  │
    │ shared   │   │ shared   │   │ shared   │
    └────┬─────┘   └────┬─────┘   └────┬─────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
    ┌────────────────────┼────────────────────┐
    │          │         │         │           │
 ┌──┴──┐  ┌──┴──┐  ┌──┴──┐  ┌──┴──┐   ┌───┴───┐
 │core:│  │core:│  │core:│  │core:│   │core:  │
 │model│  │net- │  │data │  │comm-│   │design-│
 │     │  │work │  │     │  │on   │   │system │
 └─────┘  └─────┘  └─────┘  └─────┘   └───────┘
```

### 禁止ルール

| 禁止事項 | 理由 |
|---------|------|
| `feature:sales:shared` → `feature:logistics:shared` | ドメイン間の直接依存禁止。必要なら `core/model` 経由 |
| `feature:sales:order-list:impl` → `feature:sales:order-create:impl` | impl 間の直接依存禁止。遷移は `api` モジュール経由 |
| `feature:*` → `app` | 逆依存禁止 |
| `core:*` → `feature:*` | core は feature を知らない |

---

## 8. Gradle Convention Plugin の追加

### 新規追加: AndroidFeatureSharedConventionPlugin

```kotlin
// build-logic/convention/src/main/kotlin/AndroidFeatureSharedConventionPlugin.kt
class AndroidFeatureSharedConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("retail.android.library")
                apply("retail.hilt")
            }
            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:network"))
                add("implementation", project(":core:common"))
            }
        }
    }
}
```

### Convention Plugin 一覧（改訂版）

| Plugin ID | 適用先 | 自動追加される依存 |
|-----------|-------|-------------------|
| `retail.android.feature.api` | `feature:*:*:api` | `:core:navigation` |
| `retail.android.feature.impl` | `feature:*:*:impl` | `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:analytics`, `:core:common` |
| `retail.android.feature.shared` | `feature:*:shared` | `:core:model`, `:core:network`, `:core:common` |
| `retail.android.library` | `core:*` | Kotlin, Android SDKバージョン |
| `retail.android.library.compose` | Compose使用ライブラリ | Compose BOM, Compose Compiler |
| `retail.hilt` | DI使用モジュール | Hilt, KSP |

---

## 9. ドメイン追加時の手順

新しいドメイン（例: `crm` — 顧客管理）を追加する場合:

### Step 1: ディレクトリ作成

```
feature/
└── crm/
    ├── shared/
    │   ├── build.gradle.kts
    │   └── src/main/kotlin/.../feature/crm/shared/
    │       ├── model/
    │       │   ├── Customer.kt
    │       │   └── CustomerSegment.kt
    │       ├── api/
    │       │   └── CustomerApi.kt
    │       ├── repository/
    │       │   ├── CustomerRepository.kt
    │       │   └── DefaultCustomerRepository.kt
    │       └── mapper/
    │           └── CustomerMapper.kt
    │
    ├── customer-list/
    │   ├── api/
    │   │   ├── build.gradle.kts
    │   │   └── src/.../CustomerListNavKey.kt
    │   └── impl/
    │       ├── build.gradle.kts
    │       └── src/.../
    │           ├── CustomerListScreen.kt
    │           ├── CustomerListViewModel.kt
    │           └── model/CustomerListUiState.kt
    └── customer-detail/
        ├── api/
        └── impl/
```

### Step 2: settings.gradle.kts に追加

```kotlin
// Feature: CRM（顧客管理）
include(":feature:crm:shared")
include(":feature:crm:customer-list:api")
include(":feature:crm:customer-list:impl")
include(":feature:crm:customer-detail:api")
include(":feature:crm:customer-detail:impl")
```

### Step 3: app モジュールに依存追加

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(projects.feature.crm.customerList.impl)
    implementation(projects.feature.crm.customerDetail.impl)
}
```

### Step 4: ナビゲーションに追加

```kotlin
// app/src/main/kotlin/.../navigation/RetailNavHost.kt に遷移ルートを追加
```

---

## 10. よくある質問

### Q1: ドメインをまたぐデータ共有はどうする？

**例**: 受注作成画面で商品マスタの情報を参照したい。

**方法**: `feature:sales:order-create:impl` が `feature:master:shared` に依存するのではなく、  
`core/model` に共通参照用の最小限モデルを置く。

```kotlin
// core/model/ProductRef.kt
// 他ドメインが参照するための最小限の商品情報
data class ProductRef(
    val id: String,
    val name: String,
    val price: BigDecimal,
)
```

または、バックエンドの API が受注作成のレスポンスに商品情報を含めるように設計する（推奨）。

### Q2: shared モジュールが肥大化したら？

1つの shared に大量のコードが集まった場合、shared 内をパッケージで整理:

```
feature/sales/shared/src/main/kotlin/.../
├── order/          # 受注関連
│   ├── Order.kt
│   ├── OrderApi.kt
│   └── OrderRepository.kt
├── workingplan/    # 作業計画関連
│   ├── WorkingPlan.kt
│   └── WorkingPlanApi.kt
└── stock/          # 販売在庫関連
    ├── SalesStock.kt
    └── SalesStockApi.kt
```

Gradle モジュールを分割するほどではないが、パッケージで論理的に整理する。

### Q3: 共通機能（auth, search等）にドメインフォルダは不要？

共通機能は特定のビジネスドメインに属さないため、`feature/` 直下に配置。  
将来的に共通機能が増えた場合は `feature/common/` にまとめることも可能。

### Q4: NiA では feature 直下にフラットに配置しているが問題ないのか？

NiA は **5画面のニュースアプリ** であり、ドメインが1つしかない。  
新規PJは **21+画面の業務アプリ** であり、複数ドメインがある。  
規模が異なるため、構成も異なるのが適切。

### Q5: Gradle のネストが深くなりすぎないか？

`feature:sales:order-list:impl` は4階層だが、Gradle はこれを問題なく処理する。  
NiA 自体も `feature:foryou` → `feature:foryou:api` / `feature:foryou:impl` のように3階層。  
1階層増えるだけで、保守性の向上効果の方が大きい。
