# 大規模モバイルアプリ設計ガイド（100画面超・複数ドメイン対応）

**前提**: 25億規模の海外販売システム。100画面超。複数ドメインにまたがる。

---

## 1. 問題：全部を1つの `app/` に入れるとどうなるか

現在のサンプルプロジェクトは3画面。しかし100画面超になると：

```
app/src/main/java/com/example/myapplication/
├── ui/screen/
│   ├── productlist/
│   ├── productdetail/
│   ├── cart/
│   ├── checkout/
│   ├── order/
│   ├── orderdetail/
│   ├── orderhistory/
│   ├── payment/
│   ├── inventory/
│   ├── inventoryscan/
│   ├── stocktransfer/
│   ├── profile/
│   ├── settings/
│   ├── login/
│   ├── ... あと85画面
│
├── data/repository/   ← 50個のRepository
├── domain/model/      ← 80個のModel
├── di/                ← 巨大なDIモジュール

→ ビルド時間: 10分超
→ 1ファイル変更しても全体が再コンパイル
→ どこに何があるか誰もわからない
→ チーム間でのマージコンフリクト地獄
```

---

## 2. 解決策：マルチモジュール構成

**1つの巨大な `app/` を、ドメインごとにモジュール分割する。**

```
┌─────────────────────────────────────────────────────────────────┐
│                        :app                                     │
│       アプリの起点。各featureモジュールを組み立てるだけ。        │
│       MainActivity, AppNavigation, MyApplication               │
└────────┬──────────┬──────────┬──────────┬──────────┬────────────┘
         │          │          │          │          │
┌────────▼────┐ ┌───▼─────┐ ┌──▼──────┐ ┌─▼───────┐ ┌▼──────────┐
│ :feature:   │ │:feature:│ │:feature:│ │:feature:│ │:feature:   │
│  product    │ │  cart   │ │  order  │ │inventory│ │  auth      │
│             │ │         │ │         │ │(店舗用) │ │            │
│ 商品一覧    │ │ カート  │ │ 注文    │ │ 在庫管理│ │ ログイン   │
│ 商品詳細    │ │ 決済    │ │ 注文履歴│ │ 棚卸し  │ │ 会員登録   │
│ カテゴリ    │ │         │ │ 配送追跡│ │ 入出庫  │ │ パスワード │
│ 検索        │ │         │ │         │ │ スキャン│ │            │
└──────┬──────┘ └────┬────┘ └────┬────┘ └────┬────┘ └─────┬──────┘
       │             │          │           │            │
       └──────┬──────┴──────┬───┴───────────┴────────────┘
              │             │
       ┌──────▼──────┐ ┌───▼────────────┐
       │  :core:ui   │ │  :core:data    │
       │             │ │                │
       │ AppButton   │ │ Retrofit設定   │
       │ AppCard     │ │ Room設定       │
       │ Theme       │ │ OkHttp設定     │
       │ LoadingBar  │ │ MockInterceptor│
       └──────┬──────┘ └───┬────────────┘
              │            │
       ┌──────▼────────────▼──┐
       │    :core:common      │
       │                      │
       │ Result拡張関数       │
       │ 日付フォーマット     │
       │ 通貨フォーマット     │
       │ ログユーティリティ   │
       └──────────────────────┘
```

---

## 3. モジュール一覧と責務

### 3-1. app モジュール

```
:app
├── MyApplication.kt          ← @HiltAndroidApp
├── MainActivity.kt           ← @AndroidEntryPoint
└── AppNavigation.kt          ← 全featureのNavGraphを組み合わせる
```

**責務**: 各 feature モジュールを束ねるだけ。ビジネスロジックもUIも書かない。

---

### 3-2. feature モジュール（ドメインごと）

| モジュール名 | 担当ドメイン | 画面数（目安） | 担当チーム |
|---|---|---|---|
| `:feature:product` | 商品検索・閲覧 | 10〜15画面 | チームA |
| `:feature:cart` | カート・決済 | 8〜10画面 | チームB |
| `:feature:order` | 注文・配送追跡 | 10〜12画面 | チームB |
| `:feature:inventory` | 在庫管理（店舗スタッフ用） | 12〜15画面 | チームC |
| `:feature:auth` | ログイン・会員登録 | 5〜8画面 | チームD |
| `:feature:profile` | マイページ・設定 | 8〜10画面 | チームD |
| `:feature:notification` | 通知一覧・設定 | 5〜8画面 | チームD |
| `:feature:store` | 店舗検索・案内 | 8〜10画面 | チームA |
| `:feature:scan` | バーコード・QRスキャン | 3〜5画面 | チームC |
| `:feature:review` | レビュー・評価 | 5〜8画面 | チームA |

