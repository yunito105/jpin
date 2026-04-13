# ADR-003: モバイル（Android）テスト戦略

| 項目 | 内容 |
|---|---|
| ステータス | **承認（Accepted）** |
| 日付 | 2026-04-13 |
| 決定者 | テックリード |

---

## 状況

ADR-001（フロントエンドアーキテクチャ）および ADR-002（フロントエンドテスト戦略）と並行して、モバイル（Android）のテスト戦略を確立する必要がある。

本PJのモバイルに関する前提を整理すると以下の通りである。

| 区分 | 内容 |
|---|---|
| システム規模 | 25億円規模・複数サブドメイン（在庫・発注・物流・店舗・商品マスタ等）・社内業務システム |
| モバイルの責務 | **表示ルール・UI/UX**。業務ロジックの Source of Truth はバックエンドに置く |
| バックエンドの責務 | 業務ロジック（DDD + ヘキサゴナルアーキテクチャ + 簡易CQRS） |
| モバイルアーキテクチャ | Kotlin + MVVM + UDF（Unidirectional Data Flow） |
| 対象 OS | Android 12 以上（android-version-policy.md 参照） |
| 開発環境 | AI支援開発（GitHub Copilot等）を全エンジニアが利用する前提 |

---

## 検討の経緯

### フェーズ1：初期方針の整理

モバイルの責務は「表示ルール・UI/UX」であるため、自動テストの対象をどこまで広げるかが論点となった。初期の考え方は以下の通りであった。

| 対象 | 初期方針 |
|---|---|
| UIコンポーネント（Composable） | 自動テストなし。手動テスト（打鍵・モンキー）で代替 |
| ViewModel / Repository のロジック | MockKで自動テストを実施 |
| エラーハンドリング | MockKで異常系テストを実施 |

UIコンポーネント（Composable）は変更頻度が高く、描画の細部をテストするコストが見合わないと判断した。これはADR-002でフロントエンドのコンポーネントテストを書かないと決めた根拠と同様である。

### フェーズ2：AI支援開発の考慮による強化

ADR-002と同様に「AI支援開発によりテストコードの生成・修正コストが低下している」という観点を踏まえた。

ViewModel・Repository のロジックは以下の特性を持つ。

- Jetpack Compose（UI層）に依存しない純粋なビジネスロジック
- Coroutineの非同期処理・状態管理・エラーハンドリングを含む
- MockKによってDIの依存関係を差し替えて高速に実行できる

これらはAI支援でテストコードを生成・修正しやすく、かつバグ発生時のダメージが大きい層である。自動テストの費用対効果が高いと判断し、単体テストの積極的な導入を確定した。

### フェーズ3：UI自動テストを採用しない判断の確定

Espresso・Compose UI Test（`composeTestRule`）などのUIテストフレームワークは採用しない。理由は以下の通りである。

1. **脆弱性が高い**：UIの変更（レイアウト・アニメーション・コンポーネントの分割・統合）のたびにテストが壊れる
2. **実行が遅い**：インストルメンテッドテストはエミュレータまたは実機が必要で、CIの実行時間が大幅に増加する
3. **モバイルの責務は表示**：UI/UXの品質は手動テスト（打鍵・モンキー）による体感確認の方が適している

UI/UXの品質は、モンキーテストや打鍵テストという手動検証で担保する。

---

## 課題

1. **UIテストと非UIテストの明確な境界設定** — 「どこからがUI」かを明確にしないと、ViewModelテストがUIに依存しない形で書けているかが曖昧になる
2. **Coroutineテストの標準化** — `runTest` / `TestDispatcher` の使い方をチームで統一しないと、非同期テストの品質が揺れる
3. **手動テストの役割の明文化** — UIテストを手動に委ねる以上、何を手動で確認するかを定義しないと抜け漏れが生じる

---

## 結論

本PJのモバイルテスト戦略は以下の**3層の自動テスト + 2種の手動テスト**で構成する。

### テスト戦略全体像

