# Docker 入門ガイド + アーキテクチャ議論まとめ

> **対象**: Docker を全く知らない状態からスタートするエンジニア  
> **背景**: 25億円規模・海外販売システム（小売業）のテックリード向け  
> **作成日**: 2026-03-13

---

## Part 0. これまでの議論まとめ（会話履歴）

### 議論した内容

1. **開発環境のコンテナ分割方針** → 現状DBだけ、今後どうするか
2. **技術スタック**: Java/Spring Boot + Oracle + Next.js/TypeScript
3. **アーキテクチャ**: ヘキサゴナル + 簡易CQRS
4. **コンテナ4案の比較**
5. **「コンテナって何？」という根本的な疑問の解消**

### 結論として決まったこと

| テーマ | 決定内容 |
|---|---|
| **コンテナ分割方針** | **案C推奨**: DB・Keycloak・Mock・Mailはコンテナ、FE/BEはローカル起動 |
| **アーキテクチャ** | ヘキサゴナルアーキテクチャ + 簡易CQRS（同一DB） |
| **コードの場所** | Windowsローカル（`c:\JPIN\hoge`）に置く。コンテナ内には入れない |
| **IDEデバッグ** | IntelliJ/VS Code から普通にデバッグ可能（ブレークポイント等） |

### 解消した誤解

```
❌ 誤解: コンテナの中で git clone してコードを書く
✅ 正解: コードはWindowsローカル。コンテナは「サーバーソフトが動く箱」

❌ 誤解: バックエンドもコンテナなら Linux で作業が必要
✅ 正解: ボリュームマウントで Windows のファイルをコンテナと共有できる
         コード編集は普通に Windows + IntelliJ で行う
```

---

## Part 1. Docker の基礎知識（ゼロから）

### 1-1. Docker とは何か？

一言で言うと：**「ソフトウェアを箱（コンテナ）に詰めて、どこでも同じように動かす仕組み」**

```
従来の問題:
  「私のPCでは動くのに、あなたのPCでは動かない」
  → OSのバージョン、インストール済みソフト、設定が人によって違うから

Dockerが解決すること:
  「この箱の中に動作環境ごと詰めてあるから、どこでも同じように動く」
```

### 1-2. よく使われる用語の整理

| 用語 | 意味 | 身近なたとえ |
|---|---|---|
| **イメージ** | コンテナの設計図・テンプレート | アプリのインストーラー（.exe） |
| **コンテナ** | イメージを実行した実体 | インストーラーを実行してできたアプリ |
| **Dockerfile** | イメージを作るための手順書 | 料理のレシピ |
| **docker compose** | 複数コンテナをまとめて管理するツール | 複数料理をまとめたコース料理の献立表 |
| **ボリューム** | コンテナの外にデータを保存する仕組み | 外付けHDD |
| **ポート** | コンテナへの入口番号 | ビルのフロア番号 |

### 1-3. `docker` と `docker compose` の違い（よくある混乱ポイント）

コマンドが似ているので混乱しやすいですが、**別のソフトウェア**です。

```powershell
docker --version         # Docker 本体（コンテナエンジン）のバージョン
docker compose version   # docker compose プラグインのバージョン
```

#### 歴史的な経緯

```
【昔】
  docker          → Docker社が作ったコンテナエンジン（コンテナを1個動かすもの）
  docker-compose  → 別途インストールが必要な外部ツール（Python製）
                    コマンドにハイフンあり: docker-compose up  ← ハイフン

【今（Docker Desktop v2以降）】
  docker          → 変わらずコンテナエンジン本体
  docker compose  → Docker本体に組み込まれたプラグイン（Go製に書き直し）
                    コマンドがスペース区切り: docker compose up  ← スペース
```

> **注意**: ネット上の古い記事には `docker-compose`（ハイフンあり）と書いてあるものがあります。  
> 現在は `docker compose`（スペース）が正しい書き方です。

#### 役割の違い

