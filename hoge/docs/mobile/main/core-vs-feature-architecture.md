# `core/` vs `feature/` アーキテクチャ完全ガイド

> Now in Android プロジェクトのモジュール構造を、大規模開発（数十億ユーザー規模）の観点から徹底解説

---

## 概要：一言で言うと

| 観点 | `core/` | `feature/` |
|------|---------|------------|
| **役割** | 横断的な基盤・共通ロジック | 特定画面のUI・操作ロジック |
| **依存方向** | 誰にも依存しない（最下層） | `core/` に依存する（上位層） |
| **再利用性** | 全モジュールから使われる | 基本的に単体画面で完結 |
| **ビジネスロジック** | Repository / UseCase に集中 | ViewModel のみ |
| **テスト戦略** | 単体テスト重視 | UI テスト重視 |

---

## アーキテクチャ全体像

```
┌─────────────────────────────────────────┐
│              app/                        │  ← 最終組み立て
├─────────────────────────────────────────┤
│  feature/foryou  feature/search  ...    │  ← UI + ViewModel
├─────────────────────────────────────────┤
│         core/domain  (UseCase)          │  ← ビジネスルール
│         core/data    (Repository)       │  ← データ調整
├─────────────────────────────────────────┤
│  core/database  core/datastore          │  ← ローカル永続化
│  core/network                           │  ← リモートAPI
├─────────────────────────────────────────┤
│  core/model   core/designsystem  ...    │  ← 共通型・UI部品
└─────────────────────────────────────────┘
```

依存は**必ず下向き**。feature → core/domain → core/data → core/database の一方通行。

---

## `core/` モジュール詳細

### `core/model` ── データモデル定義（最下層）

アプリ全体で共有するドメインモデル。外部依存ゼロ。

```
core/model/data/
├── NewsResource.kt       // ニュース記事
├── Topic.kt              // トピック（Android, Compose 等）
├── UserData.kt           // ユーザー設定・既読・フォロー状態
├── FollowableTopic.kt    // Topic + isFollowed の複合
├── UserNewsResource.kt   // NewsResource + UserData の複合
├── SearchResult.kt       // 検索結果（topics + newsResources）
├── UserSearchResult.kt   // ユーザー状態付き検索結果
└── ThemeBrand.kt / DarkThemeConfig.kt  // テーマ設定
```

> **大規模開発ポイント**  
> モデルを `core/model` に集中させることで、複数チームが同じ型を参照できる。  
> feature チームがモデル定義を変えても影響範囲がすぐわかる。

---

### `core/database` ── Room データベース（ローカル永続化）

#### テーブル構成（Room Entities）

```
NiaDatabase (version 14)
├── news_resources         → NewsResourceEntity
├── news_resources_topics  → NewsResourceTopicCrossRef  （多対多の中間テーブル）
├── news_resources_fts     → NewsResourceFtsEntity      （全文検索インデックス）
├── topics                 → TopicEntity
├── topics_fts             → TopicFtsEntity             （全文検索インデックス）
└── recent_search_queries  → RecentSearchQueryEntity
```

#### DAOs（データアクセスオブジェクト）

| DAO | 主な操作 |
|-----|---------|
| `NewsResourceDao` | `getNewsResources(query)` / `upsertNewsResources()` / `deleteNewsResources()` |
| `TopicDao` | トピックの取得・挿入 |
| `NewsResourceFtsDao` | 全文検索クエリ |
| `TopicFtsDao` | トピック全文検索 |
| `RecentSearchQueryDao` | 検索履歴の保存・取得 |

#### DAO の実装例（`NewsResourceDao`）

```kotlin
@Dao
interface NewsResourceDao {
    @Transaction
    @Query("""
        SELECT * FROM news_resources
        WHERE 
            CASE WHEN :useFilterNewsIds THEN id IN (:filterNewsIds) ELSE 1 END
         AND
            CASE WHEN :useFilterTopicIds
                THEN id IN (
                    SELECT news_resource_id FROM news_resources_topics
                    WHERE topic_id IN (:filterTopicIds)
                )
                ELSE 1
            END
        ORDER BY publish_date DESC
    """)
    fun getNewsResources(
        useFilterTopicIds: Boolean = false,
        filterTopicIds: Set<String> = emptySet(),
        useFilterNewsIds: Boolean = false,
        filterNewsIds: Set<String> = emptySet(),
    ): Flow<List<PopulatedNewsResource>>

    @Upsert
    suspend fun upsertNewsResources(entities: List<NewsResourceEntity>)

    @Query("DELETE FROM news_resources WHERE id in (:ids)")
    suspend fun deleteNewsResources(ids: List<String>)
}
```

