# コンテナ戦略 — 役割・メリデメ・チーム構成

> **対象**: 開発チームリード・テックリード  
> **作成日**: 2026-03-13

---

## 1. コンテナ別 メリット・デメリット・判断マトリクス

| コンテナ | 役割（何をするものか） | コンテナ化のメリット | コンテナ化のデメリット（詳細は下記） | **判断** |
|---|---|---|---|---|
| **frontend** (Next.js :3000) | ユーザーが見て操作する**画面**を表示する<br>例）商品一覧・カート・注文画面 | 全員が同じNode.jsバージョンで動く | ①保存してから画面に反映されるまで3〜10秒かかる<br>②ESLint（コード品質チェック）や補完が動かなくなる | ⚠️ **原則ローカル推奨**。FE開発者の生産性優先 |
| **backend** (Spring Boot :8080) | 画面からの要求を受け取り、**ビジネスの処理**をしてDBに保存する<br>例）注文確定・在庫チェック・価格計算 | Javaのバージョンを全員で統一できる | ①ブレークポイントで止めるのに1回だけ設定が必要（2分）<br>②コード変更の反映が数秒遅れる | ⚠️ **原則ローカル推奨**。BE開発者の生産性優先 |
| **db** (Oracle :1521) | 商品・注文・顧客などの**データをディスクに保存**する<br>アプリを止めてもデータが消えない | インストール不要・全員同じOracle版・テーブル作成SQLの自動実行 | イメージが大きい（~1.5GB）・初回起動に2分かかる | ✅ **必須コンテナ化**。手動インストールが最も困難 |
| **keycloak** (Keycloak :8180) | **ログイン画面**を提供し、「誰が何をできるか」を管理する<br>例）管理者は商品を削除できる / 一般ユーザーは閲覧のみ | ログイン設定をファイルでgit管理できる→全員同じ認証環境 | DBが起動してからでないと使えない・設定変更時に独自の知識が必要 | ✅ **必須コンテナ化**。認証なしでは本格開発不可 |
| **mock-server** (WireMock :9090) | **外部の会社のAPIの代役**を務める<br>例）決済会社・物流会社・ERPとAPIで繋がるが、開発中は本物を呼べない | 外部サービスが未契約・未完成でも開発を進められる・わざとエラーを返すテストも自在 | 外部APIの仕様が変わったときに定義ファイルの更新が必要 | ✅ **必須コンテナ化**。外部依存をゼロにできる |
| **mailhog** (MailHog :1025/8025) | バックエンドが送った**メールをローカルで受け取って確認**する<br>例）注文完了メール・パスワードリセットメールの文面確認 | 本物のメールサーバー不要・誤って顧客にメール送付なし・ブラウザで内容確認 | あくまでテスト確認用（本番のメールサーバーとは別物） | ✅ **必須コンテナ化**。誤送信リスクの排除は必須 |

### デメリット詳細

#### ① ホットリロードが遅くなる（frontend / backend をコンテナ化した場合）

Windows 固有の問題です。Windows のファイルシステム（NTFS）と Docker の Linux コンテナ間でファイルを同期するのに時間がかかります。

```
ローカル起動:    保存 → 0.5秒でブラウザ反映
コンテナ内起動:  保存 → 3〜10秒かかることがある（Windows の場合）

※ Mac / Linux ではほぼ差がない
※ Windows で開発する場合に特に影響が大きい
```

#### ② VS Code・ESLint・補完が効かなくなる（frontend をコンテナ化した場合）

Next.js をコンテナ内で動かすと `node_modules` がコンテナの中に入ります。VS Code の拡張機能はローカルを探すので見つけられなくなります。

```
ローカル起動の場合:
  c:\JPIN\hoge\frontend\node_modules\  ← ここにある
  → ESLint・Prettier・TypeScript補完が普通に動く ✅

コンテナ内で動かした場合:
  コンテナ内の /app/node_modules/ にある
  → VS Code はローカルの node_modules を探すが見つからない
  → 赤い波線が出ない / 補完が効かない / フォーマットが動かない ❌
```

#### ③ ブレークポイントに1回だけ設定が必要（backend をコンテナ化した場合）

面倒ではありません。**2分・1回限りの設定**です。以降は普通にブレークポイントが止まります。

```jsonc
// .vscode/launch.json に追加するだけ（コピペでOK）
{
  "type": "java",
  "name": "Attach to Container",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

設定後は「▶ Attach to Container」を選んで実行するだけ。ブレークポイントの使い方は完全に同じ。

---

### mock-server が模倣する対象の整理

```
【自分たちが開発するAPI】→ モックしない
  Next.js  ──→  Spring Boot（localhost:8080）
                ↑ これは自分たちが作っている本物

【mock-server（WireMock）が模倣するもの】→ 外部サービスのAPI
  Spring Boot  ──→  決済会社のAPI（Stripe / PayPal 等）  ← 外部
  Spring Boot  ──→  物流会社のAPI（DHL / FedEx 等）      ← 外部
  Spring Boot  ──→  ERPシステムのAPI                     ← 外部
                    ↑
                    まだ契約していない・開発中・本番環境しかない
                    → WireMock で「偽物のレスポンス」を返す箱を用意する
