# モバイルシステム方針書
**プロジェクト**: 海外販売システム（25億円規模）  
**業種**: 小売業（IKEA・ニトリ・カインズ類似業態）  
**作成目的**: テックリードによるアーキテクチャ選定・リーダーへの提案資料  
**対象プラットフォーム**: Android（Kotlin）

---

## ✅ 前提：モバイルの責務を明確にする

> **業務ロジックはすべてバックエンド（Spring Boot）が担う。**  
> モバイルは「受け取って表示する」ことに集中する。

```
【バックエンドの責務】              【モバイルの責務】
─────────────────────────────────────────────────────
在庫計算                           API呼び出し
価格計算・割引適用                 レスポンスを画面用に変換
注文バリデーション                 UI状態（ローディング/エラー/成功）管理
在庫引当ロジック                   画面表示・ユーザー操作のハンドリング
多通貨換算                         ローカルキャッシュ（オフライン対応）
```

**→ モバイルは「薄いクライアント（Thin Client）」として設計する。**

---

## 目次
1. [MVVMをゼロから理解する](#1-mvvmをゼロから理解する)
2. [モバイルアーキテクチャ候補パターン](#2-モバイルアーキテクチャ候補パターン)
3. [バックエンドとの整合性（ヘキサゴナル + 簡易CQRS）](#3-バックエンドとの整合性)
4. [小売業特有の考慮事項](#4-小売業特有の考慮事項)
5. [比較表と最終推奨パターン](#5-比較表と最終推奨パターン)
6. [モバイル開発チーム体制の提案](#6-モバイル開発チーム体制の提案)
7. [Figma共通コンポーネントのモバイル利用における注意点](#7-figma共通コンポーネントのモバイル利用における注意点)

---

## 1. MVVMをゼロから理解する

### 1-1. MVVM の3つの登場人物

| 役割 | 名前 | 仕事 | 例 |
|---|---|---|---|
| **V** | View | 表示するだけ。ロジックは持たない | Activity / Fragment / Composable |
| **VM** | ViewModel | ViewとModelの橋渡し。UI状態を管理 | ProductViewModel |
| **M** | Model | データの取得・保存 | Retrofit(API) / Room(DB) |

### 1-2. データの流れ（一方通行が基本）

```
  ユーザーが操作
       │
       ▼
┌─────────────────────────────────────────────────┐
│  View（Composable）                             │
│  ・画面を描画する                               │
│  ・ユーザー操作をViewModelに伝える              │
│  ・UiStateを受け取って表示するだけ              │
└──────────┬──────────────────▲───────────────────┘
           │ イベント通知      │ UiState（表示データ）
           ▼                  │
┌──────────────────────────────────────────────────┐
│  ViewModel                                      │
│  ・UiStateを StateFlow で持つ                   │
│  ・Viewからイベントを受け取りAPIを呼ぶ          │
│  ・APIの結果をUiStateに変換してViewに渡す       │
│                                                 │
│  ※業務ロジックは書かない（バックエンドに任せる）│
└──────────┬──────────────────▲───────────────────┘
           │ APIリクエスト     │ APIレスポンス
           ▼                  │
┌──────────────────────────────────────────────────┐
│  Model（Repository）                            │
│  ・Retrofitでバックエンドを呼ぶ                 │
│  ・必要に応じてRoomにキャッシュする             │
│  ・データを返すだけ。ロジックは持たない         │
└──────────────────────────────────────────────────┘
           │
           ▼
    Spring Boot API
    （業務ロジックはここ）
```

### 1-3. 具体例：商品一覧画面

```kotlin
// ① UiState（画面の状態を型で表現）
data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)

// ② ViewModel（橋渡し役）
class ProductListViewModel(
    private val repository: ProductRepository   // Model
) : ViewModel() {

    // Viewが監視する状態
    val uiState: StateFlow<ProductListUiState> = ...

    // Viewからのイベント受付
    fun onCategorySelected(categoryId: String) {
        viewModelScope.launch {
            // ローディング表示
            _uiState.update { it.copy(isLoading = true) }

            // APIを呼ぶだけ（業務ロジックはバックエンド側）
            repository.getProducts(categoryId)
                .onSuccess { products ->
                    _uiState.update { it.copy(isLoading = false, products = products) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}

// ③ View（表示するだけ）
@Composable
fun ProductListScreen(viewModel: ProductListViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> LoadingSpinner()
        uiState.errorMessage != null -> ErrorMessage(uiState.errorMessage)
        else -> ProductGrid(uiState.products)
    }
}
```

---

## 2. モバイルアーキテクチャ候補パターン

> **前提変更後の注意点**  
> 業務ロジックがバックエンドにある場合、Domain層（UseCase）は大幅に薄くなるか、省略可能になります。

---

### パターン① MVVM（シンプル構成）

#### 構成図

```
┌──────────────────────────────────────────┐
│             UI Layer                     │
│   Composable（表示・操作イベント送信）   │
└─────────────────┬────────────────────────┘
                  │ 操作イベント / UiState購読
┌─────────────────▼────────────────────────┐
│           ViewModel Layer                │
│   UiState管理 / APIコール / エラーハンドリング │
└─────────────────┬────────────────────────┘
                  │ データ取得
┌─────────────────▼────────────────────────┐
│           Repository Layer               │
│   Remote: Retrofit（バックエンドAPI）    │
│   Local : Room（オフラインキャッシュ）   │
└──────────────────────────────────────────┘
                  │ HTTP
         Spring Boot API（業務ロジック）
```

#### 特徴
- **層が少なくシンプル** → チームへの説明・実装が最も容易
- ViewModel が肥大化する場合は「画面ごとにViewModelを分ける」で対応
- 業務ロジックをモバイルに書かない前提なら、UseCase層は不要

#### ✅ メリット
- 学習コストが最も低い（Java経験者でも2週間以内に慣れる）
- Google公式のサンプルコードがそのまま使える
- シンプルな構造のため、レビューコストが低い

#### ❌ デメリット
- 複雑な画面状態（複数APIの組み合わせ）で ViewModel が大きくなりやすい
- 状態変化のバグが起きた時に原因追跡がやや難しい

---

### パターン② MVI（状態管理強化版）

#### 構成図

```
┌──────────────────────────────────────────┐
│             UI Layer                     │
│   Composable                            │
│   ・Intent（ユーザー操作）を送る        │
│   ・UiState（sealed class）を受け取る   │
└──────────┬───────────────────▲───────────┘
           │ Intent            │ UiState
┌───────────▼───────────────────┴───────────┐
│           ViewModel（MVI Store）         │
│                                          │
│   Intent受信                            │
│      ↓                                  │
│   APIコール（Repositoryへ）             │
│      ↓                                  │
│   Reducer（現在State + 結果 → 新State） │
│      ↓                                  │
│   新UiStateをViewに流す                 │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│           Repository Layer               │
│   Remote: Retrofit / Local: Room         │
└──────────────────────────────────────────┘
                  │ HTTP
         Spring Boot API（業務ロジック）
```

#### UiStateの表現例（在庫・カート複合状態）

```kotlin
sealed class CartUiState {
    object Loading : CartUiState()
    data class Success(
        val items: List<CartItem>,
        val totalPrice: String,          // バックエンドが計算済みの金額を受け取るだけ
        val stockWarnings: List<String>  // バックエンドが判定済みの警告を受け取るだけ
    ) : CartUiState()
    data class Error(val message: String) : CartUiState()
    object Empty : CartUiState()
}
```

> **ポイント**: `totalPrice` や `stockWarnings` はモバイルが計算するのではなく、  
> バックエンドが計算した結果をそのまま受け取って表示するだけ。

#### ✅ メリット
- `sealed class` で画面の状態を漏れなく型で管理できる
- バグが起きた時に「どのStateの時に起きたか」が明確でデバッグしやすい
- 在庫・カート・エラーが重なる複雑な画面状態に強い

#### ❌ デメリット
- MVVMより学習コストが高い（Intent/State/Effectの概念習得が必要）
- 単純な画面には記述量が多くなる

---

### パターン③ MVVM + Repository（本プロジェクトへの最適解）

#### 構成図

```
┌───────────────────────────────────────────────────────┐
│  UI Layer（Jetpack Compose）                          │
│  ・画面表示のみ                                       │
│  ・ユーザー操作をViewModelに転送                      │
└────────────────────────────┬───────────────────────────┘
                             │ 参照（実装は別）
              ┌──────────────┴──────────────┐
              ▼                             ▼
┌─────────────────────────┐   ┌──────────────────────────────┐
│  Webコンポーネント      │   │  Android Compose Component   │
│  （Next.js / React）    │   │  Library                     │
│                         │   │                              │
│  AppButton.tsx          │   │  AppButton.kt（Composable）  │
│  AppCard.tsx            │   │  AppCard.kt                  │
│  AppTextField.tsx       │   │  AppTextField.kt             │
└─────────────────────────┘   └──────────────────────────────┘
         ↑                                ↑
   デザイントークン共有（Style Dictionary で自動生成）
```

---

## 3. バックエンドとの整合性

### バックエンド（Spring Boot + ヘキサゴナル + 簡易CQRS）との接続

```
【モバイル（Kotlin）】                    【バックエンド（Spring Boot）】

ViewModel
  │
  ├── CommandRepository ─── POST /api/cart/add    ──► CommandHandler
  │                    ─── POST /api/orders       ──► CommandHandler
  │                                                    ↓ Oracle DB（書込）
  │
  └── QueryRepository  ─── GET /api/products      ──► QueryHandler
                       ─── GET /api/inventory     ──► QueryHandler
                                                       ↓ Oracle DB（読込）

                            WebSocket / SSE ◄─── 在庫変動・セール開始通知
```

### Repositoryの実装例

```kotlin
// Query（参照）Repository
interface ProductQueryRepository {
    suspend fun getProducts(categoryId: String): Result<List<Product>>
    suspend fun getProductDetail(productId: String): Result<ProductDetail>
}

// Command（更新）Repository
interface CartCommandRepository {
    suspend fun addToCart(productId: String, quantity: Int): Result<Unit>
    suspend fun removeFromCart(cartItemId: String): Result<Unit>
}

// ViewModel での呼び分け
class CartViewModel(
    private val query: ProductQueryRepository,   // 参照
    private val command: CartCommandRepository   // 更新
) : ViewModel() {
    // ...
}
```

---

## 4. 小売業特有の考慮事項

### 4-1. 在庫リアルタイム同期

```
倉庫システム → Spring Boot → WebSocket/SSE → モバイル
                                                  ↓
                                         StateFlow で即時UI反映
                                         "残り3点！" バッジ更新
```
- `WebSocket` または `SSE` で受信 → `StateFlow` で即時反映
- 在庫情報は `Room` にキャッシュ（オフライン時も表示可能）

---

### 4-2. オフライン対応

```
オンライン時: API → Repository → Room（キャッシュ）→ UI
オフライン時: Room（キャッシュ）→ UI
             ↓ 操作はPending Queueに保存
             ↓ 復帰後に WorkManager で自動同期
```

---

### 4-3. 多言語・多通貨対応

| 対応内容 | 実装方法 |
|---|---|
| 多言語 | `strings.xml` + APIリクエストに `Accept-Language` ヘッダー付与 |
| 多通貨 | **バックエンドが変換済み金額を返す**（モバイルは表示のみ） |
| RTL対応 | `android:supportsRtl="true"` + Compose の `LocalLayoutDirection` |
| 日付 | `java.time.DateTimeFormatter.ofLocalizedDate()` |

---

### 4-4. バーコード・QRスキャン

```
CameraX → ML Kit Barcode Scanning → 商品コード取得
    → QueryRepository.getProductByBarcode() → 商品詳細表示
```

---

### 4-5. プッシュ通知

```
Spring Boot → FCM → モバイル
                       ├── SALE_START   → セール画面へ
                       ├── STOCK_ALERT  → 商品詳細へ
                       └── ORDER_UPDATE → 注文状況へ
```

---

## 5. 比較表と最終推奨パターン

### 5-1. 比較表（業務ロジックはバックエンド前提）

| 評価軸 | ① MVVM シンプル | ② MVI | ③ MVVM+Repository（推奨） |
|---|:---:|:---:|:---:|
| **学習コスト** | ⭐⭐⭐⭐⭐（最低） | ⭐⭐⭐（中） | ⭐⭐⭐⭐（低） |
| **保守性** | ⭐⭐⭐（中） | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐（高） |
| **バックエンドCQRS整合性** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐（最高） |
| **Kotlin初心者対応** | ◎ | △ | ◎ |
| **複雑UI状態管理** | △ | ◎ | ○ |
| **業界実績** | ◎ | ○ | ◎ |
| **過剰設計リスク** | 低 | 中 | 低 |
| **薄いクライアント適性** | ○ | △（少し重厚） | ◎ |

---

### 5-2. 最終推奨：**MVVM（View + ViewModel + Model）— Model = Repository（Query/Command分離）**

#### 推奨理由

```
① 業務ロジックはバックエンド完結
   → UseCase 層は不要。Model 層（= Repository）が API・DB・キャッシュ・変換を担う
   → Model 層は「空」ではない。むしろ一番忙しい層

② CQRS と1:1対応
   → QueryRepository（GET） / CommandRepository（POST/DELETE）で分離
   → ヘキサゴナル経験者には「Repository = Port/Adapter」と説明可能

③ Google公式推奨のアーキテクチャ
   → Hilt（@HiltViewModel）, Navigation Compose, StateFlow 等すべて公式対応

④ 複雑画面のみ MVI 要素を部分採用
   → 状態が3パターン以下（Loading/Success/Error）→ 通常のMVVM
   → 状態が4パターン以上（カート等）→ sealed class で漏れなく管理
```

> **📎 詳細比較**: `docs/architecture-comparison.md` に  
> ヘキサゴナルとの概念マッピング・4パターン忖度なし比較・実コードでの使い分け例を記載

#### 段階的導入ロードマップ

```
Phase 1（0〜3ヶ月）
  ├── Kotlin基礎研修（Coroutines / Flow / StateFlow）
  ├── シンプルなMVVM + Repositoryでスケルトンアプリ作成
  └── バックエンドAPIとの疎通確認

Phase 2（3〜6ヶ月）
  ├── Command/QueryリポジトリをCQRS対応で実装
  ├── Room によるオフラインキャッシュ実装
  └── Jetpack Compose で画面実装

Phase 3（6ヶ月〜）
  ├── 複雑な画面（カート・在庫）はMVI的なStateFlowに移行検討
  ├── WebSocket/SSE によるリアルタイム同期実装
  └── CI/CD（Detekt + GitHub Actions）整備
```

---

## 6. モバイル開発チーム体制の提案

### 6-1. 必要なロール

| ロール | 人数 | 担当内容 |
|---|:---:|---|
| Androidアーキテクト | 1名 | 設計方針・スケルトンアプリ作成・レビュー |
| Androidシニアエンジニア | 2名 | コア機能実装・ジュニア指導 |
| Androidエンジニア | 3〜4名 | 機能開発（Java経験者でも担当可） |
| QAエンジニア | 1〜2名 | UI自動テスト（Compose UI Test） |

### 6-2. Kotlin初心者リスクと対策

| リスク | 対策 |
|---|---|
| Coroutines誤用（非同期バグ） | 研修 + Detektで静的検出 |
| Layer汚染（ViewにAPI処理を書く） | アーキテクト作成のスケルトンアプリを配布 |
| StateFlowの購読漏れ | コードレビューのチェックリスト化 |

```
Java経験者の学習パス：
  Week 1: Kotlinの基本文法（Null安全・data class・拡張関数）
  Week 2: Coroutines / Flow / StateFlow
  Week 3: Retrofit + Room の実装（Data層から入門）
  Week 4〜: ViewModelの実装・Composeの基礎
```

---

## 付録：推奨技術スタック

> **選定方針**: 各カテゴリで複数候補を比較し、本プロジェクトのドメイン（小売業・海外販売・25億規模）に  
> 最適なものを選定する。Google公式推奨がある場合はその根拠リンクを明示する。

---

### A. UI フレームワーク

| 候補 | 概要 |
|---|---|
| **Jetpack Compose** | 宣言的UIフレームワーク。Kotlinのみで画面を構築 |
| **XML Layout + View** | 従来のAndroid UI。XMLでレイアウトを定義 |
| **Flutter（Dart）** | Google製クロスプラットフォーム。iOS/Android共通 |

| 評価軸 | Jetpack Compose | XML Layout | Flutter |
|---|:---:|:---:|:---:|
| Google推奨度 | ◎ **公式推奨** | △ レガシー扱い | ○ 別プロダクト |
| 学習コスト | ○ Kotlin必須 | ◎ 実績豊富 | △ Dart習得必要 |
| 開発速度 | ◎ コード量少 | △ XML+コード二重管理 | ◎ ホットリロード |
| パフォーマンス | ◎ ネイティブ | ◎ ネイティブ | ○ 準ネイティブ |
| 人材確保 | ○ 増加中 | ◎ 最も多い | △ Dart人材少 |
| バーコードスキャン対応 | ◎ CameraX直接連携 | ◎ CameraX直接連携 | △ プラグイン依存 |
| 既存バックエンド連携 | ◎ Kotlin型安全 | ○ | △ Dart↔Java変換必要 |

**🏆 採用: Jetpack Compose**

> **根拠**:  
> - Google公式: [Jetpack Compose is Android's recommended modern toolkit for building native UI](https://developer.android.com/develop/ui/compose)  
> - Google公式: [Views are no longer recommended](https://developer.android.com/develop/ui/compose/migrate) — 新規開発にはComposeを推奨し、XMLからの移行ガイドも提供  
> - [Google I/O 2023: Compose の採用率が上位1000アプリの24%に到達](https://android-developers.googleblog.com/2023/05/whats-new-in-jetpack-compose.html)
>
> **ドメイン観点**: 商品一覧・カート・在庫バッジなど動的なUI更新が多い小売アプリでは、  
> 宣言的UIの「状態が変わったら自動で再描画」が非常に適している。

---

### B. DI（依存性注入）フレームワーク

| 候補 | 概要 |
|---|---|
| **Hilt** | Dagger上に構築されたAndroid公式DI |
| **Koin** | Kotlin DSLベースの軽量DI |
| **Dagger2** | Googleが開発したコンパイル時DIフレームワーク |
| **手動DI** | フレームワークを使わず自分でインスタンスを渡す |

| 評価軸 | Hilt | Koin | Dagger2 | 手動DI |
|---|:---:|:---:|:---:|:---:|
| Google推奨度 | ◎ **公式推奨** | △ | ○ | △ |
| 学習コスト | ○ | ◎ 最も簡単 | ✕ 非常に難しい | ◎ |
| コンパイル時エラー検出 | ◎ | ✕ 実行時エラー | ◎ | ◎ |
| 大規模プロジェクト適性 | ◎ | △ 規模拡大で管理困難 | ◎ | ✕ |
| ViewModel連携 | ◎ @HiltViewModel | ○ | △ 手動設定 | ✕ |
| マルチモジュール対応 | ◎ | ○ | ◎ | ✕ |

**🏆 採用: Hilt**

> **根拠**:  
> - Google公式: [Hilt is recommended for dependency injection on Android](https://developer.android.com/training/dependency-injection/hilt-android)  
> - Google公式: [MAD（Modern Android Development）ガイドでHiltを推奨](https://developer.android.com/series/mad-skills)  
>
> **ドメイン観点**: 25億規模のプロジェクトではモジュール数が多くなる。  
> Koinは小規模向けで実行時エラーのリスクがある。Hiltはコンパイル時に依存関係の不整合を検出でき、大規模に適する。

---

### C. 非同期処理

| 候補 | 概要 |
|---|---|
| **Kotlin Coroutines + Flow** | Kotlin公式の非同期フレームワーク |
| **RxJava / RxKotlin** | ReactiveX ベースのリアクティブプログラミング |
| **LiveData** | Android Architecture Components の監視可能データホルダー |

| 評価軸 | Coroutines + Flow | RxJava | LiveData |
|---|:---:|:---:|:---:|
| Google推奨度 | ◎ **公式推奨** | △ 非推奨化の流れ | △ Flow移行を推奨 |
| 学習コスト | ○ | ✕ 非常に難しい | ◎ 簡単 |
| メモリリーク耐性 | ◎ 構造化並行性 | △ dispose忘れリスク | ◎ ライフサイクル対応 |
| 在庫リアルタイム更新 | ◎ Flow + WebSocket | ◎ | △ 複雑なストリーム不向き |
| テスト容易性 | ◎ Turbine | ○ | △ |
| Compose連携 | ◎ collectAsState | △ 変換必要 | ○ observeAsState |

**🏆 採用: Kotlin Coroutines + Flow**

> **根拠**:  
> - Google公式: [Use Kotlin coroutines with lifecycle-aware components](https://developer.android.com/topic/libraries/architecture/coroutines)  
> - Google公式: [StateFlow and SharedFlow — recommended for emitting state updates](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)  
> - Google公式: [LiveData → Flow 移行ガイド](https://developer.android.com/topic/libraries/architecture/livedata#livedata-in-coroutines) — 新規開発にはFlowを推奨  
>
> **ドメイン観点**: 在庫のリアルタイム更新（WebSocket → Flow → UI）や、  
> 複数APIの並列呼び出し（商品+在庫+レビュー）が多い小売ドメインでは、Flowの合成力が必須。

---

### D. API通信

| 候補 | 概要 |
|---|---|
| **Retrofit2 + OkHttp3** | Square製の型安全HTTPクライアント |
| **Ktor Client** | JetBrains製のKotlin-native HTTPクライアント |
| **Volley** | Google製の軽量HTTPライブラリ（古い） |

| 評価軸 | Retrofit2 + OkHttp | Ktor Client | Volley |
|---|:---:|:---:|:---:|
| 業界標準度 | ◎ **デファクトスタンダード** | ○ 成長中 | ✕ レガシー |
| 学習コスト | ◎ 資料豊富 | ○ | ○ |
| Coroutines対応 | ◎ suspend関数対応 | ◎ ネイティブ対応 | ✕ |
| インターセプター（認証/ログ） | ◎ OkHttp Interceptor | ○ Plugin | △ |
| JSONパーサー選択肢 | ◎ Gson/Moshi/KotlinX | ○ KotlinX | △ Gson |
| WebSocket対応 | ◎ OkHttp WebSocket | ◎ | ✕ |

**🏆 採用: Retrofit2 + OkHttp3**

> **根拠**:  
> - Android公式サンプル [Now in Android](https://github.com/android/nowinandroid) で Retrofit を採用  
> - [Square公式: Retrofit](https://square.github.io/retrofit/)  
> - Stack Overflow Developer Survey で Android HTTP クライアントの最多利用  
>
> **ドメイン観点**: バックエンドのCQRS（POST=Command / GET=Query）を  
> Retrofitのアノテーション（`@POST` / `@GET`）でそのまま表現できる。  
> OkHttp Interceptor で認証トークン・多言語ヘッダーの自動付与も容易。

---

### E. ローカルDB

| 候補 | 概要 |
|---|---|
| **Room** | Google公式のSQLite抽象化ライブラリ |
| **Realm** | MongoDB製のモバイルDB |
| **SQLDelight** | Square製のKotlinマルチプラットフォームDB |
| **DataStore** | SharedPreferences後継のKey-Valueストア |

| 評価軸 | Room | Realm | SQLDelight | DataStore |
|---|:---:|:---:|:---:|:---:|
| Google推奨度 | ◎ **公式推奨** | △ | △ | ◎（KV用途のみ） |
| 学習コスト | ◎ SQL知識活用可 | ○ | ○ | ◎ |
| Flow/Coroutines連携 | ◎ | ○ | ◎ | ◎ |
| 大量データ対応 | ◎ SQLベース | ◎ | ◎ | ✕ |
| マイグレーション | ◎ 自動+手動 | ○ | ○ | ─ |
| オフラインキャッシュ適性 | ◎ | ◎ | ○ | ✕ |

**🏆 採用: Room（メインDB）+ DataStore（設定値保存）**

> **根拠**:  
> - Google公式: [Save data in a local database using Room](https://developer.android.com/training/data-storage/room)  
> - Google公式: [Room is the recommended way to persist data](https://developer.android.com/jetpack/androidx/releases/room)  
> - Google公式: [DataStore — replacement for SharedPreferences](https://developer.android.com/topic/libraries/architecture/datastore)  
>
> **ドメイン観点**: 商品マスタ（数万件）・在庫スナップショット・オフライン操作キューの保存にはRDBが必要。  
> 言語設定・通貨設定などのKey-Value保存にはDataStoreを使い分ける。

---

### F. 画像読み込み

| 候補 | 概要 |
|---|---|
| **Coil** | Kotlin-first の画像読み込みライブラリ |
| **Glide** | Google推奨の画像読み込みライブラリ |
| **Picasso** | Square製の軽量画像ライブラリ（メンテ停滞） |

| 評価軸 | Coil | Glide | Picasso |
|---|:---:|:---:|:---:|
| Compose対応 | ◎ **ネイティブ対応** | ○ 追加ライブラリ必要 | ✕ |
| Kotlin対応 | ◎ Kotlin-first | ○ Java製 | ○ Java製 |
| キャッシュ機能 | ◎ ディスク+メモリ | ◎ | ○ |
| 軽量さ | ◎ | △ APKサイズ増 | ◎ |
| Coroutines連携 | ◎ | △ | ✕ |

**🏆 採用: Coil**

> **根拠**:  
> - Google公式サンプル [Now in Android](https://github.com/android/nowinandroid) で Coil を採用  
> - [Coil公式: Jetpack Compose 対応](https://coil-kt.github.io/coil/compose/)  
>
> **ドメイン観点**: 商品画像の大量表示（一覧画面で50〜100枚）が必須の小売アプリでは、  
> 効率的なキャッシュ+Compose連携が重要。Coilはメモリ効率も優れている。

---

### G. プッシュ通知

| 候補 | 概要 |
|---|---|
| **Firebase Cloud Messaging (FCM)** | Google公式のプッシュ通知サービス |
| **Amazon SNS** | AWS製のプッシュ通知サービス |
| **OneSignal** | サードパーティ製通知プラットフォーム |
| **自前WebSocket** | 独自のリアルタイム通信 |

| 評価軸 | FCM | Amazon SNS | OneSignal | 自前WebSocket |
|---|:---:|:---:|:---:|:---:|
| Android対応 | ◎ **OS統合** | ○ SDK必要 | ○ SDK必要 | △ 常時接続必要 |
| 無料枠 | ◎ 無制限 | ○ 100万件/月 | ○ 一部無料 | ─ |
| トピック購読 | ◎ | ○ | ◎ | ✕ 自前実装 |
| バッテリー消費 | ◎ OS最適化 | ○ | ○ | ✕ 高い |
| 運用コスト | ◎ | ○ | △ 有料プラン | ✕ サーバー維持 |

**🏆 採用: Firebase Cloud Messaging (FCM)**

> **根拠**:  
> - Google公式: [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)  
> - Androidデバイスへのプッシュ通知はFCMが**唯一のOS統合パス**（GCMは廃止済み）  
>
> **ドメイン観点**: セール開始通知・在庫切れ通知・配送状況更新を  
> トピック購読（`sale_JP`, `stock_alert`）で効率的に配信。バッテリー消費もOS最適化済み。

---

### H. バーコード・QRスキャン

| 候補 | 概要 |
|---|---|
| **ML Kit Barcode Scanning** | Google製のオンデバイスバーコード認識 |
| **ZXing** | OSSのバーコードライブラリ（古い） |
| **Scandit** | 商用のバーコードSDK（有料） |

| 評価軸 | ML Kit | ZXing | Scandit |
|---|:---:|:---:|:---:|
| 精度 | ◎ ML ベース | ○ | ◎ 最高精度 |
| オフライン対応 | ◎ オンデバイス | ◎ | ◎ |
| 対応フォーマット | ◎ 1D/2D全種 | ○ | ◎ |
| コスト | ◎ 無料 | ◎ 無料 | ✕ 高額ライセンス |
| CameraX連携 | ◎ | △ 自前実装 | ○ |
| メンテナンス状況 | ◎ Google保守 | △ 更新停滞 | ◎ |

**🏆 採用: ML Kit Barcode Scanning**

> **根拠**:  
> - Google公式: [Scan barcodes with ML Kit on Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android)  
> - Google公式: [CameraX + ML Kit 連携ガイド](https://developer.android.com/media/camera/camerax/mlkitanalyzer)  
>
> **ドメイン観点**: 店舗スタッフの在庫確認・顧客のセルフ検索で利用。  
> 商用ライセンス不要 + オフライン動作で店舗Wi-Fi不安定時にも対応可能。

---

### I. バックグラウンド処理

| 候補 | 概要 |
|---|---|
| **WorkManager** | Android公式のバックグラウンドタスクスケジューラ |
| **AlarmManager** | OS標準のアラームAPI（低レベル） |
| **自前Service** | 自前でバックグラウンドServiceを実装 |

| 評価軸 | WorkManager | AlarmManager | 自前Service |
|---|:---:|:---:|:---:|
| Google推奨度 | ◎ **公式推奨** | △ | ✕ |
| バッテリー最適化 | ◎ Doze対応 | △ | ✕ 制限される |
| リトライ機能 | ◎ 自動リトライ | ✕ 自前実装 | ✕ 自前実装 |
| 制約設定（ネットワーク等） | ◎ | ✕ | △ |
| Android 12以降の制限対応 | ◎ | △ | ✕ 厳しい制限 |

**🏆 採用: WorkManager**

> **根拠**:  
> - Google公式: [WorkManager is the recommended solution for persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent)  
> - Google公式: [Background work overview](https://developer.android.com/develop/background-work/background-tasks)  
>
> **ドメイン観点**: オフライン中のカート操作キュー・在庫データの定期同期を  
> ネットワーク復帰時に自動送信するのに最適。リトライも自動。

---

### J. テスト

| 候補 | 概要 |
|---|---|
| **JUnit5 + MockK + Turbine** | Kotlin-first のテストスタック |
| **JUnit4 + Mockito** | Java時代からの標準テストスタック |
| **Robolectric** | JVM上でAndroidフレームワークをシミュレート |

| 評価軸 | JUnit5 + MockK + Turbine | JUnit4 + Mockito | Robolectric |
|---|:---:|:---:|:---:|
| Kotlin対応 | ◎ Kotlin-first | △ Java向け | ○ |
| Coroutines/Flowテスト | ◎ Turbine | ✕ 別途対応必要 | ○ |
| sealed class対応 | ◎ | △ | ○ |
| CI実行速度 | ◎ 高速 | ◎ 高速 | △ 遅い |
| 学習コスト | ○ | ◎ Java経験者は慣れている | ○ |

**🏆 採用: JUnit5 + MockK + Turbine**

> **根拠**:  
> - [MockK公式: Kotlin向けに設計されたモックライブラリ](https://mockk.io/)  
> - [Turbine公式: Flow のテストに特化](https://github.com/cashapp/turbine)  
> - Google公式: [Test Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test)  
>
> **ドメイン観点**: Repository（Command/Query分離）とViewModel のテストで  
> Flow（StateFlow）の状態遷移を検証するには Turbine が最適。

---

### K. CI/CD

| 候補 | 概要 |
|---|---|
| **GitHub Actions + Firebase App Distribution** | GitHub統合CI + Google公式配信 |
| **Bitrise** | モバイル特化CI/CDサービス |
| **Jenkins** | 自前ホストのCIサーバー |
| **CircleCI** | クラウドCIサービス |

| 評価軸 | GitHub Actions + FAD | Bitrise | Jenkins | CircleCI |
|---|:---:|:---:|:---:|:---:|
| セットアップ容易さ | ◎ | ◎ | ✕ | ○ |
| Android署名管理 | ○ | ◎ | ○ | ○ |
| テスター配信 | ◎ FAD連携 | ◎ | △ 別途構築 | △ |
| コスト | ○ 無料枠あり | △ 有料 | ◎ OSS | ○ 無料枠あり |
| 既存GitHub連携 | ◎ | ○ | △ | ○ |

**🏆 採用: GitHub Actions + Firebase App Distribution**

> **根拠**:  
> - [GitHub Actions: Android用ワークフロー](https://github.com/actions/setup-java)  
> - [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)  
>
> **ドメイン観点**: 海外拠点のテスターへのAPK配信が容易。  
> Firebase App Distribution はテスターグループ管理（国別・ロール別）に対応。

---

### L. 静的解析

| 候補 | 概要 |
|---|---|
| **Detekt + ktlint** | Kotlin専用の静的解析 + フォーマッター |
| **SonarQube** | 多言語対応の品質管理プラットフォーム |
| **Android Lint** | Android SDK 標準のLintツール |

| 評価軸 | Detekt + ktlint | SonarQube | Android Lint |
|---|:---:|:---:|:---:|
| Kotlin特化ルール | ◎ | ○ | △ |
| アーキテクチャ違反検出 | ◎ カスタムルール | ◎ | ✕ |
| CI統合 | ◎ | ○ サーバー必要 | ◎ |
| コスト | ◎ 無料 | △ 有料版あり | ◎ 無料 |
| 導入容易さ | ◎ Gradle Plugin | △ | ◎ |

**🏆 採用: Detekt + ktlint + Android Lint（併用）**

> **根拠**:  
> - [Detekt公式](https://detekt.dev/)  
> - [ktlint公式](https://pinterest.github.io/ktlint/)  
>
> **ドメイン観点**: Kotlin初心者が多いチームでは、レビュー前に自動で  
> コードスタイル統一 + 危険なパターン検出を行うことで品質を担保。

---

### 推奨技術スタック一覧（最終版）

| カテゴリ | 採用技術 | 選定理由（一言） |
|---|---|---|
| 言語 | **Kotlin 2.x** | Google公式の Android 開発推奨言語 |
| UI | **Jetpack Compose** | Google公式推奨。宣言的UIで動的な商品表示に最適 |
| DI | **Hilt** | Google公式推奨。コンパイル時検出で大規模に適する |
| 非同期 | **Coroutines + Flow** | Google公式推奨。在庫リアルタイム更新に必須 |
| API通信 | **Retrofit2 + OkHttp3** | 業界標準。CQRS（POST/GET）を直接表現可能 |
| ローカルDB | **Room + DataStore** | Google公式推奨。オフラインキャッシュに必須 |
| 画像読込 | **Coil** | Compose対応。大量商品画像表示に最適 |
| プッシュ通知 | **FCM** | Android唯一のOS統合プッシュ。セール通知に必須 |
| バーコード | **ML Kit** | Google公式。無料+オフライン+高精度 |
| カメラ | **CameraX** | Google公式。ML Kit との連携が容易 |
| バックグラウンド | **WorkManager** | Google公式推奨。オフライン操作の同期に必須 |
| テスト | **JUnit5 + MockK + Turbine** | Kotlin/Flow対応。CQRS分離テストに最適 |
| CI/CD | **GitHub Actions + FAD** | 海外テスターへの配信容易。無料枠あり |
| 静的解析 | **Detekt + ktlint** | Kotlin初心者チームの品質担保に必須 |

---

### 📎 Google 公式ドキュメント集（エビデンスリンク）

| 項目 | URL |
|---|---|
| Kotlin-First 宣言 (2019) | https://developer.android.com/kotlin/first |
| Jetpack Compose 公式 | https://developer.android.com/develop/ui/compose |
| Hilt 推奨 | https://developer.android.com/training/dependency-injection/hilt-android |
| Coroutines + Flow 推奨 | https://developer.android.com/kotlin/flow |
| Room 推奨 | https://developer.android.com/training/data-storage/room |
| DataStore 推奨 | https://developer.android.com/topic/libraries/architecture/datastore |
| WorkManager 推奨 | https://developer.android.com/develop/background-work/background-tasks/persistent |
| ML Kit Barcode | https://developers.google.com/ml-kit/vision/barcode-scanning/android |
| CameraX 公式 | https://developer.android.com/media/camera/camerax |
| FCM 公式 | https://firebase.google.com/docs/cloud-messaging |
| MAD（Modern Android Development） | https://developer.android.com/series/mad-skills |
| Now in Android（公式サンプルアプリ） | https://github.com/android/nowinandroid |
| アーキテクチャガイド | https://developer.android.com/topic/architecture |

---

## 7. Figma共通コンポーネントのモバイル利用における注意点

> **前提**: WebフロントエンドはFigmaの共通コンポーネントをNext.js/TypeScriptで実装している。  
> モバイル（Kotlin/Android）でも同じFigmaコンポーネントを参照する場合の課題を整理する。

---

### 7-1. Web と Android の根本的な違い

まず前提として、**FigmaのコンポーネントはWeb向けに設計されることが多く**、  
AndroidネイティブとはUIの概念が異なる。

```
【Web（Next.js）】                    【Android（Jetpack Compose）】
────────────────────────────────────────────────────────────
CSS Flexbox / Grid                  →  Column / Row / Box
px / rem / vw 単位                  →  dp / sp 単位
hover / focus 状態                  →  pressed / focused 状態
マウスクリック                      →  タップ（44dp以上の推奨サイズ）
スクロール: overflow: scroll        →  LazyColumn / LazyRow
position: fixed                     →  Scaffold / BottomAppBar
z-index                             →  Modifier.zIndex()
font-size: 16px                     →  fontSize = 16.sp
border-radius: 8px                  →  shape = RoundedCornerShape(8.dp)
```

**→ Figmaのコンポーネントをそのままモバイルで使うことは技術的に不可能。**  
　 必ず「Figmaを参照しながら、Android用に再実装」する作業が発生する。

---

### 7-2. デメリット一覧

#### ❌ デメリット① : コンポーネントの再実装コストが発生する

```
Figmaコンポーネント（Web向け設計）
        ↓  手動で再実装
Next.js コンポーネント（Web）
        ↓  手動で再実装（※流用不可）
Jetpack Compose コンポーネント（Android）
```

| 発生する作業 | 内容 |
|---|---|
| デザイントークンの変換 | カラー・余白・フォントサイズを`dp`/`sp`に手動変換 |
| コンポーネントの再実装 | ボタン・カード・モーダル等をComposableとして作り直す |
| インタラクション再定義 | hover→タップ、tooltip→BottomSheetなど |
| レスポンシブ対応 | Web基準のブレークポイントはAndroidでは使えない |

**コスト試算の目安:**  
Webで50コンポーネントある場合、Androidでの再実装に  
経験者1名で **1〜2ヶ月** 、初心者が多いチームでは **3〜4ヶ月** かかることがある。

---

#### ❌ デメリット② : Webとモバイルで「見た目の乖離」が起きやすい

Figmaで同じコンポーネントを参照しているにもかかわらず、  
実装担当者が異なると微妙なズレが発生する。

```
Figmaデザイン（正）
    │
    ├── Webエンジニア実装 → ボタン角丸8px, padding 12px
    └── Androidエンジニア実装 → ボタン角丸6dp, padding 14dp
                                          ↑
                             「なんか微妙に違う...」が発生しやすい
```

**特に乖離が起きやすい箇所:**
- フォントサイズ・行間・字間
- カラーの透明度（`#RRGGBBAA` 順序がWebとAndroidで異なる）
- 影（`box-shadow` vs `Modifier.shadow()`）
- アニメーション速度・イージング

---

#### ❌ デメリット③ : Androidプラットフォーム固有のUXガイドラインとの衝突

Webのデザインは **ブラウザのUXパターン** をベースにしているが、  
AndroidにはGoogleが定める **Material Design 3** というガイドラインがある。

| 衝突例 | Webのデザイン | Androidの推奨 |
|---|---|---|
| ナビゲーション | 上部タブ・パンくず | BottomNavigationBar / NavigationRail |
| 戻る操作 | ブラウザの「←」ボタン | システムBackジェスチャー / AppBarの「←」 |
| フォーム入力 | PC向けのinput幅 | ソフトキーボードが半分を占める想定が必要 |
| ダイアログ | 中央モーダル | AlertDialog / ModalBottomSheet |
| リスト | hover時にハイライト | Rippleエフェクト（タップ時） |
| スクロール | 自由なCSS scroll | Paging3による無限スクロール推奨 |

**WebのFigmaをそのまま踏襲すると、**  
「Androidらしくない」UI になり、ユーザーの直感に反した操作感になるリスクがある。

---

#### ❌ デメリット④ : デザイントークンの二重管理

Figmaで色・フォント・余白を更新した際に、  
WebとAndroidの**両方を手動で更新する必要**がある。

```
Figmaでプライマリカラーを #1A73E8 → #0057E7 に変更
        ↓
Webエンジニアが CSS変数 / Tailwindを更新  ← 更新もれ発生リスク
        ↓
Androidエンジニアが Color.kt を更新       ← 更新もれ発生リスク
        ↓
「WebとアプリでボタンのCが違う」バグが発生
```

---

#### ❌ デメリット⑤ : Figmaの「Auto Layout」がAndroidのレイアウトと概念が異なる

| Figma Auto Layout | Jetpack Compose |
|---|---|
| Direction: Horizontal | `Row {}` |
| Direction: Vertical | `Column {}` |
| Spacing: Space Between | `Arrangement.SpaceBetween` |
| Padding: 個別指定 | `PaddingValues(start=, top=, ...)` |
| Hug Contents | `wrapContentSize()` |
| Fill Container | `fillMaxWidth()` |
| Fixed | `width(Xdp)` |
| Clip Content: true | `clip(RoundedCornerShape())` |

**概念は対応しているが名称・設定方法が異なる**ため、  
Figmaの仕様をComposeに翻訳する作業が毎回必要になる。

---

### 7-3. 対策：デザイントークンの一元管理

デメリットを最小化するための**最も効果的な対策**は  
**デザイントークンの一元管理**である。

```
【推奨フロー】

Figma（デザインの single source of truth）
    │
    │ Figma Variables / Tokens Plugin
    ▼
tokens.json（カラー・余白・タイポグラフィの定義）
    │
    ├── Style Dictionary ──► CSS変数 / Tailwind config（Web用）
    │
    └── Style Dictionary ──► Color.kt / Typography.kt / Dimen.kt（Android用）
```

**使用ツール:**

| ツール | 役割 | URL |
|---|---|---|
| Figma Variables | Figmaでトークンを定義 | https://help.figma.com/hc/en-us/articles/15339657135383 |
| Tokens Studio for Figma | FigmaトークンをJSONでエクスポート | https://tokens.studio/ |
| Style Dictionary | JSONからWeb/Android用コードを自動生成 | https://amzn.github.io/style-dictionary/ |

**Style Dictionaryの出力例:**

```kotlin
// 自動生成される Color.kt（Androidトークン）
object AppColors {
    val Primary = Color(0xFF1A73E8)      // Figmaの "color/primary"
    val Surface = Color(0xFFFFFFFF)      // Figmaの "color/surface"
    val Error   = Color(0xFFB00020)      // Figmaの "color/error"
}

object AppSpacing {
    val XS = 4.dp    // Figmaの "spacing/xs"
    val S  = 8.dp    // Figmaの "spacing/s"
    val M  = 16.dp   // Figmaの "spacing/m"
    val L  = 24.dp   // Figmaの "spacing/l"
}
```

```css
/* 自動生成される CSS変数（Web用）*/
:root {
  --color-primary: #1A73E8;   /* Figmaの "color/primary" */
  --color-surface: #FFFFFF;
  --spacing-xs: 4px;
  --spacing-m: 16px;
}
```

→ **Figmaを更新 → JSONエクスポート → CI自動生成** のフローにすることで、  
　 WebとAndroidのカラー・余白の乖離をゼロにできる。

---

### 7-4. 対策：AndroidのUI実装方針

Figmaコンポーネントを参照しながら、  
**Android用のデザインシステム（Compose Component Library）** を別途構築することを推奨する。

```
┌──────────────────────────────────────────────────────────────┐
│  Figma共通コンポーネント                                      │
│  （デザインの正）                                            │
└────────────────────────────┬─────────────────────────────────┘
                             │ 参照（実装は別）
              ┌──────────────┴──────────────┐
              ▼                             ▼
┌─────────────────────────┐   ┌──────────────────────────────┐
│  Webコンポーネント      │   │  Android Compose Component   │
│  （Next.js / React）    │   │  Library                     │
│                         │   │                              │
│  AppButton.tsx          │   │  AppButton.kt（Composable）  │
│  AppCard.tsx            │   │  AppCard.kt                  │
│  AppTextField.tsx       │   │  AppTextField.kt             │
└─────────────────────────┘   └──────────────────────────────┘
         ↑                                ↑
   デザイントークン共有（Style Dictionary で自動生成）
```

---

### 7-5. デメリットと対策まとめ

| デメリット | 深刻度 | 対策 |
|---|:---:|---|
| ① コンポーネント再実装コスト | 🔴 高 | 初期にCompose Component Libraryをまとめて構築し、以降は流用 |
| ② WebとAndroidの見た目乖離 | 🟡 中 | デザイントークン自動生成 + Figmaレビュープロセスの整備 |
| ③ Material Design 3との衝突 | 🟡 中 | AndroidエンジニアがFigmaレビューに参加し、早期に修正依頼 |
| ④ トークンの二重管理 | 🔴 高 | Style Dictionary による一元管理・自動生成 |
| ⑤ Auto Layoutの翻訳コスト | 🟢 低 | 対応表をチームで共有・Figma仕様書にCompose名を併記 |

---

### 7-6. デザイナーへの依頼事項（早期対応推奨）

モバイル実装を効率化するために、Figmaデザイン作成時点で以下を依頼する。

```
✅ デザイントークン（カラー・余白・フォント）をFigma Variablesで管理する
✅ コンポーネントにAndroid固有の状態（pressed / disabled / loading）を追加する
✅ タッチターゲットサイズを最低44×44dp（Google推奨）に合わせる
✅ Webのhover状態はAndroidでは不要なため省略してよい
✅ ナビゲーションパターンはMaterial Design 3に準拠したものを用意する
✅ フォント指定はpx単位ではなくsp換算値も併記する
```
