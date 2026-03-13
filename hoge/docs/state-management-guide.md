# 状態管理パターン入門 — 注文フローで理解する

> **作成日**: 2026-03-13  
> **対象読者**: 「状態管理」の概念自体がまだ馴染みのない方  
> **関連**: [frontend-mobile-architecture.md](frontend-mobile-architecture.md)、[architecture-decision.md](architecture-decision.md)

---

## 1. そもそも「状態」とは？

**状態 = 今アプリがどういう状況にあるか、を表すデータ**です。

```
例えば「注文」の状態:

  見積もり済み → 注文済み → 決済済み → 発送済み → 発送中 → 配達済み

  → この「今どのステップにいるか」が状態
```

注文だけでなく、画面上のあらゆるものが「状態」を持っています:

```
・ログインしているか？ → { isLoggedIn: true, user: "田中" }
・カートに何が入っているか？ → { items: [商品A, 商品B], total: 12000 }
・商品一覧は読み込み中か？ → { loading: true, products: [] }
・モーダルは開いているか？ → { isOpen: false }
```

---

## 2. なぜ「管理」が必要なのか？

### 状態管理しないとどうなるか（注文フローで説明）

```
【問題1: あちこちに散らばる】

  注文一覧画面 → 「注文済み」と表示
  注文詳細画面 → 「見積もり済み」と表示 ← 古いまま！
  管理者画面   → 「決済済み」と表示   ← バラバラ！

  → 同じ注文なのに、画面ごとに違う状態を見ている
```

```
【問題2: 更新のタイミングがズレる】

  ユーザーが「注文確定」ボタンを押した
    → 画面A は即座に「注文済み」に変わった
    → 画面B はまだ「見積もり済み」のまま
    → ユーザーが画面B を見て「あれ？注文できてない？」と混乱
```

```
【問題3: どこで変えたかわからない】

  注文の状態が「見積もり済み」→「決済済み」にいきなり変わった
  → 「注文済み」を飛ばしてるけど、どのコードがこれをやった？
  → バグの原因が追えない
```

**状態管理 = 「状態をどこに置き」「誰が変更でき」「変更をどう伝えるか」を整理すること**

---

## 3. 注文フローの具体例

### 3.1 注文の状態遷移図

```
┌──────────┐    注文確定     ┌──────────┐    決済完了     ┌──────────┐
│ 見積もり済み │ ──────────→ │  注文済み  │ ──────────→ │  決済済み  │
└──────────┘              └──────────┘              └──────────┘
                                │                        │
                                │ キャンセル              │ 出荷指示
                                ↓                        ↓
                          ┌──────────┐            ┌──────────┐
                          │ キャンセル済 │            │  発送済み  │
                          └──────────┘            └──────────┘
                                                       │
                                                       │ 配送業者引渡
                                                       ↓
                                                 ┌──────────┐
                                                 │   発送中   │
                                                 └──────────┘
                                                       │
                                                       │ 届いた
                                                       ↓
                                                 ┌──────────┐
                                                 │  配達済み  │
                                                 └──────────┘
```

### 3.2 状態を定義する（バックエンド: Java enum）

```java
// バックエンド（Spring Boot / Java）
public enum OrderStatus {
    QUOTED,      // 見積もり済み
    ORDERED,     // 注文済み
    PAID,        // 決済済み
    SHIPPED,     // 発送済み
    IN_TRANSIT,  // 発送中
    DELIVERED,   // 配達済み
    CANCELLED    // キャンセル済み
}
```

### 3.3 状態を定義する（モバイル: Kotlin）

```kotlin
// モバイル（Kotlin）
enum class OrderStatus {
    QUOTED,      // 見積もり済み
    ORDERED,     // 注文済み
    PAID,        // 決済済み
    SHIPPED,     // 発送済み
    IN_TRANSIT,  // 発送中
    DELIVERED,   // 配達済み
    CANCELLED    // キャンセル済み
}
```

### 3.4 状態を定義する（フロントエンド: TypeScript）

```typescript
// フロントエンド（Next.js / TypeScript）
type OrderStatus =
  | "QUOTED"      // 見積もり済み
  | "ORDERED"     // 注文済み
  | "PAID"        // 決済済み
  | "SHIPPED"     // 発送済み
  | "IN_TRANSIT"  // 発送中
  | "DELIVERED"   // 配達済み
  | "CANCELLED";  // キャンセル済み
```

