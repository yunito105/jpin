# サンプルプロジェクト ステップバイステップガイド

**目的**: 方針書で推奨した全技術スタックを実装したサンプルプロジェクト  
**テーマ**: 家具・ホームセンター向け小売アプリ  

---

## 全体アーキテクチャ図

```
┌─────────────────────────────────────────────────────────────────────┐
│  UI Layer（Jetpack Compose）                                       │
│                                                                     │
│  ProductListScreen    ProductDetailScreen    CartScreen             │
│       ↕ UiState            ↕ UiState            ↕ UiState         │
│  ProductListViewModel ProductDetailViewModel CartViewModel         │
└─────────┬────────────────────┬────────────────────┬─────────────────┘
          │                    │                    │
          │    ViewModel は Repository を呼ぶだけ    │
          │                    │                    │
┌─────────▼────────────────────▼────────────────────▼─────────────────┐
│  Data Layer                                                        │
│                                                                     │
│  ProductQueryRepository ──── GET  /api/v1/products     ── Query    │
│  CartRepository ─────────── GET  /api/v1/cart          ── Query    │
│                          ── POST /api/v1/cart/items    ── Command  │
│                          ── DELETE /api/v1/cart/items/x ── Command │
│                                                                     │
│  ┌─────────────────┐    ┌──────────────────────┐                   │
│  │ Retrofit + OkHttp│    │ Room（ローカルDB）   │                   │
│  │（API通信）       │    │（オフラインキャッシュ）│                  │
│  └────────┬─────────┘    └──────────────────────┘                   │
└───────────┼─────────────────────────────────────────────────────────┘
            │ HTTP
┌───────────▼─────────────────────────────────────────────────────────┐
│  Spring Boot API（バックエンド）                                    │
│  ヘキサゴナルアーキテクチャ + 簡易CQRS                             │
│  業務ロジックはすべてここ（価格計算・在庫引当・バリデーション等）   │
│                          ↓                                         │
│                      Oracle DB                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## フォルダ構成

```
app/src/main/java/com/example/myapplication/
│
├── MyApplication.kt              ← Hilt の起点（@HiltAndroidApp）
├── MainActivity.kt               ← Compose の起点（@AndroidEntryPoint）
│
├── domain/                        ← ドメイン層（What: 何をするか）
│   ├── model/
│   │   ├── Product.kt            ← 商品モデル（データの入れ物）
│   │   └── CartItem.kt           ← カートアイテムモデル
│   └── repository/
│       ├── ProductQueryRepository.kt  ← Interface（参照系）
│       └── CartRepository.kt         ← Interface（参照+更新系）
│
├── data/                          ← データ層（How: どうやって取るか）
│   ├── remote/
│   │   ├── api/
│   │   │   ├── ProductApi.kt     ← Retrofit API定義（GET = Query）
│   │   │   └── CartApi.kt        ← Retrofit API定義（POST/DELETE = Command）
│   │   ├── dto/
│   │   │   ├── ProductDto.kt     ← JSON ↔ Kotlin 変換用
│   │   │   └── CartDto.kt
│   │   └── mock/
│   │       └── MockInterceptor.kt ← デモ用モックデータ（本番では削除）
│   ├── local/
│   │   ├── AppDatabase.kt        ← Room DB定義
│   │   ├── dao/ProductDao.kt     ← DAO（DBアクセス）
│   │   └── entity/ProductEntity.kt ← テーブル定義
│   └── repository/
│       ├── ProductQueryRepositoryImpl.kt ← Interface の実装
│       └── CartRepositoryImpl.kt
│
├── di/                            ← DI層（Hilt モジュール）
│   ├── NetworkModule.kt           ← Retrofit, OkHttp, API の生成
│   ├── DatabaseModule.kt          ← Room DB の生成
│   └── RepositoryModule.kt        ← Interface ↔ Implementation の紐付け
│
└── ui/                            ← UI層（画面）
    ├── theme/
    │   ├── Color.kt               ← デザイントークン（カラー）
    │   ├── Type.kt                ← デザイントークン（タイポグラフィ）
    │   └── Theme.kt               ← Material Theme
    ├── component/
    │   ├── AppButton.kt           ← 共通ボタン（Figma対応）
    │   ├── AppProductCard.kt      ← 商品カード（Figma対応）
    │   └── LoadingIndicator.kt    ← ローディング表示
    ├── navigation/
    │   └── AppNavigation.kt       ← 画面遷移定義
    └── screen/
        ├── productlist/
        │   ├── ProductListViewModel.kt  ← ViewModel + UiState
        │   └── ProductListScreen.kt     ← Composable（View）
        ├── productdetail/
        │   ├── ProductDetailViewModel.kt
        │   └── ProductDetailScreen.kt
        └── cart/
            ├── CartViewModel.kt
            └── CartScreen.kt
```

---

## データの流れ（商品一覧を表示する場合）

```
① ユーザーがアプリを開く
   │
② ProductListScreen（View）が表示される
   │ collectAsStateWithLifecycle で UiState を監視開始
   │
③ ProductListViewModel.init { loadProducts() } が呼ばれる
   │
④ _uiState.update { isLoading = true }  ← 画面にスピナー表示
   │
⑤ productQueryRepository.getProducts() を呼ぶ
   │
⑥ ProductQueryRepositoryImpl が Retrofit で API を呼ぶ
   │ GET http://10.0.2.2:8080/api/v1/products
   │
⑦ （デモ時）MockInterceptor がリクエストを横取りしてモックJSON返却
   （本番時）Spring Boot API が Oracle DB から商品データを返却
   │
⑧ ProductDto（JSON）→ Product（ドメインモデル）に変換
   │ 同時に Room にキャッシュ保存
   │
