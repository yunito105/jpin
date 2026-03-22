# 推奨技術スタック詳細ガイド

**プロジェクト**: 海外販売システム（25億円規模）  
**業種**: 小売業（IKEA・ニトリ・カインズ類似業態）  
**対象プラットフォーム**: Android  
**作成目的**: 推奨技術スタック各要素の詳細説明

---

## 📋 技術スタック一覧（早見表）

| カテゴリ | 採用技術 | 選定理由（一言） |
|---|---|---|
| 言語 | Kotlin 2.x | Google公式の Android 開発推奨言語 |
| UI | Jetpack Compose | Google公式推奨。宣言的UIで動的な商品表示に最適 |
| DI | Hilt | Google公式推奨。コンパイル時検出で大規模に適する |
| 非同期 | Coroutines + Flow | Google公式推奨。在庫リアルタイム更新に必須 |
| API通信 | Retrofit2 + OkHttp3 | 業界標準。CQRS（POST/GET）を直接表現可能 |
| ローカルDB | Room + DataStore | Google公式推奨。オフラインキャッシュに必須 |
| 画像読込 | Coil | Compose対応。大量商品画像表示に最適 |
| プッシュ通知 | FCM | Android唯一のOS統合プッシュ。セール通知に必須 |
| バーコード | ML Kit | Google公式。無料+オフライン+高精度 |
| カメラ | CameraX | Google公式。ML Kit との連携が容易 |
| バックグラウンド | WorkManager | Google公式推奨。オフライン操作の同期に必須 |
| テスト | JUnit5 + MockK + Turbine | Kotlin/Flow対応。CQRS分離テストに最適 |
| CI/CD | GitHub Actions + FAD | 海外テスターへの配信容易。無料枠あり |

---

## 1. 言語：Kotlin 2.x

### 🔤 これは何？
**Kotlin（コトリン）** は、JetBrains社が開発したプログラミング言語。Javaと100%互換性があり、Java仮想マシン（JVM）上で動作する。2017年にGoogleがAndroid開発の公式言語として採用し、2019年には「Kotlin First」を宣言した。**2.x** は2024年にリリースされたメジャーバージョンで、K2コンパイラによる高速ビルドが特徴。

### 📖 技術背景
- Javaの冗長な記述を改善する目的でJetBrains社が2011年に発表
- 2017年 Google I/O で Android 公式言語に採用
- 2019年「Kotlin First」宣言 → 新しいAndroid APIはKotlin優先で設計される
- 2024年 Kotlin 2.0 リリース → 新コンパイラ「K2」で **ビルド速度が最大2倍** に改善
- Android開発者の **95%以上** がKotlinを使用（Google公式統計）

### ✅ メリット
| メリット | 説明 |
|---|---|
| **Null安全** | 型システムレベルでNullPointerExceptionを防止。`String?`（nullable）と`String`（non-null）を区別する |
| **簡潔な記法** | Javaの約 **40%少ないコード量** で同等の処理を記述可能（data class, scope function 等） |
| **Coroutines対応** | 言語レベルで非同期処理をサポート。コールバック地獄を回避 |
| **Java互換** | 既存のJavaライブラリ・コードをそのまま利用可能。段階的な移行が容易 |
| **Google公式サポート** | Jetpack・Compose等すべてのモダンAndroid APIがKotlin前提で設計されている |
| **K2コンパイラ（2.x）** | ビルド速度の大幅改善。大規模プロジェクトでの開発体験が向上 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **学習コスト** | Java経験者でも拡張関数・スコープ関数・Coroutines等の独自概念を習得する必要がある（目安：2〜4週間） |
| **コンパイル速度（従来）** | Kotlin 1.x系ではJavaより遅かったが、K2コンパイラ（2.x）で大幅改善済み |
| **リフレクション性能** | Kotlinのリフレクション（`kotlin-reflect`）はJavaのそれより重い。ただしAndroidでは通常使わない |
| **バイナリサイズ増加** | Kotlin標準ライブラリの分（約1.5MB）がAPKサイズに加算される |

---

## 2. UI：Jetpack Compose

### 🔤 これは何？
**Jetpack Compose（ジェットパック コンポーズ）** は、Googleが開発したAndroid向け **宣言的UIフレームワーク**。従来のXMLレイアウトに代わり、**Kotlinコードだけ** でUIを構築する。「UIの状態が変わったら、自動的に画面が再描画される」という考え方が基本。

### 📖 技術背景
- 従来のAndroid UIはXMLで画面を定義 → Java/KotlinでfindViewByIdして操作、という手順だった
- React（Web）やSwiftUI（iOS）の「宣言的UI」が主流になり、Androidも追従
- 2021年に正式リリース（1.0）
- 2023年以降、Google公式サンプルはほぼすべてCompose前提で書かれている
- XMLベースのView SystemはGoogleが「メンテナンスモード（新機能追加なし）」と明言

