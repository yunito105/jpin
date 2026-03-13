# フロントエンド & モバイル アーキテクチャ比較

> **プロジェクト規模**: 25億円  
> **業態**: 小売業（IKEA/ニトリ/カインズ型）海外販売システム  
> **作成日**: 2026-03-13  
> **ステータス**: ドラフト（テックリードレビュー用）  
> **関連**: [architecture-decision.md](architecture-decision.md)

---

## 1. フロントエンド — レンダリング方式の比較

「画面をどこで作るか」の選択。SEO・初期表示速度・開発体験に直結する。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **SPA（CSR のみ）** | ブラウザ側でJSが全画面を描画する。初回ロード後はページ遷移が高速 | ・UX が滑らか（ページ遷移が瞬時）<br>・サーバーレスで配信可能（CDN）<br>・FE/BEの完全分離が容易 | ・初期表示が遅い（JS ダウンロード待ち）<br>・SEO が弱い（クローラーがJSを実行しないと空ページ）<br>・大規模になるとバンドルサイズ肥大化 | ・管理画面など SEO 不要な内部ツール<br>・認証後のダッシュボードのみならOK<br>・検索エンジン流入が不要 |
| **SSR（Pages Router）** | サーバーで HTML を生成してから返す。Next.js Pages Router の `getServerSideProps` 等 | ・初期表示が速い（HTML が即返る）<br>・SEO に強い<br>・既存のNext.js資産・ノウハウが豊富 | ・リクエストごとにサーバー処理が必要（負荷）<br>・Pages Router は今後メンテモードへ移行の流れ<br>・レイアウト共有が冗長 | ・SEO が必要な商品ページがある<br>・チームが Pages Router に慣れている<br>・段階的に App Router へ移行予定 |
| **SSR + RSC（App Router）** | React Server Components でサーバー側の実行とクライアント側を明確に分離。Next.js 14+ の推奨 | ・サーバーコンポーネントはバンドルサイズに含まれない<br>・データ取得がコンポーネント単位で直感的<br>・Streaming SSR で即座に一部を表示<br>・レイアウト共有が容易 | ・Server/Client Component の境界の学習コストが高い<br>・サードパーティライブラリの対応がまだ不完全な場合あり<br>・デバッグ体験が SPA より複雑 | ・**本PJ推奨**（商品ページのSEO＋管理画面の両立）<br>・25億規模なら学習投資は十分回収可能<br>・新規開発なのでレガシー制約なし |
| **SSG（静的サイト生成）** | ビルド時に全ページの HTML を事前生成。CDN 配信 | ・最速の表示速度（CDN直）<br>・サーバーコスト最小<br>・セキュリティリスクが低い | ・更新のたびにビルドが必要<br>・動的コンテンツ（在庫数・価格変動）に不向き<br>・ページ数が多いとビルド時間が膨大 | ・ヘルプページ・利用規約など変更頻度の低いページ<br>・商品数が少なく価格変動がない場合のみ<br>・**本PJのメイン方式としては不適** |
| **ISR（増分静的再生成）** | SSG の発展形。一定時間ごとにバックグラウンドで再生成。Next.js の `revalidate` | ・SSG の速度 + ほぼリアルタイムの更新<br>・サーバー負荷を大幅削減<br>・CDN キャッシュ活用 | ・リアルタイム性が数秒〜数分遅れる<br>・在庫引当のようなミリ秒精度にはそぐわない<br>・キャッシュ制御の設計が必要 | ・商品一覧・カテゴリページなど「数分遅れてもOK」なページ<br>・ISR + App Router の組み合わせが実用的<br>・**本PJでは商品一覧に部分適用を推奨** |
| **MPA（従来型）** | Thymeleaf・JSP 等でサーバーが HTML を返す。ページ遷移ごとにフルリロード | ・学習コストが低い（Java 開発者がそのまま書ける）<br>・FE/BEの分離が不要<br>・SEO は自然に対応 | ・UX が古い（ページ遷移ごとに白画面）<br>・FE/BE が密結合（テンプレートエンジン依存）<br>・モバイルアプリと API を共有しにくい | ・小規模な社内システム向け<br>・FE専任がいないチーム<br>・**25億規模のECには不適** |

---

## 2. フロントエンド — コンポーネント設計パターンの比較