> **大規模開発ポイント**  
> - `@Transaction` でJOINクエリの整合性を保証  
> - `Flow<List<T>>` で返すことでリアルタイム更新に対応  
> - `CASE WHEN` による動的フィルタで、1つのクエリを複数ユースケースに再利用  
> - `AutoMigration` により、v1→v14まで大半は自動でスキーマ管理（一部は手動 spec クラスが必要：v2→3, v10→11, v11→12）  

#### スキーマ管理（マイグレーション）

```kotlin
@Database(
    version = 14,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = DatabaseMigrations.Schema2to3::class),
        // ... v14まで全バージョン管理
    ],
    exportSchema = true,  // schemas/フォルダにJSONエクスポート → Git管理
)
```

---

### `core/data` ── Repository パターン（データ調整層）

#### Repository インターフェース一覧

```
core/data/repository/
├── NewsRepository.kt              // interface: getNewsResources()
├── TopicsRepository.kt            // interface: getTopics()
├── UserDataRepository.kt          // interface: 設定・既読・ブックマーク管理
├── UserNewsResourceRepository.kt  // interface: ユーザー状態付きニュース
├── SearchContentsRepository.kt    // interface: 全文検索
└── RecentSearchRepository.kt      // interface: 検索履歴
```

#### Repository 実装一覧（`internal` クラス）

```
├── OfflineFirstNewsRepository     // NewsRepository の実装
├── OfflineFirstTopicsRepository   // TopicsRepository の実装
├── OfflineFirstUserDataRepository // UserDataRepository の実装
├── CompositeUserNewsResourceRepository // 複数ソースを合成
├── DefaultSearchContentsRepository
└── DefaultRecentSearchRepository
```

#### Offline First の実装パターン

```kotlin
// OfflineFirstNewsRepository.kt
internal class OfflineFirstNewsRepository @Inject constructor(
    private val niaPreferencesDataSource: NiaPreferencesDataSource,  // DataStore
    private val newsResourceDao: NewsResourceDao,                     // Room
    private val topicDao: TopicDao,                                   // Room（トピック参照用）
    private val network: NiaNetworkDataSource,                        // Retrofit
    private val notifier: Notifier,
) : NewsRepository {

    // 読み取りは常にローカルDB から
    override fun getNewsResources(query: NewsResourceQuery): Flow<List<NewsResource>> =
        newsResourceDao.getNewsResources(...).map { it.map(PopulatedNewsResource::asExternalModel) }

    // 同期時だけネットワークにアクセス（バッチサイズ40件ずつ）
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        return synchronizer.changeListSync(
            changeListFetcher = { network.getNewsResourceChangeList(after = currentVersion) },
            modelUpdater = { changedIds ->
                changedIds.chunked(SYNC_BATCH_SIZE).forEach { chunkedIds ->
                    val networkData = network.getNewsResources(ids = chunkedIds)
                    newsResourceDao.upsertNewsResources(...)
                }
            }
        )
    }
}
```

> **大規模開発ポイント**  
> - `internal` 修飾子でモジュール外への実装漏洩を防ぐ  
> - Hilt DI で interface と実装を分離 → テスト時に Fake に差し替え可能  
> - Change List ベースの差分同期で不要なデータ転送を最小化  

---

### `core/domain` ── UseCase（ビジネスルール）

複数 Repository をまたぐロジックや、複数 ViewModel で共有するロジックをここに集約する。

#### UseCase 一覧

| UseCase | 依存 Repository | 返す型 | 用途 |
|---------|----------------|--------|------|
| `GetFollowableTopicsUseCase` | `TopicsRepository` + `UserDataRepository` | `Flow<List<FollowableTopic>>` | フォロー状態付きトピック一覧 |
| `GetSearchContentsUseCase` | `SearchContentsRepository` + `UserDataRepository` | `Flow<UserSearchResult>` | 検索結果をユーザー状態付きに変換 |
| `GetRecentSearchQueriesUseCase` | `RecentSearchRepository` | `Flow<List<RecentSearchQuery>>` | 検索履歴取得 |

#### UseCase の実装パターン

```kotlin
// GetFollowableTopicsUseCase.kt
class GetFollowableTopicsUseCase @Inject constructor(
    private val topicsRepository: TopicsRepository,
    private val userDataRepository: UserDataRepository,
) {
    // operator fun invoke() で関数として呼び出せる
    operator fun invoke(sortBy: TopicSortField = NONE): Flow<List<FollowableTopic>> =
        combine(
            userDataRepository.userData,
            topicsRepository.getTopics(),
        ) { userData, topics ->
            topics.map { topic ->
                FollowableTopic(
                    topic = topic,
                    isFollowed = topic.id in userData.followedTopics,
                )
            }
        }
}
```