```
┌──────────────────────────────────────────────────────────────┐
│               モバイル テスト戦略全体像                        │
├─────────────────────────────┬────────────────────────────────┤
│        自動テスト             │        手動テスト               │
├─────────────────────────────┼────────────────────────────────┤
│ [1] Static Analysis          │ [手動1] 打鍵テスト              │
│  Kotlin strict + detekt      │  機能追加・変更後の動作確認      │
│  （常時・全コード）           │  画面操作・遷移・入力の確認      │
├─────────────────────────────┼────────────────────────────────┤
│ [2] Unit Tests               │ [手動2] モンキーテスト          │
│  JUnit / Kotest + MockK      │  探索的テスト                   │
│  ViewModel / Repository /    │  リリース前の品質確認            │
│  UseCase / 純粋ロジック       │                                │
└─────────────────────────────┴────────────────────────────────┘

バックエンド：古典学派の自動単体テスト（JUnit + Mockito）
フロントエンド：Vitest + RTL + MSW + Playwright
```

---

### ツールスタック比較

プロジェクト全体のテストツールを並べ、モバイルの選定に一貫性を持たせる。

| 層 | 言語 | テストランナー | モック/スタブ |
|---|---|---|---|
| バックエンド | Java | JUnit | **Mockito**（クラス・メソッドのモック） |
| モバイル | Kotlin | JUnit / Kotest | **MockK**（Kotlin向け・Coroutine対応） |
| フロントエンド | TypeScript | Vitest | `vi.mock()` + **MSW**（HTTPインターセプト） |

MockKをモバイルに採用する理由：

- Kotlin の言語仕様（`data class`・`object`・`companion object`・`suspend fun`）に対応している
- Coroutine（`suspend`関数）のモックに `coEvery` / `coVerify` を使えるため、非同期処理のテストが自然に書ける
- Mockito はJava向けに設計されており、Kotlin の `final` クラス・`object` のモックに追加設定が必要になる場面が多い

---

### 各層の詳細

#### [1] Static Analysis（Kotlin + detekt）

**何を守るか**：型の誤り・コーディング規約・Kotlin固有のアンチパターン

- Kotlinの型システム（`null` 安全性・`sealed class` の網羅性チェック）によりコンパイル時に多くの誤りを検出する
- `detekt` により Kotlin 特有の問題（複雑すぎる関数・過剰なネスト・未使用のリソース等）を静的に検出する
- コストがほぼゼロで最も広い範囲をカバーする最優先の品質ゲートである

---

#### [2] Unit Tests（JUnit / Kotest + MockK + kotlinx-coroutines-test）

**何を守るか**：UIに依存しないロジック層（ViewModel・Repository・UseCase・純粋関数）の正確さ

##### テスト対象の層

```
UI Layer（Composable） ← テストしない（手動テストで代替）
       ↓
ViewModel              ← テストする（状態遷移・エラーハンドリング）
       ↓
UseCase / Interactor   ← テストする（業務フローのオーケストレーション）
       ↓
Repository             ← テストする（データ取得・エラーマッピング）
       ↓
DataSource（API/DB）    ← MockKでスタブ化（実際のI/Oは呼ばない）
```

##### ViewModel のテスト例（状態遷移）

```kotlin
@Test
fun `商品一覧取得が成功したとき UiState が Success に遷移する`() = runTest {
    // Arrange
    val products = listOf(Product(id = "001", name = "テスト商品"))
    coEvery { repository.getProducts() } returns products

    // Act
    viewModel.loadProducts()

    // Assert
    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(UiState.Success::class.java)
    assertThat((state as UiState.Success).data).isEqualTo(products)
}
```

##### ViewModel のテスト例（エラーハンドリング）

```kotlin
@Test
fun `APIが500エラーを返したとき UiState が Error に遷移する`() = runTest {
    // Arrange
    coEvery { repository.getProducts() } throws ApiException(500, "Internal Server Error")

    // Act
    viewModel.loadProducts()

    // Assert
    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(UiState.Error::class.java)
    assertThat((state as UiState.Error).message).contains("エラーが発生しました")
}

@Test
fun `401エラー時に authManager の logout が呼ばれる`() = runTest {
    // Arrange
    coEvery { repository.getProducts() } throws AuthException(401)

    // Act
    viewModel.loadProducts()

    // Assert
    coVerify { authManager.logout() }
}
```

##### Repository のテスト例（エラーマッピング）

