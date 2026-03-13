# コンテナ開発体験デモ

> **目的**: コンテナを使った開発がどういうものか、実際に手を動かして体験する  
> **所要時間**: 約15分  
> **前提**: Docker Desktop がインストール済み

---

## 0. このデモで体験すること

```
【ストーリー】
あなたは海外販売システムの「商品API」を開発する担当者です。
PostgreSQLに商品データを保存し、REST APIで取得する機能を作ります。

→ コンテナなしだと？  → DBインストールに30分〜数時間、バージョン違いで動かない
→ コンテナありだと？  → docker compose up → 即開発開始
```

---

## 1. ファイル構成

```
docker/demo/
├── docker-compose.yml          ← DBコンテナ定義
├── init-db/
│   └── 01_create_table.sql     ← 初期データ自動投入
├── src/
│   └── main/
│       ├── java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   ├── Product.java
│       │   ├── ProductRepository.java
│       │   └── ProductController.java
│       └── resources/
│           └── application.yml
├── build.gradle
└── Dockerfile                  ← バックエンドのコンテナ化
```

---

## 2. 開発手順（ステップバイステップ）

### Step 1: コンテナを起動する（DBだけ版 = 案B体験）

```powershell
cd c:\JPIN\hoge\docker\demo
docker compose up db -d
```

**何が起きるか:**
- PostgreSQL 16 がコンテナとして起動
- `01_create_table.sql` が自動実行され、テーブルとサンプルデータが作成される
- ローカルPCにPostgreSQLをインストールする必要なし！

### Step 2: DBにデータが入っていることを確認

```powershell
docker compose exec db psql -U demo_user -d demo_db -c "SELECT * FROM products;"
```

### Step 3: バックエンドをローカルで起動

```powershell
cd c:\JPIN\hoge\docker\demo
.\gradlew bootRun
```

### Step 4: APIを叩いてみる

```powershell
# 商品一覧取得
curl http://localhost:8080/api/products

# 商品追加
curl -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d "{\"name\":\"Standing Desk\",\"price\":49900,\"category\":\"Furniture\"}"

# 再度一覧取得 → 追加されている
curl http://localhost:8080/api/products
```

### Step 5: 全部コンテナで起動する（案D体験）

先ほどのローカルSpring Bootを停止して：

```powershell
docker compose up -d
```

**何が起きるか:**
- PostgreSQL + Spring Boot の両方がコンテナで起動
- ローカルにJavaすらインストールしていなくてもAPIが動く

### Step 6: そのまま動くことを確認

```powershell
curl http://localhost:8080/api/products
```

### Step 7: 後片付け

```powershell
docker compose down -v
```
`-v` でボリュームも削除 → 環境が完全にクリーンに戻る。

---

## 3. 「だから何が嬉しいの？」まとめ

| 体験したこと | コンテナなしだと… | コンテナありだと… |
|---|---|---|
| Step 1 | Oracle/PostgreSQLのインストール手順書を読んで30分〜数時間格闘 | `docker compose up` 1コマンドで完了 |
| Step 2 | 「テーブルが作成されてません」「文字コードが違います」 | SQLが自動実行され、毎回同じ状態で起動 |
| Step 5 | 新メンバー「Java 11入ってました…17じゃないとビルドできない…」 | Dockerfileでバージョン固定、全員同じ環境 |
| Step 7 | アンインストールが不完全で残骸が… | `docker compose down -v` で完全消去 |