> **ポイント**: 3つのプラットフォームで**同じ値（QUOTED, ORDERED, ...）**を使う。  
> これが OpenAPI で定義しておくと自動で揃う理由。

---

## 4. 「状態がどこにあるか」の2種類

状態管理で最初に理解すべきことは、**状態には2種類ある**ということです。

```
┌────────────────────────────────────────────────────────────┐
│                      状態の種類                              │
├─────────────────────────┬──────────────────────────────────┤
│   サーバー状態           │   クライアント状態                 │
│   (Server State)        │   (Client State)                 │
├─────────────────────────┼──────────────────────────────────┤
│ データの本体は           │ データの本体は                     │
│ サーバー（DB）にある     │ ブラウザ/アプリの中にしかない       │
├─────────────────────────┼──────────────────────────────────┤
│ 注文の状態               │ モーダルが開いているか             │
│ 商品一覧                 │ どのタブを選択中か                 │
│ ユーザー情報             │ カートの中身（決済前）             │
│ 在庫数                   │ フォームの入力途中の値             │
│ 配送状況                 │ ダークモード ON/OFF               │
├─────────────────────────┼──────────────────────────────────┤
│ → APIを呼んで取得        │ → アプリ内で生成・管理            │
│ → 他のユーザーも変更可能  │ → そのユーザーだけの状態           │
│ → キャッシュ・再取得の    │ → React の useState /            │
│   仕組みが必要            │   Kotlin の mutableStateOf で十分 │
└─────────────────────────┴──────────────────────────────────┘
```

### 注文フローでの具体例

| データ | 種類 | 理由 |
|---|---|---|
| 注文ステータス（見積もり済み→注文済み→...） | **サーバー状態** | DB に保存されている。管理者も変更できる |
| 商品一覧 | **サーバー状態** | DB から取得。他の管理者が商品を追加するかも |
| カートの中身（「注文確定」ボタンを押す前） | **クライアント状態** | まだ DB に保存していない。ブラウザ内だけにある |
| 注文確認ダイアログが開いているか | **クライアント状態** | 画面表示の状態。サーバーに関係ない |
| 「決済中...」のローディング表示 | **クライアント状態** | API の通信中という一時的な状態 |

---

## 5. Web（Next.js）での状態管理 — 注文フロー

### 5.1 サーバー状態の管理: TanStack Query

「APIから取ってきたデータ」を賢くキャッシュし、自動で再取得してくれるライブラリ。

```
【TanStack Query がやってくれること】

  ① 注文一覧画面を開く → API を呼んで注文データを取得
  ② 別の画面に行って戻ってくる → キャッシュがあるので即表示
  ③ 一定時間が経つ → 自動で最新データを再取得（stale-while-revalidate）
  ④ 注文を更新した → 関連するキャッシュを無効化 → 最新データで再描画
```

```typescript
// features/order/api/use-orders.ts
import { useQuery } from "@tanstack/react-query";

// 注文一覧を取得するフック
function useOrders() {
  return useQuery({
    queryKey: ["orders"],              // キャッシュのキー（"orders" で識別）
    queryFn: () =>                     // API を呼ぶ関数
      fetch("/api/orders").then(res => res.json()),
    staleTime: 30_000,                 // 30秒間はキャッシュを新鮮とみなす
  });
}

// 使う側
function OrderListPage() {
  const { data: orders, isLoading, error } = useOrders();

  if (isLoading) return <p>読み込み中...</p>;
  if (error)     return <p>エラーが発生しました</p>;

  return (
    <ul>
      {orders.map(order => (
        <li key={order.id}>
          注文#{order.id} — {order.status}
          {/*      ↑ "QUOTED" / "ORDERED" / "PAID" 等が入る */}
        </li>
      ))}
    </ul>
  );
}
```

```typescript
// features/order/api/use-confirm-order.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";

// 注文確定（見積もり済み → 注文済み）
function useConfirmOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (orderId: number) =>
      fetch(`/api/orders/${orderId}/confirm`, { method: "POST" }),

    onSuccess: () => {
      // 注文一覧のキャッシュを無効化 → 自動で再取得 → 画面が最新に
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
  });
}

// 使う側
function OrderDetailPage({ orderId }: { orderId: number }) {
  const confirmOrder = useConfirmOrder();

  return (
    <button
      onClick={() => confirmOrder.mutate(orderId)}
      disabled={confirmOrder.isPending}  // 通信中はボタン無効化
    >
      {confirmOrder.isPending ? "処理中..." : "注文を確定する"}
    </button>
  );
}
```