「コンポーネントをどう分類・整理するか」の方針。チーム規模とコンポーネント数に直結する。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **Feature-based（機能分割）** | `features/product/`、`features/cart/` のようにビジネス機能ごとにフォルダ分割。各 Feature が自己完結 | ・ドメイン境界が明確（バックエンドのドメイン分割と対応）<br>・機能追加時に既存に影響しにくい<br>・チーム分担がしやすい | ・Feature 間の共通処理の置き場に迷う<br>・Feature の粒度が人によってブレる<br>・初期設計にルール策定が必要 | ・**本PJ推奨**（ヘキサゴナルのドメイン分割と1:1対応）<br>・チームが5人以上<br>・ドメイン境界が明確な業務システム |
| **Atomic Design** | Atoms → Molecules → Organisms → Templates → Pages の5段階にコンポーネントを分類 | ・UIの再利用性が最大化<br>・デザインシステムとの親和性が高い<br>・コンポーネントカタログ（Storybook）と好相性 | ・Molecule vs Organism の境界が曖昧になりがち<br>・ビジネスロジックの置き場が不明確<br>・分類の議論に時間を取られる | ・デザインシステムを構築する場合<br>・UIの統一性が最優先（ブランドガイドライン厳格）<br>・**本PJでは共通UIコンポーネント層のみに適用を推奨** |
| **Bulletproof React** | `features/` + `components/` + `hooks/` + `lib/` + `providers/` の標準的な分割。Feature-based のベストプラクティス集 | ・実績ある構成で迷いが少ない<br>・テスト・API・型定義の配置が明確<br>・GitHub で公開されておりチーム共有しやすい | ・すべてのルールを厳密に適用すると初期工数が増える<br>・Next.js App Router の `app/` ディレクトリと整合を取る追加設計が必要 | ・Feature-based を採用しつつ具体的なルールが欲しいとき<br>・**本PJでは参考にしつつ App Router に合わせてカスタマイズ** |
| **Pages-based（Next.js デフォルト）** | `app/` または `pages/` ディレクトリにルーティングとコンポーネントを配置。Next.js のファイルベースルーティングに従う | ・Next.js 公式に沿っており学習コスト最小<br>・ルーティングとファイル構造が一致<br>・小〜中規模では十分 | ・大規模になるとページ単位の整理では限界<br>・共通ロジックの散在・重複が発生<br>・ドメイン境界が見えにくい | ・プロトタイプ・MVP フェーズ<br>・チームが小さい(3人以下)<br>・**本PJの最終形としては不十分** |
| **FSD（Feature-Sliced Design）** | `app/` → `pages/` → `widgets/` → `features/` → `entities/` → `shared/` の厳格なレイヤー構成 | ・レイヤー間の依存方向が厳密（上→下のみ）<br>・大規模プロジェクトでのスケーラビリティが高い<br>・バックエンドのレイヤードアーキテクチャと思想が近い | ・学習コストが非常に高い<br>・日本語の資料・事例が少ない<br>・レイヤーが多く、小さい機能でもファイル数が増える | ・50人以上の超大規模 FE チーム<br>・アーキテクチャ設計専任がいる<br>・**本PJではオーバースペック** |

---

## 3. フロントエンド — 状態管理パターンの比較

