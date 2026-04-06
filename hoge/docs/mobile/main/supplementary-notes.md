# 補足事項: app-catalog・その他モジュール解説

> **目的**: 提案書で説明が不足していたモジュールについて補足する。

---

## 目次

1. [app-catalog（デザインシステムショーケース）とは何か](#1-app-catalogデザインシステムショーケースとは何か)
2. [lint（カスタム Lint ルール）](#2-lintカスタム-lint-ルール)
3. [ui-test-hilt-manifest（Hilt テスト用マニフェスト）](#3-ui-test-hilt-manifesthilt-テスト用マニフェスト)
4. [build-logic/convention（ビルド規約プラグイン）](#4-build-logicconventionビルド規約プラグイン)
5. [NiA 固有モジュールで新規PJ不要のもの](#5-nia-固有モジュールで新規pj不要のもの)

---

## 1. app-catalog（デザインシステムショーケース）とは何か

### 一言で

**デザインシステムの全コンポーネントを1画面で一覧表示するための別アプリ**。

### NiA での実装

NiA プロジェクトには `app/`（メインアプリ）と `app-nia-catalog/`（カタログアプリ）の **2つのアプリ** が存在する。

```
nowinandroid/
├── app/               ← メインの Now in Android アプリ
└── app-nia-catalog/   ← デザインコンポーネントのカタログアプリ
```

カタログアプリの実態はたった **2ファイル**:

| ファイル | 内容 |
|---------|------|
| `NiaCatalogActivity.kt` | `ComponentActivity` を継承した Activity。`NiaCatalog()` を呼ぶだけ |
| `Catalog.kt` | `LazyColumn` に全デザインコンポーネントを並べて表示する `@Composable` 関数 |

### 実際の画面イメージ

```
┌──────────────────────────────┐
│    NiA Catalog               │
├──────────────────────────────┤
│                              │
│  [NiaButton]     [Outlined]  │
│  [TextButton]                │
│                              │
│  ── Filter Chips ──          │
│  [✓ Chip1]  [ Chip2]        │
│                              │
│  ── Icon Toggle ──           │
│  [🔖 ON]  [☆ OFF]           │
│                              │
│  ── View Toggle ──           │
│  [Grid ⇔ List]              │
│                              │
│  ── Topic Tag ──             │
│  [Android]  [Kotlin]         │
│                              │
│  ── Tabs ──                  │
│  | Tab1 | Tab2 | Tab3 |     │
│                              │
│  ── Navigation Bar ──        │
│  [Home] [Search] [Profile]   │
│                              │
└──────────────────────────────┘
```

### なぜ必要なのか

| 理由 | 説明 |
|------|------|
| **デザイナーとの連携** | デザインシステムの全パーツを一覧で確認・レビューできる |
| **コンポーネント開発の効率化** | メインアプリを起動せずに個別パーツを開発・デバッグできる |
| **品質保証** | テーマ変更（ダークモード等）の影響を全パーツで一括確認 |
| **新メンバーのオンボーディング** | 「このプロジェクトで使えるUIパーツ一覧」として機能 |

### Spring Boot で例えると

**Swagger UI に近い概念**。  
Swagger が「使える API 一覧」を提供するのと同様に、  
app-catalog は「使える UI パーツ一覧」を提供する。

### Web フロントエンドで例えると

**Storybook**。  
React/Vue の Storybook がコンポーネントカタログを提供するのと同じ。

### 新規PJ での対応

- `app-nia-catalog/` → `app-catalog/` にリネーム
- `core/designsystem` のコンポーネントを全て一覧表示するよう改修
- `core/ui` のビジネスコンポーネント（ProductCard, PriceDisplay等）もカタログに含める
- applicationId は `com.example.retail.catalog` など、メインアプリとは別にする

```kotlin
// app-catalog/build.gradle.kts
android {
    namespace = "com.example.retail.catalog"
    defaultConfig {
        applicationId = "com.example.retail.catalog"  // メインアプリとは別
    }
}
dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)  // ビジネスコンポーネントも含める
}
```

### 必須度

**推奨（必須ではない）**。  
ただし、25億円規模のプロジェクトでは複数チームが並行開発するため、  
コンポーネントの統一性を保つためにほぼ必須。

---

## 2. lint（カスタム Lint ルール）

### 何をしているか

**Android Lint のカスタムルール**を定義するモジュール。  
プロジェクト固有のコーディング規約を静的解析で自動チェックする。

### NiA での実装

NiA では `designsystem` モジュールのコンポーネントの使用を強制するルールを定義:

```
「Material3 の Button() を直接使うな。NiaButton() を使え」
```

これにより、チーム全員がデザインシステムのコンポーネントを使うことを自動保証する。

### 新規PJ での対応

```
lint/
└── src/main/kotlin/.../lint/
    ├── designsystem/
    │   └── DesignSystemDetector.kt    # RetailButton を使え、Button を直接使うな
    └── RetailIssueRegistry.kt         # Lint ルール登録
```

### 必須度

**推奨**。大規模チームでの品質統一に有効。

---

## 3. ui-test-hilt-manifest（Hilt テスト用マニフェスト）

### 何をしているか

**Hilt を使ったインストルメンテッドテスト（実機/エミュレータテスト）用の空マニフェスト**を提供する。

### なぜ必要なのか

技術的な背景:
1. Hilt は `@HiltAndroidApp` が付いた Application クラスを必要とする
2. テスト時には `HiltTestApplication` を使いたい
3. しかし、feature モジュール単体でテストする際、feature モジュール自体には Application がない
4. このモジュールが空の `AndroidManifest.xml` と `HiltTestApplication` の設定を提供する

### Spring Boot で例えると

`@SpringBootTest` を使う際の `@TestConfiguration` に相当。  
テスト用のアプリケーションコンテキストを提供する仕組み。

### 必須度

**Hilt を使う場合は必須**。NiA と同じ構成をそのまま使う。

---

## 4. build-logic/convention（ビルド規約プラグイン）

### 何をしているか

**Gradle の共通設定を Convention Plugin としてまとめたもの**。

### なぜ重要なのか

50+モジュールがあるプロジェクトで、各 `build.gradle.kts` にコンパイルバージョン、  
依存ライブラリ、Compose 設定等を重複記述するのは保守不可能。

Convention Plugin により:

```kotlin
// 各モジュールの build.gradle.kts がシンプルになる

// Before（Convention Plugin なし）
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}
android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // ... 30行以上の設定
}

// After（Convention Plugin あり）
plugins {
    alias(libs.plugins.retail.android.library.compose)
    alias(libs.plugins.retail.hilt)
}
// 以上。設定は Convention Plugin 内で一元管理
```

### NiA の Convention Plugin 一覧

| Plugin | 適用先 | 何をするか |
|--------|-------|-----------|
| `AndroidApplicationConventionPlugin` | `app/` | Application の基本設定（compileSdk, minSdk, targetSdk） |
| `AndroidApplicationComposeConventionPlugin` | `app/` | Compose 関連設定 |
| `AndroidApplicationFlavorsConventionPlugin` | `app/` | demo/prod フレーバー設定 |
| `AndroidLibraryConventionPlugin` | `core:*`, `feature:*` | ライブラリモジュールの基本設定 |
| `AndroidLibraryComposeConventionPlugin` | Compose使用ライブラリ | Compose BOM, Compiler 設定 |
| `AndroidFeatureConventionPlugin` | `feature:*` | Feature モジュール共通設定(Hilt, Navigation等) |
| `HiltConventionPlugin` | Hilt 使用モジュール | Hilt + KSP 設定 |
| `JvmLibraryConventionPlugin` | `core:model`, `core:common` | Pure JVM ライブラリ設定 |
| `AndroidRoomConventionPlugin` | `core:database` | Room + スキーマ出力設定 |
| `AndroidTestConventionPlugin` | テストモジュール | テストフレームワーク設定 |

### 必須度

**必須**。多モジュール構成では Convention Plugin なしは保守不可能。

---

## 5. NiA 固有モジュールで新規PJ不要のもの

### sync/ (WorkManager同期)

NiA には `sync/work/` モジュールがあり、WorkManager でバックグラウンド同期を行う。

```
sync/
├── work/          # WorkManager による定期同期
└── sync-test/     # 同期テスト用
```

**新規PJ では不要**。理由:
- NiA はオフラインファーストで、データを端末に溜めてからサーバーに同期
- 新規PJ は API ファーストで、データは常にサーバーから取得
- プッシュ通知（FCM）は別の仕組みで実装

### core/domain (UseCase層)

NiA には 3つの UseCase がある:

| UseCase | 何をしているか |
|---------|---------------|
| `GetFollowableTopicsUseCase` | TopicsRepository + UserDataRepository を組み合わせて「フォロー可能なトピック一覧」を返す |
| `GetSearchContentsUseCase` | SearchContentsRepository + UserDataRepository を組み合わせて「ユーザー視点の検索結果」を返す |
| `GetRecentSearchQueriesUseCase` | RecentSearchRepository をラップして「最近の検索クエリ」を返す |

**新規PJ では不採用**。理由:
- PJ方針: 業務ロジックはバックエンド責務
- UseCase の代わりに Repository 内の Mapper でデータ変換を行う
- ViewModel が直接 Repository を呼ぶ（1対1の単純な呼び出し）

### benchmarks/

```
benchmarks/
└── src/main/kotlin/...
    ├── startup/         # アプリ起動時間計測
    ├── interests/       # 画面描画パフォーマンス計測
    └── ...
```

**新規PJ では後期フェーズで検討**。GA（一般提供）前のパフォーマンス最適化に有用だが、初期開発フェーズでは不要。

### core/datastore-proto

NiA は Protocol Buffers で DataStore のスキーマを定義しているが、  
新規PJ の設定が単純（テーマ、トークン程度）であれば **Proto DataStore ではなく Preferences DataStore** で十分。

```kotlin
// Proto DataStore（NiA方式 — 型安全だが設定が多い）
message UserPreferences {
    map<string, bool> followed_topic_ids = 1;
    map<string, bool> bookmarked_news_resource_ids = 2;
    // ...
}

// Preferences DataStore（新規PJ方式 — 簡易で十分）
val THEME_KEY = stringPreferencesKey("theme")
val TOKEN_KEY = stringPreferencesKey("auth_token")
```

Proto DataStore にするかは、保存する設定の複雑さ次第で判断。
