/**
 * テーマレジストリコンポーネント。
 *
 * MUIの `ThemeProvider` と `CssBaseline` をラップし、
 * アプリケーション全体に統一的なテーマを適用する。
 *
 * `RootLayout` で `AppLayout` を囲む形で使用される。
 *
 * @param props.children - テーマを適用する子要素
 *
 * @example
 * ```tsx
 * <ThemeRegistry>
 *   <AppLayout>{children}</AppLayout>
 * </ThemeRegistry>
 * ```
 */
"use client";

import { ThemeProvider, CssBaseline } from "@mui/material";
import theme from "@/lib/theme";
import React from "react";

export default function ThemeRegistry({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
