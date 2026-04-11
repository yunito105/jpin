# Bulletproof React — ファイル呼び出し順 完全トレース

> **対象**: `http://localhost:3000/` をブラウザで開いた瞬間から、画面が表示されるまでの全ファイルの動き
> **方針**: 一切省略しない。importの連鎖もすべて記載する

---

## 読み方

```
STEP番号  [起点] → 呼び出されるファイル
           何が起きるか
             ↳ このファイルがさらに import するもの → 次のSTEP
```

- 🔵 **ロード** = ファイルが初めて読み込まれる
- 🟢 **実行** = 関数/コードが実行される
- 🔴 **HTTP通信** = 実際のAPIリクエストが発生する
- ⚡ **遅延ロード** = その画面に遷移した時だけ読み込まれる（起動時はスキップ）
- ✅ **解決済み** = 同じファイルが既にロード済みのため再ロードしない

---

## フェーズ一覧

| フェーズ | 内容 |
|---|---|
| [フェーズ1](#フェーズ1--ブラウザがhtmlを受け取る) | ブラウザがHTMLを受け取る |
| [フェーズ2](#フェーズ2--javascriptモジュールの読み込み連鎖) | JavaScriptモジュールの読み込み連鎖（全importの解決） |
| [フェーズ3](#フェーズ3--コード実行) | コード実行（enableMocking → createRoot） |
| [フェーズ4](#フェーズ4--reactのレンダリング開始) | Reactのレンダリング開始 |
| [フェーズ5](#フェーズ5--認証確認apiリクエスト) | 認証確認APIリクエスト |
| [フェーズ6](#フェーズ6--ルーティング解決とランディングページ表示) | ルーティング解決とランディングページ表示 |

---

## フェーズ1 — ブラウザがHTMLを受け取る

```
STEP 1
  [ユーザー] http://localhost:3000/ をブラウザのアドレスバーに入力してEnter

STEP 2
  [ブラウザ] → Viteの開発サーバー(port:3000)にHTTP GETリクエスト
              GET http://localhost:3000/

STEP 3  🔵
  [Viteサーバー] → index.html を返す

  ファイル: index.html
  ┌─────────────────────────────────────────────────────────
  │ <!doctype html>
  │ <html lang="en">
  │   <head>
  │     <meta charset="utf-8" />
  │     <link rel="icon" href="/favicon.ico" />         ← faviconをGET
  │     <link rel="stylesheet"
  │       href="https://rsms.me/inter/inter.css" />     ← Googleフォント(外部)
  │     <title>Bulletproof React</title>
  │   </head>
  │   <body>
  │     <div id="root"></div>                           ← Reactのマウントポイント
  │     <script type="module"
  │       src="/src/main.tsx"></script>                 ← ここからJS開始
  │   </body>
  │ </html>
  └─────────────────────────────────────────────────────────

  この時点でブラウザが見ているもの:
  ・<div id="root"></div> だけ（空っぽ）
  ・画面は真っ白
```

---

## フェーズ2 — JavaScriptモジュールの読み込み連鎖

> `<script type="module" src="/src/main.tsx">` が見つかった瞬間から始まる。
> Viteがimportを解決しながら、必要な全ファイルをダウンロードする。

```
STEP 4  🔵
  [index.html] → src/main.tsx を読み込む

  ファイル: src/main.tsx
  ┌─────────────────────────────────────────────────────────
  │ import * as React from 'react';              ← 外部ライブラリ（node_modules）
  │ import { createRoot } from 'react-dom/client'; ← 外部ライブラリ
  │ import './index.css';                        ← STEP 5
  │ import { App } from './app';                 ← STEP 6
  │ import { enableMocking } from
  │   './testing/mocks';                         ← STEP 18
  │
  │ const root = document.getElementById('root');
  │ enableMocking().then(() => {                 ← フェーズ3で実行
  │   createRoot(root).render(<App />);
  │ });
  └─────────────────────────────────────────────────────────

---

STEP 5  🔵
  [main.tsx の import] → src/index.css を読み込む

  ファイル: src/index.css
  ┌─────────────────────────────────────────────────────────
  │ @tailwind base;        ← Tailwindのリセット/ベーススタイル
  │ @tailwind components;  ← コンポーネント用スタイル
  │ @tailwind utilities;   ← mt-4等のユーティリティクラス
  │
  │ :root { --background: ...; --foreground: ...; ... }  ← CSS変数
  │ .dark { --background: ...; ... }                     ← ダークモード用変数
  └─────────────────────────────────────────────────────────
  (importする他のファイルなし)

---

STEP 6  🔵
  [main.tsx の import] → src/app/index.tsx を読み込む

  ファイル: src/app/index.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { AppProvider } from './provider';    ← STEP 7
  │ import { AppRouter } from './router';        ← STEP 15
  │
  │ export const App = () => (
  │   <AppProvider>
  │     <AppRouter />
  │   </AppProvider>
  │ );
  └─────────────────────────────────────────────────────────

---

STEP 7  🔵
  [app/index.tsx の import] → src/app/provider.tsx を読み込む

  ファイル: src/app/provider.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { QueryClient, QueryClientProvider }
  │   from '@tanstack/react-query';              ← 外部ライブラリ
  │ import { ReactQueryDevtools }
  │   from '@tanstack/react-query-devtools';     ← 外部ライブラリ
  │ import * as React from 'react';              ← 外部ライブラリ
  │ import { ErrorBoundary }
  │   from 'react-error-boundary';              ← 外部ライブラリ
  │ import { HelmetProvider }
  │   from 'react-helmet-async';                ← 外部ライブラリ
  │ import { MainErrorFallback }
  │   from '@/components/errors/main';          ← STEP 8
  │ import { Notifications }
  │   from '@/components/ui/notifications';     ← STEP 9
  │ import { Spinner }
  │   from '@/components/ui/spinner';           ← STEP 12
  │ import { AuthLoader }
  │   from '@/lib/auth';                        ← STEP 13
  │ import { queryConfig }
  │   from '@/lib/react-query';                 ← STEP 14
  └─────────────────────────────────────────────────────────

---

STEP 8  🔵
  [provider.tsx の import] → src/components/errors/main.tsx を読み込む

  ファイル: src/components/errors/main.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { Button } from '../ui/button';       ← src/components/ui/button/index.ts
  │                                                (button.tsx → cn.ts をimport)
  │ export const MainErrorFallback = () => (
  │   <div role="alert">
  │     <h2>Ooops, something went wrong</h2>
  │     <Button onClick={() => window.location.assign(...)}>Refresh</Button>
  │   </div>
  │ );
  └─────────────────────────────────────────────────────────

    ↳ STEP 8-a  🔵  src/components/ui/button/button.tsx
        ┌────────────────────────────────────────────
        │ import { Slot } from '@radix-ui/react-slot'; ← 外部ライブラリ
        │ import { cva } from 'class-variance-authority'; ← 外部ライブラリ
        │ import { Loader2 } from 'lucide-react';      ← 外部ライブラリ
        │ import { cn } from '@/utils/cn';             ← STEP 8-b
        │
        │ // Buttonコンポーネント定義
        │ // variant: default/destructive/outline/subtle/ghost/link
        │ // size: default/sm/lg/icon
        └────────────────────────────────────────────

        ↳ STEP 8-b  🔵  src/utils/cn.ts
            ┌────────────────────────────────────
            │ import { type ClassValue, clsx } from 'clsx'; ← 外部ライブラリ
            │ import { twMerge } from 'tailwind-merge';     ← 外部ライブラリ
            │
            │ export function cn(...inputs: ClassValue[]) {
            │   return twMerge(clsx(inputs));
            │ }
            └────────────────────────────────────
            (importする他のファイルなし)

---

STEP 9  🔵
  [provider.tsx の import] → src/components/ui/notifications/index.ts を読み込む

  ファイル: src/components/ui/notifications/index.ts
  ┌─────────────────────────────────────────────────────────
  │ export * from './notifications';             ← STEP 10
  │ export * from './notifications-store';       ← STEP 11
  └─────────────────────────────────────────────────────────

    ↳ STEP 10  🔵  src/components/ui/notifications/notifications.tsx
        ┌────────────────────────────────────────────
        │ import { Notification }
        │   from './notification';                ← notification.tsx (個別トーストUI)
        │ import { useNotifications }
        │   from './notifications-store';         ← STEP 11
        │
        │ export const Notifications = () => {
        │   const { notifications, dismissNotification }
        │     = useNotifications();               ← Zustandストアから取得
        │   return (
        │     <div aria-live="assertive" ...>
        │       {notifications.map(n => <Notification ... />)}
        │     </div>
        │   );
        │ };
        └────────────────────────────────────────────

    ↳ STEP 11  🔵  src/components/ui/notifications/notifications-store.ts
        ┌────────────────────────────────────────────
        │ import { nanoid } from 'nanoid';        ← 外部ライブラリ（ID生成）
        │ import { create } from 'zustand';       ← 外部ライブラリ（状態管理）
        │
        │ // Zustandストアを作成（グローバル状態）
        │ export const useNotifications = create((set) => ({
        │   notifications: [],                    ← 初期値：空配列
        │   addNotification: (n) => set(...),     ← 通知追加
        │   dismissNotification: (id) => set(...),← 通知削除
        │ }));
        └────────────────────────────────────────────
        (importする他のファイルなし)

---

STEP 12  🔵
  [provider.tsx の import] → src/components/ui/spinner/spinner.tsx を読み込む

  ファイル: src/components/ui/spinner/spinner.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { cva } from 'class-variance-authority'; ← 外部ライブラリ
  │ import { cn } from '@/utils/cn';             ← ✅ STEP 8-b で解決済み
  │
  │ // ローディングスピナーのSVGアニメーション定義
  │ // size: sm/md/lg/xl/default
  └─────────────────────────────────────────────────────────

---

STEP 13  🔵
  [provider.tsx の import] → src/lib/auth.tsx を読み込む

  ファイル: src/lib/auth.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { configureAuth }
  │   from 'react-query-auth';                   ← 外部ライブラリ
  │ import { Navigate, useLocation }
  │   from 'react-router';                       ← 外部ライブラリ
  │ import { z } from 'zod';                     ← 外部ライブラリ
  │ import { paths }
  │   from '@/config/paths';                     ← STEP 13-a
  │ import { AuthResponse, User }
  │   from '@/types/api';                        ← STEP 13-b
  │ import { api }
  │   from './api-client';                       ← STEP 13-c
  │
  │ // APIクライアント関数の定義
  │ const getUser = async () =>
  │   api.get('/auth/me');                       ← GET /auth/me を定義(まだ実行しない)
  │
  │ // Zodスキーマ定義
  │ export const loginInputSchema = z.object({
  │   email: z.string().min(1).email(),
  │   password: z.string().min(5),
  │ });
  │ export const registerInputSchema = z.object({...});
  │
  │ // react-query-auth で認証フックを生成
  │ export const { useUser, useLogin, useLogout,
  │               useRegister, AuthLoader }
  │   = configureAuth({ userFn: getUser, ... });
  │
  │ // 認証ガード（未ログインなら /auth/login へリダイレクト）
  │ export const ProtectedRoute = ({ children }) => {
  │   const user = useUser();
  │   const location = useLocation();
  │   if (!user.data) return <Navigate to="/auth/login?redirectTo=..." />;
  │   return children;
  │ };
  └─────────────────────────────────────────────────────────

    ↳ STEP 13-a  🔵  src/config/paths.ts
        ┌────────────────────────────────────────────
        │ // importなし（純粋な定数定義ファイル）
        │ export const paths = {
        │   home:  { path: '/', getHref: () => '/' },
        │   auth: {
        │     login:    { path: '/auth/login',    getHref: (r?) => ... },
        │     register: { path: '/auth/register', getHref: (r?) => ... },
        │   },
        │   app: {
        │     root:        { path: '/app',                    ... },
        │     dashboard:   { path: '',                        ... },
        │     discussions: { path: 'discussions',             ... },
        │     discussion:  { path: 'discussions/:discussionId',...},
        │     users:       { path: 'users',                   ... },
        │     profile:     { path: 'profile',                 ... },
        │   },
        │ } as const;
        └────────────────────────────────────────────

    ↳ STEP 13-b  🔵  src/types/api.ts
        ┌────────────────────────────────────────────
        │ // importなし（純粋な型定義ファイル）
        │ export type BaseEntity = { id: string; createdAt: number; };
        │ export type Entity<T> = T & BaseEntity;
        │ export type Meta = { page: number; total: number; totalPages: number; };
        │ export type User = Entity<{
        │   firstName: string; lastName: string;
        │   email: string; role: 'ADMIN' | 'USER';
        │   teamId: string; bio: string;
        │ }>;
        │ export type AuthResponse = { jwt: string; user: User; };
        │ export type Team = Entity<{ name: string; description: string; }>;
        │ export type Discussion = Entity<{
        │   title: string; body: string; teamId: string; author: User;
        │ }>;
        │ export type Comment = Entity<{
        │   body: string; discussionId: string; author: User;
        │ }>;
        └────────────────────────────────────────────

    ↳ STEP 13-c  🔵  src/lib/api-client.ts
        ┌────────────────────────────────────────────
        │ import Axios from 'axios';              ← 外部ライブラリ
        │ import { useNotifications }
        │   from '@/components/ui/notifications'; ← ✅ STEP 9 で解決済み
        │ import { env }
        │   from '@/config/env';                 ← STEP 13-c-①
        │ import { paths }
        │   from '@/config/paths';               ← ✅ STEP 13-a で解決済み
        │
        │ // リクエストインターセプター（全リクエストに付与）
        │ function authRequestInterceptor(config) {
        │   config.headers.Accept = 'application/json';
        │   config.withCredentials = true;        ← Cookieを自動送信
        │   return config;
        │ }
        │
        │ // Axiosインスタンス作成
        │ export const api = Axios.create({
        │   baseURL: env.API_URL,                 ← .envから読んだURL
        │ });
        │
        │ // リクエストインターセプター登録
        │ api.interceptors.request.use(authRequestInterceptor);
        │
        │ // レスポンスインターセプター登録
        │ api.interceptors.response.use(
        │   (response) => response.data,          ← .dataを自動抽出
        │   (error) => {
        │     // エラー通知を表示（Zustandストアに追加）
        │     useNotifications.getState().addNotification({
        │       type: 'error', title: 'Error', message: ...
        │     });
        │     // 401なら強制ログイン画面へ
        │     if (error.response?.status === 401) {
        │       window.location.href = paths.auth.login.getHref(...);
        │     }
        │     return Promise.reject(error);
        │   }
        │ );
        └────────────────────────────────────────────

        ↳ STEP 13-c-①  🔵  src/config/env.ts
            ┌────────────────────────────────────
            │ import * as z from 'zod';           ← 外部ライブラリ
            │
            │ const createEnv = () => {
            │   const EnvSchema = z.object({
            │     API_URL: z.string(),            ← 必須
            │     ENABLE_API_MOCKING: z.string()
            │       .optional(),                  ← 任意
            │     APP_URL: z.string()
            │       .default('http://localhost:3000'),
            │     APP_MOCK_API_PORT: z.string()
            │       .default('8080'),
            │   });
            │   // import.meta.env から VITE_APP_ プレフィックスの変数を抽出
            │   const envVars = Object.entries(import.meta.env)
            │     .reduce((acc, [key, value]) => {
            │       if (key.startsWith('VITE_APP_'))
            │         acc[key.replace('VITE_APP_', '')] = value;
            │       return acc;
            │     }, {});
            │   // Zodでバリデーション（失敗時はthrow）
            │   const parsedEnv = EnvSchema.safeParse(envVars);
            │   if (!parsedEnv.success) throw new Error('Invalid env...');
            │   return parsedEnv.data;
            │ };
            │
            │ export const env = createEnv(); ← ファイルロード時点で即実行
            └────────────────────────────────────
            (importする他のファイルなし)

---

STEP 14  🔵
  [provider.tsx の import] → src/lib/react-query.ts を読み込む

  ファイル: src/lib/react-query.ts
  ┌─────────────────────────────────────────────────────────
  │ import { UseMutationOptions, DefaultOptions }
  │   from '@tanstack/react-query';              ← 外部ライブラリ（型のみ）
  │
  │ // グローバルなキャッシュ設定
  │ export const queryConfig = {
  │   queries: {
  │     refetchOnWindowFocus: false, ← タブ切替で再取得しない
  │     retry: false,                ← エラー時にリトライしない
  │     staleTime: 1000 * 60,        ← 1分間はキャッシュを新鮮とみなす
  │   },
  │ };
  │
  │ // 型ユーティリティ（ApiFnReturnType, QueryConfig, MutationConfig）
  └─────────────────────────────────────────────────────────
  (実行ファイルのimportなし)

---

STEP 15  🔵
  [app/index.tsx の import] → src/app/router.tsx を読み込む

  ファイル: src/app/router.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { QueryClient, useQueryClient }
  │   from '@tanstack/react-query';              ← 外部ライブラリ
  │ import { useMemo } from 'react';             ← 外部ライブラリ
  │ import { createBrowserRouter }
  │   from 'react-router';                       ← 外部ライブラリ
  │ import { RouterProvider }
  │   from 'react-router/dom';                   ← 外部ライブラリ
  │ import { paths }
  │   from '@/config/paths';                     ← ✅ STEP 13-a で解決済み
  │ import { ProtectedRoute }
  │   from '@/lib/auth';                         ← ✅ STEP 13 で解決済み
  │ import AppRoot, { ErrorBoundary }
  │   from './routes/app/root';                  ← STEP 16（同期import）
  │
  │ // ルートテーブル定義（URLとコンポーネントの対応）
  │ export const createAppRouter = (queryClient) =>
  │   createBrowserRouter([
  │     { path: '/',
  │       lazy: () => import('./routes/landing') },          ← ⚡遅延ロード
  │     { path: '/auth/register',
  │       lazy: () => import('./routes/auth/register') },    ← ⚡遅延ロード
  │     { path: '/auth/login',
  │       lazy: () => import('./routes/auth/login') },       ← ⚡遅延ロード
  │     { path: '/app',
  │       element: <ProtectedRoute><AppRoot /></ProtectedRoute>,
  │       children: [
  │         { path: 'discussions',
  │           lazy: () => import('./routes/app/discussions/discussions') }, ← ⚡
  │         { path: 'discussions/:discussionId',
  │           lazy: () => import('./routes/app/discussions/discussion') },  ← ⚡
  │         { path: 'users',
  │           lazy: () => import('./routes/app/users') },                   ← ⚡
  │         { path: 'profile',
  │           lazy: () => import('./routes/app/profile') },                 ← ⚡
  │         { path: '',
  │           lazy: () => import('./routes/app/dashboard') },               ← ⚡
  │       ]},
  │     { path: '*',
  │       lazy: () => import('./routes/not-found') },        ← ⚡遅延ロード
  │   ]);
  └─────────────────────────────────────────────────────────

---

STEP 16  🔵
  [router.tsx の import] → src/app/routes/app/root.tsx を読み込む
  ※ AppRoot は lazy() ではなく直接 import → 起動時に必ず読み込まれる

  ファイル: src/app/routes/app/root.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { Outlet } from 'react-router';       ← 外部ライブラリ
  │ import { DashboardLayout }
  │   from '@/components/layouts';               ← STEP 17
  │
  │ export const ErrorBoundary = () => (
  │   <div>Something went wrong!</div>
  │ );
  │
  │ const AppRoot = () => (
  │   <DashboardLayout>
  │     <Outlet />                               ← 子ルートの描画位置
  │   </DashboardLayout>
  │ );
  │
  │ export default AppRoot;
  └─────────────────────────────────────────────────────────

---

STEP 17  🔵
  [root.tsx の import] → src/components/layouts/index.ts を読み込む

  ファイル: src/components/layouts/index.ts
  ┌─────────────────────────────────────────────────────────
  │ export * from './content-layout';            ← STEP 17-a
  │ export * from './dashboard-layout';          ← STEP 17-b
  └─────────────────────────────────────────────────────────

    ↳ STEP 17-a  🔵  src/components/layouts/content-layout.tsx
        ┌────────────────────────────────────────────
        │ import * as React from 'react';
        │ import { Head }
        │   from '../seo';                       ← STEP 17-a-①
        │
        │ export const ContentLayout = ({ children, title }) => (
        │   <>
        │     <Head title={title} />             ← <title>タグを設定
        │     <div className="py-6">
        │       <h1>{title}</h1>
        │       {children}
        │     </div>
        │   </>
        │ );
        └────────────────────────────────────────────

        ↳ STEP 17-a-①  🔵  src/components/seo/head.tsx
            ┌────────────────────────────────────
            │ import { Helmet }
            │   from 'react-helmet-async'; ← 外部ライブラリ
            │
            │ export const Head = ({ title, description }) => (
            │   <Helmet>
            │     <title>
            │       {title ? `${title} | Bulletproof React` : '...'}
            │     </title>
            │     <meta name="description" content={description} />
            │   </Helmet>
            │ );
            └────────────────────────────────────

    ↳ STEP 17-b  🔵  src/components/layouts/dashboard-layout.tsx
        ┌────────────────────────────────────────────
        │ import { Home, PanelLeft, Folder, Users, User2 }
        │   from 'lucide-react';                 ← 外部ライブラリ（アイコン）
        │ import { useEffect, useState } from 'react'; ← 外部ライブラリ
        │ import { NavLink, useNavigate, useNavigation }
        │   from 'react-router';                 ← 外部ライブラリ
        │ import logo from '@/assets/logo.svg';  ← SVG画像ファイル
        │ import { Button }
        │   from '@/components/ui/button';       ← ✅ STEP 8-a で解決済み
        │ import { Drawer, DrawerContent, DrawerTrigger }
        │   from '@/components/ui/drawer';       ← STEP 17-b-①
        │ import { paths }
        │   from '@/config/paths';               ← ✅ STEP 13-a で解決済み
        │ import { useLogout }
        │   from '@/lib/auth';                   ← ✅ STEP 13 で解決済み
        │ import { ROLES, useAuthorization }
        │   from '@/lib/authorization';          ← STEP 17-b-②
        │ import { cn }
        │   from '@/utils/cn';                   ← ✅ STEP 8-b で解決済み
        │ import { DropdownMenu, ... }
        │   from '../ui/dropdown';               ← STEP 17-b-③
        │ import { Link }
        │   from '../ui/link';                   ← STEP 17-b-④
        └────────────────────────────────────────────

        ↳ STEP 17-b-①  🔵  src/components/ui/drawer/drawer.tsx
            ┌────────────────────────────────────
            │ import { cn }
            │   from '@/utils/cn';               ← ✅ 解決済み
            │ import * as DrawerPrimitive
            │   from '@radix-ui/react-dialog';   ← 外部ライブラリ
            └────────────────────────────────────

        ↳ STEP 17-b-②  🔵  src/lib/authorization.tsx
            ┌────────────────────────────────────
            │ import * as React from 'react';
            │ import { Comment, User }
            │   from '@/types/api';              ← ✅ STEP 13-b で解決済み
            │ import { useUser }
            │   from './auth';                   ← ✅ STEP 13 で解決済み
            │
            │ export enum ROLES { ADMIN='ADMIN', USER='USER' }
            │
            │ export const POLICIES = {
            │   'comment:delete': (user, comment) =>
            │     user.role==='ADMIN' || comment.author.id===user.id
            │ };
            │
            │ // ロールチェックHook
            │ export const useAuthorization = () => {
            │   const user = useUser();
            │   const checkAccess = ({ allowedRoles }) =>
            │     allowedRoles.includes(user.data.role);
            │   return { checkAccess, role: user.data.role };
            │ };
            │
            │ // 認可コンポーネント（allowedRoles or policyCheck）
            │ export const Authorization = ({
            │   allowedRoles, policyCheck,
            │   forbiddenFallback, children
            │ }) => { ... };
            └────────────────────────────────────

        ↳ STEP 17-b-③  🔵  src/components/ui/dropdown/dropdown.tsx
            ┌────────────────────────────────────
            │ import * as DropdownMenuPrimitive
            │   from '@radix-ui/react-dropdown-menu'; ← 外部ライブラリ
            │ import { cn }
            │   from '@/utils/cn';               ← ✅ 解決済み
            └────────────────────────────────────

        ↳ STEP 17-b-④  🔵  src/components/ui/link/link.tsx
            ┌────────────────────────────────────
            │ import { Link as RouterLink }
            │   from 'react-router';             ← 外部ライブラリ
            │ import { cn }
            │   from '@/utils/cn';               ← ✅ 解決済み
            └────────────────────────────────────

---

STEP 18  🔵
  [main.tsx の import] → src/testing/mocks/index.ts を読み込む

  ファイル: src/testing/mocks/index.ts
  ┌─────────────────────────────────────────────────────────
  │ import { env }
  │   from '@/config/env';                       ← ✅ STEP 13-c-① で解決済み
  │
  │ export const enableMocking = async () => {
  │   if (env.ENABLE_API_MOCKING) {
  │     // 条件が true の時だけ動的import（起動時には読み込まれない）
  │     const { worker } = await import('./browser'); ← STEP 19（条件付き）
  │     const { initializeDb } = await import('./db'); ← STEP 19-b（条件付き）
  │     await initializeDb();
  │     return worker.start();
  │   }
  │   // ENABLE_API_MOCKING=false なら何もせずreturn
  │ };
  └─────────────────────────────────────────────────────────

---

STEP 19  🔵（ENABLE_API_MOCKING=true の場合のみ）
  [mocks/index.ts の動的import] → src/testing/mocks/browser.ts

  ファイル: src/testing/mocks/browser.ts
  ┌─────────────────────────────────────────────────────────
  │ import { setupWorker }
  │   from 'msw/browser';                        ← 外部ライブラリ
  │ import { handlers }
  │   from './handlers';                         ← STEP 19-a
  │
  │ export const worker = setupWorker(...handlers);
  └─────────────────────────────────────────────────────────

    ↳ STEP 19-a  🔵  src/testing/mocks/handlers/index.ts
        ┌────────────────────────────────────────────
        │ export * from './auth';        ← GET /auth/me, POST /auth/login 等
        │ export * from './comments';    ← GET/POST/DELETE /comments
        │ export * from './discussions'; ← GET/POST/PUT/DELETE /discussions
        │ export * from './teams';       ← GET /teams
        │ export * from './users';       ← GET/PUT/DELETE /users
        └────────────────────────────────────────────
        (各handlerファイルは db.ts からDBモデルを参照してAPIレスポンスを生成)

STEP 19-b  🔵（ENABLE_API_MOCKING=true の場合のみ）
  [mocks/index.ts の動的import] → src/testing/mocks/db.ts

  ファイル: src/testing/mocks/db.ts
  ┌─────────────────────────────────────────────────────────
  │ import { factory, primaryKey } from '@mswjs/data'; ← 外部ライブラリ
  │ import { nanoid } from 'nanoid';             ← 外部ライブラリ
  │
  │ // インメモリDBのスキーマ定義（user, team, discussion, comment）
  │ export const db = factory({ user:{...}, team:{...}, ... });
  │
  │ // initializeDb(): LocalStorageからデータを復元してDBに投入
  │ // persistDb():   DBの状態をLocalStorageに保存
  │ // resetDb():     LocalStorageをクリア
  └─────────────────────────────────────────────────────────
```

---

## フェーズ3 — コード実行

> フェーズ2で全importが解決された後、`main.tsx` の本体コードが実行される。

```
STEP 20  🟢
  [main.tsx] コード本体が実行される

  ① document.getElementById('root') でDOMを取得
     → index.html の <div id="root"></div> を参照

  ② enableMocking() を呼び出す
     → src/testing/mocks/index.ts の enableMocking() が動く

STEP 21  🟢
  [enableMocking()] env.ENABLE_API_MOCKING を確認

  ケースA: ENABLE_API_MOCKING = false（本番・通常開発）
    → 何もせずPromiseがresolve
    → STEP 23へ

  ケースB: ENABLE_API_MOCKING = true（MSW使用時）
    → browser.ts を動的import → worker を取得
    → db.ts を動的import → initializeDb() を実行
       ① LocalStorage の 'msw-db' キーを読み込む
       ② 保存されたデータをインメモリDB(db)に投入
    → worker.start() を実行
       ① ブラウザにService Workerを登録（public/mockServiceWorker.js）
       ② 以降のfetch/XHRをインターセプト可能な状態になる
    → STEP 23へ

STEP 22（ケースBのみ）  🔵
  [worker.start()] → public/mockServiceWorker.js を登録

  ファイル: public/mockServiceWorker.js
  ┌─────────────────────────────────────────────────────────
  │ // ブラウザのService Worker（MSWが自動生成するファイル）
  │ // 全HTTPリクエストを傍受し、handlers に一致するものはモックレスポンスを返す
  │ // 一致しないものは実際のネットワークへ通す
  └─────────────────────────────────────────────────────────

STEP 23  🟢
  [enableMocking() の Promise.then()] createRoot + render が実行される

  ① createRoot(root) でReactのレンダリングエンジンを起動
  ② .render(<React.StrictMode><App /></React.StrictMode>)
     → Reactコンポーネントツリーのレンダリング開始
```

---

## フェーズ4 — Reactのレンダリング開始

> 以降は「関数が呼ばれる順番」。Reactがコンポーネントツリーを上から順に実行していく。

```
STEP 24  🟢
  [render()] → React.StrictMode → App コンポーネントを実行

  ファイル: src/app/index.tsx の App()
  ┌─────────────────────────────────────────────────────────
  │ const App = () => (
  │   <AppProvider>        ← STEP 25
  │     <AppRouter />      ← AppProvider内で後から実行
  │   </AppProvider>
  │ );
  └─────────────────────────────────────────────────────────

STEP 25  🟢
  [App の return] → AppProvider コンポーネントを実行

  ファイル: src/app/provider.tsx の AppProvider()
  ┌─────────────────────────────────────────────────────────
  │ const [queryClient] = React.useState(
  │   () => new QueryClient({ defaultOptions: queryConfig })
  │ );
  │ // QueryClientを生成（APIキャッシュの器）
  │
  │ return (
  │   <React.Suspense fallback={<Spinner size="xl" />}>
  │     <ErrorBoundary FallbackComponent={MainErrorFallback}>
  │       <HelmetProvider>
  │         <QueryClientProvider client={queryClient}>
  │           <ReactQueryDevtools />   ← 開発時のみ
  │           <Notifications />        ← STEP 26
  │           <AuthLoader ...>         ← STEP 27
  │             <AppRouter />          ← STEP 32（AuthLoader完了後）
  │           </AuthLoader>
  │         </QueryClientProvider>
  │       </HelmetProvider>
  │     </ErrorBoundary>
  │   </React.Suspense>
  │ );
  └─────────────────────────────────────────────────────────

STEP 26  🟢
  [AppProvider の return] → Notifications コンポーネントを実行

  ファイル: src/components/ui/notifications/notifications.tsx の Notifications()
  ┌─────────────────────────────────────────────────────────
  │ const { notifications } = useNotifications(); ← Zustandストアから取得
  │ // 初期値は空配列 → 何も表示されない
  │ return (
  │   <div aria-live="assertive" ...>
  │     {/* 空配列なので何も描画されない */}
  │   </div>
  │ );
  └─────────────────────────────────────────────────────────
  → 画面には何も追加されない（通知なし）

STEP 27  🟢
  [AppProvider の return] → AuthLoader コンポーネントを実行

  ファイル: react-query-auth ライブラリの AuthLoader
           （内部で src/lib/auth.tsx の userFn: getUser を呼ぶ）
  ┌─────────────────────────────────────────────────────────
  │ // 内部で useQuery({ queryKey: ['authenticated-user'], queryFn: getUser })
  │ // = GET /auth/me を実行する
  │
  │ if (isLoading) {
  │   return renderLoading(); // → <Spinner size="xl" />を表示
  │ }
  │ return children; // ロード完了後にAppRouterを描画
  └─────────────────────────────────────────────────────────

  この時点でブラウザに見えるもの:
  ┌────────────────────────────────────┐
  │  🔄  (中央にスピナーが表示)          │
  └────────────────────────────────────┘
```

---

## フェーズ5 — 認証確認APIリクエスト

```
STEP 28  🔴
  [AuthLoader] → GET /auth/me を実行

  src/lib/auth.tsx の getUser() が呼ばれる
  ↓
  src/lib/api-client.ts の api.get('/auth/me')
  ↓
  リクエストインターセプター実行:
    - headers.Accept = 'application/json' を付与
    - withCredentials = true を付与（Cookieを送る）
  ↓
  HTTP GET リクエスト送信
  ↓
  [MSW有効の場合] Service Workerがインターセプト
    → src/testing/mocks/handlers/auth.ts の handler が処理
    → db.ts のインメモリDBからユーザーを検索してレスポンス生成
  [MSW無効の場合] 実際のAPIサーバーへ通信

STEP 29  🔴
  [GET /auth/me レスポンス受信]

  ケースA: 200 OK（ログイン済みセッションあり）
    → response.data に User オブジェクト
    → レスポンスインターセプターが response.data を自動抽出
    → React Queryのキャッシュに { queryKey: ['authenticated-user'] } で保存
    → AuthLoader の isLoading = false になる
    → STEP 30へ

  ケースB: 401 Unauthorized（未ログイン）
    → レスポンスインターセプターが 401 を検出
    → window.location.href = '/auth/login?redirectTo=/' に強制リダイレクト
    → ページ全体がリロードされ、ログイン画面から再スタート

STEP 30  🟢（ケースAのみ）
  [AuthLoader] ロード完了 → children (AppRouter) を描画

  → STEP 31へ
```

---

## フェーズ6 — ルーティング解決とランディングページ表示

```
STEP 31  🟢
  [AuthLoader の children] → AppRouter コンポーネントを実行

  ファイル: src/app/router.tsx の AppRouter()
  ┌─────────────────────────────────────────────────────────
  │ const queryClient = useQueryClient(); // QueryClientを取得
  │ const router = useMemo(
  │   () => createAppRouter(queryClient), // ルーターを生成
  │   [queryClient]
  │ );
  │ return <RouterProvider router={router} />;
  └─────────────────────────────────────────────────────────

  createAppRouter() が実行される:
  → createBrowserRouter([...]) でルートテーブルを構築
  → 現在のURL (/) に一致するルートを検索
  → { path: '/', lazy: () => import('./routes/landing') } が一致

STEP 32  🔵⚡
  [React Router] URLが / なので landing を遅延ロード

  → import('./routes/landing') を動的に実行
  → src/app/routes/landing.tsx を初めてダウンロード・評価

  ファイル: src/app/routes/landing.tsx
  ┌─────────────────────────────────────────────────────────
  │ import { useNavigate }
  │   from 'react-router';                       ← ✅ 解決済み
  │ import logo from '@/assets/logo.svg';        ← SVGファイル（新規）
  │ import { Head }
  │   from '@/components/seo';                   ← ✅ STEP 17-a-① で解決済み
  │ import { Button }
  │   from '@/components/ui/button';             ← ✅ STEP 8-a で解決済み
  │ import { paths }
  │   from '@/config/paths';                     ← ✅ STEP 13-a で解決済み
  │ import { useUser }
  │   from '@/lib/auth';                         ← ✅ STEP 13 で解決済み
  │
  │ const LandingRoute = () => {
  │   const navigate = useNavigate();
  │   const user = useUser();           ← React Queryキャッシュからユーザー取得
  │
  │   const handleStart = () => {
  │     if (user.data) {
  │       navigate(paths.app.dashboard.getHref()); // → /app へ
  │     } else {
  │       navigate(paths.auth.login.getHref());    // → /auth/login へ
  │     }
  │   };
  │
  │   return (
  │     <>
  │       <Head description="Welcome to bulletproof react" />
  │       <div className="flex h-screen items-center bg-white">
  │         <div className="mx-auto ...">
  │           <h2>Bulletproof React</h2>
  │           <img src={logo} alt="react" />
  │           <p>Showcasing Best Practices...</p>
  │           <Button onClick={handleStart}>Get started</Button>
  │           <a href="https://github.com/...">
  │             <Button variant="outline">Github Repo</Button>
  │           </a>
  │         </div>
  │       </div>
  │     </>
  │   );
  │ };
  │ export default LandingRoute;
  └─────────────────────────────────────────────────────────

STEP 33  🟢
  [React Router の convert()] lazy import の結果を変換

  ┌─────────────────────────────────────────────────────────
  │ const convert = (queryClient) => (m) => {
  │   const { clientLoader, clientAction,
  │           default: Component, ...rest } = m;
  │   return {
  │     loader: clientLoader?.(queryClient), // landing.tsx にはないのでundefined
  │     action: clientAction?.(queryClient), // landing.tsx にはないのでundefined
  │     Component,                           // = LandingRoute
  │   };
  │ };
  └─────────────────────────────────────────────────────────

STEP 34  🟢
  [React Router] LandingRoute コンポーネントをレンダリング

  LandingRoute() が実行される:
  → useNavigate(): React Router からナビゲーション関数を取得
  → useUser(): React Query キャッシュから認証済みユーザーを取得
  → JSXを返す

STEP 35  🟢
  [Head コンポーネント] → <title> と <meta> を更新

  react-helmet-async が <head> タグの中を更新:
  → <title>Bulletproof React</title>
  → <meta name="description" content="Welcome to bulletproof react" />

STEP 36  🟢  ✅ 画面表示完了

  ブラウザに表示される最終的な画面:
  ┌────────────────────────────────────────────────┐
  │                                                │
  │           Bulletproof React                    │
  │              🔵 (Reactロゴ)                    │
  │   Showcasing Best Practices For Building       │
  │             React Applications                 │
  │                                                │
  │    [🏠 Get started]   [GitHub Repo]            │
  │                                                │
  └────────────────────────────────────────────────┘

  「Get started」クリック時:
  → handleStart() 実行
  → user.data が存在する場合: navigate('/app')   → dashboard.tsx を遅延ロード
  → user.data が null の場合:  navigate('/auth/login') → login.tsx を遅延ロード
```

---

## 全ファイル呼び出し順 サマリー

```
URL入力
│
├─ [1]  index.html                         ← ブラウザが受け取るHTML
│
├─ [2]  src/main.tsx                       ← JSエントリーポイント
│    ├─ [3]  src/index.css                 ← グローバルCSS（Tailwind）
│    │
│    ├─ [4]  src/app/index.tsx             ← App コンポーネント
│    │    ├─ [5]  src/app/provider.tsx     ← Provider群（DI設定）
│    │    │    ├─ [6]  src/components/errors/main.tsx
│    │    │    │    └─ [6a] src/components/ui/button/button.tsx
│    │    │    │         └─ [6b] src/utils/cn.ts
│    │    │    ├─ [7]  src/components/ui/notifications/index.ts
│    │    │    │    ├─ [7a] .../notifications.tsx
│    │    │    │    └─ [7b] .../notifications-store.ts
│    │    │    ├─ [8]  src/components/ui/spinner/spinner.tsx
│    │    │    ├─ [9]  src/lib/auth.tsx    ← 認証フック群
│    │    │    │    ├─ [9a] src/config/paths.ts
│    │    │    │    ├─ [9b] src/types/api.ts
│    │    │    │    └─ [9c] src/lib/api-client.ts
│    │    │    │         └─ [9c-1] src/config/env.ts  ← .envを読んでvalidate
│    │    │    └─ [10] src/lib/react-query.ts
│    │    │
│    │    └─ [11] src/app/router.tsx       ← ルーティング定義
│    │         ├─ (✅ paths, auth 解決済み)
│    │         └─ [12] src/app/routes/app/root.tsx  ← 同期import（AppRoot）
│    │               └─ [13] src/components/layouts/index.ts
│    │                    ├─ [13a] .../content-layout.tsx
│    │                    │    └─ [13a-1] src/components/seo/head.tsx
│    │                    └─ [13b] .../dashboard-layout.tsx
│    │                         ├─ [13b-1] src/components/ui/drawer/drawer.tsx
│    │                         ├─ [13b-2] src/lib/authorization.tsx
│    │                         ├─ [13b-3] src/components/ui/dropdown/dropdown.tsx
│    │                         └─ [13b-4] src/components/ui/link/link.tsx
│    │
│    └─ [14] src/testing/mocks/index.ts   ← モック制御
│         └─ (ENABLE_API_MOCKING=true の時のみ)
│              ├─ [14a] src/testing/mocks/browser.ts
│              │    └─ [14a-1] src/testing/mocks/handlers/index.ts
│              │         ├─ handlers/auth.ts
│              │         ├─ handlers/comments.ts
│              │         ├─ handlers/discussions.ts
│              │         ├─ handlers/teams.ts
│              │         └─ handlers/users.ts
│              └─ [14b] src/testing/mocks/db.ts
│
├─ [実行] enableMocking()                  ← モック初期化（or スキップ）
│
├─ [実行] createRoot().render(<App />)     ← Reactレンダリング開始
│
├─ [render] AppProvider → Notifications   ← 空（通知なし）
│
├─ [render] AuthLoader
│    └─ 🔴 GET /auth/me                   ← 認証確認APIリクエスト
│         → スピナー表示
│         → レスポンス受信後スピナー消える
│
├─ [render] AppRouter → createBrowserRouter
│    └─ URL "/" に一致
│
└─ [⚡遅延] src/app/routes/landing.tsx    ← この時点で初めてロード
     └─ LandingRoute をレンダリング
          └─ ✅ 画面表示完了
```

---

## 遅延ロードされるファイル一覧

起動時には読み込まれず、**その画面に遷移したときに初めて**ダウンロードされるファイル:

| URL | 遅延ロードされるファイル | さらにimportするもの |
|---|---|---|
| `/` | `routes/landing.tsx` | logo.svg, Head, Button, paths, useUser |
| `/auth/login` | `routes/auth/login.tsx` | AuthLayout, LoginForm → features/auth/components/login-form.tsx |
| `/auth/register` | `routes/auth/register.tsx` | AuthLayout, RegisterForm, useTeams |
| `/app`（ダッシュボード） | `routes/app/dashboard.tsx` | ContentLayout, useUser, ROLES |
| `/app/discussions` | `routes/app/discussions/discussions.tsx` | DiscussionsList, CreateDiscussion, React Query hooks |
| `/app/discussions/:id` | `routes/app/discussions/discussion.tsx` | DiscussionView, Comments, ErrorBoundary |
| `/app/users` | `routes/app/users.tsx` | UsersList, Authorization |
| `/app/profile` | `routes/app/profile.tsx` | UpdateProfile, useUser |
| 存在しないURL | `routes/not-found.tsx` | Link, paths |
