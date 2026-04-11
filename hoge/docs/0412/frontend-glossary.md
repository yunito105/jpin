# フロントエンド用語集 — バックエンドエンジニアのための完全ガイド

> **対象読者**: Java / Spring Boot の経験はあるが、フロントエンド（React）は初めての方
> **方針**: 「聞いたことはあるが正確に説明できない」レベルの用語を、バックエンドの知識で例えて解説する

---

## 目次

1. [Web の超基本](#1-web-の超基本)
2. [JavaScript / TypeScript の基本](#2-javascript--typescript-の基本)
3. [Node.js / パッケージ管理](#3-nodejs--パッケージ管理)
4. [React の基本概念](#4-react-の基本概念)
5. [React のデータ管理](#5-react-のデータ管理)
6. [ルーティング / ナビゲーション](#6-ルーティング--ナビゲーション)
7. [HTTP通信 / API連携](#7-http通信--api連携)
8. [スタイリング / CSS](#8-スタイリング--css)
9. [ビルド / 開発ツール](#9-ビルド--開発ツール)
10. [テスト](#10-テスト)
11. [認証 / セキュリティ](#11-認証--セキュリティ)
12. [パフォーマンス / 最適化](#12-パフォーマンス--最適化)
13. [設計パターン / アーキテクチャ](#13-設計パターン--アーキテクチャ)

---

## 凡例

| カラム | 説明 |
|---|---|
| **用語** | 正式名称（略称がある場合は併記） |
| **読み方** | カタカナ読み。チーム内で発音が揃うように |
| **一言で** | 10文字以内で何者か |
| **バックエンドで例えると** | Java / Spring Boot / DB の知識で例えた説明 |
| **もう少し詳しく** | 実際に何が起きているのか、具体的に |

---

## 1. Web の超基本

### ブラウザとHTMLの仕組み

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **HTML** | エイチティーエムエル | 画面の骨格 | DBのテーブル定義（構造だけで見た目はない） | `<div>`, `<h1>`, `<input>` などのタグでページの構造を記述する。ブラウザはこれを読んで画面を構築する |
| **CSS** | シーエスエス | 画面の見た目 | DBのビュー（データの見せ方を変える） | 色、サイズ、配置、アニメーションなど「見た目」のルールを定義する。HTMLが骨格ならCSSは皮膚と服 |
| **JavaScript (JS)** | ジャバスクリプト | 画面の動き | Javaのコード（ビジネスロジック） | ボタンを押したら何が起きるか、データをどう加工するかなど「振る舞い」を記述する。※Javaとは無関係の言語 |
| **DOM** | ドム | HTMLの実体 | メモリ上のオブジェクトツリー（Javaで言うとXMLのDocument Object Model） | ブラウザがHTMLを読み込むと、メモリ上に**ツリー構造のオブジェクト**を作る。これがDOM。JavaScriptはこのDOMを操作して画面を変更する |
| **仮想DOM (Virtual DOM)** | バーチャルドム | DOMの下書き | JPA のダーティチェック（変更差分だけDBに反映する仕組み） | Reactが使う高速化技術。画面の変更を**まず軽量なJavaScriptオブジェクト（仮想DOM）で計算**し、実際のDOMには**差分だけ**を反映する。全体を書き換えるより圧倒的に速い |
| **レンダリング** | レンダリング | 画面に描画する | SQLの結果をHTMLに変換してレスポンスを返すこと（Thymeleafのテンプレート処理） | データ → HTMLに変換 → ブラウザに表示、の一連の流れ。Reactでは「コンポーネントの関数を実行してUIを生成する」こと |
| **イベント** | イベント | ユーザーの操作 | HTTPリクエスト（ユーザーが何かした→サーバーに通知される） | クリック、入力、スクロール、キーボード操作など。`onClick`, `onChange` で「何かが起きたら何をする」を定義する |

---

### SPA と従来のWebアプリの違い

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **SPA** | エスピーエー | 画面遷移しないWebアプリ | 全URLが1つのControllerに集約されたイメージ | Single Page Application。HTMLは1つだけ。画面遷移はJavaScriptが画面の中身を書き換えて実現する。ページリロードが発生しないので速い |
| **MPA** | エムピーエー | 従来型Webアプリ | 普通のSpring MVC + Thymeleaf | Multi Page Application。URLごとにサーバーがHTMLを生成して返す従来の方式。Spring Bootで作るWebアプリは基本これ |
| **CSR** | シーエスアール | ブラウザ側で描画 | ブラウザがJSPを解釈するようなもの（実際にはない） | Client Side Rendering。サーバーは空のHTMLとJSファイルを返し、**ブラウザがJavaScriptを実行して画面を組み立てる**。React/Vueの標準方式 |
| **SSR** | エスエスアール | サーバー側で描画 | 普通のThymeleaf（サーバーでHTMLを生成して返す） | Server Side Rendering。サーバーでReactを動かし、完成したHTMLをブラウザに返す。初回表示が速い、SEOに有利 |
| **SSG** | エスエスジー | 事前に描画 | `mvn package` 時にHTMLを生成しておく | Static Site Generation。ビルド時にHTMLを生成する。ブログやドキュメントサイト向き。表示が最速 |
| **ハイドレーション** | ハイドレーション | 静的HTMLに魂を入れる | サーバーがHTMLを返した後、JSがイベントリスナーを付与する処理 | SSRで生成したHTMLは見た目だけ。ブラウザでJSが動くと、ボタンのクリックなど**インタラクション**が有効になる。この過程をハイドレーションと呼ぶ |

---

## 2. JavaScript / TypeScript の基本

### 言語の基礎

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **TypeScript (TS)** | タイプスクリプト | 型付きJavaScript | Javaの型システムをJavaScriptに持ち込んだもの | Microsoftが開発。JavaScriptに**静的型チェック**を追加した上位互換言語。コンパイル（トランスパイル）するとJavaScriptに変換される |
| **型推論** | かたすいろん | 型を書かなくても分かる | Javaの `var x = 10;`（Java 10以降のローカル変数型推論） | `const x = 10` と書けばTSが `x` は `number` と自動推論する。全部書かなくていい |
| **interface / type** | インターフェース / タイプ | 型の定義 | Javaの `interface` / `class` のフィールド定義 | `interface User { name: string; age: number; }` — Javaのクラス定義に似ているが、ランタイムには存在しない（型チェックのみ） |
| **ジェネリクス** | ジェネリクス | 型の変数化 | Javaの `List<T>` と同じ | `Array<string>`, `Promise<User>` のように型を引数として渡す。Javaと全く同じ概念 |
| **ES Modules (ESM)** | イーエスモジュール | import/exportの仕組み | Javaの `import` 文 + `public` 修飾子 | `export const fn = () => {}` で公開、`import { fn } from './file'` で使う。Javaのパッケージとimport文に相当 |
| **CommonJS (CJS)** | コモンジェイエス | 旧式のimport/export | 古いJavaのクラスパス方式 | `module.exports = fn` / `const fn = require('./file')` — Node.jsの旧方式。`.cjs` 拡張子で区別 |
| **アロー関数** | アローかんすう | `=>` で書く関数 | Javaのラムダ式 `(x) -> x + 1` | `const add = (a, b) => a + b;` — Javaの `BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;` に相当 |
| **分割代入 (Destructuring)** | ぶんかつだいにゅう | オブジェクトをバラす | 直接の対応なし | `const { name, age } = user;` でオブジェクトのプロパティを個別変数に展開する。Javaにはない便利機能 |
| **スプレッド構文** | スプレッド | `...` で展開 | Stream の `flatMap` に近い | `{...obj, name: 'new'}` でオブジェクトをコピーして一部を上書き。Javaの `BeanUtils.copyProperties()` + setter |
| **Promise** | プロミス | 非同期処理の結果 | `CompletableFuture<T>` | 「まだ完了していないが、将来値が返る」ことを表すオブジェクト。`then()` でチェーンするか `await` で待つ |
| **async / await** | エイシンク / アウェイト | 非同期を同期風に書く | `CompletableFuture.get()` を自動でやってくれる構文 | `const data = await fetchUser()` — Promiseの結果を待って変数に入れる。Javaで `Future.get()` するのと似ているが、スレッドをブロックしない |
| **コールバック** | コールバック | 後で呼ばれる関数 | Javaの `Runnable` / `Consumer<T>` を引数で渡すパターン | `button.onClick(() => alert('clicked'))` — 「〇〇が起きたら、この関数を実行して」と渡す関数 |
| **null / undefined** | ナル / アンディファインド | 値がない状態 | `null` の2種類版 | `null` は「意図的に値がない」、`undefined` は「まだ値が設定されていない」。Javaの `null` が2つに分かれたようなもの |
| **truthy / falsy** | トゥルーシー / フォルシー | 暗黙の真偽変換 | 直接の対応なし | `0`, `""`, `null`, `undefined`, `NaN` は `false` 扱い（falsy）。それ以外は `true`（truthy）。`if (user)` で null チェックできる |
| **Optional Chaining `?.`** | オプショナルチェイニング | nullなら止まる | `Optional.map()` のチェーン | `user?.address?.city` — 途中が `null/undefined` ならエラーにならず `undefined` を返す。Java の `Optional.map(User::getAddress).map(Address::getCity)` に相当 |
| **Nullish Coalescing `??`** | ヌリッシュコアレッシング | null/undefinedのデフォルト値 | `Optional.orElse()` | `name ?? 'Anonymous'` — `name` が `null` か `undefined` の時だけ `'Anonymous'` を使う |
| **テンプレートリテラル** | テンプレートリテラル | 文字列に変数を埋め込む | `String.format()` / `"Hello, " + name` | `` `Hello, ${name}` `` — バッククォートで囲み、`${}` 内に式を書ける |

---

## 3. Node.js / パッケージ管理

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Node.js** | ノードジェイエス | サーバー側のJS実行環境 | **JVM (Java Virtual Machine)** | ブラウザの外でJavaScriptを実行する環境。ビルドツール、テスト、サーバーなどに使う。JVMが `.class` を実行するように、Node.jsは `.js` を実行する |
| **npm** | エヌピーエム | パッケージ管理ツール | **Maven / Gradle** | Node Package Manager。ライブラリのインストール・バージョン管理を行う。`npm install axios` = `<dependency>` に追加して `mvn install` |
| **yarn** | ヤーン | 高速なnpm代替 | Maven の高速版（Gradle的な位置づけ） | Facebookが開発。npmと互換性がありつつ、高速・安定。`yarn install` = `mvn dependency:resolve` |
| **pnpm** | ピーエヌピーエム | 省ディスクnpm代替 | ローカルリポジトリを共有するMaven | ディスク使用量を削減するパッケージマネージャー。大きいモノレポで特に有効 |
| **package.json** | パッケージジェイソン | プロジェクト定義ファイル | **`pom.xml`** | 依存関係、スクリプト、プロジェクト名などを定義。`dependencies` がMavenの `<dependencies>` に相当 |
| **node_modules/** | ノードモジュールズ | 依存ライブラリの実体 | **`.m2/repository`**（ローカルMavenリポジトリ） | `npm install` でダウンロードされたライブラリが入るフォルダ。数万ファイルになることもある。Gitには含めない |
| **lock ファイル** | ロックファイル | 依存バージョンの固定 | `pom.xml` の `<version>` をすべて固定したもの | `yarn.lock` / `package-lock.json`。全員が同じバージョンを使うことを保証する。Gradle の `gradle.lockfile` に相当 |
| **dependencies** | デペンデンシーズ | 本番で使う依存 | `<scope>compile</scope>` の依存 | `react`, `axios` など、本番コードに含まれるライブラリ |
| **devDependencies** | デブデペンデンシーズ | 開発時だけ使う依存 | `<scope>test</scope>` の依存 | `vitest`, `eslint`, `typescript` など、ビルド・テストにしか使わないライブラリ |
| **scripts** | スクリプツ | コマンドのエイリアス | Mavenプロファイル / Makefileのターゲット | `"dev": "vite"` → `yarn dev` で `vite` が起動。`mvn spring-boot:run` のようにコマンドを統一 |
| **npx** | エヌピーエックス | パッケージを一時実行 | `mvn exec:java` | インストールせずに一度だけコマンドを実行する。`npx create-react-app my-app` |
| **Corepack** | コアパック | パッケージマネージャ管理 | SDKMAN!（JDKバージョン管理）に近い | Node.jsに内蔵。`corepack enable` でyarnやpnpmを自動管理 |
| **モノレポ** | モノレポ | 複数プロジェクト1リポジトリ | マルチモジュールMavenプロジェクト | `apps/react-vite/`, `packages/shared/` のように1リポジトリに複数パッケージを配置する構成 |

---

## 4. React の基本概念

### コア概念

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **React** | リアクト | UIライブラリ | **Thymeleaf**（テンプレートエンジン） + α | Metaが開発。「データが変わったらUIを自動で更新する」ライブラリ。MVCの**V（View）**を担当するが、ロジックも書ける |
| **コンポーネント** | コンポーネント | UIの部品 | Thymeleaf の**フラグメント** / JSPの**カスタムタグ** | `function Button({ label }) { return <button>{label}</button>; }` — HTMLの再利用可能な部品。Javaのクラスに近い |
| **JSX** | ジェイエスエックス | HTML風のJS構文 | Thymeleaf のテンプレート式（`th:text`, `th:each`） | `return <div className="title">{user.name}</div>` — JavaScript の中にHTML風の記法を書ける。実体は `React.createElement()` の呼び出しに変換される |
| **TSX** | ティーエスエックス | TypeScript版JSX | 型付きのThymeleafテンプレート | `.tsx` ファイル。JSXにTypeScriptの型チェックが効く |
| **Props** | プロップス | 親→子への入力 | メソッドの引数 / Thymeleaf の `th:with` | `<Button label="Submit" onClick={handleClick} />` — 親コンポーネントが子に渡すデータ。**読み取り専用**（イミュータブル） |
| **children** | チルドレン | タグの中身 | `<slot>` / Thymeleaf の `layout:fragment` | `<Card><p>中身</p></Card>` — タグに挟まれた要素が `children` として渡される。レイアウトコンポーネントでよく使う |
| **State（状態）** | ステート | コンポーネント内のデータ | `HttpSession` に格納する値 / インスタンス変数 | ボタンの開閉状態、入力中のテキストなど、**時間とともに変わるデータ**。Stateが変わるとReactが自動で画面を再描画する |
| **再レンダリング** | さいレンダリング | 画面の再描画 | `return "forward:/page"` でコントローラを再実行するイメージ | State や Props が変わると、Reactはそのコンポーネントの関数を**もう一度実行**して、新しいUIを生成する。仮想DOMの差分だけ実際のDOMに反映するので高速 |
| **条件付きレンダリング** | じょうけんつき— | 条件で表示を切り替え | Thymeleaf の `th:if` | `{isAdmin && <AdminPanel />}` — 条件が `true` の時だけ表示する。`th:if="${user.role == 'ADMIN'}"` と同じ |
| **リスト描画** | リストびょうが | 配列をループ表示 | Thymeleaf の `th:each` | `{users.map(u => <li key={u.id}>{u.name}</li>)}` — `th:each="user : ${users}"` と同じ |
| **key** | キー | リスト要素の識別子 | DBのプライマリキー | `<li key={user.id}>` — Reactがリストの各要素を**一意に識別する**ためのID。これがないとリストの更新が正しく動かない |
| **Fragment** | フラグメント | 余分なDOMを作らない囲み | ラッパーdiv不要の `<></>` | `<><h1>Title</h1><p>Text</p></>` — コンポーネントは1つのルート要素を返す必要があるが、余計な `<div>` を追加したくない時に使う |
| **イベントハンドラ** | イベントハンドラ | ユーザー操作への対応 | `@EventListener` / ボタンの `onclick` | `<button onClick={() => setCount(c + 1)}>` — クリック、入力変更、送信などのイベントに対応する関数 |

---

### Hooks（フック）

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Hooks** | フックス | コンポーネントに機能を足す仕組み | Springの **AOP** / **アノテーション** | `use〇〇` で始まる関数。状態管理、副作用、コンテキスト参照などをコンポーネントに追加する。クラス不要で関数だけで完結 |
| **useState** | ユーズステート | 状態を持つ | インスタンス変数の宣言 | `const [count, setCount] = useState(0)` — `count` が値、`setCount` が更新関数。setter経由でないと画面が更新されない（Java Beanのsetterに近い） |
| **useEffect** | ユーズエフェクト | 副作用を実行 | `@PostConstruct` / `@PreDestroy` | `useEffect(() => { fetchData(); }, [userId])` — 「コンポーネント描画後に実行する処理」を定義。API呼び出し、タイマー開始、イベント登録などに使う。`[userId]` は依存配列で、`userId` が変わった時だけ再実行 |
| **useContext** | ユーズコンテキスト | 離れた場所のデータ参照 | `@Autowired` でDIしたBean | `const theme = useContext(ThemeContext)` — Props のバケツリレーなしに、祖先コンポーネントのデータを直接参照する。Spring の DI コンテナからBeanを取得するイメージ |
| **useMemo** | ユーズメモ | 計算結果をキャッシュ | `@Cacheable` メソッド | `useMemo(() => expensiveCalc(data), [data])` — `data` が変わるまで再計算しない。高コストな処理の最適化に使う |
| **useCallback** | ユーズコールバック | 関数をキャッシュ | メソッド参照のキャッシュ | `useCallback(() => handleClick(id), [id])` — 関数の再生成を防ぐ。子コンポーネントの不要な再レンダリングを防止 |
| **useRef** | ユーズレフ | 再描画しない変数 | `private transient` なインスタンス変数 | `const ref = useRef(null)` — 値を保持するが、変更しても再レンダリングを起こさない。DOM要素への直接参照にも使う |
| **カスタムフック** | カスタムフック | 自作のHook | 自作の `@Annotation` + AOP / ユーティリティメソッド | `useDisclosure()`, `useUser()` など `use` で始まる自作関数。状態やロジックを再利用可能な形で切り出す |

---

### コンポーネントの種類

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **関数コンポーネント** | かんすう— | 現在の標準 | 1つのメソッドだけのクラス | `function Button({ label }) { return <button>{label}</button>; }` — 関数がそのままUIパーツになる。現在のReactはこれが主流 |
| **クラスコンポーネント** | クラス— | 旧方式 | Javaのクラスベース設計 | `class Button extends React.Component { render() { ... } }` — 古いReactで使われた方式。Hooks登場後は非推奨 |
| **Presentationalコンポーネント** | プレゼンテーショナル— | 見た目だけ担当 | Thymeleaf テンプレート（ロジックなし） | Props を受け取って表示するだけ。API呼び出しや状態管理をしない。**純粋なView** |
| **Containerコンポーネント** | コンテナ— | ロジック担当 | Controller（データ取得→Viewに渡す） | データ取得・状態管理を行い、Presentationalコンポーネントにデータを渡す |

---

## 5. React のデータ管理

### 状態管理

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **サーバーステート** | サーバーステート | APIから取得したデータ | DB から SELECT した結果 | ユーザー一覧、議論リストなど**サーバーが持つ真実のデータ**。React Queryで管理するのが現代の主流 |
| **クライアントステート** | クライアントステート | ブラウザ側だけのデータ | `HttpSession` の値 | モーダルの開閉状態、通知リスト、フォーム入力中の値など**サーバーに送らないUI用のデータ** |
| **React Query (TanStack Query)** | リアクトクエリ | サーバーデータ管理 | **Spring Cache** + 非同期HTTPクライアント | サーバーデータの取得・キャッシュ・同期・再取得を自動化するライブラリ。手動で `loading` や `error` 状態を管理しなくてよい |
| **useQuery** | ユーズクエリ | データ取得Hook | `@Cacheable` 付きの `findAll()` | `useQuery({ queryKey: ['users'], queryFn: fetchUsers })` — データ取得+キャッシュ+ローディング管理を全自動化 |
| **useMutation** | ユーズミューテーション | データ更新Hook | `@CacheEvict` 付きの `save()` / `delete()` | POST/PUT/DELETEの実行 + 成功/失敗ハンドリング + キャッシュ無効化を管理 |
| **queryKey** | クエリキー | キャッシュの識別子 | `@Cacheable` の `key` 引数 | `['discussions', { page: 1 }]` — このキーでキャッシュを特定する。同じキーなら再取得しない |
| **staleTime** | ステイルタイム | キャッシュの有効期間 | `@Cacheable` の `expireAfterWrite` | 指定時間内はキャッシュを「新鮮」とみなし、再取得しない。デフォルト0（即座に古い扱い） |
| **invalidateQueries** | インバリデートクエリーズ | キャッシュの無効化 | `@CacheEvict` | データ更新後に「このキーのキャッシュは古くなった」と通知。Reactが自動で再取得する |
| **Zustand** | ズスタンド | 軽量状態管理 | シングルトンの `@Component` Bean | クライアントステート（通知、UI状態）を管理する。Reduxより簡潔。ドイツ語で「状態」の意味 |
| **Redux** | リダックス | 大規模状態管理 | ApplicationContextに格納するアプリケーションスコープのデータ | 状態管理の元祖だが、ボイラープレートが多い。React Query + Zustand の組み合わせで代替されつつある |
| **Context API** | コンテキストエーピーアイ | Reactの組み込みDI | **Spring の ApplicationContext / DI コンテナ** | Provider で値を注入し、useContext で取り出す。Spring の `@Autowired` と同じ発想だが、大量のデータ管理には不向き |
| **Props Drilling** | プロップスドリリング | Propsのバケツリレー | メソッド呼び出しの引数が延々と引き回されるアンチパターン | 親→子→孫→ひ孫と Props を何階層も渡し続けること。Context やZustand で解決する |
| **イミュータブル更新** | イミュータブル— | コピーして変更 | `new ArrayList<>(original)` で新しいリストを作る | `setState(prev => [...prev, newItem])` — 元の配列を変更（mutate）せず、新しい配列を作る。Reactは参照の違いで変更を検出する |

---

### フォーム

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **React Hook Form** | リアクトフックフォーム | フォーム管理ライブラリ | Spring MVC の `@ModelAttribute` + `BindingResult` | フォームの値管理、バリデーション、送信処理を効率的に行う。再レンダリングを最小限に抑える |
| **制御コンポーネント (Controlled)** | せいぎょ— | React が値を管理 | Spring MVC でサーバーがフォーム値を管理 | `<input value={name} onChange={e => setName(e.target.value)} />` — 入力値をReactのStateで管理する方式 |
| **非制御コンポーネント (Uncontrolled)** | ひせいぎょ— | DOM が値を管理 | 素のHTML `<form>` でブラウザが値を管理 | `<input ref={inputRef} />` — DOM要素が値を保持し、必要時に `ref.current.value` で取得する。React Hook Form はこの方式で高速化 |
| **Zod** | ゾッド | バリデーションライブラリ | **Bean Validation** (`@Valid`, `@NotNull`, `@Size`) | `z.string().min(1, 'Required').email('Invalid')` — TypeScript ファーストのスキーマ定義 + バリデーション。型も自動生成 |
| **resolver** | リゾルバー | バリデーション連携 | Bean Validation と Spring MVC の連携設定 | React Hook Form と Zod を繋ぐアダプター。`zodResolver(loginSchema)` でフォームにバリデーションルールを適用 |

---

## 6. ルーティング / ナビゲーション

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **React Router** | リアクトルーター | 画面遷移ライブラリ | **Spring MVC の `DispatcherServlet`** + `@RequestMapping` | URLとコンポーネントの対応付けを管理。`/app/users` → `<UsersPage />` のようにマッピング |
| **Route** | ルート | URLとコンポーネントの対応 | `@GetMapping("/users")` 1つ分 | `{ path: '/users', element: <UsersPage /> }` — 1つのURL→画面の対応定義 |
| **Router** | ルーター | Route の集合体 | コントローラー群のURL一覧 | `createBrowserRouter([...])` で全Routeをまとめて定義 |
| **ネストルート** | ネストルート | 子ルート | `@RequestMapping` のクラスレベル + メソッドレベルの組み合わせ | `/app` の下に `/app/users`, `/app/profile` をネストする。共通レイアウト（サイドバー等）を親ルートで定義 |
| **Outlet** | アウトレット | 子ルートの表示位置 | Thymeleaf の `layout:fragment("content")` | `<Outlet />` — ここに子ルートのコンポーネントが差し込まれる。レイアウトの「コンテンツ穴」 |
| **パスパラメータ** | パスパラメータ | URL内の変数 | `@PathVariable` | `path: 'discussions/:discussionId'` — Spring の `@GetMapping("/discussions/{id}")` と同じ。`useParams()` で取得 |
| **クエリパラメータ** | クエリパラメータ | `?key=value` | `@RequestParam` | `useSearchParams()` で `?page=2&sort=name` を取得。Spring の `@RequestParam` と同じ |
| **Navigate** | ナビゲート | プログラム的遷移 | `return "redirect:/login"` | `navigate('/login')` — ボタンクリック等でプログラムから画面遷移する。`useNavigate()` Hook で取得 |
| **Link** | リンク | 宣言的遷移 | Thymeleaf の `<a th:href="@{/users}">` | `<Link to="/users">Users</Link>` — クリックで画面遷移。`<a>` タグと違いページリロードが発生しない |
| **Lazy Loading（遅延読込）** | レイジーローディング | 必要時に読み込む | `@Lazy` Bean / プラグインの動的ロード | `lazy: () => import('./routes/users')` — その画面に遷移するまでJSファイルをダウンロードしない。初回読み込みを軽量化 |
| **clientLoader** | クライアントローダー | 画面表示前のデータ取得 | `@ModelAttribute` / Controller で Model にデータを詰める処理 | 画面のコンポーネントが描画される**前に**データを取得する。画面を開いた瞬間にローディングスピナーが出ない |

---

## 7. HTTP通信 / API連携

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Axios** | アクシオス | HTTPクライアント | **`RestTemplate`** / **`WebClient`** | `axios.get('/api/users')` — ブラウザからAPIサーバーにHTTPリクエストを送るライブラリ |
| **Fetch API** | フェッチエーピーアイ | ブラウザ標準のHTTP通信 | `HttpURLConnection`（低レベルAPI） | `fetch('/api/users')` — ブラウザに組み込まれたHTTPクライアント。Axiosは Fetch の便利ラッパー |
| **インターセプター** | インターセプター | リクエスト/レスポンスの前後処理 | Spring の **`HandlerInterceptor`** / `ClientHttpRequestInterceptor` | `api.interceptors.request.use(config => { config.headers.Authorization = ...; })` — 全リクエストに認証ヘッダーを自動付与するなど |
| **CORS** | コルス | 異なるドメイン間の通信許可 | Spring の `@CrossOrigin` / `CorsFilter` | Cross-Origin Resource Sharing。`localhost:3000` のフロントから `localhost:8080` のAPIを呼ぶ時に必要な設定 |
| **REST API** | レストエーピーアイ | HTTPベースのAPI設計 | Spring の `@RestController` | `GET /api/users`, `POST /api/discussions` — URL + HTTPメソッドでリソースを操作するAPI設計スタイル |
| **baseURL** | ベースユーアールエル | APIの共通URLプレフィックス | `application.yml` の `api.base-url` | `axios.create({ baseURL: 'http://api.example.com/api' })` — 全リクエストに共通するURLの先頭部分 |
| **withCredentials** | ウィズクレデンシャルズ | Cookie自動送信 | `RestTemplate` でCookieを送る設定 | `true` にすると、クロスオリジンのリクエストでもCookie（セッションID等）を自動で送信する |

---

## 8. スタイリング / CSS

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **TailwindCSS** | テイルウィンドシーエスエス | ユーティリティCSSフレームワーク | **Bootstrap**（ただしクラス名でスタイルを直接指定する方式） | `<div className="mt-4 p-2 bg-blue-500 text-white">` — CSSを書かずにクラス名だけでスタイリングする。`mt-4` = margin-top: 1rem |
| **className** | クラスネーム | CSSクラス指定 | HTMLの `class` 属性 | JSXでは `class` がJavaScript予約語のため `className` を使う。`<div className="title">` = `<div class="title">` |
| **CSS Modules** | シーエスエスモジュールズ | スコープ付きCSS | パッケージプライベートなスタイル | `.module.css` ファイルのクラス名が自動でユニーク化される。他コンポーネントとのクラス名衝突を防ぐ |
| **CSS-in-JS** | シーエスエスインジェイエス | JSの中にCSS | Javaアノテーションでスタイルを定義するイメージ | `styled-components` 等。JavaScriptの中にCSSを書く方式。Tailwind普及で利用は減少傾向 |
| **CSS変数 (Custom Properties)** | シーエスエスへんすう | CSS内の変数 | `application.yml` のプロパティ | `--primary: #3b82f6;` → `color: var(--primary);` で参照。テーマ切り替え（ダークモード等）に使う |
| **Radix UI** | ラディックスユーアイ | ヘッドレスUIライブラリ | 見た目なしのUIコンポーネント集 | アクセシビリティ完備のダイアログ・ドロップダウン等を提供。**見た目は自分で付ける**（ヘッドレス = headless）。TailwindCSSと組み合わせて使う |
| **レスポンシブデザイン** | レスポンシブ— | 画面サイズで表示を変える | 直接の対応なし | `className="w-full md:w-1/2 lg:w-1/3"` — スマホ幅100%、タブレット50%、PC33%のように画面サイズで切り替え |

---

## 9. ビルド / 開発ツール

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Vite** | ヴィート | 高速ビルドツール | **Maven / Gradle**（ビルドツール）+ **組み込みTomcat**（開発サーバー） | フランス語で「速い」。開発中はESMで即座にブラウザに配信、本番はRollupでバンドル。webpack より圧倒的に速い |
| **webpack** | ウェブパック | 従来のビルドツール | Mavenの旧バージョン的な存在 | 長年の標準だがビルドが遅い。新規プロジェクトではViteに移行する流れ |
| **バンドル** | バンドル | ファイルをまとめる | `mvn package` で JAR にまとめる | 数百のJSファイルを数個のファイルにまとめる。ブラウザのHTTPリクエスト数を減らして高速化 |
| **トランスパイル** | トランスパイル | 言語変換 | Lombokの `@Data` → getter/setter生成 | TypeScript → JavaScript、JSX → `createElement()` に変換する。古いブラウザ用にES6+ → ES5に変換することも |
| **HMR (Hot Module Replacement)** | エイチエムアール | 変更の即時反映 | Spring DevTools の自動リスタート（ただし**リスタートなし**） | ファイルを保存すると、ブラウザが自動で変更部分だけ更新される。画面の状態を維持したまま。開発体験が劇的に向上 |
| **ESLint** | イーエスリント | コード静的解析 | **Checkstyle** / **SpotBugs** / **SonarQube** | コーディング規約違反、バグの可能性、未使用変数などを自動検出。CI で必ず実行する |
| **Prettier** | プリティアー | コードフォーマッター | **Google Java Format** / **EditorConfig** | インデント、引用符、末尾カンマなどを自動整形。ESLint（ルール違反の検出）とは役割が異なる |
| **ソースマップ** | ソースマップ | デバッグ用の対応表 | Spring Bootのスタックトレース（コンパイル後のバイトコード→ソースの行番号の対応） | バンドル・圧縮されたコードからオリジナルのTypeScriptソースを復元する。ブラウザの開発者ツールで元のソースをデバッグできる |
| **Tree Shaking** | ツリーシェイキング | 不要コードの除去 | ProGuard / R8 の未使用クラス除去 | `import { Button } from './ui'` で `Button` だけ使う場合、`ui` モジュールの他のコンポーネントをバンドルから除外する |
| **Code Splitting** | コードスプリッティング | JSファイルの分割 | マイクロサービスの JAR 分割 | 1つの巨大なJSファイルではなく、画面ごとに分割して必要な時にダウンロードする。`lazy(() => import('./page'))` |
| **環境変数** | かんきょうへんすう | 環境ごとの設定値 | `application.yml` / `application-dev.yml` | `.env` ファイルに `VITE_APP_API_URL=...` と書く。Viteでは `VITE_` プレフィックスが必要 |
| **Plop** | プロップ | コードジェネレーター | **Maven Archetype** / Spring Initializr | テンプレートからファイルを自動生成。`yarn generate` で対話的にコンポーネントのひな形を作成 |

---

## 10. テスト

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Vitest** | ヴィテスト | テストフレームワーク | **JUnit 5** | Vite ベースの高速テストランナー。`describe`, `it`, `expect` でテストを記述 |
| **Jest** | ジェスト | 旧標準テストフレームワーク | JUnit 4 | Metaが開発。長年の標準だが、Viteプロジェクトでは Vitest に移行する流れ |
| **describe / it / test** | ディスクライブ / イット / テスト | テストの構造化 | `@Nested` / `@Test` | `describe('Button', () => { it('should render', () => { ... }); })` — `describe` がグループ、`it` / `test` が個別テスト |
| **expect** | エクスペクト | アサーション | `assertThat()` (AssertJ) | `expect(result).toBe(42)` — `assertThat(result).isEqualTo(42)` と同じ。`.toContain()`, `.toThrow()` 等多数 |
| **Testing Library** | テスティングライブラリ | UI テストユーティリティ | MockMvc の `andExpect(content())` | `screen.getByText('Submit')` — 実際のDOMを操作してUIをテスト。「ユーザーが見るもの」でテストする思想 |
| **render** | レンダー | テスト内でコンポーネントを描画 | `MockMvc.perform(get("/"))` | `render(<Button label="Click" />)` — テスト用のDOMにコンポーネントを描画する |
| **screen** | スクリーン | 描画結果の参照 | MockMvc のレスポンスオブジェクト | `screen.getByText('Submit')` — render したDOMから要素を検索する |
| **userEvent** | ユーザーイベント | ユーザー操作のシミュレーション | MockMvc の `.perform(post())` | `await userEvent.click(button)` — クリック、入力、タブ移動などを再現 |
| **MSW** | エムエスダブリュー | APIモック | **WireMock** / `@MockBean` | Mock Service Worker。ブラウザ（Service Worker）またはNode.js でHTTPリクエストをインターセプトし、モックレスポンスを返す |
| **handler** | ハンドラー | モックの定義 | WireMock のスタブ定義 | `http.get('/api/users', () => HttpResponse.json(mockUsers))` — このURLにGETが来たらこのレスポンスを返す |
| **jsdom** | ジェイエスドム | 仮想ブラウザ環境 | `@SpringBootTest(webEnvironment = MOCK)` | Node.js上でブラウザのDOMをシミュレートする。実際のブラウザを起動せずにUIテストができる |
| **Playwright** | プレイライト | E2Eテストツール | **Selenium** / **Cypress** | 実際のブラウザ（Chromium, Firefox, WebKit）を操作してテスト。`page.goto('/login')` → `page.fill('#email', 'test@test.com')` → `page.click('button')` |
| **Storybook** | ストーリーブック | UIカタログ | **Swagger UI**（APIではなくUIコンポーネントの一覧） | コンポーネントを個別に表示・操作できるツール。デザイナーとの確認、Visual Regression Test に使う |
| **Story** | ストーリー | コンポーネントの使用例 | Swagger の API リクエスト例 | `export const Primary: Story = { args: { variant: 'primary', label: 'Button' } }` — コンポーネントの表示パターンを定義 |
| **setupFiles** | セットアップファイルズ | テスト前処理 | `@BeforeAll` のグローバル版 | 全テスト実行前に自動で読み込まれるファイル。MSWの起動やグローバルモックの設定を行う |
| **act** | アクト | 状態更新の待機 | トランザクションの完了を待つ | `act(() => { button.click(); })` — Reactの状態更新が完了するまで待つユーティリティ。テストの安定性に必要 |

---

## 11. 認証 / セキュリティ

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **JWT** | ジェイダブリューティー | トークン認証 | Spring Security の JWT 認証フィルター | JSON Web Token。ログイン成功時にサーバーが発行し、以降のリクエストに付与して認証する |
| **Cookie認証** | クッキーにんしょう | セッションID方式 | Spring Session の `JSESSIONID` | サーバーがCookieにセッションIDを格納。`withCredentials: true` でAxiosが自動送信 |
| **ProtectedRoute** | プロテクテッドルート | 認証必須の画面 | Spring Security の `.authenticated()` | 未ログインユーザーがアクセスすると自動でログイン画面にリダイレクトするラッパーコンポーネント |
| **RBAC** | アールバック | ロールベース認可 | `@Secured("ROLE_ADMIN")` | Role-Based Access Control。`ADMIN` / `USER` のようなロールで表示/操作を制御する |
| **XSS** | エックスエスエス | スクリプト注入攻撃 | SQLインジェクションのHTML版 | Cross-Site Scripting。ユーザー入力をHTMLにそのまま埋め込むと、悪意のあるJavaScriptが実行される。Reactは自動でエスケープするが、`dangerouslySetInnerHTML` を使う場合はサニタイズが必要 |
| **CSRF** | シーエスアールエフ | リクエスト偽造攻撃 | Spring Security の CSRF トークン | Cross-Site Request Forgery。SPA + Cookie認証の場合、`SameSite` Cookie設定やCSRFトークンで防御 |
| **サニタイズ** | サニタイズ | 危険な入力を無害化 | OWASP HTML Sanitizer | `DOMPurify.sanitize(userInput)` — HTMLからスクリプトタグ等を除去。Markdown 描画時に必須 |

---

## 12. パフォーマンス / 最適化

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Suspense** | サスペンス | 読み込み中の表示 | try-catch で「処理中」を表示するパターン | `<Suspense fallback={<Spinner />}>` — 子コンポーネントが非同期処理中の間、`fallback` を表示する。Spring の `DeferredResult` + ローディング画面 |
| **ErrorBoundary** | エラーバウンダリー | エラー時の表示 | `@ControllerAdvice` + `@ExceptionHandler` | 子コンポーネントでエラーが発生した時に、画面全体を壊さず「エラーが発生しました」を表示する |
| **メモ化 (Memoization)** | メモか | 同じ計算を繰り返さない | `@Cacheable` メソッド | `React.memo(Component)` / `useMemo` / `useCallback` — 入力が同じなら前回の結果を再利用する |
| **Prefetch** | プリフェッチ | 先読み | SELECT文の先行実行 | ユーザーが「次に開きそうな画面」のデータを先に取得しておく。マウスホバー時にprefetchすると、クリック時に即表示 |
| **Infinite Scroll（無限スクロール）** | インフィニットスクロール | スクロールで自動追加読込 | `Slice` + 「もっと読む」ボタン | `useInfiniteQuery` で実装。ページ末尾までスクロールすると次のページを自動で取得する |
| **Debounce** | デバウンス | 連打防止 | Rate Limiter / スロットリング | 「ユーザーが入力を止めてから300ms後に処理する」。検索窓の入力で毎キーストロークごとにAPIを叩かないようにする |
| **Throttle** | スロットル | 実行頻度の制限 | Rate Limiter | 「100msに1回だけ処理する」。スクロールイベントの処理頻度を制限する |

---

## 13. 設計パターン / アーキテクチャ

| 用語 | 読み方 | 一言で | バックエンドで例えると | もう少し詳しく |
|---|---|---|---|---|
| **Feature-based Architecture** | フィーチャーベースド— | 機能単位でフォルダを分ける | DDDのBounded Context / パッケージby機能 | `features/discussions/`, `features/users/` のように機能単位でコードを整理する。Bulletproof React の核心 |
| **Barrel Export** | バレルエクスポート | index.ts で再エクスポート | `package-info.java` / public API の定義 | `index.ts` で `export { Button } from './button'` — モジュールの公開APIを1ファイルで定義。外部からは `import { Button } from '@/components/ui/button'` でアクセス |
| **Colocation** | コロケーション | 関連ファイルを近くに置く | 同じパッケージにController/Service/Repositoryを置く | テスト、Story、スタイル等を対象コンポーネントの隣に配置する。「探す距離」を最小化する設計原則 |
| **関心の分離 (SoC)** | かんしんのぶんり | 役割ごとに分ける | SOLID原則のSRP | Separation of Concerns。`api/` はデータ取得、`components/` は表示、`hooks/` はロジックと分離する |
| **宣言的UI** | せんげんてきユーアイ | 「何を表示するか」だけ書く | SQLで「何が欲しいか」を書くのに近い | `<UserList users={users} />` — 「どうやってDOMを操作するか」ではなく「こういう状態の時こう表示する」と書く。Reactの設計思想 |
| **命令的UI** | めいれいてきユーアイ | 「どう操作するか」を書く | JDBC で `ResultSet` をループして手動マッピング | `document.getElementById('list').appendChild(li)` — DOM操作を1ステップずつ記述する。jQuery 時代の方式 |
| **合成 (Composition)** | ごうせい | コンポーネントの組み合わせ | デコレーターパターン / ストラテジーパターン | 継承ではなく、コンポーネントを**入れ子にして組み合わせる**。`<Layout><Sidebar /><Content /></Layout>` — Reactでは「継承より合成」が原則 |
| **Higher-Order Component (HOC)** | ハイヤーオーダー— | コンポーネントを拡張する関数 | デコレーターパターン / Spring AOP の `@Around` | `withAuth(Component)` — コンポーネントに認証チェックなどの機能を追加する。現在はHooksで代替されることが多い |
| **Render Props** | レンダープロップス | 描画関数を渡すパターン | ストラテジーパターン（描画ロジックを外から注入） | `<Mouse render={(x, y) => <Cat x={x} y={y} />} />` — 子の描画方法を親が関数で渡す。現在はHooksで代替 |
| **Provider Pattern** | プロバイダーパターン | データを注入する仕組み | **Spring DI コンテナ** | `<ThemeProvider value={theme}>...</ThemeProvider>` — 子孫コンポーネント全体にデータを提供。Spring の `@Bean` 登録と `@Autowired` 取得に相当 |

---

## 付録：よく見る略語・ファイル拡張子

| 表記 | 正式名称 | 意味 |
|---|---|---|
| `.tsx` | TypeScript JSX | TypeScript + JSX（React コンポーネントファイル） |
| `.ts` | TypeScript | 通常の TypeScript ファイル |
| `.jsx` | JavaScript JSX | JavaScript + JSX |
| `.css` | Cascading Style Sheets | スタイルシート |
| `.json` | JavaScript Object Notation | データ記述フォーマット（Java の Map<String, Object> に近い） |
| `.md` | Markdown | ドキュメント記述フォーマット |
| `.yml` / `.yaml` | YAML | 設定ファイルフォーマット |
| `.env` | Environment | 環境変数ファイル |
| `.d.ts` | Declaration TypeScript | 型定義ファイル（実装なし、型情報のみ。Java の `interface` だけのファイルに近い） |
| `.cjs` | CommonJS | CommonJS形式のJSファイル |
| `.mjs` | Module JavaScript | ESModule形式のJSファイル |
| `.hbs` | Handlebars | テンプレートエンジンのファイル |
| `.spec.ts` | Specification | テストファイル（`.test.ts` と同じ意味） |
| `.stories.tsx` | Stories | Storybook のストーリー定義ファイル |

---

## 付録：Reactプロジェクトでよく使うコマンド対応表

| やりたいこと | React / Node.js | Java / Spring Boot |
|---|---|---|
| プロジェクト作成 | `npm create vite@latest` | `spring init` / Initializr |
| 依存インストール | `yarn install` | `mvn dependency:resolve` |
| 依存追加 | `yarn add axios` | `pom.xml` に `<dependency>` 追加 |
| 開発サーバー起動 | `yarn dev` | `mvn spring-boot:run` |
| ビルド | `yarn build` | `mvn package` |
| テスト実行 | `yarn test` | `mvn test` |
| 静的解析 | `yarn lint` | `mvn checkstyle:check` |
| 型チェック | `yarn check-types` | `mvn compile`（コンパイルエラー検出） |
| フォーマット | `yarn prettier --write .` | `mvn spotless:apply` |
| E2Eテスト | `yarn test-e2e` | Selenium / Cypress テスト |
| UIカタログ起動 | `yarn storybook` | Swagger UI 起動 |
| コード生成 | `yarn generate` | `mvn archetype:generate` |