> **UseCase を作るべきタイミング**  
> 1. 2つ以上の Repository を組み合わせる処理  
> 2. 複数の ViewModel で同じロジックを使う場合  
> 3. 複雑なデータ変換（combine, map, filter 等）

> **UseCase を作らなくてよいタイミング**  
> Repository に単純に委譲するだけなら ViewModel から直接呼ぶ

---

### その他の `core/` モジュール

| モジュール | 内容 |
|-----------|------|
| `core/datastore` | DataStore（ユーザー設定・フォロー・既読の永続化） |
| `core/network` | Retrofit によるAPIクライアント |
| `core/designsystem` | 共通UIコンポーネント（Color, Typography, Iconography） |
| `core/ui` | 再利用可能な Compose コンポーネント（NewsFeed等） |
| `core/navigation` | ナビゲーション定義の共通部品 |
| `core/analytics` | Firebase Analytics ラッパー |
| `core/notifications` | 通知送信ロジック |
| `core/testing` | テスト用 Fake / Helper クラス |

---

## `feature/` モジュール詳細

### 構造：`api/` + `impl/` の分割

各 feature は **`api`** と **`impl`** の2サブモジュールに分かれる。
（※ `feature/settings` は例外的に `impl` のみ）

NiA は **navigation3**（`NavDisplay` + `entryProvider` パターン）を採用している。

```
feature/foryou/
├── api/   → ナビゲーションキー（外部から参照可能）+ Navigator 拡張関数
│   └── ForYouNavKey.kt
└── impl/  → ViewModel + 画面コンポーザブル + EntryProvider
    ├── ForYouViewModel.kt
    ├── ForYouScreen.kt
    └── navigation/ForYouEntryProvider.kt
```

> **なぜ分割するのか？**  
> - 他の feature が `api` だけに依存することで、実装の詳細（`impl`）を隠蔽できる  
> - ナビゲーション先の変更が内部実装に影響しない  
> - ビルドグラフの並列化による高速化

### navigation3 のパターン

#### api モジュール：NavKey + Navigator 拡張関数

```kotlin
// feature/topic/api/navigation/TopicNavKey.kt
@Serializable
data class TopicNavKey(val id: String) : NavKey

// Navigator 拡張関数で遷移先を型安全に公開
fun Navigator.navigateToTopic(topicId: String) {
    navigate(TopicNavKey(topicId))
}
```

```kotlin
// feature/foryou/api/navigation/ForYouNavKey.kt
@Serializable
object ForYouNavKey : NavKey  // パラメータなし
```

#### impl モジュール：EntryProvider

```kotlin
// feature/foryou/impl/navigation/ForYouEntryProvider.kt
fun EntryProviderScope<NavKey>.forYouEntry(navigator: Navigator) {
    entry<ForYouNavKey> {
        ForYouScreen(
            onTopicClick = navigator::navigateToTopic,  // ← api の拡張関数を参照
        )
    }
}
```

```kotlin
// feature/topic/impl/navigation/TopicEntryProvider.kt
fun EntryProviderScope<NavKey>.topicEntry(navigator: Navigator) {
    entry<TopicNavKey> { key ->           // ← key からパラメータを取得
        TopicScreen(
            showBackButton = true,
            onBackClick = { navigator.goBack() },
            onTopicClick = navigator::navigateToTopic,
            viewModel = hiltViewModel<TopicViewModel, Factory>(key = key.id) { factory ->
                factory.create(key.id)
            },
        )
    }
}
```

#### app モジュール：entryProvider + NavDisplay

```kotlin
// app/src/main/.../NiaApp.kt
val navigator = remember { Navigator(appState.navigationState) }

val entryProvider = entryProvider {
    forYouEntry(navigator)
    bookmarksEntry(navigator)
    interestsEntry(navigator)
    topicEntry(navigator)
    searchEntry(navigator)
}

NavDisplay(
    entries = appState.navigationState.toEntries(entryProvider),
    onBack = { navigator.goBack() },
)
```

> **Navigation 2 との違い：**  
> - `NavHost` + `NavController` → `NavDisplay` + `Navigator`  
> - `composable<Key> { backStackEntry -> ... }` → `entry<Key> { key -> ... }`  
> - `NavGraphBuilder.xxxEntry(callbacks)` → `EntryProviderScope<NavKey>.xxxEntry(navigator)`  
> - コールバック地獄がなくなり、Navigator 経由で型安全に遷移

### feature モジュール一覧