### ✅ メリット
| メリット | 説明 |
|---|---|
| **コード量削減** | XMLファイル + Binding + Adapter 等が不要。UIとロジックが1ファイルで完結 |
| **リアルタイムプレビュー** | `@Preview`アノテーションでAndroid Studio上でUIを即時確認可能 |
| **状態管理が容易** | `State`が変わると自動で再描画（Recomposition）。手動でViewを更新する必要がない |
| **商品一覧との相性◎** | `LazyColumn` / `LazyGrid` で大量商品の効率的なリスト表示が可能 |
| **テスト容易性** | UIテストが`ComposeTestRule`で簡潔に書ける |
| **Material Design 3** | Google最新のデザインシステムにネイティブ対応 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **学習コスト** | XML経験者にとってパラダイムシフト。「状態 → UI」の考え方に慣れるまで時間がかかる |
| **Recomposition問題** | 状態管理を誤ると不要な再描画が多発しパフォーマンス低下。`remember` / `derivedStateOf`の理解が必須 |
| **エコシステムの成熟度** | 一部のサードパーティライブラリはまだXML前提。ただし主要ライブラリは対応済み |
| **デバッグの難しさ** | Recompositionのタイミングが分かりにくく、Layout Inspectorの活用が必要 |

---

## 3. DI：Hilt

### 🔤 これは何？
**Hilt（ヒルト）** は、Googleが開発したAndroid専用の **DI（Dependency Injection：依存性注入）フレームワーク**。

**DI（依存性注入）とは？**  
クラスが自分で依存オブジェクトを生成するのではなく、**外部から渡してもらう** 設計パターン。例えば `ProductViewModel` が `ProductRepository` を使う場合、自分で `new` するのではなく、Hiltが自動的に渡してくれる。

### 📖 技術背景
- DI自体はソフトウェア設計の基本原則（SOLID原則のD = Dependency Inversion）
- Androidでは長年 **Dagger2** が標準的なDIライブラリだったが、設定が非常に複雑だった
- Hiltは **Dagger2のラッパー** として2020年に登場。Dagger2の性能はそのままに、Android向けの設定を大幅簡略化
- `@HiltViewModel`, `@Inject`, `@Module` 等のアノテーションを付けるだけで自動的に依存関係が解決される

### ✅ メリット
| メリット | 説明 |
|---|---|
| **コンパイル時エラー検出** | 依存関係の不備を **ビルド時に** 検出。実行時クラッシュを防止（Koinはランタイム検出） |
| **Android統合** | Activity・Fragment・ViewModel・WorkManagerとの連携がアノテーション1つで完結 |
| **スコープ管理** | `@Singleton` / `@ViewModelScoped` 等でインスタンスのライフサイクルを自動管理 |
| **テスト支援** | `@TestInstallIn`でテスト時だけモック実装に差し替え可能 |
| **Google公式推奨** | Jetpackの一部として公式ドキュメント・サンプルが充実 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **ビルド時間増加** | コード生成（kapt/KSP）によりビルド時間が増加。大規模プロジェクトでは顕著 |
| **学習コスト** | Dagger2の概念（Component, Module, Scope）の基礎理解が必要 |
| **デバッグ困難** | 生成コードが複雑。依存関係のエラーメッセージが分かりにくい場合がある |
| **ボイラープレート** | Koin等に比べるとアノテーション・Module定義の記述量がやや多い |

---

## 4. 非同期：Coroutines + Flow

### 4-1. Coroutines（コルーチン）

#### 🔤 これは何？
**Coroutines（コルーチン）** は、Kotlinに組み込まれた **軽量な非同期処理の仕組み**。API通信やDB操作などの「待ち」が発生する処理を、メインスレッド（UIスレッド）をブロックせずに実行する。

#### 📖 技術背景
- Android では、メインスレッドで重い処理を行うと画面がフリーズ（ANR: Application Not Responding）
- 従来は `AsyncTask`, `RxJava`, `Callback` 等で対応していたが、コードが複雑化（コールバック地獄）
- Kotlin 1.3（2018年）でCoroutinesが正式リリース
- `suspend` キーワードを関数に付けるだけで、非同期処理を **同期的な見た目** で記述可能
- `viewModelScope` / `lifecycleScope` でAndroidのライフサイクルに合わせた自動キャンセルが可能

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **直感的な記法** | `suspend fun` で非同期処理を同期風に記述。コールバック地獄を完全回避 |
| **軽量** | 1つのスレッドで数万のCoroutineを起動可能。スレッド生成コストが不要 |
| **自動キャンセル** | `viewModelScope`で起動すればViewModel破棄時に自動キャンセル。メモリリーク防止 |
| **構造化並行性** | 親子関係でCoroutineを管理。親がキャンセルされれば子も自動キャンセル |
| **例外ハンドリング** | try-catchで通常の同期コードと同じように例外処理可能 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **デバッグ難易度** | スタックトレースが通常と異なり、問題箇所の特定が難しい場合がある |
| **学習コスト** | `CoroutineScope`, `Dispatcher`, `Job`等の概念を理解する必要がある |
| **暗黙のスレッド切替** | `withContext(Dispatchers.IO)` の使い忘れでメインスレッドをブロックするリスク |

---

### 4-2. Flow（フロー）

#### 🔤 これは何？
**Flow（フロー）** は、Kotlin Coroutinesの一部で、**時間の経過とともに複数の値を順次発行するデータストリーム**。「1回だけ値を返す」Coroutinesの `suspend fun` に対し、Flowは「値が変わるたびに通知する」リアクティブな仕組み。

