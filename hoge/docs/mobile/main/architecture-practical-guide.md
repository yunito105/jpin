# Android アーキテクチャ実践ガイド（複数サブドメイン対応版）
## 〜「なぜこの構造なのか」を販売ドメインで超丁寧に解説〜

> このドキュメントは `core-vs-feature-architecture.md` を前提に、  
> **複数サブドメインを持つ大規模プロジェクト**向けの  
> 各ファイル・フォルダの**存在意義**をコードレベルで深掘りする。  
> サンプルは **販売管理システム（受注 / 在庫 / 顧客）** に統一する。

> **技術前提（§14〜18 で詳述）:**  
> - バックエンド: Java / Spring Boot（ヘキサゴナルアーキテクチャ + 簡易 CQRS + DDD）  
> - モバイル: Kotlin（MVVM + UDF）  
> - リポジトリ構成: マルチレポ（フロント / モバイル / バックエンド別リポジトリ）  
> - 責務分離: バックエンド → 業務ロジック（DDD）、モバイル → 表示ルール・UI/UX  
> - §1〜13 は**モバイル単体の内部設計**を扱い、§14〜18 は**システム全体の設計判断**を扱う。

---

## 採用アーキテクチャ：垂直スライス（Vertical Slice）

NiA（Now in Android）は「単一ドメイン × 水平レイヤー」。  
このガイドは「複数サブドメイン × 垂直スライス」を扱う。

```
【NiA 方式：水平レイヤー】          【本ガイド：垂直スライス】
core/data/    ← 全Repository        core/          ← インフラのみ
core/domain/  ← 全UseCase           feature/order/ ← 受注の全層
feature/      ← UIのみ              │  ├── data/   ← 受注Repository
                                    │  ├── domain/ ← 受注UseCase
                                    │  └── impl/   ← 受注UI
                                    feature/customer/ ← 顧客の全層
                                    feature/shared/   ← 意図的な共通化のみ
```

---

## ディレクトリ構成（全体）

```
app/                          ← entryProvider + NavDisplay で全 feature を組み立て
│
core/
├── model/                    ← サブドメインをまたぐ共有型のみ
├── database/                 ← Room DB + 全 Entity/DAO（サブドメイン別パッケージ）
│   ├── order/                ← 受注テーブル定義（受注チームが所有）
│   ├── customer/             ← 顧客テーブル定義
│   ├── inventory/            ← 在庫テーブル定義
│   └── SalesDatabase.kt      ← @Database（全エンティティを列挙）
├── network/                  ← OkHttp / Retrofit の設定だけ（ドメイン知識ゼロ）
├── navigation/               ← Navigator, NavigationState（navigation3 基盤）
├── datastore/                ← DataStore の設定だけ
├── designsystem/             ← ボタン・色・タイポグラフィ
└── analytics/                ← ログ送信インターフェース
│
feature/
├── order/                    ← 受注サブドメイン（垂直に完結）
│   ├── api/                  ← 外部公開：NavKey + Navigator 拡張関数
│   ├── data/                 ← OrderRepository（interface + impl）
│   ├── domain/               ← 受注専用 UseCase 群
│   └── impl/                 ← OrderViewModel + OrderScreen + EntryProvider
│
├── inventory/                ← 在庫サブドメイン
│   ├── api/
│   ├── data/
│   ├── domain/
│   └── impl/
│
├── customer/                 ← 顧客サブドメイン
│   ├── api/                  ← 外部公開：NavKey + 他ドメインが使う軽量型 + Navigator 拡張
│   ├── data/
│   ├── domain/
│   └── impl/
│
└── shared/                   ← 「意図的な」横断共通機能のみ
    ├── auth/                 ← 認証（全サブドメインが必要）
    └── notification/         ← 通知（複数ドメインが使う）
```

---

## 目次

