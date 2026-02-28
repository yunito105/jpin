/**
 * MUIテーマ設定モジュール。
 *
 * アプリケーション全体のカラーパレット・タイポグラフィを定義する。
 * `ThemeRegistry` コンポーネントで `ThemeProvider` に渡される。
 *
 * @see {@link https://mui.com/material-ui/customization/theming/ | MUI Theming}
 * @module theme
 */
"use client";

import { createTheme } from "@mui/material/styles";

/**
 * アプリケーションのMUIテーマ。
 *
 * - **primary**: ブルー系 (`#1976d2`) — ナビゲーション・ボタン等
 * - **secondary**: オレンジ系 (`#ff9800`) — アクセント色
 * - **background**: ライトグレー (`#f5f5f5`)
 * - **フォント**: Noto Sans JP を含むシステムフォントスタック
 */
const theme = createTheme({
  palette: {
    primary: {
      main: "#1976d2",
    },
    secondary: {
      main: "#ff9800",
    },
    background: {
      default: "#f5f5f5",
    },
  },
  typography: {
    fontFamily: [
      "-apple-system",
      "BlinkMacSystemFont",
      '"Segoe UI"',
      "Roboto",
      '"Noto Sans JP"',
      "sans-serif",
    ].join(","),
  },
});

export default theme;
