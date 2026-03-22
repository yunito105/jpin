# MVVMの正体 — ヘキサゴナルアーキテクチャ経験者のための比較ガイド

**前提**:
- バックエンド: ヘキサゴナルアーキテクチャ + 簡易CQRS
- モバイル: 薄いクライアント（業務ロジックなし、表示と操作のみ）

---

## 1. そもそも MVVM の各層は何をしているのか

ヘキサゴナルに馴染みがある方向けに、**実際のコードで**各層の仕事を示す。

### ■ Model（データ層）— 「外部との通信係」

```
ヘキサゴナルで言えば: Secondary Adapter（Driven Side）
つまり: 外部システム（API / DB）とのやり取りを担当する層
```

```kotlin
// ── Model層のコード ──

// ① Retrofitでバックエンドを呼ぶ（= Secondary Adapter）
class ProductRepositoryImpl(
    private val api: ProductApi,   // HTTP通信
    private val dao: ProductDao    // ローカルDB
) : ProductRepository {

    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = api.getProducts()   // Spring Boot を呼ぶ
            dao.insertAll(response)            // Room にキャッシュ
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            val cache = dao.getAll()           // オフライン時はキャッシュ
            Result.success(cache.map { it.toDomain() })
        }
    }
}

// このクラスがやっていること:
// ・APIを叩く
// ・キャッシュに保存する
// ・エラー時にフォールバックする
//
// やっていないこと:
// ・価格計算（バックエンドの仕事）
// ・在庫判定（バックエンドの仕事）
// ・画面の状態管理（ViewModelの仕事）
```

**→ Model = 「データの調達係」。業務ロジックはゼロ。**

---

### ■ ViewModel — 「画面の状態管理係」

```
ヘキサゴナルで言えば: 該当なし（ヘキサゴナルにはない概念）
強いて言えば: Application Service に近いが、UI状態の管理だけを行う
```

```kotlin
// ── ViewModel層のコード ──

class ProductListViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // 画面の状態（Loading / Success / Error の3パターン）
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }        // → 画面にスピナー表示

            repository.getProducts()                              // → Model にデータ取得を依頼
                .onSuccess { products ->
                    _uiState.update {
                        it.copy(isLoading = false, products = products)  // → 画面に商品リスト表示
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message) // → 画面にエラー表示
                    }
                }
        }
    }
}

// このクラスがやっていること:
// ・画面の状態（Loading / Success / Error）を管理する
// ・Viewからのイベントを受け取る
// ・Repositoryにデータ取得を依頼する
// ・結果をUIが表示しやすい形に変換する
//
// やっていないこと:
// ・APIを直接叩く（Repositoryに任せる）
// ・業務ロジック（バックエンドに任せる）
// ・画面のレイアウト（Viewに任せる）
```

**→ ViewModel = 「UIの状態管理係」。Repositoryを呼んで結果をUIに渡すだけ。**

---

### ■ View（Composable）— 「表示係」

```
ヘキサゴナルで言えば: Primary Adapter（Driving Side）
つまり: ユーザーからの入力を受け取り、結果を表示する層
```

```kotlin
// ── View層のコード ──

@Composable
fun ProductListScreen(viewModel: ProductListViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> CircularProgressIndicator()       // スピナー表示
        uiState.error != null -> Text("エラー: ${uiState.error}") // エラー表示
        else -> {
            LazyColumn {
                items(uiState.products) { product ->
                    Text("${product.name} - ${product.formattedPrice}")  // 商品表示
                }
            }
        }
    }
}

// このクラスがやっていること:
// ・UiStateを受け取って表示する
// ・ユーザー操作（タップ等）をViewModelに伝える
//
// やっていないこと:
// ・状態管理（ViewModelに任せる）
// ・データ取得（ViewModelを通してRepositoryに任せる）
// ・業務ロジック（バックエンドに任せる）
```

---

## 2. ヘキサゴナルとMVVMの概念マッピング