```kotlin
@Test
fun `ネットワーク接続なしのとき NetworkUnavailableException が投げられる`() = runTest {
    // Arrange
    coEvery { apiService.getProducts() } throws IOException("No connection")

    // Act / Assert
    assertThrows<NetworkUnavailableException> {
        repository.getProducts()
    }
}
```

**自動化する対象の例**：

```
viewmodel/InventoryViewModel.kt  → 在庫状態の遷移・絞り込みロジック
viewmodel/OrderViewModel.kt      → 発注フローの状態管理・バリデーション
repository/InventoryRepository   → APIエラーの型変換・リトライロジック
usecase/SubmitOrderUseCase.kt    → 発注の整合性チェック・ドメインルール検証
util/DateFormatter.kt            → 表示用日付フォーマット変換
util/CurrencyFormatter.kt        → 通貨フォーマット（小数点・通貨記号）
```

**何を書かないか**：Composableの描画内容・Navigationグラフ・UIコンポーネントの見た目

**Coroutineテストの標準設定**：

```kotlin
// テストクラス共通のセットアップ
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // TestDispatcherをMainに差し替え

    private val repository: InventoryRepository = mockk()
    private lateinit var viewModel: InventoryViewModel

    @BeforeEach
    fun setUp() {
        viewModel = InventoryViewModel(repository)
    }
}
```

---

#### 手動テスト（打鍵・モンキー）

UIテストを自動化しない代わりに、以下の手動検証で品質を担保する。

| テスト種別 | 目的 | タイミング |
|---|---|---|
| **打鍵テスト** | 機能追加・変更後の画面操作確認。遷移・入力・表示の体感確認 | 開発完了後・リリース前 |
| **モンキーテスト** | 設計者が予期しない操作パターン・UX上の問題の探索 | リリース前の最終品質確認 |

**手動テストで確認する観点**：

| 観点 | 内容 |
|---|---|
| 画面遷移 | ボタンタップ・バック操作・ディープリンクが正しく動作するか |
| 入力操作 | ソフトキーボード・日付ピッカー・ドロップダウンが正しく動作するか |
| 読み込み状態 | LoadingIndicatorが適切に表示・非表示になるか |
| エラー表示 | Snackbar・ダイアログ・インラインエラーが正しく表示されるか |
| レイアウト | 文字列の長さ・多言語（海外拠点向け）でのレイアウト崩れがないか |
| 端末差異 | Android 12〜15 の異なるバージョン・画面サイズでの表示確認 |

---

### テスト戦略の役割分担まとめ

| テスト種別 | 自動/手動 | 守るもの | 補完関係 |
|---|:---:|---|---|
| Static Analysis | 自動 | 型・規約・Kotlinアンチパターン | すべての層の前提 |
| Unit Tests（MockK） | 自動 | ViewModel・Repository・UseCase のロジックとエラーハンドリング | 手動テストのリグレッション負担を軽減 |
| 打鍵テスト | 手動 | 変更後の画面操作・体感UX | 自動テストが届かないUI層を補完 |
| モンキーテスト | 手動 | 予期しない操作・探索的バグ | 自動テストが設計できない観点 |

---

## なぜUI自動テストを採用しないのか

フロントエンドのコンポーネントテスト不採用と同じ論理が成立する。

```
【UIテストを書く場合のコスト構造】
変更頻度（高：UIは継続的に改善される）
  × テストの脆弱性（高：Composableの構造変更で即破綻）
  × 実行コスト（高：エミュレータ起動が必要）
= 総コスト（過剰）→ 採用しない
```

一方、ViewModelテストはコスト構造が正反対である。

```
【ViewModel単体テストのコスト構造】
変更頻度（中：状態管理ロジックは比較的安定）
  × テストの安定性（高：UIに依存しないためリファクタリングに強い）
  × 実行コスト（低：JVM上で数ミリ秒で実行）
  × AI支援（メンテ修正をAIが即提案）
= 総コスト（許容範囲）→ 採用する
```

---

## テスト戦略の判断基準（ガイドライン）

