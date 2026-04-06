# モバイル新規プロジェクト サンプル構成提案書

## 目次

1. [はじめに](#1-はじめに)
2. [Now in Android (NiA) 現状構成の詳細分析](#2-now-in-android-nia-現状構成の詳細分析)
   - 2.1 [トップレベルファイル](#21-トップレベルファイル)
   - 2.2 [build-logic（ビルド規約）モジュール](#22-build-logicビルド規約モジュール)
   - 2.3 [app モジュール](#23-app-モジュール)
   - 2.4 [core モジュール群](#24-core-モジュール群)
   - 2.5 [feature モジュール群](#25-feature-モジュール群)
   - 2.6 [sync モジュール](#26-sync-モジュール)
   - 2.7 [その他モジュール](#27-その他モジュール)
3. [NiA と新規PJ の差異・判断ポイント](#3-nia-と新規pj-の差異判断ポイント)
4. [新規PJ 向けモバイルディレクトリ構成提案](#4-新規pj-向けモバイルディレクトリ構成提案)
5. [各モジュール詳細設計](#5-各モジュール詳細設計)
6. [データフロー・アーキテクチャ図](#6-データフローアーキテクチャ図)
7. [サブドメイン分割戦略](#7-サブドメイン分割戦略)
8. [テスト戦略](#8-テスト戦略)
9. [判断が必要な技術選定一覧](#9-判断が必要な技術選定一覧)

---

## 1. はじめに

### 本書の目的

25億規模の海外販売システム構築PJにおけるモバイルアプリケーション（Kotlin / Android）のサンプルプロジェクト構成を提案する。

### PJ 前提条件

| 項目 | 内容 |
|------|------|
| 規模 | 25億規模、海外販売システム |
| 業界 | 小売業（IKEA/ニトリ/カインズ型業態） |
| システム特性 | 社内ツール群、複数サブドメイン、多画面 |
| バックエンド | Java / Spring Boot / ヘキサゴナルアーキテクチャ + 簡易CQRS / DDD |
| DB | Oracle |
| モバイル | Kotlin / MVVM + UDF |
| リポジトリ戦略 | マルチレポ（フロント / モバイル / バックエンド） |
| バックエンドの責務 | 業務ロジック（ドメイン駆動設計） |
| モバイルの責務 | 表示ルール、UI/UX |
| モバイル制約 | UseCase 層を使用しない（業務ロジックはバックエンド責務のため） |

---

## 2. Now in Android (NiA) 現状構成の詳細分析

### 2.1 トップレベルファイル

| ファイル | 役割 | なぜこの構成か | 新規PJ への考慮 |
|---------|------|--------------|----------------|
| `build.gradle.kts` | ルートプロジェクトの Gradle ビルドファイル。全サブプロジェクトで使用するプラグイン（Android, Kotlin, Hilt, Firebase, Compose, Room, Spotless 等）をバージョン統一で宣言 | マルチモジュール構成では各モジュールが独立してビルドされるため、プラグインバージョンの一元管理が必要 | **必須採用**。マルチモジュール構成のプロジェクトでは不可欠。特に大規模PJではバージョン不整合によるビルド障害を防ぐ |
| `settings.gradle.kts` | 全モジュールの登録、リポジトリ設定、JDK バージョン強制（17+）、`TYPESAFE_PROJECT_ACCESSORS` 有効化 | モジュール一覧を一箇所で管理し、型安全なモジュール参照を可能にする | **必須採用**。`TYPESAFE_PROJECT_ACCESSORS` により `:core:data` → `projects.core.data` のような型安全参照が可能になり、モジュール名のタイポを防ぐ |
| `gradle.properties` | JVM ヒープサイズ（4GB）、G1GC、並列ビルド、Configuration Cache 有効化、Kotlin コードスタイル設定 | 大規模プロジェクトのビルドパフォーマンス最適化 | **必須採用**。大規模モジュール構成ではビルド時間が長くなるため、並列ビルド・キャッシュは必須 |
| `gradle/libs.versions.toml` | 50以上の依存ライブラリのバージョンを一元管理する Version Catalog | 全モジュールが同じライブラリバージョンを使用することを保証 | **必須採用**。ライブラリ更新時に1ファイルの変更で全モジュールに反映される |
| `compose_compiler_config.conf` | Compose Compiler 向けの安定性マーキング。`java.time.*` やデータモデルクラスを Stable として宣言 | Compose の recomposition 最適化。不要な再描画を防止する | **必須採用**。パフォーマンスに直結する。特に多画面アプリでは重要 |
| `gradlew` / `gradlew.bat` | Gradle Wrapper。プロジェクト固有の Gradle バージョンを保証 | 開発者間・CI 環境での Gradle バージョン統一 | **必須採用**。標準的なプラクティス |
| `spotless/copyright.*` | コードフォーマッター Spotless のテンプレート（`.kt`, `.kts`, `.xml`） | コードスタイルの自動統一 | **推奨採用**。大規模チームでのコードレビュー負荷軽減 |

### 2.2 build-logic（ビルド規約）モジュール

```
build-logic/
├── settings.gradle.kts          # build-logic 自体の設定
├── gradle.properties            # build-logic 用 Gradle プロパティ
└── convention/
    ├── build.gradle.kts         # Convention Plugin の依存定義
    └── src/main/kotlin/com/google/samples/apps/nowinandroid/
        ├── AndroidApplicationConventionPlugin.kt      # アプリモジュール基本設定
        ├── AndroidApplicationComposeConventionPlugin.kt # Compose コンパイラ設定
        ├── AndroidApplicationFlavorsConventionPlugin.kt # demo/prod フレーバー定義
        ├── AndroidApplicationJacocoConventionPlugin.kt  # カバレッジ計測
        ├── AndroidApplicationFirebaseConventionPlugin.kt # Firebase 統合
        ├── AndroidLibraryConventionPlugin.kt           # ライブラリモジュール基本設定
        ├── AndroidLibraryComposeConventionPlugin.kt    # ライブラリ Compose 設定
        ├── AndroidLibraryJacocoConventionPlugin.kt     # ライブラリカバレッジ
        ├── AndroidFeatureApiConventionPlugin.kt        # Feature API 層（最小依存）
        ├── AndroidFeatureImplConventionPlugin.kt       # Feature Impl 層（全 core 依存）
        ├── AndroidTestConventionPlugin.kt             # 計装テスト設定
        ├── HiltConventionPlugin.kt                    # Hilt DI + KSP 設定
        ├── JvmLibraryConventionPlugin.kt              # Pure Kotlin/JVM モジュール
        ├── AndroidLintConventionPlugin.kt             # カスタム Lint ルール
        ├── AndroidRoomConventionPlugin.kt             # Room DB スキーマ生成
        ├── RootPlugin.kt                              # ルートプロジェクト集約タスク
        ├── NiaBuildType.kt                            # ビルドタイプ定義
        ├── NiaFlavor.kt                               # フレーバー定義
        ├── AndroidCompose.kt                          # Compose 共通設定ヘルパー
        ├── KotlinAndroid.kt                           # Kotlin/Android 共通設定
        ├── Jacoco.kt                                  # Jacoco ヘルパー
        ├── Spotless.kt                                # フォーマッター設定
        ├── GradleManagedDevices.kt                    # テストデバイス管理
        ├── Badging.kt                                 # APK バッジング検証
        └── Graph.kt                                   # モジュール依存グラフ生成
```

#### なぜ Convention Plugin を使うのか

- **DRY 原則**: 25以上のモジュールで同じビルド設定を繰り返さない
- **設定の標準化**: `minSdk=24`, `targetSdk=35`, Compose 有効化などの設定をプラグイン適用1行で統一
- **新モジュール追加の容易さ**: 新しい feature モジュールを追加する際、`id("nowinandroid.android.feature.impl")` を書くだけで全設定が適用される

#### 新規PJ への考慮

**必須採用**。大規模プロジェクトでは Convention Plugin がないと以下の問題が起きる：

- 各モジュールの `build.gradle.kts` が肥大化し保守困難になる
- 設定の不整合が発生しやすくなる（あるモジュールだけ `minSdk` が異なる等）
- ビルド設定変更時に全モジュールを手動更新する必要がある

ただし、NiA の Convention Plugin はそのまま採用できない。新規PJ 固有の設定に合わせてカスタマイズが必要：

| NiA の Convention Plugin | 新規PJ での要否 | 理由 |
|--------------------------|----------------|------|
| AndroidApplication* | 必要 | アプリモジュール設定は共通 |
| AndroidLibrary* | 必要 | core モジュール設定は共通 |
| AndroidFeatureApi/Impl | 必要 | Feature モジュール二層分離は大規模PJにこそ必要 |
| HiltConventionPlugin | 必要 | DI 設定の統一 |
| AndroidRoomConventionPlugin | **要検討** | ローカル DB の必要性次第（後述） |
| FirebaseConventionPlugin | **要検討** | Firebase 使用の判断次第 |
| JacocoConventionPlugin | 推奨 | テストカバレッジ計測は大規模PJで重要 |

### 2.3 app モジュール

```
app/
├── build.gradle.kts              # アプリレベルビルド設定（applicationId, versionCode 等）
├── proguard-rules.pro            # R8/ProGuard 難読化ルール
├── google-services.json          # Firebase 設定ファイル
├── prodRelease-badging.txt       # リリース APK メタデータ検証
├── benchmark-rules.pro           # ベンチマーク用 ProGuard ルール
├── dependencies/
│   └── prodReleaseRuntimeClasspath.txt  # 依存ロックファイル（Dependency Guard）
└── src/
    ├── main/
    │   ├── AndroidManifest.xml    # シングルアクティビティ宣言、権限
    │   └── kotlin/.../
    │       ├── MainActivity.kt            # 唯一のアクティビティ（Compose ホスト）
    │       ├── MainActivityViewModel.kt   # アクティビティレベルの状態管理（テーマ等）
    │       ├── NiaApplication.kt          # Application クラス（Hilt エントリポイント）
    │       ├── di/
    │       │   └── JankStatsModule.kt     # パフォーマンス監視 DI
    │       ├── ui/
    │       │   ├── NiaApp.kt              # ルート Composable（ナビゲーション、スキャフォールド）
    │       │   └── NiaAppState.kt         # アプリ全体の状態ホルダー（ネットワーク監視等）
    │       ├── navigation/
    │       │   └── TopLevelNavItem.kt     # ボトムナビゲーション項目定義
    │       └── util/                      # ユーティリティ拡張関数
    ├── androidTest/kotlin/...             # 計装テスト（Espresso, Compose）
    ├── testDemo/kotlin/...                # スクリーンショットテスト（Roborazzi）
    ├── debug/                             # デバッグ用 Manifest
    ├── prod/                              # プロダクション固有設定
    ├── demoRelease/                       # デモリリース固有
    └── prodRelease/                       # プロダクションリリース固有
```

#### 各ファイルの詳細

| ファイル | 何をしているか | なぜそうしているか |
|---------|--------------|------------------|
| `MainActivity.kt` | Compose UI のホストとなる唯一のアクティビティ。`setContent {}` で `NiaApp` を起動 | シングルアクティビティアーキテクチャ。画面遷移を Navigation で管理し、アクティビティライフサイクルの複雑さを排除 |
| `MainActivityViewModel.kt` | スプラッシュ画面中のデータ読み込み、テーマ設定の状態管理 | Activity スコープの状態（テーマはアプリ全体に影響するため Activity レベル） |
| `NiaApplication.kt` | `@HiltAndroidApp` アノテーション付き Application クラス | Hilt DI のルートコンテナ。アプリのライフサイクル全体で DI グラフを維持 |
| `NiaApp.kt` | `NavigationSuiteScaffold` + `NavDisplay` でボトムナビ / ドロワーナビ / タブナビを画面サイズに応じて自動切替。`Scaffold` でトップバー・コンテンツ領域を定義 | Adaptive Layout。タブレットでは Rail ナビ、フォルダブルでは List-Detail が自動的に有効になる |
| `NiaAppState.kt` | ネットワーク接続監視、未読バッジ計算、タイムゾーン変更検知を StateFlow で管理 | Composable 外で状態を管理し、再構成（recomposition）を最小化 |
| `TopLevelNavItem.kt` | ボトムナビの各タブ（ForYou, Bookmarks, Interests）のアイコン・ラベル・ルート定義 | ナビゲーション項目を一箇所で管理し、追加・変更を容易にする |

#### 新規PJ への考慮

- **シングルアクティビティ**: **必須採用**。現在の Android 開発の標準。
- **NiaApp（ルートスキャフォールド）**: **必須採用**。ただし、サブドメインが複数ある場合、ナビゲーション構造を拡張する必要あり（ドロワーメニュー等）
- **NiaAppState**: **必須採用**。ネットワーク監視は業務アプリで必須。
- **demo/prod フレーバー**: **要検討**。NiA は demo でオフラインモックデータを使用。新規PJでは「開発用モックフレーバー + STG + PROD」の3環境が想定される

### 2.4 core モジュール群

#### 2.4.1 core:model

```
core/model/
├── build.gradle.kts              # JVM ライブラリ（Android 非依存）
└── src/main/kotlin/.../core/model/data/
    ├── NewsResource.kt           # ニュース記事データクラス
    ├── Topic.kt                  # トピック/カテゴリ
    ├── UserData.kt               # ユーザー設定（テーマ、ブックマーク等）
    ├── FollowableTopic.kt        # トピック + フォロー状態
    ├── UserNewsResource.kt       # ニュース + ユーザーコンテキスト
    ├── SearchResult.kt           # 検索結果
    ├── UserSearchResult.kt       # 検索結果 + ユーザーコンテキスト
    ├── DarkThemeConfig.kt        # テーマ設定 enum
    └── ThemeBrand.kt             # ブランドテーマ enum
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | アプリ全体で共有するイミュータブルなデータクラスを定義。`kotlinx-datetime` の `Instant` を使用し、タイムゾーン安全な日時を扱う |
| **なぜこの構成か** | Pure JVM ライブラリにすることで Android SDK への依存を排除し、ユニットテストの実行速度を最大化。Compose Compiler Config で Stable マークすることで UI パフォーマンスも担保 |
| **新規PJ への考慮** | **必須採用**。ただし、NiA は model に「UI 表示用に結合した複合モデル」（`UserNewsResource` 等）も含む。新規PJでは **API レスポンスモデル（DTO）と UI モデルを分離**することを推奨。バックエンドの API レスポンスがそのまま表示用に使えるケースと、モバイル側で表示用に変換が必要なケースを区別する |

#### 2.4.2 core:network

```
core/network/
├── build.gradle.kts              # Android ライブラリ（Retrofit, OkHttp, Kotlinx Serialization）
└── src/main/kotlin/.../core/network/
    ├── NiaNetworkDataSource.kt          # ネットワークサービスインターフェース
    ├── di/
    │   └── NetworkModule.kt             # Hilt: Retrofit, OkHttp 提供
    ├── retrofit/
    │   └── RetrofitNiaNetwork.kt        # Retrofit 実装
    ├── model/
    │   ├── NetworkNewsResource.kt       # ネットワーク DTO
    │   ├── NetworkTopic.kt              # ネットワーク DTO
    │   └── NetworkChangeList.kt         # 同期変更リスト DTO
    ├── demo/
    │   ├── DemoNiaNetworkDataSource.kt  # モックデータソース（demo フレーバー）
    │   └── DemoAssetManager.kt          # Assets からの JSON 読み込み
    └── JvmUnitTestDemoAssetManager.kt   # テスト用アセットマネージャー
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | バックエンド API との通信を抽象化。`NiaNetworkDataSource` インターフェースに対し、`RetrofitNiaNetwork`（実API）と `DemoNiaNetworkDataSource`（モック）の2実装を提供 |
| **なぜこの構成か** | Repository Pattern のデータソースとして機能。インターフェース分離により demo フレーバーではネットワーク通信なしで動作可能 |
| **新規PJ への考慮** | **必須採用（大幅カスタマイズ）**。新規PJの核心モジュール。バックエンド API（Spring Boot）との通信はここに集約。NiA との主な違い：API 数が圧倒的に多い → **サブドメイン単位で API サービスインターフェースを分割**すべき。また、認証（OAuth2/JWT）、リフレッシュトークン、API エラーハンドリング、リトライポリシーが必要 |

#### 2.4.3 core:data

```
core/data/
├── build.gradle.kts              # Android ライブラリ（依存: database, datastore, network, model）
└── src/main/kotlin/.../core/data/
    ├── repository/
    │   ├── NewsRepository.kt                     # インターフェース
    │   ├── OfflineFirstNewsRepository.kt         # オフラインファースト実装
    │   ├── TopicsRepository.kt                   # インターフェース
    │   ├── OfflineFirstTopicsRepository.kt       # オフラインファースト実装
    │   ├── UserDataRepository.kt                 # インターフェース
    │   ├── OfflineFirstUserDataRepository.kt     # DataStore 実装
    │   ├── UserNewsResourceRepository.kt         # インターフェース
    │   ├── CompositeUserNewsResourceRepository.kt # 複合データ結合実装
    │   ├── SearchContentsRepository.kt           # 検索インターフェース
    │   ├── DefaultSearchContentsRepository.kt    # FTS 検索実装
    │   ├── RecentSearchRepository.kt             # 検索履歴インターフェース
    │   └── DefaultRecentSearchRepository.kt      # 検索履歴実装
    ├── di/
    │   ├── DataModule.kt                         # Hilt: Repository バインディング
    │   └── UserNewsResourceRepositoryModule.kt   # Hilt: 複合 Repository バインディング
    ├── model/
    │   └── (DTO → ドメインモデル変換の拡張関数群)
    └── util/
        ├── SyncStatusMonitor.kt                  # 同期状態監視
        ├── NetworkMonitor.kt                     # ネットワーク接続監視
        ├── TimeZoneMonitor.kt                    # タイムゾーン変更監視
        └── AnalyticsExtensions.kt                # 分析イベントヘルパー
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Repository パターンでデータアクセスを抽象化。OfflineFirst 実装では Room（ローカルDB）をプライマリとし、ネットワーク同期で更新。`combine()` で複数ストリームを結合して UI 用の複合データを生成 |
| **なぜこの構成か** | 単一信頼源（Single Source of Truth）。ローカル DB をプライマリにすることで、オフライン対応と高速なUI描画を実現。Room の `Flow` を返すことでリアクティブな更新を実現 |
| **新規PJ への考慮** | **必須採用（構造変更あり）**。最も設計判断が必要なモジュール。NiA は「オフラインファースト」前提だが、新規PJの業務アプリでは：<br>① **業務ロジックはバックエンド責務**→ Repository は基本的に「API 呼び出し → 結果返却」のシンプルな形に<br>② **キャッシュ戦略**が重要 → オフラインファーストではなく「APIファースト + キャッシュ」が現実的<br>③ ネットワーク監視・エラーハンドリングは引き続き必要 |

#### 2.4.4 core:database

```
core/database/
├── build.gradle.kts              # Android ライブラリ（Room + KSP）
├── schemas/                      # Room スキーマ JSON（マイグレーション用）
└── src/main/kotlin/.../core/database/
    ├── NiaDatabase.kt            # Room @Database 定義
    ├── DatabaseMigrations.kt     # スキーママイグレーション
    ├── dao/
    │   ├── NewsResourceDao.kt         # ニュース CRUD
    │   ├── NewsResourceFtsDao.kt      # 全文検索 DAO
    │   ├── TopicDao.kt                # トピック CRUD
    │   ├── TopicFtsDao.kt             # 全文検索 DAO
    │   └── RecentSearchQueryDao.kt    # 検索履歴 DAO
    ├── model/
    │   ├── NewsResourceEntity.kt      # ニュース Entity
    │   ├── NewsResourceFtsEntity.kt   # ニュース FTS Entity
    │   ├── TopicEntity.kt             # トピック Entity
    │   ├── TopicFtsEntity.kt          # トピック FTS Entity
    │   ├── NewsResourceTopicCrossRef.kt # 多対多中間テーブル
    │   ├── RecentSearchQueryEntity.kt # 検索履歴 Entity
    │   └── PopulatedNewsResource.kt   # JOIN クエリ結果
    └── util/                          # DB ユーティリティ
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Room による SQLite ローカルデータベース。Entity, DAO, Migration, FTS（全文検索）を管理。バージョン管理されたスキーマ JSON でマイグレーション安全性を担保 |
| **なぜこの構成か** | Offline-First アーキテクチャのローカルキャッシュ。SQLite に全データを保存し、UI はローカル DB を直接参照。ネットワーク同期は非同期バックグラウンドで実行 |
| **新規PJ への考慮** | **選択採用**。NiA のようなフルオフラインファーストは過剰な可能性が高い。ただし以下の用途で Room は有用：<br>① 検索履歴などのローカルデータ<br>② API レスポンスのキャッシュ<br>③ ドラフト保存（入力中の注文データ等）<br>**推奨**：database モジュールは作成するが、NiA のような「全データローカル保存」ではなく「選択的キャッシュ」として使用 |

#### 2.4.5 core:datastore

```
core/datastore/
├── build.gradle.kts              # Android ライブラリ（DataStore + Protobuf）
├── consumer-proguard-rules.pro   # ProGuard ルール
└── src/main/kotlin/.../core/datastore/
    ├── NiaPreferencesDataSource.kt    # DataStore 読み書き実装
    ├── di/
    │   └── DataStoreModule.kt         # Hilt: DataStore シングルトン提供
    └── (Proto 生成クラスの利用)

core/datastore-proto/
├── build.gradle.kts              # JVM ライブラリ（Protobuf compiler）
└── src/main/proto/
    ├── com/google/samples/apps/nowinandroid/data/
    │   ├── user_preferences.proto     # ユーザー設定スキーマ
    │   ├── dark_theme_config.proto    # テーマ設定
    │   └── theme_brand.proto          # ブランド設定
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Protocol Buffers で型安全にユーザー設定（テーマ、ブックマーク、フォロー中トピック等）を永続化。SharedPreferences の後継として DataStore を使用 |
| **なぜこの構成か** | SharedPreferences の問題（型安全性なし、ANR リスク、非同期API不足）を解決。Proto で構造化データをシリアライズし、Flow で変更を観測可能に |
| **新規PJ への考慮** | **必須採用**。以下の用途で不可欠：<br>① 認証トークン管理<br>② ユーザー設定（言語、テーマ等）<br>③ オンボーディング状態<br>④ 選択中の店舗/拠点情報<br>Proto は型安全で拡張性が高いが、シンプルなキーバリューなら Preferences DataStore でも十分。チームの習熟度で判断 |

#### 2.4.6 core:domain

```
core/domain/
├── build.gradle.kts              # Android ライブラリ（依存: data, model）
└── src/main/kotlin/.../core/domain/
    ├── GetFollowableTopicsUseCase.kt      # トピック + フォロー状態結合
    ├── GetSearchContentsUseCase.kt        # 検索 + ユーザーコンテキスト結合
    ├── GetRecentSearchQueriesUseCase.kt   # 検索履歴取得
    └── TopicSortField.kt                  # ソート条件 enum
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | UseCase パターン。複数の Repository を結合して ViewModel が必要とする形にデータを加工。`operator fun invoke()` で呼び出し可能なシングルメソッドクラス |
| **なぜこの構成か** | Clean Architecture の Domain Layer。ViewModel の肥大化を防ぎ、ビジネスロジックを再利用可能にする |
| **新規PJ への考慮** | **⚠️ 不採用（PJ方針）**。要件定義で「moobileにはusecase層を使用しない。業務ロジックはバックエンドの責務」と明記。ただし、NiA の UseCase が行っている **「複数データストリームの結合」** は ViewModel またはデータ層の Mapper/Converter で代替する必要がある。具体的には：<br>- `GetFollowableTopicsUseCase` → Repository 層の結合メソッドで代替<br>- `GetSearchContentsUseCase` → バックエンド API が結合済みデータを返す設計に |

#### 2.4.7 core:designsystem

```
core/designsystem/
├── build.gradle.kts              # Android ライブラリ（Compose Material3）
└── src/main/kotlin/.../core/designsystem/
    ├── component/
    │   ├── NiaButton.kt               # カスタムボタン
    │   ├── NiaCard.kt                 # カスタムカード
    │   ├── NiaChip.kt                 # カスタムチップ
    │   ├── NiaFilterChip.kt           # フィルターチップ
    │   ├── NiaIconToggleButton.kt     # トグルボタン
    │   ├── NiaLoadingWheel.kt         # ローディングインジケーター
    │   ├── NiaNavigationSuiteScaffold.kt  # ナビゲーションスイート
    │   ├── NiaTopAppBar.kt            # トップアプリバー
    │   ├── DynamicAsyncImage.kt       # 非同期画像読み込み
    │   ├── Tag.kt                     # タグコンポーネント
    │   └── scrollbar/                 # カスタムスクロールバー
    ├── icon/
    │   └── NiaIcons.kt                # アイコン定数定義
    └── theme/
        ├── Color.kt                   # テーマカラー定義
        ├── Theme.kt                   # Material3 テーマ
        ├── Type.kt                    # タイポグラフィ
        ├── Tint.kt                    # アイコンティント
        └── GradientColors.kt         # グラデーションカラー
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Material Design 3 に基づくデザインシステム。テーマ（カラー、タイポグラフィ、シェイプ）と再利用可能な UI コンポーネントのライブラリ。カスタム Lint ルールも `lintPublish` で配布 |
| **なぜこの構成か** | UI の一貫性を保証。全 feature モジュールがこのモジュールのコンポーネントを使うことで、デザイントークンの統一とコンポーネントの再利用を実現 |
| **新規PJ への考慮** | **必須採用**。大規模 PJ でデザインシステムモジュールがないと、各画面で微妙に異なる UI が量産される。デザイナーとの協業で Figma トークン → Compose テーマの変換パイプラインを構築すべき |

#### 2.4.8 core:ui

```
core/ui/
├── build.gradle.kts              # Android ライブラリ（Compose, core:designsystem 依存）
└── src/main/kotlin/.../core/ui/
    ├── NewsFeed.kt                    # ニュースフィード LazyColumn
    ├── NewsResourceCard.kt            # ニュースカードコンポーネント
    ├── InterestsItem.kt               # 興味トピックカードコンポーネント
    ├── AnalyticsExtensions.kt         # 分析イベント拡張
    ├── JankStatsExtensions.kt         # パフォーマンス計測拡張
    ├── DevicePreviews.kt              # マルチデバイスプレビューアノテーション
    ├── LocalTimeZone.kt               # CompositionLocal タイムゾーン
    └── *PreviewParameterProvider.kt   # Compose Preview パラメータ
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | `designsystem` の基礎コンポーネントを組み合わせた「ビジネスコンポーネント」。ニュースカード、フィード一覧など、複数の feature で共有される UI パーツ |
| **なぜこの構成か** | `designsystem`（原子的コンポーネント）と `feature`（画面）の間に位置する中間層。Atomic Design でいう Molecule/Organism レベル |
| **新規PJ への考慮** | **必須採用**。サブドメイン横断で共通の UI コンポーネント（商品カード、注文ステータスバッジ、検索バー等）を配置 |

#### 2.4.9 core:common

```
core/common/
├── build.gradle.kts              # JVM ライブラリ（Kotlin Coroutines）
└── src/main/kotlin/.../core/
    ├── network/NiaDispatchers.kt       # Coroutine Dispatcher 定義
    ├── network/Dispatcher.kt           # Dispatcher Qualifier
    └── result/Result.kt                # 汎用 Result ラッパー
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Android SDK に依存しない共通ユーティリティ。Coroutine Dispatcher の DI 定義と Result 型 |
| **なぜこの構成か** | テスト時に `TestDispatcher` に差し替え可能にする。全非同期処理がインジェクトされた Dispatcher を使うことで、テストの確定性を保証 |
| **新規PJ への考慮** | **必須採用**。Dispatcher の DI は大規模プロジェクトのテスタビリティの基盤。加えて API エラー型、ページング共通コード等も配置 |

#### 2.4.10 core:navigation

```
core/navigation/
├── build.gradle.kts              # Android ライブラリ（Navigation 3, Compose）
└── src/main/kotlin/.../core/navigation/
    ├── Navigator.kt              # ナビゲーションコマンドハンドラ
    └── NavigationState.kt        # バックスタック状態管理
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | Navigation 3（Jetpack Navigation の新世代 API）のラッパー。マルチレベルバックスタック（トップレベルタブ + サブスタック）を管理 |
| **なぜこの構成か** | Feature モジュール間のナビゲーション依存を解消。各 feature は navigation モジュールの型を使ってルートを宣言し、app モジュールが最終的にワイヤリングする |
| **新規PJ への考慮** | **必須採用**。多画面アプリではナビゲーション管理が最も複雑になる部分。NiA は Navigation 3（2025年新API）を使用しているが、安定性を重視するなら Navigation 2（Compose 版）も選択肢。サブドメイン間のナビゲーションフローを事前に設計すべき |

#### 2.4.11 core:analytics

```
core/analytics/
├── build.gradle.kts              # Android ライブラリ（Firebase Analytics）
└── src/main/kotlin/.../core/analytics/
    ├── AnalyticsHelper.kt             # 分析インターフェース
    ├── AnalyticsEvent.kt              # イベントデータクラス
    ├── StubAnalyticsHelper.kt         # Demo 用スタブ実装
    └── NoOpAnalyticsHelper.kt         # No-op 実装
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | 分析イベント送信の抽象化。インターフェースに対して Firebase Analytics 実装と No-op 実装を Hilt で切り替え |
| **なぜこの構成か** | テスト時・デモ時に分析イベントが発火しないようにする。抽象化することで分析プロバイダーの変更に強い |
| **新規PJ への考慮** | **推奨採用**。ユーザー行動分析は PJ 規模から考えて必要。Firebase Analytics 以外にも Amplitude, Mixpanel 等の選択肢がある。インターフェース化しておけば後から変更可能 |

#### 2.4.12 core:notifications

```
core/notifications/
├── build.gradle.kts              # Android ライブラリ（Firebase Cloud Messaging）
└── src/main/kotlin/.../core/notifications/
    ├── Notifier.kt                    # 通知インターフェース
    ├── SystemTrayNotifier.kt          # システム通知実装
    └── NoOpNotifier.kt               # No-op 実装
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | プッシュ通知の送信抽象化。Firebase Cloud Messaging と連携し、新着コンテンツ通知を表示 |
| **なぜこの構成か** | 通知ロジックを一箇所に集約。テスト時は NoOp に差し替え |
| **新規PJ への考慮** | **要検討**。業務アプリでのプッシュ通知要否による。在庫アラート、出荷通知、承認依頼など業務通知が必要なら採用 |

#### 2.4.13 テスト関連 core モジュール

| モジュール | 役割 | 新規PJ |
|-----------|------|--------|
| `core:testing` | 共有テストユーティリティ、フェイクリポジトリ、テストルール | **必須採用** |
| `core:data-test` | データ層のテスト用フェイク・Hilt バインディング | **必須採用** |
| `core:datastore-test` | DataStore のテストヘルパー | **必須採用** |
| `core:screenshot-testing` | Roborazzi スクリーンショットテスト基盤 | **推奨採用** |

### 2.5 feature モジュール群

NiA の feature モジュールは **API / Impl 二層分離パターン** を採用している。

```
feature/
├── bookmarks/
│   ├── api/                  # ナビゲーションコントラクト（薄い）
│   │   ├── build.gradle.kts  # nowinandroid.android.feature.api プラグイン
│   │   └── src/main/kotlin/.../
│   │       └── BookmarksNavKey.kt   # ルート定義のみ
│   └── impl/                 # 画面実装（重い）
│       ├── build.gradle.kts  # nowinandroid.android.feature.impl プラグイン
│       └── src/main/kotlin/.../
│           ├── BookmarksScreen.kt       # Compose 画面
│           ├── BookmarksViewModel.kt    # MVVM ViewModel
│           └── navigation/
│               └── BookmarksNavigation.kt  # NavDisplay エントリ定義
├── foryou/                   # 同構造
├── interests/                # 同構造
├── search/                   # 同構造
├── settings/                 # impl のみ（他 feature から参照されないため）
└── topic/                    # 同構造
```

#### API / Impl 分離パターンの詳細

```
feature:bookmarks:api  →  NavKey 定義のみ（依存: core:navigation のみ）
                            ↑ 他の feature が参照可能
feature:bookmarks:impl →  Screen, ViewModel, Navigation（依存: core:*, feature:*:api）
                            ↑ app モジュールのみが参照
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | 各画面（Feature）を独立モジュールとして分離。`api` はナビゲーションルート定義のみ（他 feature から遷移先として参照可能）、`impl` は画面の実装全体 |
| **なぜこの構成か** | **コンパイル速度の最適化**: Feature A が Feature B の画面に遷移するとき、B の `api`（数ファイル）のみに依存し、B の `impl`（全画面コード）には依存しない。これにより変更時の再コンパイル範囲を最小化。**Feature 間の疎結合**: Impl 同士が直接依存しないためモジュール境界が明確 |
| **新規PJ への考慮** | **必須採用**。多画面・多サブドメインの PJ では Feature モジュール数が 20-50+ になる。API/Impl 分離なしではビルド時間が破綻する。サブドメインごとに feature グループを構成することを推奨 |

#### ViewModel の UDF パターン（NiA 実装例）

```kotlin
// ForYouViewModel.kt - NiA の UDF パターン
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,  // ← 新規PJでは不使用
) : ViewModel() {

    // State: Repository の Flow を StateFlow に変換
    val feedState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllForFollowedTopics()
            .map(NewsFeedUiState::Success)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NewsFeedUiState.Loading,
            )

    // Event: ユーザーアクションを処理
    fun updateTopicSelection(topicId: String, isChecked: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(topicId, isChecked)
        }
    }
}
```

**新規PJでの ViewModel パターン（UseCase なし）:**

```kotlin
// 新規PJ: ViewModel → Repository（API直接呼び出し）
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,  // API 呼び出しのみ
) : ViewModel() {

    val uiState: StateFlow<ProductListUiState> =
        productRepository.getProducts()
            .map { ProductListUiState.Success(it) }
            .catch { ProductListUiState.Error(it.message) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProductListUiState.Loading,
            )
}
```

### 2.6 sync モジュール

```
sync/
├── work/
│   ├── build.gradle.kts              # Android ライブラリ（WorkManager, FCM）
│   └── src/main/kotlin/.../sync/
│       ├── workers/
│       │   ├── SyncWorker.kt              # 定期同期ワーカー
│       │   ├── DelegatingWorker.kt        # ワーカー委譲パターン
│       │   └── AnalyticsExtensions.kt     # 同期分析
│       ├── initializers/
│       │   └── SyncInitializer.kt         # WorkManager 初期化
│       └── status/
│           └── WorkManagerSyncManager.kt  # 同期状態管理
└── sync-test/
    ├── build.gradle.kts              # テストヘルパー
    └── src/                          # テストフェイク
```

| 項目 | 説明 |
|------|------|
| **何をしているか** | WorkManager で定期バックグラウンド同期を実行。FCM（Firebase Cloud Messaging）のプッシュ通知による即時同期トリガーも実装。ネットワーク復帰時の自動リトライ機能 |
| **なぜこの構成か** | Offline-First の同期エンジン。端末がオンラインに復帰したときにサーバーとの差分同期を実行し、ローカル DB を最新化する |
| **新規PJ への考慮** | **限定採用**。<br>NiA の同期機構はオフラインファースト前提のため、そのままは不要。ただし WorkManager は以下の用途で有用：<br>① ログ・分析の遅延送信<br>② 大量データのバックグラウンドアップロード<br>③ 定期的なトークンリフレッシュ<br>プッシュ通知が必要な場合は FCM 部分を採用 |

### 2.7 その他モジュール

| モジュール | 役割 | 新規PJ |
|-----------|------|--------|
| `app-nia-catalog` | デザインシステムのショーケースアプリ。コンポーネント一覧を独立アプリとして閲覧可能 | **推奨**。デザイナーとの確認ツールとして有用 |
| `benchmarks` | Macro Benchmark + Baseline Profile 生成。起動速度・スクロール性能のベンチマーク | **後期採用**。初期は不要だが、GA前のパフォーマンス最適化で必要 |
| `lint` | カスタム Lint ルール（Compose ベストプラクティス強制） | **推奨**。チーム規約の自動化 |
| `ui-test-hilt-manifest` | Hilt 計装テスト用マニフェスト | **テスト時に必須**。Hilt の計装テストに技術的に必要 |

---

## 3. NiA と新規PJ の差異・判断ポイント

### アーキテクチャの根本的な違い

```
┌─ NiA（ニュースリーダーアプリ） ─────────────────────────────┐
│                                                           │
│  UI ← ViewModel ← UseCase ← Repository ← Network/DB     │
│                                                           │
│  特徴:                                                     │
│  ・オフラインファースト（ローカル DB がプライマリ）            │
│  ・業務ロジックがクライアント側にある                        │
│  ・同期エンジンでサーバーデータを定期取得                    │
│  ・単一ドメイン（ニュース配信）                             │
│  ・読み取り中心（書き込みはブックマーク/フォローのみ）        │
└───────────────────────────────────────────────────────────┘

┌─ 新規PJ（海外販売システム） ─────────────────────────────────┐
│                                                           │
│  UI ← ViewModel ← Repository ← API (Backend が業務処理)   │
│                                                           │
│  特徴:                                                     │
│  ・APIファースト（バックエンドがプライマリ）                  │
│  ・業務ロジックはバックエンド（DDD）                         │
│  ・UseCase 層不使用（モバイルは表示責務のみ）                │
│  ・複数サブドメイン（在庫/受注/商品/物流...）                │
│  ・読み書き両方（CRUD + 業務コマンド）                      │
│  ・認証/認可が必要（社内システム）                           │
└───────────────────────────────────────────────────────────┘
```

### 判断マトリックス

| NiA の要素 | 採用判断 | 理由 |
|-----------|---------|------|
| マルチモジュール構成 | ✅ 必須 | 多サブドメイン・多画面で必須 |
| Convention Plugin | ✅ 必須 | モジュール数が多いため設定統一が必須 |
| Feature API/Impl 分離 | ✅ 必須 | ビルド速度・疎結合の保証 |
| MVVM + UDF | ✅ 必須 | PJ 方針として決定済み |
| Hilt DI | ✅ 必須 | Google 公式推奨、テスタビリティ確保 |
| Jetpack Compose | ✅ 必須 | 現在の Android UI 標準 |
| Room Database | ⚠️ 選択的 | フルオフラインファーストは不要。キャッシュ・ドラフト保存用に限定使用 |
| DataStore (Proto) | ✅ 必須 | 認証トークン、設定保存 |
| core:domain (UseCase) | ❌ 不採用 | PJ 方針で不使用。`表示用のデータ変換`は Repository または ViewModel の Mapper で処理 |
| sync (WorkManager + FCM) | ⚠️ 限定的 | バックグラウンド同期は不要。通知用途で FCM は検討 |
| core:designsystem | ✅ 必須 | UI 一貫性の保証 |
| core:ui | ✅ 必須 | 共通ビジネスコンポーネントの集約 |
| core:analytics | ✅ 推奨 | 利用状況分析に有用 |
| core:notifications | ⚠️ 要検討 | 業務通知の要否次第 |
| demo/prod フレーバー | ✅ 必須 | dev/stg/prod 環境切替 |
| Baseline Profile | ⚠️ 後期 | GA 前のパフォーマンス最適化 |
| Screenshot Testing | ✅ 推奨 | UI 品質保証 |

---

## 4. 新規PJ 向けモバイルディレクトリ構成提案

### 全体構成図

```
retail-mobile/                           # リポジトリルート
├── build.gradle.kts                     # ルート Gradle（プラグイン一元管理）
├── settings.gradle.kts                  # モジュール登録・リポジトリ設定
├── gradle.properties                    # ビルド設定（並列ビルド、キャッシュ）
├── gradle/
│   ├── libs.versions.toml               # 依存バージョン一元管理
│   └── wrapper/
│       └── gradle-wrapper.properties    # Gradle Wrapper
├── compose_compiler_config.conf         # Compose Compiler 安定性設定
├── spotless/                            # コードフォーマットテンプレート
│   ├── copyright.kt
│   ├── copyright.kts
│   └── copyright.xml
│
├── build-logic/                         # ──── ビルド規約 ────
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── convention/
│       └── src/main/kotlin/.../
│           ├── AndroidApplicationConventionPlugin.kt
│           ├── AndroidApplicationComposeConventionPlugin.kt
│           ├── AndroidApplicationFlavorsConventionPlugin.kt
│           ├── AndroidLibraryConventionPlugin.kt
│           ├── AndroidLibraryComposeConventionPlugin.kt
│           ├── AndroidFeatureApiConventionPlugin.kt
│           ├── AndroidFeatureImplConventionPlugin.kt
│           ├── HiltConventionPlugin.kt
│           ├── JvmLibraryConventionPlugin.kt
│           ├── AndroidRoomConventionPlugin.kt
│           ├── AndroidTestConventionPlugin.kt
│           └── helpers/
│               ├── BuildType.kt         # debug / staging / release
│               ├── Flavor.kt           # dev / stg / prod
│               ├── KotlinAndroid.kt
│               └── AndroidCompose.kt
│
├── app/                                 # ──── メインアプリ ────
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/.../
│       │       ├── MainActivity.kt              # シングルアクティビティ
│       │       ├── MainActivityViewModel.kt     # アプリレベル状態（認証、テーマ等）
│       │       ├── RetailApplication.kt         # @HiltAndroidApp
│       │       ├── di/
│       │       │   └── AppModule.kt             # アプリスコープ DI
│       │       ├── ui/
│       │       │   ├── RetailApp.kt             # ルート Composable
│       │       │   └── RetailAppState.kt        # アプリ状態（ネットワーク、認証状態）
│       │       └── navigation/
│       │           ├── TopLevelNavItem.kt        # メインナビゲーション項目
│       │           └── RetailNavHost.kt          # ナビゲーショングラフ
│       ├── dev/                          # 開発環境固有
│       ├── stg/                          # STG 環境固有
│       ├── prod/                         # PROD 環境固有
│       ├── debug/                        # デバッグビルド固有
│       └── test/                         # ユニットテスト
│
├── core/                                # ──── 共通基盤モジュール ────
│   │
│   ├── model/                           # 【Pure JVM】共有データモデル
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/model/
│   │       ├── Product.kt               # 商品
│   │       ├── Order.kt                 # 受注
│   │       ├── Inventory.kt             # 在庫
│   │       ├── Store.kt                 # 店舗
│   │       ├── User.kt                  # ユーザー
│   │       ├── Category.kt              # カテゴリ
│   │       ├── CartItem.kt              # カート
│   │       ├── Shipment.kt              # 出荷
│   │       ├── AppSettings.kt           # アプリ設定
│   │       └── enums/                   # 各種 enum
│   │           ├── OrderStatus.kt
│   │           ├── ShipmentStatus.kt
│   │           └── UserRole.kt
│   │
│   ├── network/                         # 【Android】API 通信基盤
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/network/
│   │       ├── RetailApiClient.kt       # 共通 API クライアントインターフェース
│   │       ├── di/
│   │       │   └── NetworkModule.kt     # Hilt: Retrofit, OkHttp, Interceptor 提供
│   │       ├── interceptor/
│   │       │   ├── AuthInterceptor.kt       # 認証トークン付与
│   │       │   ├── TokenRefreshInterceptor.kt # トークンリフレッシュ
│   │       │   └── ErrorInterceptor.kt      # API エラー共通ハンドリング
│   │       ├── model/                   # API レスポンス DTO
│   │       │   ├── ApiResponse.kt           # 共通レスポンスラッパー
│   │       │   ├── ApiError.kt              # エラーレスポンス
│   │       │   ├── PaginatedResponse.kt     # ページネーション
│   │       │   └── (各サブドメインの DTO は feature:*:impl 内に配置)
│   │       ├── api/                     # サブドメイン別 API インターフェース
│   │       │   ├── ProductApi.kt            # 商品 API
│   │       │   ├── OrderApi.kt              # 受注 API
│   │       │   ├── InventoryApi.kt          # 在庫 API
│   │       │   ├── StoreApi.kt              # 店舗 API
│   │       │   ├── AuthApi.kt               # 認証 API
│   │       │   └── ShipmentApi.kt           # 出荷 API
│   │       └── mock/                    # dev フレーバー用モックデータ
│   │           └── MockApiDataSource.kt
│   │
│   ├── data/                            # 【Android】Repository 基盤
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/data/
│   │       ├── repository/              # 共通リポジトリ
│   │       │   ├── AuthRepository.kt            # 認証リポジトリ（インターフェース）
│   │       │   ├── DefaultAuthRepository.kt     # 認証実装
│   │       │   ├── UserSettingsRepository.kt    # ユーザー設定（インターフェース）
│   │       │   └── DefaultUserSettingsRepository.kt
│   │       ├── di/
│   │       │   └── DataModule.kt                # Hilt バインディング
│   │       ├── mapper/                  # DTO → Model 変換（UseCase 代替）
│   │       │   └── ResponseMapper.kt           # 共通マッピング関数
│   │       └── util/
│   │           ├── NetworkMonitor.kt            # ネットワーク接続監視
│   │           └── ConnectivityManagerNetworkMonitor.kt
│   │
│   ├── database/                        # 【Android】ローカルキャッシュ（選択的）
│   │   ├── build.gradle.kts
│   │   ├── schemas/
│   │   └── src/main/kotlin/.../core/database/
│   │       ├── RetailDatabase.kt                # Room Database
│   │       ├── dao/
│   │       │   ├── DraftDao.kt                  # 下書き保存 DAO
│   │       │   ├── RecentSearchDao.kt           # 検索履歴 DAO
│   │       │   └── CacheDao.kt                  # 汎用キャッシュ DAO
│   │       └── model/
│   │           ├── DraftEntity.kt               # 下書き Entity
│   │           ├── RecentSearchEntity.kt        # 検索履歴 Entity
│   │           └── CacheEntry.kt                # キャッシュ Entity
│   │
│   ├── datastore/                       # 【Android】設定永続化
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/datastore/
│   │       ├── UserPreferencesDataSource.kt     # 設定読み書き
│   │       ├── TokenDataSource.kt               # 認証トークン管理（暗号化）
│   │       └── di/
│   │           └── DataStoreModule.kt           # Hilt: DataStore 提供
│   │
│   ├── designsystem/                    # 【Android】デザインシステム
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/designsystem/
│   │       ├── component/               # 原子的 UI コンポーネント
│   │       │   ├── RetailButton.kt
│   │       │   ├── RetailCard.kt
│   │       │   ├── RetailTextField.kt
│   │       │   ├── RetailSearchBar.kt
│   │       │   ├── RetailLoadingIndicator.kt
│   │       │   ├── RetailErrorView.kt
│   │       │   ├── RetailEmptyView.kt
│   │       │   ├── RetailBadge.kt
│   │       │   ├── RetailChip.kt
│   │       │   ├── RetailDialog.kt
│   │       │   ├── RetailTopAppBar.kt
│   │       │   ├── RetailBottomSheet.kt
│   │       │   ├── RetailNavigationBar.kt
│   │       │   └── RetailStatusIndicator.kt     # ステータスバッジ
│   │       ├── icon/
│   │       │   └── RetailIcons.kt               # アイコン定数
│   │       └── theme/
│   │           ├── Color.kt                     # カラーパレット
│   │           ├── Theme.kt                     # Material3 テーマ
│   │           ├── Type.kt                      # タイポグラフィ
│   │           └── Shape.kt                     # シェイプ定義
│   │
│   ├── ui/                              # 【Android】共通ビジネスコンポーネント
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/ui/
│   │       ├── ProductCard.kt                   # 商品カード
│   │       ├── OrderStatusBadge.kt              # 注文ステータス
│   │       ├── StoreSelector.kt                 # 店舗選択
│   │       ├── QuantitySelector.kt              # 数量選択
│   │       ├── PriceDisplay.kt                  # 価格表示（通貨フォーマット）
│   │       ├── PaginatedList.kt                 # ページング付きリスト
│   │       ├── PullRefreshContainer.kt          # Pull-to-Refresh
│   │       ├── SearchResultItem.kt              # 検索結果項目
│   │       ├── ImageGallery.kt                  # 商品画像ギャラリー
│   │       └── DevicePreviews.kt                # Compose Preview
│   │
│   ├── navigation/                      # 【Android】ナビゲーション基盤
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/navigation/
│   │       ├── Navigator.kt                     # ナビゲーションコマンド
│   │       └── NavigationState.kt               # バックスタック管理
│   │
│   ├── common/                          # 【Pure JVM】共通ユーティリティ
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/
│   │       ├── network/
│   │       │   ├── Dispatcher.kt                # Dispatcher Qualifier
│   │       │   └── RetailDispatchers.kt         # IO/Default/Main
│   │       ├── result/
│   │       │   └── Result.kt                    # 共通 Result 型
│   │       └── extension/
│   │           ├── FlowExtensions.kt            # Flow ユーティリティ
│   │           └── StringExtensions.kt          # 文字列ヘルパー
│   │
│   ├── analytics/                       # 【Android】分析基盤
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/analytics/
│   │       ├── AnalyticsHelper.kt               # インターフェース
│   │       ├── AnalyticsEvent.kt                # イベントモデル
│   │       └── NoOpAnalyticsHelper.kt           # No-op 実装
│   │
│   ├── testing/                         # 【Android】テスト共通基盤
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/.../core/testing/
│   │       ├── MainDispatcherRule.kt            # テスト用 Dispatcher
│   │       ├── FakeAuthRepository.kt            # 認証フェイク
│   │       └── TestDataFactory.kt               # テストデータ生成
│   │
│   └── screenshot-testing/              # 【Android】スクリーンショットテスト基盤
│       └── build.gradle.kts
│
├── feature/                             # ──── 機能モジュール ────
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ サブドメイン: 商品管理 (Product)                   │
│   │  └─────────────────────────────────────────────────┘
│   ├── product-catalog/                 # 商品カタログ（一覧/検索）
│   │   ├── api/
│   │   │   └── src/main/kotlin/.../
│   │   │       └── ProductCatalogNavKey.kt
│   │   └── impl/
│   │       └── src/main/kotlin/.../
│   │           ├── ProductCatalogScreen.kt
│   │           ├── ProductCatalogViewModel.kt
│   │           ├── model/               # 画面固有 UI State
│   │           │   └── ProductCatalogUiState.kt
│   │           └── navigation/
│   │               └── ProductCatalogNavigation.kt
│   │
│   ├── product-detail/                  # 商品詳細
│   │   ├── api/
│   │   └── impl/
│   │       └── src/main/kotlin/.../
│   │           ├── ProductDetailScreen.kt
│   │           ├── ProductDetailViewModel.kt
│   │           └── navigation/
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ サブドメイン: 受注管理 (Order)                     │
│   │  └─────────────────────────────────────────────────┘
│   ├── order-list/                      # 受注一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── order-detail/                    # 受注詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── order-create/                    # 受注作成
│   │   ├── api/
│   │   └── impl/
│   │       └── src/main/kotlin/.../
│   │           ├── OrderCreateScreen.kt
│   │           ├── OrderCreateViewModel.kt
│   │           ├── model/
│   │           │   ├── OrderCreateUiState.kt
│   │           │   └── OrderFormValidation.kt   # 入力バリデーション（表示ルール）
│   │           └── navigation/
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ サブドメイン: 在庫管理 (Inventory)                  │
│   │  └─────────────────────────────────────────────────┘
│   ├── inventory-list/                  # 在庫一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── inventory-detail/                # 在庫詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── inventory-adjustment/            # 在庫調整
│   │   ├── api/
│   │   └── impl/
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ サブドメイン: 物流管理 (Shipment)                   │
│   │  └─────────────────────────────────────────────────┘
│   ├── shipment-list/                   # 出荷一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── shipment-detail/                 # 出荷詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── shipment-tracking/               # 出荷追跡
│   │   ├── api/
│   │   └── impl/
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ サブドメイン: 店舗管理 (Store)                     │
│   │  └─────────────────────────────────────────────────┘
│   ├── store-list/                      # 店舗一覧
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── store-detail/                    # 店舗詳細
│   │   ├── api/
│   │   └── impl/
│   │
│   │  ┌─────────────────────────────────────────────────┐
│   │  │ 共通機能                                          │
│   │  └─────────────────────────────────────────────────┘
│   ├── auth/                            # 認証（ログイン/ログアウト）
│   │   └── impl/                        # api 不要（他 feature から遷移しない）
│   │       └── src/main/kotlin/.../
│   │           ├── LoginScreen.kt
│   │           ├── LoginViewModel.kt
│   │           └── navigation/
│   │
│   ├── settings/                        # アプリ設定
│   │   └── impl/
│   │
│   ├── search/                          # 横断検索
│   │   ├── api/
│   │   └── impl/
│   │
│   ├── dashboard/                       # ダッシュボード（ホーム画面）
│   │   ├── api/
│   │   └── impl/
│   │
│   └── notifications/                   # 通知一覧
│       ├── api/
│       └── impl/
│
├── lint/                                # カスタム Lint ルール
│   └── src/main/kotlin/.../
│
├── app-catalog/                         # デザインシステムショーケース
│   └── src/main/kotlin/.../
│
├── ui-test-hilt-manifest/               # Hilt テスト用マニフェスト
│   └── src/
│
└── .github/
    └── workflows/                       # CI/CD
        ├── build.yml
        ├── test.yml
        └── release.yml
```

### settings.gradle.kts に登録するモジュール一覧

```kotlin
// settings.gradle.kts
include(":app")
include(":app-catalog")
include(":lint")
include(":ui-test-hilt-manifest")

// Core modules
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

// Feature modules - Product
include(":feature:product-catalog:api")
include(":feature:product-catalog:impl")
include(":feature:product-detail:api")
include(":feature:product-detail:impl")

// Feature modules - Order
include(":feature:order-list:api")
include(":feature:order-list:impl")
include(":feature:order-detail:api")
include(":feature:order-detail:impl")
include(":feature:order-create:api")
include(":feature:order-create:impl")

// Feature modules - Inventory
include(":feature:inventory-list:api")
include(":feature:inventory-list:impl")
include(":feature:inventory-detail:api")
include(":feature:inventory-detail:impl")
include(":feature:inventory-adjustment:api")
include(":feature:inventory-adjustment:impl")

// Feature modules - Shipment
include(":feature:shipment-list:api")
include(":feature:shipment-list:impl")
include(":feature:shipment-detail:api")
include(":feature:shipment-detail:impl")
include(":feature:shipment-tracking:api")
include(":feature:shipment-tracking:impl")

// Feature modules - Store
include(":feature:store-list:api")
include(":feature:store-list:impl")
include(":feature:store-detail:api")
include(":feature:store-detail:impl")

// Feature modules - Common
include(":feature:auth:impl")
include(":feature:settings:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:dashboard:api")
include(":feature:dashboard:impl")
include(":feature:notifications:api")
include(":feature:notifications:impl")
```

---

## 5. 各モジュール詳細設計

### 5.1 モジュール依存関係

```
app
 ├── feature:*:impl（全Feature実装モジュール）
 ├── core:designsystem
 ├── core:ui
 ├── core:navigation
 ├── core:data
 ├── core:model
 ├── core:analytics
 └── core:common

feature:*:api
 └── core:navigation（ルート定義のみ依存）

feature:*:impl
 ├── feature:*:api（自身のAPI）
 ├── feature:(遷移先):api（他Feature のAPI、必要な場合のみ）
 ├── core:data
 ├── core:model
 ├── core:designsystem
 ├── core:ui
 ├── core:navigation
 ├── core:analytics
 └── core:common

core:data
 ├── core:network
 ├── core:database
 ├── core:datastore
 ├── core:model
 └── core:common

core:network
 ├── core:model
 └── core:common

core:database
 └── core:model

core:datastore
 └── core:model

core:ui
 ├── core:designsystem
 ├── core:model
 └── core:analytics

core:designsystem
 └── (外部ライブラリのみ)

core:model
 └── (kotlinx-datetime のみ)

core:common
 └── (kotlinx-coroutines のみ)

core:navigation
 └── (Navigation, Compose のみ)
```

### 5.2 レイヤー別の責務定義

#### UI層（feature:*:impl の Screen）

```kotlin
// 責務: 画面の描画、ユーザーインタラクションの受付
// NOT: データの加工、API 呼び出し、バリデーション以外のロジック
@Composable
fun ProductCatalogScreen(
    uiState: ProductCatalogUiState,      // ViewModel から受け取る表示用状態
    onProductClick: (String) -> Unit,     // イベントを ViewModel に委譲
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    // Compose UI の描画のみ
}
```

#### ViewModel層（feature:*:impl の ViewModel）

```kotlin
// 責務: UI 状態の管理、ユーザーイベントの処理、Repository の呼び出し
// NOT: 業務ロジック、ドメインルール計算
@HiltViewModel
class ProductCatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository,
) : ViewModel() {

    // 検索クエリ（ユーザー入力）
    private val searchQuery = MutableStateFlow("")

    // UI状態: Repository の Flow → UiState に変換
    val uiState: StateFlow<ProductCatalogUiState> =
        searchQuery
            .debounce(300) // 表示ルール: 入力デバウンス
            .flatMapLatest { query ->
                productRepository.searchProducts(query)
            }
            .map { products ->
                ProductCatalogUiState.Success(
                    products = products,
                    // 表示ルール: 在庫なし商品のグレーアウト等はここで判定
                )
            }
            .catch { emit(ProductCatalogUiState.Error(it.message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProductCatalogUiState.Loading,
            )

    // イベント処理
    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }
}
```

#### Repository層（core:data）

```kotlin
// 責務: API呼び出し、レスポンスのモデル変換、キャッシュ制御
// NOT: 業務ロジック、画面固有のデータ加工
interface ProductRepository {
    fun searchProducts(query: String): Flow<List<Product>>
    fun getProductDetail(id: String): Flow<Product>
    suspend fun refreshProducts(): Result<Unit>
}

class DefaultProductRepository @Inject constructor(
    private val productApi: ProductApi,         // API インターフェース
    private val productMapper: ProductMapper,   // DTO → Model 変換
) : ProductRepository {

    override fun searchProducts(query: String): Flow<List<Product>> = flow {
        val response = productApi.searchProducts(query)
        emit(response.data.map { productMapper.toModel(it) })
    }
}
```

#### Mapper（UseCase の代替）

```kotlin
// 責務: API レスポンス DTO → アプリ内モデルへの変換
// NiA の UseCase が担っていた「データ結合・変換」の役割をここで果たす
class ProductMapper @Inject constructor() {
    fun toModel(dto: ProductDto): Product = Product(
        id = dto.productId,
        name = dto.productName,
        price = dto.unitPrice,
        currency = dto.currencyCode,
        imageUrl = dto.primaryImageUrl,
        stockStatus = StockStatus.fromCode(dto.stockStatusCode),  // 表示用変換
        // 業務ロジック（値引き計算等）はバックエンドが処理済み
    )
}
```

### 5.3 環境別フレーバー設計

```kotlin
// NiaFlavor.kt 相当の設定
enum class RetailFlavor(
    val dimension: String,
    val applicationIdSuffix: String?,
    val baseUrl: String,
) {
    DEV(
        dimension = "environment",
        applicationIdSuffix = ".dev",
        baseUrl = "https://dev-api.retail.example.com",
    ),
    STG(
        dimension = "environment",
        applicationIdSuffix = ".stg",
        baseUrl = "https://stg-api.retail.example.com",
    ),
    PROD(
        dimension = "environment",
        applicationIdSuffix = null,
        baseUrl = "https://api.retail.example.com",
    ),
}
```

---

## 6. データフロー・アーキテクチャ図

### 全体アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                     Android App (Mobile)                     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ UI Layer (Jetpack Compose)                            │   │
│  │  Screen → ユーザー操作を受付、状態を描画               │   │
│  │  collectAsStateWithLifecycle() で ViewModel を観測     │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ Event↓  State↑                      │
│  ┌────────────────────┴─────────────────────────────────┐   │
│  │ ViewModel Layer (MVVM + UDF)                          │   │
│  │  StateFlow で UI 状態を公開                            │   │
│  │  viewModelScope.launch で非同期処理                    │   │
│  │  表示ルール: デバウンス、フィルタ、ソート、フォーマット    │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │                                      │
│  ┌────────────────────┴─────────────────────────────────┐   │
│  │ Data Layer (Repository + Mapper)                      │   │
│  │  API 呼び出し → DTO → Model 変換                      │   │
│  │  キャッシュ戦略（必要に応じて Room / Memory Cache）     │   │
│  │  認証トークン管理（DataStore）                          │   │
│  │  ネットワーク状態監視                                   │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │ HTTP (Retrofit + OkHttp)            │
└───────────────────────┼─────────────────────────────────────┘
                        │
                ┌───────┴───────┐
                │   API Gateway  │
                └───────┬───────┘
                        │
┌───────────────────────┼─────────────────────────────────────┐
│                Backend (Java / Spring Boot)                   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Application Layer (ヘキサゴナルアーキテクチャ)          │   │
│  │  ├─ Port (入力/出力インターフェース)                    │   │
│  │  ├─ Adapter (REST Controller, DB Adapter)             │   │
│  │  └─ CQRS: Command / Query 分離                        │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Domain Layer (DDD)                                    │   │
│  │  業務ロジック、ドメインモデル、ドメインサービス          │   │
│  │  バリデーション（業務ルール）                           │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Infrastructure Layer                                  │   │
│  │  Oracle DB アクセス、外部 API 連携                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### UDF（Unidirectional Data Flow）詳細

```
              ┌─────────────────────┐
              │     Compose UI      │
              │  (Screen Composable)│
              └──┬──────────────┬───┘
     Event       │              ↑  State
  (onClick等)    ↓              │  (UiState)
              ┌──┴──────────────┴───┐
              │     ViewModel       │
              │                     │
              │ MutableStateFlow    │
              │  ↓ transform        │
              │ StateFlow<UiState>  │
              └──┬──────────────────┘
                 │ suspend fun
                 ↓
              ┌──┴──────────────────┐
              │    Repository       │
              │                     │
              │ API Call → Mapper   │
              │ Cache Read/Write    │
              └──┬──────────────────┘
                 │ HTTP Request
                 ↓
              ┌──┴──────────────────┐
              │   Backend API       │
              │ (Spring Boot + DDD) │
              └─────────────────────┘
```

### バリデーション責務分離

```
┌─ モバイル（表示ルール） ──────────────────────────────────────┐
│                                                             │
│  フォームバリデーション（入力チェック）                        │
│  ├─ 必須項目チェック（空文字、null）                          │
│  ├─ 文字数制限                                              │
│  ├─ 日付フォーマットチェック                                  │
│  ├─ 数値範囲チェック（画面表示上の制約）                       │
│  └─ メールアドレス形式チェック                                │
│                                                             │
│  表示制御                                                    │
│  ├─ 項目の表示/非表示                                        │
│  ├─ 入力可否制御（ReadOnly/Disabled）                        │
│  ├─ リストフィルタリング・ソート（表示用）                     │
│  └─ 通貨・日付・数値のフォーマット                            │
└─────────────────────────────────────────────────────────────┘

┌─ バックエンド（業務ロジック） ─────────────────────────────────┐
│                                                             │
│  ドメインバリデーション                                       │
│  ├─ 在庫引当可否判定                                         │
│  ├─ 与信チェック                                             │
│  ├─ 商品マスタとの整合性                                      │
│  ├─ 価格計算（割引、税計算、為替レート適用）                   │
│  └─ ステータス遷移ルール（受注→出荷→配送→完了）              │
│                                                             │
│  業務処理                                                    │
│  ├─ 受注確定処理                                             │
│  ├─ 在庫引当                                                 │
│  ├─ 出荷指示                                                 │
│  └─ レポート生成                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. サブドメイン分割戦略

### 想定サブドメイン（IKEA/ニトリ/カインズ型業態）

| サブドメイン | Feature モジュール | 画面例 |
|-------------|-------------------|--------|
| **商品管理** | `product-catalog`, `product-detail` | 商品検索、商品詳細、バーコードスキャン |
| **受注管理** | `order-list`, `order-detail`, `order-create` | 受注一覧、受注詳細、新規受注入力 |
| **在庫管理** | `inventory-list`, `inventory-detail`, `inventory-adjustment` | 在庫照会、棚卸、入出庫登録 |
| **物流管理** | `shipment-list`, `shipment-detail`, `shipment-tracking` | 出荷一覧、出荷詳細、配送追跡 |
| **店舗管理** | `store-list`, `store-detail` | 店舗一覧、店舗情報 |
| **認証** | `auth` | ログイン、パスワードリセット |
| **設定** | `settings` | テーマ、言語、通知設定 |
| **検索** | `search` | 横断検索（商品/受注/在庫） |
| **ダッシュボード** | `dashboard` | KPI サマリー、アラート、クイックアクション |
| **通知** | `notifications` | 通知一覧、通知詳細 |

### Feature モジュール粒度の指針

```
粒度が粗すぎる:
  feature:order/          ← 受注の全機能が1モジュール（一覧/詳細/作成/編集）
  → 問題: モジュールが肥大化、ビルド時間増大、チーム分担しにくい

粒度が細かすぎる:
  feature:order-list-filter/    ← 受注一覧のフィルター機能だけ独立
  → 問題: モジュール数爆発、管理コスト増大

推奨粒度:
  feature:order-list/           ← 画面単位（一覧画面には検索/フィルター含む）
  feature:order-detail/         ← 画面単位（詳細 + 関連アクション）
  feature:order-create/         ← 画面単位（入力フォーム + 確認）
  → 理由: 1画面 = 1モジュール がチーム分担・ビルド最適化の最適バランス
```

### ナビゲーション設計

```
BottomNavigation（メインタブ）
├── ダッシュボード (dashboard)
│   └── KPI → 詳細遷移（各サブドメインへ）
├── 商品 (product-catalog)
│   └── 一覧 → 詳細（product-detail）
├── 受注 (order-list)
│   ├── 一覧 → 詳細（order-detail）
│   └── 一覧 → 新規作成（order-create）
├── 在庫 (inventory-list)
│   ├── 一覧 → 詳細（inventory-detail）
│   └── 一覧 → 調整（inventory-adjustment）
└── その他（ドロワー or 設定タブ）
    ├── 出荷 (shipment-list → shipment-detail → shipment-tracking)
    ├── 店舗 (store-list → store-detail)
    ├── 通知 (notifications)
    └── 設定 (settings)

グローバルアクション:
  ├── 検索（search）: 全タブから AppBar の検索アイコンでアクセス
  └── ログアウト: 設定画面から
```

---

## 8. テスト戦略

### テストピラミッド

```
           ╱─────────╲
          ╱  E2E Test  ╲          少数・結合テスト（CI で Gradle Managed Device）
         ╱  (計装テスト)  ╲
        ╱─────────────────╲
       ╱ Screenshot Test    ╲      UI 回帰テスト（Roborazzi）
      ╱  (スクリーンショット)  ╲
     ╱─────────────────────────╲
    ╱     Unit Test              ╲    大量・ViewModel / Repository / Mapper テスト
   ╱   (ローカルユニットテスト)    ╲
  ╱─────────────────────────────────╲
```

### テスト配置

| テスト種別 | 配置場所 | テスト対象 | ツール |
|-----------|---------|-----------|-------|
| ViewModel ユニットテスト | `feature:*:impl/src/test/` | UiState 変換, イベント処理 | JUnit5, Turbine, Truth |
| Repository ユニットテスト | `core:data/src/test/` | API 呼び出し, マッピング | JUnit5, MockK, Truth |
| Mapper ユニットテスト | `core:data/src/test/` | DTO → Model 変換 | JUnit5, Truth |
| Compose UI テスト | `feature:*:impl/src/androidTest/` | 画面描画, インタラクション | ComposeTestRule |
| スクリーンショットテスト | `feature:*:impl/src/testDev/` | UI 回帰検知 | Roborazzi |
| 結合テスト | `app/src/androidTest/` | 画面遷移, E2E フロー | Espresso, ComposeTestRule |

### テスト用 Convention Plugin

```kotlin
// AndroidFeatureImplConventionPlugin.kt が自動で以下を設定:
testImplementation(libs.junit5)
testImplementation(libs.truth)
testImplementation(libs.turbine)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.mockk)
androidTestImplementation(libs.compose.ui.test)
```

---

## 9. 判断が必要な技術選定一覧

以下は本提案で「要検討」としている項目の判断材料をまとめたものです。

### 9.1 ローカルデータベース（Room）の採用範囲

| 選択肢 | メリット | デメリット | 推奨場面 |
|--------|---------|-----------|---------|
| **A: Room なし**（API直接のみ） | シンプル、コード量少 | オフライン非対応、毎回API呼び出し | 常時オンライン前提の社内ツール |
| **B: Room 限定使用**（キャッシュ+ドラフト） | バランス良好 | 中程度の実装コスト | **推奨**。検索履歴・ドラフト・マスターキャッシュ用 |
| **C: Room フルオフラインファースト**（NiAと同等） | オフライン完全対応 | 実装コスト大、同期ロジック複雑 | 通信環境が不安定な倉庫現場など |

**推奨: B（限定使用）。** 店舗スタッフが使う端末は Wi-Fi 環境下が多いと想定。ただし、倉庫や売場での使用がある場合は C を検討。

### 9.2 Navigation バージョン

| 選択肢 | メリット | デメリット |
|--------|---------|-----------|
| **Navigation 2**（Compose版、安定） | 事例豊富、安定性高 | Deep Link 設定が冗長 |
| **Navigation 3**（2025新API、NiA採用） | 型安全、マルチペイン対応 | 2025年リリースのため事例少 |

**推奨: プロジェクトのスケジュール次第。** 開発開始が2026年以降なら Navigation 3 が安定している可能性が高い。安全策なら Navigation 2 を採用し、後でマイグレーション。

### 9.3 ネットワーク通信ライブラリ

| 選択肢 | メリット | デメリット |
|--------|---------|-----------|
| **Retrofit + OkHttp**（NiA採用） | 実績豊富、情報多 | やや冗長なインターフェース定義 |
| **Ktor Client** | Kotlin ネイティブ、マルチプラットフォーム | Android 特化機能は Retrofit が上 |

**推奨: Retrofit + OkHttp。** Spring Boot バックエンドとの RESTful API 通信では最も実績がある組み合わせ。

### 9.4 画像読み込み

| 選択肢 | メリット | デメリット |
|--------|---------|-----------|
| **Coil**（NiA採用） | Compose 統合優秀、軽量 | 一部機能は Glide が上 |
| **Glide** | 実績豊富、大規模アプリでの最適化 | Compose 統合は Coil が上 |

**推奨: Coil。** Compose ベースアプリでは Coil が公式推奨。

### 9.5 認証方式

| 選択肢 | 概要 | 推奨 |
|--------|------|------|
| **OAuth 2.0 + PKCE** | 標準的、Spring Security OAuth2 と相性良 | **推奨** |
| **OIDC (OpenID Connect)** | OAuth2 の拡張、ID トークンで認証 | SSO 要件がある場合 |
| **API Key + Session** | シンプル | セキュリティリスク高 |

### 9.6 状態管理ライブラリ追加の要否

| 項目 | 判断 |
|------|------|
| **Paging 3** | 一覧画面が多いため **必須**。API ページネーションと連携 |
| **Accompanist Permissions** | カメラ（バーコードスキャン）使用なら **必要** |
| **Kotlin Serialization** | API レスポンスパース用に **必須** |
| **ML Kit Barcode** | バーコードスキャン機能があれば **必要** |

### 9.7 CI/CD パイプライン

```
Pull Request:
  ├── Spotless Check（フォーマット）
  ├── Lint Check
  ├── Unit Test（全モジュール）
  └── Screenshot Test（Roborazzi verify）

Merge to develop:
  ├── 上記全て
  ├── Instrumented Test（Gradle Managed Device）
  └── Dev 環境デプロイ（Firebase App Distribution）

Release:
  ├── 上記全て
  ├── Baseline Profile 生成
  ├── ProGuard / R8 最適化ビルド
  └── Play Store 内部テスト配信
```

---

## 付録: NiA との構成比較表

| レイヤー | NiA | 新規PJ | 差分理由 |
|---------|-----|--------|---------|
| `app/` | シングルアクティビティ + ルートComposable | 同左 | 共通 |
| `core:model` | データクラス | データクラス + API DTO | バックエンド API 連携で DTO が増加 |
| `core:domain` | UseCase（3クラス） | **不使用** | 業務ロジックはバックエンド責務 |
| `core:data` | OfflineFirst Repository | **APIFirst Repository + Mapper** | キャッシュは選択的、Mapper で DTO→Model 変換 |
| `core:database` | Room（全データローカル同期） | **Room（キャッシュ/ドラフトのみ）** | フルオフラインは不要 |
| `core:datastore` | ユーザー設定（Proto DataStore） | ユーザー設定 + **認証トークン** | 社内システム認証の永続化が追加 |
| `core:network` | Retrofit + Demo モック | Retrofit + **Auth Interceptor** + Mock | 認証トークン管理、エラーハンドリング強化 |
| `core:designsystem` | Material3 コンポーネント | 同左（PJ カスタマイズ） | 共通 |
| `core:ui` | ニュースカード等 | **商品カード、注文ステータス等** | 業種に特化した共通コンポーネント |
| `core:navigation` | Navigation 3 | Navigation 2 or 3（要判断） | 安定性 vs 最新 |
| `core:analytics` | Firebase Analytics | 同左 or 代替 | 共通 |
| `core:notifications` | FCM | **要検討** | 業務通知の要否次第 |
| `feature:*` | 6 Feature | **15+ Feature** | サブドメインが多い |
| `sync/` | WorkManager + FCM 同期 | **不使用 or 限定** | APIファーストのため |
| `build-logic/` | Convention Plugin 群 | 同左（カスタマイズ） | 共通 |
| `lint/` | カスタム Lint | 同左 | 共通 |
| `-` | なし | **認証モジュール**（feature:auth） | 社内システム認証が必要 |
| `-` | なし | **環境フレーバー（dev/stg/prod）** | NiA は demo/prod の2環境 |
