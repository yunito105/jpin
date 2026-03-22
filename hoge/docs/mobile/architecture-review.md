# アーキテクチャ方針書レビュー — Now in Android 公式リポジトリとの照合

**レビュー日**: 2026年3月23日  
**レビュー対象**:
- `mobile-system-policy.md`（モバイルシステム方針書）
- `sample-project-guide.md`（サンプルプロジェクトガイド）
- `tech-stack-details.md`（技術スタック詳細ガイド）
- `layer-responsibilities.md`（各層の責務）
- `architecture-comparison.md`（アーキテクチャ比較）

**照合元**: Now in Android（NiA）— Google公式 Android リファレンスプロジェクト  
（Kotlin 2.3.0 / AGP 9.0.0 / Compose BOM 2025.09.01 時点）

---

## 1. 技術スタック採用判定

### ✅ 正しい選定（NiA と一致）

| カテゴリ | 方針書の選定 | NiA での採用 | 判定 |
|---|---|---|---|
| **言語** | Kotlin 2.x | Kotlin 2.3.0 | ✅ 完全一致 |
| **UI** | Jetpack Compose | 100% Compose（XML なし） | ✅ 完全一致 |
| **DI** | Hilt | Hilt 2.59 + KSP | ✅ 完全一致 |
| **非同期** | Coroutines + Flow | Coroutines 1.10.1 + StateFlow | ✅ 完全一致 |
| **API通信** | Retrofit2 + OkHttp3 | Retrofit 2.11.0 + OkHttp 4.12.0 | ✅ 完全一致 |
| **ローカルDB** | Room | Room 2.8.3 | ✅ 完全一致 |
| **設定保存** | DataStore | DataStore 1.2.0（Proto） | ✅ 完全一致 |
| **画像読込** | Coil | Coil 2.7.0（SVG対応付き） | ✅ 完全一致 |
| **バックグラウンド** | WorkManager | WorkManager 2.10.0 | ✅ 完全一致 |
| **JSON変換** | Kotlin Serialization | Kotlinx Serialization 1.8.0 | ✅ 完全一致 |
| **Material Design** | Material Design 3 | Material 3 + Adaptive Layout | ✅ 完全一致 |
| **ナビゲーション** | Navigation Compose | Navigation 2.8.5 + Navigation3 | ✅ 完全一致 |
| **テスト（assertions）** | Google Truth | Truth 1.4.4 | ✅ 完全一致 |
| **テスト（Flow）** | Turbine | Turbine 1.2.0 | ✅ 完全一致 |
| **コードフォーマッタ** | —（未記載） | Spotless + KtLint | ⚠️ 後述 |

**総評**: 技術スタックの選定は Google 公式推奨と **ほぼ完全に一致** しており、的確な選定です。

---

## 2. アーキテクチャパターンの判定

### ✅ 正しい点

| 項目 | 方針書の記述 | NiA の実装 | 判定 |
|---|---|---|---|
| **MVVM + Repository** | 推奨パターンとして選定 | ViewModel + Repository が基本構造 | ✅ |
| **UDF（一方向データフロー）** | UiState を StateFlow で管理 | `StateFlow<UiState>` + `collectAsStateWithLifecycle()` | ✅ |
| **sealed class/interface で状態管理** | MVI 要素の部分採用として言及 | `sealed interface` で Loading/Success/Error を管理 | ✅ |
| **Repository Interface + Impl 分離** | Interface と Implementation を分離 | `TopicsRepository` (interface) + `OfflineFirstTopicsRepository` (impl) | ✅ |
| **Hilt Module で DI バインド** | `@Module` + `@InstallIn` で紐付け | `DataModule` で `@Binds` 使用 | ✅ |
| **オフラインファースト** | Room キャッシュ + オフライン対応 | `OfflineFirst*Repository` パターン | ✅ |

### ⚠️ 要注意の差異点

#### 差異①: UseCase 層を「不要」としている点

**方針書の記述**:
> UseCase 層は不要。Model 層（= Repository）が API・DB・キャッシュ・変換を担う

**NiA の実態**:
- `core/domain/` モジュールに UseCase が存在する
  - `GetFollowableTopicsUseCase`
  - `GetRecentSearchQueriesUseCase`
  - `GetSearchContentsUseCase`
- **複数の Repository を組み合わせる** ロジックを UseCase に配置

