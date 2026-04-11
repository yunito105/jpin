# Bulletproof React — 起動からUI表示までの完全フロー解説

> **本書の目的**: `apps/react-vite` を題材に、「ブラウザでURLを開いた瞬間から画面が表示されるまで」に**どのファイルの・どの関数が・どの順番で呼ばれるか**を、コードレベルで徹底的に解説する。
>
> **読み方**: 上から順に読めば、アプリの起動順序がそのまま追える構成になっている。

---

## 目次

1. [全体フロー図（サマリー）](#1-全体フロー図サマリー)
2. [Phase 1: HTMLロード → main.tsx](#2-phase-1-htmlロード--maintsx)
3. [Phase 2: MSW（モックAPI）の初期化](#3-phase-2-mswモックapiの初期化)
4. [Phase 3: Provider チェーン（AppProvider）](#4-phase-3-provider-チェーンappprovider)
5. [Phase 4: 認証チェック（AuthLoader）](#5-phase-4-認証チェックauthloader)
6. [Phase 5: ルーティング（AppRouter）](#6-phase-5-ルーティングapprouter)
7. [Phase 6: レイアウト描画（DashboardLayout）](#7-phase-6-レイアウト描画dashboardlayout)
8. [Phase 7: Feature画面の描画（Discussions一覧）](#8-phase-7-feature画面の描画discussions一覧)
9. [Phase 8: 個別画面の描画（Discussion詳細 + コメント）](#9-phase-8-個別画面の描画discussion詳細--コメント)
10. [Phase 9: データ変更操作（CRUD）の流れ](#10-phase-9-データ変更操作crudの流れ)
11. [横断的関心事（エラーハンドリング・通知・認可）](#11-横断的関心事エラーハンドリング通知認可)
12. [ファイル一覧と役割マップ](#12-ファイル一覧と役割マップ)

---

## 1. 全体フロー図（サマリー）

```
ブラウザがURLを開く
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ index.html                                                   │
│   <div id="root"></div>                                      │
│   <script type="module" src="/src/main.tsx">                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ src/main.tsx                                                 │
│   ① enableMocking()   ← MSWの初期化（開発時のみ）            │
│   ② createRoot(root).render(<App />)                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ src/app/index.tsx  — <App />                                 │
│   <AppProvider>     ← 全Providerのラッパー                   │
│     <AppRouter />   ← ルーティング                           │
│   </AppProvider>                                             │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ src/app/provider.tsx  — <AppProvider>                         │
│                                                              │
│   <Suspense>              ← 非同期読み込み中のフォールバック   │
│     <ErrorBoundary>       ← グローバルエラーキャッチ          │
│       <HelmetProvider>    ← HTMLヘッダー管理                  │
│         <QueryClientProvider>  ← React Query                 │
│           <Notifications />    ← トースト通知                │
│           <AuthLoader>         ← ★ ここでGET /auth/me       │
│             {children}         ← <AppRouter />               │
│           </AuthLoader>                                      │
│         </QueryClientProvider>                               │
│       </HelmetProvider>                                      │
│     </ErrorBoundary>                                         │
│   </Suspense>                                                │
└───────────────────────────┬─────────────────────────────────┘
                            │
                    認証結果に応じて分岐
                   ┌────────┴────────┐
                   │                 │
              未ログイン          ログイン済み
                   │                 │
                   ▼                 ▼
             /auth/login        /app/discussions
             ログイン画面        ダッシュボード
```

---

## 2. Phase 1: HTMLロード → main.tsx

### ファイル: `index.html`

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  </head>
  <body>
    <div id="root"></div>                          ← Reactのマウントポイント
    <script type="module" src="/src/main.tsx"></script>  ← ★ ここからJS開始
  </body>
</html>
```

**何が起きるか**: ブラウザは `index.html` を読み込み、`<div id="root">` を空の状態でレンダリングする。その後 `<script type="module">` により `src/main.tsx` が読み込まれる。

---

### ファイル: `src/main.tsx`

```typescript
import { createRoot } from 'react-dom/client';
import { App } from './app';
import { enableMocking } from './testing/mocks';

enableMocking().then(() => {              // ← ① MSW初期化を待つ
  const root = document.getElementById('root') as HTMLElement;
  createRoot(root).render(               // ← ② ReactをDOMにマウント
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
});
```

**ポイント**:
- `enableMocking()` は **Promiseを返す**。MSWの初期化が完了してからReactのレンダリングが始まる
- これにより、Reactコンポーネントが発行するAPI呼び出しを**確実にMSWがインターセプト**できる
- `React.StrictMode` は開発時にコンポーネントを2回レンダリングしてバグを検出する（本番では無効）

---

## 3. Phase 2: MSW（モックAPI）の初期化

### ファイル: `src/testing/mocks/index.ts`

```typescript
import { env } from '@/config/env';

export const enableMocking = async () => {
  if (env.ENABLE_API_MOCKING) {             // ← .envの設定を確認
    const { worker } = await import('./browser');   // ← 動的インポート
    const { initializeDb } = await import('./db');
    initializeDb();                          // ← モックDBの初期化
    return worker.start();                   // ← Service Workerの起動
  }
};
```

**なぜ動的インポートなのか**: `ENABLE_API_MOCKING=false`（本番環境）のとき、MSW関連のコードがバンドルに含まれないようにするため。

---

### ファイル: `src/testing/mocks/browser.ts`

```typescript
import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
```

`setupWorker` はブラウザ上で **Service Worker** を登録し、`fetch` / `XMLHttpRequest` をインターセプトする。

---

### ファイル: `src/testing/mocks/db.ts` — インメモリDB

`@mswjs/data` を使ったモックデータベース。以下のモデルを持つ：

| モデル | フィールド |
|---|---|
| `user` | id, firstName, lastName, email, password, teamId, role, bio, createdAt |
| `team` | id, name, description, createdAt |
| `discussion` | id, title, body, authorId, teamId, createdAt |
| `comment` | id, body, authorId, discussionId, createdAt |

**永続化**: ブラウザでは `localStorage`（キー: `msw-db`）に保存。リロードしてもデータが残る。

---

### ファイル: `src/testing/mocks/handlers/` — APIハンドラー

各ファイルが REST API のエンドポイントをモックする：

| ファイル | エンドポイント | 処理内容 |
|---|---|---|
| `auth.ts` | `POST /auth/register` | ユーザー作成、パスワードハッシュ、JWT発行 |
| | `POST /auth/login` | 認証、JWT発行、Cookieセット |
| | `POST /auth/logout` | Cookie削除 |
| | `GET /auth/me` | Cookieからユーザー情報取得 |
| `discussions.ts` | `GET /discussions` | チーム内の議論一覧（ページネーション付き） |
| | `GET /discussions/:id` | 個別の議論取得 |
| | `POST /discussions` | 議論作成（Admin権限チェック） |
| | `PATCH /discussions/:id` | 議論更新 |
| | `DELETE /discussions/:id` | 議論削除 |
| `comments.ts` | `GET /comments` | コメント一覧（無限スクロール対応） |
| | `POST /comments` | コメント作成 |
| | `DELETE /comments/:id` | コメント削除（本人またはAdmin） |
| `users.ts` | `GET /users` | チームメンバー一覧 |
| | `PATCH /users/profile` | プロフィール更新 |
| | `DELETE /users/:id` | ユーザー削除（Admin） |
| `teams.ts` | `GET /teams` | チーム一覧 |

---

## 4. Phase 3: Provider チェーン（AppProvider）

### ファイル: `src/app/index.tsx`

```typescript
import { AppProvider } from './provider';
import { AppRouter } from './router';

export const App = () => {
  return (
    <AppProvider>
      <AppRouter />
    </AppProvider>
  );
};
```

シンプルだが重要。**Providerの中にRouterがある**ことで、ルーティング先のすべてのコンポーネントが各Providerにアクセスできる。

---

### ファイル: `src/app/provider.tsx` — Provider チェーンの詳解

```typescript
export const AppProvider = ({ children }: { children: React.ReactNode }) => {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: queryConfig })  // ← ①
  );

  return (
    <React.Suspense fallback={<Spinner size="xl" />}>        {/* ② */}
      <ErrorBoundary FallbackComponent={MainErrorFallback}>   {/* ③ */}
        <HelmetProvider>                                       {/* ④ */}
          <QueryClientProvider client={queryClient}>           {/* ⑤ */}
            <ReactQueryDevtools />                             {/* ⑥ */}
            <Notifications />                                  {/* ⑦ */}
            <AuthLoader renderLoading={<Spinner size="xl" />}> {/* ⑧ */}
              {children}                                       {/* ⑨ */}
            </AuthLoader>
          </QueryClientProvider>
        </HelmetProvider>
      </ErrorBoundary>
    </React.Suspense>
  );
};
```

各Providerの役割と**なぜこの順番なのか**：

| 順番 | Provider | 役割 | この位置にある理由 |
|:---:|---|---|---|
| ① | `QueryClient` 生成 | React Query のクライアントインスタンス | `useState` で1回だけ生成。再レンダリングでも同一インスタンスを維持 |
| ② | `Suspense` | 非同期コンポーネントの読み込み中にSpinnerを表示 | 最外側に置くことで、どの子コンポーネントの `lazy()` もキャッチできる |
| ③ | `ErrorBoundary` | 未キャッチのReactエラーをキャッチしてフォールバックUIを表示 | Suspenseの内側。ローディング中のエラーもキャッチできる |
| ④ | `HelmetProvider` | `<title>` や `<meta>` をReactコンポーネントから制御 | 他のProviderに依存しないので早い段階で配置 |
| ⑤ | `QueryClientProvider` | React Query のコンテキスト提供 | Auth（⑧）がReact Queryを使うため、Authより外側にある**必要がある** |
| ⑥ | `ReactQueryDevtools` | 開発時にReact Queryのキャッシュを可視化 | QueryClientProviderの内側に必要 |
| ⑦ | `Notifications` | トースト通知の表示領域 | Authより外側に置くことで、認証エラー時にも通知を表示できる |
| ⑧ | `AuthLoader` | **★ アプリ起動時に `GET /auth/me` を呼んで認証状態を確認** | QueryClientの内側（React Queryを使う）かつRouterの外側（認証結果でルーティングを制御する） |
| ⑨ | `{children}` | `<AppRouter />` が入る | 全Providerの内側。すべてのコンテキストにアクセス可能 |

---

### ファイル: `src/lib/react-query.ts` — React Query のグローバル設定

```typescript
export const queryConfig = {
  queries: {
    refetchOnWindowFocus: false,  // タブ切り替え時に自動再取得しない
    retry: false,                  // 失敗時に自動リトライしない
    staleTime: 1000 * 60,         // 1分間はキャッシュを「新鮮」とみなす
  },
} satisfies DefaultOptions;
```

**なぜこの設定か**:
- `refetchOnWindowFocus: false` → 業務システムでは画面切り替えのたびにAPIを叩くのは過剰
- `retry: false` → エラーは即座にユーザーに通知する方針
- `staleTime: 60秒` → 1分以内の再アクセスはキャッシュを使う

---

## 5. Phase 4: 認証チェック（AuthLoader）

### ファイル: `src/lib/auth.tsx`

このファイルが認証の**すべて**を統括する。`react-query-auth` ライブラリを使っている。

```typescript
const { useUser, useLogin, useLogout, useRegister, AuthLoader } =
  configureAuth({
    userFn: getUser,       // ← アプリ起動時に呼ばれる
    loginFn: loginFn,      // ← ログイン時に呼ばれる
    registerFn: registerFn,// ← 登録時に呼ばれる
    logoutFn: logoutFn,    // ← ログアウト時に呼ばれる
  });
```

#### アプリ起動時の認証フロー

```
AuthLoaderがマウントされる
    │
    ▼
userFn() = getUser() が呼ばれる
    │
    ▼
GET /auth/me （APIリクエスト）
    │
    ├─ 200 OK + ユーザー情報
    │   → user = { id, firstName, lastName, email, role, teamId, bio }
    │   → React Queryのキャッシュにユーザー情報を保存
    │   → {children}（=AppRouter）をレンダリング
    │
    └─ 401 Unauthorized
        → user = null
        → {children}（=AppRouter）をレンダリング
        → ProtectedRouteが未認証を検出 → /auth/login にリダイレクト
```

#### ProtectedRoute（認証ガード）

```typescript
export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const user = useUser();
  const location = useLocation();

  if (!user.data) {
    return (
      <Navigate
        to={paths.auth.login.getHref(location.pathname)}  // ← リダイレクト先にreturnURLを含める
        replace
      />
    );
  }

  return children;
};
```

**ポイント**: `location.pathname` を `redirectTo` パラメータとして渡すため、ログイン後に**元いたページに戻れる**。

---

### ファイル: `src/lib/api-client.ts` — Axiosクライアント

すべてのAPI呼び出しはこのAxiosインスタンスを経由する。

```typescript
export const api = Axios.create({
  baseURL: env.API_URL,
});

// リクエストインターセプター: 認証ヘッダーを付与
api.interceptors.request.use((config) => {
  config.headers.Accept = 'application/json';
  config.withCredentials = true;              // ← Cookieを自動送信
  return config;
});

// レスポンスインターセプター: データ抽出 + エラーハンドリング
api.interceptors.response.use(
  (response) => {
    return response.data;                      // ← response.data だけを返す（毎回.dataと書かなくて良い）
  },
  (error) => {
    const message = error.response?.data?.message || error.message;

    // エラー通知をZustandストアに追加
    useNotifications.getState().addNotification({
      type: 'error',
      title: 'Error',
      message,
    });

    if (error.response?.status === 401) {
      const searchParams = new URLSearchParams();
      const redirectTo = searchParams.get('redirectTo');
      window.location.href = paths.auth.login.getHref(redirectTo);  // ← 401なら強制ログアウト
    }

    return Promise.reject(error);
  }
);
```

**API呼び出しの完全な流れ**:

```
コンポーネント → useQuery/useMutation
    │
    ▼
feature内のapi関数 （例: getDiscussions）
    │
    ▼
api.get('/discussions')  ← src/lib/api-client.ts のAxiosインスタンス
    │
    ▼
リクエストインターセプター: Accept ヘッダー追加、Cookie添付
    │
    ▼
MSW（開発時）または 本番APIサーバー
    │
    ▼
レスポンスインターセプター:
    ├─ 成功: response.data を抽出して返す
    └─ 失敗: エラー通知を表示 + 401なら/auth/loginへリダイレクト
```

---

## 6. Phase 5: ルーティング（AppRouter）

### ファイル: `src/app/router.tsx`

```typescript
export const createAppRouter = (queryClient: QueryClient) =>
  createBrowserRouter([
    {
      path: paths.home.path,           // "/"
      lazy: async () => {
        const { LandingRoute } = await import('./routes/landing');
        return { Component: LandingRoute };
      },
    },
    {
      path: paths.auth.register.path,  // "/auth/register"
      lazy: async () => { /* RegisterRoute を遅延ロード */ },
    },
    {
      path: paths.auth.login.path,     // "/auth/login"
      lazy: async () => { /* LoginRoute を遅延ロード */ },
    },
    {
      path: paths.app.root.path,       // "/app"
      lazy: async () => {
        const { AppRoot } = await import('./routes/app/root');
        return { Component: AppRoot }; // ← ProtectedRouteでラップされている
      },
      children: [
        {
          path: paths.app.discussions.path,  // "/app/discussions"
          lazy: async () => {
            const module = await import('./routes/app/discussions/discussions');
            return {
              Component: module.default,
              loader: module.clientLoader?.(queryClient),  // ★ データの事前取得
            };
          },
        },
        {
          path: paths.app.discussion.path,   // "/app/discussions/:discussionId"
          lazy: async () => { /* DiscussionRoute + clientLoader */ },
        },
        // ... users, profile, dashboard
      ],
    },
  ]);
```

#### ルーティング全体像

```
URL                          コンポーネント              認証  ローダー
─────────────────────────────────────────────────────────────────────
/                            LandingRoute               不要  なし
/auth/login                  LoginRoute                 不要  なし
/auth/register               RegisterRoute              不要  なし
/app                         AppRoot → DashboardLayout   必要  なし
/app （子: ""）               DashboardRoute              必要  なし
/app/discussions             DiscussionsRoute            必要  ★ あり
/app/discussions/:id         DiscussionRoute             必要  ★ あり
/app/users                   UsersRoute                  必要  ★ あり（Admin）
/app/profile                 ProfileRoute                必要  なし
*                            NotFoundRoute               不要  なし
```

#### `lazy` と `clientLoader` のしくみ

**lazy（遅延ロード）**: ルートに到達するまでコンポーネントのJSファイルを読み込まない。これにより初期バンドルサイズが小さくなる。

**clientLoader（データ事前取得）**: ルート遷移時に**コンポーネントのレンダリングより先に**データを取得する。

```
ユーザーが /app/discussions をクリック
    │
    ├── ① lazy: discussions.tsx のJSを読み込む（コード分割）
    │
    ├── ② clientLoader: queryClient.ensureQueryData() を実行
    │       → キャッシュにデータがあれば即座に返す
    │       → なければ GET /discussions?page=1 を発行して待つ
    │
    └── ③ Component: データが準備できた状態で DiscussionsRoute をレンダリング
            → ローディングスピナーなしで画面が表示される！
```

---

### ファイル: `src/app/routes/landing.tsx` — ランディングページ

```typescript
export const LandingRoute = () => {
  const navigate = useNavigate();
  const user = useUser();

  const handleStart = () => {
    if (user.data) {
      navigate(paths.app.root.getHref());     // ログイン済み → /app
    } else {
      navigate(paths.auth.login.getHref());   // 未ログイン → /auth/login
    }
  };

  return (
    // "Get started" ボタン → handleStart()
  );
};
```

---

### ファイル: `src/app/routes/app/root.tsx` — 認証済みエリアのルート

```typescript
export default function AppRoot() {
  return (
    <ProtectedRoute>            {/* ← 未認証なら /auth/login へリダイレクト */}
      <DashboardLayout>         {/* ← サイドバー + ヘッダーのレイアウト */}
        <Suspense fallback={<Spinner />}>
          <Outlet />            {/* ← 子ルート（discussions等）がここに描画される */}
        </Suspense>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
```

---

## 7. Phase 6: レイアウト描画（DashboardLayout）

### ファイル: `src/components/layouts/dashboard-layout.tsx`

```
┌─────────────────────────────────────────────────────┐
│ DashboardLayout                                      │
│ ┌──────────┐ ┌────────────────────────────────────┐  │
│ │ Sidebar  │ │ Header                             │  │
│ │          │ │  [ハンバーガー]  [ローディングバー]    │  │
│ │ ■ Dash   │ │              [ユーザードロップダウン] │  │
│ │ ■ Discuss│ ├────────────────────────────────────┤  │
│ │ ■ Users  │ │ Main Content                       │  │
│ │  (Admin) │ │                                    │  │
│ │          │ │  <Outlet /> ← 子ルートの内容       │  │
│ │          │ │                                    │  │
│ └──────────┘ └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**サイドバーのナビゲーション生成**:

```typescript
const navigation = [
  { name: 'Dashboard', to: paths.app.root.getHref(), icon: Home },
  { name: 'Discussions', to: paths.app.discussions.getHref(), icon: Folder },
  checkAccess({ allowedRoles: [ROLES.ADMIN] }) &&
    { name: 'Users', to: paths.app.users.getHref(), icon: Users },
].filter(Boolean);
```

**ポイント**: `checkAccess` は `src/lib/authorization.tsx` の `useAuthorization()` フックから取得。ロールが `ADMIN` でないユーザーには「Users」リンクが**そもそも表示されない**。

**ユーザードロップダウンメニュー**:

```
[▼ ユーザー名]
├── Your Profile  → /app/profile
└── Sign Out      → useLogout() → POST /auth/logout → /auth/login
```

---

### ファイル: `src/components/layouts/content-layout.tsx`

各Featureページの共通レイアウト。

```typescript
export const ContentLayout = ({ children, title }: ContentLayoutProps) => {
  return (
    <>
      <Head title={title} />              {/* ← ブラウザタブのタイトルを変更 */}
      <div className="py-6">
        <h1 className="text-2xl">{title}</h1>
        <div className="py-6">{children}</div>
      </div>
    </>
  );
};
```

---

## 8. Phase 7: Feature画面の描画（Discussions一覧）

ここからが **feature/ ディレクトリ** の出番。画面表示に関わるファイルを呼ばれる順に追う。

### Step 1: ルートローダー（データの事前取得）

#### ファイル: `src/app/routes/app/discussions/discussions.tsx`

```typescript
// ① ルート遷移時に最初に実行される
export const clientLoader = (queryClient: QueryClient) => async () => {
  const url = new URL(window.location.href);
  const page = Number(url.searchParams.get('page') || 1);

  const query = getDiscussionsQueryOptions({ page });
  return (
    queryClient.getQueryData(query.queryKey) ??     // キャッシュにあればそれを返す
    (await queryClient.fetchQuery(query))            // なければAPIを叩く
  );
};

// ② ローダー完了後にレンダリング
export default function DiscussionsRoute() {
  const queryClient = useQueryClient();
  return (
    <ContentLayout title="Discussions">
      <CreateDiscussion />                    {/* ← 作成ボタン（Admin用） */}
      <DiscussionsList
        onDiscussionPrefetch={(id) => {       {/* ← ホバー時にコメントも先読み */}
          queryClient.prefetchQuery(getInfiniteCommentsQueryOptions(id));
        }}
      />
    </ContentLayout>
  );
}
```

---

### Step 2: API関数とReact Queryフック

#### ファイル: `src/features/discussions/api/get-discussions.ts`

```typescript
// ① 生のAPI呼び出し関数
export const getDiscussions = (page = 1): Promise<{ data: Discussion[]; meta: Meta }> => {
  return api.get('/discussions', { params: { page } });
};

// ② React Query のクエリ設定を生成する関数
export const getDiscussionsQueryOptions = ({ page }: { page?: number } = {}) => {
  return queryOptions({
    queryKey: page ? ['discussions', { page }] : ['discussions'],
    queryFn: () => getDiscussions(page),
  });
};

// ③ コンポーネントから使うカスタムフック
export const useDiscussions = ({ page, queryConfig }: UseDiscussionsOptions = {}) => {
  return useQuery({
    ...getDiscussionsQueryOptions({ page }),
    ...queryConfig,
  });
};
```

**この3層構造のメリット**:

| 層 | 関数 | 使う場面 |
|---|---|---|
| ① API関数 | `getDiscussions()` | テスト、ルートローダー |
| ② QueryOptions | `getDiscussionsQueryOptions()` | ルートローダーの `fetchQuery` / `prefetchQuery` |
| ③ カスタムフック | `useDiscussions()` | Reactコンポーネント内（自動再取得・キャッシュ管理） |

---

### Step 3: UIコンポーネント（一覧テーブル）

#### ファイル: `src/features/discussions/components/discussions-list.tsx`

```typescript
export const DiscussionsList = ({ onDiscussionPrefetch }: DiscussionsListProps) => {
  const [searchParams] = useSearchParams();
  const page = searchParams.get('page') ? +searchParams.get('page')! : 1;

  // ① React Queryフックでデータ取得（ローダーで既にキャッシュ済み）
  const discussionsQuery = useDiscussions({ page });

  // ② ローディング中
  if (discussionsQuery.isLoading) {
    return <Spinner size="lg" />;
  }

  const discussions = discussionsQuery.data?.data;
  const meta = discussionsQuery.data?.meta;

  // ③ データなし
  if (!discussions?.length) {
    return (
      <div>
        <ArchiveX />
        <p>No Discussions Found</p>
      </div>
    );
  }

  // ④ テーブル描画
  return (
    <Table
      data={discussions}
      columns={[
        { title: 'Title', field: 'title' },
        { title: 'Created At', field: 'createdAt',
          Cell: ({ entry }) => <span>{formatDate(entry.createdAt)}</span> },
        {
          title: '',
          field: 'id',
          Cell: ({ entry }) => (
            <Link
              to={paths.app.discussion.getHref(entry.id)}
              onMouseEnter={() => {
                // ★ ホバー時にprefetch → クリック時は即座に表示
                queryClient.prefetchQuery(getDiscussionQueryOptions(entry.id));
                onDiscussionPrefetch?.(entry.id);
              }}
            >
              View
            </Link>
          ),
        },
        {
          title: '',
          field: 'id',
          Cell: ({ entry }) => <DeleteDiscussion id={entry.id} />,
        },
      ]}
      pagination={{ totalPages: meta?.totalPages, currentPage: meta?.page, rootUrl: '' }}
    />
  );
};
```

**データフローの完全な流れ**:

```
URL遷移 /app/discussions?page=2
    │
    ▼
clientLoader が実行
    │ getDiscussionsQueryOptions({ page: 2 })
    │ queryClient.fetchQuery()
    │     → GET /discussions?page=2
    │     → レスポンス: { data: [...], meta: { page: 2, totalPages: 5 } }
    │     → キャッシュキー ['discussions', { page: 2 }] に保存
    │
    ▼
DiscussionsRoute がレンダリング
    │
    ▼
DiscussionsList がマウント
    │ useDiscussions({ page: 2 })
    │     → キャッシュヒット！ isLoading = false
    │
    ▼
Table コンポーネントがレンダリング
    │ ← ローディングスピナーなしで即座に表示
    │
    ▼
ユーザーが「View」リンクにホバー
    │ onMouseEnter → prefetchQuery(getDiscussionQueryOptions(entry.id))
    │     → バックグラウンドで GET /discussions/{id}
    │     → キャッシュに保存
    │
    ▼
ユーザーがクリック → /app/discussions/{id}
    │ clientLoader → キャッシュヒット！ → 即座に画面遷移
```

---

## 9. Phase 8: 個別画面の描画（Discussion詳細 + コメント）

### Step 1: ルートローダー（並列データ取得）

#### ファイル: `src/app/routes/app/discussions/discussion.tsx`

```typescript
export const clientLoader = (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const discussionId = params.discussionId as string;

    // ★ 議論データとコメントデータを並列で取得
    const discussionQuery = getDiscussionQueryOptions(discussionId);
    const commentsQuery = getInfiniteCommentsQueryOptions(discussionId);

    const promises = [
      queryClient.getQueryData(discussionQuery.queryKey) ??
        queryClient.fetchQuery(discussionQuery),
      queryClient.getQueryData(commentsQuery.queryKey) ??
        queryClient.fetchQuery(commentsQuery),
    ] as const;

    const [discussion, comments] = await Promise.all(promises);
    return { discussion, comments };
  };

export default function DiscussionRoute() {
  const params = useParams();
  const discussionId = params.discussionId as string;

  return (
    <ContentLayout title="Discussion">
      <DiscussionView discussionId={discussionId} />

      <ErrorBoundary fallback={<div>コメント読み込みエラー</div>}>
        <Suspense fallback={<Spinner />}>
          <Comments discussionId={discussionId} />
        </Suspense>
      </ErrorBoundary>
    </ContentLayout>
  );
}
```

**なぜコメントを `ErrorBoundary` で囲むのか**: コメントの取得に失敗しても、議論本体は表示し続けるため。部分的な障害に耐える設計。

---

### Step 2: 議論の表示

#### ファイル: `src/features/discussions/components/discussion-view.tsx`

```typescript
export const DiscussionView = ({ discussionId }: { discussionId: string }) => {
  const discussionQuery = useDiscussion({ discussionId });

  if (discussionQuery.isLoading) return <Spinner />;

  const discussion = discussionQuery.data?.data;
  if (!discussion) return null;

  return (
    <div>
      <span>{formatDate(discussion.createdAt)}</span>
      <span>{discussion.author?.firstName} {discussion.author?.lastName}</span>
      <MDPreview value={discussion.body} />         {/* ← MarkdownをHTML描画 */}
      <UpdateDiscussion discussionId={discussionId} /> {/* ← 編集ボタン（Admin） */}
    </div>
  );
};
```

---

### Step 3: コメント一覧（無限スクロール）

#### ファイル: `src/features/comments/api/get-comments.ts`

```typescript
export const getInfiniteCommentsQueryOptions = (discussionId: string) => {
  return infiniteQueryOptions({
    queryKey: ['comments', discussionId],
    queryFn: ({ pageParam = 1 }) => {
      return getComments({ discussionId, page: pageParam });
    },
    getNextPageParam: (lastPage) => {
      if (lastPage?.meta?.page === lastPage?.meta?.totalPages) return undefined;
      return lastPage.meta.page + 1;
    },
    initialPageParam: 1,
  });
};
```

**無限スクロールのしくみ**:

```
初回ロード
    │ pageParam = 1
    │ GET /comments?discussionId=123&page=1
    │ → { data: [comment1, comment2, ...], meta: { page: 1, totalPages: 3 } }
    │ → getNextPageParam: page(1) !== totalPages(3) → return 2
    │
    ▼
「Load More」ボタンクリック
    │ fetchNextPage() → pageParam = 2
    │ GET /comments?discussionId=123&page=2
    │ → 前ページのデータの後ろに追加
    │
    ▼
もう1回クリック
    │ pageParam = 3
    │ → page(3) === totalPages(3) → return undefined
    │ → hasNextPage = false → 「Load More」ボタン非表示
```

#### ファイル: `src/features/comments/components/comments-list.tsx`

```typescript
export const CommentsList = ({ discussionId }: CommentsListProps) => {
  const commentsQuery = useInfiniteComments({ discussionId });

  if (commentsQuery.isLoading) return <Spinner />;

  const comments = commentsQuery.data?.pages.flatMap((page) => page.data) ?? [];

  return (
    <>
      <ul>
        {comments.map((comment) => (
          <li key={comment.id}>
            <MDPreview value={comment.body} />
            <span>{comment.author.firstName}</span>
            <span>{formatDate(comment.createdAt)}</span>
            {/* ポリシーベースの削除ボタン表示 */}
            <Authorization policyCheck={POLICIES['comment:delete'](user, comment)}>
              <DeleteComment id={comment.id} discussionId={discussionId} />
            </Authorization>
          </li>
        ))}
      </ul>
      {commentsQuery.hasNextPage && (
        <Button onClick={() => commentsQuery.fetchNextPage()}>
          Load More
        </Button>
      )}
    </>
  );
};
```

---

## 10. Phase 9: データ変更操作（CRUD）の流れ

### Discussion作成の全フロー

#### ファイル: `src/features/discussions/components/create-discussion.tsx`

```typescript
export const CreateDiscussion = () => {
  const { addNotification } = useNotifications();

  const createDiscussionMutation = useCreateDiscussion({
    mutationConfig: {
      onSuccess: () => {
        addNotification({ type: 'success', title: 'Discussion Created' });
      },
    },
  });

  return (
    <Authorization allowedRoles={[ROLES.ADMIN]}>        {/* ← Admin権限チェック */}
      <FormDrawer
        isDone={createDiscussionMutation.isSuccess}       {/* ← 成功したら閉じる */}
        triggerButton={<Button><Plus /> Create Discussion</Button>}
        title="Create Discussion"
        submitButton={
          <Button
            form="create-discussion"
            type="submit"
            isLoading={createDiscussionMutation.isPending}
          />
        }
      >
        <Form
          id="create-discussion"
          onSubmit={(values) => {
            createDiscussionMutation.mutate({ data: values });  // ← ★ mutation実行
          }}
          schema={createDiscussionInputSchema}                  // ← Zodバリデーション
        >
          {({ register, formState }) => (
            <>
              <Input label="Title" error={formState.errors['title']}
                     registration={register('title')} />
              <Textarea label="Body" error={formState.errors['body']}
                        registration={register('body')} />
            </>
          )}
        </Form>
      </FormDrawer>
    </Authorization>
  );
};
```

---

#### ファイル: `src/features/discussions/api/create-discussion.ts`

```typescript
// Zodスキーマ: フロント側バリデーション
export const createDiscussionInputSchema = z.object({
  title: z.string().min(1, 'Required'),
  body: z.string().min(1, 'Required'),
});

// API呼び出し関数
export const createDiscussion = ({ data }: { data: CreateDiscussionInput }): Promise<Discussion> => {
  return api.post('/discussions', data);
};

// React Query Mutation フック
export const useCreateDiscussion = ({ mutationConfig }: UseCreateDiscussionOptions = {}) => {
  const queryClient = useQueryClient();
  const { onSuccess, ...restConfig } = mutationConfig || {};

  return useMutation({
    onSuccess: (...args) => {
      // ★ 一覧キャッシュを無効化 → 次に一覧を見た時に再取得される
      queryClient.invalidateQueries({
        queryKey: getDiscussionsQueryOptions().queryKey,
      });
      onSuccess?.(...args);  // コンポーネント側のonSuccess（通知表示）も呼ぶ
    },
    ...restConfig,
    mutationFn: createDiscussion,
  });
};
```

**作成操作の完全フロー**:

```
① ユーザーが「Create Discussion」ボタンをクリック
    │ Authorization: ADMIN ロールかチェック
    │
    ▼
② FormDrawer（スライドアウトパネル）が開く
    │ react-hook-form が初期化
    │
    ▼
③ ユーザーがTitle, Bodyを入力して「Submit」
    │
    ▼
④ Zodスキーマでバリデーション（クライアント側）
    │ createDiscussionInputSchema
    │ → title: 空なら "Required" エラー表示
    │ → body: 空なら "Required" エラー表示
    │
    ├── バリデーション失敗 → エラーメッセージ表示、送信しない
    │
    └── バリデーション成功
        │
        ▼
⑤ createDiscussionMutation.mutate({ data: values })
    │
    ▼
⑥ api.post('/discussions', data)
    │ → リクエストインターセプター: Cookie添付
    │ → POST /discussions (MSW or 本番API)
    │ → レスポンスインターセプター: response.data 抽出
    │
    ├── 失敗 → エラー通知（レスポンスインターセプターで自動表示）
    │
    └── 成功
        │
        ▼
⑦ onSuccess コールバック
    │ ├── queryClient.invalidateQueries(['discussions'])
    │ │       → キャッシュを「stale」に → 次に一覧表示時に再取得
    │ │
    │ └── addNotification({ type: 'success', title: 'Discussion Created' })
    │         → トースト通知表示
    │
    ▼
⑧ isDone={true} → FormDrawer が閉じる
    │
    ▼
⑨ 一覧画面が再レンダリング
    │ useDiscussions() → キャッシュが stale → 自動で GET /discussions
    │ → 新しいDiscussionが一覧に表示される
```

---

### CRUD操作のキャッシュ戦略まとめ

| 操作 | API | 成功時のキャッシュ操作 | 理由 |
|---|---|---|---|
| **一覧取得** | `GET /discussions?page=X` | キャッシュに保存 | 通常のクエリ |
| **個別取得** | `GET /discussions/:id` | キャッシュに保存 | ホバーでprefetch |
| **作成** | `POST /discussions` | 一覧を **invalidate** | 新しいアイテムが一覧に必要 |
| **更新** | `PATCH /discussions/:id` | 個別を **refetch** | 変更後の最新を取得 |
| **削除** | `DELETE /discussions/:id` | 一覧を **invalidate** | 削除されたアイテムを一覧から除去 |

**invalidate vs refetch の使い分け**:
- `invalidateQueries` → キャッシュを「期限切れ」にする。**次に使われた時**に再取得
- `refetchQueries` → **即座に**再取得を実行

---

## 11. 横断的関心事（エラーハンドリング・通知・認可）

### エラーハンドリングの3段構え

```
レベル1: APIインターセプター（src/lib/api-client.ts）
    │ → すべてのAPIエラーをキャッチ
    │ → トースト通知を自動表示
    │ → 401なら /auth/login へリダイレクト
    │
レベル2: 画面単位のErrorBoundary（各ルートファイル）
    │ → コメント読み込み失敗 → 議論本体は表示継続
    │ → 部分的な障害に耐える
    │
レベル3: グローバルErrorBoundary（src/app/provider.tsx）
    │ → すべての未キャッチエラーをキャッチ
    │ → "Ooops, something went wrong" + リフレッシュボタン
```

---

### 通知システム（Zustand ストア）

#### ファイル: `src/components/ui/notifications/notifications-store.ts`

```typescript
type Notification = {
  id: string;
  type: 'info' | 'warning' | 'success' | 'error';
  title: string;
  message?: string;
};

export const useNotifications = create<NotificationsStore>((set) => ({
  notifications: [],
  addNotification: (notification) =>
    set((state) => ({
      notifications: [...state.notifications, { id: nanoid(), ...notification }],
    })),
  dismissNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),
}));
```

**なぜZustandなのか**: React Queryの外（Axiosインターセプター等）からも `useNotifications.getState().addNotification()` で直接操作できるため。Reactの `useContext` ではコンポーネント外から呼べない。

---

### 認可（Authorization）

#### ファイル: `src/lib/authorization.tsx`

2つのパターンがある:

**パターン1: ロールベース（UI要素の表示/非表示）**

```typescript
<Authorization allowedRoles={[ROLES.ADMIN]}>
  <CreateDiscussion />    {/* ADMINにだけ表示 */}
</Authorization>
```

**パターン2: ポリシーベース（ビジネスルール）**

```typescript
export const POLICIES = {
  'comment:delete': (user: User, comment: Comment) => {
    if (user.role === 'ADMIN') return true;          // 管理者は何でも消せる
    if (user.role === 'USER' && comment.author?.id === user.id) return true;  // 自分のコメントは消せる
    return false;
  },
};

// 使い方
<Authorization policyCheck={POLICIES['comment:delete'](user, comment)}>
  <DeleteComment />       {/* 権限がある人にだけ表示 */}
</Authorization>
```

---

## 12. ファイル一覧と役割マップ

### 起動時に呼ばれるファイル（呼び出し順）

| 順番 | ファイルパス | 役割 |
|:---:|---|---|
| 1 | `index.html` | HTMLエントリーポイント |
| 2 | `src/main.tsx` | React初期化 |
| 3 | `src/testing/mocks/index.ts` | MSW初期化判定 |
| 4 | `src/testing/mocks/browser.ts` | Service Worker登録 |
| 5 | `src/testing/mocks/db.ts` | モックDB初期化 |
| 6 | `src/app/index.tsx` | App コンポーネント |
| 7 | `src/app/provider.tsx` | Provider チェーン |
| 8 | `src/lib/react-query.ts` | QueryClient 設定 |
| 9 | `src/lib/auth.tsx` | AuthLoader → GET /auth/me |
| 10 | `src/lib/api-client.ts` | Axios でAPI呼び出し |
| 11 | `src/app/router.tsx` | ルーティング設定 |
| 12 | `src/config/paths.ts` | ルートパス定義 |
| 13 | `src/app/routes/landing.tsx` | ランディングページ（初回表示） |

### Feature ディレクトリ構造

```
src/features/
├── auth/                          ← 認証（ログイン/登録フォーム）
│   └── components/
│       ├── login-form.tsx              Zodスキーマ + react-hook-form
│       └── register-form.tsx           チーム選択 + バリデーション
│
├── discussions/                   ← 議論（フルCRUD）
│   ├── api/
│   │   ├── get-discussions.ts          一覧取得（ページネーション）
│   │   ├── get-discussion.ts           個別取得
│   │   ├── create-discussion.ts        作成 + Zodスキーマ
│   │   ├── update-discussion.ts        更新 + Zodスキーマ
│   │   └── delete-discussion.ts        削除
│   └── components/
│       ├── discussions-list.tsx         テーブル表示 + prefetch
│       ├── discussion-view.tsx          詳細表示 + Markdown
│       ├── create-discussion.tsx        FormDrawer + Authorization
│       ├── update-discussion.tsx        既存値の事前読み込み
│       └── delete-discussion.tsx        確認ダイアログ
│
├── comments/                      ← コメント（無限スクロール）
│   ├── api/
│   │   ├── get-comments.ts             infiniteQuery（ページネーション）
│   │   ├── create-comment.ts           作成
│   │   └── delete-comment.ts           削除（ポリシーベース認可）
│   └── components/
│       ├── comments.tsx                全体ラッパー
│       ├── comments-list.tsx           一覧 + Load More
│       ├── create-comment.tsx          入力フォーム
│       └── delete-comment.tsx          確認ダイアログ
│
├── users/                         ← ユーザー管理（Admin用）
│   ├── api/
│   │   ├── get-users.ts                一覧取得
│   │   ├── update-profile.ts           プロフィール更新
│   │   └── delete-user.ts              ユーザー削除
│   └── components/
│       ├── users-list.tsx              テーブル表示
│       ├── update-profile.tsx          プロフィール編集フォーム
│       └── delete-user.tsx             確認ダイアログ
│
└── teams/                         ← チーム（読み取り専用）
    └── api/
        └── get-teams.ts                登録時のドロップダウン用
```

### 共通インフラ

| ディレクトリ | ファイル | 役割 |
|---|---|---|
| `src/lib/` | `api-client.ts` | Axios インスタンス + インターセプター |
| | `auth.tsx` | 認証の全機能（useUser, useLogin等） |
| | `authorization.tsx` | ロール/ポリシーベースの認可 |
| | `react-query.ts` | QueryClient のグローバル設定 |
| `src/config/` | `paths.ts` | 全ルートパスの定義 |
| | `env.ts` | 環境変数のZodバリデーション |
| `src/components/ui/` | `form/`, `table/`, `dialog/` 等 | 共通UIコンポーネント |
| `src/components/layouts/` | `dashboard-layout.tsx` | サイドバー + ヘッダー |
| | `content-layout.tsx` | ページ共通レイアウト |
| `src/components/errors/` | `main.tsx` | グローバルエラーフォールバック |
| `src/hooks/` | `use-disclosure.ts` | 開閉状態のフック |
| `src/utils/` | `format.ts` | 日付フォーマット（dayjs） |
| | `cn.ts` | Tailwind CSSユーティリティ |
| `src/types/` | `api.ts` | APIエンティティの型定義 |
| `src/testing/` | `mocks/`, `test-utils.tsx` | MSW + テストヘルパー |

### 使用ライブラリと役割

| ライブラリ | 用途 | 使用箇所 |
|---|---|---|
| **@tanstack/react-query** | サーバー状態管理（データ取得・キャッシュ・同期） | 全API呼び出し |
| **axios** | HTTPクライアント | `src/lib/api-client.ts` |
| **zod** | スキーマバリデーション + 型推論 | 各feature の api/ ファイル |
| **react-hook-form** | フォーム状態管理 | 作成・更新フォーム |
| **@hookform/resolvers** | zodスキーマとreact-hook-formの接続 | Formコンポーネント |
| **react-router** | ルーティング + データローダー | `src/app/router.tsx` |
| **react-query-auth** | 認証状態のReact Query統合 | `src/lib/auth.tsx` |
| **zustand** | クライアント状態管理 | 通知ストア |
| **react-error-boundary** | エラーバウンダリ | Provider + 個別画面 |
| **react-helmet-async** | HTML headタグの管理 | ContentLayout |
| **msw** | APIモック（開発・テスト） | `src/testing/mocks/` |
| **@mswjs/data** | インメモリDB（モックデータ） | `src/testing/mocks/db.ts` |
| **Radix UI** | アクセシブルなUIプリミティブ | Dialog, Dropdown, Switch等 |
| **Tailwind CSS** | ユーティリティファーストCSS | 全コンポーネント |
| **dayjs** | 日付フォーマット | `src/utils/format.ts` |
| **marked** | MarkdownをHTMLに変換 | MDPreviewコンポーネント |
| **dompurify** | XSS対策（HTMLサニタイズ） | MDPreviewコンポーネント |