「アプリケーションの状態（データ）をどこに・どう持つか」。サーバー状態とクライアント状態を区別することが重要。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **TanStack Query + useState** | サーバー状態（API データ）は TanStack Query、UI状態は `useState`/`useReducer` で管理 | ・サーバー状態のキャッシュ・再取得・楽観的更新が自動<br>・Redux のような大量のボイラープレート不要<br>・Server Component との共存が容易 | ・クライアント側のグローバルステートには別途手段が必要<br>・キャッシュ設計（staleTime 等）の理解が必要 | ・**本PJ推奨**（API呼び出しが主体のECサイト）<br>・サーバー状態が大半を占めるアプリ<br>・App Router + RSC との親和性が高い |
| **Zustand** | 軽量なグローバルステート管理。シンプルな API で store を作成 | ・学習コストが極めて低い<br>・バンドルサイズが小さい（~1KB）<br>・Redux DevTools 対応 | ・大規模になると store の分割戦略が必要<br>・ミドルウェア等のエコシステムは Redux に劣る | ・カート・UI設定などクライアント状態が少量ある場合に追加<br>・**TanStack Query と組み合わせてカート管理に利用可** |
| **Redux Toolkit** | Flux アーキテクチャの標準実装。Action → Reducer → Store の単方向データフロー | ・最大のエコシステム・実績<br>・DevTools が優秀<br>・大規模チームでの一貫性が高い | ・ボイラープレートが多い（RTK で軽減済みだが依然多い）<br>・サーバー状態の管理に過剰<br>・RSC との併用が複雑 | ・既存 Redux 資産がある場合の移行<br>・クライアントのグローバル状態が非常に多い<br>・**新規の EC サイトでは推奨しない** |
| **Jotai** | アトム（原子）単位で状態を定義。React の `useState` に近い感覚 | ・直感的な API（atom ベース）<br>・再レンダリングの最適化が自然<br>・TypeScript との親和性が高い | ・アトム数が増えるとグラフの見通しが悪くなる<br>・複雑な派生状態の管理は設計力が必要 | ・コンポーネント間で細かく状態を共有したい場合<br>・Zustand の代替として |
| **React Context のみ** | React 組み込みの Context API で状態を共有。追加ライブラリ不要 | ・追加依存ゼロ<br>・React 公式機能なので長期サポート安心<br>・認証情報・テーマなど低頻度更新に最適 | ・頻繁に更新されるデータで不要な再レンダリングが発生<br>・大量のデータを Context に入れるとパフォーマンス劣化<br>・テスト時のプロバイダーネストが深くなる | ・認証状態・テーマ・ロケールなど更新頻度が低いグローバル状態<br>・**本PJでは認証 Context のみに限定使用を推奨** |

---

## 4. フロントエンド — ディレクトリ構成パターンの比較

「実際にフォルダをどう切るか」。技術スタック（Next.js App Router）とチーム構成に合わせた具体案。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **パターンA: App Router + Feature-based（推奨）** | `app/` にルーティング、`features/` にビジネスロジック・コンポーネントを機能単位で配置 | ・ルーティングとビジネスロジックが分離<br>・バックエンドのドメイン分割と対応<br>・チーム分担が明確 | ・`app/` と `features/` の責務分離ルールの周知が必要<br>・ファイル数がやや多い | ・**本PJ推奨**<br>・EC サイトのように複数ドメイン（商品・注文・在庫）がある<br>・FE チーム5人以上 |
| **パターンB: App Router + Colocation** | `app/` ディレクトリ内にルーティングとコンポーネント・フック・テストをすべて同居させる | ・ファイル遷移の距離が短い（関連ファイルが近い）<br>・Next.js 公式のガイダンスに近い<br>・小規模チームでは見通しが良い | ・ドメイン横断のロジック共有が難しい<br>・`app/` 配下が肥大化しやすい<br>・ページ数が増えるとカオスに | ・FE チーム3人以下の小規模<br>・プロトタイプ段階<br>・ドメインが単純な場合 |
| **パターンC: src/ 直下フラット** | `src/components/`、`src/hooks/`、`src/lib/`、`src/types/` のように技術的関心ごとにフラット分割 | ・構成がシンプルで初心者にわかりやすい<br>・ファイル検索が容易<br>・小〜中規模では十分機能する | ・機能追加時に複数フォルダを横断して修正が必要<br>・ドメイン境界が見えない<br>・コンポーネント数が100を超えると破綻 | ・学習用・個人プロジェクト<br>・コンポーネント数が50以下の場合<br>・**本PJの本番構成としては不適** |

### パターンA の具体的なディレクトリ構成

```
frontend/
├── app/                          # Next.js App Router（ルーティングのみ）
│   ├── (shop)/                   # EC サイト画面グループ
│   │   ├── products/
│   │   │   ├── page.tsx          # 商品一覧ページ
│   │   │   └── [id]/
│   │   │       └── page.tsx      # 商品詳細ページ
│   │   ├── cart/
│   │   │   └── page.tsx
│   │   └── orders/
│   │       └── page.tsx
│   ├── (admin)/                  # 管理画面グループ
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   └── inventory/
│   │       └── page.tsx
│   ├── layout.tsx
│   └── globals.css
│
├── features/                     # ビジネス機能ごとに自己完結
│   ├── product/
│   │   ├── components/           # 商品関連の UI コンポーネント
│   │   ├── hooks/                # 商品関連の カスタムフック
│   │   ├── api/                  # 商品 API 呼び出し（TanStack Query）
│   │   ├── types/                # 商品の型定義
│   │   └── utils/                # 商品ドメイン固有のユーティリティ
│   ├── cart/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api/
│   │   ├── store/                # カート状態（Zustand）
│   │   └── types/
│   ├── order/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api/
│   │   └── types/
│   └── inventory/
│       ├── components/
│       ├── hooks/
│       ├── api/
│       └── types/
│
├── components/                   # 共通 UI コンポーネント（Atomic Design 的）
│   ├── ui/                       # Button, Input, Modal 等の基本パーツ
│   ├── layout/                   # Header, Footer, Sidebar
│   └── feedback/                 # Toast, Loading, ErrorBoundary
│
├── lib/                          # 技術基盤（FW / ライブラリのラッパー）
│   ├── api-client.ts             # fetch / axios の共通設定
│   ├── auth.ts                   # Keycloak 連携
│   └── i18n.ts                   # 多言語対応
│
├── hooks/                        # ドメイン横断の共通フック
│   ├── use-auth.ts
│   └── use-currency.ts
│
├── types/                        # グローバル型定義
│   └── api.d.ts
│
├── public/                       # 静的ファイル
├── next.config.ts
├── tsconfig.json
├── package.json
└── pnpm-lock.yaml
```

