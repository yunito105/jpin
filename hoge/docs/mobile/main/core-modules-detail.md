# core/ 共通基盤モジュール 全ファイル詳細解説

> **目的**: 提案構成の `core/` 配下の全モジュール・全ファイルについて、1ファイルずつ「何のために存在するのか」を解説する。
>
> **前提**: NiA (Now in Android) の実構成を元に、新規PJ向けに再設計した提案構成を解説する。

---

## 目次

1. [core/model — 共有データモデル](#1-coremodel--共有データモデル)
2. [core/common — 共通ユーティリティ](#2-corecommon--共通ユーティリティ)
3. [core/network — API 通信基盤](#3-corenetwork--api-通信基盤)
4. [core/data — Repository 基盤](#4-coredata--repository-基盤)
5. [core/database — ローカルキャッシュ](#5-coredatabase--ローカルキャッシュ)
6. [core/datastore — 設定永続化](#6-coredatastore--設定永続化)
7. [core/designsystem — デザインシステム](#7-coredesignsystem--デザインシステム)
8. [core/ui — 共通ビジネスコンポーネント](#8-coreui--共通ビジネスコンポーネント)
9. [core/navigation — ナビゲーション基盤](#9-corenavigation--ナビゲーション基盤)
10. [core/analytics — 分析基盤](#10-coreanalytics--分析基盤)
11. [core/testing — テスト共通基盤](#11-coretesting--テスト共通基盤)
12. [core/screenshot-testing — スクリーンショットテスト基盤](#12-corescreenshot-testing--スクリーンショットテスト基盤)
13. [指摘事項への回答: なぜ商品・受注モデルが core にあるのか](#13-指摘事項への回答)

---

## 1. core/model — 共有データモデル

### モジュールの役割

**Pure Kotlin（Android 依存なし）** のデータクラスを置くモジュール。  
アプリ全体で使う「共通語彙」を定義する。Spring Boot でいう共有 DTO パッケージに相当。

### なぜ独立モジュールなのか

- 複数モジュール（network, database, data, feature各種）が同じモデルを参照する
- Android SDK に依存しないため、JVMライブラリとしてビルドが高速
- モデル変更時の影響範囲が明確（このモジュールに依存する全モジュールが再ビルド対象）

### NiA の実装（参考）

NiA では以下が定義されている:

| ファイル | 内容 | 説明 |
|---------|------|------|
| `Topic.kt` | `data class Topic(id, name, shortDescription, longDescription, url, imageUrl)` | トピック（タグのようなもの） |
| `NewsResource.kt` | `data class NewsResource(id, title, content, url, headerImageUrl, publishDate, type, topics)` | ニュース記事本体 |
| `UserData.kt` | `data class UserData(bookmarkedNewsResources, viewedNewsResources, followedTopics, themeBrand, darkThemeConfig, useDynamicColor, shouldHideOnboarding)` | ユーザーのローカル設定全体 |
| `FollowableTopic.kt` | `data class FollowableTopic(topic, isFollowed)` | トピック + フォロー状態 |
| `UserNewsResource.kt` | `data class UserNewsResource(...)` + 拡張関数 `mapToUserNewsResources()` | ニュース + ユーザー状態（既読/ブックマーク） |
| `SearchResult.kt` | `data class SearchResult(topics, newsResources)` | 検索結果 |
| `UserSearchResult.kt` | `data class UserSearchResult(topics, newsResources)` | 検索結果 + ユーザー状態 |
| `ThemeBrand.kt` | `enum ThemeBrand { DEFAULT, ANDROID }` | テーマブランド定義 |
| `DarkThemeConfig.kt` | `enum DarkThemeConfig { FOLLOW_SYSTEM, LIGHT, DARK }` | ダークテーマ設定 |

### 新規PJ への提案: 問題の指摘と改善案

**指摘:**  
> 「共通基盤モジュールに商品や受注や在庫などがあるのは気になる」

これは**正当な懸念**。改善案は [13. 指摘事項への回答](#13-指摘事項への回答) で詳述。

### build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.retail.jvm.library) // Pure JVM（Androidなし）
}
dependencies {
    api(libs.kotlinx.datetime) // 日付型のみ外部依存
}
```

---

## 2. core/common — 共通ユーティリティ

### モジュールの役割

**全モジュールが使う最小限のユーティリティ**を置く場所。  
Spring Boot でいう `common-lib` や `shared-kernel` に相当。

### 全ファイル解説

| # | ファイル | 何をしているか | なぜ必要か |
|---|---------|---------------|-----------|
| 1 | **build.gradle.kts** | JVM ライブラリ + Hilt でビルド | Dispatcher提供に Hilt が必要 |
| 2 | **result/Result.kt** | `sealed interface Result<T>` — `Success(data)`, `Error(exception)`, `Loading` の3状態。`Flow<T>.asResult()` 拡張で自動変換 | API呼び出しやDB読み取りの結果を統一的に扱う。全 Repository/ViewModel で使う |
| 3 | **network/NiaDispatchers.kt** | `@Qualifier annotation class Dispatcher(val niaDispatchers: NiaDispatchers)` + `enum NiaDispatchers { Default, IO }` | Hilt で `@Dispatcher(IO)` のように特定スレッドプールを注入するための目印。Spring の `@Qualifier("io")` に相当 |
| 4 | **network/di/DispatchersModule.kt** | `@Provides` で `Dispatchers.IO` と `Dispatchers.Default` を Hilt に登録 | テスト時に TestDispatcher に差替え可能にする |
| 5 | **network/di/CoroutineScopesModule.kt** | `@ApplicationScope` アノテーション定義 + `CoroutineScope(SupervisorJob() + Default)` を提供 | アプリ全体のバックグラウンド処理用スコープ。Activity/Fragment のライフサイクルに依存しない処理に使う |

### ポイント

- **たった5ファイル**しかない。「common」が肥大化しないよう最小限に抑えている
- `Result.kt` は最も重要。全データ取得処理の「成功/エラー/読込中」を統一表現する

---

## 3. core/network — API 通信基盤

### モジュールの役割

**バックエンド API との HTTP 通信**を担当。  
Spring Boot でいう `RestTemplate` / `WebClient` のラッパークラス群に相当。

### 全ファイル解説

| # | ファイル | 何をしているか | なぜ必要か |
|---|---------|---------------|-----------|
| 1 | **build.gradle.kts** | Android lib + Hilt + kotlinx-serialization | Retrofit, OkHttp, Coil を依存に含む |
| 2 | **NiaNetworkDataSource.kt** | `interface NiaNetworkDataSource` — `getTopics()`, `getNewsResources()` 等を定義 | APIクライアントの共通インターフェース。実装を差替え可能にする（本番 vs Demo） |
| 3 | **model/NetworkTopic.kt** | `@Serializable data class NetworkTopic(...)` + `asExternalModel()` でcore/modelの`Topic`に変換 | **API レスポンスの DTO**。ネットワーク層のモデルとアプリ内モデルを分離する |
| 4 | **model/NetworkNewsResource.kt** | `@Serializable data class NetworkNewsResource(...)` | ニュース記事の API レスポンス DTO |
| 5 | **model/NetworkChangeList.kt** | `@Serializable data class NetworkChangeList(id, changeListVersion, isDelete)` | 差分同期用の変更リスト DTO（NiA 固有。新規PJでは不要） |
| 6 | **retrofit/RetrofitNiaNetwork.kt** | `class RetrofitNiaNetwork : NiaNetworkDataSource` — Retrofit 実装 | 本番（prod フレーバー）で使う実際の HTTP 通信 |
| 7 | **demo/DemoAssetManager.kt** | `fun interface DemoAssetManager { fun open(fileName): InputStream }` | アプリ内蔵 JSON を読むためのインターフェース |
| 8 | **demo/DemoNiaNetworkDataSource.kt** | `class DemoNiaNetworkDataSource : NiaNetworkDataSource` — ローカル JSON からデータ読み込み | demo フレーバーでサーバーなしで動作させるための実装 |
| 9 | **JvmUnitTestDemoAssetManager.kt** | JVMテスト用のアセット読み込み実装 | テスト時にAndroidのAssetManagerなしでJSONを読む |
| 10 | **di/NetworkModule.kt** | Hilt モジュール: `Json`, `DemoAssetManager`, `OkHttpClient`（ロギング付き）, `ImageLoader`（SVG対応）を提供 | DI で各コンポーネントを注入可能にする |

### 新規PJ での対応

新規PJでは `NiaNetworkDataSource` を **サブドメイン別の Retrofit API インターフェース** に置き換える:

```
core/network/
├── api/
│   ├── ProductApi.kt        # @GET("/products"), @POST("/products") 等
│   ├── OrderApi.kt
│   └── ...
├── interceptor/
│   ├── AuthInterceptor.kt   # Bearer Token 自動付与
│   └── ErrorInterceptor.kt  # 共通エラーハンドリング
├── model/
│   ├── ApiResponse.kt       # { "data": T, "error": null } 共通ラッパー
│   └── PaginatedResponse.kt # ページネーション
└── di/
    └── NetworkModule.kt      # Retrofit, OkHttp 提供
```

---

## 4. core/data — Repository 基盤

### モジュールの役割

**データ取得の一元窓口**。ViewModel は Repository だけを見る。  
Spring Boot の `@Service` 層に最も近い。

### 全ファイル解説

| # | カテゴリ | ファイル | 何をしているか |
|---|---------|---------|---------------|
| 1 | **同期** | `SyncUtilities.kt` | `interface Synchronizer`, `interface Syncable`, `changeListSync()` — サーバーとの差分同期ロジック |
| 2 | **マッピング** | `model/Topic.kt` | `NetworkTopic.asEntity()` — API DTO → Room Entity への変換 |
| 3 | **マッピング** | `model/NewsResource.kt` | `NetworkNewsResource.asEntity()`, `.topicCrossReferences()` — ニュースの変換 + 中間テーブルデータ生成 |
| 4 | **マッピング** | `model/RecentSearchQuery.kt` | `RecentSearchQueryEntity.asExternalModel()` — 検索履歴の変換 |
| 5 | **ユーティリティ** | `util/NetworkMonitor.kt` | `interface NetworkMonitor { val isOnline: Flow<Boolean> }` — ネットワーク接続監視 |
| 6 | **ユーティリティ** | `util/ConnectivityManagerNetworkMonitor.kt` | Android の ConnectivityManager を使った実装 |
| 7 | **ユーティリティ** | `util/SyncManager.kt` | `interface SyncManager { val isSyncing: Flow<Boolean>; fun requestSync() }` |
| 8 | **ユーティリティ** | `util/TimeZoneMonitor.kt` | タイムゾーン変更を監視。BroadcastReceiver で検知 |
| 9 | **リポジトリIF** | `repository/TopicsRepository.kt` | `interface TopicsRepository : Syncable` — `getTopics()`, `getTopic(id)` |
| 10 | **リポジトリIF** | `repository/NewsRepository.kt` | `interface NewsRepository : Syncable` — `getNewsResources(query)` |
| 11 | **リポジトリIF** | `repository/UserDataRepository.kt` | `interface UserDataRepository` — `userData: Flow<UserData>`, 各種設定変更メソッド |
| 12 | **リポジトリIF** | `repository/UserNewsResourceRepository.kt` | `interface UserNewsResourceRepository` — ニュース + ユーザー状態の結合 |
| 13 | **リポジトリIF** | `repository/SearchContentsRepository.kt` | `interface SearchContentsRepository` — 全文検索 |
| 14 | **リポジトリIF** | `repository/RecentSearchRepository.kt` | `interface RecentSearchRepository` — 検索履歴 |
| 15 | **実装** | `repository/OfflineFirstTopicsRepository.kt` | **オフラインファースト**実装。Room → API → Room の同期パターン |
| 16 | **実装** | `repository/OfflineFirstNewsRepository.kt` | バッチ同期（40件）、通知連携あり |
| 17 | **実装** | `repository/OfflineFirstUserDataRepository.kt` | DataStore 経由でユーザー設定を永続化 |
| 18 | **実装** | `repository/CompositeUserNewsResourceRepository.kt` | `NewsRepository` + `UserDataRepository` を `Flow.combine` で結合 |
| 19 | **実装** | `repository/DefaultSearchContentsRepository.kt` | Room FTS (全文検索) で検索実行 |
| 20 | **実装** | `repository/DefaultRecentSearchRepository.kt` | 検索履歴の保存・取得 |
| 21 | **拡張** | `repository/AnalyticsExtensions.kt` | 分析イベント送信のヘルパー関数群 |
| 22 | **DI** | `di/DataModule.kt` | 全リポジトリの `@Binds`（IF → 実装の紐付け） |
| 23 | **DI** | `di/UserNewsResourceRepositoryModule.kt` | CompositeUserNewsResourceRepository の紐付け |

### 新規PJ での変更点

NiA は **オフラインファースト**（Room にまず保存 → UI に表示 → バックグラウンドで API 同期）だが、  
新規PJ は **API ファースト**（API 呼び出し → 結果を直接 UI 表示）。

```
新規PJ の core/data/
├── repository/
│   ├── AuthRepository.kt              # IF: 認証
│   ├── DefaultAuthRepository.kt       # 実装: 認証
│   ├── UserSettingsRepository.kt      # IF: ユーザー設定
│   └── DefaultUserSettingsRepository.kt
├── mapper/
│   └── ResponseMapper.kt             # DTO → Model 共通変換
├── util/
│   ├── NetworkMonitor.kt             # ネットワーク監視IF
│   └── ConnectivityManagerNetworkMonitor.kt
└── di/
    └── DataModule.kt                  # Hilt バインディング
```

**重要**: 商品・受注などサブドメイン固有の Repository は **core/data には置かない**。  
→ [13. 指摘事項への回答](#13-指摘事項への回答) で詳述。

---

## 5. core/database — ローカルキャッシュ

### モジュールの役割

**端末内 SQLite データベース (Room)** を管理。  
Spring Boot でいう JPA の `@Entity` + `@Repository` に相当。

### NiA の全ファイル解説

| # | カテゴリ | ファイル | 何をしているか |
|---|---------|---------|---------------|
| 1 | **DB定義** | `NiaDatabase.kt` | `@Database(version=14)` — 6エンティティ、5 DAO、13回の自動マイグレーション |
| 2 | **マイグレーション** | `DatabaseMigrations.kt` | カラム名変更、テーブル削除等のマイグレーション仕様 |
| 3 | **変換** | `util/InstantConverter.kt` | `@TypeConverter` で `kotlinx.datetime.Instant` ↔ `Long` 相互変換 |
| 4 | **Entity** | `model/TopicEntity.kt` | トピックテーブル + `asExternalModel()` で core/model の `Topic` に変換 |
| 5 | **Entity** | `model/TopicFtsEntity.kt` | トピック全文検索インデックス（FTS4） |
| 6 | **Entity** | `model/NewsResourceEntity.kt` | ニュースリソーステーブル |
| 7 | **Entity** | `model/NewsResourceFtsEntity.kt` | ニュース全文検索インデックス（FTS4） |
| 8 | **Entity** | `model/NewsResourceTopicCrossRef.kt` | ニュース ↔ トピックの多対多中間テーブル（外部キー付き） |
| 9 | **Entity** | `model/PopulatedNewsResource.kt` | `@Embedded` + `@Relation` でニュース + トピック一覧をJOINして取得 |
| 10 | **Entity** | `model/RecentSearchQueryEntity.kt` | 検索履歴テーブル |
| 11 | **DAO** | `dao/TopicDao.kt` | トピック CRUD + upsert |
| 12 | **DAO** | `dao/TopicFtsDao.kt` | トピック全文検索 |
| 13 | **DAO** | `dao/NewsResourceDao.kt` | ニュース CRUD + 複合フィルタクエリ |
| 14 | **DAO** | `dao/NewsResourceFtsDao.kt` | ニュース全文検索 |
| 15 | **DAO** | `dao/RecentSearchQueryDao.kt` | 検索履歴 CRUD |
| 16 | **DI** | `di/DatabaseModule.kt` | `Room.databaseBuilder()` を Hilt に登録 |
| 17 | **DI** | `di/DaosModule.kt` | 5つの DAO を Hilt に登録 |

### 新規PJ での変更点

NiA は全データを Room に保存する**オフラインファースト**だが、  
新規PJ では Room は **限定的な用途** のみ:

- **下書き保存** — 入力中のフォームデータの一時保存
- **検索履歴** — 最近の検索クエリ
- **汎用キャッシュ** — オフライン時の最低限のデータ保持

```
新規PJ の core/database/
├── RetailDatabase.kt       # Room Database（3テーブル程度）
├── dao/
│   ├── DraftDao.kt         # 下書き CRUD
│   ├── RecentSearchDao.kt  # 検索履歴 CRUD
│   └── CacheDao.kt         # 汎用キャッシュ CRUD
└── model/
    ├── DraftEntity.kt
    ├── RecentSearchEntity.kt
    └── CacheEntry.kt
```

---

## 6. core/datastore — 設定永続化

### モジュールの役割

**Key-Value ストア** でユーザー設定を永続化。  
Spring Boot でいう `application.properties` のユーザー版。ただしアプリ実行中にリアクティブに変更を検知できる。

### NiA の全ファイル解説

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **build.gradle.kts** | core:datastore-proto（Protobuf定義）に依存 |
| 2 | **ChangeListVersions.kt** | `data class ChangeListVersions(topicVersion, newsResourceVersion)` — 同期で最後に取得したバージョン番号 |
| 3 | **NiaPreferencesDataSource.kt** | ユーザー設定の読み書き全体を管理するクラス。フォロー中トピック、ブックマーク、既読、テーマ、ダーク/ライト、ダイナミックカラー、オンボーディング状態、同期バージョンを管理 |
| 4 | **UserPreferencesSerializer.kt** | Protocol Buffers のシリアライザ。DataStore がファイルに書き込む際の形式を定義 |
| 5 | **IntToStringIdsMigration.kt** | データマイグレーション: Int型ID → String型ID への変換 |
| 6 | **ListToMapMigration.kt** | データマイグレーション: List → Map への変換（検索効率向上） |
| 7 | **di/DataStoreModule.kt** | Hilt で `DataStore<UserPreferences>` を提供。マイグレーション設定も含む |

### 関連モジュール: core/datastore-proto

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **build.gradle.kts** | JVM ライブラリ + Protobuf プラグイン |
| 2 | **user_preferences.proto** | `message UserPreferences` — Protobuf スキーマ定義。全ユーザー設定フィールド |
| 3 | **theme_brand.proto** | `enum ThemeBrandProto` のスキーマ |
| 4 | **dark_theme_config.proto** | `enum DarkThemeConfigProto` のスキーマ |

### 関連モジュール: core/datastore-test

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **InMemoryDataStore.kt** | `MutableStateFlow` ベースのインメモリ DataStore 実装。テスト用 |
| 2 | **TestDataStoreModule.kt** | Hilt の `@TestInstallIn` で本番DataStoreをインメモリ版に差替え |

### 新規PJ での構成

```
core/datastore/
├── UserPreferencesDataSource.kt  # テーマ、言語、表示設定等
├── TokenDataSource.kt            # 認証トークン管理（暗号化DataStore使用）
└── di/
    └── DataStoreModule.kt
```

---

## 7. core/designsystem — デザインシステム

### モジュールの役割

**アプリ全体の見た目の統一ルール**を定義。  
Spring Boot にはない概念。フロントエンドの**コンポーネントライブラリ** (Storybook) に近い。

### 全ファイル解説

#### theme/ — テーマ定義

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **Theme.kt** | `NiaTheme()` — Material 3 テーマの統合設定。4つのカラースキーム(ライト/ダーク × デフォルト/Android)。ダイナミックカラー対応 |
| 2 | **Color.kt** | 80以上のカラー定数定義（Blue10〜Blue90, Green10〜Green90 等）。アプリ全体で使う色パレット |
| 3 | **Type.kt** | タイポグラフィ設定。フォントサイズ・行間・ウェイトを全テキストスタイルに定義（display/headline/title/body/label） |
| 4 | **Gradient.kt** | グラデーション背景色の定義。`data class GradientColors(top, bottom, container)` |
| 5 | **Tint.kt** | アイコンの色調設定。`data class TintTheme(iconTint)` |
| 6 | **Background.kt** | 背景テーマ設定。`data class BackgroundTheme(color, tonalElevation)` |

#### icon/ — アイコン

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 7 | **NiaIcons.kt** | `object NiaIcons` — アプリ内で使うアイコンの一覧定義。Add, ArrowBack, Bookmark, Search, Settings 等 |

#### component/ — 再利用可能UIパーツ

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 8 | **Button.kt** | `NiaButton()`, `NiaOutlinedButton()`, `NiaTextButton()` — 3種のボタン |
| 9 | **Chip.kt** | `NiaFilterChip()` — フィルター用チップ（選択時にチェックアイコン表示） |
| 10 | **DynamicAsyncImage.kt** | `DynamicAsyncImage()` — URL からの非同期画像読み込み + ローディング表示 |
| 11 | **IconButton.kt** | `NiaIconToggleButton()` — ON/OFF 切替のアイコンボタン |
| 12 | **LoadingWheel.kt** | `NiaLoadingWheel()` — 12本線の回転アニメーション。読込中表示用 |
| 13 | **Navigation.kt** | `NiaNavigationBar()`, `NiaNavigationRail()`, `NiaNavigationSuiteScaffold()` — 画面下部/サイドのナビゲーション。スマホ/タブレット自動切替 |
| 14 | **Tabs.kt** | `NiaTab()`, `NiaTabRow()` — タブ切替UI |
| 15 | **Tag.kt** | `NiaTopicTag()` — トピックタグ（フォロー状態で色変化） |
| 16 | **TopAppBar.kt** | `NiaTopAppBar()` — 画面上部のバー。タイトル + 戻るボタン + アクションボタン |
| 17 | **ViewToggle.kt** | `NiaViewToggleButton()` — リスト表示/グリッド表示切替ボタン |
| 18 | **Background.kt** | `NiaBackground()` — アプリの基本背景。`NiaGradientBackground()` — グラデーション版 |

#### component/scrollbar/ — スクロールバー

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 19 | **Scrollbar.kt** | スクロールバーの基盤実装。ドラッグ/タップ/長押し対応 |
| 20 | **AppScrollbars.kt** | Active/Inactive/Dormant 状態のアニメーション付きスクロールバー |
| 21 | **LazyScrollbarUtilities.kt** | LazyList のアイテム位置を補間してスクロールバー位置を計算 |
| 22 | **ScrollbarExt.kt** | `LazyListState.scrollbarState()` 等の拡張関数 |
| 23 | **ThumbExt.kt** | スクロールバードラッグ時のスクロール位置連動 |

### 新規PJ への対応

`NiaXxx` を `RetailXxx` にリネームし、新規PJ のブランドカラー・タイポグラフィに差替え。  
スクロールバー等の汎用コンポーネントは流用可能。

---

## 8. core/ui — 共通ビジネスコンポーネント

### モジュールの役割

**ビジネスドメインに紐づいた共通UIパーツ**。  
`designsystem` が「ボタン」「カード」等の汎用パーツなのに対し、  
`ui` は「ニュースカード」「トピック一覧アイテム」等の**アプリ固有パーツ**。

### NiA の全ファイル解説

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **NewsResourceCard.kt** | ニュース記事のカード表示。ヘッダー画像、タイトル、ブックマークボタン、トピックタグ一覧、共有ボタンを含む複合コンポーネント |
| 2 | **NewsFeed.kt** | ニュースフィード全体。`LazyStaggeredGrid` にニュースカードを配置。Chrome Custom Tabs でリンクを開く |
| 3 | **NewsResourceCardList.kt** | ニュースカードのリスト表示版 |
| 4 | **InterestsItem.kt** | トピック（興味）の1アイテム表示。アイコン + 名前 + フォロートグル |
| 5 | **LocalTimeZone.kt** | `CompositionLocal` でタイムゾーンを提供。日時表示の一貫性確保 |
| 6 | **JankStatsExtensions.kt** | パフォーマンス監視。フレーム落ちを検出 |
| 7 | **DevicePreviews.kt** | `@DevicePreviews` アノテーション。Phone/Landscape/Foldable/Tablet/Desktop の5デバイスでプレビュー表示 |
| 8 | **AnalyticsExtensions.kt** | 分析イベント送信のCompose用ヘルパー。`TrackScreenViewEvent` で画面表示をログ |
| 9 | **UserNewsResourcePreviewParameterProvider.kt** | Compose Preview 用のサンプルデータ提供 |
| 10 | **FollowableTopicPreviewParameterProvider.kt** | Compose Preview 用のサンプルトピックデータ |

### 新規PJ での変更

NiA のニュース系コンポーネントを、リテール業務で使う共通コンポーネントに置換:

```
core/ui/
├── ProductCard.kt           # 商品カード（画像+名前+価格）
├── OrderStatusBadge.kt      # 注文ステータスバッジ
├── StoreSelector.kt         # 店舗選択ドロップダウン
├── QuantitySelector.kt      # 数量 +/- 選択
├── PriceDisplay.kt          # 価格表示（通貨フォーマット付き）
├── PaginatedList.kt         # ページング付きリスト
├── PullRefreshContainer.kt  # Pull-to-Refresh ラッパー
├── SearchResultItem.kt      # 検索結果1アイテム
├── ImageGallery.kt          # 商品画像ギャラリー
└── DevicePreviews.kt        # マルチデバイスプレビュー
```

---

## 9. core/navigation — ナビゲーション基盤

### モジュールの役割

**画面遷移のルール管理**。  
Spring Boot でいう `DispatcherServlet` のルーティングテーブルに相当。

### NiA の全ファイル解説

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **build.gradle.kts** | Navigation 3 API, Compose, kotlin-serialization に依存 |
| 2 | **NavigationState.kt** | `class NavigationState` — トップレベルスタック + サブスタックの管理。`currentTopLevelKey` / `currentKey` を derived state で提供。`toEntries()` で Navigation3 の `DecoratedNavEntries` に変換 |
| 3 | **Navigator.kt** | `class Navigator(state)` — `navigate(key)` でトップレベル or サブスタックに振り分け。`goBack()` でバック。`clearSubStack()` でサブスタックリセット |

### 新規PJ での対応

NiA の構成をそのまま踏襲。トップレベルナビゲーション項目を新規PJ用に変更するだけ。

---

## 10. core/analytics — 分析基盤

### モジュールの役割

**ユーザー行動分析イベントの送信**。  
Spring Boot でいう Logging + Metrics（Micrometer）に相当。

### NiA の全ファイル解説

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **AnalyticsEvent.kt** | `data class AnalyticsEvent(type, extras)` — イベントモデル。`SCREEN_VIEW` 等の定数 |
| 2 | **AnalyticsHelper.kt** | `interface AnalyticsHelper { fun logEvent(event) }` — 分析送信インターフェース |
| 3 | **StubAnalyticsHelper.kt** | demo フレーバー用: Logcat にイベント出力 |
| 4 | **NoOpAnalyticsHelper.kt** | テスト/プレビュー用: 何もしない実装 |
| 5 | **UiHelpers.kt** | `LocalAnalyticsHelper` — Compose の `CompositionLocal` で AnalyticsHelper をコンポーネントツリーに提供 |

### 新規PJ

構成は同じ。Firebase Analytics の代わりに自社分析基盤を使う場合は `AnalyticsHelper` の実装を差替え。

---

## 11. core/testing — テスト共通基盤

### モジュールの役割

**テスト用のフェイク実装、テストデータ、DI差替え**を集約。  
Spring Boot でいう `@MockBean` や TestFixture に相当。

### 全ファイル解説

#### DI 差替え

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **di/TestDispatcherModule.kt** | `UnconfinedTestDispatcher` を提供 |
| 2 | **di/TestDispatchersModule.kt** | 本番 DispatchersModule を TestDispatcher に差替え |

#### ユーティリティ

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 3 | **util/MainDispatcherRule.kt** | JUnit テストルール: `Dispatchers.Main` を TestDispatcher に差替え |
| 4 | **util/TestAnalyticsHelper.kt** | 分析イベントを `MutableList` に記録。`hasLogged(event)` で検証 |
| 5 | **util/TestNetworkMonitor.kt** | `MutableStateFlow(true)` でオンライン状態をコントロール |
| 6 | **util/TestSyncManager.kt** | 同期状態をコントロール |
| 7 | **util/TestTimeZoneMonitor.kt** | タイムゾーンをコントロール |

#### フェイクリポジトリ（テスト用の偽物）

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 8 | **repository/TestUserDataRepository.kt** | `MutableSharedFlow<UserData>` でテスト中にデータを流し込める |
| 9 | **repository/TestTopicsRepository.kt** | `sendTopics()` でテストデータを注入 |
| 10 | **repository/TestNewsRepository.kt** | ニュースデータ注入 + フィルタ対応 |
| 11 | **repository/TestSearchContentsRepository.kt** | 検索テスト用 |
| 12 | **repository/TestRecentSearchRepository.kt** | 検索履歴テスト用 |

#### テスト通知

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 13 | **notifications/TestNotifier.kt** | 通知送信を記録し、呼び出し回数を検証用 |

#### テストデータ

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 14 | **data/TopicsTestData.kt** | サンプルトピック3件 (Headlines, UI, Testing) |
| 15 | **data/NewsResourcesTestData.kt** | サンプルニュース4件 |
| 16 | **data/FollowableTopicTestData.kt** | フォロー状態付きトピック |
| 17 | **data/UserNewsResourcesTestData.kt** | ブックマーク/既読状態付きニュース |

#### その他

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 18 | **NiaTestRunner.kt** | Hilt用カスタムテストランナー。`HiltTestApplication` を使用 |
| 19 | **GrantPostNotificationsPermissionRule.kt** | API 33+の通知権限を自動付与するテストルール |

---

## 12. core/screenshot-testing — スクリーンショットテスト基盤

### モジュールの役割

**UIの見た目が意図せず変わっていないかを画像比較で検証**する。  
CI で自動実行し、差分があればテスト失敗。

### ファイル解説

| # | ファイル | 何をしているか |
|---|---------|---------------|
| 1 | **build.gradle.kts** | Roborazzi（ロボラッチ）+ アクセシビリティチェックに依存 |
| 2 | **ScreenshotHelper.kt** | `captureMultiDevice()` — Phone/Foldable/Tablet の3デバイスでスクリーンショット。`captureMultiTheme()` — light/dark × default/android × dynamic の全組み合わせ（最大6パターン）。アクセシビリティチェックも自動実行 |

---

## 13. 指摘事項への回答

### 指摘: 「共通基盤モジュールに商品や受注などがあるのは気になる」

#### 問題の本質

元の提案では `core/model/` に以下のようなファイルを置いていた:

```
core/model/
├── Product.kt      # 商品
├── Order.kt        # 受注
├── Inventory.kt    # 在庫
├── Store.kt        # 店舗
├── Shipment.kt     # 出荷
└── ...
```

これに対する懸念は**妥当**。理由:

1. **サブドメインの独立性が損なわれる** — 商品サブドメインの変更が全サブドメインの再ビルドを引き起こす
2. **単一責務の原則に反する** — core/model が「なんでも入れ」のゴミ箱になるリスク
3. **バックエンドの DDD と整合しない** — バックエンド側は各サブドメイン内にモデルを持つはず

#### NiA でモデルが core にある理由

NiA は **単一ドメイン（ニュース配信）** で、モデルが2種類しかない（Topic, NewsResource）。  
全画面がこの2モデルを使うため、core に集約しても問題なかった。

#### 改善案: 2段階のモデル配置

```
core/model/                      # ← 本当に全サブドメインで共通のモデルのみ
├── User.kt                      # ユーザー（全画面で使う）
├── AppSettings.kt               # アプリ設定
└── enums/
    └── UserRole.kt

feature/sales/                   # ← サブドメイン固有モデルは feature 内
├── order-list/impl/
│   └── model/
│       └── OrderListUiState.kt  # 画面固有の表示用モデル
├── order-create/impl/
│   └── model/
│       └── OrderCreateUiState.kt
└── shared/                      # ← サブドメイン内共有モデル（新設）
    └── src/main/kotlin/.../
        ├── Order.kt             # 受注モデル（sales 内で共有）
        ├── OrderStatus.kt
        └── OrderLine.kt
```

**原則:**
- `core/model` → 全アプリ共通（ユーザー、設定など）
- `feature/{domain}/shared` → サブドメイン内共有モデル
- `feature/{domain}/{screen}/impl/model` → 画面固有UIステート

この改善は [feature 構成改訂](./feature-structure-revised.md) で反映。

### 同様に core/network の API も移動すべき

元の提案では `core/network/api/` に全サブドメインの API を置いていたが、これも改善:

```
# Before（元提案）
core/network/api/
├── ProductApi.kt
├── OrderApi.kt
├── InventoryApi.kt
└── ...

# After（改善案）
core/network/            # ← 共通基盤のみ
├── di/NetworkModule.kt  # Retrofit, OkHttp 提供
├── interceptor/         # 認証、エラーハンドリング
└── model/               # ApiResponse, PaginatedResponse 等

feature/sales/shared/    # ← サブドメイン固有
├── api/OrderApi.kt
└── model/OrderDto.kt
```

### core/data の Repository も同様

```
core/data/              # ← 共通のみ
├── repository/
│   ├── AuthRepository.kt
│   └── UserSettingsRepository.kt
└── mapper/ResponseMapper.kt

feature/sales/shared/   # ← サブドメイン固有
├── repository/
│   ├── OrderRepository.kt
│   └── DefaultOrderRepository.kt
└── mapper/OrderMapper.kt
```

---

## まとめ: core に置くべきもの / 置くべきでないもの

| 配置先 | 置くもの | 例 |
|--------|---------|-----|
| `core/model` | **全アプリ共通**のモデル | User, AppSettings, UserRole |
| `core/common` | Result型、Dispatcher、CoroutineScope | Result.kt, NiaDispatchers.kt |
| `core/network` | HTTP通信の**共通基盤**のみ | Retrofit設定, Interceptor, ApiResponse |
| `core/data` | **共通**のRepository | AuthRepository, UserSettingsRepository |
| `core/database` | **汎用的な**ローカルストレージ | 下書き、検索履歴、キャッシュ |
| `core/datastore` | アプリ設定の永続化 | テーマ、言語、認証トークン |
| `core/designsystem` | デザインパーツ（ドメイン非依存） | Button, Card, Theme, Color |
| `core/ui` | **複数サブドメインで使う**ビジネスUI | PriceDisplay, PaginatedList |
| `core/navigation` | ナビゲーション基盤 | Navigator, NavigationState |
| `core/analytics` | 分析イベント送信 | AnalyticsHelper |
| `core/testing` | テスト共通基盤 | フェイク、テストデータ、DI差替え |
| `feature/{domain}/shared` | **サブドメイン固有**のモデル・API・Repository | Order, ProductApi, OrderRepository |
| `feature/{domain}/{screen}/impl` | **画面固有**のUI・ViewModel・UiState | OrderListScreen, OrderListViewModel |
