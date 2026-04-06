# 技術用語集・技術背景解説書

> 対象読者: テックリード・開発リーダー層（Java/Spring Boot 経験はあるが、モバイル（Android/Kotlin）開発の知見が少ない方）

---

## 目次

1. [ソフトウェアアーキテクチャ用語](#1-ソフトウェアアーキテクチャ用語)
2. [Android / モバイル基本用語](#2-android--モバイル基本用語)
3. [Kotlin 言語機能](#3-kotlin-言語機能)
4. [Jetpack Compose（UI フレームワーク）](#4-jetpack-composeui-フレームワーク)
5. [DI（依存性注入）関連](#5-di依存性注入関連)
6. [データ層・永続化](#6-データ層永続化)
7. [ネットワーク通信](#7-ネットワーク通信)
8. [ナビゲーション（画面遷移）](#8-ナビゲーション画面遷移)
9. [ビルドシステム（Gradle）](#9-ビルドシステムgradle)
10. [テスト関連](#10-テスト関連)
11. [CI/CD・品質管理](#11-cicd品質管理)
12. [設計パターン・原則](#12-設計パターン原則)
13. [バックエンド連携用語](#13-バックエンド連携用語)

---

## 1. ソフトウェアアーキテクチャ用語

### MVVM（Model-View-ViewModel）

```
┌────────────┐    状態を観測     ┌──────────────┐    データ取得    ┌────────────┐
│    View     │ ←───────────── │  ViewModel    │ ──────────→ │   Model     │
│  （画面）    │ ──────────→  │（状態管理）    │              │（データ）    │
└────────────┘   イベント通知   └──────────────┘              └────────────┘
```

**Java/Spring Boot での対比:**
| MVVM（モバイル） | MVC（Spring Boot） | 役割 |
|-----------------|-------------------|------|
| View (Compose画面) | View (Thymeleaf/JSP) | 画面の描画 |
| ViewModel | Controller | ユーザー操作の処理、状態管理 |
| Model | Service + Repository | データの取得・加工 |

**なぜ MVVM か:**
- View が ViewModel を「観測」するだけで、直接更新しない → テストしやすい
- Android のライフサイクル（画面回転、バックグラウンド移行）に強い
- Spring Boot の Controller は1リクエスト1レスポンスだが、Android の ViewModel は「常に最新状態を保持し続ける」

---

### UDF（Unidirectional Data Flow / 単方向データフロー）

```
    ユーザー操作（ボタンタップ等）
         │
         ↓
    ┌─────────┐
    │ Event   │ ← ユーザーのアクション（例: 「検索」ボタンを押した）
    └────┬────┘
         ↓
    ┌─────────┐
    │ViewModel│ ← イベントを受けてデータを取得・加工
    └────┬────┘
         ↓
    ┌─────────┐
    │ State   │ ← 画面の状態（例: 「読み込み中」「検索結果10件」「エラー」）
    └────┬────┘
         ↓
    ┌─────────┐
    │  UI     │ ← 状態に応じて画面を描画
    └─────────┘
         │
         └──→ ユーザー操作 → Event → ... （ループ）
```

**ポイント:**
- データは常に **一方向** に流れる（Event → ViewModel → State → UI）
- UI が直接データを書き換えることはない
- Spring Boot でいうと「リクエスト→Controller→Service→レスポンス」の単方向と同じ思想
- **バグが減る理由**: 状態の変更元が1箇所（ViewModel）に限定されるため、「どこで値が変わったかわからない」問題が起きない

---

### ヘキサゴナルアーキテクチャ（Hexagonal Architecture / Ports and Adapters）

```
              ┌─────────────────────────────┐
              │       Domain (業務ロジック)    │
              │                             │
    入力     │  ┌────────┐   ┌──────────┐  │     出力
   Port ←──│──│ UseCase │───│ Domain   │──│──→ Port
  (REST等)   │  └────────┘   │ Model    │  │   (DB等)
              │               └──────────┘  │
              └─────────────────────────────┘
                       ↑             ↑
                    Adapter        Adapter
                  (Controller)   (Repository実装)
```

**Spring Boot での適用:**
- **Port** = インターフェース（Java の `interface`）。「何をするか」を定義
- **Adapter** = 実装クラス。「どうやるか」を定義
- **入力 Port**: REST Controller が呼ぶインターフェース
- **出力 Port**: DB アクセスのインターフェース
- **メリット**: DB を Oracle → PostgreSQL に変えても、Adapter（実装）の差し替えだけで済む

---

### CQRS（Command Query Responsibility Segregation / コマンドクエリ責務分離）

```
  ┌────────────────┐         ┌───────────────────┐
  │ Command (書込)  │         │  Query (読取)       │
  │                │         │                   │
  │ 受注登録       │         │ 受注一覧取得        │
  │ 在庫引当       │         │ 商品検索            │
  │ 出荷指示       │         │ ダッシュボード表示   │
  └───────┬────────┘         └────────┬──────────┘
          │                           │
          ↓                           ↓
  ┌───────────────┐          ┌──────────────────┐
  │ Write Model   │          │ Read Model        │
  │ (正規化DB)     │          │ (読取専用View等)   │
  └───────────────┘          └──────────────────┘
```

**簡易 CQRS の意味:**
- フル CQRS は書込DB と読取DB を完全分離する（イベントソーシング等）
- **簡易 CQRS** はDB は共通だが、**処理のエントリポイントを Command / Query で分ける**
- Spring Boot では `CommandService` と `QueryService` を分けるイメージ
- **モバイルへの影響**: API が「登録系（POST/PUT）」と「参照系（GET）」で別エンドポイントになる可能性がある

---

### DDD（Domain-Driven Design / ドメイン駆動設計）

```
┌─ 戦略的設計（大きな設計） ─────────────────────┐
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 商品管理  │  │ 受注管理  │  │ 在庫管理  │   │  ← サブドメイン
│  │(Bounded  │  │(Bounded  │  │(Bounded  │   │
│  │ Context) │  │ Context) │  │ Context) │   │
│  └──────────┘  └──────────┘  └──────────┘   │
│       ↕              ↕              ↕        │
│    コンテキスト間の連携ルール                    │
└──────────────────────────────────────────────┘

┌─ 戦術的設計（小さな設計） ─────────────────────┐
│                                              │
│  Entity（識別子を持つオブジェクト: 注文, 商品）   │
│  Value Object（識別子なし: 金額, 住所）         │
│  Aggregate（整合性の単位: 注文+明細）           │
│  Domain Service（複数エンティティの操作）        │
│  Repository（データアクセス抽象化）              │
└──────────────────────────────────────────────┘
```

**なぜ今回の PJ に関係するか:**
- バックエンドが DDD で設計される → API のレスポンス構造が DDD のモデルに沿う
- サブドメイン（商品/受注/在庫/物流/店舗）= モバイルの feature モジュール分割基準になる
- **Bounded Context（境界づけられたコンテキスト）**: 同じ「商品」でも、受注管理での「商品」と在庫管理での「商品」は異なる属性を持つ。モバイル側でもこの境界を意識してモデルを分ける

---

### Repository パターン

```
  ViewModel
      │
      ↓ データが欲しい
  ┌──────────────────┐
  │ Repository       │ ← インターフェース（What: 何を取得するか）
  │ (interface)      │
  └───────┬──────────┘
          │
    ┌─────┴──────┐
    ↓            ↓
┌──────────┐ ┌──────────┐
│ API実装   │ │ Cache実装 │  ← 具体的実装（How: どう取得するか）
│(Retrofit) │ │ (Room)   │
└──────────┘ └──────────┘
```

**Java/Spring Boot との対比:**
- Spring の `@Repository` と概念は同じ
- **違い**: Android の Repository は `Flow`（後述）でリアクティブにデータを返す。Spring は1回のメソッド呼び出しで1回返す。

---

### Clean Architecture

```
  ┌──────────────────────────────────────────┐
  │ UI Layer         (Compose画面, ViewModel)  │ ← 外側（変わりやすい）
  ├──────────────────────────────────────────┤
  │ Domain Layer     (UseCase, Model)         │ ← 中間
  ├──────────────────────────────────────────┤
  │ Data Layer       (Repository, API, DB)    │ ← 外側（変わりやすい）
  └──────────────────────────────────────────┘

  依存の方向: 外側 → 内側（UIはDomainに依存するが、DomainはUIに依存しない）
```

**今回の PJ では:**
- **Domain Layer（UseCase）は不採用** — 業務ロジックはバックエンドにあるため
- `UI Layer → Data Layer` の2層構成（ViewModel が直接 Repository を呼ぶ）

---

### オフラインファースト vs API ファースト

| | オフラインファースト（NiA） | APIファースト（新規PJ） |
|---|---|---|
| **データの正** | ローカルDB（Room） | バックエンドAPI |
| **画面表示** | ローカルDBから即表示 | API呼び出し → 表示 |
| **同期** | バックグラウンドでサーバーと同期 | 通信時にリアルタイム取得 |
| **オフライン時** | 完全動作 | エラー表示 or キャッシュ |
| **適するケース** | ニュースアプリ、メモアプリ | 業務アプリ（データ鮮度が重要） |
| **実装コスト** | 高（同期ロジック複雑） | 低〜中 |

---

## 2. Android / モバイル基本用語

### Activity（アクティビティ）

**一言で**: Android アプリの「画面の器」。Spring Boot でいう `@Controller` クラスのようなもの。

```
┌─ Application ──────────────────────────┐
│                                        │
│  ┌─ MainActivity ─────────────────┐    │
│  │                                │    │
│  │  ┌─ Compose UI ─────────────┐  │    │
│  │  │  画面A → 画面B → 画面C    │  │    │  ← 全画面を1つのActivityで管理
│  │  └──────────────────────────┘  │    │    = シングルアクティビティ
│  └────────────────────────────────┘    │
└────────────────────────────────────────┘
```

**シングルアクティビティ**: Activity は1つだけ作り、画面遷移は Compose + Navigation で管理する。昔は画面ごとに Activity を作っていたが、現在は **非推奨**。

---

### ライフサイクル（Lifecycle）

Android 特有の概念。アプリは常にOSから「停止」「破棄」される可能性がある。

```
  onCreate   → 画面が作られた（初回）
  onStart    → 画面が見えるようになった
  onResume   → 画面が操作可能になった（フォアグラウンド）
  onPause    → 他のアプリが前面に来た
  onStop     → 画面が見えなくなった（バックグラウンド）
  onDestroy  → 画面が破棄された（メモリ回収）

  画面回転時: onDestroy → onCreate（画面が一度破棄されて再作成される）
```

**なぜ重要か:**
- Spring Boot はサーバーが起動したら基本ずっと動いている
- Android はOSがアプリを勝手に kill したり、画面回転で再作成したりする
- **ViewModel** が解決: 画面が破棄されてもデータが消えない（ViewModel はライフサイクルをまたいで生存する）

---

### Context（コンテキスト）

**一言で**: Android アプリの「実行環境への参照」。リソース（文字列、画像）へのアクセス、システムサービスの取得、画面の起動などに必要。

- `Application Context`: アプリ全体で共有。シングルトン的な存在
- `Activity Context`: 画面に紐づく。画面が破棄されると無効になる

**Spring Boot 対比**: `ApplicationContext`（Spring の DI コンテナ）に近い概念。

---

### Intent（インテント）

**一言で**: Android コンポーネント間の「メッセージ」。画面の起動、外部アプリの呼び出し、ブロードキャスト送信に使う。

```kotlin
// カメラアプリを起動（暗黙的Intent）
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
startActivity(intent)

// 自アプリの画面を起動（明示的Intent）
val intent = Intent(this, SettingsActivity::class.java)
startActivity(intent)
```

**注意**: Compose + Navigation を使う場合、画面遷移に Intent は基本不要。外部アプリ連携（カメラ、ブラウザ起動等）でのみ使用。

---

### Manifest（AndroidManifest.xml）

**一言で**: アプリの「設計図」。OS に対してアプリの構成要素（Activity、権限、対応バージョン等）を宣言する XML ファイル。

```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />  <!-- ネット通信許可 -->
    <uses-permission android:name="android.permission.CAMERA" />    <!-- カメラ許可 -->

    <application>
        <activity android:name=".MainActivity"                      <!-- 画面定義 -->
            android:exported="true">
            <intent-filter>                                         <!-- アプリ起動時の画面 -->
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Spring Boot 対比**: `application.yml` + `pom.xml` に近い役割（設定宣言 + 依存宣言）。

---

### ProGuard / R8

**一言で**: リリースビルド時にコードを **難読化・最小化** するツール。

```
Before: class UserRepository { fun getUserById(id: String) ... }
After:  class a { fun b(c: String) ... }
```

- **R8**: ProGuard の後継。Google が開発。現在のデフォルト
- **目的**: APK サイズの削減 + リバースエンジニアリング防止
- **proguard-rules.pro**: 「このクラスは難読化しないで」といったルールを書くファイル

---

### APK / AAB

| 形式 | 説明 |
|------|------|
| **APK** (Android Package) | Android アプリのインストールファイル。Spring Boot でいう `.jar` |
| **AAB** (Android App Bundle) | Google Play 配信用の形式。デバイスに応じて最適化された APK を自動生成 |

---

### SDK バージョン用語

```
compileSdk = 35    ← コンパイル時に使う Android SDK バージョン（最新推奨）
targetSdk  = 35    ← 「このバージョンの動作を想定している」宣言
minSdk     = 24    ← サポートする最低 Android バージョン（Android 7.0）
```

**minSdk の選択**: 低いほど幅広い端末に対応するが、新しい API が使えなくなる。`24`（Android 7.0）はシェア 99%+ をカバー。

---

## 3. Kotlin 言語機能

### Coroutine（コルーチン）

**一言で**: Kotlin の **非同期処理の仕組み**。Java の `CompletableFuture` や RxJava の `Observable` に代わるもの。

```kotlin
// Java (Spring Boot) の非同期
CompletableFuture.supplyAsync(() -> {
    return repository.findById(id);
});

// Kotlin Coroutine
viewModelScope.launch {
    val result = repository.findById(id)  // 一見同期的だが、非同期に実行される
}
```

**キーワード:**

| キーワード | 意味 | Spring Boot 対比 |
|-----------|------|-----------------|
| `suspend` | 「この関数は中断できる」印。非同期処理用 | `@Async` 的な役割 |
| `launch` | コルーチンを起動する。Fire-and-forget | `CompletableFuture.runAsync()` |
| `async/await` | コルーチンを起動し、結果を待つ | `CompletableFuture.supplyAsync().get()` |
| `viewModelScope` | ViewModel のライフサイクルに紐づくスコープ。画面が破棄されると自動キャンセル | リクエストスコープ的な自動管理 |
| `Dispatchers.IO` | I/O 向けスレッドプール | `@Async` のスレッドプール |
| `Dispatchers.Main` | UI スレッド（メインスレッド） | （該当なし。サーバーに UI スレッドはない） |

---

### Flow（フロー）

**一言で**: Kotlin の **リアクティブストリーム**。値が時間とともに流れてくる「パイプ」。Java の `Stream` + RxJava の `Observable` に近い。

```kotlin
// Spring Boot: 1回呼んで1回返る
fun getUser(id: String): User

// Android (Flow): データが変わるたびに新しい値が流れる
fun observeUser(id: String): Flow<User>
```

```
データベース変更 → Flow がデータを流す → ViewModel が受け取る → UI が自動更新

  Room DB                Flow              ViewModel           Compose UI
  ┌─────┐    insert     ┌────┐  collect   ┌───────┐  state   ┌────────┐
  │     │ ──────────→  │ ○──│─────────→ │       │────────→│        │
  │     │               │ ○──│            │       │         │ 再描画  │
  │     │    update     │ ○──│            │       │         │        │
  │     │ ──────────→  │ ○──│─────────→ │       │────────→│        │
  └─────┘               └────┘            └───────┘         └────────┘
```

**派生型:**

| 型 | 特徴 | 使いどころ |
|----|------|-----------|
| `Flow<T>` | コールド。collect されるまで動かない | データ層 |
| `StateFlow<T>` | ホット。常に最新値を保持。`BehaviorSubject` 相当 | ViewModel → UI の状態公開 |
| `SharedFlow<T>` | ホット。イベント配信用 | ワンショットイベント（トースト表示等） |
| `MutableStateFlow<T>` | 書き換え可能な StateFlow | ViewModel 内部の状態管理 |

---

### sealed class / sealed interface

**一言で**: 「この型の子クラスはこれだけ」と限定する仕組み。Java の `sealed class`（Java 17+）と同等。

```kotlin
// UI の状態を「Loading / Success / Error のどれか」に限定
sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(val products: List<Product>) : ProductListUiState
    data class Error(val message: String) : ProductListUiState
}

// when 式で全パターンを網羅（else 不要 = 漏れがコンパイルエラーになる）
when (uiState) {
    is Loading -> ShowLoadingSpinner()
    is Success -> ShowProductList(uiState.products)
    is Error   -> ShowErrorMessage(uiState.message)
}
```

**なぜ重要か**: UI 状態を型安全に管理できる。新しい状態を追加したとき、`when` の処理漏れがコンパイルエラーで検出される。

---

### data class

**一言で**: `equals()`, `hashCode()`, `toString()`, `copy()` を自動生成する不変（イミュータブル）データクラス。Java の `record`（Java 16+）に相当。

```kotlin
data class Product(
    val id: String,
    val name: String,
    val price: BigDecimal,
)

// copy() で一部だけ変更した新インスタンスを作れる
val updated = product.copy(price = BigDecimal("1200"))
```

---

### Extension Function（拡張関数）

**一言で**: 既存のクラスにメソッドを「外から追加」できる Kotlin の機能。

```kotlin
// String に新しい関数を追加
fun String.toSlug(): String = this.lowercase().replace(" ", "-")

// 使い方
"Hello World".toSlug()  // → "hello-world"
```

**Spring Boot 対比**: Java にはない機能。ユーティリティクラスの static メソッドを、あたかもインスタンスメソッドのように呼べる。

---

### operator fun invoke

```kotlin
class GetProductsUseCase {
    operator fun invoke(query: String): Flow<List<Product>> {
        // ...
    }
}

// 使い方: 関数のように呼べる
val useCase = GetProductsUseCase()
useCase("keyword")   // ← useCase.invoke("keyword") のシンタックスシュガー
```

NiA の UseCase クラスはこのパターンを使っている。新規PJ では UseCase を使わないが、概念として知っておくと良い。

---

## 4. Jetpack Compose（UI フレームワーク）

### Jetpack Compose とは

**一言で**: Android の **宣言的 UI フレームワーク**。XML レイアウトを書かず、Kotlin コードで UI を構築する。

```
旧方式（View システム）:
  activity_main.xml (XMLでレイアウト定義)
  + MainActivity.kt (Kotlin でロジック = findViewById等)
  = UI とロジックが分離（ファイルが分かれる）

新方式（Compose）:
  ProductScreen.kt (KotlinだけでUIもロジックも書く)
  = UI とロジックが1ファイルに統合
```

**Spring Boot 対比:**
- 旧方式 = Thymeleaf（HTML テンプレート + Controller）
- Compose = React や Vue のような **コンポーネントベースの宣言的 UI**

---

### @Composable

**一言で**: 「この関数は UI を描画する関数です」というマーカー。

```kotlin
@Composable
fun ProductCard(
    product: Product,         // 表示するデータ
    onClick: () -> Unit,      // クリック時の処理（イベントコールバック）
) {
    Card(onClick = onClick) {
        Text(text = product.name)
        Text(text = "${product.price}円")
    }
}
```

**ルール:**
- `@Composable` 関数は他の `@Composable` 関数からしか呼べない
- 関数名は **PascalCase**（通常の Kotlin 関数は camelCase）
- **副作用を持たない**: 同じ引数 → 同じ UI を出力

---

### Recomposition（再構成）

**一言で**: Compose が **データ変更を検知して、変更された部分だけ再描画** する仕組み。

```
State: count = 0
  ┌──────────────────┐
  │  Counter: 0      │  ← 全体を描画
  │  [+] ボタン       │
  └──────────────────┘

State: count = 1  (ボタンタップ)
  ┌──────────────────┐
  │  Counter: 1      │  ← Text だけ再描画（ボタンは再描画しない）
  │  [+] ボタン       │
  └──────────────────┘
```

**パフォーマンスへの影響**: 不必要なRecompositionが頻発するとカクつく。`compose_compiler_config.conf` で `@Stable` マークすることで最適化。

---

### State（状態管理）

```kotlin
// ViewModel 側: 状態を公開
val uiState: StateFlow<ProductListUiState> = ...

// Compose 側: 状態を購読
@Composable
fun ProductListScreen(viewModel: ProductListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    //                                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //    StateFlow を Compose の State に変換。
    //    ライフサイクル対応: 画面が見えている間だけ collect する

    when (uiState) {
        is Loading -> CircularProgressIndicator()
        is Success -> LazyColumn { items(uiState.products) { ProductCard(it) } }
        is Error   -> Text("エラー: ${uiState.message}")
    }
}
```

---

### Material Design 3 / Material3

**一言で**: Google が策定した **UI デザインガイドライン** の第3版。色、タイポグラフィ、コンポーネントの仕様。

- **Material You**: ユーザーの壁紙からテーマカラーを自動生成（Dynamic Color）
- **コンポーネント**: Button, Card, TextField, TopAppBar, NavigationBar, BottomSheet 等
- **Compose Material3**: Material Design 3 の Compose 実装ライブラリ

---

### Scaffold（スキャフォールド）

**一言で**: アプリの **基本レイアウト骨格**。トップバー、ボトムナビ、コンテンツ領域、FAB 等を配置する「枠組み」。

```
┌─ Scaffold ──────────────────────────┐
│ ┌─ TopAppBar ─────────────────────┐ │
│ │ 商品一覧            🔍  ⚙️      │ │
│ └─────────────────────────────────┘ │
│ ┌─ Content ───────────────────────┐ │
│ │                                 │ │
│ │  (ここに各画面のコンテンツ)      │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│ ┌─ BottomNavigationBar ──────────┐ │
│ │ 🏠ホーム │ 📦商品 │ 📋受注 │ ...│ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

### LazyColumn / LazyRow

**一言で**: **仮想スクロールリスト**。画面に見えている分だけレンダリングする。

- `LazyColumn`: 縦スクロールリスト（RecyclerView の代替）
- `LazyRow`: 横スクロールリスト
- Spring Boot 対比: ページネーションと似た発想（必要な分だけ処理する）

---

### Adaptive Layout（アダプティブレイアウト）

**一言で**: **画面サイズに応じてレイアウトを自動変更** する仕組み。

```
スマートフォン（縦）:              タブレット（横）:
┌──────────┐                   ┌──────────┬──────────┐
│ 一覧      │                   │ 一覧      │ 詳細      │
│ ├ 商品A   │                   │ ├ 商品A   │          │
│ ├ 商品B   │ →タップ→ 詳細画面  │ ├ 商品B ● │ 商品B    │
│ └ 商品C   │                   │ └ 商品C   │ の詳細   │
└──────────┘                   └──────────┴──────────┘
                               ↑ List-Detail パターン
```

- `NavigationSuiteScaffold`: 画面幅に応じて BottomBar / NavigationRail / NavigationDrawer を自動切替

---

## 5. DI（依存性注入）関連

### Hilt（ヒルト）

**一言で**: Android 専用の **依存性注入（DI）フレームワーク**。Google が Dagger の上に構築。

**Spring Boot との対比:**

| Hilt (Android) | Spring Boot | 役割 |
|---------------|-------------|------|
| `@HiltAndroidApp` | `@SpringBootApplication` | アプリのエントリポイント |
| `@HiltViewModel` | `@Controller` / `@Service` | DI 対象クラスのマーク |
| `@Inject constructor(...)` | `@Autowired` / コンストラクタ注入 | 依存を注入する場所 |
| `@Module` + `@Provides` | `@Configuration` + `@Bean` | インスタンスの生成方法を定義 |
| `@Binds` | （該当なし） | インターフェース → 実装のバインド |
| `@Singleton` | `@Singleton` / デフォルト | スコープ指定 |
| `@ViewModelScoped` | `@RequestScope` | ViewModel の生存期間 |

```kotlin
// Spring Boot
@Service
public class ProductService {
    @Autowired
    private ProductRepository repository;
}

// Hilt (Android)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,  // ← Hilt が自動注入
) : ViewModel()
```

---

### KSP（Kotlin Symbol Processing）

**一言で**: Kotlin のコンパイル時に **コードを自動生成** する仕組み。Java の APT（Annotation Processing Tool）の Kotlin 版。

- Hilt: `@Inject`, `@Module` 等を読んで DI コードを自動生成
- Room: `@Entity`, `@Dao` 等を読んで SQL コードを自動生成
- Spring Boot 対比: Lombok の `@Data` がコンパイル時にコードを生成するのと同じ仕組み

---

### Scope（スコープ）

```
@Singleton           → アプリ全体で1つのインスタンス（Application スコープ）
@ActivityScoped      → Activity が生きている間、1つのインスタンス
@ViewModelScoped     → ViewModel が生きている間、1つのインスタンス
```

Spring Boot でいう `@Singleton`, `@RequestScope`, `@SessionScope` に対応。

---

## 6. データ層・永続化

### Room

**一言で**: Android 公式の **ORM（Object-Relational Mapping）**。SQLite の上に構築。Spring Boot の JPA/Hibernate に相当。

```kotlin
// JPA (Spring Boot)                     // Room (Android)
@Entity                                  @Entity(tableName = "products")
public class Product {                   data class ProductEntity(
    @Id                                      @PrimaryKey
    private Long id;                         val id: String,
    private String name;                     val name: String,
}                                        )

// JPA Repository                        // Room DAO
public interface ProductRepo             @Dao
    extends JpaRepository<Product, Long> interface ProductDao {
{                                            @Query("SELECT * FROM products")
    List<Product> findAll();                 fun getAll(): Flow<List<ProductEntity>>
}                                        }
```

**NiA での使い方**: 全データをローカルDBに保存（オフラインファースト）
**新規PJ での想定**: キャッシュ、ドラフト保存、検索履歴に限定使用

---

### Entity（エンティティ）

Room で DB テーブルに対応するデータクラス。Spring Boot の `@Entity` と同じ。

---

### DAO（Data Access Object）

Room で DB への CRUD 操作を定義するインターフェース。Spring Boot の `@Repository` / `JpaRepository` と同じ。

---

### DataStore

**一言で**: Android 公式の **軽量データ永続化**。SharedPreferences の後継。

| 種類 | 用途 | Spring Boot 対比 |
|------|------|-----------------|
| **Preferences DataStore** | キーバリュー形式。シンプルな設定値 | `application.properties` の動的版 |
| **Proto DataStore** | Protocol Buffers で型安全に構造化データを保存 | Redis にシリアライズされたオブジェクトを保存するイメージ |

```kotlin
// ユーザー設定の保存例
dataStore.updateData { prefs ->
    prefs.copy(darkMode = true, language = "ja")
}

// ユーザー設定の読み取り（Flow で変更を監視）
dataStore.data.map { it.darkMode }
```

**SharedPreferences との違い:**
- SharedPreferences: 同期 API（ANR リスク）、型安全でない
- DataStore: 非同期 API（Flow ベース）、型安全（Proto 使用時）

---

### Protocol Buffers（Protobuf / プロトバフ）

**一言で**: Google が開発した **バイナリシリアライゼーション形式**。JSON より高速・小サイズ。

```protobuf
// user_preferences.proto
message UserPreferences {
    string selected_store_id = 1;
    bool dark_mode = 2;
    string language = 3;
    ThemeConfig theme = 4;
}
```

**Spring Boot 対比**: gRPC でよく使われる。REST API の JSON に対して、より効率的なシリアライゼーション。Android では DataStore の保存形式として使用。

---

### FTS（Full-Text Search / 全文検索）

Room に組み込まれた SQLite の全文検索機能。`LIKE '%keyword%'` より高速。NiA ではニュースとトピックの検索に使用。

---

### Migration（マイグレーション）

DB スキーマのバージョン管理。テーブル追加・カラム変更時に既存データを壊さずにアップグレードする仕組み。Spring Boot の Flyway / Liquibase に相当。

---

## 7. ネットワーク通信

### Retrofit（レトロフィット）

**一言で**: Android の **REST API クライアントライブラリ**。インターフェース定義から HTTP 通信コードを自動生成。

```kotlin
// Spring Boot の RestTemplate/WebClient 相当
interface ProductApi {
    @GET("api/v1/products")
    suspend fun getProducts(@Query("q") query: String): ApiResponse<List<ProductDto>>

    @POST("api/v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<OrderDto>

    @GET("api/v1/products/{id}")
    suspend fun getProduct(@Path("id") productId: String): ApiResponse<ProductDto>
}
```

**Spring Boot 対比:**

| Retrofit (Android) | Spring Boot | 役割 |
|-------------------|-------------|------|
| `@GET`, `@POST` | `@GetMapping`, `@PostMapping` | HTTP メソッド指定 |
| `@Query` | `@RequestParam` | クエリパラメータ |
| `@Path` | `@PathVariable` | パス変数 |
| `@Body` | `@RequestBody` | リクエストボディ |
| `@Header` | `@RequestHeader` | ヘッダー |

---

### OkHttp（オーケーエイチティーティーピー）

**一言で**: HTTP 通信の **低レベルライブラリ**。Retrofit の内部で使用。

- **Interceptor**: リクエスト/レスポンスを **横断的に処理** するフィルター
  - `AuthInterceptor`: 全リクエストに認証トークンを付与
  - `LoggingInterceptor`: 通信ログを出力
  - Spring Boot 対比: `HandlerInterceptor` / `Filter` と同じ概念

---

### Kotlinx Serialization

**一言で**: Kotlin 公式の **JSON シリアライゼーション/デシリアライゼーション**。Jackson / Gson の代替。

```kotlin
@Serializable
data class ProductDto(
    @SerialName("product_id") val id: String,
    @SerialName("product_name") val name: String,
    val price: Double,
)
```

- Spring Boot の `@JsonProperty` → Kotlin の `@SerialName`
- Spring Boot の Jackson → Kotlin の Kotlinx Serialization

---

### DTO（Data Transfer Object）

**一言で**: API 通信用のデータクラス。バックエンドの API レスポンスをそのまま表現する。

```
API Response (JSON) → DTO (Kotlin data class) → Mapper → Model (アプリ内モデル)
```

**なぜ DTO とModel を分けるか:**
- API レスポンスの形式が変わっても、アプリ内のモデルは影響を受けない
- API のフィールド名（`snake_case`）とアプリ内フィールド名（`camelCase`）を変換できる
- API レスポンスに不要なフィールドを除外できる

---

### Coil（コイル）

**一言で**: Android の **画像読み込みライブラリ**。URL → 画像表示を非同期で行う。Kotlin Coroutines ベース。

```kotlin
// Compose での使用
AsyncImage(
    model = "https://example.com/product-image.jpg",
    contentDescription = "商品画像",
)
```

- メモリキャッシュ、ディスクキャッシュ、プレースホルダー表示を自動管理
- 類似ライブラリ: Glide, Picasso

---

## 8. ナビゲーション（画面遷移）

### Jetpack Navigation

**一言で**: Android 公式の **画面遷移管理ライブラリ**。

```
Navigation 2 (Compose版):     現在の安定版。NavHost + NavController で遷移を管理
Navigation 3:                  2025年登場の新API。型安全性が向上。NiA が採用
```

**基本概念:**

| 用語 | 説明 | Web 対比 |
|------|------|---------|
| **NavHost** | 画面を表示する「枠」 | HTML の `<router-view>`（Vue）, `<Outlet>`（React Router） |
| **NavController** | 画面遷移を制御する | `router.push()` |
| **Route** | 画面のアドレス。`"product/{id}"` のような文字列 | URL パス `/product/123` |
| **BackStack** | 画面遷移の履歴スタック | ブラウザの「戻る」履歴 |
| **NavKey** | Navigation 3 で使うルート定義（型安全） | 型付きルート |
| **Deep Link** | 外部から特定の画面を直接開くURL | そのまま |

---

### ボトムナビゲーション / NavigationBar

```
┌────────────────────────────────────┐
│            画面コンテンツ            │
│                                    │
│                                    │
│                                    │
├────────────────────────────────────┤
│ 🏠ホーム │ 📦商品 │ 📋受注 │ ⚙設定 │  ← BottomNavigationBar
└────────────────────────────────────┘
```

- 各タブが独立したバックスタックを持つ（タブA で画面遷移 → タブB → タブA に戻ると、元の画面状態が保持される）

---

### NavigationRail / NavigationDrawer

```
NavigationRail（タブレット縦）:     NavigationDrawer（タブレット横）:
┌──┬───────────────────┐        ┌────────┬───────────────────┐
│🏠│                   │        │ 🏠 ホーム│                   │
│📦│   画面コンテンツ    │        │ 📦 商品  │                   │
│📋│                   │        │ 📋 受注  │   画面コンテンツ    │
│⚙ │                   │        │ ⚙ 設定  │                   │
└──┴───────────────────┘        └────────┴───────────────────┘
```

`NavigationSuiteScaffold` を使うと、画面幅に応じて自動で切り替わる。

---

## 9. ビルドシステム（Gradle）

### Gradle（グレードル）

**一言で**: ビルド自動化ツール。Spring Boot でも使われている（`build.gradle`）ため馴染み深いはず。

**Android 固有の要素:**

| 要素 | 説明 |
|------|------|
| **AGP (Android Gradle Plugin)** | Android アプリをビルドするための Gradle プラグイン |
| **build.gradle.kts** | Kotlin DSL 版の Gradle ファイル（`.kts` = Kotlin Script） |

---

### マルチモジュール

```
project/
  ├── app/              # メインモジュール（最終的な APK を作る）
  ├── core/data/        # ライブラリモジュール（app から参照される）
  ├── core/network/     # ライブラリモジュール
  └── feature/order/    # ライブラリモジュール
```

**Spring Boot 対比**: Maven / Gradle のマルチモジュールプロジェクトと同じ概念。

```groovy
// Spring Boot
dependencies {
    implementation project(':common')
}

// Android (Kotlin DSL)
dependencies {
    implementation(projects.core.data)   // ← TYPESAFE_PROJECT_ACCESSORS
}
```

**なぜマルチモジュール化するか:**
1. **ビルド速度**: 変更のあったモジュールだけ再ビルド（インクリメンタルビルド）
2. **責務の分離**: モジュール間の依存を制限できる（feature が直接 DB を見ることを禁止等）
3. **チーム分担**: モジュール単位で担当を分けられる
4. **テスト容易性**: モジュール単位でテスト実行

---

### Convention Plugin（コンベンションプラグイン）

**一言で**: 複数モジュールに**共通のビルド設定を1箇所で定義**するカスタム Gradle プラグイン。

```
【Convention Plugin なし】
module_A/build.gradle.kts  → android { compileSdk = 35; minSdk = 24; ... } (30行)
module_B/build.gradle.kts  → android { compileSdk = 35; minSdk = 24; ... } (30行)
module_C/build.gradle.kts  → android { compileSdk = 35; minSdk = 24; ... } (30行)
↑ 同じ設定を30モジュールにコピペ...

【Convention Plugin あり】
convention/AndroidLibraryPlugin.kt → 共通設定を1箇所で定義
module_A/build.gradle.kts  → plugins { id("retail.android.library") }  (1行)
module_B/build.gradle.kts  → plugins { id("retail.android.library") }  (1行)
module_C/build.gradle.kts  → plugins { id("retail.android.library") }  (1行)
```

**Spring Boot 対比**: 親 POM（`spring-boot-starter-parent`）に近い。共通設定の継承。

---

### Version Catalog（libs.versions.toml）

**一言で**: 依存ライブラリのバージョンを **1つの TOML ファイルで一元管理** する仕組み。

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.3.0"
compose-bom = "2025.09.01"
hilt = "2.59"
retrofit = "2.11.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**Spring Boot 対比**: `dependencyManagement` / BOM（Bill of Materials）と同じ発想。

---

### Product Flavor（プロダクトフレーバー）

**一言で**: **同じコードベースから異なるバージョンのアプリ** を作る仕組み。

```
                    ┌─ devDebug       (開発環境 + デバッグ)
          ┌─ dev ──┤
          │        └─ devRelease      (開発環境 + リリース)
          │
ソースコード ├─ stg ──┬─ stgDebug       (STG環境 + デバッグ)
          │        └─ stgRelease      (STG環境 + リリース)
          │
          └─ prod ─┬─ prodDebug      (本番環境 + デバッグ)
                   └─ prodRelease    (本番環境 + リリース)
```

各フレーバーで異なる値を設定可能:
- API の接続先 URL
- アプリの ApplicationId（別アプリとしてインストール可能）
- アイコン・アプリ名
- Firebase 設定ファイル

**Spring Boot 対比**: `application-dev.yml` / `application-stg.yml` / `application-prod.yml` のプロファイル切り替えと同じ。

---

### Build Type（ビルドタイプ）

| ビルドタイプ | 用途 | 設定例 |
|------------|------|--------|
| `debug` | 開発中 | デバッグ可能、難読化なし、デバッグ署名 |
| `release` | 本番配信 | 難読化あり（R8）、リリース署名、最適化 |

---

### Gradle Managed Device（GMD）

**一言で**: CI 環境で **エミュレーターを自動作成・管理** してテストを実行する仕組み。物理デバイスやクラウドデバイスが不要。

```kotlin
// Gradle で定義
managedDevices {
    localDevices {
        create("pixel6api33") {
            device = "Pixel 6"
            apiLevel = 33
            systemImageSource = "aosp"
        }
    }
}
```

---

### TYPESAFE_PROJECT_ACCESSORS

```kotlin
// 有効化前
implementation(project(":core:data"))        // 文字列 → タイポしてもビルドまで気づかない

// 有効化後
implementation(projects.core.data)            // 型安全 → 存在しないモジュールはコンパイルエラー
```

---

## 10. テスト関連

### テスト種別の全体像

```
  ┌─────────────────────────────────────────────┐
  │ ローカルテスト（JVM上で実行。高速）              │
  │  ├── ユニットテスト（ViewModel, Repository等）  │
  │  ├── スクリーンショットテスト（Roborazzi）       │
  │  └── Robolectric テスト（JVM上でAndroid模倣）   │
  ├─────────────────────────────────────────────┤
  │ 計装テスト（実機/エミュレーター上で実行。遅い）    │
  │  ├── UI テスト（Compose TestRule）              │
  │  └── E2E テスト（Espresso）                    │
  └─────────────────────────────────────────────┘
```

**Spring Boot 対比:**

| Android テスト | Spring Boot テスト | 実行場所 |
|---------------|-------------------|---------|
| ローカルユニットテスト | `@Test` (JUnit) | JVM |
| Robolectric テスト | `@SpringBootTest` (組み込みサーバー) | JVM |
| 計装テスト | 結合テスト (TestRestTemplate) | 実機/エミュ |
| スクリーンショットテスト | （該当なし） | JVM |

---

### Turbine（タービン）

**一言で**: Kotlin Flow の **テストヘルパーライブラリ**。Flow から流れる値を順番に検証できる。

```kotlin
@Test
fun `検索結果が正しく流れること`() = runTest {
    val viewModel = ProductViewModel(fakeRepository)

    viewModel.uiState.test {   // ← Turbine の test 拡張
        assertEquals(Loading, awaitItem())      // 最初は Loading
        viewModel.search("chair")
        assertEquals(Success(chairs), awaitItem())  // 検索結果が来る
    }
}
```

---

### Truth（トゥルース）

**一言で**: Google の **アサーションライブラリ**。JUnit の `assertEquals` より読みやすい。

```kotlin
// JUnit
assertEquals(3, list.size());
assertTrue(list.contains("item"));

// Truth
assertThat(list).hasSize(3)
assertThat(list).contains("item")
```

---

### Roborazzi（ロボラッツィ）

**一言で**: **スクリーンショット比較テスト**。画面のスクリーンショットを撮影し、以前のスクリーンショットと画像比較して変更を検知。

```
テスト実行: UI のスクリーンショットを撮影

前回の画像:              今回の画像:               差分:
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ 商品一覧      │     │ 商品一覧      │     │              │
│ ┌──┐ 椅子    │     │ ┌──┐ 椅子    │     │    ●ここが    │
│ │  │ ¥1,000  │  vs │ │  │ ¥1,200  │  →  │    ●変わった   │
│ └──┘         │     │ └──┘         │     │              │
└──────────────┘     └──────────────┘     └──────────────┘

→ 意図した変更なら画像を更新。意図しない変更ならバグ発見。
```

---

### Robolectric（ロボレクトリック）

**一言で**: **JVM 上で Android フレームワークをエミュレート** するテストライブラリ。実機不要で Android テストが実行できる。

- 通常、Android 固有の API（`Context`, `Resources` 等）はエミュレーターが必要
- Robolectric があれば JVM 上で高速に実行可能
- Spring Boot の `@SpringBootTest` がサーバー起動なしでテストするのと似た発想

---

### MockK（モック・ケー）

**一言で**: Kotlin 用の **モックライブラリ**。Java の Mockito の Kotlin 版。

```kotlin
val mockApi = mockk<ProductApi>()
coEvery { mockApi.getProducts(any()) } returns ApiResponse(listOf(testProduct))
// coEvery: Coroutine 対応の every（suspend 関数をモック可能）
```

---

### ComposeTestRule

**一言で**: Compose UI の **テスト用ルール**。画面を起動して、要素の存在確認・クリック操作等を自動化。

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun `商品名が表示されること`() {
    composeTestRule.setContent {
        ProductCard(product = testProduct)
    }
    composeTestRule.onNodeWithText("テスト商品").assertIsDisplayed()
}
```

---

### Espresso（エスプレッソ）

**一言で**: Android 公式の **UI テストフレームワーク**。実機/エミュレーターで UI 操作を自動化。Selenium の Android 版。

---

### Baseline Profile（ベースラインプロファイル）

**一言で**: アプリ起動時に **よく使うコードパスを事前コンパイル** して起動速度を向上させる仕組み。

- 通常の Android アプリは JIT（Just-In-Time）コンパイル — 実行時に逐次コンパイル
- Baseline Profile があると AOT（Ahead-Of-Time）コンパイル — 事前にネイティブコードに変換
- 起動速度が 30-50% 向上する場合もある
- **後期最適化**: 初期開発では不要。GA 前のパフォーマンスチューニングで導入

---

## 11. CI/CD・品質管理

### Spotless（スポットレス）

**一言で**: コード **フォーマッター**。Google Java Format / ktfmt の Gradle 統合。

```bash
# フォーマット適用
./gradlew spotlessApply

# フォーマットチェック（CI で使用）
./gradlew spotlessCheck
```

Spring Boot 対比: Checkstyle + Formatter の組み合わせ。

---

### Jacoco（ジャココ）

**一言で**: **テストカバレッジ計測**ツール。Spring Boot でも使用されるため馴染み深いはず。

---

### Lint（リント）

**一言で**: ソースコードの **静的解析**ツール。潜在的なバグ、パフォーマンス問題、アクセシビリティ問題を検出。

- Android 固有の Lint ルール（メモリリーク検知、API レベルチェック等）
- カスタム Lint ルール: プロジェクト固有のコーディング規約を自動チェック
- Spring Boot 対比: SonarQube / SpotBugs

---

### Dependency Guard

**一言で**: ライブラリ依存の **ロックファイル**。意図しない依存追加を防ぐ。

```
prodReleaseRuntimeClasspath.txt:
  com.google.dagger:hilt-android:2.59
  com.squareup.retrofit2:retrofit:2.11.0
  ...

→ 新しいライブラリを追加すると差分が出る → レビューで検知
```

Spring Boot 対比: npm の `package-lock.json` / Gradle の dependency locking。

---

### Firebase App Distribution

**一言で**: テストビルドを **テスターに配信** するサービス。Google Play Store を通さずに APK/AAB を直接配布。

- Spring Boot 対比: テストサーバーへのデプロイ
- テスターのメールアドレスを登録 → ビルドをアップロード → テスターにメール通知

---

## 12. 設計パターン・原則

### シングルアクティビティパターン

**一言で**: アプリ全体で Activity を1つだけ使い、画面遷移は Navigation Compose で管理するパターン。

```
【旧パターン（非推奨）】              【新パターン（推奨）】
LoginActivity                      MainActivity
  ↓                                  └── Compose Navigation
ProductListActivity                       ├── LoginScreen
  ↓                                       ├── ProductListScreen
ProductDetailActivity                     ├── ProductDetailScreen
                                          └── SettingsScreen
```

**メリット:**
- Activity 間のデータ受け渡しが不要（Intent のバンドルサイズ制限を回避）
- 画面遷移アニメーションが滑らか
- 状態管理が容易（アプリ全体の状態を1箇所で管理可能）

---

### Feature API / Impl 分離パターン

```
feature:order-list:api   ← 「この画面はこのルートで遷移できます」という宣言のみ（10行程度）
feature:order-list:impl  ← 画面の実装全体（画面、ViewModel、テスト、数百〜数千行）

feature:product-detail:impl が order-list に遷移したい場合:
  → feature:order-list:api のみに依存（10行を参照するだけ）
  → feature:order-list:impl には依存しない（数千行を参照しない）
```

**なぜこの分離が必要か:**

```
【分離なし】
product-detail:impl → order-list:impl (数千行) に依存
→ order-list:impl を変更するたびに product-detail:impl も再ビルド

【分離あり】
product-detail:impl → order-list:api (10行) にだけ依存
→ order-list:impl を変更しても product-detail:impl は再ビルド不要（api が変わらない限り）
```

30モジュールあると、この差が **ビルド時間に数分〜数十分** の差を生む。

---

### Mapper パターン（DTO → Model 変換）

```kotlin
// API レスポンス DTO（バックエンドの形式に依存）
@Serializable
data class ProductDto(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("stock_status_code") val stockStatusCode: Int,
)

// アプリ内モデル（UI が使いやすい形式）
data class Product(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val currency: Currency,
    val stockStatus: StockStatus,  // enum に変換済み
)

// Mapper（変換ロジック）
class ProductMapper {
    fun toModel(dto: ProductDto): Product = Product(
        id = dto.productId,
        name = dto.productName,
        price = BigDecimal(dto.unitPrice.toString()),
        currency = Currency.fromCode(dto.currencyCode),
        stockStatus = StockStatus.fromCode(dto.stockStatusCode),
    )
}
```

**NiA の UseCase が担っていた役割をこの Mapper が代替する。**

---

### SharingStarted.WhileSubscribed(5_000)

NiA のコードに頻出するこの設定の意味:

```kotlin
val uiState: StateFlow<UiState> = repository.getData()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),  // ← これ
        initialValue = UiState.Loading,
    )
```

- **WhileSubscribed**: 最後の購読者が離れてから **5秒待って** データ収集を停止する
- **なぜ5秒**: 画面回転時、UI は一度 unsubscribe → resubscribe する。即停止すると回転のたびにデータ再取得してしまう。5秒の猶予で回転を吸収
- **メモリ・バッテリー節約**: 画面が完全にバックグラウンドに行ったら（5秒経過）無駄なデータ取得を止める

---

## 13. バックエンド連携用語

### REST API 設計（モバイル視点）

バックエンド（Spring Boot）が提供する API をモバイルが消費する関係:

```
Backend (Spring Boot)              Mobile (Android)
━━━━━━━━━━━━━━━━━━━              ━━━━━━━━━━━━━━━━
@RestController                    Retrofit Interface
                                   
GET /api/v1/products          →    @GET("api/v1/products")
POST /api/v1/orders           →    @POST("api/v1/orders")
GET /api/v1/orders/{id}       →    @GET("api/v1/orders/{id}")
```

**モバイル特有の API 設計考慮事項:**

| 項目 | 理由 |
|------|------|
| **ページネーション** | モバイルは一度に大量データを扱えない（メモリ制約） |
| **レスポンスサイズ最小化** | 通信量 = バッテリー消費・通信料 |
| **BFF (Backend for Frontend)** | モバイル画面に最適化された API を用意する層 |
| **バージョニング** | アプリは即時更新されない → 古い API を一定期間維持する必要がある |
| **オフライン考慮** | ネットワーク断時のエラーハンドリング |

---

### OAuth 2.0 + PKCE

**一言で**: モバイルアプリ向けの **認証フロー**。

```
1. ユーザーがログイン画面を開く
2. モバイルアプリが code_verifier（ランダム文字列）を生成
3. code_challenge = SHA256(code_verifier) を計算
4. 認証サーバーに code_challenge を送ってログインページを表示
5. ユーザーが ID/パスワード入力
6. 認証サーバーが authorization_code を返す
7. モバイルアプリが authorization_code + code_verifier を送信
8. 認証サーバーが code_verifier を検証し、access_token を返す
```

**なぜ PKCE（Proof Key for Code Exchange）か:**
- モバイルアプリは `client_secret` を安全に保持できない（APK を解析されるリスク）
- PKCE は `client_secret` 不要で安全に認証できる

---

### JWT（JSON Web Token）

**一言で**: 認証トークンの標準形式。Base64 エンコードされた JSON。

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.    (ヘッダー)
eyJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6ImFkbWluIn0. (ペイロード: ユーザーID, ロール等)
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c   (署名)
```

**モバイルでの管理:**
- `access_token`: API 通信のたびに HTTP ヘッダーに付与（`Authorization: Bearer <token>`）
- `refresh_token`: access_token の有効期限が切れたとき、新しい access_token を取得するために使用
- 保存場所: DataStore（暗号化推奨）

---

### API レスポンスラッパー

バックエンド API のレスポンスを統一的に扱うための共通構造:

```kotlin
// 共通成功レスポンス
@Serializable
data class ApiResponse<T>(
    val status: String,      // "success" or "error"
    val data: T?,            // レスポンスデータ
    val message: String?,    // エラーメッセージ
    val errors: List<ApiError>?,  // バリデーションエラー詳細
)

// ページネーション付きレスポンス
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Long,
)
```

**このレスポンス構造はバックエンドと事前合意が必要。**

---

## 付録: 略語一覧

| 略語 | 正式名称 | 意味 |
|------|---------|------|
| AGP | Android Gradle Plugin | Android ビルドプラグイン |
| APK | Android Package | アプリインストールファイル |
| AAB | Android App Bundle | Play Store 配信形式 |
| AOT | Ahead-Of-Time | 事前コンパイル |
| BFF | Backend for Frontend | フロント向け API 層 |
| BOM | Bill of Materials | 依存バージョン一括管理 |
| CI/CD | Continuous Integration / Continuous Delivery | 継続的インテグレーション/デリバリー |
| CQRS | Command Query Responsibility Segregation | コマンドクエリ責務分離 |
| DAO | Data Access Object | データアクセスオブジェクト |
| DDD | Domain-Driven Design | ドメイン駆動設計 |
| DI | Dependency Injection | 依存性注入 |
| DTO | Data Transfer Object | データ転送オブジェクト |
| FCM | Firebase Cloud Messaging | プッシュ通知サービス |
| FTS | Full-Text Search | 全文検索 |
| GMD | Gradle Managed Device | Gradle デバイス管理 |
| JIT | Just-In-Time | 実行時コンパイル |
| JWT | JSON Web Token | 認証トークン形式 |
| KSP | Kotlin Symbol Processing | Kotlin コンパイル時コード生成 |
| MVVM | Model-View-ViewModel | UI アーキテクチャパターン |
| OIDC | OpenID Connect | OAuth2 ベースの認証プロトコル |
| ORM | Object-Relational Mapping | オブジェクト関係マッピング |
| PKCE | Proof Key for Code Exchange | モバイル向け OAuth2 拡張 |
| R8 | （特になし。ProGuard の後継） | コード難読化・最小化ツール |
| SDK | Software Development Kit | ソフトウェア開発キット |
| SSO | Single Sign-On | シングルサインオン |
| UDF | Unidirectional Data Flow | 単方向データフロー |