```
【何が起きているか — 流れ図】

  ユーザーが「注文を確定する」ボタンを押す
    ↓
  POST /api/orders/123/confirm を送信
    ↓
  バックエンドで状態を「見積もり済み → 注文済み」に変更
    ↓
  成功レスポンスが返る
    ↓
  TanStack Query が "orders" キャッシュを無効化
    ↓
  自動で GET /api/orders を再取得
    ↓
  注文一覧画面に「注文済み」と即反映  ✅
```

### 5.2 クライアント状態の管理: Zustand

「画面の中だけの状態」を管理する。カートの中身がこれに該当。

```typescript
// features/cart/store/cart-store.ts
import { create } from "zustand";

type CartItem = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
};

type CartStore = {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (productId: number) => void;
  clear: () => void;
  total: () => number;
};

const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  addItem: (newItem) =>
    set((state) => {
      const existing = state.items.find(i => i.productId === newItem.productId);
      if (existing) {
        // 既にカートにある → 数量を増やす
        return {
          items: state.items.map(i =>
            i.productId === newItem.productId
              ? { ...i, quantity: i.quantity + 1 }
              : i
          ),
        };
      }
      // 新しく追加
      return { items: [...state.items, { ...newItem, quantity: 1 }] };
    }),

  removeItem: (productId) =>
    set((state) => ({
      items: state.items.filter(i => i.productId !== productId),
    })),

  clear: () => set({ items: [] }),

  total: () =>
    get().items.reduce((sum, item) => sum + item.price * item.quantity, 0),
}));
```

```
【カートの状態がどう変わるか】

  初期状態:
    { items: [], total: 0 }

  商品Aを追加:
    { items: [{ name: "KALLAX棚", price: 7990, quantity: 1 }], total: 7990 }

  商品Bを追加:
    { items: [
      { name: "KALLAX棚", price: 7990, quantity: 1 },
      { name: "MALM机",   price: 14990, quantity: 1 }
    ], total: 22980 }

  注文確定（API を呼んで成功したら clear）:
    { items: [], total: 0 }

  ※ このデータはブラウザの中だけにある（DB にはまだ保存していない）
  ※ 注文確定 API を呼ぶまではサーバーに送られない
```

---

## 6. モバイル（Kotlin）での状態管理 — 注文フロー

### 6.1 全体像: MVVM + StateFlow

```
┌──────────┐         ┌─────────────────┐         ┌──────────────┐
│   View   │ ←──── │   ViewModel     │ ←──── │  Repository  │
│ (Compose │  State  │ (StateFlow で   │  Data   │ (API呼び出し │
│  の画面)  │  Flow   │  状態を公開)     │         │  + キャッシュ) │
└──────────┘         └─────────────────┘         └──────────────┘
     │                      ↑
     │  ユーザー操作          │
     └──────────────────────┘
       （ボタンタップ等）
```

```
【React（Web）との対応関係】

  React の世界              Kotlin（Android）の世界
  ────────────              ─────────────────────
  useState / useReducer  →  StateFlow / mutableStateOf
  useEffect              →  LaunchedEffect / viewModelScope
  TanStack Query         →  Repository + StateFlow
  Zustand (store)        →  ViewModel + StateFlow
  Context                →  Hilt (DI)
  コンポーネント           →  Composable 関数
```

### 6.2 サーバー状態の管理: ViewModel + Repository

```kotlin
// ── 注文の状態を表す sealed class ──
// 「今 API 通信のどの段階にあるか」も状態
sealed class OrderListUiState {
    data object Loading : OrderListUiState()                    // 読み込み中
    data class Success(val orders: List<Order>) : OrderListUiState()  // 成功
    data class Error(val message: String) : OrderListUiState()  // エラー
}

data class Order(
    val id: Long,
    val productName: String,
    val status: OrderStatus,    // QUOTED, ORDERED, PAID, ...
    val totalPrice: Int,
)
```

