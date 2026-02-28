/**
 * メニュー一覧ページコンポーネント。
 *
 * 全メニュー項目をテーブル形式で表示する。
 * カテゴリ別のタブフィルタリング、提供可能状態のチップ表示を含む。
 *
 * @route `/menu`
 */
"use client";

import React, { useEffect, useState } from "react";

const allCategories: (MenuCategory | "ALL")[] = [
  "ALL",
  "APPETIZER",
  "MAIN_COURSE",
  "SIDE_DISH",
  "DRINK",
  "DESSERT",
];

export default function MenuPage() {
  const [menuItems, setMenuItems] = useState<MenuItemView[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTab, setSelectedTab] = useState(0);

  useEffect(() => {
    menuApi
      .getAll()
      .then(setMenuItems)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const filteredItems =
    selectedTab === 0
      ? menuItems
      : menuItems.filter(
          (item) => item.category === allCategories[selectedTab]
        );

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom fontWeight="bold">
        メニュー一覧
      </Typography>

      <Tabs
        value={selectedTab}
        onChange={(_, v) => setSelectedTab(v)}
        sx={{ mb: 3 }}
        variant="scrollable"
        scrollButtons="auto"
      >
        <Tab label="すべて" />
        {(Object.keys(CATEGORY_LABELS) as MenuCategory[]).map((cat) => (
          <Tab key={cat} label={CATEGORY_LABELS[cat]} />
        ))}
      </Tabs>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>メニュー名</TableCell>
              <TableCell>カテゴリ</TableCell>
              <TableCell align="right">税抜価格</TableCell>
              <TableCell align="right">税込価格</TableCell>
              <TableCell align="center">提供状況</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredItems.map((item) => (
              <TableRow key={item.menuItemId}>
                <TableCell>
                  <Typography fontWeight="medium">{item.name}</Typography>
                </TableCell>
                <TableCell>
                  <Chip label={item.categoryDisplayName} size="small" />
                </TableCell>
                <TableCell align="right">
                  ¥{item.price.toLocaleString()}
                </TableCell>
                <TableCell align="right">
                  <Typography fontWeight="bold" color="primary">
                    ¥{item.priceWithTax.toLocaleString()}
                  </Typography>
                </TableCell>
                <TableCell align="center">
                  <Chip
                    label={item.available ? "提供中" : "提供停止"}
                    color={item.available ? "success" : "default"}
                    size="small"
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