```kotlin
// NiA の実装例
class GetFollowableTopicsUseCase @Inject constructor(
    private val topicsRepository: TopicsRepository,
    private val userDataRepository: UserDataRepository,
) {
    operator fun invoke(sortBy: TopicSortField = NONE): Flow<List<FollowableTopic>> =
        combine(
            userDataRepository.userData,
            topicsRepository.getTopics(),
        ) { userData, topics ->
            // 2つの Repository の結果を結合して返す
        }
}
```

**評価**:
```
方針書の「UseCase不要」は業務ロジックをバックエンドに置く前提では理解できるが、
NiAでは「複数Repositoryの結合」というモバイル側の責務がUseCaseに存在する。

推奨: UseCase層を「不要」と断定するのではなく、以下のルールにすべき
  ・単一 Repository で済む場合 → UseCase 不要（ViewModel が直接 Repository を呼ぶ）
  ・複数 Repository の結合が必要な場合 → UseCase を使う
  ・業務ロジック（価格計算等）は UseCase に書かない（バックエンドの責務）
```

---

#### 差異②: Query/Command Repository の分離

**方針書の記述**:
> Repository は Query（参照 = GET）と Command（更新 = POST/DELETE）に分離する

**NiA の実態**:
- `TopicsRepository` は Query/Command を **分離していない**
- 1つの Repository Interface に `getTopics()` も `syncWith()` も含まれる

```kotlin
// NiA: 分離していない
interface TopicsRepository : Syncable {
    fun getTopics(): Flow<List<Topic>>         // Query
    fun getTopic(id: String): Flow<Topic>      // Query
    suspend fun syncWith(synchronizer: ...): Boolean  // Command（同期）
}
```

**評価**:
```
Query/Command 分離はバックエンド CQRS との整合性で有効な設計だが、
NiA は分離していない。これは NiA がニュースアプリであり更新系が少ないため。

小売業アプリ（カート操作・注文送信など更新系が多い）では
方針書通りの Query/Command 分離が合理的。この点は方針書の方が適切。
```

---

#### 差異③: DTO → Domain Model 変換の場所

**方針書の記述**:
> DTO → ドメインモデル変換は Repository 内で行う

**NiA の実態**:
- Network レイヤーに `NetworkTopic` → `TopicEntity` への変換（`asEntity()`）
- Database レイヤーに `TopicEntity` → `Topic` への変換（`asExternalModel()`）
- **3層モデル**: NetworkModel → Entity → DomainModel

```
方針書: DTO → DomainModel（2層）
NiA:    NetworkModel → Entity → DomainModel（3層）
```

**評価**:
```
NiA の 3層変換はオフラインファーストを徹底するための設計。
API レスポンスを必ず Room に保存し、Room から読み出して表示する。
小売業アプリもオフライン対応するなら、NiA と同じ 3層変換が望ましい。

推奨: sample-project-guide.md のフローを以下に更新
  API Response → NetworkDto → Entity（Room保存）→ DomainModel（UI用）
```

---

## 3. 方針書に追記すべき事項

### 🔴 重要（必ず検討すべき）

#### 3-1. モジュール分割戦略

方針書はフォルダ構成のみ記述しているが、NiA は **Gradle マルチモジュール** で構成されている。

```
NiA のモジュール構成（30+ モジュール）:
├── app/                    ← メインアプリ
├── feature/
│   ├── foryou/api/         ← Navigation Key のみ（薄い公開API）
│   ├── foryou/impl/        ← 画面実装（ViewModel + Screen）
│   ├── bookmarks/api/
│   ├── bookmarks/impl/
│   └── ...
├── core/
│   ├── model/              ← ドメインモデル（純Kotlin、Android依存なし）
│   ├── data/               ← Repository 実装
│   ├── database/           ← Room（Entity + DAO）
│   ├── network/            ← Retrofit（API定義 + NetworkModel）
│   ├── domain/             ← UseCase
│   ├── ui/                 ← 共通 Composable
│   ├── designsystem/       ← テーマ・共通コンポーネント
│   ├── common/             ← 共通ユーティリティ
│   ├── datastore/          ← DataStore（Proto）
│   ├── analytics/          ← 分析イベント
│   ├── notifications/      ← プッシュ通知
│   └── testing/            ← テスト用ユーティリティ
└── sync/work/              ← WorkManager 同期
```

**方針書に不足している点**:
- 25億円規模のプロジェクトで単一モジュール（`app/`配下にフォルダ分け）は **ビルド時間・チーム開発の面でリスク**
- 機能ごとにモジュール分割すべき。特に feature モジュールの API/Impl 分離は並行開発を可能にする
- `core/model` を **純 Kotlin モジュール**（Android依存なし）にすることで、テスト高速化・クロスプラットフォーム対応が容易になる