| モジュール | 画面 | ViewModel | 使用する UseCase | 直接使う Repository |
|-----------|------|-----------|-----------------|---------------------|
| `feature/foryou` | おすすめフィード | `ForYouViewModel` | `GetFollowableTopicsUseCase` | `UserNewsResourceRepository`, `UserDataRepository` |
| `feature/search` | 検索 | `SearchViewModel` | `GetSearchContentsUseCase`, `GetRecentSearchQueriesUseCase` | `SearchContentsRepository`, `RecentSearchRepository`, `UserDataRepository` |
| `feature/interests` | 興味・フォロー管理 | `InterestsViewModel` | `GetFollowableTopicsUseCase` | `UserDataRepository` |
| `feature/topic` | トピック詳細 | `TopicViewModel` | ― | `TopicsRepository`, `UserNewsResourceRepository`, `UserDataRepository` |
| `feature/bookmarks` | ブックマーク | `BookmarksViewModel` | ― | `UserNewsResourceRepository`, `UserDataRepository` |
| `feature/settings` | 設定 | `SettingsViewModel` | ― | `UserDataRepository` |

### ViewModel の実装パターン

```kotlin
// ForYouViewModel.kt
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,  // ← UseCase を注入
    syncManager: SyncManager,
) : ViewModel() {

    // UI状態は StateFlow で公開（コールドではなくホット）
    val feedState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllForFollowedTopics()
            .map(NewsFeedUiState::Success)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NewsFeedUiState.Loading,
            )

    // ユーザー操作は suspend fun で Repository を直接呼ぶ
    fun updateTopicSelection(topicId: String, isChecked: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(topicId, isChecked)
        }
    }
}
```

---

## 依存関係の図解

```
feature/search
    │
    ├── core/domain/GetSearchContentsUseCase
    │       ├── core/data/SearchContentsRepository
    │       │       └── core/database/NewsResourceFtsDao
    │       └── core/data/UserDataRepository
    │               └── core/datastore/NiaPreferencesDataSource
    │
    ├── core/domain/GetRecentSearchQueriesUseCase
    │       └── core/data/RecentSearchRepository
    │               └── core/database/RecentSearchQueryDao
    │
    └── core/model (UserSearchResult, etc.)
```

---

## 大規模開発（数十億ユーザー規模）での設計意図

### 1. チーム分割への対応

```
チームA: core/database, core/data     → データ基盤チーム
チームB: core/domain                  → ビジネスロジックチーム
チームC: feature/foryou, feature/search → フィーチャーチーム
チームD: core/designsystem            → デザインシステムチーム
```

各チームが独立してビルド・テスト可能。他チームの実装を待たずに Fake で開発継続できる。

### 2. テスト戦略

```
core/database   → Instrumented Test（実 Room DB）
core/data       → Unit Test（Fake DAO / Fake DataSource）
core/domain     → Unit Test（Fake Repository）
feature/        → UI Test（ComposeTestRule + ComponentActivity）
                   Unit Test（Fake UseCase / Fake Repository）
```

### 3. Offline First アーキテクチャ

```
ユーザー操作 → ViewModel → Repository
                               │
                    ┌──────────┴────────────┐
                    ↓                       ↓
               Room（即座に返す）       WorkManager同期
                    ↑                       ↓
                    └──────────────── Network API
```

- 読み取りはすべてローカルDB から → ネットワーク障害でもUI継続
- 同期は差分ベース（Change List） → 帯域幅とバッテリー最適化
- バッチサイズ40件で分割同期 → OOM防止・キャンセル対応

### 4. `internal` による実装隠蔽

```kotlin
// ❌ feature から直接触れない
internal class OfflineFirstNewsRepository : NewsRepository

// ✅ feature が触れるのはインターフェースだけ
interface NewsRepository {
    fun getNewsResources(...): Flow<List<NewsResource>>
}
```

実装を差し替えても feature 側のコードは一切変更不要。

### 5. Flow による反応型UI

```
DataStore/Room（書き込み）
    ↓ emit
Repository（Flow変換）
    ↓ collect
ViewModel（stateIn → StateFlow）
    ↓ collectAsStateWithLifecycle
Compose UI（自動再コンポーズ）
```

状態管理のコードが宣言的で、競合状態（race condition）が起きにくい。

---

## まとめ：どちらに何を書くべきか

| 要件 | 書く場所 |
|------|---------|
| DBテーブル・DAOの追加 | `core/database` |
| 新しいAPIエンドポイントの追加 | `core/network` |
| ユーザー設定の追加 | `core/datastore` |
| 複数ソースのデータ合成ロジック | `core/data` (Repository) |
| 複数 VM で使うビジネスルール | `core/domain` (UseCase) |
| 単純なデータ取得（1 Repository のみ） | `feature/` ViewModel から直接 |
| 画面UI・アニメーション | `feature/impl` |
| 画面間のナビゲーションキー | `feature/api` |
| 再利用する UI コンポーネント | `core/ui` または `core/designsystem` |

---

## 複数サブドメインがある場合：NiA モデルの限界と代替設計

