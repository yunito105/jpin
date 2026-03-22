# 各層は何をするのか — 全仕事の棚卸し

**疑問**: Model でやることがないと言ったが、API呼び出しやDB格納はどこでやるのか？

**回答**: それをやるのが Model 層（= Repository）。**Model は全然空じゃない。むしろ一番忙しい。**

---

## 誤解の整理

```
「空になる」と言ったのは…
  ✕ Model 層が空         ← これは間違い。Model層はやること山盛り
  ○ UseCase 層が空       ← これが正しい。業務ロジック層が不要という意味

つまり:
  バックエンドのヘキサゴナル → Application Service（業務ロジック）に価値がある
  モバイルのMVVM           → Model（データ取得・保存）に価値がある
                              UseCase（業務ロジック）は不要 ← バックエンドにある
```

---

## 各層の全仕事リスト

### View（Composable）の仕事

```
✅ やること:
  ・画面のレイアウトを描く
  ・UiStateの変化を監視して表示を切り替える（Loading → 商品一覧 → エラー）
  ・ユーザー操作（タップ、スワイプ、入力）を ViewModel に伝える
  ・アニメーション・トランジション
  ・画面遷移（Navigation）

❌ やらないこと:
  ・API を呼ぶ
  ・データを加工する
  ・状態を持つ（ViewModel に任せる）
```

**ファイル数の目安**: 1画面につき1ファイル

---

### ViewModel の仕事

```
✅ やること:
  ・UiState を StateFlow で管理する（Loading / Success / Error）
  ・View からのイベントを受け取る（「カテゴリが選ばれた」「カートに追加ボタンが押された」）
  ・Repository を呼ぶ（Query or Command を選んで依頼する）
  ・Repository の結果を UiState に変換する
  ・Coroutine のスコープ管理（viewModelScope）
  ・画面回転時のデータ保持（ViewModel は画面回転で破棄されない）

❌ やらないこと:
  ・API を直接呼ぶ（Repository に任せる）
  ・SQLを書く（Repository に任せる）
  ・業務ロジック（バックエンドに任せる）
  ・画面のレイアウト（View に任せる）
```

**ファイル数の目安**: 1画面につき1ファイル

---

### Model（= Repository）の仕事 ← ★ ここが一番忙しい

```
✅ やること:
  ┌──────────────────────────────────────────────────────┐
  │ 1. API呼び出し                                       │
  │    ・Retrofit で Spring Boot API を呼ぶ              │
  │    ・GET /api/v1/products（Query）                   │
  │    ・POST /api/v1/cart/items（Command）              │
  │    ・認証トークンをヘッダーに付与                     │
  │    ・Accept-Language ヘッダー付与（多言語対応）       │
  ├──────────────────────────────────────────────────────┤
  │ 2. レスポンス変換                                    │
  │    ・JSON → DTO（ProductDto）                        │
  │    ・DTO → ドメインモデル（Product）                 │
  │    ・snake_case → camelCase のマッピング             │
  ├──────────────────────────────────────────────────────┤
  │ 3. ローカルDB保存（Room）                            │
  │    ・API レスポンスを Room にキャッシュ保存           │
  │    ・テーブル設計（Entity定義）                       │
  │    ・DAO（CRUD操作）の実装                           │
  ├──────────────────────────────────────────────────────┤
  │ 4. オフライン対応                                    │
  │    ・API 失敗時に Room のキャッシュから返す           │
  │    ・オフライン中の操作をキューに保存                 │
  │    ・ネットワーク復帰時に自動同期（WorkManager）     │
  ├──────────────────────────────────────────────────────┤
  │ 5. エラーハンドリング                                │
  │    ・HTTP 401 → 再認証フロー                         │
  │    ・HTTP 404 → 商品が見つからない                   │
  │    ・HTTP 409 → 在庫切れ（バックエンドが判定済み）   │
  │    ・タイムアウト → リトライ or キャッシュフォールバック│
  ├──────────────────────────────────────────────────────┤
  │ 6. キャッシュ戦略                                    │
  │    ・いつキャッシュを使うか判断                       │
  │    ・キャッシュの有効期限管理                         │
  │    ・API → キャッシュ更新のフロー制御                │
  ├──────────────────────────────────────────────────────┤
  │ 7. DataStore（設定値保存）                           │
  │    ・言語設定の保存・読み出し                         │
  │    ・通貨設定の保存・読み出し                         │
  │    ・ログイン状態の保持                               │
  └──────────────────────────────────────────────────────┘

❌ やらないこと:
  ・価格計算（バックエンドの仕事）
  ・在庫引当ロジック（バックエンドの仕事）
  ・注文バリデーション（バックエンドの仕事）
  ・UI状態の管理（ViewModel の仕事）
  ・画面表示（View の仕事）
```

**ファイル数の目安**: 
- Repository Interface: 1ドメインにつき1〜2ファイル
- Repository Implementation: 1ドメインにつき1〜2ファイル
- API Interface: 1ドメインにつき1ファイル
- DTO: 1エンドポイントにつき1ファイル
- Room Entity: 1テーブルにつき1ファイル
- Room DAO: 1テーブルにつき1ファイル

---

## 具体例：商品一覧画面のデータ取得フロー