```kotlin
// ── Repository: API を呼ぶ層 ──
class OrderRepository(
    private val api: OrderApi  // Retrofit / Ktor で定義した API interface
) {
    suspend fun getOrders(): List<Order> {
        return api.getOrders()  // GET /api/orders
    }

    suspend fun confirmOrder(orderId: Long) {
        api.confirmOrder(orderId)  // POST /api/orders/{id}/confirm
    }
}
```

```kotlin
// ── ViewModel: 状態を管理する層 ──
class OrderListViewModel(
    private val repository: OrderRepository
) : ViewModel() {

    // UI が観測する状態（StateFlow）
    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState

    init {
        loadOrders()  // 画面を開いたら自動で取得
    }

    // 注文一覧を取得
    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderListUiState.Loading
            try {
                val orders = repository.getOrders()
                _uiState.value = OrderListUiState.Success(orders)
            } catch (e: Exception) {
                _uiState.value = OrderListUiState.Error("取得に失敗しました")
            }
        }
    }

    // 注文を確定（見積もり済み → 注文済み）
    fun confirmOrder(orderId: Long) {
        viewModelScope.launch {
            try {
                repository.confirmOrder(orderId)
                loadOrders()  // 成功したら一覧を再取得 → 画面が最新に
            } catch (e: Exception) {
                _uiState.value = OrderListUiState.Error("注文確定に失敗しました")
            }
        }
    }
}
```

```kotlin
// ── View（Jetpack Compose）: 状態を画面に反映 ──
@Composable
fun OrderListScreen(viewModel: OrderListViewModel) {

    // ViewModel の状態を Compose が観測する
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is OrderListUiState.Loading ->
            CircularProgressIndicator()  // ぐるぐる回るやつ

        is OrderListUiState.Error ->
            Text("エラー: ${state.message}")

        is OrderListUiState.Success ->
            LazyColumn {
                items(state.orders) { order ->
                    OrderCard(
                        order = order,
                        onConfirm = { viewModel.confirmOrder(order.id) }
                    )
                }
            }
    }
}

@Composable
fun OrderCard(order: Order, onConfirm: () -> Unit) {
    Card {
        Text("注文 #${order.id}")
        Text("商品: ${order.productName}")
        Text("状態: ${order.status.displayName()}")

        // 見積もり済みの時だけ「注文確定」ボタンを表示
        if (order.status == OrderStatus.QUOTED) {
            Button(onClick = onConfirm) {
                Text("注文を確定する")
            }
        }
    }
}
```

```
【何が起きているか — 流れ図】

  ① 画面を開く
     → ViewModel が Repository.getOrders() を呼ぶ
     → StateFlow が Loading → Success(orders) に変化
     → Compose が自動で再描画

  ② 「注文を確定する」ボタンをタップ
     → ViewModel.confirmOrder(123) が呼ばれる
     → Repository.confirmOrder(123) が POST /api/orders/123/confirm を送信
     → 成功 → loadOrders() で一覧を再取得
     → StateFlow が Success(最新のorders) に変化
     → Compose が自動で「注文済み」に再描画  ✅
```

### 6.3 クライアント状態の管理: ViewModel（カート）

```kotlin
// ── カート ViewModel ──
class CartViewModel : ViewModel() {

    data class CartState(
        val items: List<CartItem> = emptyList(),
    ) {
        val total: Int get() = items.sumOf { it.price * it.quantity }
    }

    data class CartItem(
        val productId: Long,
        val name: String,
        val price: Int,
        val quantity: Int,
    )

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state

    fun addItem(productId: Long, name: String, price: Int) {
        _state.update { current ->
            val existing = current.items.find { it.productId == productId }
            if (existing != null) {
                current.copy(
                    items = current.items.map {
                        if (it.productId == productId) it.copy(quantity = it.quantity + 1)
                        else it
                    }
                )
            } else {
                current.copy(
                    items = current.items + CartItem(productId, name, price, 1)
                )
            }
        }
    }

    fun removeItem(productId: Long) {
        _state.update { current ->
            current.copy(items = current.items.filter { it.productId != productId })
        }
    }

    fun clear() {
        _state.value = CartState()
    }
}
```

---

## 7. Web と Kotlin の対応まとめ

