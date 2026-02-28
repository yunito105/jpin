@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM  全ドキュメント一括生成スクリプト
REM  使い方: プロジェクトルートでダブルクリック
REM         または  .\generate-docs.bat
REM ============================================
echo.
echo ========================================
echo   全ドキュメント生成を開始します
echo ========================================
echo.

REM --- バックエンド（Javadoc + JiG） ---
echo [1/3] Javadoc 生成中...
cd /d "%~dp0demo"
call gradlew.bat javadoc --quiet
if %ERRORLEVEL% NEQ 0 (
    echo   ❌ Javadoc 生成に失敗しました
) else (
    echo   ✅ Javadoc  → demo\build\docs\javadoc\index.html
)

echo.
echo [2/3] JiG レポート生成中...
call gradlew.bat jigReports --quiet
if %ERRORLEVEL% NEQ 0 (
    echo   ❌ JiG 生成に失敗しました
) else (
    echo   ✅ JiG      → demo\build\jig\index.html
)

REM --- フロントエンド（TypeDoc） ---
echo.
echo [3/3] TypeDoc 生成中...
cd /d "%~dp0frontend"
call npm run doc --silent
if %ERRORLEVEL% NEQ 0 (
    echo   ❌ TypeDoc 生成に失敗しました
) else (
    echo   ✅ TypeDoc  → frontend\docs\typedoc\index.html
)

cd /d "%~dp0"

echo.
echo ========================================
echo   📚 全ドキュメント生成完了!
echo ========================================
echo.
echo   Javadoc  : demo\build\docs\javadoc\index.html
echo   JiG      : demo\build\jig\index.html
echo   TypeDoc  : frontend\docs\typedoc\index.html
echo   OpenAPI  : gradlew bootRun 後 → http://localhost:8080/swagger-ui.html
echo.
pause
