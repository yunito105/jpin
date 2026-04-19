# モックアプローチ比較: WireMock vs Fixed Controller

> **前提**: 25億規模 海外販売システム / 小売業 / 社内ツール複数サブドメイン  
> Tech: Java + Spring Boot (Hexagonal + 簡易CQRS) / React + Vite + TypeScript

---

## まず「wiremock モード」って何？ → 実はただの env 切り替え

混乱しやすいポイントなので先に整理する。

### フロント側でやっていること（全部）

```
.env.development   → VITE_API_PORT=8080   （Spring Boot）
.env.wiremock      → VITE_API_PORT=9090   （WireMock Docker）
```

```ts
// vite.config.ts
server: {
  proxy: {
    '/api': { target: `http://localhost:${apiPort}` }  // ← ポートを env で切り替えるだけ
  }
}
```

```bash
npm run dev           # = vite --mode development  → port 8080 に向ける
npm run dev:wiremock  # = vite --mode wiremock     → port 9090 に向ける
```

**フロントのコードは一切変わらない。** `fetch('/api/v1/products')` はどちらのモードでも同じ。  
「wiremock モード」とは「.env.wiremock を読み込んで Vite を起動する」というだけ。

---

### なぜ直接 `http://localhost:9090/api/...` と書かないのか

ブラウザの **CORS 制約** のため。フロントが `localhost:5173` でバックが `localhost:9090` の場合、  
`Access-Control-Allow-Origin` ヘッダーがないとブラウザがリクエストをブロックする。

| 対策案 | 方法 | 採用 |
|--------|------|------|
| **Vite proxy**（今回の実装） | `/api` へのリクエストを Vite dev server が代理転送 → CORS 問題なし | ✅ |
| WireMock に CORS ヘッダーを付ける | mappings の `response.headers` に `Access-Control-Allow-Origin: *` を追加 | 可能だが管理が増える |
| env で `VITE_API_BASE_URL=http://localhost:9090` を設定 | fetch の URL をフルパスにする必要あり | 可能（CORS 設定も必要） |

Vite proxy なら「ポートだけ env で切り替え」で済むので、今回の実装はこの方式。

---

## Approach 1: WireMock (Docker)

### 構成図

```
ブラウザ(5173)
  └─[fetch /api/v1/products]─▶ Vite proxy ─▶ WireMock:9090
                                                └─ mappings/*.json が応答を返す
```

### やること

1. `docker compose up` で WireMock コンテナ起動（port 9090）
2. `npm run dev:wiremock` でフロント起動（Vite が 9090 に proxy）
3. レスポンスは `wiremock/mappings/*.json` に記述

### メリット

| # | 内容 |
|---|------|
| ✅ | **バックエンドがゼロでも開発できる** — Spring Boot すら不要 |
| ✅ | **異常系・境界値のシナリオが作りやすい** — 404 / 500 / 遅延レスポンスを JSON で定義できる |
| ✅ | **フロントとバックが完全並行開発できる** — API 仕様だけ合意すれば独立して進められる |
| ✅ | **E2E テストや負荷テストにも流用できる** — CI で WireMock コンテナを使いまわせる |

### デメリット

| # | 内容 |
|---|------|
| ❌ | **JSON ファイルと実装が乖離する** — バックエンドのコードを変えても WireMock の JSON は自動更新されない。二重管理が発生する |
| ❌ | **画面・サブドメインが増えるほど JSON ファイルが爆発する** — 100 画面 × 各シナリオ = 膨大な管理コスト |
| ❌ | **Docker が必要** — Docker Desktop の有無・バージョンに依存する |
| ❌ | **フロントエンジニアが「正しい JSON」を知る術がない** — バックの実装を参照できないとズレが生じる |
| ❌ | **フロントエンジニアはバックを意識できない** — ヘキサゴナルの構造や型安全性が見えない |

---

## Approach 2: Fixed Controller (Spring Boot)

### 構成図

```
ブラウザ(5173)
  └─[fetch /api/v1/products]─▶ Vite proxy ─▶ Spring Boot:8080
                                                └─ Controller（固定値を返す）
```

### やること

1. `./gradlew bootRun` で Spring Boot 起動（port 8080）
2. `npm run dev` でフロント起動（Vite が 8080 に proxy）
3. レスポンスは Controller の固定値が返す

### Controller の固定値実装