#### 📖 技術背景
- 従来、リアクティブストリームはRxJava（`Observable`, `Flowable`）で実現していた
- RxJavaは強力だが学習コストが非常に高い（Operator が200以上）
- Kotlin FlowはCoroutinesの上に構築され、**シンプルなAPIでRxJavaの主要機能をカバー**
- `StateFlow`（最新値を常に保持）と `SharedFlow`（イベント発行）の2種類が基本
- Jetpack ComposeはFlowとの統合が深く、`collectAsState()` で自動的にUIが更新される

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **在庫リアルタイム更新** | 在庫数の変化をFlowで流し、UIに即時反映。本プロジェクトの核心機能 |
| **Compose統合** | `collectAsState()` でFlowの値を直接Composableに変換。ボイラープレートなし |
| **Cold Stream** | 購読者がいる時だけ処理実行。不要な通信・計算を自動回避 |
| **演算子が直感的** | `map`, `filter`, `combine` 等、Kotlinのコレクション操作と同じ感覚 |
| **バックプレッシャー対応** | 大量データを安全に処理。消費者の処理速度に合わせて自動調整 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Hot/Coldの理解** | `StateFlow`(Hot)と`flow{}`(Cold)の違いを理解しないとバグの原因になる |
| **ライフサイクル管理** | `repeatOnLifecycle`を適切に使わないと、バックグラウンドで無駄にデータ収集が続く |
| **デバッグ難易度** | ストリーム内のデータ変換チェーンが長いと、どこで問題が起きたか追跡が難しい |

---

## 5. API通信：Retrofit2 + OkHttp3

### 5-1. Retrofit2（レトロフィット）

#### 🔤 これは何？
**Retrofit2（レトロフィット）** は、Square社が開発した **Android/Java向けのHTTP APIクライアントライブラリ**。RESTful APIのエンドポイントをKotlin/Javaのインターフェースとして定義し、**型安全にAPI通信** を行える。

#### 📖 技術背景
- AndroidでHTTP通信を行う場合、標準の`HttpURLConnection`は非常に低レベルで記述量が多い
- Square社が2013年にRetrofit（v1）をリリース。2016年にRetrofit2にメジャーアップデート
- インターフェースにアノテーション（`@GET`, `@POST`等）を付けるだけでAPI定義が完了
- Kotlin Coroutinesの`suspend fun`に対応しており、非同期API通信がシンプルに書ける
- **業界標準**: Androidアプリの大多数がRetrofitを使用

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **型安全** | APIレスポンスを`data class`に自動変換。JSON手動パースが不要 |
| **宣言的API定義** | `@GET("products/{id}")` のようにアノテーションでエンドポイントを定義。コードの見通しが良い |
| **CQRS対応** | `@GET`（Query）と `@POST`（Command）でCQRS パターンをそのまま表現 |
| **Coroutines対応** | `suspend fun`を使い、`viewModelScope`内でシンプルにAPI呼び出し |
| **カスタマイズ性** | Converter（Gson/Moshi/Kotlinx Serialization）やAdapterを差し替え可能 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **学習コスト** | アノテーション・Converter・Interceptor等の概念理解が必要 |
| **エラーハンドリング** | HTTPエラー（4xx/5xx）の処理を自前で実装する必要がある（`Response<T>`のハンドリング） |
| **リアルタイム通信非対応** | REST専用。WebSocketやSSE（Server-Sent Events）は別途OkHttpを直接使う必要がある |

---

### 5-2. OkHttp3（オーケーエイチティーティーピー）

#### 🔤 これは何？
**OkHttp3** は、Square社が開発した **HTTPクライアントライブラリ**。Retrofit2の **内部通信エンジン** として動作する。Retrofit2が「何を通信するか」を定義し、OkHttp3が「どう通信するか」を担当する。

#### 📖 技術背景
- Androidの標準HTTPクライアントは性能・機能面で不十分だった
- OkHttpはHTTP/2、接続プーリング、透過的なGZIP圧縮、レスポンスキャッシュを自動で処理
- **Interceptor（インターセプター）** 機能で、リクエスト/レスポンスを加工・ログ出力できる
- Retrofit2を導入すると自動的にOkHttp3も含まれるが、直接設定することでカスタマイズ可能

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **Interceptor** | 認証トークンの自動付加、リクエストログ出力、エラーハンドリング等を一括で設定可能 |
| **接続管理** | HTTP/2 + 接続プーリングで効率的な通信。商品一覧の大量API通信でも高速 |
| **リトライ・リダイレクト** | 通信失敗時の自動リトライ・リダイレクト追従を標準搭載 |
| **キャッシュ機能** | HTTPキャッシュヘッダに基づく透過的キャッシュ。オフライン時のフォールバックに活用 |
| **デバッグ容易** | `HttpLoggingInterceptor`で全リクエスト/レスポンスをログ出力。開発効率向上 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **設定の複雑さ** | タイムアウト・Interceptor・証明書ピンニング等、細かい設定が多く初心者には難しい |
| **バイナリサイズ** | ライブラリ自体が約2MB。APKサイズに影響 |
| **バージョン管理** | Retrofit2と互換性のあるOkHttpバージョンを合わせる必要がある |

### 5-3. Retrofit2 + OkHttp3 の連携

```
アプリコード → Retrofit2（API定義・型変換） → OkHttp3（実際のHTTP通信）→ バックエンドAPI
```

| 役割 | Retrofit2 | OkHttp3 |
|---|---|---|
| 位置づけ | 上位レイヤー（API定義） | 下位レイヤー（通信実行） |
| 担当 | エンドポイント定義・JSON変換 | HTTP通信・Interceptor・キャッシュ |
| 例えるなら | 「注文伝票」 | 「配達員」 |

---

## 6. ローカルDB：Room + DataStore

### 6-1. Room（ルーム）

#### 🔤 これは何？
**Room（ルーム）** は、Googleが開発した **AndroidのSQLiteデータベース抽象化ライブラリ**。SQLiteを直接操作するのではなく、Kotlinのアノテーションとデータクラスを使い、**型安全でコンパイル時検証付き** のデータベースアクセスを提供する。