| | `docker` | `docker compose` |
|---|---|---|
| **役割** | コンテナ **1個** を操作するエンジン | **複数**のコンテナをまとめて管理するツール |
| **操作対象** | コンテナ・イメージ単体 | `docker-compose.yml` に書いた複数サービス |
| **コマンド例** | `docker run postgres` | `docker compose up` |
| **関係性** | 土台となるエンジン | 裏で `docker` を呼び出している |

#### `docker compose` は `docker` の便利な束ね役

```
docker compose up
        ↓ 内部では以下を自動実行している

docker network create demo_network
docker run --name db   -p 5432:5432 -e POSTGRES_DB=... postgres:16
docker run --name backend -p 8080:8080 --network demo_network ...
       ↑
  これを毎回手打ちするのは大変 → docker-compose.yml に書いて1コマンドで済ませる
```

---

### 1-4. コンテナとVMの違い

```
仮想マシン（VM）:
  ┌─────────────────────────┐
  │ Guest OS (Linux全体)    │ ← OS全体を仮想化（重い・数分かかる）
  │   └── アプリ            │
  └─────────────────────────┘
  Host OS (Windows)

コンテナ:
  ┌───────────────┐
  │   アプリ      │ ← アプリと最低限の依存関係だけ（軽い・数秒で起動）
  └───────────────┘
  Docker Engine（Host OS のカーネルを共有）
```

---

## Part 2. Docker の操作を覚える（ステップバイステップ）

### Step 1: Docker Desktop インストール確認

```powershell
# バージョン確認（これが動けばOK）
docker --version
docker compose version
```

期待する出力例：
```
Docker version 26.x.x, build xxxxxxx
Docker Compose version v2.x.x
```

---

### Step 2: 初めてのコンテナ起動

まず超シンプルなコンテナを1つ動かしてみます。

```powershell
# "Hello World" コンテナを動かす
docker run hello-world
```

**何が起きるか：**
```
1. Docker Hub （インターネット上のイメージ置き場）から hello-world イメージをダウンロード
2. そのイメージからコンテナを起動
3. "Hello from Docker!" というメッセージを表示
4. コンテナが終了して消える
```

---

### Step 3: 動いているコンテナを確認する

```powershell
# 現在動いているコンテナ一覧
docker ps

# 停止中も含めた全コンテナ一覧
docker ps -a

# ダウンロード済みのイメージ一覧
docker images
```

---

### Step 4: データベースコンテナを1つ起動してみる

```powershell
# PostgreSQL を起動（デモプロジェクトと同じもの）
docker run -d `
  --name my-first-db `
  -e POSTGRES_DB=testdb `
  -e POSTGRES_USER=testuser `
  -e POSTGRES_PASSWORD=testpass `
  -p 5432:5432 `
  postgres:16-alpine
```

**オプションの意味：**

| オプション | 意味 |
|---|---|
| `-d` | バックグラウンドで起動（detached mode） |
| `--name my-first-db` | コンテナに名前をつける |
| `-e POSTGRES_DB=testdb` | 環境変数を渡す（DBの初期設定） |
| `-p 5432:5432` | `ホストのポート:コンテナのポート` を繋げる |
| `postgres:16-alpine` | 使うイメージ名（PostgreSQL 16、軽量版） |

---

### Step 5: 起動確認 & 接続

```powershell
# 起動確認
docker ps

# コンテナの中でSQL実行
docker exec -it my-first-db psql -U testuser -d testdb -c "\l"

# ログを見る
docker logs my-first-db

# コンテナを止める
docker stop my-first-db

# コンテナを削除する
docker rm my-first-db
```

**`-p 5432:5432` の意味を図で理解する：**

```
あなたのPC（Windows）            コンテナ内
                                 ┌──────────────────┐
localhost:5432  ────────────────▶│ PostgreSQL :5432  │
                                 └──────────────────┘
                ↑
  WindowsのPCからはここに接続するだけ
  「コンテナの中にいる」意識は不要
```

---

### Step 6: docker compose を使う（複数コンテナ管理）