---

## 5. モバイル — 技術選定パターンの比較

「モバイルアプリをどの技術で作るか」。開発コスト・UX 品質・人材確保のバランスで判断する。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **PWA（Progressive Web App）** | Web 技術（Next.js 等）をそのまま使い、スマホのホーム画面に追加可能にする。ネイティブアプリは作らない | ・追加開発コストがほぼゼロ（Web をそのまま使う）<br>・FE チームだけで完結<br>・アプリストア審査不要・即時デプロイ | ・プッシュ通知の制限（iOS Safari で制約あり）<br>・カメラ・GPS 等のネイティブ機能が限定的<br>・「アプリストアにある」ことによる信頼感がない | ・EC サイトでネイティブ機能が不要なら十分<br>・モバイルの予算が限られる場合 |
| **React Native** | JavaScript / TypeScript + React でiOS / Android共通のネイティブアプリを開発 | ・Web（Next.js）と言語・思想を共有できる<br>・FE チームのスキルを転用可能<br>・ホットリロードで開発体験が良い<br>・New Architecture（Fabric / TurboModules）で性能向上 | ・ネイティブモジュールの互換性問題がまれに発生<br>・iOS / Android 固有の UI 差異を個別対応する場面あり<br>・大規模アニメーション・3D は不得意 | ・TypeScript / React を既に採用しており共通化したい<br>・EC アプリの標準的な機能（一覧・詳細・カート・決済）が主体 |
| **Flutter** | Dart 言語でiOS / Android / Web 共通のUIを描画。独自レンダリングエンジン（Skia / Impeller） | ・UI の再現度が高い（ピクセル単位の制御）<br>・パフォーマンスが安定（ネイティブ描画エンジン）<br>・Material / Cupertino ウィジェットが豊富<br>・ホットリロードが高速 | ・Dart を新たに学ぶコストが発生<br>・Web（Next.js）との技術スタック共有ができない<br>・日本での Dart 人材が React Native より少ない | ・モバイル UX の品質を最優先する場合<br>・モバイル専任チームが確保できる場合<br>・Web とモバイルを別チームで開発するなら選択肢 |
| **Kotlin Multiplatform（KMP）** | Kotlin でビジネスロジックを共通化し、UIは各プラットフォームのネイティブで書く。Android は Jetpack Compose、iOS は SwiftUI | ・ビジネスロジック（API通信・バリデーション等）の iOS/Android 共通化<br>・UI はネイティブのまま（最高のUX）<br>・Google の公式サポートあり<br>・バックエンド（Spring Boot / Java）と言語が近い | ・Compose Multiplatform（UI共有）はまだ成熟途上<br>・iOS 側で Kotlin ビルド体制を整える必要あり<br>・Web（Next.js）との型共有は OpenAPI 経由になる | ・**本PJ推奨**（Kotlin 確定・BE と言語ファミリーが同じ）<br>・Android は Jetpack Compose で最高の UX<br>・iOS もビジネスロジックを Kotlin で共通化 |
| **Kotlin Android + Swift iOS（完全ネイティブ）** | iOS は Swift、Android は Kotlin でそれぞれ個別に開発。ロジック共通化なし | ・各プラットフォーム最高の UX・パフォーマンス<br>・OS 最新機能をすぐ活用できる<br>・Apple / Google の公式サポートが最も手厚い | ・iOS / Android で2倍の開発工数<br>・2つの専任チームが必要（人件費大）<br>・ビジネスロジックの重複管理 | ・iOS / Android で大きく異なる体験を提供する場合<br>・KMP のビルド環境構築を避けたい場合<br>・人員が十分にいる場合 |
| **Ionic / Capacitor** | Web 技術（HTML/CSS/JS）をネイティブアプリの WebView で動かす。Capacitor でネイティブ API にアクセス | ・Web 開発者がそのまま書ける<br>・1つのコードベースで Web / iOS / Android 対応<br>・Capacitor でカメラ・GPS 等も利用可能 | ・WebView 描画なのでパフォーマンスがネイティブより劣る<br>・複雑なアニメーション・ジェスチャーが苦手<br>・ネイティブ感が薄い（ユーザーに伝わる） | ・社内向けアプリや補助的なモバイルアプリ<br>・パフォーマンス要件が低い場合<br>・**Kotlin 確定なので本PJでは対象外** |