#### 📖 技術背景
- AndroidにはSQLiteが組み込まれているが、直接操作はSQL文字列の手書きでエラーが発生しやすい
- Roomは2017年にJetpackの一部としてリリース
- `@Entity`（テーブル定義）, `@Dao`（クエリ定義）, `@Database`（DB定義）の3アノテーションが基本
- **Flowとの統合**: クエリ結果を`Flow<List<T>>`で返すことで、DBの変更をリアルタイムにUIに反映
- 本プロジェクトでは **オフラインキャッシュ**（商品一覧・カート情報の保持）に使用

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **コンパイル時SQLチェック** | SQL文をビルド時に検証。実行時のSQLエラーを防止 |
| **Flow対応** | `Flow<List<Product>>` でDBの変更を自動通知。オフライン→オンライン復帰時の自動更新に最適 |
| **マイグレーション** | バージョンアップ時のDBスキーマ変更を安全に管理 |
| **Coroutines対応** | `suspend fun`でクエリを非同期実行。メインスレッドブロックを防止 |
| **テスト容易** | `inMemoryDatabaseBuilder()`でテスト用DBを即座に作成可能 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **学習コスト** | SQLの基礎知識 + Room固有のアノテーション・リレーション定義の理解が必要 |
| **マイグレーション管理** | スキーマ変更時のマイグレーション実装が煩雑。テストも必須 |
| **複雑なクエリ** | JOIN・サブクエリ等はRoomのアノテーションだけでは表現しにくい場合がある |

---

### 6-2. DataStore（データストア）

#### 🔤 これは何？
**DataStore（データストア）** は、Googleが開発した **軽量なキーバリューストア**。従来の`SharedPreferences`の後継で、**Coroutines + Flow ベース** で安全にデータを読み書きする。設定値やユーザー情報など、小さなデータの保存に使う。

#### 📖 技術背景
- `SharedPreferences`はAndroid初期から存在するが、**メインスレッドブロック**・**型安全でない**・**例外が発生しない（無言で失敗）** 等の問題があった
- DataStoreは2020年にリリース。2種類ある：
  - **Preferences DataStore**: キーバリュー形式（`SharedPreferences`の代替）
  - **Proto DataStore**: Protocol Buffersで型安全にデータを定義
- 本プロジェクトでは **ユーザー設定**（言語・通貨・通知設定）、**認証トークンの保持** に使用

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **非同期安全** | Coroutines + Flowベースで、メインスレッドブロックなし |
| **型安全（Proto）** | Proto DataStoreならProtocol Buffersで型を定義。誤った型の読み書きを防止 |
| **エラーハンドリング** | Flowの例外処理機構でIOエラーを安全にキャッチ |
| **SharedPreferences移行** | `SharedPreferencesMigration`で既存データからの移行が容易 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **小規模データ専用** | 大量データや構造化データにはRoom を使うべき。DataStoreは設定値向け |
| **Proto DataStoreの学習コスト** | Protocol Buffersの`.proto`ファイル定義が必要で、初心者には敷居が高い |
| **同期読み取り不可** | 起動直後に値がすぐ必要な場面では`runBlocking`を使う必要があり、注意が必要 |

### 6-3. Room + DataStore の使い分け

| 用途 | 使うもの | 理由 |
|---|---|---|
| 商品一覧キャッシュ | **Room** | 構造化データ・大量レコード・リレーションあり |
| カート情報 | **Room** | 複数商品・数量・小計などの構造化データ |
| ユーザー設定（言語・通貨） | **DataStore** | キーバリュー形式の少量データ |
| 認証トークン | **DataStore** | 単一値の保存 |
| 最後に見た商品ID | **DataStore** | 単一値の保存 |

---

## 7. 画像読込：Coil

### 🔤 これは何？
**Coil（コイル）** は、Instacart社が開発した **Kotlin製の画像読み込みライブラリ**。名前は「**Co**routine **I**mage **L**oader」の略。ネットワーク/ローカルの画像を非同期で取得し、メモリ/ディスクにキャッシュした上で画面に表示する。

### 📖 技術背景
- 従来はGlide（Google）やPicasso（Square）が定番だったが、いずれもJava製
- Coilは **Kotlin/Coroutinesネイティブ** で設計され、Jetpack Composeとの統合が深い
- `AsyncImage` Composableで1行で画像表示が可能
- メモリキャッシュ → ディスクキャッシュ → ネットワーク の3段階で自動的に最適な取得元を選択
- 本プロジェクトでは **商品画像の大量表示**（一覧画面・詳細画面）に使用

### ✅ メリット
| メリット | 説明 |
|---|---|
| **Compose対応** | `AsyncImage`で宣言的に画像表示。placeholder・error画像も簡単に設定 |
| **Kotlin First** | Coroutinesベースで軽量。Kotlin拡張関数による直感的なAPI |
| **自動キャッシュ** | メモリ + ディスクの2層キャッシュ。商品一覧のスクロール時も高速表示 |
| **軽量** | APKサイズ増加が約 **~1,500 メソッド**（Glideの約1/3） |
| **変換機能** | 角丸・円形・ぼかし等の画像変換を内蔵 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Glideと比べて歴史が浅い** | Glideは10年以上の実績がありエッジケースの対応が豊富。Coilは比較的新しい |
| **GIF/動画サポート** | GIF対応は追加ライブラリが必要。動画サムネイルは別途対応 |
| **iOS非対応** | KMM（Kotlin Multiplatform Mobile）で共有する場合はCoilは使えない（Android専用） |