### NiA が前提としている世界観

NiA の `core/` が綺麗に機能するのは、**ドメインが1つ**だからである。

```
「Android ニュースを読む」という単一目的
    → ニュース記事 / トピック / ユーザー設定 の3エンティティで完結
    → core/data, core/domain が肥大化しない
```

**販売領域に複数のサブドメインが存在する場合、この前提が崩れる。**

```
販売ドメイン
├── 受注管理（Order）
├── 在庫管理（Inventory）
├── 顧客管理（Customer）
└── 請求管理（Billing）
```

これを NiA 流に作ると：

```
core/data/repository/
├── OrderRepository.kt
├── InventoryRepository.kt
├── CustomerRepository.kt
├── BillingRepository.kt
├── OrderItemRepository.kt
├── ShipmentRepository.kt
...（際限なく増える）
```

`core/` がサブドメイン全員の「引き出し置き場」になり、**どこに何があるか誰もわからない状態**になる。

---

### feature に data/usecase を入れることの何が問題か

「feature に data/usecase を入れてはいけない」は**絶対ルールではない**。
ただし、以下の問題が起きやすい。

#### 問題1：feature 間でデータを共有できなくなる

```
feature/order/
└── data/OrderRepository.kt   ← ここに閉じ込めた

feature/billing/
└── domain/CreateInvoiceUseCase.kt
         ↑ 請求書作成には受注データが必要だが...
         feature/order には依存できない（feature 間の循環依存）
```

feature モジュール間の依存は原則禁止。受注データを取りたい billing が取れなくなる。

```
❌ feature/billing → feature/order  // 循環依存の温床
```

#### 問題2：同じデータを2箇所で取ることになる

```
feature/order/data/OrderRepository.kt     // 受注一覧
feature/inventory/data/OrderRepository.kt // 在庫計算用の受注データ（同じAPIを叩いてる）
```

ネットワーク・DBアクセスが重複し、キャッシュも効かない。

#### 問題3：テスト戦略が崩れる

feature を「UIのテスト単位」として切り出した意味がなくなる。
data 層まで入ると feature のテストが実質的な統合テストになり、遅くなる。

#### 問題4：Hilt スコープの管理が複雑になる

feature の DI モジュールでは、ActivityComponent や ViewModelComponent で bind する。
data 層は SingletonComponent で bind するのが基本。
feature に data を入れると「このRepositoryのライフサイクルは何か」が曖昧になる。

---

### 「偶発的な共通化」の問題：間違った DRY

> 「異なる目的なのに、たまたま同じ実装だから core に置く」は危険

これは **Wrong Abstraction（間違った抽象化）** と呼ばれるアンチパターン。

#### 具体例

```kotlin
// 受注管理チームが作った
fun getCustomerName(customerId: String): String

// 顧客管理チームが「同じだから共通化しよう」と core に移動
// core/domain/GetCustomerNameUseCase.kt
```

半年後：

```kotlin
// 受注管理：顧客の「請求先名」が必要になった
// 顧客管理：顧客の「表示名（ニックネーム可）」が必要になった
// → 同じ関数に if 分岐が増える

fun getCustomerName(
    customerId: String,
    nameType: NameType = NameType.DISPLAY, // 顧客管理用
    useBillingName: Boolean = false,       // 受注管理用
    fallbackToLegal: Boolean = true,       // いつの間にか増えた
): String
```

**「目的が一致している」と「実装がたまたま一致している」は別物。**

| 共通化すべき | 共通化すべきでない |
|------------|-----------------|
| 同じビジネスルールを共有している | たまたま同じ実装になっている |
| 変更理由が必ず同時に発生する | 変更理由が独立している |
| チームをまたいで意味が同じ | チームによって意味が微妙に違う |

> Sandi Metz の言葉：  
> **"Duplication is far cheaper than the wrong abstraction."**  
> （重複のほうが、間違った抽象化よりずっと安い）

---

### 複数サブドメイン向けの代替構造：垂直スライス

NiA の「水平レイヤー」モデルに対して、**「垂直スライス（Vertical Slice）」** を採用する。

#### 水平レイヤー（NiA 方式）

```
core/data/    ← 全ドメインの Repository が横一列
core/domain/  ← 全ドメインの UseCase が横一列
feature/      ← UI のみ
```

#### 垂直スライス（サブドメイン分離方式）

