# Axios → fetch ラッパー 移行ガイド

> **背景**: Axios に脆弱性が発見されたため、ネイティブ `fetch` API をラップして  
> Axios と同等の機能を実現する。既存の呼び出しコード（`api.get()` / `api.post()` 等）は**一切変更しない**ことを目標とする。

---

## 目次

1. [なぜ fetch で Axios の代替ができるのか](#1-なぜ-fetch-で-axios-の代替ができるのか)
2. [Axios と fetch の機能対応表](#2-axios-と-fetch-の機能対応表)
3. [現在プロジェクトで使っている Axios の機能](#3-現在プロジェクトで使っている-axios-の機能)
4. [fetch ラッパーの完全実装](#4-fetch-ラッパーの完全実装)
5. [api-client.ts の置き換え](#5-api-clientts-の置き換え)
6. [各機能の仕組み詳解](#6-各機能の仕組み詳解)
7. [既存コードへの影響確認](#7-既存コードへの影響確認)
8. [エラーハンドリングの違いと注意点](#8-エラーハンドリングの違いと注意点)
9. [移行チェックリスト](#9-移行チェックリスト)

---

## 1. なぜ fetch で Axios の代替ができるのか

### Axios の主な既知脆弱性

| CVE | 影響バージョン | 内容 |
|---|---|---|
| **CVE-2023-45857** | `< 1.6.0` | `XSRF-TOKEN` を第三者オリジンのリクエストヘッダーに誤って付与 → CSRF攻撃に悪用可能 |
| **CVE-2024-39338** | `< 1.7.4` | サーバーサイド実行時のSSRF（Server-Side Request Forgery）脆弱性 |
| **Prototype Pollution** | 複数バージョン | `merge` ユーティリティでの Prototype Pollution（過去に複数報告） |

> **Axios は「ブラウザ組み込み fetch のラッパー」に過ぎない**。  
> 本質的に同じ処理を fetch で実装すれば、脆弱性リスクゼロ + 依存ゼロで同等の機能が得られる。

### fetch で実現できること

```
Axios.create({ baseURL })        →  ファクトリ関数で baseURL を保持するクラスを作る
interceptors.request.use()       →  リクエスト送信前に関数チェーンを実行する
interceptors.response.use()      →  レスポンス受信後に関数チェーンを実行する
response.data の自動抽出          →  response.json() を呼び出すだけ
params: { page: 1 }              →  URLSearchParams で URL に付与する
withCredentials: true            →  fetch の credentials: 'include' オプション
エラー時の error.response.status →  カスタムエラークラスで同じプロパティを持たせる
```

---

## 2. Axios と fetch の機能対応表

| Axios の機能 | fetch での実現方法 | 難易度 |
|---|---|---|
| `Axios.create({ baseURL })` | `new URL(path, baseURL).toString()` | ★☆☆ |
| `config.headers.Accept = '...'` | `headers: { Accept: '...' }` | ★☆☆ |
| `withCredentials: true` | `credentials: 'include'` | ★☆☆ |
| JSON body の自動シリアライズ | `JSON.stringify(data)` + `Content-Type` ヘッダー | ★☆☆ |
| `response.data` の自動抽出 | `await response.json()` | ★☆☆ |
| `params: { page: 1 }` | `URLSearchParams` で URL に付与 | ★★☆ |
| リクエストインターセプター | 送信前に関数チェーンを実行するメソッド | ★★☆ |
| レスポンスインターセプター | 受信後に関数チェーンを実行するメソッド | ★★☆ |
| `error.response.status` | カスタム `ApiError` クラス | ★★☆ |
| `error.response.data.message` | カスタム `ApiError` クラス | ★★☆ |
| タイムアウト | `AbortController` + `setTimeout` | ★★★ |
| リトライ | `for` ループ or 再帰 | ★★★ |

> 今回のプロジェクトで使用中の機能は **タイムアウト・リトライ以外のすべて**。  
> これらは fetch で完全に再現できる。

---

## 3. 現在プロジェクトで使っている Axios の機能

`src/lib/api-client.ts` の現在の実装から抽出:

```typescript
// ① baseURL を持つインスタンスを作成
export const api = Axios.create({
  baseURL: env.API_URL,
});

// ② リクエストインターセプター
//    - Accept: application/json を付与
//    - Cookie を自動送信（withCredentials）
api.interceptors.request.use((config) => {
  config.headers.Accept = 'application/json';
  config.withCredentials = true;
  return config;
});

// ③ レスポンスインターセプター（成功時）
//    - response.data を自動抽出（呼び出し側は直接データが受け取れる）
api.interceptors.response.use(
  (response) => response.data,
  ...
);

// ④ レスポンスインターセプター（エラー時）
//    - エラー通知を表示（Zustand）
//    - 401 なら /auth/login にリダイレクト
api.interceptors.response.use(
  ...,
  (error) => {
    const message = error.response?.data?.message || error.message;
    useNotifications.getState().addNotification({ type: 'error', ... });
    if (error.response?.status === 401) {
      window.location.href = paths.auth.login.getHref(...);
    }
    return Promise.reject(error);
  }
);
```

**呼び出し側で使われているメソッド**:

```typescript
api.get('/discussions', { params: { page } })   // クエリパラメータあり
api.get('/auth/me')                              // シンプルなGET
api.post('/discussions', data)                  // JSONボディのPOST
api.post('/auth/login', data)                   // 同上
api.patch(`/discussions/${id}`, data)           // 部分更新
api.delete(`/discussions/${id}`)                // 削除
```

---

## 4. fetch ラッパーの完全実装

> **ファイル**: `src/lib/api-client.ts` を以下のコードで**まるごと置き換える**。

```typescript
// ─────────────────────────────────────────────
//  型定義
// ─────────────────────────────────────────────

/** リクエスト設定（インターセプターで加工する対象） */
type RequestConfig = {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  headers: Record<string, string>;
  params?: Record<string, string | number | boolean | null | undefined>;
  body?: unknown;
  credentials: RequestCredentials;
};

type RequestInterceptor = (
  config: RequestConfig,
) => RequestConfig | Promise<RequestConfig>;

type ResponseInterceptorHandlers = {
  onFulfilled?: (data: unknown) => unknown | Promise<unknown>;
  onRejected?: (error: ApiError) => never | unknown;
};

// ─────────────────────────────────────────────
//  カスタムエラークラス
//  Axiosの error.response.status / error.response.data と同じ構造を持つ
// ─────────────────────────────────────────────

export class ApiError extends Error {
  response: {
    status: number;
    data: unknown;
  };

  constructor(message: string, status: number, data: unknown) {
    super(message);
    this.name = 'ApiError';
    this.response = { status, data };
  }
}

// ─────────────────────────────────────────────
//  ApiClient クラス
//  Axios.create() で返ってくるインスタンスと同じインターフェース
// ─────────────────────────────────────────────

class ApiClient {
  readonly #baseURL: string;
  readonly #requestInterceptors: RequestInterceptor[] = [];
  readonly #responseInterceptors: ResponseInterceptorHandlers[] = [];

  /** Axios の interceptors プロパティと同じ構造 */
  interceptors = {
    request: {
      use: (fn: RequestInterceptor) => {
        this.#requestInterceptors.push(fn);
      },
    },
    response: {
      use: (
        onFulfilled?: ResponseInterceptorHandlers['onFulfilled'],
        onRejected?: ResponseInterceptorHandlers['onRejected'],
      ) => {
        this.#responseInterceptors.push({ onFulfilled, onRejected });
      },
    },
  };

  constructor(baseURL: string) {
    this.#baseURL = baseURL;
  }

  // ─────────────────────────────────────────
  //  コアリクエストメソッド
  // ─────────────────────────────────────────

  async #request<T>(config: RequestConfig): Promise<T> {
    // ① リクエストインターセプターを順番に適用
    let finalConfig = { ...config };
    for (const interceptor of this.#requestInterceptors) {
      finalConfig = await interceptor(finalConfig);
    }

    // ② URL を組み立てる（baseURL + path + クエリパラメータ）
    const url = new URL(finalConfig.url, this.#baseURL);
    if (finalConfig.params) {
      for (const [key, value] of Object.entries(finalConfig.params)) {
        if (value !== undefined && value !== null) {
          url.searchParams.append(key, String(value));
        }
      }
    }

    // ③ fetch に渡すオプションを組み立てる
    const headers = { ...finalConfig.headers };
    const fetchOptions: RequestInit = {
      method: finalConfig.method,
      headers,
      credentials: finalConfig.credentials,
    };

    if (finalConfig.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      fetchOptions.body = JSON.stringify(finalConfig.body);
    }

    // ④ fetch 実行
    let response: Response;
    try {
      response = await fetch(url.toString(), fetchOptions);
    } catch {
      // ネットワークエラー（サーバーに到達できない場合）
      const networkError = new ApiError('Network Error', 0, null);
      return this.#handleError<T>(networkError);
    }

    // ⑤ レスポンスをパース
    const contentType = response.headers.get('content-type') ?? '';
    const data: unknown = contentType.includes('application/json')
      ? await response.json()
      : await response.text();

    // ⑥ HTTPエラー（4xx, 5xx）の場合は ApiError を生成
    if (!response.ok) {
      const message =
        (data as Record<string, unknown>)?.message as string | undefined
        ?? response.statusText;
      const apiError = new ApiError(message, response.status, data);
      return this.#handleError<T>(apiError);
    }

    // ⑦ 成功レスポンスにインターセプターを適用
    let result: unknown = data;
    for (const { onFulfilled } of this.#responseInterceptors) {
      if (onFulfilled) result = await onFulfilled(result);
    }
    return result as T;
  }

  /** エラーにレスポンスインターセプターの onRejected を適用して投げ直す */
  async #handleError<T>(error: ApiError): Promise<T> {
    let result: unknown = error;
    for (const { onRejected } of this.#responseInterceptors) {
      if (onRejected) {
        try {
          result = await onRejected(result as ApiError);
        } catch (e) {
          result = e;
        }
      }
    }
    if (result instanceof Error) throw result;
    return result as T;
  }

  // ─────────────────────────────────────────
  //  Axios 互換の HTTP メソッド
  // ─────────────────────────────────────────

  get<T = unknown>(
    url: string,
    config?: { params?: RequestConfig['params'] },
  ): Promise<T> {
    return this.#request<T>({
      url,
      method: 'GET',
      headers: {},
      params: config?.params,
      credentials: 'include',
    });
  }

  post<T = unknown>(url: string, data?: unknown): Promise<T> {
    return this.#request<T>({
      url,
      method: 'POST',
      headers: {},
      body: data,
      credentials: 'include',
    });
  }

  put<T = unknown>(url: string, data?: unknown): Promise<T> {
    return this.#request<T>({
      url,
      method: 'PUT',
      headers: {},
      body: data,
      credentials: 'include',
    });
  }

  patch<T = unknown>(url: string, data?: unknown): Promise<T> {
    return this.#request<T>({
      url,
      method: 'PATCH',
      headers: {},
      body: data,
      credentials: 'include',
    });
  }

  delete<T = unknown>(url: string): Promise<T> {
    return this.#request<T>({
      url,
      method: 'DELETE',
      headers: {},
      credentials: 'include',
    });
  }
}

/** Axios.create() 相当のファクトリ関数 */
export const createApiClient = (config: { baseURL: string }) =>
  new ApiClient(config.baseURL);
```

---

## 5. api-client.ts の置き換え

現在の `src/lib/api-client.ts` をそのまま以下に差し替える。  
**呼び出し側のコード（`api.get()` / `api.post()` 等）は一切変更不要**。

```typescript
// src/lib/api-client.ts（Axios → fetch ラッパーへの置き換え）

import { useNotifications } from '@/components/ui/notifications';
import { env } from '@/config/env';
import { paths } from '@/config/paths';

import { createApiClient } from './fetch-client'; // ← 上記のラッパーをここに配置

export const api = createApiClient({
  baseURL: env.API_URL,
});

// ── リクエストインターセプター ─────────────────────────────────
// Axios版: config.headers.Accept = '...' / config.withCredentials = true
// fetch版: headers を加工して返す（credentials は各メソッドで設定済み）
api.interceptors.request.use((config) => {
  config.headers['Accept'] = 'application/json';
  return config;
});

// ── レスポンスインターセプター ─────────────────────────────────
api.interceptors.response.use(
  // 成功時: APIレスポンス本体をそのまま返す（Axios版の response.data 相当）
  // fetch版ではすでに response.json() 済みなので、ここでは何もせずそのまま返す
  (data) => data,

  // エラー時: 通知を出して、401 ならログインへ飛ばす
  (error) => {
    const message =
      (error.response?.data as Record<string, unknown>)?.message as string
      ?? error.message;

    useNotifications.getState().addNotification({
      type: 'error',
      title: 'Error',
      message,
    });

    if (error.response?.status === 401) {
      const redirectTo = new URLSearchParams(window.location.search).get('redirectTo')
        ?? window.location.pathname;
      window.location.href = paths.auth.login.getHref(redirectTo);
    }

    return Promise.reject(error);
  },
);
```

> **配置場所の提案**:
> ```
> src/lib/
>   ├── fetch-client.ts   ← ApiClient クラスの実装（セクション4のコード）
>   └── api-client.ts     ← fetch-client を使って api インスタンスを作成・設定
> ```

---

## 6. 各機能の仕組み詳解

### 6-1. baseURL

**Axios**:
```typescript
Axios.create({ baseURL: 'https://api.example.com' });
// 以後 api.get('/users') → 'https://api.example.com/users' に送信
```

**fetch ラッパー**:
```typescript
// new URL(path, baseURL) が同じことをする
const url = new URL('/users', 'https://api.example.com');
url.toString(); // → 'https://api.example.com/users'
```

---

### 6-2. クエリパラメータ（params）

**Axios**:
```typescript
api.get('/discussions', { params: { page: 2 } });
// → GET /discussions?page=2
```

**fetch ラッパー**:
```typescript
// URLSearchParams を使って URL に付与する
const url = new URL('/discussions', baseURL);
url.searchParams.append('page', '2');
url.toString(); // → 'https://api.example.com/discussions?page=2'
```

> 実際の呼び出しコードは変わらない:
> ```typescript
> // get-discussions.ts はそのままでOK
> api.get(`/discussions`, { params: { page } });
> ```

---

### 6-3. Cookie の送信（withCredentials）

**Axios**:
```typescript
config.withCredentials = true;
// → リクエスト時にCookieを付与し、Set-Cookieも受け付ける
```

**fetch**:
```typescript
credentials: 'include'
// 完全に同じ動作。CORS時もCookieを送受信する。
```

> ラッパーでは全HTTPメソッドに `credentials: 'include'` をデフォルト設定済み。  
> インターセプターで設定する必要はない。

---

### 6-4. JSONボディの自動シリアライズ

**Axios**: `data` を渡すだけで自動的に `JSON.stringify` + `Content-Type: application/json` を付与。

**fetch ラッパー**:
```typescript
// body が存在する場合に自動で行う
if (finalConfig.body !== undefined) {
  headers['Content-Type'] = 'application/json';
  fetchOptions.body = JSON.stringify(finalConfig.body);
}
```

> 呼び出しコードは変わらない:
> ```typescript
> api.post('/discussions', { title: 'Hello', body: 'World' });
> // → Content-Type: application/json が自動付与される
> ```

---

### 6-5. リクエストインターセプター

Java/Spring Boot との対比:

```
Axios interceptors.request.use()
  ≒ Spring の HandlerInterceptor.preHandle()
  ≒ Spring の ClientHttpRequestInterceptor (RestTemplate)
  ≒ Spring の ExchangeFilterFunction (WebClient)
```

**動作の仕組み**:
```typescript
// 登録
api.interceptors.request.use((config) => {
  config.headers['Accept'] = 'application/json';
  return config;             // ← 加工したconfigを返す（必須）
});

// 内部では配列に積んで順番に実行する
for (const interceptor of this.#requestInterceptors) {
  finalConfig = await interceptor(finalConfig);
}
// fetch() はインターセプター適用後の finalConfig で実行される
```

---

### 6-6. レスポンスインターセプター（成功時）

**動作の仕組み**:
```typescript
// response.json() で取得した data に対してインターセプターを適用
let result: unknown = data;
for (const { onFulfilled } of this.#responseInterceptors) {
  if (onFulfilled) result = await onFulfilled(result);
}
return result as T;
```

**現在の設定**（成功時はそのまま返す）:
```typescript
api.interceptors.response.use(
  (data) => data,  // ← response.json() 済みのデータをそのまま返す
  ...
);
```

> Axios版は `response.data`（Axiosのレスポンスオブジェクトから`.data`を取り出す）  
> fetch版はすでに `response.json()` 済みなので `.data` へのアクセスは不要

---

### 6-7. レスポンスインターセプター（エラー時）

```typescript
api.interceptors.response.use(
  undefined, // 成功時のハンドラーは省略可能
  (error) => {
    // error.response.status   → fetch後に ApiError に格納した HTTP ステータス
    // error.response.data     → fetch後に response.json() で取得したボディ
    // error.message           → ApiError コンストラクタで設定したメッセージ
    ...
  }
);
```

**ApiError の構造**（Axios の AxiosError と同じ構造）:
```
ApiError
  ├── message: string          ← error.message
  └── response
       ├── status: number      ← error.response.status  (例: 401, 404, 500)
       └── data: unknown       ← error.response.data    (レスポンスボディ全体)
            └── message: string ← error.response.data.message (APIのエラーメッセージ)
```

---

### 6-8. 404 / 500 時の Axios との違い

| 状況 | Axios の動作 | fetch の動作 | ラッパーの動作 |
|---|---|---|---|
| 200 OK | resolve | resolve | resolve |
| 4xx / 5xx | **reject（例外）** | **resolve**（！） | **reject（例外）** ← Axiosに揃える |
| ネットワーク断 | reject | reject | reject |

> ⚠️ **fetch の重要な落とし穴**: fetch は `404` や `500` でも `Promise.reject()` しない。  
> `response.ok` が `false` になるだけ。ラッパーで `response.ok` チェックして  
> `ApiError` をthrowすることで Axios と同じ動作にしている。

```typescript
// ラッパー内のチェック
if (!response.ok) {
  const apiError = new ApiError(message, response.status, data);
  return this.#handleError<T>(apiError); // ← onRejected を通してからthrow
}
```

---

## 7. 既存コードへの影響確認

### 変更が必要なファイル

| ファイル | 変更内容 |
|---|---|
| `src/lib/fetch-client.ts` | **新規作成**（ApiClient クラス） |
| `src/lib/api-client.ts` | **書き換え**（Axios → createApiClient） |
| `package.json` | `axios` を依存から削除 |

### 変更不要なファイル（そのまま動く）

```
src/lib/auth.tsx                              ← api.get(), api.post() そのまま
src/features/discussions/api/get-discussions.ts ← api.get({ params }) そのまま
src/features/discussions/api/create-discussion.ts ← api.post() そのまま
src/features/discussions/api/update-discussion.ts ← api.patch() そのまま
src/features/discussions/api/delete-discussion.ts ← api.delete() そのまま
src/features/comments/api/get-comments.ts    ← api.get({ params }) そのまま
src/features/comments/api/create-comment.ts  ← api.post() そのまま
src/features/comments/api/delete-comment.ts  ← api.delete() そのまま
src/features/users/api/get-users.ts          ← api.get() そのまま
src/features/users/api/update-profile.ts     ← api.patch() そのまま
src/features/users/api/delete-user.ts        ← api.delete() そのまま
src/features/teams/api/get-teams.ts          ← api.get() そのまま
```

**具体例：変更前後の比較**

```typescript
// ──────── 変更前（Axios） ────────
// get-discussions.ts
import { api } from '@/lib/api-client';

export const getDiscussions = (page = 1) => {
  return api.get(`/discussions`, { params: { page } });
};

// ──────── 変更後（fetch ラッパー） ────────
// get-discussions.ts（まったく同じ！変更不要）
import { api } from '@/lib/api-client';

export const getDiscussions = (page = 1) => {
  return api.get(`/discussions`, { params: { page } });
};
```

---

## 8. エラーハンドリングの違いと注意点

### 型の違い

Axiosのエラーは `AxiosError` 型、fetchラッパーは `ApiError` 型。  
エラーを型で判定している箇所があれば変更が必要。

```typescript
// Axios の場合
import axios from 'axios';
if (axios.isAxiosError(error)) { ... }

// fetch ラッパーの場合
import { ApiError } from '@/lib/fetch-client';
if (error instanceof ApiError) { ... }
```

### テストコードへの影響

MSWを使ったテストは fetch をインターセプトするため、**MSW は fetch でも問題なく動作する**。  
（MSWはもともとfetchをインターセプトする仕組みのため、Axiosよりfetchの方が相性が良い）

```typescript
// テストコードでのエラーモック（変更例）
// Before: AxiosError をモックする
// After: ApiError をモックする

// MSWのハンドラー側は変更不要（HTTPレスポンスレベルで動作するため）
```

### TypeScript の型安全性

fetchラッパーはジェネリクスをサポートしているため、Axiosと同様に型付きで使える:

```typescript
// 型パラメータを明示する場合（変更不要）
const user = await api.get<User>('/auth/me');
const discussions = await api.get<{ data: Discussion[]; meta: Meta }>('/discussions');
```

---

## 9. 移行チェックリスト

```
□ src/lib/fetch-client.ts を新規作成（セクション4のコードをコピー）
□ src/lib/api-client.ts を書き換え（セクション5のコードに差し替え）
□ npm/yarn で axios をアンインストール
    yarn remove axios
□ 動作確認: ログイン → ダッシュボード → 一覧取得 → 作成 → 削除
□ 401 動作確認: 無効なCookieでアクセス → /auth/login にリダイレクトされるか
□ エラー通知確認: 存在しないIDにアクセス → 右上にエラートーストが出るか
□ AxiosError 型を直接参照しているコードがないか確認
    grep -r "AxiosError\|isAxiosError\|from 'axios'" src/
```

---

## 補足: fetch が Axios より優れている点

| 観点 | Axios | fetch ラッパー |
|---|---|---|
| **バンドルサイズ** | ~14KB | 0KB（ブラウザ組み込み） |
| **依存リスク** | 脆弱性発見のリスクあり | なし |
| **MSW との相性** | △ (XHR adapter 経由) | ◎ (fetch を直接インターセプト) |
| **ブラウザサポート** | IE11対応（レガシー） | モダンブラウザ全対応 |
| **Node.js 18+** | 対応 | 対応（Node 18 以降 fetch 標準搭載） |
| **TypeScript** | 型定義別パッケージ | ブラウザ型定義に含まれる |