毎回 `docker run` に長いオプションを書くのは大変です。  
`docker-compose.yml` にまとめて書けば、1コマンドで複数のコンテナを管理できます。

```yaml
# docker-compose.yml の例（シンプル版）
services:
  db:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: testuser
      POSTGRES_PASSWORD: testpass
```

```powershell
# 起動（-d でバックグラウンド）
docker compose up -d

# 状態確認
docker compose ps

# ログ確認
docker compose logs db

# 停止（コンテナは残る）
docker compose stop

# 停止して削除
docker compose down

# 停止・削除 + ボリューム（DBのデータ）も全消去
docker compose down -v
```

---

### Step 7: ボリューム（データの永続化）を理解する

コンテナは**使い捨て**が基本です。削除するとデータも消えます。  
DBのデータを残したい場合は「ボリューム」を使います。

```yaml
services:
  db:
    image: postgres:16-alpine
    volumes:
      - db_data:/var/lib/postgresql/data  ← データをボリュームに保存

volumes:
  db_data:  ← ボリュームの定義
```

```
docker compose down     → コンテナは消えるが db_data ボリュームは残る → データが残る
docker compose down -v  → コンテナもボリュームも消える → データが消える（リセット）
```

---

### Step 8: このプロジェクトのデモを動かす

以上を踏まえて、実際に作ったデモを動かしてみましょう。

```powershell
# デモプロジェクトに移動
cd c:\JPIN\hoge\docker\demo

# ===== 【案B体験】DBコンテナだけ起動 =====
docker compose up db -d

# DBにサンプルデータが入っていることを確認
docker compose exec db psql -U demo_user -d demo_db -c "SELECT * FROM products;"

# Spring Boot をローカルで起動（別ターミナルで）
.\gradlew bootRun

# APIを叩く
curl http://localhost:8080/api/products

# ===== 【案D体験】全部コンテナで起動 =====
# Spring Boot を停止してから:
docker compose up -d

# 同じAPIが動くことを確認（Javaがローカルになくても動く）
curl http://localhost:8080/api/products

# ===== 後片付け =====
docker compose down -v
```

---

## Part 3. このプロジェクトでの構成（本番環境設計）

### 案C（推奨）の日常的な使い方

```powershell
# 朝の開始
cd c:\JPIN\hoge\docker
docker compose up db keycloak mock-server mailhog -d

# IntelliJ で Spring Boot 起動 → localhost:8080
# VS Code で Next.js 起動     → localhost:3000

# 退勤時
docker compose stop

# 環境を完全リセットしたいとき
docker compose down -v
docker compose up db keycloak mock-server mailhog -d
```

### コンテナとローカルの責務分担

```
┌────────────────────────────────────────────────────┐
│  Windows ローカル                                   │
│                                                    │
│  ┌──────────────┐  ┌──────────────┐              │
│  │ IntelliJ     │  │ VS Code      │              │
│  │ Spring Boot  │  │ Next.js      │              │
│  │ :8080        │  │ :3000        │              │
│  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                       │
│  ┌──────▼─────────────────▼──────────────────┐   │
│  │          Docker Desktop                    │   │
│  │  ┌──────┐ ┌──────────┐ ┌──────┐ ┌──────┐ │   │
│  │  │  DB  │ │Keycloak  │ │Mock  │ │Mail  │ │   │
│  │  │:1521 │ │  :8180   │ │:9090 │ │:8025 │ │   │
│  │  └──────┘ └──────────┘ └──────┘ └──────┘ │   │
│  └────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────┘
```

### 各コンテナの役割

| コンテナ | 役割 | なぜコンテナか |
|---|---|---|
| **db** (Oracle) | データ永続化 | インストールが複雑、バージョン管理が必要 |
| **keycloak** | 認証・認可（ログイン画面、JWT発行） | 設定をRealm JSONでコード管理できる |
| **mock-server** | 外部API（決済・物流）の模倣 | 外部システム未完成でも開発着手できる |
| **mailhog** | メール送信の確認 | 誤って本物のメールを送らずに済む |

