# Bulletproof React 完全解説ガイド

> **対象者**: React/TypeScript/Viteのライブラリ名は知っているが、実際にどう書くのか・なぜそう書くのかを理解したい開発者
>
> **本書の元**: [alan2207/bulletproof-react](https://github.com/alan2207/bulletproof-react) の `apps/react-vite` 実装を一字一句解説

---

## 目次

1. [全体像 — このアプリケーションは何をしているのか](#1-全体像)
2. [ディレクトリ構造 — なぜこの配置なのか](#2-ディレクトリ構造)
3. [起動の流れ — main.tsx からアプリが表示されるまで](#3-起動の流れ)
4. [設定ファイル群 — 各設定の意味と理由](#4-設定ファイル群)
5. [app/ — アプリケーション層の詳解](#5-app-アプリケーション層)
6. [features/ — Feature-based Architectureの核心](#6-features-featureの構造)
7. [components/ — 共通UIコンポーネント設計](#7-components-共通ui)
8. [lib/ — ライブラリ統合層](#8-lib-ライブラリ統合層)
9. [状態管理 — 4種類の状態をどう分類するか](#9-状態管理)
10. [API層 — データ取得の設計パターン](#10-api層)
11. [フォームとバリデーション](#11-フォームとバリデーション)
12. [ルーティングとコード分割](#12-ルーティングとコード分割)
13. [認証・認可](#13-認証認可)
14. [エラーハンドリング](#14-エラーハンドリング)
15. [セキュリティ](#15-セキュリティ)
16. [テスティング](#16-テスティング)
17. [プロジェクト標準 — チーム開発の規約](#17-プロジェクト標準)
18. [ESLint による構造の強制](#18-eslintによる構造の強制)

---

## 1. 全体像

### このアプリケーションが解決する問題

Reactは自由度が高い。自由度が高いということは「正解がない」ということでもある。結果、プロジェクトごとにバラバラな構造が生まれ、規模が大きくなると破綻する。

Bulletproof Reactは「大規模でも破綻しないReactアプリの構造とは何か」に対する一つの回答である。

### サンプルアプリの仕様

- ユーザーが**チーム**を作成し、他のユーザーが参加できる
- チーム内で**ディスカッション**を作成し、**コメント**でやりとりする
- ロール（ADMIN / USER）による権限制御がある

### データモデル

```
User (ADMIN | USER)
  └── belongs to → Team
                      └── has many → Discussion
                                        └── has many → Comment
```

---

## 2. ディレクトリ構造

```
src/
├── app/                   # アプリケーション層（ルーティング・プロバイダー・ページ）
│   ├── index.tsx          #   Appコンポーネント（エントリポイント）
│   ├── provider.tsx       #   グローバルプロバイダーの集約
│   ├── router.tsx         #   ルーティング定義
│   └── routes/            #   ページコンポーネント
│       ├── landing.tsx
│       ├── not-found.tsx
│       ├── auth/
│       │   ├── login.tsx
│       │   └── register.tsx
│       └── app/
│           ├── root.tsx
│           ├── dashboard.tsx
│           ├── profile.tsx
│           ├── users.tsx
│           └── discussions/
│               ├── discussions.tsx
│               └── discussion.tsx
│
├── components/            # アプリ全体で共有されるUIコンポーネント
│   ├── errors/            #   エラー表示
│   ├── layouts/           #   レイアウト（認証・ダッシュボード・コンテンツ）
│   ├── seo/               #   SEOメタタグ管理
│   └── ui/                #   UIコンポーネントライブラリ
│       ├── button/
│       ├── dialog/
│       ├── drawer/
│       ├── dropdown/
│       ├── form/
│       ├── link/
│       ├── md-preview/
│       ├── notifications/
│       ├── spinner/
│       └── table/
│
├── config/                # アプリ設定
│   ├── env.ts             #   環境変数のバリデーション
│   └── paths.ts           #   ルートパス定義
│
├── features/              # ★ Feature-based モジュール
│   ├── auth/              #   認証
│   │   └── components/
│   ├── comments/          #   コメント
│   │   ├── api/
│   │   └── components/
│   ├── discussions/       #   ディスカッション
│   │   ├── api/
│   │   └── components/
│   ├── teams/             #   チーム
│   │   └── api/
│   └── users/             #   ユーザー管理
│       ├── api/
│       └── components/
│
├── hooks/                 # アプリ全体で共有されるカスタムフック
│   └── use-disclosure.ts
│
├── lib/                   # 外部ライブラリの設定・ラッパー
│   ├── api-client.ts      #   Axiosの設定
│   ├── auth.tsx           #   認証ロジック
│   ├── authorization.tsx  #   認可（RBAC/PBAC）
│   └── react-query.ts    #   React Queryの設定
│
├── testing/               # テストユーティリティ・モック
│   ├── mocks/
│   ├── data-generators.ts
│   ├── setup-tests.ts
│   └── test-utils.tsx
│
├── types/                 # アプリ全体で共有される型定義
│   └── api.ts
│
├── utils/                 # アプリ全体で共有されるユーティリティ
│   ├── cn.ts              #   Tailwind CSSクラス結合
│   └── format.ts          #   日付フォーマット
│
├── index.css              # グローバルスタイル（Tailwind CSS）
└── main.tsx               # エントリポイント
```

### なぜこの構造なのか — 3つの設計原則

#### 原則1: コードの流れは一方向（Unidirectional）

```
shared（components, hooks, lib, types, utils）
    ↑ import可能
features（auth, discussions, comments, users, teams）
    ↑ import可能
app（routes, provider, router）
```

**下から上には import できない。** つまり：

- `features/discussions/` は `components/ui/button/` を import できる ✅
- `components/ui/button/` は `features/discussions/` を import できない ❌
- `features/discussions/` は `features/comments/` を import できない ❌（feature間の横断禁止）

**なぜ**: 依存関係が一方向であれば、あるモジュールを変更した時の影響範囲が予測可能になる。feature間のimportを許可すると、最終的にすべてのfeatureが相互依存し、1つの変更が全体に波及する「ビッグボールオブマッド」になる。

#### 原則2: Featureは自己完結する

各featureフォルダは、そのfeatureに必要なapi/components/hooks/types/utilsをすべて内包する。「ディスカッション機能を修正したい」とき、開発者は `features/discussions/` の中だけを見ればよい。

**なぜ**: 技術種別（components/, hooks/, utils/）ではなくfeature単位で分割すると、関連するコードの物理的距離が近くなる（コロケーション）。人間の認知負荷が下がり、チーム開発でのコンフリクトも減る。

#### 原則3: Featureの組み合わせはapp層で行う

feature同士を組み合わせる（ディスカッション詳細ページにコメント一覧を表示する等）のは、`app/routes/` の責務である。featureが互いを知らないままapp層が統合するため、feature間の結合が生まれない。

---

## 3. 起動の流れ

### Step 1: `main.tsx` — すべてはここから始まる

```typescript
import * as React from 'react';
import { createRoot } from 'react-dom/client';

import './index.css';
import { App } from './app';
import { enableMocking } from './testing/mocks';

const root = document.getElementById('root');
if (!root) throw new Error('No root element found');

enableMocking().then(() => {
  createRoot(root).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
});
```

**一行ずつ解説:**

| 行 | 何をしているか | なぜそうするのか |
|---|---|---|
| `import './index.css'` | Tailwind CSSのグローバルスタイルを読み込む | Viteはimportされたcssを自動的にHTMLに挿入する |
| `import { App } from './app'` | `app/index.tsx` から `App` コンポーネントを取得 | `./app` と書くとViteが自動的に `./app/index.tsx` を解決する |
| `import { enableMocking }` | MSW（Mock Service Worker）の初期化関数を取得 | 開発中にAPIをモックするため |
| `document.getElementById('root')` | HTMLの `<div id="root">` を取得 | Reactがレンダリングする対象DOM要素 |
| `if (!root) throw new Error(...)` | root要素がなければエラーをスロー | 無言の失敗を防ぐ。デバッグが容易になる |
| `enableMocking().then(...)` | MSWの初期化完了を待ってからレンダリング | MSWが準備完了する前にAPI呼び出しが走ると、モックが効かないため |
| `<React.StrictMode>` | React開発モードの厳格チェックを有効化 | 副作用の検知、非推奨APIの警告を得るため |

**ポイント**: `enableMocking` がPromiseを返すのは、MSWのService Workerの登録が非同期だから。この `then` がないと、初回のAPI呼び出しがモックされずに失敗する可能性がある。

### Step 2: `app/index.tsx` — Appコンポーネント

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

**設計意図**: Appコンポーネントの役割は2つだけ — プロバイダーでラップし、ルーターを描画する。これ以上のロジックをここに書かない。関心事の分離を徹底するため、プロバイダーの設定は `provider.tsx` に、ルーティングの設定は `router.tsx` に委譲している。

### Step 3: `app/provider.tsx` — グローバルプロバイダーの集約

```typescript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import * as React from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { HelmetProvider } from 'react-helmet-async';

import { MainErrorFallback } from '@/components/errors/main';
import { Notifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { AuthLoader } from '@/lib/auth';
import { queryConfig } from '@/lib/react-query';

type AppProviderProps = {
  children: React.ReactNode;
};

export const AppProvider = ({ children }: AppProviderProps) => {
  const [queryClient] = React.useState(
    () =>
      new QueryClient({
        defaultOptions: queryConfig,
      }),
  );

  return (
    <React.Suspense
      fallback={
        <div className="flex h-screen w-screen items-center justify-center">
          <Spinner size="xl" />
        </div>
      }
    >
      <ErrorBoundary FallbackComponent={MainErrorFallback}>
        <HelmetProvider>
          <QueryClientProvider client={queryClient}>
            {import.meta.env.DEV && <ReactQueryDevtools />}
            <Notifications />
            <AuthLoader
              renderLoading={() => (
                <div className="flex h-screen w-screen items-center justify-center">
                  <Spinner size="xl" />
                </div>
              )}
            >
              {children}
            </AuthLoader>
          </QueryClientProvider>
        </HelmetProvider>
      </ErrorBoundary>
    </React.Suspense>
  );
};
```

**プロバイダーの入れ子構造と、その順序の理由:**

```
React.Suspense          ← 最外側: 遅延読み込みの待機画面を表示
  └── ErrorBoundary     ← エラーが発生したらフォールバックUIを表示
    └── HelmetProvider  ← <head>タグのメタ情報を管理
      └── QueryClientProvider  ← React Queryのキャッシュ・データ取得を提供
        ├── ReactQueryDevtools  ← 開発時のみ表示されるデバッグツール
        ├── Notifications       ← トースト通知の表示領域
        └── AuthLoader          ← ユーザー情報の取得完了を待つ
          └── {children}        ← ここでルーターが描画される
```

| プロバイダー | 役割 | なぜこの順序か |
|---|---|---|
| `React.Suspense` | 遅延ロード中の待機UI | `lazy()` で読み込まれるコンポーネントの親に必要 |
| `ErrorBoundary` | 致命的エラーのキャッチ | 最も外側に近い場所でキャッチしないと白画面になる |
| `HelmetProvider` | SEOメタタグ管理 | 他のプロバイダーとの依存なし。早めに配置 |
| `QueryClientProvider` | サーバーデータ管理 | AuthLoaderがReact Queryを使うため、これより外に置く |
| `AuthLoader` | 認証状態の初期化 | ユーザー情報が揃ってから子コンポーネントを描画するため、最も内側 |

**なぜ `useState` で QueryClient を作るのか:**

```typescript
const [queryClient] = React.useState(
  () => new QueryClient({ defaultOptions: queryConfig }),
);
```

`useState` の初期化関数を使うと、`new QueryClient(...)` はコンポーネントのライフタイムで**1回だけ**実行される。直接 `const queryClient = new QueryClient(...)` と書くと、再レンダリングのたびに新しいインスタンスが生成され、キャッシュがリセットされてしまう。

---

## 4. 設定ファイル群

### `config/env.ts` — 環境変数のバリデーション

```typescript
import * as z from 'zod';

const createEnv = () => {
  const EnvSchema = z.object({
    API_URL: z.string(),
    ENABLE_API_MOCKING: z
      .string()
      .refine((s) => s === 'true' || s === 'false')
      .transform((s) => s === 'true')
      .optional(),
    APP_URL: z.string().optional().default('http://localhost:3000'),
    APP_MOCK_API_PORT: z.string().optional().default('8080'),
  });

  const envVars = Object.entries(import.meta.env).reduce<
    Record<string, string>
  >((acc, curr) => {
    const [key, value] = curr;
    if (key.startsWith('VITE_APP_')) {
      acc[key.replace('VITE_APP_', '')] = value;
    }
    return acc;
  }, {});

  const parsedEnv = EnvSchema.safeParse(envVars);

  if (!parsedEnv.success) {
    throw new Error(
      `Invalid env provided.
The following variables are missing or invalid:
${Object.entries(parsedEnv.error.flatten().fieldErrors)
  .map(([k, v]) => `- ${k}: ${v}`)
  .join('\n')}
`,
    );
  }

  return parsedEnv.data;
};

export const env = createEnv();
```

**なぜZodで環境変数をバリデーションするのか:**

1. **起動時に失敗する**: 環境変数が不足していたら、API呼び出し時ではなくアプリ起動時にエラーになる。問題の発見が早い。
2. **型安全**: `env.API_URL` は `string` 型として推論される。`process.env.VITE_APP_API_URL` は `string | undefined` であり、毎回undefinedチェックが必要になる。
3. **変換**: `ENABLE_API_MOCKING` は環境変数では文字列 `"true"` だが、Zodの `.transform()` で `boolean` に変換される。使う側は `if (env.ENABLE_API_MOCKING)` と自然に書ける。

**`VITE_APP_` プレフィックスの剥がし方**: Viteでは `VITE_` で始まる環境変数のみがクライアントに公開される。このプロジェクトでは `VITE_APP_` を使い、`reduce` でプレフィックスを除去して短い名前（`API_URL`）で扱えるようにしている。

### `config/paths.ts` — ルートパスの一元管理

```typescript
export const paths = {
  home: {
    path: '/',
    getHref: () => '/',
  },

  auth: {
    register: {
      path: '/auth/register',
      getHref: (redirectTo?: string | null | undefined) =>
        `/auth/register${redirectTo ? `?redirectTo=${encodeURIComponent(redirectTo)}` : ''}`,
    },
    login: {
      path: '/auth/login',
      getHref: (redirectTo?: string | null | undefined) =>
        `/auth/login${redirectTo ? `?redirectTo=${encodeURIComponent(redirectTo)}` : ''}`,
    },
  },

  app: {
    root: {
      path: '/app',
      getHref: () => '/app',
    },
    dashboard: {
      path: '',
      getHref: () => '/app',
    },
    discussion: {
      path: 'discussions/:discussionId',
      getHref: (id: string) => `/app/discussions/${id}`,
    },
    // ...
  },
} as const;
```

**なぜパスを一元管理するのか:**

1. **path と getHref の分離**: `path` はルーター定義用（`discussions/:discussionId` のようにパラメータプレースホルダを含む）。`getHref` はリンク生成用（`/app/discussions/abc123` のように実際の値が入る）。
2. **型安全**: `as const` で全リテラルが固定されるため、タイポがコンパイルエラーになる。
3. **一箇所の変更で全体に反映**: URLの変更時に `paths.ts` だけ修正すればよい。アプリ全体からこの定数を参照しているため、漏れがない。

### `vite.config.ts` — ビルド設定

```typescript
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import viteTsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
  base: './',
  plugins: [react(), viteTsconfigPaths()],
  server: { port: 3000 },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/testing/setup-tests.ts',
    exclude: ['**/node_modules/**', '**/e2e/**'],
  },
});
```

| 設定 | 意味 | なぜ |
|---|---|---|
| `base: './'` | 相対パスでアセットを参照 | サブディレクトリにデプロイしても動作するため |
| `react()` | React JSXの変換 | `.tsx` ファイルのJSX構文をJavaScriptに変換 |
| `viteTsconfigPaths()` | `tsconfig.json` のパスエイリアスをViteに反映 | `@/components/...` のようなimportをViteが解決できるようになる |
| `globals: true` | Vitestのグローバル関数 | `import { describe, it } from 'vitest'` を毎回書かなくて済む |
| `environment: 'jsdom'` | テスト環境をブラウザ模擬 | DOMのAPI（`document.querySelector`等）がテストで使える |

### `tsconfig.json` — TypeScript設定

```json
{
  "compilerOptions": {
    "target": "ESNext",
    "strict": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

| 設定 | 意味 | なぜ |
|---|---|---|
| `"strict": true` | 厳密な型チェック | `null` チェック漏れや暗黙の `any` をコンパイル時に検出 |
| `"moduleResolution": "bundler"` | Vite互換のモジュール解決 | Viteのimport解決と一致させる |
| `"jsx": "react-jsx"` | 新しいJSXランタイム | `import React from 'react'` が各ファイルで不要になる |
| `"paths": { "@/*": ["./src/*"] }` | パスエイリアス | `../../../components/button` の代わりに `@/components/button` と書ける |

---

## 5. app/ — アプリケーション層

### app/router.tsx — ルーティング定義

```typescript
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';

import { paths } from '@/config/paths';
import { ProtectedRoute } from '@/lib/auth';

import {
  default as AppRoot,
  ErrorBoundary as AppRootErrorBoundary,
} from './routes/app/root';

const convert = (queryClient: QueryClient) => (m: any) => {
  const { clientLoader, clientAction, default: Component, ...rest } = m;
  return {
    ...rest,
    loader: clientLoader?.(queryClient),
    action: clientAction?.(queryClient),
    Component,
  };
};

export const createAppRouter = (queryClient: QueryClient) =>
  createBrowserRouter([
    {
      path: paths.home.path,
      lazy: () => import('./routes/landing').then(convert(queryClient)),
    },
    {
      path: paths.auth.login.path,
      lazy: () => import('./routes/auth/login').then(convert(queryClient)),
    },
    {
      path: paths.app.root.path,
      element: (
        <ProtectedRoute>
          <AppRoot />
        </ProtectedRoute>
      ),
      ErrorBoundary: AppRootErrorBoundary,
      children: [
        {
          path: paths.app.discussions.path,
          lazy: () =>
            import('./routes/app/discussions/discussions').then(
              convert(queryClient),
            ),
        },
        // ... 他のルート
      ],
    },
    {
      path: '*',
      lazy: () => import('./routes/not-found').then(convert(queryClient)),
    },
  ]);
```

**重要な設計判断を一つずつ:**

**1. `lazy: () => import(...)` — コード分割**

ルートごとに `lazy()` で動的importを使っている。これにより、各ページのコードはユーザーがそのページに遷移した時にはじめてダウンロードされる。初回の `bundle.js` が小さくなり、First Contentful Paintが高速化される。

**2. `convert` 関数 — loaderパターン**

```typescript
const convert = (queryClient: QueryClient) => (m: any) => {
  const { clientLoader, clientAction, default: Component, ...rest } = m;
  return {
    ...rest,
    loader: clientLoader?.(queryClient),
    action: clientAction?.(queryClient),
    Component,
  };
};
```

各ルートファイル（例: `discussions.tsx`）が `clientLoader` をexportしている場合、`convert` がそれを React Routerの `loader` に変換する。`queryClient` を注入することで、ページ遷移時にデータをプリフェッチできる。

**3. `ProtectedRoute` — 認証ガード**

`/app` 配下のルートはすべて `ProtectedRoute` でラップされている。ログインしていないユーザーはログインページにリダイレクトされる。ネスト構造（`children`）により、`/app/*` のすべての子ルートに認証が適用される。

### app/routes/app/discussions/discussions.tsx — ルートファイルの例

```typescript
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import { LoaderFunctionArgs } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { getInfiniteCommentsQueryOptions } from '@/features/comments/api/get-comments';
import { getDiscussionsQueryOptions } from '@/features/discussions/api/get-discussions';
import { CreateDiscussion } from '@/features/discussions/components/create-discussion';
import { DiscussionsList } from '@/features/discussions/components/discussions-list';

export const clientLoader =
  (queryClient: QueryClient) =>
  async ({ request }: LoaderFunctionArgs) => {
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') || 1);

    const query = getDiscussionsQueryOptions({ page });

    return (
      queryClient.getQueryData(query.queryKey) ??
      (await queryClient.fetchQuery(query))
    );
  };

const DiscussionsRoute = () => {
  const queryClient = useQueryClient();
  return (
    <ContentLayout title="Discussions">
      <div className="flex justify-end">
        <CreateDiscussion />
      </div>
      <div className="mt-4">
        <DiscussionsList
          onDiscussionPrefetch={(id) => {
            queryClient.prefetchInfiniteQuery(
              getInfiniteCommentsQueryOptions(id),
            );
          }}
        />
      </div>
    </ContentLayout>
  );
};

export default DiscussionsRoute;
```

**このファイルの役割**: ルートファイルは**featureの組み立て場所**である。自分自身にはビジネスロジックもUI部品も持たず、feature（discussions, comments）のコンポーネントを配置し、レイアウト（ContentLayout）で包んでいるだけ。

**clientLoader — データプリフェッチの仕組み:**

```typescript
queryClient.getQueryData(query.queryKey)    // キャッシュにあればそれを返す
  ?? (await queryClient.fetchQuery(query))  // なければフェッチ
```

ページ遷移**前に** React Routerのloaderが実行されるため、ユーザーがページに到達した瞬間にはデータが準備済みになる。キャッシュがあればネットワークリクエストも不要。

**onDiscussionPrefetch — ホバー時の先読み:**

リスト内のディスカッションにマウスを乗せた時点で、そのコメントデータを先読みする。ユーザーがクリックして詳細ページに遷移した時にはデータがキャッシュ済みになり、即座に表示される。

---

## 6. features/ — Featureの構造

### 1つのFeatureの完全な構造（discussionsの例）

```
features/discussions/
├── api/                          # API呼び出しとReact Queryフック
│   ├── get-discussions.ts        #   一覧取得（Query）
│   ├── get-discussion.ts         #   詳細取得（Query）
│   ├── create-discussion.ts      #   作成（Mutation）
│   ├── update-discussion.ts      #   更新（Mutation）
│   └── delete-discussion.ts      #   削除（Mutation）
└── components/                   # feature固有のUIコンポーネント
    ├── discussions-list.tsx       #   一覧表示
    ├── discussion-view.tsx        #   詳細表示
    ├── create-discussion.tsx      #   作成フォーム
    ├── update-discussion.tsx      #   更新フォーム
    └── delete-discussion.tsx      #   削除確認
```

**全featureが `api/` と `components/` を持つ必要はない。** そのfeatureに必要なものだけを含める。例えば `teams/` は `api/get-teams.ts` のみで、UIコンポーネントを持たない。

### APIファイルの構造 — Queryの場合

```typescript
// features/discussions/api/get-discussions.ts

import { queryOptions, useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api-client';
import { QueryConfig } from '@/lib/react-query';
import { Discussion, Meta } from '@/types/api';

// ① fetcher関数: 純粋なAPI呼び出し
export const getDiscussions = (
  page = 1,
): Promise<{ data: Discussion[]; meta: Meta }> => {
  return api.get(`/discussions`, {
    params: { page },
  });
};

// ② queryOptions: React Queryのキー・fetcher定義
export const getDiscussionsQueryOptions = ({
  page,
}: { page?: number } = {}) => {
  return queryOptions({
    queryKey: page ? ['discussions', { page }] : ['discussions'],
    queryFn: () => getDiscussions(page),
  });
};

// ③ カスタムフック: コンポーネントから使うインターフェース
type UseDiscussionsOptions = {
  page?: number;
  queryConfig?: QueryConfig<typeof getDiscussionsQueryOptions>;
};

export const useDiscussions = ({
  queryConfig,
  page,
}: UseDiscussionsOptions) => {
  return useQuery({
    ...getDiscussionsQueryOptions({ page }),
    ...queryConfig,
  });
};
```

**3層構造の理由:**

| 層 | 名前 | 使われる場面 | なぜ分離するか |
|---|---|---|---|
| ① | `getDiscussions` | テスト、非Reactコンテキスト | React Queryに依存しない純粋な関数。テストでモックしやすい |
| ② | `getDiscussionsQueryOptions` | loader（router.tsx）、prefetch | queryKeyとqueryFnをセットで定義。重複を防ぐ |
| ③ | `useDiscussions` | Reactコンポーネント | フックとして使える。queryConfigでオプション上書き可能 |

### APIファイルの構造 — Mutationの場合

```typescript
// features/discussions/api/create-discussion.ts

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { api } from '@/lib/api-client';
import { MutationConfig } from '@/lib/react-query';
import { Discussion } from '@/types/api';
import { getDiscussionsQueryOptions } from './get-discussions';

// ① バリデーションスキーマ: 入力値の型とルールを定義
export const createDiscussionInputSchema = z.object({
  title: z.string().min(1, 'Required'),
  body: z.string().min(1, 'Required'),
});

// スキーマから型を自動導出
export type CreateDiscussionInput = z.infer<typeof createDiscussionInputSchema>;

// ② mutator関数: 純粋なAPI呼び出し
export const createDiscussion = ({
  data,
}: {
  data: CreateDiscussionInput;
}): Promise<Discussion> => {
  return api.post(`/discussions`, data);
};

// ③ カスタムフック: mutation + キャッシュ更新
type UseCreateDiscussionOptions = {
  mutationConfig?: MutationConfig<typeof createDiscussion>;
};

export const useCreateDiscussion = ({
  mutationConfig,
}: UseCreateDiscussionOptions = {}) => {
  const queryClient = useQueryClient();

  const { onSuccess, ...restConfig } = mutationConfig || {};

  return useMutation({
    onSuccess: (...args) => {
      queryClient.invalidateQueries({
        queryKey: getDiscussionsQueryOptions().queryKey,
      });
      onSuccess?.(...args);
    },
    ...restConfig,
    mutationFn: createDiscussion,
  });
};
```

**Mutation固有のポイント:**

1. **バリデーションスキーマ**: `createDiscussionInputSchema` はZodで定義。これがフォームバリデーションにそのまま使われる（後述）。スキーマとAPIが同じファイルに共存することで「入力の形」と「送信先」が一目でわかる。

2. **`z.infer<typeof schema>`**: Zodスキーマから TypeScript型を自動導出する。スキーマと型を二重に定義する必要がない。スキーマを変更すれば型も自動的に更新される。

3. **`invalidateQueries`**: 作成に成功したら、一覧のキャッシュを無効化する。次に一覧を表示する時にReact Queryが自動的に最新データをフェッチする。

4. **`mutationConfig`**: 呼び出し側が `onSuccess` を追加できる設計。内部の `onSuccess`（キャッシュ無効化）と外部の `onSuccess`（通知表示など）が両方実行される。

---

## 7. components/ — 共通UI

### ボタンコンポーネント — CVAによるバリアント管理

```typescript
// components/ui/button/button.tsx

import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';
import { cn } from '@/utils/cn';
import { Spinner } from '../spinner';

const buttonVariants = cva(
  // ベースクラス: すべてのバリアントに共通するスタイル
  'inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground shadow hover:bg-primary/90',
        destructive: 'bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90',
        outline: 'border border-input bg-background shadow-sm hover:bg-accent hover:text-accent-foreground',
        secondary: 'bg-secondary text-secondary-foreground shadow-sm hover:bg-secondary/80',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-4 py-2',
        sm: 'h-8 rounded-md px-3 text-xs',
        lg: 'h-10 rounded-md px-8',
        icon: 'size-9',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    isLoading?: boolean;
    icon?: React.ReactNode;
  };

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, children, isLoading, icon, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      >
        {isLoading && <Spinner size="sm" className="text-current" />}
        {!isLoading && icon && <span className="mr-2">{icon}</span>}
        <span className="mx-2">{children}</span>
      </Comp>
    );
  },
);
Button.displayName = 'Button';
```

**なぜCVA（class-variance-authority）を使うのか:**

CVAなしの場合:
```typescript
// ❌ 条件分岐が増えるたびに複雑化する
className={`btn ${variant === 'destructive' ? 'bg-red-500' : variant === 'outline' ? 'border' : 'bg-blue-500'} ${size === 'sm' ? 'h-8 px-3' : 'h-9 px-4'}`}
```

CVAありの場合:
```typescript
// ✅ 宣言的にバリアントを定義
buttonVariants({ variant: 'destructive', size: 'sm' })
```

CVAはバリアントの組み合わせをオブジェクトとして宣言的に定義する。条件分岐が消え、新しいバリアントの追加が1行で済む。

**`cn()` ユーティリティ:**

```typescript
// utils/cn.ts
import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

`clsx` は条件付きクラス名を結合し、`twMerge` はTailwindのクラス衝突を解決する（例: `px-4` と `px-2` が両方あると `px-2` だけを残す）。

**`asChild` パターン（Radix UI Slot）:**

```typescript
const Comp = asChild ? Slot : 'button';
```

`asChild` を渡すと、`<Button>` は `<button>` タグを生成せず、子要素にpropsを移譲する。例えば `<Button asChild><a href="...">Link</a></Button>` でボタンスタイルのリンクが作れる。

### 3rdパーティコンポーネントのラッピング

```typescript
// components/ui/link/link.tsx

import { Link as RouterLink, LinkProps } from 'react-router';
import { cn } from '@/utils/cn';

export const Link = ({ className, children, ...props }: LinkProps) => {
  return (
    <RouterLink
      className={cn('text-slate-600 hover:text-slate-900', className)}
      {...props}
    >
      {children}
    </RouterLink>
  );
};
```

**なぜラップするのか**: react-routerの `Link` を直接使うと、スタイルが各所でバラバラになる。ラッパーを挟むことでデフォルトスタイルを統一できる。将来react-routerから別のライブラリに移行する場合も、このファイルだけ修正すればよい。

---

## 8. lib/ — ライブラリ統合層

### api-client.ts — Axiosの設定

```typescript
import Axios, { InternalAxiosRequestConfig } from 'axios';
import { useNotifications } from '@/components/ui/notifications';
import { env } from '@/config/env';
import { paths } from '@/config/paths';

function authRequestInterceptor(config: InternalAxiosRequestConfig) {
  if (config.headers) {
    config.headers.Accept = 'application/json';
  }
  config.withCredentials = true;
  return config;
}

export const api = Axios.create({
  baseURL: env.API_URL,
});

api.interceptors.request.use(authRequestInterceptor);
api.interceptors.response.use(
  (response) => {
    return response.data;  // response.dataを直接返す
  },
  (error) => {
    const message = error.response?.data?.message || error.message;
    useNotifications.getState().addNotification({
      type: 'error',
      title: 'Error',
      message,
    });

    if (error.response?.status === 401) {
      const searchParams = new URLSearchParams();
      const redirectTo = searchParams.get('redirectTo') || window.location.pathname;
      window.location.href = paths.auth.login.getHref(redirectTo);
    }

    return Promise.reject(error);
  },
);
```

**インターセプターの設計意図:**

| インターセプター | 何をしているか | なぜ |
|---|---|---|
| リクエスト | `Accept: application/json` ヘッダを追加、`withCredentials: true` を設定 | すべてのリクエストで共通のヘッダ。Cookie認証を有効にするため |
| レスポンス（成功） | `response.data` を返す | Axiosは `{ data, status, headers, ... }` を返すが、ほとんどの場面で必要なのは `data` だけ |
| レスポンス（エラー） | 通知表示 + 401ならログインへリダイレクト | エラーハンドリングを一箇所に集約。各API呼び出し箇所でエラー処理を書かなくて済む |

**`useNotifications.getState()`**: zustandのストアはReactコンポーネント外からでも `.getState()` で直接アクセスできる。Axiosのインターセプター（React外のコード）から通知を追加する唯一の方法。

### react-query.ts — React Queryの設定と型ヘルパー

```typescript
import { UseMutationOptions, DefaultOptions } from '@tanstack/react-query';

export const queryConfig = {
  queries: {
    refetchOnWindowFocus: false,
    retry: false,
    staleTime: 1000 * 60,
  },
} satisfies DefaultOptions;

export type ApiFnReturnType<FnType extends (...args: any) => Promise<any>> =
  Awaited<ReturnType<FnType>>;

export type QueryConfig<T extends (...args: any[]) => any> = Omit<
  ReturnType<T>,
  'queryKey' | 'queryFn'
>;

export type MutationConfig<
  MutationFnType extends (...args: any) => Promise<any>,
> = UseMutationOptions<
  ApiFnReturnType<MutationFnType>,
  Error,
  Parameters<MutationFnType>[0]
>;
```

**各設定の意味:**

| 設定 | 値 | なぜ |
|---|---|---|
| `refetchOnWindowFocus` | `false` | ブラウザタブに戻った時の自動再フェッチを無効化。業務アプリでは不要なリクエストが多くなるため |
| `retry` | `false` | 失敗時の自動リトライを無効化。すぐにエラー通知を表示するため |
| `staleTime` | `60秒` | データを「古い」とみなすまでの時間。60秒以内の再表示はキャッシュから返される |

**型ヘルパーの活用:**

`QueryConfig<typeof getDiscussionsQueryOptions>` と書くだけで、`queryKey` と `queryFn` を除いたReact Queryのオプション型が得られる。各featureのカスタムフックで「呼び出し側がオプションを上書きできる」設計を可能にする。

---

## 9. 状態管理

Bulletproof Reactは状態を**4種類**に分類する。これが最も重要な設計判断の一つ。

### ① コンポーネント状態（Component State）

```typescript
// components/layouts/dashboard-layout.tsx（抜粋）
const [progress, setProgress] = useState(0);
```

**特徴**: そのコンポーネント内でのみ使われる。`useState` や `useReducer` で管理。
**原則**: まずはここに置く。他のコンポーネントから必要になったら初めて「持ち上げ」を検討する。

### ② アプリケーション状態（Application State）

```typescript
// components/ui/notifications/notifications-store.ts

import { nanoid } from 'nanoid';
import { create } from 'zustand';

export type Notification = {
  id: string;
  type: 'info' | 'warning' | 'success' | 'error';
  title: string;
  message?: string;
};

type NotificationsStore = {
  notifications: Notification[];
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  dismissNotification: (id: string) => void;
};

export const useNotifications = create<NotificationsStore>((set) => ({
  notifications: [],
  addNotification: (notification) =>
    set((state) => ({
      notifications: [
        ...state.notifications,
        { id: nanoid(), ...notification },
      ],
    })),
  dismissNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter(
        (notification) => notification.id !== id,
      ),
    })),
}));
```

**特徴**: グローバルに共有されるUI状態。zustandで管理。
**なぜzustandか**: Reduxと比べてボイラープレートが圧倒的に少ない。ストアの定義が1ファイルで完結する。React外（Axiosインターセプター）からも `getState()` でアクセスできる。
**なぜContextではないか**: 高頻度で更新される状態（通知の追加・削除）をContextに置くと、Context消費者すべてが再レンダリングされる。zustandは購読（subscribe）ベースなので、必要なコンポーネントだけが再レンダリングされる。

### ③ サーバーキャッシュ状態（Server Cache State）

```typescript
// features/discussions/api/get-discussions.ts で定義済み
const discussionsQuery = useDiscussions({ page: 1 });

discussionsQuery.data     // キャッシュされたデータ
discussionsQuery.isLoading // ローディング中か
discussionsQuery.error     // エラーがあるか
```

**特徴**: サーバーから取得したデータ。React Queryで管理。
**なぜReduxに入れないのか**: サーバーデータには「古くなる」「再取得が必要」「楽観的更新」「無効化」といったサーバー固有の関心事がある。React Queryはこれらをすべて内蔵している。Reduxに入れると、これらの仕組みを自前で実装する必要がある。

### ④ URL状態（URL State）

```typescript
// features/discussions/components/discussions-list.tsx（抜粋）
const [searchParams] = useSearchParams();
const page = +(searchParams.get('page') || 1);
```

**特徴**: URLのクエリパラメータやパスパラメータ。react-routerで管理。
**なぜURLに入れるか**: ページ番号やフィルタ条件をURLに持たせると、ブックマーク可能・共有可能・ブラウザバック対応になる。

---

## 10. API層

### API呼び出しの統一パターン

すべてのAPI呼び出しは以下の3層構造に従う:

```
┌─────────────────────────────────────────┐
│ useXxx()  ← コンポーネントが使うフック      │
│   └── xxxQueryOptions()  ← Query定義      │
│         └── getXxx()  ← 純粋なfetcher     │
└─────────────────────────────────────────┘
```

**Query（データ取得）の場合:**

```
getDiscussions()           → api.get('/discussions')  → Promise<Data>
getDiscussionsQueryOptions → { queryKey, queryFn }
useDiscussions()           → useQuery(options)        → { data, isLoading, error }
```

**Mutation（データ変更）の場合:**

```
createDiscussionInputSchema → Zod validation schema
createDiscussion()          → api.post('/discussions')  → Promise<Data>
useCreateDiscussion()       → useMutation(fn) + invalidateQueries
```

### 無限スクロール（Infinite Query）

```typescript
// features/comments/api/get-comments.ts

export const getInfiniteCommentsQueryOptions = (discussionId: string) => {
  return infiniteQueryOptions({
    queryKey: ['comments', discussionId],
    queryFn: ({ pageParam = 1 }) => {
      return getComments({ discussionId, page: pageParam as number });
    },
    getNextPageParam: (lastPage) => {
      if (lastPage?.meta?.page === lastPage?.meta?.totalPages) return undefined;
      const nextPage = lastPage.meta.page + 1;
      return nextPage;
    },
    initialPageParam: 1,
  });
};
```

**`getNextPageParam`**: 最後のページのメタデータから次のページ番号を計算する。`undefined` を返すと「これ以上ページがない」ことを示す。React Queryの `fetchNextPage()` を呼ぶだけで次ページを自動取得できる。

---

## 11. フォームとバリデーション

### Form コンポーネント — React Hook Form + Zod の統合

```typescript
// components/ui/form/form.tsx（核心部分）

const Form = <
  Schema extends ZodType<any, any, any>,
  TFormValues extends FieldValues = z.infer<Schema>,
>({
  onSubmit,
  children,
  className,
  options,
  id,
  schema,
}: FormProps<TFormValues, Schema>) => {
  const form = useForm({ ...options, resolver: zodResolver(schema) });
  return (
    <FormProvider {...form}>
      <form
        className={cn('space-y-6', className)}
        onSubmit={form.handleSubmit(onSubmit)}
        id={id}
      >
        {children(form)}
      </form>
    </FormProvider>
  );
};
```

**この設計が解決する問題:**

1. **Render Props パターン**: `children(form)` でフォームのメソッド（register, formState）を子に渡す。子コンポーネントが `useFormContext` を使わなくても直接アクセスできる。
2. **Zodリゾルバー**: `zodResolver(schema)` でZodスキーマをReact Hook Formのバリデーターに変換。スキーマはAPIファイルで定義済みのものを再利用する。
3. **ジェネリクス**: `Schema` の型からフォーム値の型 (`TFormValues`) が自動推論される。型安全なフォーム。

### 実際の使用例 — ログインフォーム

```typescript
// features/auth/components/login-form.tsx

import { Form, Input } from '@/components/ui/form';
import { useLogin, loginInputSchema } from '@/lib/auth';

export const LoginForm = ({ onSuccess }: LoginFormProps) => {
  const login = useLogin({ onSuccess });

  return (
    <Form
      onSubmit={(values) => {
        login.mutate(values);   // valuesはloginInputSchemaの型に自動推論される
      }}
      schema={loginInputSchema} // Zodスキーマをそのまま渡す
    >
      {({ register, formState }) => (
        <>
          <Input
            type="email"
            label="Email Address"
            error={formState.errors['email']}
            registration={register('email')}  // 'email'は型安全（タイポするとエラー）
          />
          <Input
            type="password"
            label="Password"
            error={formState.errors['password']}
            registration={register('password')}
          />
          <Button isLoading={login.isPending} type="submit" className="w-full">
            Log in
          </Button>
        </>
      )}
    </Form>
  );
};
```

**データの流れ:**

```
loginInputSchema（Zodスキーマ: lib/auth.tsx で定義）
    ↓ Formに渡す
zodResolver がバリデーション実行
    ↓ バリデーション通過
onSubmit(values) が呼ばれる（valuesの型はスキーマから推論済み）
    ↓
login.mutate(values) でAPI呼び出し（lib/auth.tsx の loginWithEmailAndPassword）
    ↓ 成功
onSuccess コールバック（ルートファイルで定義: ダッシュボードに遷移）
```

**バリデーションスキーマの定義場所:**

```typescript
// lib/auth.tsx

export const loginInputSchema = z.object({
  email: z.string().min(1, 'Required').email('Invalid email'),
  password: z.string().min(5, 'Required'),
});
```

スキーマはAPIの近くに定義する。「このAPIに送るデータの形」と「そのバリデーションルール」が同じファイルにあることで、整合性が保たれる。

---

## 12. ルーティングとコード分割

### コード分割の仕組み

```typescript
// app/router.tsx

{
  path: paths.app.discussions.path,
  lazy: () =>
    import('./routes/app/discussions/discussions').then(
      convert(queryClient),
    ),
},
```

`lazy` + `import()` の組み合わせで、Viteが自動的にチャンク分割する。ビルド結果:

```
dist/assets/
├── index-abc123.js          # 共通コード（React, React Query等）
├── discussions-def456.js    # /app/discussions ページのコード
├── discussion-ghi789.js     # /app/discussions/:id ページのコード
└── ...
```

ユーザーがログインページにいる間、discussions のJSは一切ダウンロードされない。discussionsページに遷移して初めてダウンロードが走る。

### データプリフェッチとの連携

```typescript
// app/routes/app/discussions/discussions.tsx

export const clientLoader =
  (queryClient: QueryClient) =>
  async ({ request }: LoaderFunctionArgs) => {
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') || 1);
    const query = getDiscussionsQueryOptions({ page });

    return (
      queryClient.getQueryData(query.queryKey) ??
      (await queryClient.fetchQuery(query))
    );
  };
```

React Routerの `loader` はページコンポーネントのレンダリング**前に**実行される。これにより:

1. URL遷移 → loader実行（データフェッチ開始）→ JS チャンクのダウンロード → コンポーネントレンダリング
2. **データとコードが並行でロード**される（loaderはasync、importも並行で走る）
3. コンポーネントがレンダリングされる時にはデータが準備済み

---

## 13. 認証・認可

### 認証（Authentication） — react-query-auth

```typescript
// lib/auth.tsx

import { configureAuth } from 'react-query-auth';

const authConfig = {
  userFn: getUser,                           // 現在のユーザー情報を取得
  loginFn: async (data: LoginInput) => {     // ログイン
    const response = await loginWithEmailAndPassword(data);
    return response.user;
  },
  registerFn: async (data: RegisterInput) => { // 登録
    const response = await registerWithEmailAndPassword(data);
    return response.user;
  },
  logoutFn: logout,                           // ログアウト
};

export const { useUser, useLogin, useLogout, useRegister, AuthLoader } =
  configureAuth(authConfig);
```

**react-query-authが提供するもの:**

| フック/コンポーネント | 役割 |
|---|---|
| `useUser()` | 現在のユーザー情報を取得（React Queryで自動キャッシュ） |
| `useLogin()` | ログインmutation |
| `useLogout()` | ログアウトmutation |
| `useRegister()` | 登録mutation |
| `AuthLoader` | ユーザー情報取得完了まで子コンポーネントの描画を遅延 |

### 認証ガード — ProtectedRoute

```typescript
// lib/auth.tsx

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const user = useUser();
  const location = useLocation();

  if (!user.data) {
    return (
      <Navigate to={paths.auth.login.getHref(location.pathname)} replace />
    );
  }

  return children;
};
```

ユーザーが未認証なら、現在のパスを `redirectTo` パラメータとしてログインページにリダイレクト。ログイン後に元のページに戻れる。

### 認可（Authorization） — RBAC + PBAC

```typescript
// lib/authorization.tsx

export const POLICIES = {
  'comment:delete': (user: User, comment: Comment) => {
    if (user.role === 'ADMIN') return true;
    if (user.role === 'USER' && comment.author?.id === user.id) return true;
    return false;
  },
};

export const Authorization = ({
  policyCheck,
  allowedRoles,
  forbiddenFallback = null,
  children,
}: AuthorizationProps) => {
  const { checkAccess } = useAuthorization();

  let canAccess = false;

  if (allowedRoles) {
    canAccess = checkAccess({ allowedRoles });  // RBAC: ロールベース
  }

  if (typeof policyCheck !== 'undefined') {
    canAccess = policyCheck;                     // PBAC: ポリシーベース
  }

  return <>{canAccess ? children : forbiddenFallback}</>;
};
```

**使い分け:**

```typescript
// RBAC: ADMINだけが見える
<Authorization allowedRoles={[ROLES.ADMIN]}>
  <CreateDiscussion />
</Authorization>

// PBAC: コメントの作者だけが削除できる
<Authorization policyCheck={POLICIES['comment:delete'](user, comment)}>
  <DeleteComment />
</Authorization>
```

**なぜ2つのアプローチを持つのか**: RBACは「管理者だけが操作できる」というシンプルなケースに。PBACは「自分のコメントだけ削除できる」というリソース所有者チェックに。現実のアプリケーションでは両方が必要。

---

## 14. エラーハンドリング

### 3層のエラーハンドリング

**① グローバルAPIエラー（api-client.ts のインターセプター）:**

すべてのAPIエラーがトースト通知として表示される。401は自動的にログインへリダイレクト。

**② ページレベルのErrorBoundary:**

```typescript
// app/routes/app/discussions/discussion.tsx

<ErrorBoundary
  fallback={
    <div>Failed to load comments. Try to refresh the page.</div>
  }
>
  <Comments discussionId={discussionId} />
</ErrorBoundary>
```

コメント読み込みに失敗しても、ディスカッション本文は表示される。ErrorBoundaryはエラーの影響範囲を限定する。

**③ アプリ全体のErrorBoundary（provider.tsx）:**

```typescript
<ErrorBoundary FallbackComponent={MainErrorFallback}>
  ...
</ErrorBoundary>
```

上記2つで捕まらなかった致命的エラーの最終防衛線。

---

## 15. セキュリティ

### XSS対策 — DOMPurify

```typescript
// components/ui/md-preview/md-preview.tsx

import createDOMPurify from 'dompurify';
import { parse } from 'marked';

const DOMPurify = createDOMPurify(window);

export const MDPreview = ({ value = '' }: MDPreviewProps) => {
  return (
    <div
      className="prose prose-slate w-full p-2"
      dangerouslySetInnerHTML={{
        __html: DOMPurify.sanitize(parse(value) as string),
      }}
    />
  );
};
```

**なぜDOMPurifyが必要か**: `dangerouslySetInnerHTML` はHTMLをそのまま描画する。ユーザー入力のMarkdownに `<script>alert('xss')</script>` が含まれていたら、そのスクリプトが実行されてしまう。`DOMPurify.sanitize()` が危険なHTMLタグ・属性を除去してから描画する。

### 認証トークンの保護

```typescript
// lib/api-client.ts
config.withCredentials = true;
```

`withCredentials: true` により、Cookieがリクエストに自動で含まれる。トークンを `localStorage` に保存するとXSSで盗まれるリスクがあるため、`HttpOnly` Cookieでの管理を前提とした設計。

---

## 16. テスティング

### テストの種類と対象

| 種類 | ツール | テスト対象 | 例 |
|---|---|---|---|
| 単体テスト | Vitest + Testing Library | 共通UIコンポーネント | dialog, form, notification |
| 統合テスト | Vitest + MSW | ページ全体のフロー | discussions一覧→詳細→コメント |
| E2Eテスト | Playwright | ユーザー操作の自動化 | ログイン→プロフィール編集 |

### MSW（Mock Service Worker）の活用

MSWはService Workerとしてブラウザに常駐し、HTTPリクエストをインターセプトしてモックレスポンスを返す。

**開発中のモック**: `enableMocking()` で `main.tsx` から起動。ブラウザの Network タブに実際のHTTPリクエストが見え、通常のAPIと同じ挙動をする。

**テスト時のモック**: Vitestのsetupファイルで `server.listen()` を呼び、Node.js環境でモックサーバーを起動。`fetch` や `axios` のモックが不要になり、実際のHTTP通信と同じコードでテストできる。

---

## 17. プロジェクト標準

### ファイル命名規則

```
すべてのファイル: kebab-case（例: create-discussion.tsx）
すべてのフォルダ: kebab-case（例: discussions/）
```

ESLintの `check-file` プラグインで強制。PascalCaseの `CreateDiscussion.tsx` やcamelCaseの `createDiscussion.tsx` はエラーになる。

**なぜkebab-case**: OSによるファイルシステムの大文字小文字の扱いの違い（macOSは非区別、Linuxは区別）でCI/CDが壊れる問題を防ぐ。kebab-caseなら大文字が一切含まれない。

### import順序

```typescript
// ESLintルール: import/order
import { useQuery } from '@tanstack/react-query';    // 1. 外部パッケージ

import { api } from '@/lib/api-client';               // 2. 内部（absolute import）

import { getDiscussionQueryOptions } from '../api/get-discussion';  // 3. 親ディレクトリ
import { DeleteDiscussion } from './delete-discussion';             // 4. 同階層
```

アルファベット順 + グループ間に空行。自動修正されるため、手動で気にする必要はない。

---

## 18. ESLint による構造の強制

### Feature間のimport禁止

```javascript
// .eslintrc.cjs

'import/no-restricted-paths': [
  'error',
  {
    zones: [
      // discussions は comments を直接importできない
      {
        target: './src/features/discussions',
        from: './src/features',
        except: ['./discussions'],
      },
      // comments は discussions を直接importできない
      {
        target: './src/features/comments',
        from: './src/features',
        except: ['./comments'],
      },
      // ... 各featureごとに定義
    ],
  },
],
```

**これがなぜ最も重要な設定か**: Feature-based Architectureの核心は「featureの独立性」だが、ESLintルールがなければ、誰かが `features/discussions/` 内で `import { something } from '../comments/...'` と書いてしまう。一度でもこのimportが入ると、feature間の依存が芋づる式に広がり、構造が崩壊する。

### 一方向依存の強制

```javascript
// features は app をimportできない
{
  target: './src/features',
  from: './src/app',
},

// 共通層（components, hooks, lib等）は features と app をimportできない
{
  target: [
    './src/components',
    './src/hooks',
    './src/lib',
    './src/types',
    './src/utils',
  ],
  from: ['./src/features', './src/app'],
},
```

```
  app    →  features  →  shared(components, hooks, lib, types, utils)
  ─────────────────────────────────────────────────────>
  import可能な方向（左→右のみ）
```

**ESLintが構造を守る。人間のレビューだけに頼らない。**

---

## まとめ — Bulletproof Reactの本質

Bulletproof Reactは特定の技術選定ではなく、**設計原則**の集合である:

| 原則 | 具体的な実装 |
|---|---|
| Feature単位のコロケーション | `features/discussions/` にapi/components/hooksをまとめる |
| 一方向の依存フロー | ESLint `import/no-restricted-paths` で強制 |
| 状態の分類管理 | Component / Application / Server Cache / URL の4種 |
| API層の3層構造 | fetcher → queryOptions → useHook |
| 型安全の徹底 | Zodスキーマ → 型推論 → フォームバリデーション |
| エラーの段階的キャッチ | インターセプター → 部分ErrorBoundary → 全体ErrorBoundary |
| セキュリティのデフォルト化 | DOMPurify, HttpOnly Cookie, RBAC/PBAC |
| 構造のコード化 | ESLintルールで人間の規律に頼らず構造を維持 |

これらの原則は React + Vite + TypeScript に限定されるものではなく、フロントエンド開発全般に適用できる設計思想である。
