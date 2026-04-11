# Bulletproof React — 全ファイル解説（Java/Spring Boot対比付き）

> **本書の目的**: `apps/react-vite` の全ファイルについて、「何をしているか」「なぜ必要か」を1つずつ解説する。
> Java/Spring Boot経験者が「ああ、あれのことか」と直感的に理解できるよう、対応する概念を併記する。

---

## 目次

1. [読み方ガイド — React と Spring Boot の構造対応表](#1-読み方ガイド)
2. [ルート設定ファイル群](#2-ルート設定ファイル群)
3. [src/ エントリーポイント](#3-src-エントリーポイント)
4. [src/app/ — アプリケーションシェル](#4-srcapp--アプリケーションシェル)
5. [src/app/routes/ — 画面定義（ページコンポーネント）](#5-srcapproutes--画面定義)
6. [src/lib/ — 共通ライブラリ層](#6-srclib--共通ライブラリ層)
7. [src/config/ — 設定](#7-srcconfig--設定)
8. [src/components/ — 共通UIコンポーネント](#8-srccomponents--共通uiコンポーネント)
9. [src/features/ — Feature モジュール](#9-srcfeatures--featureモジュール)
10. [src/hooks/ — 共通カスタムフック](#10-srchooks--共通カスタムフック)
11. [src/types/ — 型定義](#11-srctypes--型定義)
12. [src/utils/ — ユーティリティ関数](#12-srcutils--ユーティリティ関数)
13. [src/testing/ — テスト基盤](#13-srctesting--テスト基盤)
14. [その他のディレクトリ](#14-その他のディレクトリ)

---

## 1. 読み方ガイド

### React と Spring Boot の構造対応表

この表を頭に入れておくと、以降の解説が「ああ、あれか」で済む。

| React (Bulletproof React) | Spring Boot | 抽象的な役割 |
|---|---|---|
| `main.tsx` | `Application.java` の `main()` | アプリのエントリーポイント |
| `app/provider.tsx` | `@Configuration` クラス群 + `application.yml` | DI設定・グローバル設定の定義 |
| `app/router.tsx` | `@Controller` + `@RequestMapping` のルーティング定義 | URLとハンドラーの対応付け |
| `app/routes/discussions.tsx` | `DiscussionController.java` | 特定のURL（画面）のハンドラー |
| `features/discussions/api/` | `DiscussionService.java` + `DiscussionRepository.java` | ビジネスロジック + データアクセス |
| `features/discussions/components/` | Thymeleaf テンプレート（`discussions.html`） | 画面描画（View層） |
| `lib/api-client.ts` | `RestTemplate` / `WebClient` の共通設定 | HTTPクライアントの一元管理 |
| `lib/auth.tsx` | `SecurityConfig.java` + `UserDetailsService` | 認証の設定と実装 |
| `lib/authorization.tsx` | `@PreAuthorize` / `@Secured` | 認可のルール定義 |
| `lib/react-query.ts` | Spring Cache の設定 (`@EnableCaching`) | キャッシュ戦略のグローバル設定 |
| `config/env.ts` | `application.yml` + `@ConfigurationProperties` | 環境変数の読み込みとバリデーション |
| `config/paths.ts` | ルート定数クラス（`Routes.java`） | URLパスの一元管理 |
| `types/api.ts` | DTO クラス群 (`DiscussionDto.java`) | データ転送オブジェクトの型定義 |
| `components/ui/` | 共通UIテンプレートフラグメント | 再利用可能なUI部品 |
| `components/layouts/` | Thymeleafレイアウト（`layout.html`） | ページ共通のヘッダー・サイドバー |
| `testing/mocks/` | `@MockBean` + WireMock | テスト用のモック定義 |
| `testing/test-utils.tsx` | テスト用 `@SpringBootTest` のベースクラス | テスト共通のセットアップ |
| `package.json` | `pom.xml` / `build.gradle` | 依存管理とビルドスクリプト |
| `tsconfig.json` | `pom.xml` のコンパイラ設定 | コンパイラ（型チェック）の設定 |
| `vite.config.ts` | Maven/Gradle プラグイン設定 | ビルドツールの設定 |
| `.eslintrc.cjs` | Checkstyle / SpotBugs の設定 | 静的解析ルール |
| `.prettierrc` | Google Java Format の設定 | コードフォーマッタ設定 |

### Spring Boot と対比した全体像

```
Spring Boot                          React (Bulletproof React)
─────────────────────────────────    ─────────────────────────────
pom.xml                              package.json
application.yml                      config/env.ts + .env
@SpringBootApplication               main.tsx
@Configuration(Security等)            app/provider.tsx
@Controller + @RequestMapping        app/router.tsx
  └─ 各Controller                      └─ app/routes/
@Service                             features/*/api/ (React Queryフック)
@Repository                          features/*/api/ (API呼び出し関数)
DTO                                  types/api.ts
Thymeleafテンプレート                  features/*/components/ + components/
SecurityConfig                       lib/auth.tsx + lib/authorization.tsx
RestTemplate                         lib/api-client.ts
共通ユーティリティ                     utils/ + hooks/
テスト                                testing/ + __tests__/
```

---

## 2. ルート設定ファイル群

プロジェクトルートにある設定ファイル。Spring Bootでいう `pom.xml`、`application.yml`、Checkstyle設定などに相当する。

---

### `package.json`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 依存ライブラリの定義、ビルド/テスト/起動のスクリプト定義 |
| **Spring Boot対応** | **`pom.xml`（Maven）** または **`build.gradle`（Gradle）** |
| **なぜ必要か** | `npm install` / `yarn install` で依存を解決する。`scripts` でビルド・テスト・起動コマンドを統一する |

**主要スクリプト**:

| コマンド | 何が起きるか | Spring Boot対応 |
|---|---|---|
| `yarn dev` | Vite開発サーバー起動（ホットリロード付き） | `mvn spring-boot:run` |
| `yarn build` | 本番用にバンドル（`dist/` に出力） | `mvn package` |
| `yarn test` | Vitest でユニットテスト実行 | `mvn test` |
| `yarn lint` | ESLint で静的解析 | `mvn checkstyle:check` |
| `yarn check-types` | TypeScript型チェック（コンパイルのみ） | `mvn compile`（型エラー検出） |
| `yarn generate` | Plop でファイルテンプレート生成 | Mavenアーキタイプ `mvn archetype:generate` |
| `yarn storybook` | UIカタログ起動 | Swagger UIに近い（UIの可視化） |
| `yarn test-e2e` | Playwright でE2Eテスト | Selenium / Cypress のE2Eテスト |

**主要依存ライブラリ**:

| ライブラリ | 役割 | Spring Boot対応 |
|---|---|---|
| `react` | UIフレームワーク | Thymeleaf（View層） |
| `react-router` | ルーティング | Spring MVC の `@RequestMapping` |
| `@tanstack/react-query` | サーバーデータ管理（キャッシュ・同期） | Spring Cache + 非同期HTTP呼び出し |
| `axios` | HTTPクライアント | `RestTemplate` / `WebClient` |
| `zod` | ランタイムバリデーション | Bean Validation (`@Valid`, `@NotNull`) |
| `react-hook-form` | フォーム状態管理 | Spring MVC の `@ModelAttribute` + BindingResult |
| `zustand` | クライアント状態管理 | HttpSession / アプリケーションスコープBean |
| `tailwindcss` | CSSフレームワーク | Bootstrap（UI装飾） |

---

### `tsconfig.json`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | TypeScriptコンパイラの設定。型チェックの厳格度、モジュール解決方式、パスエイリアスを定義 |
| **Spring Boot対応** | `pom.xml` の `<maven.compiler.source>` + Lombokの設定に近い |
| **なぜ必要か** | `strict: true` で型安全を強制。`@/*` エイリアスで `src/` 配下を絶対パスのようにインポートできる |

**主要設定**:

```jsonc
{
  "compilerOptions": {
    "target": "ESNext",           // コンパイルターゲット（Java で言う -source 17）
    "strict": true,               // 厳格な型チェック（Javaの-Xlintに相当）
    "jsx": "react-jsx",           // JSX変換方式
    "moduleResolution": "bundler",// Viteに最適なモジュール解決
    "paths": { "@/*": ["./src/*"] } // import '@/lib/auth' → src/lib/auth.tsx
  }
}
```

`paths` の `@/*` は Spring Boot でいう「パッケージのルート」。`import { api } from '@/lib/api-client'` と書けば、どのディレクトリからでも同じパスでインポートできる。Javaで `import com.example.lib.ApiClient;` と書くのと同じ感覚。

---

### `vite.config.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ビルドツール Vite の設定。開発サーバー、プラグイン、テスト環境を定義 |
| **Spring Boot対応** | **Maven/Gradleプラグイン設定** + **組み込みTomcatの設定** |
| **なぜ必要か** | 開発時はホットリロード付きサーバー（port:3000）、テスト時はjsdom環境、本番ビルド時はバンドル最適化 |

```typescript
export default defineConfig({
  plugins: [react(), viteTsconfigPaths()],   // Reactプラグイン + パスエイリアス
  server: { port: 3000 },                    // 開発サーバーポート（Tomcatの8080に相当）
  test: {
    globals: true,                            // describe/it をインポート不要に
    environment: 'jsdom',                     // ブラウザ環境をシミュレート
    setupFiles: './src/testing/setup-tests.ts', // テスト前の共通セットアップ
  },
});
```

Spring Boot で `@SpringBootTest` に `webEnvironment = WebEnvironment.MOCK` を指定するのと似ている。テスト時にブラウザDOMをシミュレートする。

---

### `.eslintrc.cjs`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 静的解析ルールの定義。インポートの制約、命名規則、コード品質ルール |
| **Spring Boot対応** | **Checkstyle** / **SpotBugs** / **ArchUnit** |
| **なぜ必要か** | Feature間の不正なインポートをビルド時に検出。アーキテクチャ違反を防ぐ |

**最重要ルール**:

```javascript
// features/ 配下のモジュール間の直接インポートを禁止
// → ArchUnitで「serviceパッケージからcontrollerパッケージをimportしてはいけない」と書くのと同じ
'no-restricted-imports': [
  { patterns: ['@/features/*/*'] }  // features/discussions/components/xxx のような深いインポートを禁止
]
```

これはArchUnitの `noClasses().that().resideInAPackage("..service..").should().dependOnClassesThat().resideInAPackage("..controller..")` と同じ思想。Feature間の境界を静的解析で強制する。

他にも:
- **ファイル名はkebab-case**（`discussion-list.tsx`）— Javaの `DiscussionList.java` と異なるが、React/Node.jsの慣習
- **フォルダ名もkebab-case**（`get-discussions`）
- **インポート順序の強制** — builtin → external → internal → parent → sibling

---

### `.prettierrc`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | コードフォーマットの統一ルール |
| **Spring Boot対応** | **Google Java Format** / **EditorConfig** |
| **なぜ必要か** | 全員のコードスタイルを自動で統一。PR上でスタイルの議論が不要になる |

```json
{
  "singleQuote": true,     // 文字列はシングルクォートに統一
  "trailingComma": "all",  // 末尾カンマを常に付ける（git diffが見やすくなる）
  "printWidth": 80,        // 1行の最大文字数
  "tabWidth": 2            // インデント幅
}
```

---

### `postcss.config.cjs`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | CSSの後処理パイプライン設定 |
| **Spring Boot対応** | Spring Bootには直接の対応なし。CSSのコンパイル設定 |
| **なぜ必要か** | TailwindCSSのユーティリティクラスを実際のCSSに変換する。Autoprefixerでブラウザ互換性を自動付与 |

```javascript
module.exports = {
  plugins: {
    tailwindcss: {},   // Tailwindのユーティリティクラスをコンパイル
    autoprefixer: {},  // -webkit- 等のベンダープレフィックスを自動付与
  },
};
```

---

### `tailwind.config.cjs`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | TailwindCSSのテーマ設定（色・フォント・アニメーション） |
| **Spring Boot対応** | Bootstrapのカスタムテーマ設定（`_variables.scss`） |
| **なぜ必要か** | アプリ全体の配色・間隔・フォント等のデザイントークンを一元管理 |

CSS変数でダークモード対応も定義されている。Java/Spring Bootプロジェクトでは `src/main/resources/static/css/variables.css` にあたるファイル。

---

### `plopfile.cjs`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | コードジェネレーターの設定。`yarn generate` で新しいコンポーネントのひな形を自動生成 |
| **Spring Boot対応** | **Maven Archetype** / **Spring Initializr** のプロジェクト内版 |
| **なぜ必要か** | チーム全員が同じファイル構造でコンポーネントを作成できる。手動でファイルを作ると構造がブレる |

`yarn generate` を実行すると:
1. コンポーネント名を聞かれる
2. featureに属するか共通かを聞かれる
3. `component.tsx`、`index.ts`（バレル）、`component.stories.tsx`（Storybook）を自動生成

---

### `mock-server.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | E2Eテスト用のスタンドアロンなモックAPIサーバー |
| **Spring Boot対応** | **WireMock のスタンドアロンモード** |
| **なぜ必要か** | PlaywrightのE2Eテストはブラウザ上で動くため、Service Worker（MSW browser）が使えない。独立したHTTPサーバーとしてモックAPIを提供 |

```typescript
// Express + MSW middleware で /api/* のモックを提供
const app = express();
app.use(cors({ origin: env.APP_URL }));
app.use(express.json());
app.use(createMiddleware(...handlers));
app.listen(env.APP_MOCK_API_PORT);
```

---

### `playwright.config.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | E2Eテスト（Playwright）の設定 |
| **Spring Boot対応** | **Selenium WebDriver の設定** |
| **なぜ必要か** | ブラウザを自動操作して、ユーザーの操作フロー全体をテスト |

---

### `index.html`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ブラウザが最初に読み込むHTMLファイル。`<div id="root">` がReactのマウントポイント |
| **Spring Boot対応** | Thymeleafの `layout.html`（最外側のHTMLテンプレート） |
| **なぜ必要か** | SPAではHTMLは1つだけ。この中の `<div>` にReactが全UIを動的に描画する |

Spring Bootでは各URLに対応するHTMLテンプレートがあるが、Reactでは**HTMLは1つだけ**。全画面遷移はJavaScript（React Router）が制御する。

---

### `vite-env.d.ts`（ルート）

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ViteのTypeScript型宣言。`import.meta.env` の型定義を有効化 |
| **Spring Boot対応** | 直接の対応なし。型システムの補助ファイル |

---

### `.gitignore`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Gitの除外設定 |
| **Spring Boot対応** | そのまま `.gitignore` |
| **除外対象** | `node_modules/`, `dist/`, `.env`, `coverage/`, `mocked-db.json` |

`node_modules` は Java の `.m2/repository`、`dist` は `target/` に相当。

---

## 3. src/ エントリーポイント

---

### `src/main.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Reactアプリケーションの起動処理。DOMにReactツリーをマウントする |
| **Spring Boot対応** | **`Application.java` の `public static void main(String[] args)`** |
| **なぜ必要か** | Viteはこのファイルをエントリーポイントとして認識し、ここからimportツリーを辿ってバンドルを構築する |

```typescript
import './index.css';                              // ① グローバルCSS読み込み
import { enableMocking } from '@/testing/mocks';   // ② MSWのモック有効化
import { App } from '@/app';                       // ③ Appコンポーネント

enableMocking().then(() => {                       // ④ モック初期化完了を待つ
  const root = document.getElementById('root')!;
  createRoot(root).render(                         // ⑤ DOMにマウント
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
});
```

Spring Bootでいうと:
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // ④ テスト用のモックサーバーを先に起動（開発時のみ）
        // ⑤ Spring Bootを起動
        SpringApplication.run(Application.class, args);
    }
}
```

---

### `src/index.css`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | グローバルCSS。Tailwindの読み込みとCSS変数（デザイントークン）の定義 |
| **Spring Boot対応** | `src/main/resources/static/css/global.css` |
| **なぜ必要か** | TailwindCSSのベーススタイル・ユーティリティを有効にし、アプリ全体の配色をCSS変数で定義 |

```css
@tailwind base;        /* Tailwindのリセット/ベーススタイル（normalize.cssに相当） */
@tailwind components;  /* コンポーネント用クラス */
@tailwind utilities;   /* ユーティリティクラス（mt-4, text-red-500等） */

:root {
  --background: 0 0% 100%;    /* 背景色 */
  --foreground: 222 47% 11%;  /* 文字色 */
  --primary: 222 47% 11%;     /* 主要色 */
  /* ... */
}

.dark {
  --background: 224 71% 4%;   /* ダークモードの背景色 */
  /* ... */
}
```

---

### `src/vite-env.d.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Viteのクライアント型定義を有効化する宣言ファイル |
| **Spring Boot対応** | 直接の対応なし |

---

## 4. src/app/ — アプリケーションシェル

Spring Bootで言えば `@Configuration` クラス群と `@Controller` のルーティング定義が集まる場所。

---

### `src/app/index.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `<AppProvider>` と `<AppRouter>` を組み合わせてアプリのルートコンポーネントを構成 |
| **Spring Boot対応** | `@SpringBootApplication` クラス（ConfigurationとComponentScanを統合） |

```typescript
export const App = () => (
  <AppProvider>      {/* ← @Configurationクラス群（DI・認証・キャッシュ等） */}
    <AppRouter />    {/* ← @Controller + @RequestMapping のルート定義 */}
  </AppProvider>
);
```

**なぜ分離されているか**: Provider（設定）とRouter（画面定義）は関心が異なる。Spring Bootで `SecurityConfig.java` と `WebMvcConfig.java` を分けるのと同じ。

---

### `src/app/provider.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 全グローバルProvider（React Query、ErrorBoundary、認証、通知、SEO）をネストして定義 |
| **Spring Boot対応** | **複数の `@Configuration` クラスの集合体**。SecurityConfig + CacheConfig + ErrorHandlerConfig をひとまとめにしたもの |
| **なぜ必要か** | Reactでは「コンテキスト」を Provider コンポーネントで提供する。この Provider の**ネストの順番が重要** |

```
Suspense                    ← try-catch的なフォールバック
  └─ ErrorBoundary          ← @ControllerAdvice（グローバル例外ハンドラー）
    └─ HelmetProvider       ← HTMLヘッダー管理
      └─ QueryClientProvider ← @EnableCaching（キャッシュ設定）
        └─ Notifications    ← 通知表示
          └─ AuthLoader     ← SecurityFilterChain（認証フィルター）
            └─ {children}   ← 以降のルーティング/画面
```

Spring Boot では `SecurityFilterChain` が自動的にフィルターチェーンを構築するが、Reactでは**手動でネスト順序を定義する**。順序を間違えると、例えば「認証チェック中にReact Queryが使えない」といった問題が起きる。

---

### `src/app/router.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 全URLと対応するコンポーネント（画面）のマッピング定義 |
| **Spring Boot対応** | 全 `@Controller` の `@RequestMapping` を一箇所にまとめた**ルーティングテーブル** |
| **なぜ必要か** | SPAでは画面遷移をサーバーではなくJavaScript側で制御する。どのURLでどのコンポーネントを表示するかを定義 |

```typescript
createBrowserRouter([
  { path: '/',                  lazy: () => import('./routes/landing') },
  { path: '/auth/login',       lazy: () => import('./routes/auth/login') },
  { path: '/auth/register',    lazy: () => import('./routes/auth/register') },
  {
    path: '/app',               // ← 認証必要なエリア
    element: <ProtectedRoute>,  // ← Spring Securityの .authenticated() に相当
    children: [
      { path: '',              lazy: () => import('./routes/app/dashboard') },
      { path: 'discussions',   lazy: () => import('./routes/app/discussions/discussions'),
        loader: clientLoader },  // ← データ事前取得（後述）
      { path: 'discussions/:discussionId',
        lazy: () => import('./routes/app/discussions/discussion'),
        loader: clientLoader },
      { path: 'users',        lazy: () => import('./routes/app/users') },
      { path: 'profile',      lazy: () => import('./routes/app/profile') },
    ],
  },
]);
```

Spring Boot で書くとこうなる:

```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/auth/**").permitAll()   // 認証不要
    .requestMatchers("/app/**").authenticated()     // 認証必要
);

// 各Controller
@GetMapping("/app/discussions")       → DiscussionsController
@GetMapping("/app/discussions/{id}")  → DiscussionController
@GetMapping("/app/users")             → UsersController
```

**`lazy` とは**: コード分割。`/app/discussions` に遷移して初めてそのJSファイルをダウンロードする。Spring Bootにはない概念だが、「必要になるまでBeanを生成しない」`@Lazy` に近い発想。

**`loader`（clientLoader）とは**: 画面が表示される**前に**データを取得する仕組み。Spring MVCの `@ModelAttribute` でビューに渡すデータを事前に準備するのに似ている。

---

## 5. src/app/routes/ — 画面定義

Spring Bootの `@Controller` に相当。各ファイルが1つのURLに対応する。

---

### `routes/landing.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/` のランディングページ。ログイン済みなら `/app` へ、未ログインなら `/auth/login` へ案内 |
| **Spring Boot対応** | `@GetMapping("/")` の `HomeController` |

---

### `routes/auth/login.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/auth/login` のログイン画面。LoginFormコンポーネントを描画し、ログイン成功後にリダイレクト |
| **Spring Boot対応** | `@GetMapping("/auth/login")` + Spring Securityのログインページ設定 |

ログイン成功時に `?redirectTo=` パラメータを使って元のページに戻る。Spring Securityの `defaultSuccessUrl` / `SavedRequestAwareAuthenticationSuccessHandler` と同じ挙動。

---

### `routes/auth/register.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/auth/register` のユーザー登録画面。チーム選択（既存チームに参加 or 新規作成）を含む |
| **Spring Boot対応** | `@GetMapping("/auth/register")` + `@PostMapping("/auth/register")` |

---

### `routes/app/root.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app` 配下の共通レイアウト。DashboardLayout（サイドバー+ヘッダー）をラップし、子ルートを `<Outlet>` で描画 |
| **Spring Boot対応** | Thymeleafの **レイアウトテンプレート**（`layout.html`）。ヘッダー・サイドバーは固定で、コンテンツ部分だけ差し替わる |

```typescript
// React
<DashboardLayout>
  <Outlet />    ← 子ルートの内容がここに入る
</DashboardLayout>

// Thymeleaf
<div layout:fragment="content">
  <!-- 各画面の内容がここに入る -->
</div>
```

---

### `routes/app/dashboard.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app`（ダッシュボード）。ログインユーザーの名前・ロール・権限に応じた案内を表示 |
| **Spring Boot対応** | `@GetMapping("/app")` の `DashboardController` |

---

### `routes/app/discussions/discussions.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app/discussions` の議論一覧画面。clientLoaderでデータ事前取得 + DiscussionsListコンポーネント描画 |
| **Spring Boot対応** | `@GetMapping("/app/discussions")` の `DiscussionController#list()` |

```typescript
// React: clientLoader（画面表示前にデータ取得）
export const clientLoader = (queryClient) => async ({ request }) => {
  const page = new URL(request.url).searchParams.get('page') || 1;
  const query = getDiscussionsQueryOptions({ page });
  return queryClient.getQueryData(query.queryKey) ??
         (await queryClient.fetchQuery(query));
};

// Spring Boot: @ModelAttributeでビューにデータを渡す
@GetMapping("/app/discussions")
public String list(@RequestParam(defaultValue = "1") int page, Model model) {
    Page<Discussion> discussions = discussionService.findAll(page);
    model.addAttribute("discussions", discussions);
    return "discussions/list";
}
```

---

### `routes/app/discussions/discussion.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app/discussions/:discussionId` の議論詳細画面。議論データとコメントを**並列で**事前取得 |
| **Spring Boot対応** | `@GetMapping("/app/discussions/{id}")` の `DiscussionController#detail()` |

```typescript
// React: 2つのデータを並列取得
const [discussion, comments] = await Promise.all([
  queryClient.fetchQuery(getDiscussionQueryOptions(id)),
  queryClient.fetchInfiniteQuery(getInfiniteCommentsQueryOptions(id)),
]);

// Spring Boot: CompletableFutureで並列取得
CompletableFuture<Discussion> discussionFuture = discussionService.findByIdAsync(id);
CompletableFuture<Page<Comment>> commentsFuture = commentService.findByDiscussionIdAsync(id);
CompletableFuture.allOf(discussionFuture, commentsFuture).join();
```

コメント部分は `<ErrorBoundary>` で囲まれている。コメントの取得が失敗しても議論本体は表示される。Spring Bootで `try-catch` でコメント取得を囲み、失敗時はエラーメッセージだけ表示するのと同じ考え。

---

### `routes/app/users.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app/users` のユーザー一覧画面。ADMINロール限定 |
| **Spring Boot対応** | `@PreAuthorize("hasRole('ADMIN')")` + `@GetMapping("/app/users")` |

---

### `routes/app/profile.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `/app/profile` のプロフィール画面。自分の情報表示 + 編集機能 |
| **Spring Boot対応** | `@GetMapping("/app/profile")` でログインユーザーの情報を表示 |

---

### `routes/not-found.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 404ページ |
| **Spring Boot対応** | `@ControllerAdvice` の `@ExceptionHandler(NoHandlerFoundException.class)` |

---

## 6. src/lib/ — 共通ライブラリ層

Spring Bootでいう `infrastructure` パッケージ。技術基盤の設定と共通機能を集約。

---

### `lib/api-client.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Axiosインスタンスの生成と、リクエスト/レスポンスインターセプターの設定 |
| **Spring Boot対応** | **`RestTemplate` の `@Bean` 定義** + **`ClientHttpRequestInterceptor`** |
| **なぜ必要か** | 全API呼び出しで共通の処理（認証ヘッダー付与、エラー通知、401リダイレクト）を一箇所で定義 |

```typescript
// React: Axiosインターセプター
api.interceptors.request.use((config) => {
  config.withCredentials = true;      // Cookie自動送信
  config.headers.Accept = 'application/json';
  return config;
});
api.interceptors.response.use(
  (response) => response.data,        // .data の自動抽出
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/auth/login';  // 強制ログアウト
    }
    return Promise.reject(error);
  }
);
```

```java
// Spring Boot: RestTemplateのインターセプター
@Bean
public RestTemplate restTemplate() {
    RestTemplate rt = new RestTemplate();
    rt.getInterceptors().add((request, body, execution) -> {
        request.getHeaders().set("Accept", "application/json");
        return execution.execute(request, body);
    });
    rt.setErrorHandler(new ResponseErrorHandler() {
        // 401なら認証エラー処理
    });
    return rt;
}
```

---

### `lib/auth.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 認証の全機能。ログイン/ログアウト/登録/ユーザー取得のAPI呼び出しと、認証状態の管理 |
| **Spring Boot対応** | **`SecurityConfig.java`** + **`UserDetailsService`** + **`AuthenticationProvider`** |
| **なぜ必要か** | 認証関連の処理を一箇所に集約。`useUser()` でどのコンポーネントからでもログインユーザー情報にアクセスできる |

**主要エクスポート**:

| エクスポート | 役割 | Spring Boot対応 |
|---|---|---|
| `useUser()` | 現在のログインユーザー取得 | `SecurityContextHolder.getContext().getAuthentication()` |
| `useLogin()` | ログイン処理 | `AuthenticationManager.authenticate()` |
| `useLogout()` | ログアウト処理 | `SecurityContextLogoutHandler` |
| `useRegister()` | ユーザー登録処理 | `UserService.register()` |
| `AuthLoader` | アプリ起動時に `GET /auth/me` で認証状態を確認 | Spring Securityの `SecurityFilterChain`（リクエストごとにセッション検証） |
| `ProtectedRoute` | 未認証ユーザーをログイン画面にリダイレクト | `.authenticated()` の設定 |
| `loginInputSchema` | ログイン入力のZodバリデーション | `LoginRequest` DTOの `@Valid` |
| `registerInputSchema` | 登録入力のZodバリデーション | `RegisterRequest` DTOの `@Valid` |

---

### `lib/authorization.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ロールベース認可（RBAC）とポリシーベース認可 |
| **Spring Boot対応** | **`@PreAuthorize`** / **`@Secured`** / **カスタムPermissionEvaluator** |
| **なぜ必要か** | 「この画面はADMINだけ表示」「このコメントは本人かADMINだけ削除可能」というルールを宣言的に書く |

```typescript
// React: ロールベース（ADMINだけ表示）
<Authorization allowedRoles={[ROLES.ADMIN]}>
  <CreateDiscussion />
</Authorization>

// Spring Boot: アノテーションで同じことをする
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/discussions")
public Discussion create(@RequestBody CreateDiscussionRequest request) { ... }
```

```typescript
// React: ポリシーベース（本人またはADMIN）
POLICIES['comment:delete'] = (user, comment) => {
  return user.role === 'ADMIN' || comment.author.id === user.id;
};

// Spring Boot: PermissionEvaluator
@PreAuthorize("hasRole('ADMIN') or #comment.author.id == authentication.principal.id")
@DeleteMapping("/comments/{id}")
public void delete(@PathVariable String id) { ... }
```

---

### `lib/react-query.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | React Query（サーバー状態管理ライブラリ）のグローバル設定と型ヘルパー |
| **Spring Boot対応** | **Spring Cacheの設定** (`@EnableCaching` + `CacheManager` の `@Bean`) |
| **なぜ必要か** | 全APIデータのキャッシュ戦略（TTL、リトライ、ウィンドウフォーカス時の再取得）を一括で設定 |

```typescript
// React: React Query設定
export const queryConfig = {
  queries: {
    staleTime: 1000 * 60,           // 1分間はキャッシュを新鮮とみなす
    retry: false,                    // リトライしない
    refetchOnWindowFocus: false,     // タブ切り替えで再取得しない
  },
};

// Spring Boot: Caffeine Cacheの設定
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cm = new CaffeineCacheManager();
    cm.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES));  // 1分間キャッシュ
    return cm;
}
```

---

## 7. src/config/ — 設定

---

### `config/env.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 環境変数を Zod スキーマでバリデーションし、型安全なオブジェクトとして提供 |
| **Spring Boot対応** | **`application.yml`** + **`@ConfigurationProperties`** + **`@Validated`** |
| **なぜ必要か** | 環境変数の不足や型違いを**アプリ起動時に即座に検出**する。本番デプロイで設定漏れがあっても即エラー |

```typescript
// React: Zodで環境変数を検証
const EnvSchema = z.object({
  API_URL: z.string(),                    // 必須
  ENABLE_API_MOCKING: z.string().optional(), // 任意
  APP_URL: z.string().default('http://localhost:3000'),
});
export const env = EnvSchema.parse(envVars);

// Spring Boot: @ConfigurationPropertiesで同じことをする
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {
    @NotBlank
    private String apiUrl;
    private boolean enableApiMocking = false;
}
```

---

### `config/paths.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 全ルートパスを定数として一元管理。`path`（定義用）と `getHref`（リンク生成用）を持つ |
| **Spring Boot対応** | ルーティング定数クラス（URLをハードコードせず定数で管理する慣習） |
| **なぜ必要か** | URLをハードコードすると、パス変更時に全箇所を修正する必要がある。定数化で一箇所の変更で済む |

```typescript
// React
paths.app.discussion.getHref('abc-123')  // → '/app/discussions/abc-123'

// Java
public static String discussionPath(String id) {
    return "/app/discussions/" + id;
}
```

---

## 8. src/components/ — 共通UIコンポーネント

Feature横断で使われるUIパーツ。Spring Bootでは Thymeleaf のフラグメント（`fragments/header.html`等）に相当。

---

### `components/layouts/dashboard-layout.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | サイドバー + ヘッダー + メインコンテンツエリアの3カラムレイアウト |
| **Spring Boot対応** | Thymeleaf の **`layout.html`**（共通レイアウト） |

ナビゲーションリンクは `useAuthorization()` でロール判定。ADMINでないユーザーには「Users」リンクが表示されない。

---

### `components/layouts/content-layout.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 各画面の共通ラッパー。タイトル表示 + SEOメタタグ設定 |
| **Spring Boot対応** | Thymeleaf の `content` フラグメント |

---

### `components/layouts/auth-layout.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ログイン/登録画面専用のレイアウト。ロゴ + 中央寄せフォーム |
| **Spring Boot対応** | ログイン画面専用のThymeleafテンプレート |

---

### `components/errors/main.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | グローバルエラー表示。「予期しないエラーが発生しました」+ リフレッシュボタン |
| **Spring Boot対応** | `@ControllerAdvice` のデフォルトエラーハンドラー / `error.html` |

---

### `components/seo/head.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | `<title>` と `<meta>` タグをReactコンポーネントから宣言的に設定 |
| **Spring Boot対応** | Thymeleaf の `<title th:text="${title}">` |

---

### `components/ui/` — UIプリミティブ

これらは全て、Radix UI（アクセシビリティ対応のヘッドレスUIライブラリ）をベースに、Tailwind CSSでスタイリングしたもの。Spring Bootで言えば「Bootstrapコンポーネント」に相当する。

| ファイル | 役割 | Spring Boot対応 |
|---|---|---|
| `button/button.tsx` | ボタン（6つのバリアント + 4つのサイズ + ローディング状態） | Bootstrap `<button class="btn btn-primary">` |
| `form/form.tsx` | react-hook-form + Zodの統合ラッパー | Spring MVC の `<form:form>` タグ |
| `form/input.tsx` | テキスト入力 + エラー表示 | `<input type="text">` + `<span class="error">` |
| `form/textarea.tsx` | テキストエリア | `<textarea>` |
| `form/select.tsx` | セレクトボックス | `<select>` |
| `form/switch.tsx` | トグルスイッチ | `<input type="checkbox">` のスタイリング |
| `form/label.tsx` | フォームラベル | `<label>` |
| `form/error.tsx` | バリデーションエラーメッセージ | `<span th:if="${#fields.hasErrors('email')}">` |
| `form/field-wrapper.tsx` | ラベル+入力+エラーの3点セット | Bootstrapの `form-group` |
| `form/form-drawer.tsx` | スライドアウトパネル内のフォーム | モーダルダイアログ内のフォーム |
| `dialog/dialog.tsx` | ダイアログ/モーダル + 確認ダイアログ | Bootstrap Modal / `window.confirm()` |
| `drawer/drawer.tsx` | スライドアウトパネル | Bootstrap Offcanvas |
| `dropdown/dropdown.tsx` | ドロップダウンメニュー | Bootstrap Dropdown |
| `link/link.tsx` | React Router対応リンク | `<a th:href="@{/path}">` |
| `md-preview/md-preview.tsx` | Markdown→HTML変換 + XSSサニタイズ | Markdownレンダリング + OWASP HTML Sanitizer |
| `notifications/notification.tsx` | 個別の通知トースト | Spring の `FlashAttribute` + Bootstrap Toast |
| `notifications/notifications.tsx` | 通知一覧の描画 | Flash Messageの表示領域 |
| `notifications/notifications-store.ts` | 通知のZustandストア（状態管理） | `HttpSession` に格納するフラッシュメッセージ |
| `spinner/spinner.tsx` | ローディングスピナー | ローディングインジケーター |
| `table/table.tsx` | データテーブル + カラム定義 | Thymeleaf の `<table th:each>` |
| `table/pagination.tsx` | ページネーションUI | Spring Data の `Page` + Thymeleafのページネーション |

---

## 9. src/features/ — Feature モジュール

**ここがBulletproof Reactの核心**。各Featureは Spring Boot でいう**1つの Bounded Context（パッケージ）**に相当し、`api/` + `components/` を持つ。

```
Spring Boot                              React (Feature)
───────────────────────────────          ───────────────────────────
com.example.discussions/                 features/discussions/
├── controller/                          ├── (routes で定義)
│   └── DiscussionController.java
├── service/                             ├── api/
│   └── DiscussionService.java           │   ├── get-discussions.ts
│                                        │   ├── create-discussion.ts
│                                        │   └── ...
├── repository/                          │   （APIクライアント経由でバックエンドに委譲）
│   └── DiscussionRepository.java
├── dto/                                 │   （types/api.ts に集約）
│   └── DiscussionDto.java
└── (Thymeleafテンプレート)               └── components/
                                             ├── discussions-list.tsx
                                             ├── create-discussion.tsx
                                             └── ...
```

---

### `features/discussions/api/get-discussions.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論一覧の取得。API関数 + React Queryオプション + カスタムフックの3層構造 |
| **Spring Boot対応** | `DiscussionService#findAll()` + `@Cacheable` |

3層構造の意味:

```typescript
// 層1: 生のAPI呼び出し（Service のメソッドに相当）
export const getDiscussions = (page = 1) =>
  api.get('/discussions', { params: { page } });

// 層2: React Queryの設定（@Cacheable のキー設定に相当）
export const getDiscussionsQueryOptions = ({ page }) =>
  queryOptions({
    queryKey: ['discussions', { page }],   // ← キャッシュキー
    queryFn: () => getDiscussions(page),
  });

// 層3: Reactコンポーネント用フック（Controllerから呼ぶServiceメソッドに相当）
export const useDiscussions = ({ page }) =>
  useQuery(getDiscussionsQueryOptions({ page }));
```

```java
// Spring Boot で同じ3層を書くと:

// 層1: Repository
@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, String> {
    Page<Discussion> findAllByTeamId(String teamId, Pageable pageable);
}

// 層2: Service（キャッシュ設定付き）
@Service
public class DiscussionService {
    @Cacheable(value = "discussions", key = "#page")
    public Page<Discussion> findAll(int page) {
        return repository.findAll(PageRequest.of(page, 10));
    }
}

// 層3: Controller（ビューにデータを渡す）
@GetMapping("/discussions")
public String list(@RequestParam int page, Model model) {
    model.addAttribute("discussions", service.findAll(page));
    return "discussions/list";
}
```

---

### `features/discussions/api/create-discussion.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論の作成。Zodスキーマ（入力バリデーション） + API呼び出し + キャッシュ無効化 |
| **Spring Boot対応** | `DiscussionService#create()` + `@CacheEvict` + `@Valid CreateDiscussionRequest` |

```typescript
// React: Zodスキーマ（バリデーションルール）
export const createDiscussionInputSchema = z.object({
  title: z.string().min(1, 'Required'),
  body: z.string().min(1, 'Required'),
});

// React: ミューテーション + キャッシュ無効化
export const useCreateDiscussion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => api.post('/discussions', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['discussions'] });  // ← @CacheEvict
    },
  });
};
```

```java
// Spring Boot: DTOバリデーション
public class CreateDiscussionRequest {
    @NotBlank private String title;
    @NotBlank private String body;
}

// Spring Boot: Service + キャッシュ無効化
@CacheEvict(value = "discussions", allEntries = true)
public Discussion create(CreateDiscussionRequest request) {
    return repository.save(new Discussion(request.getTitle(), request.getBody()));
}
```

---

### `features/discussions/api/update-discussion.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論の更新。更新後に**個別キャッシュを再取得** |
| **Spring Boot対応** | `DiscussionService#update()` + `@CachePut` |

`invalidate`（一覧のキャッシュ無効化）ではなく `refetch`（個別データの再取得）を使うのがポイント。一覧を丸ごと捨てるのではなく、更新した1件だけを最新化する。

---

### `features/discussions/api/delete-discussion.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論の削除。削除後に一覧キャッシュを無効化 |
| **Spring Boot対応** | `DiscussionService#delete()` + `@CacheEvict` |

---

### `features/discussions/api/get-discussion.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 個別議論の取得 |
| **Spring Boot対応** | `DiscussionService#findById()` |

---

### `features/discussions/components/discussions-list.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論一覧のテーブル表示。ホバー時にデータをprefetch |
| **Spring Boot対応** | `discussions/list.html`（Thymeleafテンプレート） |

**prefetchの概念**: ユーザーが「View」リンクにマウスを乗せた瞬間にAPIを叩き、キャッシュに入れておく。クリックした時には**データが既にある**ので即座に画面が表示される。Spring Bootにはこの概念はない（サーバーサイドレンダリングでは不要なため）。

---

### `features/discussions/components/create-discussion.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論作成フォーム。ADMIN権限チェック + FormDrawer + バリデーション |
| **Spring Boot対応** | `discussions/create.html` + `@PreAuthorize("hasRole('ADMIN')")` |

---

### `features/discussions/components/update-discussion.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 議論編集フォーム。既存データを初期値として読み込み |
| **Spring Boot対応** | `discussions/edit.html` の `th:value="${discussion.title}"` |

---

### `features/discussions/components/delete-discussion.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 削除確認ダイアログ + 削除実行 |
| **Spring Boot対応** | JavaScript の `confirm()` + `@DeleteMapping` |

---

### `features/comments/api/get-comments.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | コメント一覧の無限スクロール取得（`useInfiniteQuery`） |
| **Spring Boot対応** | `CommentService#findByDiscussionId()` + `Slice`（次ページの有無判定） |

通常の `useQuery`（1ページ分のデータ取得）ではなく `useInfiniteQuery`（ページを追加読み込み）を使う。Spring Data の `Slice` がページ終端を `hasNext()` で判定するのと同じロジックを `getNextPageParam` で実装。

---

### `features/comments/api/create-comment.ts` / `delete-comment.ts`

| 項目 | 内容 |
|---|---|
| **Spring Boot対応** | `CommentService#create()` / `CommentService#delete()` |

---

### `features/comments/components/comments.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | コメント機能の統合コンポーネント。CommentsList + CreateComment を組み合わせる |
| **Spring Boot対応** | コメントセクション全体のThymeleafフラグメント |

---

### `features/comments/components/comments-list.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | コメント一覧 + 「Load More」ボタン + ポリシーベースの削除ボタン表示 |
| **Spring Boot対応** | `comments/list.html` + `sec:authorize` での権限表示制御 |

---

### `features/auth/components/login-form.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ログインフォーム（Email + Password + Submit）。Zodバリデーション + useLogin() |
| **Spring Boot対応** | `login.html` + Spring Security のログインフォーム |

---

### `features/auth/components/register-form.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 登録フォーム。既存チームに参加 or 新規チーム作成の切り替えをトグルスイッチで実装 |
| **Spring Boot対応** | `register.html` |

---

### `features/users/api/` + `features/users/components/`

| ファイル | 役割 | Spring Boot対応 |
|---|---|---|
| `get-users.ts` | チーム内ユーザー一覧取得 | `UserService#findByTeamId()` |
| `update-profile.ts` | プロフィール更新 | `UserService#updateProfile()` |
| `delete-user.ts` | ユーザー削除（Admin用） | `UserService#delete()` + `@PreAuthorize` |
| `users-list.tsx` | ユーザー一覧テーブル | `users/list.html` |
| `update-profile.tsx` | プロフィール編集フォーム | `users/edit.html` |
| `delete-user.tsx` | 削除確認ダイアログ | `confirm()` + DELETE |

---

### `features/teams/api/get-teams.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | チーム一覧取得（登録画面のドロップダウン用） |
| **Spring Boot対応** | `TeamService#findAll()` |

---

## 10. src/hooks/ — 共通カスタムフック

---

### `hooks/use-disclosure.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 開閉状態（isOpen / open / close / toggle）を管理するフック |
| **Spring Boot対応** | 直接の対応なし（UIの状態管理はサーバーサイドにはない概念）。あえて言えば、フォームのモーダル表示/非表示を管理する**セッション変数** |
| **なぜ必要か** | ドロワー、ダイアログ、ドロップダウンなど「開く/閉じる」UIが多数あるため、状態管理を共通化 |

---

## 11. src/types/ — 型定義

---

### `types/api.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | バックエンドAPIのレスポンス型を定義。User, Discussion, Comment, Team等のエンティティ型 |
| **Spring Boot対応** | **DTO クラス群** (`UserDto.java`, `DiscussionDto.java` 等) |
| **なぜ必要か** | APIレスポンスの型をフロントエンドで定義することで、型安全にデータを扱える |

```typescript
// React: TypeScript型定義
export type Discussion = Entity<{
  title: string;
  body: string;
  teamId: string;
  author: User;
}>;

// Spring Boot: Java DTO
public class DiscussionDto {
    private String id;
    private String title;
    private String body;
    private String teamId;
    private UserDto author;
    private Instant createdAt;
}
```

ファイル内のコメントに `// ideally, we want to keep these api related types in sync with the backend` とあるように、**理想的にはバックエンドのOpenAPI仕様から自動生成する**のが本来の姿。

---

## 12. src/utils/ — ユーティリティ関数

---

### `utils/cn.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | CSSクラス名の結合ユーティリティ。`clsx` でクラス名を条件付き結合し、`twMerge` でTailwindの競合を解消 |
| **Spring Boot対応** | Javaには直接の対応なし。CSSの管理はフロントエンド固有 |

```typescript
cn('px-4 py-2', isActive && 'bg-blue-500', 'bg-red-500')
// → 'px-4 py-2 bg-red-500'（twMergeがbg-の競合を解消し、後のbg-red-500が勝つ）
```

---

### `utils/format.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | 日付フォーマット関数。UNIXタイムスタンプを `April 9, 2026 9:31 PM` 形式に変換 |
| **Spring Boot対応** | `DateTimeFormatter` のユーティリティメソッド |

---

## 13. src/testing/ — テスト基盤

---

### `testing/setup-tests.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Vitestの全テスト実行前の共通セットアップ。MSWサーバー起動、DB初期化、グローバルモック |
| **Spring Boot対応** | **`@SpringBootTest` の `@BeforeAll`** / **テスト用 `application-test.yml`** |

```typescript
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));  // MSWサーバー起動
afterAll(() => server.close());                                    // サーバー停止
beforeEach(() => { initializeDb(); });                             // テストごとにDB初期化
afterEach(() => { server.resetHandlers(); resetDb(); });           // ハンドラーとDB をリセット
```

Spring Boot で `@Sql(scripts = "cleanup.sql")` を `@BeforeEach` で実行するのと同じ。テストの独立性を保証。

---

### `testing/test-utils.tsx`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | テスト用のレンダリングヘルパー。Provider付きでコンポーネントをレンダリング + 認証済みユーザーの自動セットアップ |
| **Spring Boot対応** | **`@SpringBootTest` のベーステストクラス** + **`@WithMockUser`** |

```typescript
// React: テスト用レンダリング
await renderApp(<DiscussionsList />, { user: adminUser, url: '/app/discussions' });

// Spring Boot: MockMvcでテスト
@WithMockUser(roles = "ADMIN")
@Test
void shouldListDiscussions() throws Exception {
    mockMvc.perform(get("/app/discussions"))
           .andExpect(status().isOk());
}
```

---

### `testing/data-generators.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | テスト用のランダムデータ生成（@ngneat/falso ライブラリ使用） |
| **Spring Boot対応** | **Faker** / **EasyRandom** / **テスト用ファクトリメソッド** |

---

### `testing/mocks/db.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | @mswjs/data によるインメモリデータベース。4モデル（user, team, discussion, comment） |
| **Spring Boot対応** | **H2 Database**（テスト用インメモリDB）+ **`@DataJpaTest`** |

---

### `testing/mocks/handlers/*.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | MSWのリクエストハンドラー。各APIエンドポイントのモックレスポンスを定義 |
| **Spring Boot対応** | **WireMock のスタブ定義** / **`@MockBean` での振る舞い定義** |

---

### `testing/mocks/browser.ts` / `server.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | browser.ts = ブラウザ上でのAPIインターセプト（開発用）、server.ts = Node.js上でのAPIインターセプト（テスト用） |
| **Spring Boot対応** | browser.ts = ブラウザのモック（対応なし）、server.ts = `@MockBean` + `WireMockServer` |

---

### `testing/mocks/utils.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | モック用のユーティリティ。JWT生成、パスワードハッシュ、認証検証、ネットワーク遅延シミュレーション |
| **Spring Boot対応** | テスト用の `JwtTokenProvider` / `PasswordEncoder` のモック実装 |

---

### `__mocks__/zustand.ts`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ZustandストアのVitest用モック。テスト間で状態をリセット |
| **Spring Boot対応** | `@DirtiesContext` でApplicationContextをリセットするのに近い |

---

## 14. その他のディレクトリ

---

### `.storybook/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Storybook（UIコンポーネントカタログ）の設定 |
| **Spring Boot対応** | **Swagger UI** に近い概念。APIではなくUIコンポーネントを一覧で確認・テストできる環境 |
| **ファイル** | `main.ts`（Stories検索パターン + アドオン設定）、`preview.tsx`（デコレーター + グローバルCSS） |

---

### `generators/component/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | Plopコードジェネレーターのテンプレート |
| **Spring Boot対応** | Maven Archetype のテンプレートファイル |
| **ファイル** | `index.cjs`（質問定義）、`*.hbs`（Handlebarsテンプレート） |

---

### `e2e/tests/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | PlaywrightによるE2Eテスト |
| **Spring Boot対応** | **Selenium / Cypress のE2Eテスト** |
| **ファイル** | `auth.setup.ts`（認証セットアップ）、`smoke.spec.ts`（全機能スモークテスト）、`profile.spec.ts` |

---

### `.vscode/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | VS Codeのプロジェクト固有設定 |
| **Spring Boot対応** | `.idea/`（IntelliJ IDEA設定） |
| **ファイル** | `settings.json`（保存時フォーマット、ESLint自動修正）、`extensions.json`（推奨拡張機能） |

---

### `public/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ビルド時にそのままコピーされる静的ファイル（favicon、MSWのService Worker） |
| **Spring Boot対応** | `src/main/resources/static/` |

---

### `src/assets/`

| 項目 | 内容 |
|---|---|
| **具体的な役割** | ビルド時にバンドルに含まれる静的アセット（ロゴSVG等）。`public/` と異なりimportして使う |
| **Spring Boot対応** | `src/main/resources/static/images/` |

`public/` はURLで直接アクセスできる（`/favicon.ico`）。`assets/` は `import logo from '@/assets/logo.svg'` のようにコードからimportする。Spring Bootでは両方とも `static/` に置くが、Viteではビルド最適化のために分けている。