---

## Part 4. よく使うコマンド チートシート

```powershell
# ===== イメージ操作 =====
docker images                      # イメージ一覧
docker pull postgres:16-alpine     # イメージをダウンロード
docker rmi postgres:16-alpine      # イメージを削除

# ===== コンテナ操作（単体）=====
docker ps                          # 起動中コンテナ一覧
docker ps -a                       # 全コンテナ一覧（停止含む）
docker stop <名前>                 # 停止
docker start <名前>                # 起動
docker rm <名前>                   # 削除（停止済みのもの）
docker logs <名前>                 # ログ表示
docker logs -f <名前>              # ログをリアルタイム表示
docker exec -it <名前> bash        # コンテナの中に入る
docker inspect <名前>              # コンテナの詳細情報

# ===== docker compose 操作 =====
docker compose up -d               # 全サービス起動（バックグラウンド）
docker compose up db -d            # db サービスのみ起動
docker compose ps                  # サービス一覧・状態確認
docker compose logs                # 全サービスのログ
docker compose logs -f backend     # backend のログをリアルタイム
docker compose stop                # 全サービス停止（データ残る）
docker compose down                # 停止 + コンテナ削除（データ残る）
docker compose down -v             # 停止 + コンテナ + ボリューム削除（データ消える）
docker compose restart backend     # backend だけ再起動
docker compose exec db bash        # db コンテナの中に入る

# ===== 掃除 =====
docker system prune                # 不要なコンテナ・イメージを一括削除
docker volume prune                # 未使用ボリュームを削除
```

---

## Part 5. トラブルシューティング

### よくあるエラーと対処

| エラー | 原因 | 対処 |
|---|---|---|
| `port is already allocated` | そのポートを他のプロセスが使っている | `docker compose ps` で起動中のコンテナを確認。または `netstat -ano \| findstr :1521` |
| `Cannot connect to Docker daemon` | Docker Desktop が起動していない | タスクトレイから Docker Desktop を起動 |
| コンテナが `unhealthy` になる | ヘルスチェックが失敗（DB起動に時間がかかっている） | しばらく待つ。`docker compose logs db` でエラー確認 |
| DBのデータがリセットされた | `docker compose down -v` を実行してしまった | `-v` なしで `docker compose down` を使う |
| コンテナ内でファイルが見つからない | ボリュームマウントのパス間違い | `docker compose exec backend ls /app` でコンテナ内を確認 |

### ログを見るクセをつける

```powershell
# 問題が起きたらまずこれ
docker compose logs --tail=50       # 最後50行
docker compose logs -f              # リアルタイム表示
docker compose logs db 2>&1         # エラー出力も含めて表示
```

---

## 参考: ファイル構成（このプロジェクト全体）

```
c:\JPIN\hoge\
├── src/                            ← Spring Boot ソースコード（ここを編集）
├── build.gradle                    ← 依存関係定義
├── docker/
│   ├── docker-compose.yml          ← 本番用コンテナ構成（6コンテナ）
│   ├── keycloak/
│   │   └── realm-export.json       ← Keycloak設定（自動インポート）
│   ├── wiremock/
│   │   └── mappings/               ← 外部APIのモック定義
│   ├── init-scripts/
│   │   └── oracle/01_init.sql      ← Oracle初期化SQL
│   └── demo/                       ← Docker学習用デモプロジェクト
│       ├── docker-compose.yml      ← デモ用（PostgreSQL + Spring Boot）
│       ├── Dockerfile
│       ├── build.gradle
│       ├── init-db/
│       │   └── 01_create_table.sql
│       ├── src/main/java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   ├── Product.java
│       │   ├── ProductRepository.java
│       │   └── ProductController.java
│       └── README.md               ← デモの手順書
└── docs/
    ├── architecture-decision.md    ← アーキテクチャ方針書（リーダー提出用）
    └── docker-learning.md          ← このファイル
```