```
推奨: 以下のモジュール分割を最低限検討
  :core:model        → 純 Kotlin（data class のみ）
  :core:network      → Retrofit + NetworkDto
  :core:database     → Room + Entity + DAO
  :core:data         → Repository 実装
  :core:designsystem → テーマ + 共通 Composable
  :feature:products  → 商品一覧・詳細
  :feature:cart      → カート
  :feature:orders    → 注文
  :app               → エントリーポイント + Navigation
```

---

#### 3-2. テスト戦略の詳細化

方針書では `JUnit5 + MockK + Turbine` と記載しているが、NiA の実際のテスト構成と差異がある。

| 項目 | 方針書 | NiA | 推奨 |
|---|---|---|---|
| **Unit Test** | JUnit5 | JUnit4 | JUnit4 で十分。Android との統合がより安定 |
| **Mocking** | MockK | Fake実装（手書き）| **Fake 推奨**（後述） |
| **Flow テスト** | Turbine | Turbine 1.2.0 | ✅ 一致 |
| **Assertions** | —（未記載） | Google Truth | Truth を方針書に追記 |
| **UI テスト** | —（未記載） | ComposeTestRule | 方針書に追記すべき |
| **スクリーンショットテスト** | —（未記載） | Roborazzi | 検討推奨 |
| **ローカル実行** | —（未記載） | Robolectric | 方針書に追記すべき |

**Fake vs Mock について**:
NiA は MockK/Mockito を使わず、**Repository の Fake 実装** をテスト用に手書きしている。

```kotlin
// NiA のテスト方法: Fake 実装
class TestTopicsRepository : TopicsRepository {
    private val topicsFlow = MutableStateFlow(emptyList<Topic>())

    override fun getTopics(): Flow<List<Topic>> = topicsFlow

    fun sendTopics(topics: List<Topic>) {
        topicsFlow.value = topics
    }
}

// テスト
@Test
fun uiState_whenTopicsLoaded_isSuccess() = runTest {
    val repository = TestTopicsRepository()
    val viewModel = TopicsViewModel(repository)

    repository.sendTopics(testTopics)

    assertEquals(TopicsUiState.Success(testTopics), viewModel.uiState.value)
}
```

```
Fake のメリット（NiA が MockK を使わない理由）:
  ① コンパイル時に型チェックされる（Mockはランタイム）
  ② Repository の Interface 変更時にテストも強制的に更新される
  ③ テストが実装詳細（どのメソッドが何回呼ばれたか）に依存しない
  ④ 複数テストで再利用しやすい

推奨: MockK は補助的に使い、Repository のテストは Fake 実装を基本とする
```

---

#### 3-3. DataStore の Preferences vs Proto

方針書では DataStore に軽く触れているが、NiA は **Proto DataStore** を使用している。

```
Preferences DataStore: キー・バリュー形式（SharedPreferences の後継）
Proto DataStore:       Protocol Buffers で型安全なスキーマ定義

NiA は Proto DataStore を使用:
  ・core/datastore-proto/ に .proto ファイルを定義
  ・ユーザー設定（テーマ・フォロー中トピック等）を型安全に管理
  ・スキーマ変更時にコンパイルエラーで検出可能
```

```
推奨:
  ・言語設定・通貨設定・認証トークン等に Proto DataStore を使う
  ・単純な boolean/string 設定には Preferences DataStore で可
  ・方針書に Proto DataStore の採用を明記
```

---

#### 3-4. 同期戦略（Sync Strategy）

NiA はオフラインファーストを実現するために **Change List Sync パターン** を採用している。

```
NiA の同期パターン:
  ① アプリ起動時に WorkManager が同期ジョブを実行
  ② 各 Repository が最後の同期バージョン番号を DataStore に保持
  ③ バックエンドに「バージョン N 以降の変更リスト」を要求
  ④ 変更リスト（追加/更新/削除の ID リスト）を受け取る
  ⑤ 変更があった項目だけを取得して Room を更新
  ⑥ UI は Room を監視しているので自動更新

interface Syncable {
    suspend fun syncWith(synchronizer: Synchronizer): Boolean
}
```

方針書の「オフライン対応」セクションは概念的な記述のみで、具体的な同期戦略が不足している。

```
推奨: 以下を方針書に追加
  ・初回起動時のフルデータ取得フロー
  ・増分同期の仕組み（バージョン番号 or タイムスタンプ）
  ・コンフリクト解決方針（Last Write Wins / Server Wins 等）
  ・同期頻度（WorkManager の Constraints 設定）
  ・Spring Boot 側の Change List API 仕様
```