```
┌────────────────────────────────────────────────────────────────────┐
│  ヘキサゴナル（バックエンド）       MVVM（モバイル）              │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Driving Adapter                    View (Composable)              │
│  （REST Controller）                （ユーザーの入力を受ける）     │
│         │                                    │                     │
│         ▼                                    ▼                     │
│  Primary Port                       ViewModel                     │
│  （UseCase Interface）              （UiStateを管理する）         │
│         │                                    │                     │
│         ▼                                    ▼                     │
│  Application Service                ※ 該当なし                    │
│  （業務ロジック実行）               （業務ロジックはバックエンドに │
│         │                             あるので、モバイルには不要） │
│         ▼                                    │                     │
│  Secondary Port                     Repository Interface           │
│  （Repository Interface）           （データ取得の抽象化）        │
│         │                                    │                     │
│         ▼                                    ▼                     │
│  Driven Adapter                     Repository Implementation     │
│  （JPA / Oracle接続）               （Retrofit / Room）           │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 重要な気づき

**バックエンドのヘキサゴナルと比較すると、モバイルのMVVMには「Application Service」に相当する層がない。**

なぜなら:
- バックエンドの Application Service = 業務ロジックの実行場所
- モバイルは業務ロジックを持たない（バックエンドに任せている）
- だから**モバイルのViewModelは「ロジックを実行する」のではなく「UIの状態を管理する」だけ**

---

## 3. 忖度なしの比較：本当にMVVMが最適か？

4つのアーキテクチャを**あなたのプロジェクト条件**で評価する。

### 条件の再確認

```
・業務ロジックはバックエンド完結
・モバイルはAPIを呼んで表示するだけ（Thin Client）
・Kotlin経験者が少ない（Java経験者が多い）
・100画面超の大規模プロジェクト
・バックエンドはヘキサゴナル + 簡易CQRS
```

---

### ① MVVM + Repository

```
View → ViewModel → Repository → Retrofit → Spring Boot
                                         → Room（キャッシュ）
```

**ViewModelの中身:**
```kotlin
class ProductListViewModel(private val repo: ProductRepository) : ViewModel() {
    val uiState: StateFlow<UiState> = ...

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repo.getProducts()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message) }
        }
    }
}
```

✅ **メリット**
- Google公式推奨。ドキュメント・サンプルが最も多い
- Java経験者でも理解しやすい
- Hiltとの連携が標準対応（@HiltViewModel）
- Thin Client に適した軽量さ

❌ **デメリット**
- 「Model」の定義が曖昧（Repository? Domain Model? DTO? 全部？）
- ヘキサゴナル経験者には用語が混乱しやすい
- ViewModelが「状態管理」と「ユースケース呼び出し」の2責務を持つ
- 画面が複雑になるとViewModelが肥大化する

---

### ② MVI（Model-View-Intent）

```
View --(Intent)--> ViewModel --(Reduce)--> UiState --> View
                       ↓
                   Repository → Spring Boot
```

**ViewModelの中身:**
```kotlin
// 画面が取りうる全イベントを型で定義
sealed class ProductIntent {
    object LoadProducts : ProductIntent()
    data class SelectCategory(val id: String) : ProductIntent()
    data class AddToCart(val productId: String) : ProductIntent()
}

// 画面が取りうる全状態を型で定義
sealed class ProductUiState {
    object Loading : ProductUiState()
    data class Success(val products: List<Product>) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}

class ProductViewModel(private val repo: ProductRepository) : ViewModel() {
    val uiState: StateFlow<ProductUiState> = ...

    fun onIntent(intent: ProductIntent) {
        when (intent) {
            is ProductIntent.LoadProducts -> loadProducts()
            is ProductIntent.SelectCategory -> loadByCategory(intent.id)
            is ProductIntent.AddToCart -> addToCart(intent.productId)
        }
    }
}
```

✅ **メリット**
- 全イベント・全状態が sealed class で列挙される → 漏れがない
- 単方向データフロー → 状態変化の追跡が確実
- CQRSの「Command」≒「Intent」で概念が一致する
- テストが書きやすい（Intent入力 → UiState出力を検証するだけ）

❌ **デメリット**
- Kotlin初心者には sealed class, when 式の理解が必要
- 単純な画面でもIntent・UiState・Reducerのボイラープレートが必要
- MVVMより学習コストが高い
- Thin Client（APIを呼ぶだけ）には少し重い

---

### ③ モバイル版ヘキサゴナル（バックエンドと統一）

```
Driving Adapter（Composable）
    ↓ Primary Port
Application Core（UseCase）
    ↓ Secondary Port
Driven Adapter（Retrofit / Room）→ Spring Boot
```

**コード例:**
```kotlin
// Primary Port（Input）
interface GetProductsPort {
    suspend fun getProducts(): Result<List<Product>>
}

// Application Service（UseCase）
class GetProductsUseCase(
    private val repository: ProductRepositoryPort  // Secondary Port
) : GetProductsPort {
    override suspend fun getProducts(): Result<List<Product>> {
        return repository.getProducts()   // ← ただRepositoryを呼ぶだけ…
    }
}

// Secondary Port（Output）
interface ProductRepositoryPort {
    suspend fun getProducts(): Result<List<Product>>
}