| やりたいこと | Web（Next.js） | Kotlin（Android） |
|---|---|---|
| API データの取得・キャッシュ | TanStack Query の `useQuery` | ViewModel + Repository + StateFlow |
| API データの更新（POST/PUT） | TanStack Query の `useMutation` | ViewModel 内で `viewModelScope.launch` |
| 更新後のデータ再取得 | `invalidateQueries` | `loadOrders()` を再呼び出し |
| カート等のローカル状態 | Zustand の `create` | ViewModel + `MutableStateFlow` |
| 読み込み中の表示 | `isLoading` フラグ | `sealed class` の `Loading` 状態 |
| エラー表示 | `error` フラグ | `sealed class` の `Error` 状態 |
| 状態の型定義 | TypeScript の `type` / `interface` | Kotlin の `data class` / `enum class` |

---

## 8. 注文フローの状態遷移ルール（ビジネスルール）

「どの状態からどの状態に変われるか」には**ルール**があります。  
これは**バックエンド（ドメイン層）で制御**します。

```
【許可される遷移】
  見積もり済み → 注文済み     ✅（注文確定）
  見積もり済み → キャンセル済み ✅（見積もり段階のキャンセル）
  注文済み     → 決済済み     ✅（決済完了）
  注文済み     → キャンセル済み ✅（注文後のキャンセル）
  決済済み     → 発送済み     ✅（出荷指示）
  発送済み     → 発送中       ✅（配送業者引渡）
  発送中       → 配達済み     ✅（配達完了）

【禁止される遷移の例】
  見積もり済み → 決済済み     ❌（注文を飛ばせない）
  配達済み     → 注文済み     ❌（戻れない）
  キャンセル済み → 注文済み   ❌（キャンセルを取り消せない）
  発送中       → キャンセル済み ❌（発送後はキャンセル不可）
```

```java
// バックエンド（ドメイン層）で遷移ルールを実装
public class Order {
    private OrderStatus status;

    public void confirm() {
        if (this.status != OrderStatus.QUOTED) {
            throw new IllegalStateException(
                "見積もり済みの注文のみ確定できます。現在: " + this.status
            );
        }
        this.status = OrderStatus.ORDERED;
    }

    public void markAsPaid() {
        if (this.status != OrderStatus.ORDERED) {
            throw new IllegalStateException(
                "注文済みの注文のみ決済完了にできます。現在: " + this.status
            );
        }
        this.status = OrderStatus.PAID;
    }

    // ... 以下同様
}
```

**重要**: フロントエンドやモバイルは**ボタンの表示/非表示**で遷移を制御しますが、  
最終的なルールの実行は**必ずバックエンドで行います**（セキュリティのため）。

```
  ┌───────────────────────────────────────────────────┐
  │ フロントエンド・モバイル（UI 制御）                    │
  │                                                   │
  │  見積もり済み → 「注文確定」ボタンを表示              │
  │  注文済み     → 「注文確定」ボタンを非表示            │
  │                 「決済する」ボタンを表示              │
  │  配達済み     → すべてのアクションボタンを非表示       │
  │                                                   │
  │  ※ ただし、ボタンを隠すだけではセキュリティ不十分      │
  │  ※ 不正なAPIリクエストは直接送れてしまうため          │
  └───────────────────────────────────────────────────┘
                          ↓
  ┌───────────────────────────────────────────────────┐
  │ バックエンド（業務ルール実行）                        │
  │                                                   │
  │  API が呼ばれたら必ず状態遷移ルールを検証              │
  │  不正な遷移 → 400 Bad Request / 422 を返す           │
  │  正当な遷移 → DB 更新 + 成功レスポンス               │
  │                                                   │
  │  → ここが「ヘキサゴナルのドメイン層」                  │
  └───────────────────────────────────────────────────┘
```

---

## 9. まとめ — 状態管理で押さえるべき3つのこと

```
① 状態は2種類ある
   ・サーバー状態（注文ステータス等）→ APIで取得・更新
   ・クライアント状態（カート・UI等）→ アプリ内で管理

② 管理する道具は役割で選ぶ
   ・サーバー状態 → TanStack Query（Web）/ ViewModel + Repository（Kotlin）
   ・クライアント状態 → Zustand（Web）/ ViewModel + StateFlow（Kotlin）

③ ビジネスルールはバックエンドで守る
   ・フロント/モバイル → UIでガイド（ボタン表示制御）
   ・バックエンド → 実際のルール検証（不正な遷移を拒否）
```