**各 feature モジュールの内部構成はサンプルプロジェクトと同じ MVVM + Repository:**

```
feature/product/
├── build.gradle.kts
└── src/main/java/com/example/feature/product/
    ├── data/
    │   ├── api/ProductApi.kt
    │   ├── dto/ProductDto.kt
    │   └── repository/ProductQueryRepositoryImpl.kt
    ├── domain/
    │   ├── model/Product.kt
    │   └── repository/ProductQueryRepository.kt
    ├── di/
    │   └── ProductModule.kt
    └── ui/
        ├── list/ProductListScreen.kt
        ├── list/ProductListViewModel.kt
        ├── detail/ProductDetailScreen.kt
        ├── detail/ProductDetailViewModel.kt
        ├── search/SearchScreen.kt
        ├── search/SearchViewModel.kt
        └── navigation/ProductNavGraph.kt   ← このfeature内の画面遷移
```

---

### 3-3. core モジュール（共通基盤）

| モジュール名 | 提供するもの |
|---|---|
| `:core:ui` | 共通Composeコンポーネント（AppButton, AppCard等）、Theme、デザイントークン |
| `:core:data` | Retrofit/OkHttp設定、Room基底クラス、NetworkModule |
| `:core:common` | ユーティリティ関数、拡張関数、定数、共通Model |
| `:core:navigation` | 画面遷移のRoute定義（feature間の遷移ルール） |
| `:core:testing` | テスト用ユーティリティ、FakeRepository、テストルール |

---

## 4. 依存関係のルール（最重要）

```
                        ✅ 依存してOK
         ┌──────────────────────────────────┐
         │              :app                │
         │  (全featureと全coreに依存可)      │
         └──────┬───────────────────────────┘
                │
    ┌───────────▼───────────┐
    │    :feature:xxx       │
    │                       │
    │  ✅ :core:ui に依存   │
    │  ✅ :core:data に依存 │
    │  ✅ :core:common に依存│
    │                       │
    │  ❌ 他のfeatureに依存しない │  ← ★ 最重要ルール
    │                       │
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │     :core:xxx         │
    │                       │
    │  ✅ :core:common に依存│
    │  ❌ featureに依存しない │
    └───────────────────────┘
```

### なぜ feature 同士の依存を禁止するのか

```
❌ :feature:cart が :feature:product に直接依存すると...
   → product を変更するたびに cart も再ビルド
   → チームAの変更がチームBのビルドを壊す
   → 循環依存のリスク

✅ feature 間の連携は :core:navigation 経由で行う
   → product と cart は互いの存在を知らない
   → でも画面遷移は可能（Routeの定義のみ共有）
```

---

## 5. feature 間の画面遷移（共通ナビゲーション）

feature 同士が直接依存しないのに、どうやって画面遷移するのか？

### :core:navigation にRouteを定義

```kotlin
// :core:navigation/src/.../AppRoutes.kt
// 全featureの画面Routeをここで一元定義
object AppRoutes {
    // ── Product ──
    const val PRODUCT_LIST = "products"
    const val PRODUCT_DETAIL = "products/{productId}"
    fun productDetail(id: String) = "products/$id"

    // ── Cart ──
    const val CART = "cart"
    const val CHECKOUT = "cart/checkout"

    // ── Order ──
    const val ORDER_HISTORY = "orders"
    const val ORDER_DETAIL = "orders/{orderId}"
    fun orderDetail(id: String) = "orders/$id"

    // ── Auth ──
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"

    // ── Inventory（店舗スタッフ用） ──
    const val INVENTORY_LIST = "inventory"
    const val INVENTORY_SCAN = "inventory/scan"
}
```

### 各 feature が自分のNavGraphを定義

