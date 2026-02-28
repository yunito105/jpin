/**
 * ホームページコンポーネント。
 *
 * アプリケーションのトップページ。
 * 「注文する」「メニュー一覧」「注文管理」の3つのカードを表示し、
 * 各機能ページへのナビゲーションを提供する。
 *
 * @route `/`
 */
"use client";

import { Box, Typography, Card, CardContent, Grid, Button } from "@mui/material";
import RestaurantIcon from "@mui/icons-material/Restaurant";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import { useRouter } from "next/navigation";

export default function HomePage() {
  const router = useRouter();

  const cards = [
    {
      title: "注文する",
      description: "メニューから商品を選択して注文を作成します",
      icon: <RestaurantIcon sx={{ fontSize: 60, color: "primary.main" }} />,
      path: "/order",
    },
    {
      title: "メニュー一覧",
      description: "提供中のメニュー項目を確認できます",
      icon: <MenuBookIcon sx={{ fontSize: 60, color: "secondary.main" }} />,
      path: "/menu",
    },
    {
      title: "注文管理",
      description: "受付した注文の確認・ステータス管理を行います",
      icon: <ReceiptLongIcon sx={{ fontSize: 60, color: "success.main" }} />,
      path: "/orders",
    },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom fontWeight="bold">
        店舗ご注文システム
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        メニューの選択から注文の管理まで、店舗運営をサポートします。
      </Typography>

      <Grid container spacing={3}>
        {cards.map((card) => (
          <Grid size={{ xs: 12, sm: 6, md: 4 }} key={card.path}>
            <Card
              sx={{
                height: "100%",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                cursor: "pointer",
                "&:hover": { boxShadow: 6 },
              }}
              onClick={() => router.push(card.path)}
            >
              <CardContent sx={{ textAlign: "center", p: 4 }}>
                {card.icon}
                <Typography variant="h5" sx={{ mt: 2, mb: 1 }}>
                  {card.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {card.description}
                </Typography>
                <Button variant="contained" sx={{ mt: 2 }}>
                  開く
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