```
Q1: 型・null安全性・Kotlinのアンチパターンを検出したいか？
    → 自動：Kotlin strict + detekt

Q2: ViewModelの状態遷移・ユーザーイベント処理が正しいか？
    → 自動：JUnit/Kotest + MockK 単体テスト

Q3: Repositoryがエラーを正しくマッピング・伝播しているか？
    → 自動：JUnit/Kotest + MockK 単体テスト

Q4: 認証エラー（401）時のlogoutやリダイレクト処理が動くか？
    → 自動：MockKで AuthManager をスパイしてverify

Q5: 画面の見た目・ナビゲーション・体感UXを確認したいか？
    → 手動：打鍵テスト

Q6: 予期しない操作パターン・レイアウト崩れを探索したいか？
    → 手動：モンキーテスト

Q7: ComposableのDOMツリー・アニメーション・CSSをテストしたいか？
    → 書かない（変更に弱く、費用対効果が低い）
```

---

## リスクと対策

| リスク | 対策 |
|---|---|
| ViewModelテストがComposableの実装詳細に依存して壊れやすくなる | ViewModelはUIフレームワーク（Composable/Context）に依存しない純粋なKotlinクラスとして設計する。`ViewModel()`を継承するが、`@Composable`関数や`Context`を直接参照しない |
| Coroutineテストで `runBlocking` を使い非同期テストが曖昧になる | `runTest`（`kotlinx-coroutines-test`）と`TestDispatcher`を標準化し、`MainDispatcherRule`をチーム共通のRuleとして定義する |
| MockKの`relaxed mockk`を多用して何も検証しないテストが増える | コードレビューで「`verify`または`assert`が含まれているか」を必須確認項目とする |
| 手動テストでUIバグが見逃される | 打鍵テストのチェックリスト（画面遷移・エラー表示・レイアウト等）をスプリント完了条件（DoD）に含める |
| AI生成テストの品質が低い | AI が書いたテストコードはレビューを必須とする。「何を検証しているか」が不明なテストはリジェクトする |

---

## 採用しなかった選択肢

| 選択肢 | 不採用理由 |
|---|---|
| Espresso によるUIテスト | エミュレータ依存で実行コストが高い。UIの構造変更のたびに壊れやすい。ViewModelテストと手動テストで代替可能 |
| Compose UI Test（`composeTestRule`） | Espressoと同様の問題に加え、Composableの実装詳細に強く依存する。UIの責務は手動テストで担保する |
| Robolectric（JVMでAndroidテスト） | AndroidフレームワークをJVMでシミュレートするため、実機との乖離が生じやすい。ViewModelはAndroidフレームワーク非依存で設計するため不要 |
| Firebase Test Lab / Device Farm | 費用・運用コストに対して、社内業務システムとしての優先度が低い。端末差異の確認は手動テストで対応する |
| Screenshot Test（Paparazzi等） | ビジュアルリグレッションの検出は有効だが、デザインシステムが確立した段階での導入を検討する |

---

## 結論（再掲）

```
【自動テスト】
  [1] Static Analysis : Kotlin strict + detekt（常時）
  [2] Unit Tests      : JUnit / Kotest + MockK
                        対象：ViewModel / Repository / UseCase / 純粋関数
                        特に：状態遷移・エラーハンドリング・401ログアウト等の異常系

【手動テスト】
  打鍵テスト    : 機能変更後の画面操作・体感UX確認
  モンキーテスト: 探索的テスト・リリース前品質確認
```

UIに依存しないロジック層（ViewModel・Repository・UseCase）はMockKによる自動テストで網羅的にカバーする。UIの品質は手動テストで担保し、Composableの自動テストは採用しない。AI支援開発によりテストコードのメンテナンスコストが低下していることから、この戦略は費用対効果の観点で合理的と判断した。

---

## 参考資料

- [MockK 公式](https://mockk.io/)
- [Kotest 公式](https://kotest.io/)
- [kotlinx-coroutines-test 公式](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Android ViewModel テストガイド（公式）](https://developer.android.com/topic/architecture/ui-layer/viewmodels/test)
- [ADR-001: フロントエンドソフトウェアアーキテクチャの選定](./adr-001-frontend-architecture.md)
- [ADR-002: フロントエンドテスト戦略](./adr-002-frontend-test-strategy.md)
- [android-version-policy.md](./android-version-policy.md)