```
core/                    ← インフラのみ（DB接続、Network Client、Design System）
│
feature/
├── order/               ← 受注サブドメイン（垂直に完結）
│   ├── api/             ← ナビゲーションキー（外部公開）
│   ├── data/            ← OrderRepository（受注専用）
│   ├── domain/          ← CreateOrderUseCase, CancelOrderUseCase
│   └── ui/              ← OrderListScreen, OrderDetailScreen
│
├── inventory/           ← 在庫サブドメイン（垂直に完結）
│   ├── api/
│   ├── data/            ← InventoryRepository（在庫専用）
│   ├── domain/          ← CheckStockUseCase, ReserveStockUseCase
│   └── ui/
│
├── customer/            ← 顧客サブドメイン（垂直に完結）
│   ├── api/
│   ├── data/
│   ├── domain/
│   └── ui/
│
└── shared/              ← サブドメインをまたぐ共有（意図的な共通化のみ）
    ├── data/            ← 例：認証トークン管理
    └── domain/          ← 例：通知送信UseCase（複数ドメインが使う）
```

#### core には「ドメイン知識ゼロ」のものだけ置く

```
core/network/    ← HTTP クライアント（どのドメインにも依存しない）
core/database/   ← Room セットアップ（テーブル定義は各 feature/*/data/ が持つ）
core/datastore/  ← DataStore セットアップ
core/designsystem/ ← ボタン、色、タイポグラフィ
core/analytics/  ← ログ送信インターフェース
```

---

### 横断依存が必要になったときの対処

「受注画面で顧客名を表示したい」など、サブドメインをまたぐ参照が発生した場合：

#### パターンA：`feature/shared/` に切り出す（意図的な共通化）

```
feature/shared/customer-ref/  ← 「他ドメインが顧客を参照するための最小API」
    CustomerReference.kt      ← id + displayName だけの軽量モデル
    CustomerReferenceRepository.kt
```

受注ドメインは `CustomerReferenceRepository` だけを使う。
顧客ドメイン内部の詳細（ポイント、会員ランク等）には触れない。

#### パターンB：API の境界を明示する（`feature/*/api/` モジュール）

```
feature/customer/api/
    CustomerSummary.kt           ← 外部公開用の軽量モデル
    CustomerLookupContract.kt    ← 外部から検索するためのインターフェース
```

他の feature は `customer/api` にだけ依存する。`customer/impl` には依存しない。

---

### 判断フローチャート

```
新しいロジックを書く
       │
       ▼
複数サブドメインで使われるか？
 ├─ No  → feature/{subdomain}/domain/ に書く
 └─ Yes → 意図的に同じ目的か？（≠ たまたま同じ実装）
            ├─ No  → 各ドメインに重複して書く（DRY より境界を優先）
            └─ Yes → feature/shared/ または core/ に書く
                         │
                         ▼
                    ドメイン知識を含むか？
                     ├─ Yes → feature/shared/
                     └─ No  → core/
```

---

### NiA 方式 vs 垂直スライス　比較まとめ

| 観点 | NiA（水平レイヤー） | 垂直スライス |
|------|-------------------|-------------|
| **向いているドメイン数** | 1〜2（単一目的） | 3以上（複数サブドメイン） |
| **チーム分割** | レイヤーごとにチーム | サブドメインごとにチーム |
| **feature 間依存** | 発生しにくい | api モジュールで制御が必要 |
| **core の肥大化** | 起きやすい | 起きにくい |
| **誤った共通化リスク** | 高い（全部 core に入れがち） | 低い（境界が明確） |
| **ビルド速度** | feature が軽量で速い | feature が重くなりうる |
| **テストの独立性** | 高い | 中（data まで含むと統合テスト化） |

---

## マルチレポ + バックエンド DDD 環境での適用

> **前提：** バックエンド（Java / Spring Boot / ヘキサゴナルアーキテクチャ + 簡易 CQRS / DDD）と  
> モバイル（Kotlin / MVVM + UDF）を**別リポジトリ**で管理するプロジェクト。  
> バックエンドが業務ロジックを持ち、モバイルは表示ルール・UI/UX に専念する。

本ドキュメントの `core/` vs `feature/` 構造はモバイル単体リポジトリの内部設計として有効だが、  
マルチレポ構成ではいくつかの観点が加わる。

---

### 1. 責務の分界点：「誰がルールを持つか」

NiA は単体アプリのため、ビジネスロジック（UseCase）もモバイル側に置いている。  
バックエンドに DDD を導入するプロジェクトでは、この責務配分が根本的に変わる。

```
┌──────────────────────────────────────────────────────┐
│  バックエンド（Java / Spring Boot / DDD）             │
│  ├── ドメイン層: Order.canCancel(), 金額計算, 在庫引当 │
│  ├── アプリケーション層: Command / Query Handler       │
│  └── アダプター層: REST API で結果を返す               │
└──────────────────────┬───────────────────────────────┘
                       │ REST API（JSON）
                       ▼
┌──────────────────────────────────────────────────────┐
│  モバイル（Kotlin / MVVM + UDF）                      │
│  ├── core/: インフラ（HTTP, Cache, DesignSystem）     │
│  └── feature/: 表示ルール + UI                        │
│       ├── data/: API呼び出し＋キャッシュ               │
│       ├── domain/: 表示変換・フォームバリデーション     │
│       └── impl/: ViewModel + Screen                  │
└──────────────────────────────────────────────────────┘
```