---

## 8. プッシュ通知：FCM

### 🔤 これは何？
**FCM（Firebase Cloud Messaging）** は、Googleが提供する **無料のクロスプラットフォーム・プッシュ通知サービス**。バックエンドサーバーからAndroid/iOS/Webのデバイスに対して、リアルタイムにメッセージ（通知）を送信できる。

### 📖 技術背景
- 前身は **GCM（Google Cloud Messaging）**。2016年にFCMに移行
- Androidでプッシュ通知を送る **唯一のOS統合手段**（iOS の APNs に相当）
- **2種類のメッセージ**:
  - **通知メッセージ**: FCMが自動的に通知バーに表示。サーバー側で完結
  - **データメッセージ**: アプリ側でハンドリング。カスタム処理が可能
- **トピック配信**: 特定のトピック（例: `sale_japan`, `new_arrival`）を購読したユーザーにだけ配信
- 本プロジェクトでは **セール通知・在庫復活通知・注文ステータス更新** に使用

### ✅ メリット
| メリット | 説明 |
|---|---|
| **完全無料** | 送信数・デバイス数に制限なし |
| **OS統合** | Androidのプッシュ通知インフラを直接利用。バッテリー効率が最も良い |
| **トピック配信** | ユーザーセグメント（国・興味カテゴリ）ごとの配信が容易 |
| **Firebase統合** | Analytics・A/Bテスト・Remote Configと連携し、通知の効果測定が可能 |
| **バックグラウンド対応** | アプリが閉じていても通知を受信可能 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Firebase依存** | Googleのインフラに依存。Firebase障害時は通知不能 |
| **中国市場非対応** | 中国ではGoogle Playサービスが利用不可。別途対応（Huawei Push Kit等）が必要 |
| **配信保証なし** | ベストエフォート型。100%の配信保証はない（デバイスがオフライン等） |
| **デバッグ困難** | 通知が届かない原因の切り分け（トークン・サーバー・端末設定）が難しい |

---

## 9. バーコード：ML Kit

### 🔤 これは何？
**ML Kit（エムエルキット）** は、Googleが提供する **モバイル向け機械学習SDK**。本プロジェクトでは主に **バーコード/QRコード スキャン機能** に使用する。カメラで撮影した映像からリアルタイムでバーコードを認識・デコードする。

### 📖 技術背景
- 2018年にFirebase ML Kitとしてリリース → 2020年にFirebase非依存のML Kitとして独立
- バーコード認識は **オンデバイス（端末内）** で完結。ネットワーク不要
- 対応フォーマット: EAN-13, EAN-8, UPC-A, UPC-E, Code-128, QRコード 等（主要フォーマット網羅）
- 本プロジェクトでは **店舗での商品スキャン → 在庫確認・価格確認** ワークフローに使用

### ✅ メリット
| メリット | 説明 |
|---|---|
| **完全無料** | 使用料・API呼び出し制限なし |
| **オフライン動作** | モデルが端末に内蔵。ネットワークなしでもスキャン可能（倉庫・地下売場でも使える） |
| **高精度・高速** | Googleの機械学習モデルで傾き・汚れのあるバーコードも認識 |
| **CameraXとの統合** | `ImageAnalysis` にML Kitの分析器を接続するだけで実装完了 |
| **多フォーマット** | 1D（バーコード）・2D（QRコード）を1つのAPIで両方認識 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **APKサイズ増加** | バーコードモデル分（約3〜5MB）がAPKサイズに加算 |
| **Google Play Services依存** | 一部機能はGoogle Play Servicesが必要。Huaweiデバイスなどでは注意 |
| **カスタマイズ制限** | 認識アルゴリズムのチューニング（閾値変更等）はできない |

---

## 10. カメラ：CameraX

### 🔤 これは何？
**CameraX（カメラエックス）** は、Googleが開発した **Jetpackのカメラライブラリ**。Android端末のカメラを簡単・安全に制御するための抽象化レイヤー。従来のCamera2 APIの複雑さを解消し、**デバイス差異を自動吸収** する。

### 📖 技術背景
- AndroidのカメラAPIは歴史的に3世代：Camera1（非推奨）→ Camera2（複雑）→ CameraX
- Camera2は端末ごとの動作差異が激しく、**200行以上のボイラープレート** が必要だった
- CameraXは2019年リリース。内部でCamera2を使用しつつ、**端末固有のバグをGoogleが吸収**
- 主な用途（UseCase）が3つに整理されている:
  - **Preview**: カメラプレビュー表示
  - **ImageCapture**: 写真撮影
  - **ImageAnalysis**: フレームごとの画像解析（ML Kitとの接続ポイント）
- 本プロジェクトでは **バーコードスキャン時のカメラ制御** に使用

### ✅ メリット
| メリット | 説明 |
|---|---|
| **デバイス互換性** | Google が数百種類の端末でテスト。端末固有のバグを自動回避 |
| **少ないコード量** | Camera2の1/5程度のコードでカメラ機能を実装 |
| **ライフサイクル対応** | `ProcessCameraProvider`がActivityのライフサイクルに自動追従。メモリリーク防止 |
| **ML Kit連携** | `ImageAnalysis`のアナライザーにML Kitのスキャナーを渡すだけで連携完了 |
| **Compose対応** | `PreviewView`のComposeラッパーでカメラプレビューをComposable内に配置可能 |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **カスタマイズ制限** | Camera2の全機能にはアクセスできない（手動フォーカス・RAW撮影等は制限あり） |
| **初期化時間** | カメラの初期化に数百msかかる場合がある。UXへの配慮が必要 |
| **ライブラリサイズ** | カメラ関連の依存で APK サイズが増加 |