```java
@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private static final List<ProductResponse> STUB_PRODUCTS = List.of(
        new ProductResponse(1L, "商品A", 1000),
        new ProductResponse(2L, "商品B", 2000)
    );

    @GetMapping("/products")
    public List<ProductResponse> list() {
        return STUB_PRODUCTS;  // ← 本番化時にここを Service 呼び出しに置き換える
    }
}
```

**Service 層は本番実装に移行するタイミングで追加すればよい。**  
スタブ期間中は Controller に固定値を書くだけで十分。

### メリット

| # | 内容 |
|---|------|
| ✅ | **「実際のコード」がAPI仕様書になる** — フロントエンジニアが Controller の型・パスを直接確認できる |
| ✅ | **管理ファイルが増えない** — JSON ファイルの二重管理不要。コードだけで完結 |
| ✅ | **本番実装への移行コストがゼロに近い** — スタブ Service を DB 実装に差し替えるだけ |
| ✅ | **フロントエンジニアがバックの構造を自然に意識できる** — ヘキサゴナルの層分けが見える |
| ✅ | **追加ツール不要** — Docker 不要。Java 環境があれば動く |
| ✅ | **型安全** — DTO レコードが「返せる型」を保証。JSON の書き間違いが起きない |

### デメリット

| # | 内容 |
|---|------|
| ❌ | **JVM 起動が遅い** — 初回起動に 5〜10 秒かかる（WireMock はほぼ瞬時） |
| ❌ | **最低限のコードが必要** — バックエンドエンジニアが Controller + Service を先に書く必要がある |
| ❌ | **異常系シナリオの表現が少し手間** — 404 や 500 を返したい場合はコードで条件分岐が必要 |

---

## 比較サマリー

| 観点 | WireMock | Fixed Controller |
|------|----------|------------------|
| バックが不要か | ✅ 不要 | ❌ Spring Boot 必要 |
| API仕様との整合性 | ❌ ズレやすい | ✅ コードが仕様書 |
| 管理コスト（画面数 ✕） | ❌ JSON が爆発 | ✅ コードだけ |
| 本番実装への移行 | ❌ WireMock を捨てて書き直し | ✅ スタブを差し替えるだけ |
| 異常系シナリオ | ✅ JSON で簡単 | △ コード変更が必要 |
| フロント開発の独立性 | ✅ 完全独立 | △ Spring Boot 起動が必要 |
| フロントのコード変更 | なし（env 切り替えのみ） | なし（env 切り替えのみ） |
| Docker 必要 | ✅ 必要 | ❌ 不要 |

---

## 判断基準

### Fixed Controller を選ぶべき状況

- **バックエンドエンジニアが先行して Controller を書ける体制**
- **画面数・サブドメインが多い**（本プロジェクトはここ）→ JSON 管理が破綻しない
- **フロントエンジニアにバックの構造を理解させたい**
- **ヘキサゴナルアーキテクチャが採用済み** → スタブ差し替えが自然にできる
- **本番実装までのリードタイムを短くしたい**

### WireMock を選ぶべき状況

- **バックエンドが存在しない初期フェーズ**（MVP 検証など）
- **フロントとバックが完全に別チーム・別スプリントで動く**
- **E2E テスト・契約テスト（Consumer-Driven Contract Testing）を整備したい**
- **異常系シナリオを宣言的（JSON）に管理したい** — Fixed Controller でも `Thread.sleep` や `ResponseStatusException` で遅延・エラーは再現できるが、スタブコードが本番コードに混入する

---

## 本プロジェクトへの推奨

**→ Approach 2 (Fixed Controller) を推奨**

理由:
1. **画面数・サブドメインが多い** — WireMock の JSON ファイルが数百本になり管理破綻する
2. **ヘキサゴナルアーキテクチャ採用済み** — スタブ → 本番 DB の差し替えがアーキテクチャとして保証されている
3. **フロントエンジニアへのバック理解促進** — Controller の型定義を見ながら開発できる
4. **マルチリポ構成** — フロントエンジニアがバックのリポジトリを clone して `bootRun` するだけで API が動く

ただし、**「まだ API 設計が固まっていない初期画面」や「異常系の集中テスト」** に限り WireMock を部分活用するのはあり。

---

## フロント切り替えの仕組み（再掲・整理）

```
my-app/
├── .env.development   ← npm run dev         → Spring Boot (8080)
└── .env.wiremock      ← npm run dev:wiremock → WireMock   (9090)
```

**フロントのコードは変わらない。** env ファイルのポート番号が変わるだけ。  
Vite proxy が「フロント → バック」の中継役を担うので、CORS の設定もフロント側の fetch コードも同一のまま動く。