| ロジック種別 | 配置先 | 具体例 |
|---|---|---|
| **業務ルール** | バックエンド ドメイン層 | キャンセル可否判定、金額計算、在庫引当 |
| **業務バリデーション** | バックエンド アプリケーション層 | 受注数量の上限チェック、顧客与信チェック |
| **フォームバリデーション** | モバイル feature/*/domain/ | 必須入力、文字数制限、形式チェック（メール形式等） |
| **表示ルール** | モバイル feature/*/domain/ | ステータスバッジの色、一覧のセクション分け、エラーメッセージ変換 |
| **画面遷移** | モバイル feature/*/api/ + impl/ | NavKey、EntryProvider |
| **状態管理** | モバイル feature/*/impl/ | ViewModel（UDF） |

> **判断基準：**  
> 「この判断を間違えたとき、業務上の損害が出るか？」  
> → Yes ならバックエンド。No ならモバイル。

---

### 2. `core/` の変化：API クライアントの比重が上がる

NiA では `core/database`（Room）が中心的なデータソースだが、  
バックエンド DDD + マルチレポ構成では **`core/network` の重要度が上がる**。

```
NiA（単体アプリ）:
  core/database ★★★  ← データの中心
  core/network  ★     ← 同期時だけ使う
  core/datastore ★★   ← ユーザー設定

マルチレポ（バックエンド DDD）:
  core/network  ★★★  ← データの中心（API 呼び出し）
  core/database ★     ← キャッシュ用途のみ（or 不使用）
  core/datastore ★★   ← ユーザー設定・トークン管理
```

#### `core/network` に追加すべきもの

```
core/network/
├── SalesHttpClient.kt        ← OkHttpClient 設定
├── RetrofitFactory.kt        ← Retrofit インスタンス生成
├── AuthInterceptor.kt        ← 認証トークン付与
├── TokenRefreshAuthenticator.kt ← トークン自動リフレッシュ  ← 追加
├── ErrorResponseParser.kt    ← RFC 7807 等の共通エラー解析  ← 追加
├── NetworkMonitor.kt         ← ネットワーク状態監視        ← 追加
└── di/
    └── NetworkModule.kt
```

```kotlin
// core/network/ErrorResponseParser.kt
// バックエンドの業務エラーをモバイル共通の型に変換する
// エラーフォーマットの標準は全サブドメイン共通 → core に置いてよい
data class ApiBusinessError(
    val type: String,             // エラー種別 URI
    val title: String,            // 概要
    val status: Int,              // HTTP ステータス
    val detail: String,           // 詳細メッセージ（ユーザー向け表示可）
    val fieldErrors: List<FieldError>,  // フィールド単位のエラー
)

data class FieldError(
    val field: String,   // "items[0].quantity"
    val message: String, // "在庫が不足しています"
)
```

---

### 3. `feature/*/domain/` の意味が変わる

NiA では `domain/` = ビジネスルール。  
マルチレポ構成では `domain/` = **表示・操作ルール**。

```kotlin
// ✅ マルチレポ構成でモバイル domain/ に置くべき UseCase

// 表示変換：APIレスポンス → 画面表示用モデル
class FormatOrderListUseCase @Inject constructor() {
    operator fun invoke(orders: List<Order>): List<OrderSection> {
        return orders
            .groupBy { it.createdAt.toLocalDate() }
            .map { (date, orders) -> OrderSection(date, orders) }
    }
}

// フォームバリデーション（即時フィードバック用）
class ValidateOrderFormUseCase @Inject constructor() {
    operator fun invoke(input: OrderFormInput): ValidationResult {
        val errors = buildList {
            if (input.customerName.isBlank()) add(FieldError("customerName", "必須項目です"))
            if (input.items.isEmpty()) add(FieldError("items", "商品を1つ以上追加してください"))
            input.items.forEachIndexed { i, item ->
                if (item.quantity <= 0) add(FieldError("items[$i].quantity", "1以上を入力してください"))
            }
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    // ※ 業務バリデーション（在庫チェック、与信チェック等）はAPI呼び出し時にバックエンドが実施
}

// APIエラーのUI表示変換
class MapApiErrorToUiMessageUseCase @Inject constructor(
    private val resourceProvider: StringResourceProvider,
) {
    operator fun invoke(error: ApiBusinessError): UserMessage {
        return when {
            error.type.contains("insufficient-stock") ->
                UserMessage(resourceProvider.getString(R.string.error_insufficient_stock), Severity.WARNING)
            error.status == 409 ->
                UserMessage(resourceProvider.getString(R.string.error_conflict), Severity.ERROR)
            else ->
                UserMessage(error.detail, Severity.ERROR)
        }
    }
}
```

