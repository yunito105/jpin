/**
 * 注文作成ページコンポーネント。
 *
 * 提供可能なメニューをカテゴリ別に表示し、カートに追加して注文を確定する。
 * テーブル番号の指定、カート内の数量変更・削除、注文確認ダイアログを含む。
 *
 * @route `/order`
 */
"use client";

import React, { useEffect, useState } from "react";
import AddIcon from "@mui/icons-material/Add";
import RemoveIcon from "@mui/icons-material/Remove";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import DeleteIcon from "@mui/icons-material/Delete";
import { menuApi, orderApi } from "@/lib/api";
import { MenuItemView, CATEGORY_LABELS, MenuCategory } from "@/types";

interface CartItem {
  menuItem: MenuItemView;
  quantity: number;
}

const categories: MenuCategory[] = [
  "APPETIZER",
  "MAIN_COURSE",
  "SIDE_DISH",
  "DRINK",
  "DESSERT",
];

export default function OrderPage() {
  const [menuItems, setMenuItems] = useState<MenuItemView[]>([]);
  const [selectedCategory, setSelectedCategory] = useState(0);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [tableNumber, setTableNumber] = useState<number>(1);
  const [cartOpen, setCartOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  useEffect(() => {
    menuApi.getAvailable().then(setMenuItems).catch(console.error);
  }, []);

  const filteredItems = menuItems.filter(
    (item) => item.category === categories[selectedCategory]
  );

  const addToCart = (menuItem: MenuItemView) => {
    setCart((prev) => {
      const existing = prev.find(
        (c) => c.menuItem.menuItemId === menuItem.menuItemId
      );
      if (existing) {
        return prev.map((c) =>
          c.menuItem.menuItemId === menuItem.menuItemId
            ? { ...c, quantity: c.quantity + 1 }
            : c
        );
      }
      return [...prev, { menuItem, quantity: 1 }];
    });
  };

  const updateQuantity = (menuItemId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((c) =>
          c.menuItem.menuItemId === menuItemId
            ? { ...c, quantity: Math.max(0, c.quantity + delta) }
            : c
        )
        .filter((c) => c.quantity > 0)
    );
  };

  const removeFromCart = (menuItemId: string) => {
    setCart((prev) =>
      prev.filter((c) => c.menuItem.menuItemId !== menuItemId)
    );
  };

  const totalItems = cart.reduce((sum, c) => sum + c.quantity, 0);
  const totalPrice = cart.reduce(
    (sum, c) => sum + c.menuItem.priceWithTax * c.quantity,
    0
  );

  const handlePlaceOrder = async () => {
    if (cart.length === 0) return;
    try {
      const result = await orderApi.place({
        tableNumber,
        items: cart.map((c) => ({
          menuItemId: c.menuItem.menuItemId,
          quantity: c.quantity,
        })),
      });
      setCart([]);
      setCartOpen(false);
      setSnackbar({
        open: true,
        message: `注文が完了しました（注文ID: ${result.orderId.substring(0, 8)}...）`,
        severity: "success",
      });
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "注文に失敗しました";
      setSnackbar({ open: true, message, severity: "error" });
    }
  };

  return (
    <Box>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Typography variant="h4" fontWeight="bold">
          注文する
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
          <TextField
            label="テーブル番号"
            type="number"
            size="small"
            value={tableNumber}
            onChange={(e) => setTableNumber(Math.max(1, Number(e.target.value)))}
            sx={{ width: 120 }}
            slotProps={{ htmlInput: { min: 1 } }}
          />
          <Button
            variant="contained"
            startIcon={
              <Badge badgeContent={totalItems} color="error">
                <ShoppingCartIcon />
              </Badge>
            }
            onClick={() => setCartOpen(true)}
            disabled={cart.length === 0}
          >
            カート
          </Button>
        </Box>
      </Box>

      <Tabs
        value={selectedCategory}
        onChange={(_, v) => setSelectedCategory(v)}
        sx={{ mb: 3 }}
        variant="scrollable"
        scrollButtons="auto"
      >
        {categories.map((cat) => (
          <Tab key={cat} label={CATEGORY_LABELS[cat]} />
        ))}
      </Tabs>

      <Grid container spacing={2}>
        {filteredItems.map((item) => (
          <Grid size={{ xs: 12, sm: 6, md: 4 }} key={item.menuItemId}>
            <Card>
              <CardContent>
                <Typography variant="h6">{item.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  税抜: ¥{item.price.toLocaleString()}
                </Typography>
                <Typography variant="h6" color="primary">
                  ¥{item.priceWithTax.toLocaleString()}（税込）
                </Typography>
                <Chip
                  label={item.categoryDisplayName}
                  size="small"
                  sx={{ mt: 1 }}
                />
              </CardContent>
              <CardActions>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => addToCart(item)}
                  fullWidth
                >
                  カートに追加
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* カートダイアログ */}
      <Dialog
        open={cartOpen}
        onClose={() => setCartOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>カート（テーブル {tableNumber}）</DialogTitle>
        <DialogContent>
          {cart.length === 0 ? (
            <Typography color="text.secondary">
              カートが空です
            </Typography>
          ) : (
            <List>
              {cart.map((item) => (
                <React.Fragment key={item.menuItem.menuItemId}>
                  <ListItem
                    secondaryAction={
                      <IconButton
                        edge="end"
                        onClick={() => removeFromCart(item.menuItem.menuItemId)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    }
                  >
                    <ListItemText
                      primary={item.menuItem.name}
                      secondary={`¥${item.menuItem.priceWithTax.toLocaleString()} × ${item.quantity} = ¥${(item.menuItem.priceWithTax * item.quantity).toLocaleString()}`}
                    />
                    <Box sx={{ display: "flex", alignItems: "center", mr: 2 }}>
                      <IconButton
                        size="small"
                        onClick={() =>
                          updateQuantity(item.menuItem.menuItemId, -1)
                        }
                      >
                        <RemoveIcon />
                      </IconButton>
                      <Typography sx={{ mx: 1 }}>{item.quantity}</Typography>
                      <IconButton
                        size="small"
                        onClick={() =>
                          updateQuantity(item.menuItem.menuItemId, 1)
                        }
                      >
                        <AddIcon />
                      </IconButton>
                    </Box>
                  </ListItem>
                  <Divider />
                </React.Fragment>
              ))}
              <ListItem>
                <ListItemText
                  primary={
                    <Typography variant="h6">
                      合計: ¥{totalPrice.toLocaleString()}（税込）
                    </Typography>
                  }
                />
              </ListItem>
            </List>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCartOpen(false)}>閉じる</Button>
          <Button
            variant="contained"
            onClick={handlePlaceOrder}
            disabled={cart.length === 0}
          >
            注文を確定する
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      >
        <Alert severity={snackbar.severity} variant="filled">
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