---

### 🟡 推奨（やるとよい）

#### 3-5. Adaptive Layout（画面サイズ対応）

NiA は **Compose Material 3 Adaptive Layout** を使用して、スマホ・タブレット・折りたたみデバイスに対応。

```
NiA の app/build.gradle.kts:
  implementation(libs.androidx.compose.material3.adaptive)

小売業アプリで重要:
  ・タブレットで商品カタログを見る業務ユーザー
  ・倉庫端末（大画面）でのピッキング操作
  ・折りたたみデバイスでのマルチペイン表示
```

```
推奨: 方針書に Adaptive Layout の検討を追加
  ・WindowSizeClass（Compact / Medium / Expanded）による画面分岐
  ・ListDetailPaneScaffold（一覧 + 詳細の2ペイン表示）
```

---

#### 3-6. Analytics（分析基盤）

NiA は `core/analytics` モジュールで分析イベントを抽象化している。

```kotlin
// NiA の Analytics Interface
interface AnalyticsHelper {
    fun logEvent(event: AnalyticsEvent)
}

// Firebase Analytics 実装
class FirebaseAnalyticsHelper @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsHelper { ... }
```

```
推奨: 方針書に Analytics 戦略を追加
  ・商品閲覧ログ、カート追加イベント、購入完了イベント等
  ・Firebase Analytics or 自社分析基盤の選定
  ・Interface 経由で実装を差し替え可能にする設計
```

---

#### 3-7. Crashlytics / Performance Monitoring

NiA は Firebase Crashlytics と Performance Monitoring を統合。

```
NiA の採用:
  ・firebase-crashlytics（クラッシュレポート）
  ・firebase-perf（パフォーマンス計測）

25億円規模のアプリでは必須:
  ・本番環境のクラッシュ率を 0.1% 以下に維持
  ・API レスポンスタイム計測
  ・画面表示速度の計測
```

---

#### 3-8. Baseline Profile（起動速度最適化）

NiA は Baseline Profile を使用してアプリ起動を最適化。

```
Baseline Profile とは:
  ・アプリの「よく使うコードパス」をプロファイルとして記録
  ・インストール時に事前コンパイル（AOT）
  ・起動速度が 30-50% 改善される場合がある

NiA の構成:
  ・benchmarks/ モジュールで Macrobenchmark テスト
  ・BaselineProfileGenerator で自動生成
  ・CI で自動更新
```

```
推奨: Phase 3（6ヶ月〜）で Baseline Profile を導入
  ・商品一覧画面の起動を最適化
  ・CI パイプラインでプロファイル自動生成
```

---

#### 3-9. Lint カスタムルール

NiA は独自の Lint モジュール（`:lint`）を持っている。

```
NiA のカスタム Lint:
  ・designsystem モジュールを経由せず Material コンポーネントを直接使うことを禁止
  （例: 直接 MaterialTheme.colorScheme を使わず、NiaTheme 経由を強制）

小売業アプリでの活用例:
  ・デザイントークンの直接参照禁止（共通コンポーネント経由を強制）
  ・ハードコードされた文字列の禁止（strings.xml 使用を強制）
  ・Main Thread での API 呼び出し禁止
```

---

#### 3-10. Spotless / KtLint（コードフォーマッタ）

方針書に **コードフォーマッタの記載がない**。NiA は Spotless + KtLint を使用。

```
NiA の構成:
  ・Spotless 8.3.0 + KtLint 1.4.0
  ・CI でフォーマットチェック（`./gradlew spotlessCheck`）
  ・ローカルで自動修正（`./gradlew spotlessApply`）
  ・copyright ヘッダーの自動挿入

推奨: 方針書の CI/CD セクションに追加
  ・チーム全体のコードスタイル統一
  ・PRレビューの無駄なスタイル指摘を削減
```

---

### 🟢 参考（知っておくとよい）

#### 3-11. Feature Module の API/Impl 分離（MAPI パターン）

NiA では各 feature モジュールが `api/` と `impl/` に分離されている。

```
feature/foryou/
  ├── api/     ← NavigationKey のみ。依存が極めて少ない
  └── impl/    ← 画面実装（ViewModel + Screen + UiState）

メリット:
  ・feature モジュール間が直接依存しない（api のみ依存）
  ・ビルドのキャッシュ効率向上
  ・並行開発時のコンフリクト減少
```

---

#### 3-12. Product Flavor 戦略