---

## 11. バックグラウンド：WorkManager

### 🔤 これは何？
**WorkManager（ワークマネージャー）** は、Googleが開発した **バックグラウンドタスク管理ライブラリ**。アプリが閉じられても、端末が再起動されても、**確実に実行される必要がある非同期タスク** をスケジュール・実行する。

### 📖 技術背景
- Androidはバッテリー最適化のため、バックグラウンド処理に厳しい制限がある（Dozeモード等）
- 従来は `AlarmManager`, `JobScheduler`, `Firebase JobDispatcher` 等が乱立していた
- WorkManagerはこれらを統合し、**APIレベル14以上で統一的に動作**
- 内部でOS バージョンに応じて最適な実装（JobScheduler / AlarmManager）を自動選択
- 本プロジェクトでは **オフライン時の注文同期・在庫データの定期同期** に使用

### ✅ メリット
| メリット | 説明 |
|---|---|
| **実行保証** | アプリ終了・端末再起動後も確実に実行。注文データの欠損を防止 |
| **制約条件設定** | ネットワーク接続時のみ・充電中のみ等の条件付き実行が可能 |
| **チェーン実行** | 複数タスクの直列・並列実行を組み合わせ可能（例: データ取得 → 変換 → アップロード） |
| **Hilt統合** | `@HiltWorker`で依存性注入可能。テストも容易 |
| **進捗監視** | `WorkInfo`をFlowで監視可能。同期状況をUIに表示できる |

### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **即時実行保証なし** | OSのバッテリー最適化により、実行タイミングが遅延する場合がある（最大15分） |
| **15分の最小間隔** | `PeriodicWorkRequest`の最小実行間隔は15分。リアルタイム性が必要な場合は不向き |
| **デバッグ困難** | バックグラウンド実行のため、動作確認・ログ取得が難しい |
| **OEMカスタマイズ問題** | 一部の中国メーカー（Xiaomi, Huawei等）のOS独自最適化でタスクが強制終了される場合がある |

---

## 12. テスト：JUnit5 + MockK + Turbine

### 12-1. JUnit5（ジェイユニット ファイブ）

#### 🔤 これは何？
**JUnit5** は、Java/Kotlinの **ユニットテストフレームワーク** の最新バージョン。テストメソッドの定義・アサーション・テストのライフサイクル管理を行う。テストコードの「骨格」を提供する。

#### 📖 技術背景
- JUnit は Javaエコシステムで **20年以上** 使われているテストフレームワーク
- JUnit4 → JUnit5 で大幅リニューアル：
  - **JUnit Jupiter**: 新しいアノテーション（`@Nested`, `@ParameterizedTest`等）
  - **JUnit Platform**: テスト実行エンジン
- Kotlin の `suspend fun` テストにも対応（`kotlinx-coroutines-test`と組み合わせ）
- AndroidのローカルテストとJUnit5が組み合わさり、高速なテスト実行が可能

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **@Nested** | テストをグループ化して階層構造にでき、テストの意図が明確になる |
| **@ParameterizedTest** | 複数のテストデータを1つのテストメソッドで実行。CQRS各パターンの網羅が容易 |
| **@DisplayName** | テスト名を日本語で記述可能。レポートの可読性向上 |
| **アサーション充実** | `assertAll`, `assertThrows` 等でテストの表現力が高い |
| **拡張機能** | `@ExtendWith`でカスタム拡張（CoroutineTest等）を容易に統合 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Android Instrumented Test** | JUnit5はAndroidの Instrumented Test（実機テスト）に完全対応していない。ローカルテスト向け |
| **プラグイン必要** | Android Gradleプラグインとの統合に `android-junit5` プラグインの追加設定が必要 |
| **移行コスト** | JUnit4からのアノテーション変更（`@Before` → `@BeforeEach` 等）が必要 |

---

### 12-2. MockK（モック ケー）

#### 🔤 これは何？
**MockK（モック ケー）** は、Kotlin専用の **モッキングライブラリ**。テスト時に依存オブジェクト（Repository・APIクライアント等）の **偽物（モック）** を作り、本物のAPI通信やDB操作をせずにViewModelのテストを行えるようにする。

#### 📖 技術背景
- Javaでは `Mockito` がモッキングの定番だが、Kotlinの `data class`, `object`, `suspend fun` との相性が悪い
- MockKは **Kotlin専用に設計** されており、Kotlinの言語機能をフル活用
- `every { }`, `coEvery { }`（Coroutines用）でモックの振る舞いを定義
- `verify { }`, `coVerify { }` でメソッドが期待通り呼ばれたか検証
- 本プロジェクトでは **Repository層のモック化** により、ViewModel のテストをAPI通信なしで実行

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **Kotlin完全対応** | `suspend fun`, `data class`, `object`, `companion object` すべてモック可能 |
| **DSL記法** | `every { repo.getProducts() } returns flowOf(products)` のように直感的に記述 |
| **Coroutines対応** | `coEvery` / `coVerify`でsuspend functionのモック・検証がシンプル |
| **Relaxed Mock** | `relaxed = true`で未定義の呼び出しにデフォルト値を返すモックを生成。テスト準備を簡略化 |
| **スパイ機能** | 本物のオブジェクトの一部だけモックに差し替えるスパイ機能を搭載 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **学習コスト** | MockK固有のDSL（`every`, `slot`, `answers`等）の習得が必要 |
| **テスト実行速度** | リフレクションを多用するため、Mockitoと比べてやや遅い場合がある |
| **Android Instrumented Test** | Instrumented Testでの利用にはいくつかの制限・追加設定が必要 |