```kotlin
// ═══════════════════════════════════════════════
// View（Composable）の仕事: 表示するだけ
// ═══════════════════════════════════════════════
@Composable
fun ProductListScreen(viewModel: ProductListViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingSpinner()        // ← 表示するだけ
        uiState.error != null -> ErrorMessage(...)    // ← 表示するだけ
        else -> ProductGrid(uiState.products)         // ← 表示するだけ
    }
}


// ═══════════════════════════════════════════════
// ViewModel の仕事: 状態管理 + Repository に依頼
// ═══════════════════════════════════════════════
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductQueryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState = _uiState.asStateFlow()

    init { loadProducts() }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }   // ← 状態管理

            repository.getProducts()                          // ← Repository に「商品くれ」と依頼
                .onSuccess { products ->                      //    （どうやって取るかは知らない）
                    _uiState.update {
                        it.copy(isLoading = false, products = products)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }
}


// ═══════════════════════════════════════════════
// Model（Repository）の仕事: ここが一番忙しい
// ═══════════════════════════════════════════════
class ProductQueryRepositoryImpl @Inject constructor(
    private val api: ProductApi,       // Retrofit（API通信）
    private val dao: ProductDao,       // Room（ローカルDB）
    private val prefs: UserPreferences // DataStore（設定値）
) : ProductQueryRepository {

    override suspend fun getProducts(categoryId: String?): Result<List<Product>> {
        return try {
            // ① 設定から言語情報を取得
            val language = prefs.getLanguage()    // ← DataStore から読み出し

            // ② API を呼ぶ（Spring Boot の QueryHandler に対応）
            val response = api.getProducts(       // ← Retrofit で HTTP リクエスト
                categoryId = categoryId,
                language = language                // ← Accept-Language ヘッダー付与
            )

            // ③ DTO → Entity に変換して Room に保存（キャッシュ）
            val entities = response.map { dto ->  // ← JSON → DTO → Entity 変換
                dto.toEntity()
            }
            dao.insertAll(entities)                // ← Room にキャッシュ保存

            // ④ DTO → ドメインモデルに変換して返す
            val products = response.map { dto ->  // ← DTO → Product 変換
                dto.toDomain()
            }
            Result.success(products)

        } catch (e: HttpException) {
            // ⑤ HTTPエラーのハンドリング
            when (e.code()) {
                401 -> Result.failure(AuthException("再ログインが必要です"))
                500 -> {
                    // サーバーエラー → キャッシュから返す
                    val cached = dao.getAll().map { it.toDomain() }
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(e)
                }
                else -> Result.failure(e)
            }

        } catch (e: IOException) {
            // ⑥ ネットワークエラー（オフライン）→ キャッシュから返す
            val cached = if (categoryId != null) {
                dao.getByCategory(categoryId)     // ← Room からキャッシュ読み出し
            } else {
                dao.getAll()
            }
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }
}
```

---

## まとめ：仕事量の比較

```
┌─────────────┬──────────────────────────────────┬──────────┐
│    層        │ やること                         │ 仕事量   │
├─────────────┼──────────────────────────────────┼──────────┤
│ View        │ 表示する。操作を伝える            │ ★★☆☆☆  │
├─────────────┼──────────────────────────────────┼──────────┤
│ ViewModel   │ 状態管理。Repository に依頼する    │ ★★★☆☆  │
├─────────────┼──────────────────────────────────┼──────────┤
│ Model       │ API呼び出し、DB保存、キャッシュ、 │ ★★★★★  │
│ (Repository)│ エラーハンドリング、変換、         │ 一番忙しい│
│             │ オフライン対応、認証トークン管理   │          │
├─────────────┼──────────────────────────────────┼──────────┤
│ UseCase     │（バックエンドにあるので不要）      │ なし     │
│（業務ロジック）│                                │          │
└─────────────┴──────────────────────────────────┴──────────┘
```

結論:
  MVVM の M（Model）= Repository。やることは山ほどある。
  「空」なのは Clean Architecture の UseCase 層だけ。
  だから UseCase 層を省略して View / ViewModel / Repository の3層にする。
  Repository は Query（参照 = GET）と Command（更新 = POST/DELETE）に分離する。
  これがこのプロジェクトにおける MVVM の正しい姿。

```
                 MVVM アーキテクチャ（最終形）
┌───────────────────────────────────────────────────────────┐
│  View（Composable）                                       │
│  表示するだけ。ロジックなし。                              │
└──────────────────┬────────────────────────────────────────┘
                   │ UiState / イベント
┌──────────────────▼────────────────────────────────────────┐
│  ViewModel                                                │
│  UI状態管理。Repository に依頼するだけ。                    │
└─────────┬────────────────────────┬────────────────────────┘
          │                        │
┌─────────▼──────────┐    ┌────────▼───────────┐
│  QueryRepository   │    │  CommandRepository │
│  （参照系 = GET）  │    │  （更新系 = POST/  │
│                    │    │   DELETE）          │
│  商品一覧取得      │    │  カート追加         │
│  商品詳細取得      │    │  カート削除         │
│  カート取得        │    │  注文送信           │
│                    │    │                    │
│  + Room キャッシュ │    │                    │
│  + オフライン対応  │    │                    │
└─────────┬──────────┘    └────────┬───────────┘
          │                        │
          └──────────┬─────────────┘
                     │ HTTP
              Spring Boot API
             （業務ロジックはここ）
```