1. [そもそもなぜ「層」に分けるのか](#1-そもそもなぜ層に分けるのか)
2. [core/model ── 「共通語彙集」はなぜ必要か](#2-coremodel)
3. [core/database ── なぜ Entity/DAO だけここに残すのか](#3-coredatabase)
4. [core/network ── なぜドメイン知識をゼロにするのか](#4-corenetwork)
5. [feature/*/data ── Repository はなぜ feature に置くのか](#5-featuredata)
6. [feature/*/domain ── UseCase はなぜ feature に置くのか](#6-featuredomain)
7. [feature/*/impl ── ViewModel と UI](#7-featureimpl)
8. [feature/*/api ── 外部公開の境界線](#8-featureapi)
9. [feature/shared ── 「意図的な共通化」とは何か](#9-featureshared)
10. [Hilt DI ── どこで何を bind するか](#10-hilt-di)
11. [Flow / StateFlow ── なぜ必要か](#11-flow--stateflow)
12. [全体の依存フローを追う（受注一覧画面の例）](#12-全体の依存フローを追う)
13. [「一見共通な型」と「サブドメイン内共通」の設計判断](#13-一見共通な型とサブドメイン内共通の設計判断)
14. [バックエンドとモバイルの責務分界点](#14-バックエンドとモバイルの責務分界点)
15. [マルチレポ構成での API 契約管理](#15-マルチレポ構成での-api-契約管理)
16. [Offline-First vs API-First の選定基準](#16-offline-first-vs-api-first-の選定基準)
17. [モバイル `domain/` 層の再定義](#17-モバイル-domain-層の再定義)
18. [バックエンドアーキテクチャ概要（ヘキサゴナル + 簡易 CQRS）](#18-バックエンドアーキテクチャ概要ヘキサゴナル--簡易-cqrs)

---

## 1. そもそもなぜ「層」に分けるのか

### 問い：1ファイルに全部書いたらダメなのか？

```kotlin
// ❌ 動くけど、チーム開発・テスト・保守でつらくなる書き方
@Composable
fun OrderListScreen() {
    val orders = remember { mutableStateListOf<Order>() }
    LaunchedEffect(Unit) {
        val db = Room.databaseBuilder(...).build()
        val response = Retrofit.create(OrderApi::class.java).getOrders()
        val filtered = response.filter { it.status != "CANCELLED" }
        orders.addAll(filtered)
    }
    LazyColumn { items(orders) { OrderRow(it) } }
}
```

| 問題 | 具体的に何が起きるか |
|------|-------------------|
| **テストできない** | DB/API がないと動かない。Preview もクラッシュ |
| **再利用できない** | 在庫画面でも受注一覧が必要→コピペしか手がない |
| **変更が怖い** | API 変更の影響範囲が追えない |
| **並行開発できない** | UI チームが API 変更を待つ必要がある |
| **ビジネスルール散在** | 「キャンセル除外」ロジックが3画面にコピペされる |

**層に分けることで：**

```
UI（Composable）  ← 「見せ方」だけ
ViewModel         ← 「UI状態管理」だけ
UseCase           ← 「ビジネスルール」だけ     ← feature/*/domain/
Repository        ← 「データの出どころ判断」だけ ← feature/*/data/
DAO / API         ← 「SQL・HTTP」だけ          ← core/database/
```

---

## 2. `core/model`

### 「共通語彙集」はなぜ必要か

#### 問い：型は使う場所に書けばよくないか？

```kotlin
// feature/order/impl の中に定義した場合
data class Order(val id: String, val totalAmount: Int)

// feature/billing/impl でも受注データが必要になった
// → feature 間の依存は禁止 → Order 型が使えない
// → 仕方なく BillingOrder という別の型を作る → 型が乱立
```

`core/model` は「全チームが参照できる共通語彙」。**ここだけは全 feature が依存してよい。**

```
core/model/                ← Android 非依存。KMP でも使えるピュア Kotlin
  shared/                  ← サブドメインをまたぐ型のみ
    Money.kt               ← 金額（価格計算は全ドメインで使う）
    Address.kt             ← 住所（受注・顧客・配送で共通）
    PagedResult.kt         ← ページネーション結果の共通型
```

> **重要：各サブドメインの詳細な型は `core/model` に入れない。**  
> `Order`, `Customer`, `InventoryItem` のような型は各 feature の中で定義する。  
> `core/model` に入れるのは、**複数ドメインが本当に同じ意味で使うもの**だけ。

#### 実装例（販売ドメインの共有型）

```kotlin
// core/model/shared/Money.kt
// 「金額」は受注・請求・在庫すべてで同じ意味を持つ → 共有してよい
@JvmInline
value class Money(val amountInYen: Int) {
    operator fun plus(other: Money) = Money(amountInYen + other.amountInYen)
    operator fun times(quantity: Int) = Money(amountInYen * quantity)
    fun formatted(): String = "¥${"%,d".format(amountInYen)}"
}
```

```kotlin
// core/model/shared/Address.kt
// 配送先住所は受注・顧客・配送の3ドメインで同じ構造 → 共有してよい
data class Address(
    val postalCode: String,
    val prefecture: String,
    val city: String,
    val street: String,
    val building: String? = null,
)
```

```kotlin
// ❌ これは core/model に入れてはいけない
// Order は受注ドメインの型 → feature/order/data/ に置く
data class Order(
    val id: String,
    val items: List<OrderItem>,  // OrderItem も受注ドメイン専有
    ...
)
```

#### 実務では

```
「住所の postal_code を7桁ハイフンなし固定にしたい」
→ core/model/shared/Address.kt を1箇所修正
→ 受注チーム・顧客チーム・配送チームに自動で反映
→ 「住所の表現方法」に関して複数の型が乱立しなくなる
```

---

## 3. `core/database`

### なぜ Entity/DAO だけここに残すのか

#### 全体構造

```
core/database/
├── order/                          ← 受注チームが所有するテーブル定義
│   ├── OrderEntity.kt
│   ├── OrderItemEntity.kt
│   ├── PopulatedOrder.kt           ← JOIN結果のラッパー
│   └── OrderDao.kt
├── customer/                       ← 顧客チームが所有するテーブル定義
│   ├── CustomerEntity.kt
│   └── CustomerDao.kt
├── inventory/                      ← 在庫チームが所有するテーブル定義
│   ├── InventoryItemEntity.kt
│   └── InventoryDao.kt
├── util/
│   └── InstantConverter.kt         ← Room の型コンバーター
├── SalesDatabase.kt                ← @Database（全エンティティを列挙）
└── di/
    └── DatabaseModule.kt           ← DAO の DI 定義
```

> **なぜ Entity/DAO が core/database にあって、Repository が feature/*/data/ にあるのか？**  
> Room の `@Database` アノテーションはコンパイル時に**全 Entity クラスを列挙**する必要がある。  
> Entity が複数の feature モジュールに散らばると、`@Database` がそれらすべてに依存することになり、  
> 依存グラフが逆転する（`core/database` が `feature` に依存 → 禁止）。  
> だから Entity/DAO は `core/database` に置き、ビジネスロジック（Repository/UseCase）だけを feature に移す。

#### Entity はなぜ Model と別の型なのか

```kotlin
// feature/order/data/model/Order.kt（アプリで使うドメインモデル）
data class Order(
    val id: String,
    val customerId: String,
    val items: List<OrderItem>,  // ← リストをそのまま持てる
    val status: OrderStatus,     // ← 型安全な enum
    val totalAmount: Money,      // ← 金額専用型
    val createdAt: Instant,
)
```

```kotlin
// core/database/order/OrderEntity.kt（DBのテーブル行）
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    val status: String,                   // ← DB は文字列しか保存できない
    @ColumnInfo(name = "total_amount") val totalAmountInYen: Int,  // ← Moneyは分解
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    // items は別テーブル order_items に外部キーで保存
)
```

#### 実装例：受注テーブルの全体像

```kotlin
// core/database/order/OrderItemEntity.kt
@Entity(
    tableName = "order_items",
    foreignKeys = [ForeignKey(
        entity = OrderEntity::class,
        parentColumns = ["id"],
        childColumns = ["order_id"],
        onDelete = ForeignKey.CASCADE,  // 受注削除 → 明細も自動削除
    )],
    indices = [Index(value = ["order_id"])],
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "order_id") val orderId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,  // 非正規化（参照コスト削減）
    val quantity: Int,
    @ColumnInfo(name = "unit_price_yen") val unitPriceYen: Int,
)
```

```kotlin
// core/database/order/PopulatedOrder.kt
// JOINクエリ結果のラッパー。アプリモデルへの変換もここに書く
data class PopulatedOrder(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "order_id")
    val items: List<OrderItemEntity>,
)

// DB型 → ドメイン型の変換。Repositoryがこれを呼ぶ
fun PopulatedOrder.asExternalModel() = Order(
    id = order.id,
    customerId = order.customerId,
    status = OrderStatus.valueOf(order.status),
    totalAmount = Money(order.totalAmountInYen),
    items = items.map { it.asExternalModel() },
    createdAt = order.createdAt,
)

fun OrderItemEntity.asExternalModel() = OrderItem(
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = Money(unitPriceYen),
)
```

#### DAO の実装例

```kotlin
// core/database/order/OrderDao.kt
@Dao
interface OrderDao {

    // Flow を返す → DB が更新されると自動で再取得
    @Transaction  // ← orders + order_items を同一トランザクションで取得（整合性保証）
    @Query("""
        SELECT * FROM orders
        WHERE
            CASE WHEN :useStatusFilter
                THEN status IN (:filterStatuses)
                ELSE 1
            END
         AND
            CASE WHEN :useCustomerFilter
                THEN customer_id = :customerId
                ELSE 1
            END
        ORDER BY created_at DESC
        LIMIT CASE WHEN :limit > 0 THEN :limit ELSE -1 END
    """)
    fun getOrders(
        useStatusFilter: Boolean = false,
        filterStatuses: Set<String> = emptySet(),
        useCustomerFilter: Boolean = false,
        customerId: String = "",
        limit: Int = 0,
    ): Flow<List<PopulatedOrder>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrder(orderId: String): Flow<PopulatedOrder?>

    @Upsert  // ← 存在すればUPDATE、なければINSERT（サーバー同期に使う）
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Upsert
    suspend fun upsertOrderItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM orders WHERE id IN (:ids)")
    suspend fun deleteOrders(ids: List<String>)
}
```

#### @Database：全エンティティの列挙とマイグレーション管理

```kotlin
// core/database/SalesDatabase.kt
@Database(
    entities = [
        // 受注ドメイン（core/database/order/ に定義）
        OrderEntity::class,
        OrderItemEntity::class,
        // 顧客ドメイン（core/database/customer/ に定義）
        CustomerEntity::class,
        // 在庫ドメイン（core/database/inventory/ に定義）
        InventoryItemEntity::class,
        InventoryTransactionEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),                           // orders テーブル追加
        AutoMigration(from = 2, to = 3),                           // customers テーブル追加
        AutoMigration(from = 3, to = 4, spec = Schema3to4::class), // status カラムの型変更（手動）
        AutoMigration(from = 4, to = 5),                           // delivery_date カラム追加
    ],
    exportSchema = true,  // schemas/SalesDatabase/5.json がコミットされる
)
@TypeConverters(InstantConverter::class)
internal abstract class SalesDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun inventoryDao(): InventoryDao
}
```

```kotlin
// core/database/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton  // ← DBインスタンスはアプリで1つ（複数あるとロック競合）
    fun providesSalesDatabase(@ApplicationContext context: Context): SalesDatabase =
        Room.databaseBuilder(context, SalesDatabase::class.java, "sales-database")
            .build()

    @Provides
    fun providesOrderDao(db: SalesDatabase): OrderDao = db.orderDao()

    @Provides
    fun providesCustomerDao(db: SalesDatabase): CustomerDao = db.customerDao()

    @Provides
    fun providesInventoryDao(db: SalesDatabase): InventoryDao = db.inventoryDao()
}
```

#### 実務では

```
受注チームが「配送希望日」カラムを追加したいとき：
→ core/database/order/OrderEntity.kt に deliveryDate: Instant? を追加
→ SalesDatabase.kt の version を 4 → 5 に上げる
→ AutoMigration(from = 4, to = 5) を追加（カラム追加は自動対応）
→ schemas/5.json がコミットされ、スキーマ変更が PR でレビューできる
→ 顧客チーム・在庫チームのコードは一切変更不要
```

---

## 4. `core/network`

### なぜドメイン知識をゼロにするのか

#### NiA との違い

NiA では `core/network/` にドメイン固有の API インターフェース（`NiaNetworkDataSource`）が含まれる。  
複数サブドメインでこれをやると、`core/network/` がドメイン知識だらけになる。

```
// ❌ ドメイン知識が混入した core/network/（NiA 流）
core/network/
├── NiaNetworkDataSource.kt  ← getNewsResources(), getTopics() など全部ここ
└── model/NetworkNewsResource.kt  ← ネットワークモデルも全部ここ

// これを販売ドメインでやると...
core/network/
├── OrderApiService.kt
├── CustomerApiService.kt
├── InventoryApiService.kt
├── BillingApiService.kt
└── ... （サブドメインが増えるたびに肥大化）
```

#### 垂直スライスでの `core/network/` の役割

```
core/network/
├── SalesHttpClient.kt    ← OkHttpClient の設定（タイムアウト・証明書ピンニング等）
├── RetrofitFactory.kt    ← Retrofit インスタンスの生成
├── AuthInterceptor.kt    ← 認証ヘッダー付与（全 API 共通）
├── LoggingInterceptor.kt ← HTTPログ（全 API 共通）
└── di/
    └── NetworkModule.kt  ← OkHttp・Retrofit の DI 定義
```

**ドメイン固有の API インターフェースは各 `feature/*/data/` に置く。**

```kotlin
// core/network/RetrofitFactory.kt
// ドメイン知識ゼロ。「Retrofit のインスタンスを作る」だけ
object RetrofitFactory {
    fun create(
        httpClient: OkHttpClient,
        baseUrl: String,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(kotlinx.serialization.json.Json.asConverterFactory("application/json".toMediaType()))
        .build()
}
```

```kotlin
// core/network/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit は @Named で区別（サブドメインごとに BaseURL が違う場合）
    @Provides
    @Singleton
    @Named("sales-api")
    fun providesRetrofit(httpClient: OkHttpClient): Retrofit =
        RetrofitFactory.create(httpClient, BuildConfig.SALES_API_BASE_URL)
}
```

```kotlin
// feature/order/data/network/OrderApiService.kt  ← ここに置く
// 受注ドメイン専用の API インターフェース
interface OrderApiService {
    @GET("orders")
    suspend fun getOrders(
        @Query("ids") ids: List<String>? = null,
        @Query("after_version") afterVersion: Int? = null,
    ): OrderListResponse

    @PATCH("orders/{id}/status")
    suspend fun updateStatus(
        @Path("id") orderId: String,
        @Body body: UpdateStatusRequest,
    ): OrderResponse
}
```

#### 実務では

```
「全 API に X-Request-ID ヘッダーをつけたい」
→ core/network/AuthInterceptor.kt（または新規 RequestIdInterceptor.kt）だけ修正
→ 受注・顧客・在庫 API 全部に自動適用

「受注 API だけ別の BaseURL を使いたい」
→ feature/order/data/di/OrderNetworkModule.kt で @Named("order-api") Retrofit を作る
→ 他ドメインに影響ゼロ
```

---

## 5. `feature/*/data`

### Repository はなぜ feature に置くのか

#### 全体構造（受注ドメイン）

```
feature/order/data/
├── model/                         ← 受注ドメインのモデル（feature 内で完結）
│   ├── Order.kt                   ← ドメインモデル
│   ├── OrderItem.kt
│   └── OrderStatus.kt
├── network/
│   ├── OrderApiService.kt         ← Retrofit インターフェース（受注専用）
│   ├── model/
│   │   ├── NetworkOrder.kt        ← API レスポンスの型
│   │   └── NetworkOrder.ext.kt   ← NetworkOrder → Order 変換
│   └── di/
│       └── OrderNetworkModule.kt  ← OrderApiService の DI
├── OrderRepository.kt             ← interface（外部公開）
├── OfflineFirstOrderRepository.kt ← impl（internal）
├── OrderQuery.kt                  ← クエリパラメータ
└── di/
    └── OrderDataModule.kt         ← Repository の DI バインディング
```

#### ドメインモデルは feature の中に置く

```kotlin
// feature/order/data/model/Order.kt
// これは feature/order 専有のモデル。core/model には入れない
data class Order(
    val id: String,
    val customerId: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val totalAmount: Money,   // ← core/model/shared/Money.kt を参照（共有型はこれだけ）
    val deliveryDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// feature/order/data/model/OrderStatus.kt
enum class OrderStatus(val displayName: String) {
    PENDING("受注確認待ち"),
    CONFIRMED("確認済み"),
    PICKING("ピッキング中"),
    SHIPPED("出荷済み"),
    DELIVERED("配達完了"),
    CANCELLED("キャンセル"),
    ;
    
    fun canCancel(): Boolean = this in setOf(PENDING, CONFIRMED)
    fun canEdit(): Boolean = this == PENDING
}
```

#### Repository インターフェース

```kotlin
// feature/order/data/OrderRepository.kt
interface OrderRepository {
    // 一覧取得（Flow → DB更新で自動再取得）
    fun getOrders(query: OrderQuery = OrderQuery()): Flow<List<Order>>
    
    // 1件取得
    fun getOrder(orderId: String): Flow<Order?>
    
    // ステータス更新
    suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit>
    
    // 同期（WorkManager から呼ばれる）
    suspend fun sync(): Boolean
}

data class OrderQuery(
    val filterStatuses: Set<OrderStatus>? = null,
    val filterCustomerId: String? = null,
    val limit: Int = 0,
)
```

#### Repository 実装（Offline First）

```kotlin
// feature/order/data/OfflineFirstOrderRepository.kt
internal class OfflineFirstOrderRepository @Inject constructor(
    private val orderDao: OrderDao,                // ← core/database/order/
    private val orderApiService: OrderApiService,  // ← feature/order/data/network/
) : OrderRepository {

    // 読み取り：常に Room から返す（ネットワーク不要）
    override fun getOrders(query: OrderQuery): Flow<List<Order>> =
        orderDao.getOrders(
            useStatusFilter = query.filterStatuses != null,
            filterStatuses = query.filterStatuses?.map { it.name }?.toSet() ?: emptySet(),
            useCustomerFilter = query.filterCustomerId != null,
            customerId = query.filterCustomerId ?: "",
            limit = query.limit,
        ).map { entities -> entities.map(PopulatedOrder::asExternalModel) }

    override fun getOrder(orderId: String): Flow<Order?> =
        orderDao.getOrder(orderId).map { it?.asExternalModel() }

    // ステータス更新：楽観的更新パターン
    override suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit> {
        val original = orderDao.getOrder(orderId).first() ?: return Result.failure(
            IllegalStateException("Order $orderId not found")
        )
        // 即座にローカルを更新（UI が即反映）
        orderDao.upsertOrders(listOf(original.order.copy(status = status.name)))
        return try {
            orderApiService.updateStatus(orderId, UpdateStatusRequest(status.name))
            Result.success(Unit)
        } catch (e: IOException) {
            // 失敗したらロールバック
            orderDao.upsertOrders(listOf(original.order))
            Result.failure(e)
        }
    }

    // 同期：差分ベースで最小限のデータだけ取得
    override suspend fun sync(): Boolean = runCatching {
        val response = orderApiService.getOrders(afterVersion = getLastSyncVersion())
        orderDao.upsertOrders(response.orders.map { it.asEntity() })
        orderDao.upsertOrderItems(response.orders.flatMap { it.asItemEntities() })
        saveLastSyncVersion(response.version)
    }.isSuccess
}
```

#### DI バインディング

```kotlin
// feature/order/data/di/OrderDataModule.kt
@Module
@InstallIn(SingletonComponent::class)  // ← Repository はアプリ生存中ずっと同一インスタンス
abstract class OrderDataModule {

    @Binds
    internal abstract fun bindsOrderRepository(
        impl: OfflineFirstOrderRepository,
    ): OrderRepository
}

@Module
@InstallIn(SingletonComponent::class)
object OrderNetworkModule {

    @Provides
    @Singleton
    fun providesOrderApiService(
        @Named("sales-api") retrofit: Retrofit,
    ): OrderApiService = retrofit.create(OrderApiService::class.java)
}
```

#### 実務では

```
「受注の同期を WebSocket に切り替えたい」
→ WebSocketOrderRepository を新しく作り、OrderDataModule.kt のバインディングを差し替える
→ feature/order/domain/（UseCase）も feature/order/impl/（ViewModel）も変更ゼロ
→ 同様に、API の BaseURL 変更・認証方式変更もここだけ対応
```

---

## 6. `feature/*/domain`

### UseCase はなぜ feature に置くのか

#### 全体構造（受注ドメイン）

```
feature/order/domain/
├── GetOrdersUseCase.kt             ← 受注一覧取得（フィルタ・ソート含む）
├── GetOrderWithCustomerUseCase.kt  ← 受注 + 顧客名の合成（ドメインをまたぐ）
├── CreateOrderUseCase.kt           ← 受注作成のビジネスロジック
├── CanCancelOrderUseCase.kt        ← キャンセル可否判定
├── CanEditOrderUseCase.kt          ← 編集可否判定
└── ValidateOrderInputUseCase.kt    ← 受注入力のバリデーション
```

#### UseCase をここに置く理由

垂直スライスでは「受注に関するビジネスルール」は受注ドメインが所有する。  
NiA 流で `core/domain/` に置くと、全サブドメインのルールが混在して肥大化する。

```
// ❌ NiA 流：core/domain/ が全ドメインのルール置き場になる
core/domain/
├── GetOrdersUseCase.kt
├── GetCustomersUseCase.kt
├── GetInventoryUseCase.kt
├── CreateOrderUseCase.kt
├── ReserveStockUseCase.kt
... （サブドメインが増えるたびに肥大化）

// ✅ 垂直スライス：各ドメインが自分のルールを所有する
feature/order/domain/    ← 受注のルールは受注チームが管理
feature/customer/domain/ ← 顧客のルールは顧客チームが管理
feature/inventory/domain/← 在庫のルールは在庫チームが管理
```

#### 実装例：受注専用 UseCase 群

```kotlin
// feature/order/domain/GetOrdersUseCase.kt
// 単純な取得は UseCase を挟まず ViewModel から直接 Repository を呼んでもよいが、
// ソートや変換ロジックが増えたら UseCase に切り出す
class GetOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
) {
    operator fun invoke(
        statusFilter: Set<OrderStatus>? = null,
        sortBy: OrderSortField = OrderSortField.CREATED_AT_DESC,
    ): Flow<List<Order>> =
        orderRepository.getOrders(OrderQuery(filterStatuses = statusFilter))
            .map { orders ->
                when (sortBy) {
                    OrderSortField.CREATED_AT_DESC -> orders  // DBが既にソート済み
                    OrderSortField.AMOUNT_DESC -> orders.sortedByDescending { it.totalAmount.amountInYen }
                    OrderSortField.CUSTOMER_NAME -> orders  // 顧客名ソートは別UseCase
                }
            }
}

enum class OrderSortField { CREATED_AT_DESC, AMOUNT_DESC, CUSTOMER_NAME }
```

```kotlin
// feature/order/domain/GetOrderWithCustomerUseCase.kt
// 受注画面で顧客名を表示したい → 受注 + 顧客の合成
// 「受注ドメインが顧客ドメインを参照する」ユースケース
class GetOrderWithCustomerUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val customerLookup: CustomerLookupContract,  // ← customer/api/ で定義されたインターフェース
) {
    operator fun invoke(orderId: String): Flow<OrderWithCustomer?> =
        orderRepository.getOrder(orderId)
            .combine(
                customerLookup.getCustomerSummaries()
            ) { order, customerSummaries ->
                if (order == null) return@combine null
                val customer = customerSummaries.find { it.id == order.customerId }
                OrderWithCustomer(
                    order = order,
                    customerName = customer?.displayName ?: "不明",
                    customerPhone = customer?.phone,
                )
            }
}

// 合成後のモデル（feature/order/domain/ に置く。core/model には入れない）
data class OrderWithCustomer(
    val order: Order,
    val customerName: String,
    val customerPhone: String?,
)
```

```kotlin
// feature/order/domain/CanCancelOrderUseCase.kt
// ビジネスルール：「いつキャンセルできるか」
// これが UseCase にあることで、ルール変更が1箇所で済む
class CanCancelOrderUseCase @Inject constructor() {
    operator fun invoke(order: Order): Boolean = order.status.canCancel()
    // OrderStatus.canCancel() に委譲（ドメインモデル自身がルールを持つ場合もある）
}
```

```kotlin
// feature/order/domain/CreateOrderUseCase.kt
// 受注作成：在庫確認 → 受注登録 → 在庫引当 の複合処理
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val stockReservation: StockReservationContract, // ← inventory/api/ のインターフェース
    private val validateInput: ValidateOrderInputUseCase,
) {
    suspend operator fun invoke(input: CreateOrderInput): Result<Order> {
        // 1. 入力バリデーション
        val validationResult = validateInput(input)
        if (validationResult is ValidationResult.Invalid) {
            return Result.failure(ValidationException(validationResult.errors))
        }

        // 2. 在庫確認（inventory ドメインに委譲）
        val stockCheck = stockReservation.checkAvailability(
            input.items.map { StockQuery(it.productId, it.quantity) }
        )
        if (!stockCheck.isAvailable) {
            return Result.failure(InsufficientStockException(stockCheck.unavailableItems))
        }

        // 3. 受注登録
        val order = orderRepository.createOrder(input.toOrder())

        // 4. 在庫引当
        stockReservation.reserve(order.id, input.items.map { ReservationItem(it.productId, it.quantity) })

        return Result.success(order)
    }
}
```

#### UseCase のテスト

```kotlin
// feature/order/domain/test/CanCancelOrderUseCaseTest.kt
// DB・Network・Android 不要。純粋 Kotlin テスト
class CanCancelOrderUseCaseTest {
    private val useCase = CanCancelOrderUseCase()

    @Test fun `PENDING状態はキャンセル可能`() {
        assertThat(useCase(testOrder(status = OrderStatus.PENDING))).isTrue()
    }

    @Test fun `SHIPPED状態はキャンセル不可`() {
        assertThat(useCase(testOrder(status = OrderStatus.SHIPPED))).isFalse()
    }
}
```

#### 実務では

```
「出荷済みでも24時間以内ならキャンセル可能にしてほしい」という仕様変更
→ feature/order/domain/CanCancelOrderUseCase.kt（または OrderStatus.canCancel()）を修正
→ 受注一覧・受注詳細・通知バナーの3画面に自動で反映
→ テストを追加して意図をドキュメント化
```

---

## 7. `feature/*/impl`

### ViewModel と UI

#### 全体構造（受注ドメイン）

```
feature/order/impl/
├── list/
│   ├── OrderListViewModel.kt      ← 一覧画面の状態管理
│   ├── OrderListScreen.kt         ← 一覧画面 UI
│   └── OrderListUiState.kt        ← UI状態の型定義
├── detail/
│   ├── OrderDetailViewModel.kt    ← 詳細画面の状態管理
│   └── OrderDetailScreen.kt       ← 詳細画面 UI
└── navigation/
    └── OrderEntryProvider.kt       ← NavDisplay への登録（navigation3）
```

#### ViewModel の責務：3つだけ

```kotlin
// feature/order/impl/list/OrderListViewModel.kt
@HiltViewModel
class OrderListViewModel @Inject constructor(
    // ビジネスロジックは UseCase に委譲（ViewModel は持たない）
    getOrders: GetOrdersUseCase,
    private val canCancelOrder: CanCancelOrderUseCase,
    private val orderRepository: OrderRepository,  // 書き込みは直接 Repository を呼ぶ
    private val analyticsHelper: AnalyticsHelper,
) : ViewModel() {

    // ── 責務1：UI状態の管理 ─────────────────────────────────
    private val _statusFilter = MutableStateFlow<Set<OrderStatus>?>(null)

    val uiState: StateFlow<OrderListUiState> =
        _statusFilter
            .flatMapLatest { filter -> getOrders(statusFilter = filter) }
            .map { orders ->
                OrderListUiState.Success(
                    orders = orders.map { order ->
                        OrderListItem(order = order, canCancel = canCancelOrder(order))
                    },
                    activeFilter = _statusFilter.value,
                )
            }
            .catch { emit(OrderListUiState.Error(it.message ?: "エラーが発生しました")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OrderListUiState.Loading,
            )

    // ── 責務2：ユーザーアクションの処理 ─────────────────────
    fun onStatusFilterChanged(statuses: Set<OrderStatus>?) {
        _statusFilter.value = statuses
        analyticsHelper.logEvent(AnalyticsEvent("order_filter_changed"))
    }

    fun onCancelOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.updateStatus(orderId, OrderStatus.CANCELLED)
                .onFailure { /* エラー表示のための状態更新 */ }
        }
    }

    // ── 責務3：画面回転をまたいだ状態保持（SavedStateHandle） ─
    // StateFlow 自体が viewModelScope に紐付くので自動保持される
}
```

```kotlin
// feature/order/impl/list/OrderListUiState.kt
sealed interface OrderListUiState {
    data object Loading : OrderListUiState
    data class Success(
        val orders: List<OrderListItem>,
        val activeFilter: Set<OrderStatus>?,
    ) : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}

data class OrderListItem(
    val order: Order,
    val canCancel: Boolean,
)
```

#### UI：「状態を見せるだけ」の分離

```kotlin
// feature/order/impl/list/OrderListScreen.kt
@Composable
fun OrderListScreen(
    onOrderClick: (orderId: String) -> Unit,  // ← ナビゲーションは親が注入
    onBackClick: () -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ViewModel とのつなぎ役（薄いラッパー）
    OrderListContent(
        uiState = uiState,
        onStatusFilterChanged = viewModel::onStatusFilterChanged,
        onCancelOrder = viewModel::onCancelOrder,
        onOrderClick = onOrderClick,
    )
}

// hiltViewModel() に依存しない純粋なコンポーザブル
// → Preview・UI テスト・Storybook 的な用途に使える
@Composable
internal fun OrderListContent(
    uiState: OrderListUiState,
    onStatusFilterChanged: (Set<OrderStatus>?) -> Unit,
    onCancelOrder: (String) -> Unit,
    onOrderClick: (String) -> Unit,
) {
    when (uiState) {
        is OrderListUiState.Loading -> OrderListSkeleton()
        is OrderListUiState.Error   -> ErrorMessage(uiState.message)
        is OrderListUiState.Success -> {
            Column {
                OrderStatusFilterRow(
                    activeFilter = uiState.activeFilter,
                    onFilterChanged = onStatusFilterChanged,
                )
                LazyColumn {
                    items(uiState.orders, key = { it.order.id }) { item ->
                        OrderListItemRow(
                            item = item,
                            onClick = { onOrderClick(item.order.id) },
                            onCancel = if (item.canCancel) {
                                { onCancelOrder(item.order.id) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

// Preview は状態を直接渡すだけ（ViewModel 不要）
@Preview(name = "受注一覧 - 通常")
@Composable
private fun OrderListContentPreview() {
    SalesTheme {
        OrderListContent(
            uiState = OrderListUiState.Success(
                orders = listOf(
                    OrderListItem(previewOrder(status = OrderStatus.PENDING), canCancel = true),
                    OrderListItem(previewOrder(status = OrderStatus.SHIPPED), canCancel = false),
                ),
                activeFilter = null,
            ),
            onStatusFilterChanged = {},
            onCancelOrder = {},
            onOrderClick = {},
        )
    }
}
```

#### UI テスト

```kotlin
// feature/order/impl/test/OrderListScreenTest.kt
@RunWith(AndroidJUnit4::class)
class OrderListScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `キャンセル可能な受注にはキャンセルボタンが表示される`() {
        composeTestRule.setContent {
            OrderListContent(
                uiState = OrderListUiState.Success(
                    orders = listOf(OrderListItem(pendingOrder, canCancel = true)),
                    activeFilter = null,
                ),
                onStatusFilterChanged = {}, onCancelOrder = {}, onOrderClick = {},
            )
        }
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()
    }

    @Test
    fun `キャンセル不可の受注にはキャンセルボタンが非表示`() {
        composeTestRule.setContent {
            OrderListContent(
                uiState = OrderListUiState.Success(
                    orders = listOf(OrderListItem(shippedOrder, canCancel = false)),
                    activeFilter = null,
                ),
                onStatusFilterChanged = {}, onCancelOrder = {}, onOrderClick = {},
            )
        }
        composeTestRule.onNodeWithText("キャンセル").assertDoesNotExist()
    }
}
```

---

## 8. `feature/*/api`

### 外部公開の境界線

#### 問題：feature 間でナビゲーションするための循環依存

```
// ❌ api/impl 分割なしの場合
feature/order/impl が feature/customer/impl に依存
feature/customer/impl も feature/order/impl に依存したい
→ 循環依存 → Gradle ビルドエラー
```

#### 受注ドメインの api モジュール

```
feature/order/api/
└── navigation/
    ├── OrderListNavKey.kt          ← 一覧画面へのナビゲーションキー
    └── OrderDetailNavKey.kt        ← 詳細画面へのナビゲーションキー
```

```kotlin
// feature/order/api/navigation/OrderListNavKey.kt
@Serializable
data object OrderListNavKey : NavKey  // パラメータなし（一覧はどこからでも遷移可）

// feature/order/api/navigation/OrderDetailNavKey.kt
@Serializable
data class OrderDetailNavKey(
    val orderId: String,
) : NavKey

// Navigator 拡張関数（他ドメインが受注詳細に遷移するための便利関数）
fun Navigator.navigateToOrderDetail(orderId: String) {
    navigate(OrderDetailNavKey(orderId))
}
```

#### 顧客ドメインの api モジュール（外部公開型も含む）

```
feature/customer/api/
├── navigation/
│   └── CustomerDetailNavKey.kt   ← NavKey + Navigator 拡張関数
├── CustomerSummary.kt          ← 他ドメインが顧客を参照するための軽量型
└── CustomerLookupContract.kt   ← 他ドメインが顧客を検索するためのインターフェース
```

```kotlin
// feature/customer/api/navigation/CustomerDetailNavKey.kt
@Serializable
data class CustomerDetailNavKey(
    val customerId: String,
) : NavKey

// Navigator 拡張関数（受注ドメインなどが顧客詳細に遷移するとき使う）
fun Navigator.navigateToCustomerDetail(customerId: String) {
    navigate(CustomerDetailNavKey(customerId))
}
```

```kotlin
// feature/customer/api/CustomerSummary.kt
// 他ドメインが顧客を参照するときは、この型だけを使う
// 顧客ドメイン内部の詳細（ポイント、会員ランク等）は公開しない
data class CustomerSummary(
    val id: String,
    val displayName: String,
    val phone: String?,
)

// feature/customer/api/CustomerLookupContract.kt
// 受注ドメインなど他ドメインが「顧客を検索したい」ときに使うインターフェース
// 実装は feature/customer/impl/ の中にある（他ドメインは実装詳細を知らない）
interface CustomerLookupContract {
    fun getCustomerSummaries(): Flow<List<CustomerSummary>>
    suspend fun getCustomerSummary(customerId: String): CustomerSummary?
}
```

```kotlin
// feature/customer/impl/ の中で実装
internal class CustomerLookupImpl @Inject constructor(
    private val customerRepository: CustomerRepository,
) : CustomerLookupContract {
    override fun getCustomerSummaries(): Flow<List<CustomerSummary>> =
        customerRepository.getCustomers().map { customers ->
            customers.map { CustomerSummary(it.id, it.name, it.phone) }
        }
}
```

#### app/ での NavDisplay 組み立て（navigation3）

NiA と同じ **navigation3** パターンを使い、`NavDisplay` + `entryProvider` で画面遷移を管理する。

```kotlin
// core/navigation/Navigator.kt（NiA と同じパターン）
// 各ドメインのナビゲーションを管理する
class Navigator(val state: NavigationState) {
    fun navigate(key: NavKey) { ... }
    fun goBack() { ... }
}
```

```kotlin
// feature/order/impl/navigation/OrderEntryProvider.kt
fun EntryProviderScope<NavKey>.orderEntry(navigator: Navigator) {
    entry<OrderListNavKey> {
        OrderListScreen(
            onOrderClick = { orderId -> navigator.navigate(OrderDetailNavKey(orderId)) },
            onCustomerClick = { customerId -> navigator.navigateToCustomerDetail(customerId) },
        )
    }
    entry<OrderDetailNavKey> { key ->
        OrderDetailScreen(
            orderId = key.orderId,
            onBackClick = { navigator.goBack() },
        )
    }
}
```

```kotlin
// feature/customer/impl/navigation/CustomerEntryProvider.kt
fun EntryProviderScope<NavKey>.customerEntry(navigator: Navigator) {
    entry<CustomerDetailNavKey> { key ->
        CustomerDetailScreen(
            customerId = key.customerId,
            onBackClick = { navigator.goBack() },
        )
    }
}
```

```kotlin
// app/src/main/.../SalesApp.kt
@Composable
fun SalesApp(appState: SalesAppState) {
    val navigator = remember { Navigator(appState.navigationState) }

    // 全ドメインの entry を登録
    val entryProvider = entryProvider {
        orderEntry(navigator)
        customerEntry(navigator)
        inventoryEntry(navigator)
    }

    // NavDisplay が現在のキーに対応する Composable を表示
    NavDisplay(
        entries = appState.navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
    )
}
```

> **ポイント：**  
> - `NavHost` + `NavController` ではなく、`NavDisplay` + `Navigator` を使う  
> - 各 feature は `EntryProviderScope<NavKey>.xxxEntry(navigator)` を公開する  
> - `entry<Key> { key -> ... }` で NavKey のパラメータに型安全にアクセスできる  
> - NiA の `TopicNavKey` → `navigator::navigateToTopic` と同じパターンで拡張関数を定義

#### ビルド速度への効果

```
// feature/customer/impl を修正（UI を大幅リニューアル）
// → feature/customer/api は変わっていない
// → feature/order/impl は feature/customer/api に依存しているが、変わっていないのでスキップ
// → feature/order/impl の再ビルドが不要になる

// モジュール数が10超えると、このスキップが積み重なって
// フルビルド時間が数分 → 数十秒に短縮されることも
```

---

## 9. `feature/shared`

### 「意図的な共通化」とは何か

#### Wrong Abstraction vs 意図的な共通化

```kotlin
// ❌ Wrong Abstraction（偶発的な共通化）
// 受注チームと顧客チームが「たまたま同じ実装」を共通化
// → 半年後に if 分岐だらけになる

// feature/shared/GetNameUseCase.kt（危険な例）
class GetNameUseCase @Inject constructor() {
    operator fun invoke(
        id: String,
        nameType: NameType = NameType.DISPLAY,  // 顧客チームの要望で追加
        useBillingName: Boolean = false,         // 受注チームの要望で追加
        fallbackToCode: Boolean = true,          // いつの間にか増えた
    ): Flow<String> { ... }
}
```

```kotlin
// ✅ 意図的な共通化（変更理由が同じものだけ共通化）
// 「認証」は全サブドメインが同じルールで必要とする → shared に置いてよい

// feature/shared/auth/
// ├── AuthRepository.kt
// └── GetCurrentUserUseCase.kt

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> = authRepository.currentUser
    // 「誰がログインしているか」は全ドメインで同じ意味 → 共通化してよい
}
```

#### feature/shared の構造

```
feature/shared/
├── auth/                      ← 認証（全サブドメインが同じルールで使う）
│   ├── AuthRepository.kt      ← interface
│   ├── AuthRepositoryImpl.kt  ← impl
│   ├── AuthUser.kt            ← 認証ユーザーの型
│   ├── GetCurrentUserUseCase.kt
│   └── di/AuthModule.kt
│
├── notification/              ← 通知送信（受注確定・在庫切れ・配送完了で使う）
│   ├── NotificationSender.kt  ← interface
│   ├── OrderNotification.kt   ← 受注関連通知の型定義
│   └── di/NotificationModule.kt
│
└── sync/                      ← 同期管理（全ドメインの WorkManager 同期を調整）
    ├── SyncManager.kt
    └── SyncWorker.kt
```

```kotlin
// feature/shared/notification/NotificationSender.kt
// 「通知を送る」という行為は複数ドメインで必要
// ただし「どんな通知を送るか」はドメインごとに異なる → 型で区別

interface NotificationSender {
    suspend fun send(notification: SalesNotification)
}

sealed interface SalesNotification {
    data class OrderConfirmed(val orderId: String, val customerName: String) : SalesNotification
    data class StockShortage(val productId: String, val currentStock: Int) : SalesNotification
    data class ShipmentCompleted(val orderId: String, val trackingNumber: String) : SalesNotification
}
```

#### 共通化の判断基準

```
新しいコードを書くとき：

Q1: 複数のサブドメインで使われるか？
 ├─ No  → feature/{subdomain}/domain/ に書く
 └─ Yes → Q2へ

Q2: 変更理由が常に同時に発生するか？（= 同じビジネスルールを共有しているか？）
 ├─ No  → 各ドメインに重複して書く（偶発的な共通化は避ける）
 └─ Yes → Q3へ

Q3: ドメイン知識を含むか？
 ├─ No  → core/（インフラ）に置く
 └─ Yes → feature/shared/ に置く
```

---

## 10. Hilt DI

### どこで何を bind するか

#### モジュールの全体マップ

```
SingletonComponent（アプリ生存中ずっと同一インスタンス）
├── core/database/di/DatabaseModule        ← Room DB, DAO の provide
├── core/network/di/NetworkModule          ← OkHttp, Retrofit の provide
├── feature/order/data/di/OrderDataModule  ← OrderRepository の bind
├── feature/order/data/di/OrderNetworkModule ← OrderApiService の provide
├── feature/customer/data/di/CustomerDataModule
├── feature/shared/auth/di/AuthModule
└── ...

ViewModelComponent（ViewModel が生存中）
└── 基本的に @HiltViewModel + @Inject constructor で自動解決
```

```kotlin
// feature/order/data/di/OrderDataModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class OrderDataModule {

    // interface → impl のバインディング
    @Binds
    internal abstract fun bindsOrderRepository(
        impl: OfflineFirstOrderRepository,
    ): OrderRepository
}
```

```kotlin
// feature/customer/data/di/CustomerDataModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class CustomerDataModule {

    // CustomerLookupContract の実装を顧客チームが提供
    // 受注チームは CustomerLookupContract という interface しか知らない
    @Binds
    internal abstract fun bindsCustomerLookup(
        impl: CustomerLookupImpl,  // feature/customer/impl/ に実装がある
    ): CustomerLookupContract
}
```

#### テスト時の差し替え

```kotlin
// feature/order/impl/test/OrderListViewModelTest.kt
@HiltAndroidTest
class OrderListViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    // ← 本物の Repository を Fake に差し替える
    @BindValue
    val orderRepository: OrderRepository = FakeOrderRepository()

    @BindValue
    val customerLookup: CustomerLookupContract = FakeCustomerLookup()

    @Inject
    lateinit var getOrdersUseCase: GetOrdersUseCase

    @Test
    fun `受注一覧が取得できる`() = runTest {
        (orderRepository as FakeOrderRepository).emit(listOf(testOrder()))
        val viewModel = OrderListViewModel(getOrdersUseCase, CanCancelOrderUseCase(), orderRepository)
        
        assertThat(viewModel.uiState.value).isInstanceOf(OrderListUiState.Success::class.java)
    }
}
```

---

## 11. Flow / StateFlow

### なぜ必要か

#### DB 更新 → UI 自動反映の仕組み

```kotlin
// 受注をキャンセルした瞬間に一覧が自動更新される流れ

// 1. UI がボタン押下をキャッチ
//    OrderListScreen → onCancelOrder(orderId)

// 2. ViewModel がアクションを処理
fun onCancelOrder(orderId: String) {
    viewModelScope.launch {
        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED)
        //              ↑ ここを呼ぶだけ
    }
}

// 3. Repository が Room を更新
orderDao.upsertOrders(listOf(entity.copy(status = "CANCELLED")))
//                    ↑ Room がこの書き込みを検知する

// 4. Room が Flow に変更を自動通知
// orderDao.getOrders() の Flow が新しいデータを emit

// 5. UseCase / ViewModel の Flow が自動再計算
val uiState = getOrders()
    .map { orders -> OrderListUiState.Success(orders.map { ... }) }
    .stateIn(...)
//  ↑ 自動で再計算される

// 6. Compose が変更を検知して再コンポーズ
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//                               ↑ 変更を自動検知して UI 更新

// → 「データを取り直す」コードは一切不要
```

#### なぜ `stateIn` が必要か

```kotlin
// Flow のまま公開すると...
val orders: Flow<List<Order>> = getOrders()
// → OrderListScreen と OrderDetailScreen の2画面が collect
// → DB クエリが2回走る（無駄）
// → 画面回転時に最新値がなく、Loading 状態が再表示される

// StateFlow に変換すると...
val uiState: StateFlow<OrderListUiState> = getOrders()
    .map { ... }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // ↑ 全 Composable が離脱後5秒でリソース解放（画面回転はこの間に完了）
        initialValue = OrderListUiState.Loading,
        // ↑ 最初の値（ちらつき防止）
    )
// → 購読者が何人いても DB クエリは1回
// → 画面回転しても5秒以内なら最新値を即座に返す
// → バックグラウンド遷移から戻っても即座に表示
```

---

## 12. 全体の依存フローを追う

### 受注一覧画面が表示されるまで

```
[ユーザーが受注一覧画面を開く]
        │
        ▼
app/SalesApp の NavDisplay
    → NavigationState の currentKey が OrderListNavKey
    → entryProvider 内の orderEntry() が entry<OrderListNavKey> をマッチ
        │
        ▼
feature/order/impl/OrderListScreen
    ← hiltViewModel() で OrderListViewModel を DI
        │
        ▼
feature/order/impl/OrderListViewModel
    @Inject constructor:
    ├── GetOrdersUseCase (feature/order/domain/)
    │       └── OrderRepository (feature/order/data/) [interface]
    │               └── OfflineFirstOrderRepository [impl, internal]
    │                       ├── OrderDao (core/database/order/) ← Room が実装生成
    │                       │       └── SalesDatabase (core/database/)
    │                       └── OrderApiService (feature/order/data/network/) ← Retrofit
    ├── CanCancelOrderUseCase (feature/order/domain/)
    │       └── （外部依存なし・純粋ロジック）
    ├── OrderRepository（書き込み用に直接注入）
    └── AnalyticsHelper (core/analytics/)
```

### 受注作成（複数ドメインをまたぐ）

```
[ユーザーが「受注確定」ボタンを押す]
        │
        ▼
feature/order/impl/OrderCreateViewModel.onConfirm()
        │
        ▼
feature/order/domain/CreateOrderUseCase
    ├── feature/order/data/OrderRepository  ← 受注を保存
    ├── feature/inventory/api/StockReservationContract ← 在庫引当（インターフェース経由）
    │       └── feature/inventory/impl/StockReservationImpl  ← 実装は inventory が持つ
    └── feature/order/domain/ValidateOrderInputUseCase

→ 受注ドメインは在庫ドメインの内部実装を知らない
→ StockReservationContract（api モジュール）だけを知っている
```

---

## まとめ：各ファイルが「なぜ存在するか」一覧

| 場所 | 役割 | なぜここか |
|------|------|----------|
| `core/model/shared/*.kt` | サブドメインをまたぐ共有型（Money, Address等） | 「同じビジネス意味」を持つ型だけ。型の乱立を防ぐ |
| `core/database/{domain}/*Entity.kt` | DBテーブル行の型 | Room の @Database が全 Entity をコンパイル時に参照するため core に集約 |
| `core/database/{domain}/*Dao.kt` | SQL 操作の宣言 | Room がコンパイル時に実装生成。テスト時に Fake 差し替え可 |
| `core/database/SalesDatabase.kt` | Room DB インスタンス管理 | アプリで1インスタンス。Singleton で管理 |
| `core/network/` | HTTP クライアント設定のみ | ドメイン知識ゼロ。共通の認証・ロギング・タイムアウト設定だけ |
| `feature/{domain}/data/model/*.kt` | サブドメイン専用のドメインモデル | ここが「本物の型」。core/model は本当に共有するものだけ |
| `feature/{domain}/data/*Repository.kt` | データ取得の抽象（interface） | ViewModel が DB/Network の詳細を知らなくてよい |
| `feature/{domain}/data/Offline*.kt` | Offline First の実装（internal） | 実装詳細を隠蔽。差し替えても呼び出し側変更ゼロ |
| `feature/{domain}/data/di/*.kt` | interface → impl の DI バインディング | テスト時に Fake を注入できる仕組み |
| `feature/{domain}/domain/*UseCase.kt` | サブドメイン専用ビジネスルール | ルール変更が1箇所で済む。純粋 Kotlin テストが書ける |
| `feature/{domain}/impl/*ViewModel.kt` | UI 状態管理・アクション処理 | 画面回転をまたいで状態保持。UseCase に委譲して薄く保つ |
| `feature/{domain}/impl/*Screen.kt` | Composable UI（Screen + Content の2層） | Screen は hiltViewModel と薄くつなぐ。Content は状態を見せるだけ |
| `feature/{domain}/impl/navigation/*EntryProvider.kt` | navigation3 の entry 登録 | `EntryProviderScope<NavKey>.xxxEntry(navigator)` で NavDisplay に画面を登録 |
| `feature/{domain}/api/navigation/*NavKey.kt` | 型安全なナビゲーションキー（`: NavKey`） | 他ドメインが impl に依存せずに遷移先を参照できる |
| `feature/{domain}/api/navigation/*.kt` | Navigator 拡張関数 | `navigator.navigateToXxx()` で型安全に遷移 |
| `feature/{domain}/api/*Contract.kt` | ドメイン間インターフェース | 受注が在庫を呼ぶとき、実装詳細を知らずに済む |
| `feature/shared/{concern}/` | 意図的な横断共通機能 | 「変更理由が同じ」ものだけ。偶発的な共通化は避ける |

---

## 13. 「一見共通な型」と「サブドメイン内共通」の設計判断

### 設計の根本原則

> **「今同じに見える」は設計の根拠にならない。**
> **「変更理由が同時に発生するか」が根拠になる。**

スキーマが同じでも、ビジネスの意味が違えば別の型。
実装が重複していても、目的が違えば共通化しない。

---

### ケース1：受注・受注キャンセル・受注クレームの共通コード

#### 結論：`feature/shared/` には置かない

受注・キャンセル・クレームは全て「受注サブドメイン」の内部事情。
`feature/shared/` は **複数サブドメインを跨ぐ** 場合のみ使う。
同じサブドメイン内の共通化は `feature/order/` の中だけで完結させる。

#### 規模に応じた3段階の整理方法

**段階1：小規模（UseCase 10個以下）── フラットに並べる**

```
feature/order/domain/
├── GetOrdersUseCase.kt         ← 受注一覧取得
├── GetOrderDetailUseCase.kt    ← 受注詳細取得
├── CanCancelOrderUseCase.kt    ← キャンセル可否判定
├── CancelOrderUseCase.kt       ← キャンセル実行
├── CreateClaimUseCase.kt       ← クレーム起票
└── ResolveClaimUseCase.kt      ← クレーム解決
```

共通ロジック（「受注が有効か？」など）はドメインモデルのメソッドとして表現。

```kotlin
// feature/order/data/model/Order.kt
data class Order(
    val id: String,
    val status: OrderStatus,
    val claimStatus: ClaimStatus?,
    val shippedAt: Instant?,
) {
    // 受注・キャンセル・クレームの全 UseCase が参照する共通ルール
    fun canCancel(): Boolean =
        status == OrderStatus.PENDING || status == OrderStatus.PROCESSING

    fun canClaim(): Boolean =
        status == OrderStatus.DELIVERED

    fun isActive(): Boolean =
        status != OrderStatus.CANCELLED && status != OrderStatus.REFUNDED
}
```

**段階2：中規模（UseCase 20〜30個 / チーム3名以上）── サブパッケージで整理**

```
feature/order/domain/
├── query/                     ← 参照系（読み取り専用）
│   ├── GetOrdersUseCase.kt
│   └── GetOrderDetailUseCase.kt
├── cancel/                    ← キャンセルフロー
│   ├── CanCancelOrderUseCase.kt
│   └── CancelOrderUseCase.kt
└── claim/                     ← クレームフロー
    ├── CreateClaimUseCase.kt
    └── ResolveClaimUseCase.kt
```

UseCase 同士の依存はなし（cancel と claim は互いを知らない）。
両方とも `OrderRepository` に依存するだけ。

**段階3：大規模（チームが別れる / ビルド時間が問題）── Gradle モジュール分割**

```
feature/
├── order-base/               ← Order モデル + OrderRepository（全チームが依存）
│   ├── data/
│   └── api/                  ← OrderSummary など外部公開型
│
├── order-list/               ← 一覧・詳細チームが所有
│   └── impl/                 ← GetOrdersUseCase + OrderListScreen
│
├── order-cancel/             ← キャンセルチームが所有
│   ├── domain/               ← CancelOrderUseCase（order-base に依存）
│   └── impl/
│
└── order-claim/              ← クレーム対応チームが所有
    ├── domain/               ← CreateClaimUseCase（order-base に依存）
    └── impl/
```

`order-cancel` と `order-claim` は**互いに依存しない**。
どちらも `order-base` だけに依存するツリー構造。

```
order-base ←── order-list
           ←── order-cancel   （order-list を知らない）
           ←── order-claim    （order-cancel を知らない）
```

これにより `order-cancel` を修正しても `order-claim` の再ビルドは不要。

#### 判断フロー

```
「受注ドメイン内の共通ロジック」をどこに置くか
        │
        ▼
UseCase の総数 / チーム規模は？
 ├─ ~10個 / 1〜2名  → feature/order/domain/ にフラット
 ├─ ~30個 / 3〜5名  → feature/order/domain/{query,cancel,claim}/
 └─ 30個+ / 複数チーム → feature/order-base/ + feature/order-cancel/ 等に Gradle 分割
                          ↑ このとき初めて feature/shared/ への配置を検討する
                            （もし cancel と claim で本当に共通な「受注外」ロジックが出たら）
```

---

### ケース2：Money ── 「形が同じだが目的が違う型」

#### なぜ単純に共通化してはいけないか

```kotlin
// ❌ 今は綺麗に見える
// core/model/Money.kt
data class Money(val amount: Long, val currencyCode: String)
```

3年後の現実：

```kotlin
// 販売チームの追加要望
data class Money(
    val amount: Long,
    val currencyCode: String,
    val discountRate: Double?,       // 「割引率を持ちたい」
    val promotionLabel: String?,     // 「キャンペーン表示名を持ちたい」
    val taxIncluded: Boolean,        // 「税込みフラグが必要」
)

// 会計チームの追加要望
data class Money(
    val amount: BigDecimal,          // 「Long では精度が足りない」
    val currencyCode: String,
    val taxCategory: TaxCategory,    // 「消費税区分が必要」
    val roundingMode: RoundingMode,  // 「端数処理方法が違う」
    val accountingDate: LocalDate,   // 「計上日を持ちたい」
)

// → 両方の要望を入れると、どちらにとっても使いにくい型になる
// → 片方の変更がもう片方のレビューを必要とする
// → Money を修正するたびに全ドメインのテストが落ちる
```

#### 3つのパターンと使い分け

---

**パターン A：最小共通型（raw data）+ ドメイン固有の拡張**

使うとき：
- 複数ドメインが同じ DB カラム / API フィールドを読む
- 変換・計算ロジックはドメインごとに独立している

```kotlin
// core/model/Money.kt  ← 「値」だけ。振る舞いなし
// Long にする理由：シリアライズが安全、比較が単純、精度は最小通貨単位で担保
data class Money(
    val amountMinorUnit: Long,   // 最小通貨単位（円 = 1円単位、ドル = 1セント単位）
    val currencyCode: String,    // ISO 4217（"JPY", "USD"）
)
// ← ここには applyDiscount() も toDisplayString() も addTax() も書かない
```

```kotlin
// feature/order/data/model/SalesPriceExt.kt  ← 販売ドメインの拡張
fun Money.applyDiscount(rate: Double): Money =
    copy(amountMinorUnit = (amountMinorUnit * (1.0 - rate)).toLong())

fun Money.toDisplayString(): String = when (currencyCode) {
    "JPY" -> "¥${amountMinorUnit}"
    "USD" -> "$${amountMinorUnit / 100}.${(amountMinorUnit % 100).toString().padStart(2, '0')}"
    else  -> "${amountMinorUnit} $currencyCode"
}
```

```kotlin
// feature/billing/data/model/AccountingAmountExt.kt  ← 会計ドメインの拡張
val Money.asBigDecimal: BigDecimal
    get() = BigDecimal(amountMinorUnit).scaleByPowerOfTen(-2)  // JPY以外は要調整

fun Money.toAccountingEntry(
    taxCategory: TaxCategory,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
): AccountingEntry = AccountingEntry(
    amount = this,
    taxCategory = taxCategory,
    roundingMode = roundingMode,
)
```

適する状況：
```
DB カラム: orders.price_minor_unit = 10000, currency_code = "JPY"
          ↓ 読み取り（全ドメイン共通）
Money(amountMinorUnit = 10000, currencyCode = "JPY")
          ↓ ドメインごとに変換
販売 : "¥10,000" の表示文字列
会計 : BigDecimal(10000) + 消費税 10% = AccountingEntry
```

---

**パターン B：最初から別々の型**

使うとき：
- 型の意味が根本的に異なる（同じ「金額」でも目的が違う）
- 将来的にほぼ確実に乖離する（販売 vs 会計はその典型）

```kotlin
// feature/order/data/model/SalesPrice.kt  ← 販売価格
data class SalesPrice(
    val listPrice: Long,          // 定価（円）
    val salePrice: Long,          // 実売価格（円）
    val currencyCode: String,
    val taxIncluded: Boolean,
) {
    val discountAmount: Long get() = listPrice - salePrice
    val discountRate: Double get() = 1.0 - salePrice.toDouble() / listPrice
}
```

```kotlin
// feature/billing/data/model/AccountingAmount.kt  ← 会計金額
data class AccountingAmount(
    val amount: BigDecimal,
    val currencyCode: String,
    val taxCategory: TaxCategory,   // 課税 / 非課税 / 軽減税率
    val roundingMode: RoundingMode,
) {
    fun addTax(rate: BigDecimal): AccountingAmount = copy(
        amount = (amount * (BigDecimal.ONE + rate)).setScale(0, roundingMode)
    )
}
```

変換が必要な場合はマッパーで明示する：

```kotlin
// feature/billing/data/mapper/OrderPriceMapper.kt
// 受注確定 → 請求書作成のタイミングで「変換」という行為を明示
fun SalesPrice.toAccountingAmount(taxCategory: TaxCategory): AccountingAmount =
    AccountingAmount(
        amount = BigDecimal(salePrice),
        currencyCode = currencyCode,
        taxCategory = taxCategory,
        roundingMode = RoundingMode.HALF_UP,
    )
// ← この変換コードは billing ドメインが所有する（order ではない）
// ← 「受注金額をどう解釈して会計に使うか」は会計ドメインの責任
```

---

**パターン C：DTO（転送用）は共通、ドメインモデルは別々**

使うとき：
- API レスポンスの型（NetworkMoney）は共通にしたい
- でもビジネスロジックが乗るドメインモデルは別々にしたい

```kotlin
// core/network/model/NetworkMoney.kt  ← API の形を表すだけ（ドメイン知識なし）
@Serializable
data class NetworkMoney(
    @SerialName("amount") val amount: Long,
    @SerialName("currency") val currency: String,
)
```

```kotlin
// feature/order/data/network/model/NetworkOrder.ext.kt  ← 販売ドメインの解釈
fun NetworkMoney.toSalesPrice(listPrice: NetworkMoney?): SalesPrice = SalesPrice(
    listPrice = listPrice?.amount ?: amount,
    salePrice = amount,
    currencyCode = currency,
    taxIncluded = false,   // 受注 API は税抜き仕様
)

// feature/billing/data/network/model/NetworkInvoice.ext.kt  ← 会計ドメインの解釈
fun NetworkMoney.toAccountingAmount(taxCategory: TaxCategory): AccountingAmount =
    AccountingAmount(
        amount = BigDecimal(amount),
        currencyCode = currency,
        taxCategory = taxCategory,
        roundingMode = RoundingMode.HALF_UP,
    )
```

---

#### Money の設計判断マトリクス

| 状況 | 推奨 | 理由 |
|------|------|------|
| 同じ DB カラムを複数ドメインが読む | **A（最小共通型）** | スキーマ変更が全ドメインに影響するため共通化は不可避 |
| API レスポンスに含まれる金額 | **C（DTO共通、モデル別）** | API 変更対応は一箇所。変換後のモデルは独立 |
| 販売と会計で計算ロジックが明確に違う | **B（別々の型）** | 変更理由が独立している。共通化すると双方が使いにくい |
| 同一ドメイン内の複数 UseCase | **A（最小共通型）** | 同じドメイン内なら共通化のコスト < 重複のコスト |
| 将来的な乖離が予測できる | **B（別々の型）** | 最初から分けた方が、後から統合するより安い |

---

### 「変更の独立性」で判断する 2 つの問い

設計判断に迷ったとき、次の 2 問だけ問え。

**問 1：片方のドメインが型を変更したいとき、もう片方のチームに確認が必要か？**

```
販売チーム「Money に promotionLabel を追加したい」
  → 会計チームに確認が必要？
    Yes → 型を共有すべきでない（変更が他チームを巻き込む）
    No  → 共有してもよい（変更が独立している）
```

**問 2：DB スキーマが変わったとき、全ドメインが同時に影響を受けるか？**

```
price_minor_unit カラムの精度変更
  → 全ドメインが同時に影響を受ける？
    Yes → 最小共通型（パターン A）。どうせ全員変更するので共通化する価値がある
    No  → DTO のみ共通（パターン C）。変換層でドメインごとに対処する
```

---

### 向こう3年を読む：Money が辿る典型的な運命

```
1年目：受注と請求の Money が同じ形。「共通化しよう」の誘惑。
         ↓ 共通化した場合
2年目：会計が BigDecimal を要求。販売は割引率を追加。
         → core/model/Money.kt に if 分岐と nullable フィールドが増殖
3年目：海外展開で通貨換算ロジックが爆発。
         → Money に exchangeRate, baseAmount, convertedAmount が追加
4年目：会計が IFRS 16 対応で Money の「計上タイミング」概念が変わる。
         → 販売ドメインには全く関係ない変更なのに全テストが落ちる

→ 結論：最初から「販売価格」と「会計金額」を別の型にしておくべきだった
```

```
1年目：受注と請求の Money が同じ形。「今は重複だが別型にする」という判断。
         ↓ パターン B を選んだ場合
2年目：会計が BigDecimal を要求 → AccountingAmount だけ変更。SalesPrice は無影響。
3年目：海外展開 → SalesPrice に currencyConversion を追加。会計は無影響。
4年目：IFRS 16 対応 → AccountingAmount にロジック追加。販売チームは知らない。

→ 型の重複は IDE のリファクタリングで解消できるが、
　 間違った抽象化の代償は「設計を捨てて書き直す」ことになる。
```

---

### 総まとめ：どこに置くか判断フロー（全ケース対応版）

```
新しい共通コード / 共通型を書く
        │
        ▼
複数の「サブドメイン」を跨ぐか？（order と billing など）
 ├─ No  → feature/{subdomain}/ 内で完結
 │          └─ UseCase 数が多い → サブパッケージ or Gradle 分割
 └─ Yes →
        │
        ▼
    変更理由が常に同時に発生するか？
     ├─ No  → 各ドメインに「重複して」書く（DRY より境界を優先）
     │         Money の例：SalesPrice と AccountingAmount を別々に持つ
     └─ Yes →
            │
            ▼
        ドメイン知識を含むか？
         ├─ Yes → feature/shared/ に書く
         │         例：認証ユーザー、通知送信、同期管理
         └─ No  → core/ に書く
                   例：HTTP クライアント、DB セットアップ、Money（最小 raw 型のみ）
```

---

## 14. バックエンドとモバイルの責務分界点

> **このプロジェクトの前提：**  
> - バックエンド（Java / Spring Boot）がヘキサゴナルアーキテクチャ + 簡易 CQRS + DDD で**業務ロジック**を持つ  
> - モバイル（Kotlin / MVVM + UDF）は**表示ルール・UI/UX** に専念する  
> - フロント・モバイル・バックエンドは**別リポジトリ**（マルチレポ）  
> - 業界は小売業。社内ツールで複数サブドメインが存在する

### 問い：なぜ責務の分界点を最初に決めるのか？

分界点が曖昧なまま開発を進めると、以下が起きる：

```
1ヶ月目：モバイルチームが「キャンセル可否判定」をモバイルに実装
2ヶ月目：バックエンドチームも「キャンセル可否判定」をサーバーに実装
3ヶ月目：小売の業務ルール変更「出荷後24時間以内はキャンセル可に」
         → モバイル版を修正してストア申請 → 審査待ち2日
         → バックエンド版を修正してデプロイ → 即日反映
         → バックエンドは「キャンセル可」なのにモバイルは「不可」を表示
4ヶ月目：「どっちが正しいの？」という問い合わせが社内で頻発
```

**分界点を明確にすることで、「どこを直せばいいか」が即座にわかる体制を作る。**

---

### 責務マトリクス

| ロジック種別 | 配置先 | 理由 | 具体例 |
|---|---|---|---|
| **業務ルール** | バックエンド ドメイン層 | 業務損害に直結。一箇所で管理すべき | キャンセル可否、金額計算、在庫引当、与信判定 |
| **業務バリデーション** | バックエンド アプリケーション層 | データ整合性の最終防衛線 | 受注数量の上限、顧客ステータスチェック、重複受注検知 |
| **フォームバリデーション** | モバイル `feature/*/domain/` | ユーザー体験（即時フィードバック）のため | 必須入力チェック、文字数制限、メール形式、数値範囲 |
| **表示ルール** | モバイル `feature/*/domain/` | UI/UX の責務 | ステータスバッジの色、一覧のグルーピング、日付フォーマット |
| **操作制御** | バックエンド → API レスポンス | ルール変更にアプリ更新を不要にするため | ボタンの活性/非活性（`actions.canCancel` 等） |
| **画面遷移** | モバイル `feature/*/api/` + `impl/` | 純粋にモバイル内部の関心事 | NavKey、EntryProvider、Navigator 拡張関数 |
| **状態管理** | モバイル `feature/*/impl/` | MVVM + UDF の責務 | ViewModel の StateFlow |
| **エラーメッセージ変換** | モバイル `feature/*/domain/` | 表示言語・表現はモバイルの責務 | API エラー → ユーザー向けメッセージ |

---

### 判断基準：3 つの問い

新しいロジックを書くとき、以下を順に問う：

```
問1: この判断を間違えたとき、業務上の損害が出るか？
 ├─ Yes → バックエンドに置く（ドメイン層 or アプリケーション層）
 └─ No  → 問2へ

問2: このルールが変わったとき、アプリ更新なしで反映したいか？
 ├─ Yes → バックエンドに置き、APIレスポンスで結果を返す
 └─ No  → 問3へ

問3: 複数プラットフォーム（モバイル + Web フロント）で同じ判定が必要か？
 ├─ Yes → バックエンドに置く（各プラットフォームで重複実装を避ける）
 └─ No  → モバイルに置いてよい（表示ルール・フォームバリデーション等）
```

---

### API レスポンスに業務判定結果を含める設計

バックエンドが DDD で業務ルールを持つ場合、**判定結果を API レスポンスに含める**ことで、  
モバイルから業務 UseCase を排除できる。

#### レスポンス設計例

```json
// GET /api/orders/{id}
{
  "id": "ORD-001",
  "status": "CONFIRMED",
  "statusDisplayName": "確認済み",
  "totalAmount": { "amount": 10000, "currency": "JPY" },
  "items": [
    {
      "productId": "PROD-001",
      "productName": "商品A",
      "quantity": 2,
      "unitPrice": { "amount": 5000, "currency": "JPY" }
    }
  ],

  // ← バックエンドのドメイン層が判定した結果
  "actions": {
    "canCancel": true,
    "canEdit": false,
    "canClaim": false,
    "cancelDeadline": "2026-04-10T23:59:59Z"
  },

  // ← 表示に必要な関連データを含める（追加 API 呼び出し不要に）
  "customer": {
    "id": "CUST-001",
    "displayName": "山田商店"
  }
}
```

```json
// GET /api/orders（一覧）
{
  "items": [
    {
      "id": "ORD-001",
      "statusDisplayName": "確認済み",
      "customerDisplayName": "山田商店",
      "totalAmount": { "amount": 10000, "currency": "JPY" },
      "createdAt": "2026-04-08T10:00:00Z",
      "actions": { "canCancel": true }
    }
  ],
  "pagination": {
    "page": 1,
    "totalPages": 5,
    "totalItems": 48
  },

  // ← 一覧画面全体に対するアクション
  "bulkActions": {
    "canBulkCancel": true,
    "canExportCsv": true
  }
}
```

#### 効果比較

| 観点 | 単体アプリ方式（NiA流） | マルチレポ方式（API レスポンスに判定含む） |
|---|---|---|
| キャンセル可否判定 | `CanCancelOrderUseCase` をモバイルに実装 | `actions.canCancel` を表示するだけ |
| 受注 + 顧客名の合成 | `GetOrderWithCustomerUseCase` で 2 Flow を combine | API が `customer.displayName` を含めて返す |
| ルール変更の反映 | モバイルアプリ更新 + ストア審査 | バックエンドデプロイのみ（即日反映） |
| ビジネスルールの一元性 | モバイルとバックエンドで重複リスク | バックエンドの DDD ドメイン層に一元化 |
| モバイル domain/ の複雑度 | 高（業務ルール + 表示ルール） | 低（表示ルールのみ） |
| テスト | モバイルで業務ルールのテストが必要 | バックエンドの JUnit で完結 |

---

### バックエンド側の構造（ヘキサゴナル × 簡易 CQRS）

モバイルチームが「バックエンドがどう作られているか」を知る必要はないが、  
**テックリードとして全体像を示す**ために概要を記す。

```
backend-repo/
├── order-service/                       ← 受注 Bounded Context
│   ├── domain/                          ← ドメイン層（純粋 Java、外部依存なし）
│   │   ├── model/
│   │   │   ├── Order.java               ← 集約ルート。canCancel(), addItem() 等
│   │   │   ├── OrderItem.java           ← 値オブジェクト
│   │   │   └── OrderStatus.java         ← 値オブジェクト（enum）
│   │   ├── service/
│   │   │   └── OrderDomainService.java  ← エンティティ単体で完結しないルール
│   │   └── port/
│   │       ├── in/                      ← 入力ポート（UseCase interface）
│   │       │   ├── CreateOrderUseCase.java
│   │       │   └── CancelOrderUseCase.java
│   │       └── out/                     ← 出力ポート（Repository interface）
│   │           ├── OrderRepository.java
│   │           └── StockReservationPort.java
│   │
│   ├── application/                     ← アプリケーション層（CQRS の分岐点）
│   │   ├── command/                     ← 書き込み：ドメインモデルを経由
│   │   │   ├── CreateOrderCommand.java
│   │   │   └── CreateOrderCommandHandler.java
│   │   └── query/                       ← 読み取り：ドメインモデルをバイパス
│   │       ├── GetOrdersQuery.java
│   │       └── GetOrdersQueryHandler.java  ← Oracle に直接 SQL（性能最適化）
│   │
│   └── adapter/                         ← アダプター層
│       ├── in/web/
│       │   ├── OrderController.java     ← REST API エンドポイント
│       │   └── dto/
│       │       ├── CreateOrderRequest.java
│       │       └── OrderResponse.java   ← actions フィールドを含む
│       └── out/persistence/
│           ├── OrderJpaEntity.java       ← Oracle テーブルマッピング
│           └── OrderPersistenceAdapter.java  ← port/out/OrderRepository の実装
│
├── inventory-service/                   ← 在庫 Bounded Context（同様の構造）
├── customer-service/                    ← 顧客 Bounded Context
└── shared-kernel/                       ← 複数 Context が共有する値オブジェクト
    └── Money.java
```

#### 簡易 CQRS のポイント

```
Command（書き込み）:
  Controller → CommandHandler → Domain Model → Repository.save()
  ・ドメインモデルを経由（DDD の恩恵を受ける）
  ・トランザクション整合性を保証

Query（読み取り）:
  Controller → QueryHandler → Oracle に直接 SQL → DTO を返す
  ・ドメインモデルをバイパス（パフォーマンス最適化）
  ・JOIN・集約を Oracle で実行し、画面表示用 DTO を直接構築
  ・「actions」フィールドの判定はここでドメインサービスを呼ぶ

「簡易」の意味:
  ・イベントソーシングは使わない
  ・読み書きは同一 Oracle DB（物理的に分離しない）
  ・Query 側で「ドメインモデルを経由しない読み取り最適化」ができるだけで十分な効果
```

#### モバイルに返す「actions」の生成

```java
// backend: GetOrdersQueryHandler.java
public OrderResponse toResponse(OrderQueryResult row) {
    // ドメインモデルに判定を委譲
    Order order = orderRepository.findById(row.getId());

    return OrderResponse.builder()
        .id(row.getId())
        .status(row.getStatus())
        .statusDisplayName(row.getStatus().getDisplayName())
        .totalAmount(new MoneyDto(row.getTotalAmount(), row.getCurrency()))
        .actions(ActionsDto.builder()
            .canCancel(order.canCancel())          // ← ドメインモデルの判定
            .canEdit(order.canEdit())              // ← ドメインモデルの判定
            .canClaim(order.canClaim())            // ← ドメインモデルの判定
            .build())
        .customer(new CustomerSummaryDto(row.getCustomerId(), row.getCustomerName()))
        .build();
}
```

> **モバイルチームはこの構造を知らなくてよいが、テックリードは両方を見渡す必要がある。**  
> API レスポンスの設計（特に `actions` フィールドの内容）は、  
> バックエンド・モバイル両チームの合意事項として API 仕様書に明記する。

---

## 15. マルチレポ構成での API 契約管理

### 問い：マルチレポで最も壊れやすいのは何か？

答え：**リポジトリ間の契約（API スキーマ）**。

```
バックエンドチームが OrderResponse に breaking change を入れた
→ モバイルチームはそのことを知らない
→ デプロイ後にモバイルアプリがクラッシュ
→ 「誰が悪いのか」で会議が3時間
```

**契約を明示的に管理する仕組み** がなければ、マルチレポは破綻する。

---

### 選択肢と比較

| 方式 | Spring Boot 親和性 | モバイル親和性 | 学習コスト | 適する場面 |
|---|---|---|---|---|
| **OpenAPI（Swagger）** | ◎ springdoc-openapi で自動生成 | ◎ openapi-generator で Retrofit コード生成 | 低 | REST API 中心の社内ツール |
| **GraphQL** | ○ spring-graphql | ○ Apollo Kotlin | 中 | 画面ごとに必要データが大きく異なる場合 |
| **gRPC / Protobuf** | ○ grpc-spring-boot-starter | △ Android での HTTP/2 対応が必要 | 高 | マイクロサービス間通信と統一したい場合 |

> **推奨：OpenAPI + コード生成。**  
> 理由：Spring Boot との親和性が最も高く、チームの学習コストが低い。  
> 小売業の社内ツールでは GraphQL の柔軟性よりも、REST の単純さが運用しやすい。

---

### OpenAPI を使った契約管理フロー

#### リポジトリ構成

```
github.com/yourorg/
├── sales-backend/           ← バックエンドリポジトリ
│   ├── order-service/
│   ├── build.gradle         ← springdoc-openapi プラグイン
│   └── docs/api/
│       └── openapi.yaml     ← CI で自動生成 → artifact として publish
│
├── sales-api-schema/        ← API スキーマ専用リポジトリ（Option A）
│   ├── order/
│   │   └── openapi.yaml
│   ├── inventory/
│   │   └── openapi.yaml
│   └── CHANGELOG.md         ← Breaking Change の記録
│
└── sales-mobile/            ← モバイルリポジトリ
    ├── core/network/
    │   └── generated/       ← openapi-generator で自動生成された Retrofit interface
    ├── feature/order/data/
    │   └── network/         ← 生成コードを使うか、手書きするかはチーム判断
    └── build.gradle.kts     ← openapi-generator プラグイン
```

#### 管理フロー

```
1. バックエンドチームが API を変更
   → PR に openapi.yaml の diff が含まれる
   → レビューでモバイルチームに通知

2. CI がスキーマの破壊的変更を検知
   → openapi-diff ツールで自動チェック
   → Breaking Change なら PR にラベル付与 + モバイルチームに自動通知

3. モバイルチームがスキーマを取得
   → Gradle タスクで最新の openapi.yaml からコード生成
   → 型の変更がコンパイルエラーとして即座に検知される
```

---

### エラーハンドリング契約

API のエラー形式もスキーマの一部。**全サブドメイン共通のエラー形式**を定義する。

#### RFC 7807（Problem Details for HTTP APIs）ベースの例

```json
// 422 Unprocessable Entity
{
  "type": "https://sales.example.com/problems/insufficient-stock",
  "title": "在庫不足",
  "status": 422,
  "detail": "商品A の在庫が不足しています（要求: 10, 在庫: 3）",
  "traceId": "abc-123-def",
  "errors": [
    {
      "field": "items[0].quantity",
      "code": "INSUFFICIENT_STOCK",
      "message": "在庫が不足しています",
      "meta": { "requested": 10, "available": 3 }
    }
  ]
}
```

```json
// 409 Conflict
{
  "type": "https://sales.example.com/problems/order-already-cancelled",
  "title": "受注は既にキャンセル済み",
  "status": 409,
  "detail": "受注 ORD-001 は既にキャンセルされています"
}
```

#### モバイル側のエラー処理

```kotlin
// core/network/ErrorResponseParser.kt
// 全サブドメイン共通のエラー解析（core/ に置く）
@Serializable
data class ProblemDetail(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val traceId: String? = null,
    val errors: List<FieldError> = emptyList(),
)

@Serializable
data class FieldError(
    val field: String,
    val code: String,
    val message: String,
)

// Retrofit の Response からエラーを解析する拡張関数
fun <T> Response<T>.parseError(json: Json): ProblemDetail? {
    if (isSuccessful) return null
    val body = errorBody()?.string() ?: return null
    return runCatching { json.decodeFromString<ProblemDetail>(body) }.getOrNull()
}
```

```kotlin
// feature/order/domain/MapOrderErrorToUiMessageUseCase.kt
// 受注ドメイン固有のエラーメッセージ変換（feature/ に置く）
class MapOrderErrorToUiMessageUseCase @Inject constructor() {
    operator fun invoke(error: ProblemDetail): UserMessage = when {
        error.type.contains("insufficient-stock") -> UserMessage(
            title = "在庫不足",
            body = error.detail,
            severity = Severity.WARNING,
            fieldErrors = error.errors.map { it.field to it.message },
        )
        error.type.contains("already-cancelled") -> UserMessage(
            title = "操作できません",
            body = error.detail,
            severity = Severity.INFO,
        )
        error.status in 500..599 -> UserMessage(
            title = "サーバーエラー",
            body = "しばらくしてからもう一度お試しください（traceId: ${error.traceId}）",
            severity = Severity.ERROR,
        )
        else -> UserMessage(
            title = error.title,
            body = error.detail,
            severity = Severity.ERROR,
        )
    }
}
```

---

### 認証フロー

マルチレポ構成では、認証の実装が**バックエンド・モバイル両方に跨がる**。

```
モバイル                              バックエンド                IdP（社内認証基盤）
  │                                    │                         │
  ├─ ログイン画面表示 ─────────────────→│                         │
  │                                    │                         │
  │  OAuth 2.0 Authorization Code Flow │                         │
  ├─ redirect ─────────────────────────┼────────────────────────→│
  │                                    │                         │
  │←── authorization code ─────────────┼─────────────────────────┤
  │                                    │                         │
  ├─ POST /token (code + PKCE) ───────→│                         │
  │                                    ├─ verify code ──────────→│
  │                                    │←── tokens ──────────────┤
  │←── access_token + refresh_token ───┤                         │
  │                                    │                         │
  ├─ GET /api/orders ─────────────────→│                         │
  │  (Authorization: Bearer token)     │                         │
  │←── 200 OK ─────────────────────────┤                         │
```

```kotlin
// core/network/AuthInterceptor.kt
// 全 API リクエストにトークンを付与
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getAccessToken() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

// core/network/TokenRefreshAuthenticator.kt
// 401 レスポンス時にトークンを自動リフレッシュ
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApi: AuthApi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= 2) return null  // 無限ループ防止

        val refreshToken = tokenStore.getRefreshToken() ?: return null
        val newTokens = runBlocking {
            runCatching { authApi.refreshToken(refreshToken) }.getOrNull()
        } ?: return null

        tokenStore.saveTokens(newTokens)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }
}
```

```
// feature/shared/auth/ に認証の UI ロジックを置く（意図的な共通化）
feature/shared/auth/
├── AuthRepository.kt          ← interface（ログイン・ログアウト・トークン管理）
├── AuthRepositoryImpl.kt      ← impl（core/network の AuthApi を使う）
├── AuthUser.kt                ← ログインユーザー型
├── GetCurrentUserUseCase.kt   ← 全サブドメインが使う「誰がログインしているか」
└── di/AuthModule.kt
```

---

## 16. Offline-First vs API-First の選定基準

### 問い：社内ツールに Offline-First は必要か？

NiA（Now in Android）が Offline-First を採用しているのは、  
一般消費者向けアプリで**ネットワーク環境が不安定**な前提があるため。

社内ツールの場合、利用環境によって最適解が異なる。

---

### 3 つの戦略

#### 戦略A：API-First + 軽量キャッシュ（推奨デフォルト）

```
モバイル                    バックエンド
  │                          │
  ├─ GET /api/orders ──────→│
  │←── JSON レスポンス ──────┤
  │                          │
  │  ViewModel に StateFlow で保持（メモリキャッシュ）
  │  OkHttp Cache で HTTP レスポンスキャッシュ
  │  画面回転 → ViewModel が生存中なので再取得不要
  │  画面遷移で戻る → ViewModel 再生成 → API 再取得
  │
  │  ネットワーク断 → 「接続してください」メッセージ表示
```

**適する場面：**
- オフィス内 WiFi 常時接続
- データのリアルタイム性が重要（在庫状況、受注ステータス等）
- Room の学習・運用コストを避けたい

```kotlin
// feature/order/data/OrderRepository.kt（API-First版）
internal class ApiFirstOrderRepository @Inject constructor(
    private val orderApi: OrderApiService,
) : OrderRepository {

    // API から直接取得。キャッシュは OkHttp に任せる
    override suspend fun getOrders(query: OrderQuery): List<Order> =
        orderApi.getOrders(
            status = query.filterStatuses?.map { it.name },
            customerId = query.filterCustomerId,
            page = query.page,
            size = query.pageSize,
        ).items.map { it.toDomainModel() }

    override suspend fun getOrder(orderId: String): Order =
        orderApi.getOrder(orderId).toDomainModel()

    override suspend fun cancelOrder(orderId: String): Result<Unit> = runCatching {
        orderApi.cancelOrder(orderId)
    }
}
```

```kotlin
// feature/order/impl/OrderListViewModel.kt（API-First版）
@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<OrderListUiState> = _refreshTrigger
        .flatMapLatest {
            flow {
                emit(OrderListUiState.Loading)
                try {
                    val orders = orderRepository.getOrders(currentQuery())
                    emit(OrderListUiState.Success(orders))
                } catch (e: IOException) {
                    emit(OrderListUiState.Error("ネットワークに接続できません"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderListUiState.Loading)

    fun refresh() { _refreshTrigger.value++ }
}
```

#### 戦略B：Offline-First（Room + 同期）

```
モバイル                    バックエンド
  │                          │
  │  起動時 or 定期的に同期   │
  ├─ GET /api/orders/sync ─→│
  │←── 差分データ ────────── ┤
  │  Room に保存              │
  │                          │
  │  画面表示は常に Room から  │
  │  Flow<List<Order>> で自動更新
  │
  │  ネットワーク断 → ローカルデータで業務継続可能
```

**適する場面：**
- 倉庫・店舗でネットワークが不安定
- オフラインでも業務を止められない（ピッキング、棚卸し等）
- バッチ的にデータを持っておきたい（マスタデータ等）

```kotlin
// feature/inventory/data/OfflineFirstInventoryRepository.kt
// 在庫ピッキング用。倉庫の WiFi が不安定なため Offline-First
internal class OfflineFirstInventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val inventoryApi: InventoryApiService,
) : InventoryRepository {

    override fun getInventoryItems(): Flow<List<InventoryItem>> =
        inventoryDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun sync(): Boolean = runCatching {
        val lastSync = inventoryDao.getLastSyncVersion()
        val response = inventoryApi.getChanges(afterVersion = lastSync)
        inventoryDao.upsertAll(response.items.map { it.toEntity() })
        inventoryDao.updateSyncVersion(response.version)
    }.isSuccess
}
```

#### 戦略C：ハイブリッド（サブドメインごとに選択）

```
feature/order/data/       → API-First（受注はリアルタイム性重視）
feature/inventory/data/   → Offline-First（倉庫でのピッキング用）
feature/customer/data/    → API-First + マスタキャッシュ（顧客一覧だけ Room）
feature/product/data/     → Offline-First（商品マスタは頻繁に変わらない）
```

**推奨：複数サブドメインの社内ツールでは戦略C が最も現実的。**

---

### 選定マトリクス

| 判断基準 | API-First | Offline-First |
|---|---|---|
| **利用環境** | オフィス（WiFi 安定） | 倉庫・店舗（WiFi 不安定） |
| **データ鮮度** | リアルタイム性が重要 | 数分〜数時間の遅延許容 |
| **オフライン時** | 「接続してください」で OK | 業務を止められない |
| **データ量** | ページネーションで都度取得 | 端末にキャッシュすべき量がある |
| **競合処理** | サーバー側で排他制御 | 楽観ロック + 同期時に競合解決 |
| **実装コスト** | 低（Room 不要） | 高（Room + 同期 + 競合解決） |
| **テストコスト** | 低（API モックだけ） | 高（Room + 同期のテスト） |

---

### `core/database` の位置づけ変化

| 戦略 | core/database の役割 |
|---|---|
| API-First | **不要**、または DataStore でトークン/設定だけ管理 |
| Offline-First | NiA と同等（Entity/DAO を全て管理） |
| ハイブリッド | Offline-First のサブドメインの Entity/DAO だけ管理。API-First のサブドメインは Room を使わない |

```
// ハイブリッド構成の core/database
core/database/
├── inventory/             ← Offline-First のサブドメインのみ
│   ├── InventoryItemEntity.kt
│   └── InventoryDao.kt
├── product/               ← マスタキャッシュ用
│   ├── ProductEntity.kt
│   └── ProductDao.kt
├── SalesDatabase.kt       ← Offline-First のテーブルだけ列挙
└── di/DatabaseModule.kt

// ← order/, customer/ の Entity は存在しない（API-First）
```

---

## 17. モバイル `domain/` 層の再定義

### 問い：バックエンドに業務ロジックがある場合、モバイルの `domain/` に何を書くのか？

本ドキュメントの前半（§6）では `feature/*/domain/` に業務 UseCase を置いていた。  
バックエンドが DDD で業務ロジックを持つ場合、モバイルの `domain/` の役割は以下に変わる。

---

### モバイル `domain/` の 4 つの責務

#### 1. 表示変換（API レスポンス → 画面表示用モデル）

```kotlin
// feature/order/domain/FormatOrderListUseCase.kt
// API が返す受注一覧を、画面表示用のセクション分けリストに変換
class FormatOrderListUseCase @Inject constructor() {
    operator fun invoke(orders: List<Order>): List<OrderSection> =
        orders
            .groupBy { it.createdAt.toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, orders) ->
                OrderSection(
                    header = formatSectionHeader(date),
                    orders = orders,
                )
            }

    private fun formatSectionHeader(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> "今日"
            date == today.minusDays(1) -> "昨日"
            date.isAfter(today.minusDays(7)) -> "今週"
            else -> date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        }
    }
}

data class OrderSection(
    val header: String,
    val orders: List<Order>,
)
```

#### 2. フォームバリデーション（即時フィードバック用）

```kotlin
// feature/order/domain/ValidateOrderFormUseCase.kt
// モバイル側で即時にフィードバックするバリデーション
// ※ 業務バリデーション（在庫チェック等）はAPI送信時にバックエンドが実施
class ValidateOrderFormUseCase @Inject constructor() {

    operator fun invoke(input: OrderFormInput): FormValidationResult {
        val errors = buildList {
            // 必須チェック
            if (input.customerName.isBlank()) {
                add(FormFieldError("customerName", "顧客名は必須です"))
            }

            // 明細の存在チェック
            if (input.items.isEmpty()) {
                add(FormFieldError("items", "商品を1つ以上追加してください"))
            }

            // 明細ごとのチェック
            input.items.forEachIndexed { i, item ->
                if (item.quantity <= 0) {
                    add(FormFieldError("items[$i].quantity", "1以上の数値を入力してください"))
                }
                if (item.quantity > 9999) {
                    add(FormFieldError("items[$i].quantity", "9999以下の数値を入力してください"))
                }
                if (item.productId.isBlank()) {
                    add(FormFieldError("items[$i].productId", "商品を選択してください"))
                }
            }

            // 配送日チェック（未来日のみ）
            input.deliveryDate?.let { date ->
                if (date.isBefore(LocalDate.now())) {
                    add(FormFieldError("deliveryDate", "配送日は明日以降を指定してください"))
                }
            }
        }
        return if (errors.isEmpty()) FormValidationResult.Valid
               else FormValidationResult.Invalid(errors)
    }
}

sealed interface FormValidationResult {
    data object Valid : FormValidationResult
    data class Invalid(val errors: List<FormFieldError>) : FormValidationResult
}

data class FormFieldError(
    val field: String,
    val message: String,
)
```

#### 3. API エラー → ユーザーメッセージ変換

```kotlin
// feature/order/domain/MapOrderErrorUseCase.kt
// バックエンドの ProblemDetail をユーザーに見せるメッセージに変換
class MapOrderErrorUseCase @Inject constructor() {

    operator fun invoke(error: ProblemDetail): OrderErrorUiModel = when {
        // 在庫不足 → フィールドレベルのエラー表示
        error.type.contains("insufficient-stock") -> OrderErrorUiModel.FieldErrors(
            title = "在庫が不足しています",
            fieldErrors = error.errors.associate { it.field to it.message },
        )

        // 競合（他のユーザーが先に操作） → ダイアログ
        error.status == 409 -> OrderErrorUiModel.Dialog(
            title = "操作できません",
            message = error.detail,
            action = DialogAction.REFRESH,
        )

        // 認証エラー → ログイン画面へ
        error.status == 401 -> OrderErrorUiModel.NavigateToLogin

        // サーバーエラー → スナックバー
        error.status in 500..599 -> OrderErrorUiModel.Snackbar(
            message = "サーバーエラーが発生しました。しばらくしてからお試しください。",
            traceId = error.traceId,
        )

        // その他 → 汎用エラー
        else -> OrderErrorUiModel.Snackbar(
            message = error.detail,
            traceId = error.traceId,
        )
    }
}

sealed interface OrderErrorUiModel {
    data class FieldErrors(val title: String, val fieldErrors: Map<String, String>) : OrderErrorUiModel
    data class Dialog(val title: String, val message: String, val action: DialogAction) : OrderErrorUiModel
    data class Snackbar(val message: String, val traceId: String?) : OrderErrorUiModel
    data object NavigateToLogin : OrderErrorUiModel
}
```

#### 4. 複数 API レスポンスの画面用合成

```kotlin
// feature/order/domain/BuildOrderDashboardUseCase.kt
// ダッシュボード画面用に複数の API レスポンスを合成
// ※ 業務ロジックではなく「1画面に必要なデータを集める」だけ
class BuildOrderDashboardUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
) {
    suspend operator fun invoke(): OrderDashboard {
        // 複数 API を並行呼び出し
        return coroutineScope {
            val todayOrders = async { orderRepository.getOrders(OrderQuery(dateFrom = LocalDate.now())) }
            val pendingCount = async { orderRepository.getPendingCount() }
            val recentCancels = async { orderRepository.getRecentCancels(limit = 5) }

            OrderDashboard(
                todayOrderCount = todayOrders.await().size,
                pendingCount = pendingCount.await(),
                recentCancels = recentCancels.await(),
            )
        }
    }
}
```

---

### `domain/` 配置の判断フロー（マルチレポ版）

```
新しいロジックを書く
       │
       ▼
業務上の損害に直結するか？
 ├─ Yes → バックエンドのドメイン層に書く
 │         例: canCancel(), 金額計算, 在庫引当
 └─ No  →
       │
       ▼
ルール変更をアプリ更新なしで反映したいか？
 ├─ Yes → バックエンドに書き、APIレスポンスで結果を返す
 │         例: 操作可否 → actions フィールド
 └─ No  →
       │
       ▼
モバイル feature/*/domain/ に書く
 ├── 表示変換（日付フォーマット、セクション分け、ソート）
 ├── フォームバリデーション（即時フィードバック用）
 ├── APIエラーのUI表示変換
 └── 複数APIの画面用合成
```

---

### 変更前 vs 変更後のディレクトリ比較

```
// 変更前（NiA 流：モバイルに業務ロジックを置く）
feature/order/domain/
├── GetOrdersUseCase.kt             ← 単純な Repository 委譲
├── GetOrderWithCustomerUseCase.kt  ← 2つの Flow を combine（重い）
├── CreateOrderUseCase.kt           ← 業務ロジック（本来バックエンド）
├── CanCancelOrderUseCase.kt        ← 業務ルール（本来バックエンド）
├── CanEditOrderUseCase.kt          ← 業務ルール（本来バックエンド）
└── ValidateOrderInputUseCase.kt    ← 業務バリデーション混在

// 変更後（マルチレポ：表示ルールに特化）
feature/order/domain/
├── FormatOrderListUseCase.kt       ← 表示変換（セクション分け）
├── ValidateOrderFormUseCase.kt     ← フォームバリデーションのみ
├── MapOrderErrorUseCase.kt         ← APIエラー → UIメッセージ
└── BuildOrderDashboardUseCase.kt   ← 複数API合成（画面用）

// ↑ 業務ロジック（canCancel, createOrder）はバックエンドに移動
// ↑ GetOrderWithCustomerUseCase は不要（APIが customer を含めて返す）
```

> **UseCase を作るかどうかの基準も変わる：**  
> - NiA 流：「2つ以上の Repository を組み合わせるなら UseCase」  
> - マルチレポ流：「ViewModel が太くなりすぎるなら UseCase に切り出す」  
> - シンプルな API 呼び出し → Repository → ViewModel で直接使う（UseCase 不要）

---

## 18. バックエンドアーキテクチャ概要（ヘキサゴナル + 簡易 CQRS）

> このセクションはモバイルチームへの参考情報として、  
> バックエンドの設計思想と構造の概要を示す。  
> テックリードがシステム全体のアーキテクチャを俯瞰するためのもの。

---

### ヘキサゴナルアーキテクチャ（Ports & Adapters）とは

```
                  ┌──── Adapter(in) ────┐
                  │  REST Controller    │
                  │  Message Consumer   │
                  └────────┬────────────┘
                           │ Port(in) = UseCase interface
                           ▼
              ┌─────────────────────────┐
              │     Application Layer   │
              │  CommandHandler / Query  │
              ├─────────────────────────┤
              │     Domain Layer        │
              │  Entity, Value Object   │
              │  Domain Service         │
              └────────────┬────────────┘
                           │ Port(out) = Repository interface
                           ▼
                  ┌──── Adapter(out) ────┐
                  │  JPA (Oracle)        │
                  │  REST Client         │
                  │  Message Producer    │
                  └─────────────────────┘
```

**核心：ドメイン層が外部に一切依存しない。**  
DB が Oracle でも PostgreSQL でも、ドメイン層のコードは変わらない。  
これはモバイルの `feature/*/domain/` が `core/database` に依存しないのと同じ原則。

---

### 簡易 CQRS の構造

```
                     ┌──────────────────────┐
                     │    Controller         │
                     │  (REST Adapter in)    │
                     └──────┬───────────────┘
                            │
               ┌────────────┴────────────┐
               │                         │
               ▼                         ▼
       ┌─── Command ───┐        ┌─── Query ────┐
       │ CommandHandler │        │ QueryHandler  │
       │       │        │        │       │       │
       │       ▼        │        │       ▼       │
       │ Domain Model   │        │  Oracle SQL   │
       │ (DDD Entity)   │        │  (直接実行)   │
       │       │        │        │       │       │
       │       ▼        │        │       ▼       │
       │ Repository     │        │  DTO 返却     │
       │ (JPA/Oracle)   │        │  (JOIN済み)   │
       └────────────────┘        └───────────────┘
```

#### Command 側（書き込み）

```java
// application/command/CreateOrderCommandHandler.java
@Service
@Transactional
public class CreateOrderCommandHandler implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final StockReservationPort stockPort;
    private final OrderDomainService domainService;

    @Override
    public OrderId execute(CreateOrderCommand command) {
        // 1. ドメインモデルを生成
        Order order = Order.create(
            command.getCustomerId(),
            command.getItems().stream()
                .map(item -> OrderItem.of(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList()
        );

        // 2. ドメインサービスでビジネスルール検証
        domainService.validateOrder(order);

        // 3. 在庫引当（出力ポート経由）
        stockPort.reserve(order.getId(), order.getItems());

        // 4. 永続化
        orderRepository.save(order);

        return order.getId();
    }
}
```

#### Query 側（読み取り）

```java
// application/query/GetOrdersQueryHandler.java
@Service
@Transactional(readOnly = true)
public class GetOrdersQueryHandler {

    private final JdbcTemplate jdbc;  // ドメインモデルをバイパスして Oracle に直接 SQL
    private final OrderDomainService domainService;

    public PagedResult<OrderResponse> execute(GetOrdersQuery query) {
        // 1. Oracle に最適化した SQL で取得（JOIN 済みの DTO）
        List<OrderQueryRow> rows = jdbc.query("""
            SELECT o.id, o.status, o.total_amount, o.currency,
                   o.created_at, c.display_name AS customer_name
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            WHERE (:status IS NULL OR o.status = :status)
            ORDER BY o.created_at DESC
            OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """, ...);

        // 2. actions フィールドの判定（ドメインサービスに委譲）
        return rows.stream()
            .map(row -> OrderResponse.builder()
                .id(row.getId())
                .statusDisplayName(OrderStatus.valueOf(row.getStatus()).getDisplayName())
                .customerDisplayName(row.getCustomerName())
                .totalAmount(new MoneyDto(row.getTotalAmount(), row.getCurrency()))
                .actions(buildActions(row))  // ← ドメインの判定結果を埋め込む
                .build())
            .toList();
    }

    private ActionsDto buildActions(OrderQueryRow row) {
        OrderStatus status = OrderStatus.valueOf(row.getStatus());
        return ActionsDto.builder()
            .canCancel(status.canCancel())
            .canEdit(status.canEdit())
            .canClaim(status == OrderStatus.DELIVERED)
            .build();
    }
}
```

---

### Bounded Context とモバイル feature の対応

```
バックエンド Bounded Context        モバイル feature/
┌─────────────────────┐           ┌─────────────────────┐
│ order-service/      │ ←──API──→ │ feature/order/      │
│  domain/            │           │  data/ (API呼出)    │
│  application/       │           │  domain/ (表示変換) │
│  adapter/           │           │  impl/ (ViewModel)  │
├─────────────────────┤           ├─────────────────────┤
│ inventory-service/  │ ←──API──→ │ feature/inventory/  │
├─────────────────────┤           ├─────────────────────┤
│ customer-service/   │ ←──API──→ │ feature/customer/   │
└─────────────────────┘           └─────────────────────┘
```

| バックエンド | モバイル | 接点 |
|---|---|---|
| ドメイン層（Entity, Value Object） | — | モバイルには存在しない |
| アプリケーション層（Command/Query） | — | モバイルから API として呼ばれるだけ |
| アダプター層（Controller + DTO） | feature/*/data/network/ | **API レスポンスの DTO が契約** |
| — | feature/*/domain/ | 表示ルール（バックエンドには存在しない） |
| — | feature/*/impl/ | ViewModel + UI（バックエンドには存在しない） |

> **テックリードのチェックポイント：**  
> - バックエンドの Bounded Context とモバイルの feature/ が 1:1 対応しているか？  
> - API レスポンス DTO に `actions` フィールドが含まれているか？  
> - モバイルに業務ロジックが漏れていないか？（PR レビューの観点）

---

### テスト戦略（システム全体）

```
バックエンド                              モバイル
┌──────────────────────────────────┐    ┌──────────────────────────────┐
│ ドメイン層 Unit Test             │    │ feature/*/domain/ Unit Test  │
│  → Order.canCancel() のテスト    │    │  → 表示変換のテスト           │
│  → 純粋 Java、外部依存なし      │    │  → 純粋 Kotlin、外部依存なし  │
│                                  │    │                              │
│ アプリケーション層 Unit Test     │    │ feature/*/impl/ Unit Test    │
│  → CommandHandler のテスト       │    │  → ViewModel のテスト         │
│  → Repository は Fake           │    │  → Repository は Fake         │
│                                  │    │                              │
│ アダプター層 Integration Test    │    │ feature/*/impl/ UI Test      │
│  → Controller + Oracle テスト    │    │  → ComposeTestRule            │
│  → Testcontainers + Oracle XE   │    │  → ComponentActivity          │
└──────────────────────────────────┘    └──────────────────────────────┘

                    ┌──────────────────────────┐
                    │    E2E / Contract Test    │
                    │  → API 契約テスト         │
                    │  → Spring Cloud Contract  │
                    │    or Pact               │
                    │  → バックエンド API の    │
                    │    レスポンス形式を保証   │
                    └──────────────────────────┘
```

| テスト種別 | 対象 | ツール | 目的 |
|---|---|---|---|
| バックエンド ドメイン Unit | `Order.canCancel()` 等 | JUnit 5 + AssertJ | 業務ルールの正しさ |
| バックエンド アプリ Unit | `CommandHandler` | JUnit 5 + Mockito | ユースケースの正しさ |
| バックエンド Integration | Controller → Oracle | Testcontainers | API レスポンスの正しさ |
| **API 契約テスト** | **API スキーマ** | **Spring Cloud Contract / Pact** | **マルチレポ間の契約保証** |
| モバイル domain Unit | `FormatOrderListUseCase` | JUnit + Truth | 表示変換の正しさ |
| モバイル ViewModel Unit | `OrderListViewModel` | JUnit + Turbine | 状態遷移の正しさ |
| モバイル UI | `OrderListScreen` | ComposeTestRule | UI 表示の正しさ |

> **最も重要なテスト：API 契約テスト。**  
> マルチレポ構成では、バックエンドとモバイルが別々に CI/CD を回すため、  
> 「API の形が変わっていないこと」を自動で保証する仕組みが不可欠。