---

### 12-3. Turbine（タービン）

#### 🔤 これは何？
**Turbine（タービン）** は、Cash App（Square社）が開発した **Kotlin Flowのテスト専用ライブラリ**。Flowから発行される値を **順番通りにテスト** するための専用APIを提供する。

#### 📖 技術背景
- Flowは非同期ストリームのため、通常のテストでは「どの値がいつ来るか」の検証が困難
- `flow.first()` や `toList()` だけでは中間状態（Loading → Success等）のテストが不十分
- Turbineは `flow.test { }` ブロック内で `awaitItem()` を呼ぶだけで、発行順に値を取得・検証可能
- 本プロジェクトでは **ViewModelのUiState変化のテスト**（Loading → データ表示 → エラー等）に必須

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **直感的なAPI** | `awaitItem()` で次の値を取得、`awaitComplete()` で完了を確認。分かりやすい |
| **中間状態テスト** | Loading → Success の状態遷移を1つずつ検証。CQRS各コマンドの結果確認に最適 |
| **タイムアウト制御** | 値が来ない場合は自動タイムアウト。テストがハングすることを防止 |
| **エラー検証** | `awaitError()` でFlowの例外を簡単にテスト |
| **軽量** | 依存が最小限。既存のテストスイートに容易に追加可能 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Flow専用** | LiveData等のFlow以外のリアクティブストリームには使用不可 |
| **学習コスト** | `awaitItem()` の呼び出し順序を間違えるとテストが失敗。Flowの発行順の理解が必要 |
| **UIテスト非対応** | Composeのテストには`ComposeTestRule`を併用する必要がある |

### 12-4. 3つを組み合わせたテストの全体像

```kotlin
@Test
fun `商品一覧を取得すると、Loading→Successの順にUiStateが変化する`() = runTest {
    // MockK: Repositoryのモック作成
    val repository = mockk<ProductRepository>()
    coEvery { repository.getProducts("furniture") } returns flowOf(sampleProducts)

    // JUnit5: ViewModelのテスト
    val viewModel = ProductListViewModel(repository)

    // Turbine: Flowのテスト
    viewModel.uiState.test {
        // 初期状態
        awaitItem() shouldBe ProductListUiState()

        // 商品取得を実行
        viewModel.onCategorySelected("furniture")

        // Loading状態
        awaitItem().isLoading shouldBe true

        // Success状態
        val successState = awaitItem()
        successState.isLoading shouldBe false
        successState.products shouldBe sampleProducts
    }

    // MockK: Repositoryが正しく呼ばれたか検証
    coVerify { repository.getProducts("furniture") }
}
```

| ライブラリ | 上記テストでの役割 |
|---|---|
| **JUnit5** | テストの実行基盤。`@Test`アノテーション・アサーション |
| **MockK** | `ProductRepository`のモック作成・振る舞い定義・呼び出し検証 |
| **Turbine** | `uiState`（StateFlow）の値を時系列で取得・検証 |

---

## 13. CI/CD：GitHub Actions + FAD

### 13-1. GitHub Actions（ギットハブ アクションズ）

#### 🔤 これは何？
**GitHub Actions** は、GitHubが提供する **CI/CD（継続的インテグレーション/継続的デリバリー）サービス**。コードをGitHubにpushするだけで、自動的に **ビルド・テスト・デプロイ** を実行するワークフローを構築できる。

**CI/CDとは？**
- **CI（Continuous Integration）**: コード変更のたびに自動でビルド・テストを実行し、問題を早期発見
- **CD（Continuous Delivery）**: テスト通過後、自動でテスト配信（FAD）やストア公開を実行

#### 📖 技術背景
- 従来のCI/CDは Jenkins（サーバー管理必要）や CircleCI（別サービス契約必要）が主流
- GitHub Actionsは2019年に一般公開。**GitHubリポジトリと完全統合** でゼロコンフィグに近い
- **YAML形式** でワークフローを定義（`.github/workflows/`ディレクトリ）
- パブリックリポジトリは **完全無料**、プライベートリポジトリも **月2,000分の無料枠**
- **マーケットプレイス** に数千のAction（再利用可能なステップ）が公開されている

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **GitHub統合** | PRごとに自動テスト実行。テスト結果がPR上に直接表示 |
| **無料枠** | プライベートリポジトリでも月2,000分の無料枠。小〜中規模チームなら十分 |
| **YAML定義** | ワークフローをコードで管理（Infrastructure as Code）。バージョン管理可能 |
| **マーケットプレイス** | Android Build, Firebase Deploy 等のActionが豊富。車輪の再発明不要 |
| **マトリックスビルド** | 複数APIレベルでの同時テスト実行が容易 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **Androidビルド時間** | Gradleビルドが重く、無料ランナーでは **15〜30分** かかる場合がある |
| **キャッシュ管理** | Gradleキャッシュの適切な設定をしないとビルド時間が大幅に増加 |
| **デバッグ困難** | ワークフローのデバッグはpush→実行→ログ確認のサイクルが遅い |
| **GitHub依存** | GitHub障害時はCI/CDが停止。GitLab等への移行コストが発生 |

---

### 13-2. FAD（Firebase App Distribution）