```kotlin
// :feature:product/src/.../navigation/ProductNavGraph.kt
fun NavGraphBuilder.productNavGraph(
    onNavigateToCart: () -> Unit,          // ← cartに遷移する関数を外から受け取る
    onNavigateToDetail: (String) -> Unit   // ← 他featureは知らない
) {
    composable(AppRoutes.PRODUCT_LIST) {
        val viewModel: ProductListViewModel = hiltViewModel()
        ProductListScreen(
            viewModel = viewModel,
            onProductClick = onNavigateToDetail,
            onCartClick = onNavigateToCart     // cart への遷移
        )
    }
    composable(AppRoutes.PRODUCT_DETAIL) {
        val viewModel: ProductDetailViewModel = hiltViewModel()
        ProductDetailScreen(viewModel = viewModel)
    }
}
```

### :app で全NavGraphを組み立て

```kotlin
// :app/src/.../AppNavigation.kt
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = AppRoutes.PRODUCT_LIST) {

        // Product feature の画面遷移を登録
        productNavGraph(
            onNavigateToCart = { navController.navigate(AppRoutes.CART) },
            onNavigateToDetail = { id -> navController.navigate(AppRoutes.productDetail(id)) }
        )

        // Cart feature の画面遷移を登録
        cartNavGraph(
            onNavigateToCheckout = { navController.navigate(AppRoutes.CHECKOUT) },
            onNavigateToOrders = { navController.navigate(AppRoutes.ORDER_HISTORY) }
        )

        // Order feature
        orderNavGraph(
            onNavigateToDetail = { id -> navController.navigate(AppRoutes.orderDetail(id)) }
        )

        // Auth feature
        authNavGraph(...)

        // Inventory feature
        inventoryNavGraph(...)
    }
}
```

**→ 各 feature は互いの実装を知らない。Route文字列だけを共有する。**

---

## 6. 共通コンポーネントの管理（:core:ui）

### Figma の共通コンポーネントはここに集約

```
:core:ui/src/main/java/com/example/core/ui/
├── theme/
│   ├── Color.kt              ← Figma Variables → Style Dictionary で自動生成
│   ├── Type.kt
│   ├── Shape.kt
│   └── Theme.kt
│
├── component/                 ← Figma 共通コンポーネントの Android 実装
│   ├── button/
│   │   ├── AppButton.kt      ← Figma "Button/Primary"
│   │   ├── AppOutlinedButton.kt
│   │   └── AppTextButton.kt
│   ├── card/
│   │   ├── AppCard.kt        ← Figma "Card/Default"
│   │   └── AppProductCard.kt ← Figma "Card/Product"
│   ├── input/
│   │   ├── AppTextField.kt   ← Figma "Input/Text"
│   │   ├── AppSearchBar.kt   ← Figma "Input/Search"
│   │   └── AppDropdown.kt
│   ├── feedback/
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorMessage.kt
│   │   └── EmptyState.kt
│   ├── navigation/
│   │   ├── AppTopBar.kt
│   │   ├── AppBottomBar.kt
│   │   └── AppDrawer.kt
│   └── badge/
│       ├── StockBadge.kt     ← "残り3点"
│       └── SaleBadge.kt      ← "SALE"
│
└── extension/
    ├── ModifierExtensions.kt  ← Modifier の便利拡張
    └── PaddingExtensions.kt
```

### feature での使い方

```kotlin
// :feature:product の画面で core:ui のコンポーネントを使う
import com.example.core.ui.component.button.AppButton
import com.example.core.ui.component.card.AppProductCard
import com.example.core.ui.component.feedback.LoadingIndicator

@Composable
fun ProductListScreen(...) {
    // 共通コンポーネントをそのまま使う（自分で作らない）
    AppProductCard(product = product, onClick = { ... })
    AppButton(text = "カートに入れる", onClick = { ... })
}
```

---

## 7. 共通データ層の管理（:core:data）

### Retrofit / OkHttp / Room の設定は :core:data に集約

```
:core:data/src/main/java/com/example/core/data/
├── network/
│   ├── NetworkModule.kt          ← OkHttp, Retrofit 生成（Hilt Module）
│   ├── AuthInterceptor.kt        ← 認証トークン自動付与
│   ├── LanguageInterceptor.kt    ← Accept-Language ヘッダー付与（多言語）
│   └── ErrorConverter.kt         ← APIエラー → アプリ共通エラーに変換
│
├── database/
│   ├── AppDatabase.kt            ← Room DB（全テーブルを管理）
│   └── DatabaseModule.kt         ← Hilt Module
│
├── datastore/
│   └── UserPreferences.kt        ← DataStore（言語設定・通貨設定等）
│
└── model/
    ├── ApiError.kt               ← 共通エラーモデル
    └── PaginatedResponse.kt      ← ページネーション共通モデル
```