---

## 6. モバイル — アプリアーキテクチャパターンの比較

「モバイルアプリ内のコード構造をどうするか」。選択した技術に応じて最適なパターンが変わる。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **MVVM + Jetpack Compose** | View（Compose UI）↔ ViewModel（StateFlow）↔ Repository の3層。Google 公式推奨 | ・**Google の Android 公式推奨パターン**<br>・Jetpack Compose との親和性が最高<br>・StateFlow / LiveData で UI が自動更新<br>・ViewModel 単体テストが容易 | ・ViewModel が肥大化しやすい（機能を詰め込みがち）<br>・画面ごとの ViewModel が増える<br>・Repository 層の設計に規律が必要 | ・**本PJ推奨**（Kotlin + Compose の王道構成）<br>・Google のサンプル・ドキュメントが豊富<br>・学習リソースが充実 |
| **MVI + Jetpack Compose** | Intent（ユーザー操作）→ Reducer → State → View の単方向サイクル。Redux の思想に近い | ・状態が完全に予測可能（1つの State オブジェクト）<br>・デバッグしやすい（状態遷移がログで追える）<br>・複雑な状態遷移をきれいに管理 | ・学習コストが MVVM より高い<br>・単純な画面にはボイラープレートが多い<br>・ライブラリの選択が必要（Orbit / Redux-Kotlin 等） | ・注文フローのように状態遷移が多い画面に部分適用<br>・MVVM をベースにしつつ複雑な画面のみ MVI にする混合も可 |
| **Clean Architecture + MVVM** | Presentation → Domain → Data の依存方向。バックエンドのヘキサゴナルと同様の思想 | ・バックエンド（ヘキサゴナル）と思想が統一<br>・ドメインロジックが技術非依存<br>・UseCase 単位のテストが容易 | ・層が深くなりファイル数が多い<br>・小規模機能でもボイラープレートが発生<br>・チーム全員に設計理解が必要 | ・ビジネスロジックがモバイル側にも多い場合<br>・オフライン同期・複雑な計算がある場合<br>・**25億規模なら投資する価値はある** |
| **Feature-based + MVVM** | `feature/product/`、`feature/order/` のように機能単位で分割。各 Feature 内で MVVM | ・ドメイン境界が明確（バックエンドの分割と対応）<br>・チーム分担がしやすい（Feature 単位で担当）<br>・バックエンドの Feature-based と統一感 | ・Feature 間の共通処理の配置ルールが必要<br>・Feature の粒度を揃える規律が必要 | ・**大規模チーム向けの実用的な構成**<br>・Web の Feature-based と対応させたい場合<br>・**MVVM と組み合わせて本PJに推奨** |
| **BLoC（Flutter 向け）** | Flutter 公式推奨パターン。Stream ベースで状態管理 | ・状態の予測可能性が高い<br>・テストが容易 | ・Dart 言語前提<br>・Kotlin では使えない | ・**Kotlin 確定なので本PJでは対象外** |

---

## 7. モバイル — コード共有戦略の比較

「Web とモバイルでどこまでコードを共有するか」。共有範囲を正しく決めることが開発効率に直結する。