NiA は `demo` / `prod` の 2フレーバーを持つ。

```
demo:  モックデータ使用。バックエンドなしで動作
prod:  本番 API に接続

方針書の MockInterceptor 方式と同じ発想だが、
NiA は Flavor によるビルドバリアント切り替えで実現。
Flavor の方がより安全（MockInterceptor の削除忘れリスクがない）。
```

---

#### 3-13. JUnit5 → JUnit4 の検討

方針書は JUnit5 を推奨しているが、NiA は JUnit4 を使用。

```
理由:
  ・Android Instrumentation Test は JUnit4 ベース
  ・Robolectric も JUnit4 ベース
  ・JUnit5 を使う場合、android-junit5 プラグインが必要で複雑さが増す
  ・NiA、公式サンプル、ほぼ全ての Android ライブラリが JUnit4

推奨: JUnit4 に統一する（JUnit5 固有の機能は Android テストではほぼ不要）
```

---

## 4. 方針書の記述で正確ではない箇所

### 4-1. 「UseCase 層が空」の表現

方針書とlayer-responsibilities.mdで繰り返し記述されている：
> UseCase 層は不要。空になる。

**修正提案**: 「業務ロジックとしての UseCase は不要」に変更し、複数 Repository 結合の UseCase は許容すると明記。

---

### 4-2. `collectAsState` → `collectAsStateWithLifecycle`

方針書の複数箇所で `collectAsState()` が使用されている。

```kotlin
// 方針書の記述
val uiState by viewModel.uiState.collectAsState()

// NiA の実装（推奨）
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

```
違い:
  collectAsState()                 → バックグラウンドでも Flow を収集し続ける
  collectAsStateWithLifecycle()    → ライフサイクルに従い自動で停止/再開

Android 公式は collectAsStateWithLifecycle() を推奨。
バッテリー消費・不要なネットワーク通信を削減。
```

---

### 4-3. kapt → KSP

tech-stack-details.md のHiltセクションで「kapt」に言及があるが、NiA は **KSP（Kotlin Symbol Processing）** を使用。

```
kapt: Java のアノテーションプロセッサを Kotlin で使う仕組み（レガシー）
KSP:  Kotlin ネイティブのシンボル処理（高速、Google推奨）

Hilt 2.59 は KSP に完全対応。kapt は非推奨。
ビルド速度が 20-30% 改善される。
```

---

## 5. 最終サマリー

### 方針書の評価

| 評価項目 | スコア | コメント |
|---|---|---|
| **技術スタック選定** | ⭐⭐⭐⭐⭐ | NiA 公式と完全一致。非常に的確 |
| **アーキテクチャ方針** | ⭐⭐⭐⭐☆ | MVVM + Repository は正しい。UseCase の扱いを微修正 |
| **層の責務定義** | ⭐⭐⭐⭐☆ | 詳細で分かりやすい。3層変換の補足を追加 |
| **テスト戦略** | ⭐⭐⭐☆☆ | Fake パターン・UI テスト・スクリーンショットテストの追記が必要 |
| **モジュール分割** | ⭐⭐☆☆☆ | フォルダ構成のみで Gradle モジュール分割の記述が不足 |
| **運用面（CI/CD, 監視）** | ⭐⭐⭐☆☆ | Crashlytics・Analytics・Baseline Profile の追記が必要 |

### 対応優先度

```
🔴 必須対応:
  1. Gradle マルチモジュール分割戦略の策定
  2. テスト戦略の詳細化（Fake パターン、UI テスト、Robolectric）
  3. 同期戦略（オフラインファースト）の具体化
  4. UseCase 層の方針を修正（「不要」→「業務ロジックは書かないが結合処理は許可」）

🟡 推奨対応:
  5. Adaptive Layout（タブレット/折りたたみ対応）の検討
  6. Analytics / Crashlytics / Performance Monitoring の追加
  7. Proto DataStore の採用方針
  8. Spotless / KtLint によるコードフォーマット統一

🟢 後日対応可:
  9. Baseline Profile による起動最適化
  10. カスタム Lint ルール
  11. Feature Module の API/Impl 分離
  12. JUnit4 への統一検討
```

### コードサンプルの修正箇所

```
以下を方針書・ガイド類で更新する:
  ① collectAsState() → collectAsStateWithLifecycle() に置換
  ② kapt の記述 → KSP に変更
  ③ サンプルコードに @Inject constructor を追加（Hilt 対応）
  ④ DTO 変換を 3層（NetworkDto → Entity → DomainModel）に変更
```