⑨ _uiState.update { isLoading = false, products = [...] }
   │
⑩ ProductListScreen が UiState の変化を検知して商品グリッドを表示
   （Coil が各商品画像を非同期で読み込む）
```

---

## バックエンド（Spring Boot）との接続方法

### 1. MockInterceptor を使う（デモモード）

現在の状態。バックエンド不要でアプリが動作する。

```
OkHttpClient
  └── MockInterceptor ← リクエストを横取りしてモックデータ返却
  └── LoggingInterceptor
```

### 2. 実際の Spring Boot に接続する（本番モード）

`NetworkModule.kt` の2箇所を変更するだけ。

```kotlin
// ① BASE_URL を Spring Boot のアドレスに変更
private const val BASE_URL = "http://10.0.2.2:8080/api/"  // ← エミュレータ用
// private const val BASE_URL = "https://api.example.com/api/"  // ← 本番用

// ② MockInterceptor の行を削除
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        // .addInterceptor(mockInterceptor)  ← この行を削除するだけ！
        .addInterceptor(HttpLoggingInterceptor().apply { ... })
        .build()
}
```

### 3. Spring Boot 側の対応するエンドポイント

| モバイル（Retrofit） | Spring Boot Controller | CQRS |
|---|---|---|
| `GET /api/v1/products` | `ProductQueryController.getProducts()` | Query |
| `GET /api/v1/products/{id}` | `ProductQueryController.getProductById()` | Query |
| `GET /api/v1/cart` | `CartQueryController.getCart()` | Query |
| `POST /api/v1/cart/items` | `CartCommandController.addToCart()` | Command |
| `DELETE /api/v1/cart/items/{id}` | `CartCommandController.removeFromCart()` | Command |

### 4. Spring Boot のサンプルコントローラ（参考）

```java
// ── Query Controller（参照系） ──
@RestController
@RequestMapping("/api/v1/products")
public class ProductQueryController {

    private final ProductQueryHandler queryHandler;

    @GetMapping
    public List<ProductResponse> getProducts(
        @RequestParam(required = false) String category
    ) {
        return queryHandler.handle(new GetProductsQuery(category));
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable String id) {
        return queryHandler.handle(new GetProductByIdQuery(id));
    }
}

// ── Command Controller（更新系） ──
@RestController
@RequestMapping("/api/v1/cart")
public class CartCommandController {

    private final CartCommandHandler commandHandler;

    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(@RequestBody AddToCartCommand command) {
        commandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeFromCart(@PathVariable String id) {
        commandHandler.handle(new RemoveFromCartCommand(id));
        return ResponseEntity.noContent().build();
    }
}
```

### 5. JSON フォーマット（API仕様）

モバイルの `@Serializable` DTO とバックエンドのレスポンスが一致している必要がある。

```json
// GET /api/v1/products レスポンス
[
  {
    "id": "1",
    "name": "BILLY 本棚",
    "description": "シンプルで使いやすい定番の本棚",
    "price": 4990,
    "currency": "JPY",
    "image_url": "https://cdn.example.com/products/1.jpg",
    "category_id": "furniture",
    "stock_quantity": 15
  }
]

// POST /api/v1/cart/items リクエスト
{
  "product_id": "1",
  "quantity": 2
}

// GET /api/v1/cart レスポンス
{
  "items": [
    {
      "id": "cart-1",
      "product_id": "1",
      "product_name": "BILLY 本棚",
      "price": 4990,
      "quantity": 2,
      "image_url": "https://cdn.example.com/products/1.jpg"
    }
  ],
  "total_price": 9980
}
```

---

## 各技術がどこで使われているか

| 推奨技術 | 使用ファイル | 役割 |
|---|---|---|
| **Jetpack Compose** | `*Screen.kt`, `AppButton.kt`, `AppProductCard.kt` | 画面描画 |
| **Hilt** | `di/*.kt`, `@HiltViewModel`, `@AndroidEntryPoint` | 依存性注入 |
| **Coroutines + Flow** | `*ViewModel.kt`, `*Repository.kt` | 非同期処理 + 状態管理 |
| **Retrofit2** | `ProductApi.kt`, `CartApi.kt` | API呼び出し |
| **OkHttp3** | `NetworkModule.kt`, `MockInterceptor.kt` | HTTP通信 + ログ |
| **Room** | `AppDatabase.kt`, `ProductDao.kt`, `ProductEntity.kt` | ローカルキャッシュ |
| **Coil** | `AppProductCard.kt`, `ProductDetailScreen.kt`, `CartScreen.kt` | 画像読み込み |
| **Navigation Compose** | `AppNavigation.kt` | 画面遷移 |
| **Kotlin Serialization** | `ProductDto.kt`, `CartDto.kt` | JSON変換 |
| **Material Design 3** | `Theme.kt`, `Color.kt`, `Type.kt` | デザインシステム |

---

## MVVM + Repository パターンの復習

このサンプルプロジェクト全体が以下のパターンで構成されている。

```
View（Composable）
  │ UiState を監視して表示するだけ
  │ ユーザー操作を ViewModel に伝えるだけ
  │
ViewModel
  │ UiState を StateFlow で管理
  │ Repository を呼ぶだけ（業務ロジックは書かない）
  │
Repository（Interface）
  │ What: 何を取得するか・何を更新するかを定義
  │
Repository（Implementation）
  │ How: Retrofit で API を呼ぶ / Room でキャッシュする
  │
Spring Boot API（バックエンド）
    業務ロジックはすべてここ
```

**★ モバイルは「薄いクライアント」。APIを呼んで結果を表示するだけ。**

