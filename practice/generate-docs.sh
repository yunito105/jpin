#!/bin/bash
# ============================================
#  全ドキュメント一括生成スクリプト
#  使い方: ./generate-docs.sh
# ============================================
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo "========================================"
echo "  全ドキュメント生成を開始します"
echo "========================================"
echo ""

# --- バックエンド（Javadoc + JiG） ---
echo "[1/3] Javadoc 生成中..."
cd "$SCRIPT_DIR/demo"
./gradlew javadoc --quiet && echo "  ✅ Javadoc  → demo/build/docs/javadoc/index.html" || echo "  ❌ Javadoc 生成に失敗しました"

echo ""
echo "[2/3] JiG レポート生成中..."
./gradlew jigReports --quiet && echo "  ✅ JiG      → demo/build/jig/index.html" || echo "  ❌ JiG 生成に失敗しました"

# --- フロントエンド（TypeDoc） ---
echo ""
echo "[3/3] TypeDoc 生成中..."
cd "$SCRIPT_DIR/frontend"
npm run doc --silent && echo "  ✅ TypeDoc  → frontend/docs/typedoc/index.html" || echo "  ❌ TypeDoc 生成に失敗しました"

cd "$SCRIPT_DIR"

echo ""
echo "========================================"
echo "  📚 全ドキュメント生成完了!"
echo "========================================"
echo ""
echo "  Javadoc  : demo/build/docs/javadoc/index.html"
echo "  JiG      : demo/build/jig/index.html"
echo "  TypeDoc  : frontend/docs/typedoc/index.html"
echo "  OpenAPI  : ./gradlew bootRun 後 → http://localhost:8080/swagger-ui.html"
echo ""
