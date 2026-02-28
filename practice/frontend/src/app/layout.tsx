/**
 * ルートレイアウト。
 *
 * Next.js App Router のルートレイアウト。
 * `ThemeRegistry` と `AppLayout` で全ページをラップし、
 * 統一的なデザインとナビゲーションを提供する。
 */
import type { Metadata } from "next";
import ThemeRegistry from "@/components/ThemeRegistry";
import AppLayout from "@/components/AppLayout";

export const metadata: Metadata = {
  title: "店舗ご注文システム",
  description: "実店舗向け注文管理システム",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ja">
      <body>
        <ThemeRegistry>
          <AppLayout>{children}</AppLayout>
        </ThemeRegistry>
      </body>
    </html>
  );
}