### feature は :core:data が提供する Retrofit インスタンスを使う

```kotlin
// :feature:product/di/ProductModule.kt
@Module
@InstallIn(SingletonComponent::class)
object ProductModule {
    @Provides
    fun provideProductApi(retrofit: Retrofit): ProductApi {  // ← core:data が提供するRetrofit
        return retrofit.create(ProductApi::class.java)
    }
}
```

---

## 8. settings.gradle.kts でモジュールを登録

```kotlin
// settings.gradle.kts
rootProject.name = "RetailApp"

include(":app")

// ── core モジュール ──
include(":core:ui")
include(":core:data")
include(":core:common")
include(":core:navigation")
include(":core:testing")

// ── feature モジュール ──
include(":feature:product")
include(":feature:cart")
include(":feature:order")
include(":feature:inventory")
include(":feature:auth")
include(":feature:profile")
include(":feature:notification")
include(":feature:store")
include(":feature:scan")
include(":feature:review")
```

---

## 9. 各モジュールの build.gradle.kts での依存関係

```kotlin
// :app/build.gradle.kts
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
    // 全featureに依存
    implementation(project(":feature:product"))
    implementation(project(":feature:cart"))
    implementation(project(":feature:order"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:auth"))
    // ...
}

// :feature:product/build.gradle.kts
dependencies {
    implementation(project(":core:ui"))         // ✅ 共通UI
    implementation(project(":core:data"))        // ✅ 共通データ
    implementation(project(":core:common"))      // ✅ 共通ユーティリティ
    implementation(project(":core:navigation"))  // ✅ Route定義
    // ❌ implementation(project(":feature:cart"))  ← 禁止！
}

// :core:ui/build.gradle.kts
dependencies {
    implementation(project(":core:common"))    // ✅ 共通ユーティリティ
    // ❌ featureへの依存は禁止
}
```

---

## 10. マルチモジュールのメリット

| 観点 | 1モジュール（現状） | マルチモジュール |
|---|---|---|
| **ビルド時間** | 1ファイル変更で全再ビルド（10分超） | 変更モジュールのみ再ビルド（1〜2分） |
| **チーム開発** | マージコンフリクト頻発 | feature単位で独立開発 |
| **コード把握** | 数百ファイルで迷子 | モジュール内は20〜30ファイルで把握可能 |
| **テスト** | 全テスト実行が遅い | モジュール単位でテスト実行可能 |
| **依存関係** | 何でもアクセスできて無秩序 | モジュール境界で強制的に秩序が保たれる |
| **新メンバー参入** | 全体理解が必要で時間がかかる | 担当featureだけ理解すれば着手可能 |

---

## 11. 移行ロードマップ

小規模プロジェクト（サンプル）→ 大規模プロジェクトへの段階的移行。

```
Phase 0（現在）
  1つの :app モジュールに全部入っている（サンプルプロジェクトの状態）

Phase 1（プロジェクト初期）
  :core:ui, :core:data, :core:common を切り出す
  → 共通基盤を先に整備する

Phase 2（機能開発開始時）
  最初の2〜3 featureを切り出す（:feature:product, :feature:auth）
  → チームがマルチモジュール開発に慣れる

Phase 3（本格開発）
  残りのfeatureを順次切り出す
  → 各チームが独立して開発可能に

Phase 4（安定運用）
  Convention Plugin で build.gradle.kts を共通化
  → 新feature追加時のボイラープレートを最小化
```

---

## 12. まとめ：設計判断フローチャート

```
画面数は何画面？
  │
  ├── 10画面以下 → 1モジュールで十分（サンプルプロジェクトのまま）
  │
  ├── 10〜30画面 → core と app の2層構成
  │                core:ui + core:data を切り出す
  │
  └── 30画面超  → マルチモジュール構成（本ガイド）
       │
       ├── feature はドメイン単位で分割
       ├── core は関心事ごとに分割（ui / data / common / navigation）
       ├── feature 間の依存は禁止（navigation 経由で連携）
       └── 各 feature 内部は MVVM + Repository（サンプルと同じ構成）
```

---

*本ガイドは方針書（mobile-system-policy.md）およびサンプルプロジェクト（sample-project-guide.md）の補足資料です。*