```

**自分たちのREST API（`/api/products` 等）は mock-server とは無関係です。**

### 判断基準

```
コンテナ化すべきもの（ミドルウェア）:
  ✅ 開発者全員が同じバージョン・設定で使う必要があるもの
  ✅ インストールが複雑・時間がかかるもの
  ✅ データや設定をコードで管理（gitで共有）したいもの
  → db / keycloak / mock-server / mailhog

ローカルで動かすもの（アプリケーション）:
  ✅ IDEのデバッグ・ホットリロードをフル活用したいもの
  ✅ コード変更の反映速度が生産性に直結するもの
  → frontend / backend
```

---

## 2. チーム別の接続関係

```
┌─────────────────────────────────────────────────────────────┐
│  Windows ローカル（各開発者）                                │
│                                                             │
│  FE開発者              BE開発者              モバイル開発者  │
│  Next.js :3000  ──→  Spring Boot :8080  ←─  iOS/Android   │
│       │                    │                     │         │
│       └──────────┬─────────┘                     │         │
│                  ↓                               ↓         │
│  ┌───────────────────────────────────────────────────────┐ │
│  │                Docker Desktop（全員共通）              │ │
│  │  ┌────────┐  ┌──────────┐  ┌──────┐  ┌──────────┐  │ │
│  │  │   db   │  │keycloak  │  │mock  │  │ mailhog  │  │ │
│  │  │ :1521  │  │  :8180   │  │:9090 │  │:1025/8025│  │ │
│  │  └────────┘  └──────────┘  └──────┘  └──────────┘  │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. FE / BE で開発者が分かれる場合

### 問題: 「相手チームのサーバーが起動していないと開発できない」

**→ mock-server がこれを解決する**

#### BEが未実装 / BE開発者がいない日（FE側の作業）

```powershell
# FE開発者の手元
docker compose up db keycloak mock-server mailhog -d
npm run dev  # Next.js 起動

# .env.local で接続先を mock-server に向ける
NEXT_PUBLIC_API_URL=http://localhost:9090
```

- BEなしで画面の開発・表示確認ができる
- モックデータでUIを作り込める

#### FEが未実装 / FE開発者がいない日（BE側の作業）

```powershell
# BE開発者の手元
docker compose up db keycloak mock-server mailhog -d
./gradlew bootRun  # Spring Boot 起動

# curl や Postman で API を直接テスト
curl http://localhost:8080/api/products
```

- FEなしでAPIの開発・テストができる

#### BEの実装が完了したら

```bash
# .env.local の接続先を backend に切り替えるだけ
NEXT_PUBLIC_API_URL=http://localhost:8080
```

接続先の環境変数を1行変えるだけで、モック→本物に切替完了。

### FE/BE 並行開発のフロー

```
【開発初期】
  FE ──→ mock-server（:9090）   ← BEのAPI定義だけ先に決める
  BE ──→ 実装中

【BE実装完了後】
  FE ──→ backend（:8080）       ← .env.local の1行を変更するだけ
  BE ──→ 稼働中
```

---

## 4. モバイル（iOS / Android）の扱い

### 問題: エミュレータ・実機から `localhost` は届かない

```
PC上のブラウザ      → localhost:8080  ✅ 届く
Androidエミュレータ → localhost は「エミュレータ自身」を指す ❌
iOS実機             → localhost は「iPhoneのlocalhost」を指す ❌
```

### 解決策: PCのIPアドレスを使う

```powershell
# PCのIPアドレスを調べる
ipconfig
# → 例: 192.168.1.10  （WiFiのIPv4アドレス）
```

### 端末別の接続先一覧

| 端末 | BEへのアクセス先 | Keycloakへのアクセス先 |
|---|---|---|
| **PC上のブラウザ（FE開発）** | `http://localhost:8080` | `http://localhost:8180` |
| **Androidエミュレータ** | `http://10.0.2.2:8080`（固定の特殊IP） | `http://10.0.2.2:8180` |
| **iOSシミュレータ（Mac）** | `http://localhost:8080` | `http://localhost:8180` |
| **Android / iOS 実機（同一WiFi）** | `http://192.168.1.10:8080` | `http://192.168.1.10:8180` |

> **docker-compose.yml の変更は不要。**  
> ポートをPC外に公開する設定（`0.0.0.0`バインド）がデフォルトで有効です。

### モバイル開発者のdocker compose起動コマンド

モバイル開発者も docker compose は起動する必要があります（Keycloakに繋ぐため）。  
コマンドはFE/BE開発者と同じです。

```powershell
docker compose up db keycloak mock-server mailhog -d
```

---

## 5. 役割別 起動コマンドまとめ

### FE開発者

```powershell
# コンテナ起動
docker compose up db keycloak mock-server mailhog -d

# Next.js 起動
cd c:\JPIN\hoge\frontend
npm run dev
```

### BE開発者

```powershell
# コンテナ起動
docker compose up db keycloak mock-server mailhog -d

# Spring Boot 起動（VS Code のターミナルから）
cd c:\JPIN\hoge
./gradlew bootRun
```

### モバイル開発者

```powershell
# コンテナ起動（同じ）
docker compose up db keycloak mock-server mailhog -d

# エミュレータ or 実機から接続先を設定
# Android: http://10.0.2.2:8080
# 実機:    http://192.168.1.10:8080（各自のPCのIP）
```

### 全員共通: 退勤時・リセット

```powershell
# 退勤時（データを残して停止）
docker compose stop

# 環境ごとリセットしたいとき
docker compose down -v
docker compose up db keycloak mock-server mailhog -d
```