#### 🔤 これは何？
**FAD（Firebase App Distribution）** は、Googleが提供する **テスト版アプリの配信サービス**。Google Play Storeを経由せずに、テスターのAndroid端末に直接APK/AABを配信できる。

#### 📖 技術背景
- 従来のテスト配信は **Google Play Console の内部テストトラック** や **DeployGate** が主流
- FADは2019年にFirebaseの一部としてリリース
- **メールアドレス or Googleグループ** でテスターを招待するだけで配信可能
- テスターはメール内のリンクからAPKをインストール（ストア不要）
- Gradle プラグイン (`appDistribution`) で **ビルド → 配信を1コマンド** で実行可能
- 本プロジェクトでは **海外拠点のテスターへのテスト配信** に使用

#### ✅ メリット
| メリット | 説明 |
|---|---|
| **完全無料** | 配信数・テスター数に制限なし |
| **簡単な配信フロー** | メールアドレスを登録 → ビルド → 配信。Google Play Consoleの審査不要 |
| **GitHub Actions連携** | CI/CDパイプラインに組み込んで自動配信。PR マージ → 即テスト版配信 |
| **リリースノート** | 配信時にリリースノートを添付可能。テスターに変更点を伝達 |
| **テスターグループ** | 国・役割ごとにグループ分けして配信可能（例: `japan-qa`, `overseas-pm`） |
| **Firebase統合** | Crashlytics と連携し、テスト版のクラッシュレポートを自動収集 |

#### ❌ デメリット
| デメリット | 説明 |
|---|---|
| **テスター上限管理** | 大規模テスト（数百人規模）では招待管理が煩雑になる場合がある |
| **iOSの制限** | iOS版はUDID登録 or Ad Hocプロビジョニングが必要。Androidより手順が多い |
| **分析機能は限定的** | インストール率・起動率等の詳細分析はGoogle Play Consoleの方が充実 |
| **Firebase依存** | Firebaseプロジェクトのセットアップが必須。Firebase未使用プロジェクトには導入コストあり |

### 13-3. GitHub Actions + FAD のCI/CDフロー

```
開発者がPRを作成
     │
     ▼
┌──────────────────────────────────────────┐
│  GitHub Actions（自動実行）              │
│                                          │
│  ① Lint チェック（ktlint / detekt）     │
│  ② ユニットテスト（JUnit5 + MockK）     │
│  ③ ビルド（assembleDebug）              │
│  ④ テスト結果をPRにコメント            │
└──────────────────┬───────────────────────┘
                   │ PRがマージされたら
                   ▼
┌──────────────────────────────────────────┐
│  GitHub Actions（自動実行）              │
│                                          │
│  ① リリースビルド（assembleRelease）    │
│  ② FAD にアップロード                   │
│  ③ テスターにメール通知                 │
└──────────────────┬───────────────────────┘
                   │
                   ▼
          テスターが端末で
          テスト版をインストール
```

---

## 📌 技術スタック依存関係マップ

各技術がどう連携しているかの全体像：

```
┌─────────────────────────────────────────────────────────────┐
│                     Kotlin 2.x（全体の基盤）                │
├──────────┬──────────┬──────────┬──────────┬─────────────────┤
│          │          │          │          │                 │
│  Jetpack │  Hilt    │Coroutines│ Room     │  WorkManager    │
│  Compose │  (DI)    │ + Flow   │+DataStore│ (バックグラウンド)│
│  (UI)    │          │ (非同期) │(ローカル)│                 │
│    │     │    │     │    │     │    │     │       │         │
│    └─────┼────┘     │    │     │    │     │       │         │
│  Coil────┘          │    │     │    │     │       │         │
│ (画像)              │    │     │    │     │       │         │
│                     │    │     │    │     │       │         │
│  CameraX ─── ML Kit │    │     │    │     │       │         │
│ (カメラ)  (バーコード)│    │     │    │     │       │         │
│                     │    │     │    │     │       │         │
│          Retrofit2──┼── OkHttp3│    │     │       │         │
│          (API定義)  │(HTTP通信)│    │     │       │         │
│                     │          │    │     │       │         │
│                     │    FCM ──┘    │     │       │         │
│                     │ (プッシュ通知) │     │       │         │
├─────────────────────┴──────────┴────┴─────┴───────┴─────────┤
│              GitHub Actions + FAD（CI/CD・テスト配信）       │
├─────────────────────────────────────────────────────────────┤
│           JUnit5 + MockK + Turbine（テスト基盤）            │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 まとめ：このスタックを選んだ理由

1. **Google公式推奨の統一感**: Kotlin / Compose / Hilt / Room / CameraX / WorkManager すべてがJetpackエコシステムの一部。相互連携が保証されている
2. **Coroutines + Flow を軸にした非同期統一**: API通信 → UI更新 → テストまで、一貫してCoroutines/Flowで記述。パラダイムの混在を回避
3. **CQRS パターンとの親和性**: Retrofit2の `@GET` / `@POST` でQuery/Commandを明確に分離。Flowで状態変化を追跡
4. **オフラインファースト対応**: Room（キャッシュ）+ WorkManager（同期）+ ML Kit（オフラインスキャン）で、ネットワーク不安定な店舗環境にも対応
5. **テスト容易性**: MockK（モック）+ Turbine（Flow テスト）で、ViewModelの状態遷移を高精度に検証可能
6. **大規模開発対応**: Hilt（コンパイル時DI検証）+ GitHub Actions（自動テスト）で、50人規模のチームでも品質を維持