| パターン | 特徴 | メリット | デメリット | 判断基準 |
|---|---|---|---|---|
| **OpenAPI 自動生成（リポジトリ分離）** | `openapi.yml` から Web（TypeScript）とモバイル（Kotlin）がそれぞれの言語でクライアントコードを自動生成 | ・**言語が異なっても API 仕様を1つの YAML で統一管理**<br>・各チームが自分の言語に最適な生成ツールを選べる<br>・リポジトリが独立しておりチーム間の git 競合がない | ・生成タイミングのズレで一時的に不一致が起きうる<br>・バリデーションルールは各言語で個別に書く必要がある<br>・CI で自動生成 + 整合性チェックの仕組みを入れるべき | ・**本PJ推奨**（Web=TypeScript / Mobile=Kotlin で言語が異なるため）<br>・OpenAPI 定義ファーストの利点を最大化<br>・Kotlin には openapi-generator の Kotlin クライアントあり |
| **Monorepo + KMP 共有層** | Kotlin Multiplatform でビジネスロジック・API クライアント・バリデーションを共通化。Web は別リポジトリ | ・モバイル内（Android / iOS）でロジック共通化<br>・API レスポンスのパース・バリデーションが1箇所<br>・Ktor Client でネットワーク層も共通化可能 | ・KMP のビルド環境構築が必要<br>・Web（TypeScript）とは共有できない<br>・iOS 開発者に Kotlin ビルドの理解が必要 | ・iOS と Android の両方を Kotlin で開発する場合<br>・**モバイル内の共通化として検討価値あり** |
| **完全分離（共有なし）** | Web とモバイルは完全に独立。API仕様書（PDF / Confluence）で繋がるのみ | ・各チームが好きな技術を選べる<br>・相互依存ゼロ | ・API仕様のドリフト（ズレ）が必ず起きる<br>・型定義・バリデーションの二重メンテナンス<br>・結合テストでのバグ率が高い | ・**原則として推奨しない**<br>・最低でも OpenAPI で仕様統一すべき |

---

## 8. 総合推奨構成（本PJ向け）

```
┌──────────────────────────────────────────────────────────┐
│                  海外販売システム 推奨構成                 │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  【フロントエンド（Web）】                                │
│    レンダリング: Next.js App Router + RSC + ISR           │
│    コンポーネント設計: Feature-based（Bulletproof 参考）   │
│    状態管理: TanStack Query + Zustand（カート用）         │
│    ディレクトリ: パターンA（app/ + features/ 分離）       │
│                                                          │
│  【モバイル】★ Kotlin 確定                                │
│    技術: Kotlin（Android: Jetpack Compose）               │
│          iOS: KMP でロジック共通化 + SwiftUI              │
│    アーキテクチャ: Feature-based + MVVM                   │
│    状態管理: StateFlow + ViewModel                        │
│    DI: Hilt（Android）/ Koin（KMP 共通）                  │
│                                                          │
│  【Web ↔ モバイル の繋ぎ】                                │
│    openapi.yml を単一の信頼源（Single Source of Truth）   │
│    Web: openapi.yml → TypeScript 型自動生成               │
│    Mobile: openapi.yml → Kotlin クライアント自動生成      │
│    リポジトリは分離、API 仕様で統一                       │
│                                                          │
│  【バックエンド】（既存方針）                              │
│    Spring Boot + ヘキサゴナル + 簡易CQRS                  │
│    OpenAPI 定義ファースト                                 │
│    ※ BE(Java) と Mobile(Kotlin) は JVM ファミリーで      │
│      ドメイン知識の転用がしやすい                         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 段階的導入のロードマップ

| フェーズ | やること | 判断ポイント |
|---|---|---|
| **Phase 1** | Web（Next.js App Router）+ Android（Kotlin + Jetpack Compose） | まず2プラットフォームで MVP を構築 |
| **Phase 2** | KMP で共通ロジック層を抽出 | iOS 対応が必要になったタイミングで |
| **Phase 3** | iOS アプリ（KMP + SwiftUI）を追加 | ビジネスロジックは Kotlin 共通、UI は SwiftUI |

---

## 9. 次のアクション

- [ ] Phase 1: Next.js App Router + Feature-based のプロジェクト初期構築
- [ ] Android プロジェクト初期構築（Kotlin + Jetpack Compose + MVVM）
- [ ] OpenAPI → TypeScript 型自動生成のパイプライン構築
- [ ] OpenAPI → Kotlin クライアント自動生成のパイプライン構築
- [ ] TanStack Query + Zustand のサンプル実装（Web）
- [ ] StateFlow + ViewModel のサンプル実装（Android）
- [ ] KMP 導入タイミングの決定（iOS 対応の要否）