// Driven Adapter
class ProductRepositoryAdapter(
    private val api: ProductApi
) : ProductRepositoryPort {
    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            Result.success(api.getProducts().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

✅ **メリット**
- バックエンドと設計思想・用語が完全統一
- Port/Adapterの概念をチーム全体で共有できる
- テスト時にAdapterを差し替えやすい

❌ **致命的なデメリット**
- **UseCase層がほぼ空になる**（業務ロジックがバックエンドにあるため）
- 上の `GetProductsUseCase` を見てほしい。Repositoryを呼ぶだけの1行。**この層に存在意義がない**
- Androidの公式ガイド・ライブラリ（Hilt, Navigation等）と噛み合わない
- ヘキサゴナルを知らないAndroidエンジニアには馴染みがない
- 過剰設計。ファイル数が1.5倍〜2倍に増える

---

### ④ シンプル3層（Screen + ViewModel + Repository）

```
Screen（Composable）→ ViewModel → Repository → Retrofit → Spring Boot
                                             → Room
```

**MVVM と何が違うのか？ → ほぼ同じ。ただし「Model」という曖昧な用語を使わない。**

```kotlin
// Screen（表示するだけ）
@Composable
fun ProductListScreen(viewModel: ProductListViewModel) { ... }

// ViewModel（UIの状態管理）
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productQuery: ProductQueryRepository    // ← 参照系
) : ViewModel() { ... }

// Repository Interface（Command/Query分離）
interface ProductQueryRepository {                      // ← 参照系（GET）
    suspend fun getProducts(): Result<List<Product>>
}
interface CartCommandRepository {                       // ← 更新系（POST/DELETE）
    suspend fun addToCart(productId: String): Result<Unit>
}
```

✅ **メリット**
- MVVMの良さを維持しつつ「Model」の曖昧さを排除
- Command/Query分離でバックエンドCQRSと一致
- ヘキサゴナル経験者にも理解しやすい（Repository = Port/Adapter）
- Google公式ガイドと整合性がある
- Thin Client に最適な薄さ

❌ **デメリット**
- 特にない（MVVMとほぼ同じなので、MVVMのデメリットを軽減した形）
- 強いて言えば「MVVM」という広く知られた名前がないため、外部に説明しにくい

---

## 4. 比較表

| 評価軸 | ① MVVM | ② MVI | ③ ヘキサゴナル | ④ シンプル3層 |
|---|:---:|:---:|:---:|:---:|
| **Thin Client適性** | ○ | △ 少し重い | ✕ UseCase空 | ◎ |
| **バックエンド整合性** | △ 用語不一致 | ○ Intent≒Command | ◎ 完全一致 | ○ CQ分離 |
| **学習コスト** | ○ | △ | ✕ | ◎ |
| **Kotlin初心者対応** | ○ | △ | ✕ | ◎ |
| **ボイラープレート量** | 少 | 多 | 最多 | 最少 |
| **状態管理の安全性** | △ | ◎ sealed class | △ | △ |
| **Google公式との整合** | ◎ | ○ | ✕ | ◎ |
| **100画面超の保守性** | ○ | ◎ | ○ | ○ |
| **テスト容易性** | ○ | ◎ | ◎ | ○ |
| **過剰設計リスク** | 低 | 中 | 高 | 最低 |
| **ファイル数（1画面あたり）** | 3〜4 | 4〜5 | 5〜7 | 3〜4 |

---

## 5. 結論

### 推奨: MVVM（View + ViewModel + Model） — Model = Repository（Query/Command分離）

```
基本方針:
  MVVM を採用する。ただし Clean Architecture の UseCase 層は不要。
  Model 層 = Repository で、Query（参照）と Command（更新）を明確に分離する。

  ほとんどの画面 → MVVM（Screen + ViewModel + Repository）
  複雑な画面     → MVVM + MVI 要素（sealed class で状態を型安全に管理）

判断基準:
  画面の状態パターンが3つ以下（Loading / Success / Error）
    → シンプル3層で十分

  画面の状態パターンが4つ以上（Loading / Success / Error / Empty / PartialError / Refreshing ...）
    → MVI を採用（sealed class で漏れなく管理）
```

### 推奨理由

```
① ヘキサゴナルをモバイルに適用しない理由
   → 業務ロジックがバックエンドにある以上、UseCase層が空になる
   → 空のクラスを100個作るのは無駄
   → Androidの公式エコシステム（Hilt, Navigation）と噛み合わない

② MVVM を採用する理由
   → Google公式推奨のアーキテクチャ
   → Model（= Repository）は API呼び出し・キャッシュ・変換で一番忙しい
   → ViewModel は UI状態管理に専念し、業務ロジックは書かない
   → Repository を Query / Command に分離し、バックエンドの CQRS と1:1対応

③ 複雑画面のみ MVI 要素を部分採用する理由
   → カート画面のように状態が4パターン以上になる場合のみ
     sealed class で全状態を列挙して型安全に管理する価値がある
   → 全画面に適用すると過剰設計
```

---

## 6. ヘキサゴナル経験者への翻訳表

チーム内の共通言語として、バックエンドの概念とモバイルの概念の対応表を共有する。

| バックエンド（ヘキサゴナル） | モバイル（シンプル3層） | 説明 |
|---|---|---|
| **Driving Adapter** (Controller) | **Screen** (Composable) | 外部からの入力を受ける |
| **Primary Port** (UseCase IF) | — (不要) | 業務ロジックがないので不要 |
| **Application Service** | **ViewModel** | 処理の調整役 |
| **Domain Model** | **Domain Model** | 同じ。データの入れ物 |
| **Secondary Port** (Repository IF) | **Repository Interface** | データ取得の抽象化 |
| **Driven Adapter** (JPA等) | **Repository Impl** (Retrofit/Room) | 外部システムへの接続 |
| **Command** (POST/PUT/DELETE) | **CommandRepository** | 更新系操作 |
| **Query** (GET) | **QueryRepository** | 参照系操作 |

```
バックエンドエンジニアへの説明:
「モバイルは Driving Adapter（画面）と Driven Adapter（API通信）だけで構成される。
 Application Core（業務ロジック）は存在しない。
 なぜなら業務ロジックは全てバックエンドにあるから。」
```

---

## 7. 具体例：カート画面で比較

### シンプル3層（通常の画面向け）

```kotlin
// UiState: シンプルな data class
data class CartUiState(
    val isLoading: Boolean = false,
    val items: List<CartItem> = emptyList(),
    val error: String? = null
)

// ViewModel: Repository を呼ぶだけ
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartQuery: CartQueryRepository,      // GET = 参照
    private val cartCommand: CartCommandRepository   // POST/DELETE = 更新
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState = _uiState.asStateFlow()

    fun loadCart() { /* cartQuery.getCart() → UiState更新 */ }
    fun removeItem(id: String) { /* cartCommand.remove(id) → loadCart() */ }
}
```

### MVI（複雑な画面向け）

```kotlin
// Intent: ユーザー操作を全て列挙
sealed class CartIntent {
    object LoadCart : CartIntent()
    data class RemoveItem(val id: String) : CartIntent()
    data class UpdateQuantity(val id: String, val qty: Int) : CartIntent()
    data class ApplyCoupon(val code: String) : CartIntent()
    object ProceedToCheckout : CartIntent()
}

// UiState: 画面の全状態を列挙
sealed class CartUiState {
    object Loading : CartUiState()
    data class Success(
        val items: List<CartItem>,
        val totalPrice: String,
        val couponApplied: Boolean,
        val stockWarnings: List<String>
    ) : CartUiState()
    data class Error(val message: String) : CartUiState()
    object Empty : CartUiState()
    object CheckoutReady : CartUiState()
}

// Effect: 1回だけ発生するイベント（スナックバー、画面遷移）
sealed class CartEffect {
    data class ShowSnackbar(val message: String) : CartEffect()
    object NavigateToCheckout : CartEffect()
}

// ViewModel: Intent を受け取り、UiState を更新
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartQuery: CartQueryRepository,
    private val cartCommand: CartCommandRepository
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = ...
    val effect: SharedFlow<CartEffect> = ...

    fun onIntent(intent: CartIntent) {
        when (intent) {
            is CartIntent.LoadCart -> { ... }
            is CartIntent.RemoveItem -> { ... }
            is CartIntent.UpdateQuantity -> { ... }
            is CartIntent.ApplyCoupon -> { ... }
            is CartIntent.ProceedToCheckout -> { ... }
            // ↑ sealed class なので、新しい Intent を追加し忘れるとコンパイルエラー
        }
    }
}
```

### 使い分けの判断

```
商品一覧画面:
  状態 = Loading / Success / Error の3パターン
  操作 = スクロール、タップのみ
  → シンプル3層で十分 ✅

カート画面:
  状態 = Loading / Success / Error / Empty / CheckoutReady の5パターン
  操作 = 削除 / 数量変更 / クーポン適用 / 注文手続き
  副作用 = スナックバー / 画面遷移
  → MVI を採用 ✅

商品詳細画面:
  状態 = Loading / Success / Error + AddedToCart フラグ
  操作 = カートに追加
  → シンプル3層で十分（4パターン目はフラグで対応可） ✅
```

---

*「MVVMか否か」にこだわる必要はない。プロジェクトの実態に合った層構成を選ぶことが重要。*