```kotlin
// ❌ マルチレポ構成でモバイル domain/ に置いてはいけない UseCase
// （バックエンドのドメイン層が持つべき）

class CreateOrderUseCase { ... }      // → バックエンド: CreateOrderCommandHandler
class CanCancelOrderUseCase { ... }   // → APIレスポンスの actions.canCancel で返す
class ReserveStockUseCase { ... }     // → バックエンド: StockReservationService
```

---

### 4. API レスポンス設計が `feature/` 構造に影響する

バックエンドが業務判定結果を API レスポンスに含める設計にすると、  
モバイル側の `feature/*/domain/` が大幅にシンプルになる。

```json
// GET /api/orders/{id} のレスポンス例
{
  "id": "ORD-001",
  "status": "CONFIRMED",
  "statusDisplayName": "確認済み",
  "totalAmount": { "amount": 10000, "currency": "JPY" },
  "items": [...],

  "actions": {
    "canCancel": true,
    "canEdit": false,
    "canClaim": false,
    "cancelDeadline": "2026-04-10T23:59:59Z"
  },

  "customer": {
    "id": "CUST-001",
    "displayName": "山田商店"
  }
}
```

**効果：**

| NiA / 単体アプリ方式 | マルチレポ方式 |
|---|---|
| `CanCancelOrderUseCase` でルール判定 | `actions.canCancel` をそのまま使う |
| `GetOrderWithCustomerUseCase` で 2 Flow を combine | API が customer を含めて返す |
| モバイルにビジネスルールが散在 | ルールはバックエンド 1 箇所 |
| ルール変更 → モバイルアプリ更新必要 | ルール変更 → バックエンドデプロイだけ |

> **「ルール変更にアプリ更新が不要」** は大規模運用での大きな利点。  
> 特に小売業の社内ツールでは、業務ルールの頻繁な変更が予想される。

---

### 5. マルチレポでの API 契約管理

フロント・モバイル・バックエンドが別リポジトリの場合、  
**API 契約（スキーマ）の管理方法** がアーキテクチャ上の最重要決定事項になる。

```
バックエンドRepo                     モバイルRepo
├── order-service/                  ├── feature/order/data/
│   └── adapter/in/web/             │   ├── network/
│       ├── OrderController.java    │   │   ├── OrderApiService.kt  ← ここが契約に依存
│       └── dto/                    │   │   └── model/
│           └── OrderResponse.java  │   │       └── NetworkOrder.kt
│                                   │   └── OrderRepository.kt
└── docs/api/                       │
    └── openapi.yaml ★              └── ← openapi.yaml を参照してコード生成
```

#### 選択肢

| 方式 | 利点 | 欠点 | 適する場面 |
|---|---|---|---|
| **OpenAPI + コード生成** | Spring Boot 親和性◎、エコシステム成熟 | YAML 管理が手間 | REST API 中心 |
| **GraphQL** | モバイルが必要なフィールドだけ取得 | バックエンド実装コスト高 | 画面数が多く、画面ごとに必要データが大きく異なる場合 |
| **gRPC / Protocol Buffers** | 型安全、高速 | Android での HTTP/2 対応、学習コスト | マイクロサービス間通信と統一したい場合 |

> **社内ツール × 小売業 × Spring Boot の場合、OpenAPI が最もバランスが良い。**

---

### 6. 本ドキュメントの記述をどう読むか

本ドキュメント（および `architecture-practical-guide.md`）の設計パターンは、  
以下の読み替えを行うことでマルチレポ構成に適用できる。

| 本ドキュメントの記述 | マルチレポでの読み替え |
|---|---|
| `core/domain` の UseCase | モバイル: 表示変換 UseCase に限定。業務ルールはバックエンド |
| `core/data` の Repository（Offline First） | `feature/*/data/` の Repository（API-First + 軽量キャッシュ） |
| `core/database` の Room Entity/DAO | キャッシュ用途に限定。真のスキーマはバックエンドの Oracle |
| `feature/*/domain/` のビジネスルール UseCase | 表示ルール・フォームバリデーション・エラーメッセージ変換 |
| `feature/*/data/` の差分同期 | API 呼び出し + OkHttp / Room キャッシュ |
| 共通モデル（`core/model`） | API レスポンス型（`core/network/model/`）の比重が上がる |

垂直スライス構造、api/impl 分割、判断フローチャートなどは**そのまま有効**。
