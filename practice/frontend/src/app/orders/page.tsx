/**
 * 注文管理ページコンポーネント。
 *
 * 受付済み注文の一覧表示・ステータス管理・キャンセル操作を行う。
 * ステータス別のタブフィルタリング、注文詳細ダイアログ、
 * ステータス遷移ボタン（PLACED → PREPARING → READY → SERVED）を含む。
 *
 * @route `/orders`
 */
"use client";

import React, { useEffect, useState, useCallback } from "react";
import { orderApi } from "@/lib/api";
import {
  OrderSummary,
  OrderDetailView,
  OrderStatus,
  STATUS_LABELS,
} from "@/types";

const statusTabs: (OrderStatus | "ALL")[] = [
  "ALL",
  "PLACED",
  "PREPARING",
  "READY",
  "SERVED",
  "CANCELLED",
];

const statusColors: Record<
  OrderStatus,
  "default" | "primary" | "secondary" | "success" | "warning" | "error" | "info"
> = {
  PLACED: "info",
  PREPARING: "warning",
  READY: "success",
  SERVED: "default",
  CANCELLED: "error",
};

const nextStatusMap: Partial<Record<OrderStatus, OrderStatus>> = {
  PLACED: "PREPARING",
  PREPARING: "READY",
  READY: "SERVED",
};

const nextStatusLabel: Partial<Record<OrderStatus, string>> = {
  PLACED: "調理開始",
  PREPARING: "調理完了",
  READY: "提供済みにする",
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTab, setSelectedTab] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<OrderDetailView | null>(
    null
  );
  const [detailOpen, setDetailOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  const fetchOrders = useCallback(async () => {
    try {
      const status = statusTabs[selectedTab];
      const data =
        status === "ALL"
          ? await orderApi.getAll()
          : await orderApi.getAll({ status });
      setOrders(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [selectedTab]);

  useEffect(() => {
    setLoading(true);
    fetchOrders();
  }, [fetchOrders]);

  // 自動リフレッシュ（5秒間隔）
  useEffect(() => {
    const interval = setInterval(fetchOrders, 5000);
    return () => clearInterval(interval);
  }, [fetchOrders]);

  const handleViewDetail = async (orderId: string) => {
    try {
      const detail = await orderApi.getDetail(orderId);
      setSelectedOrder(detail);
      setDetailOpen(true);
    } catch (error) {
      console.error(error);
    }
  };

  const handleStatusUpdate = async (
    orderId: string,
    newStatus: string
  ) => {
    try {
      await orderApi.updateStatus(orderId, newStatus);
      setSnackbar({
        open: true,
        message: `ステータスを更新しました`,
        severity: "success",
      });
      fetchOrders();
      setDetailOpen(false);
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "更新に失敗しました";
      setSnackbar({ open: true, message, severity: "error" });
    }
  };

  const handleCancel = async (orderId: string) => {
    try {
      await orderApi.cancel(orderId);
      setSnackbar({
        open: true,
        message: "注文をキャンセルしました",
        severity: "success",
      });
      fetchOrders();
      setDetailOpen(false);
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "キャンセルに失敗しました";
      setSnackbar({ open: true, message, severity: "error" });
    }
  };

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
        注文管理
      </Typography>

      <Tabs
        value={selectedTab}
        onChange={(_, v) => setSelectedTab(v)}
        sx={{ mb: 3 }}
        variant="scrollable"
        scrollButtons="auto"
      >
        <Tab label="すべて" />
        {(Object.keys(STATUS_LABELS) as OrderStatus[]).map((status) => (
          <Tab key={status} label={STATUS_LABELS[status]} />
        ))}
      </Tabs>

      {orders.length === 0 ? (
        <Typography color="text.secondary" sx={{ textAlign: "center", mt: 4 }}>
          注文がありません
        </Typography>
      ) : (
        <Grid container spacing={2}>
          {orders.map((order) => (
            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={order.orderId}>
              <Card>
                <CardContent>
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      mb: 1,
                    }}
                  >
                    <Typography variant="h6">
                      テーブル {order.tableNumber}
                    </Typography>
                    <Chip
                      label={order.statusDisplayName}
                      color={statusColors[order.status as OrderStatus]}
                      size="small"
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    注文ID: {order.orderId.substring(0, 8)}...
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    商品数: {order.itemCount}点
                  </Typography>
                  <Typography variant="h6" color="primary" sx={{ mt: 1 }}>
                    ¥{order.totalAmountWithTax.toLocaleString()}（税込）
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(order.orderedAt).toLocaleString("ja-JP")}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button
                    size="small"
                    onClick={() => handleViewDetail(order.orderId)}
                  >
                    詳細
                  </Button>
                  {nextStatusMap[order.status as OrderStatus] && (
                    <Button
                      size="small"
                      variant="contained"
                      onClick={() =>
                        handleStatusUpdate(
                          order.orderId,
                          nextStatusMap[order.status as OrderStatus]!
                        )
                      }
                    >
                      {nextStatusLabel[order.status as OrderStatus]}
                    </Button>
                  )}
                  {(order.status === "PLACED" ||
                    order.status === "PREPARING") && (
                    <Button
                      size="small"
                      color="error"
                      onClick={() => handleCancel(order.orderId)}
                    >
                      キャンセル
                    </Button>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* 注文詳細ダイアログ */}
      <Dialog
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        maxWidth="md"
        fullWidth
      >
        {selectedOrder && (
          <>
            <DialogTitle>
              注文詳細 - テーブル {selectedOrder.tableNumber}
              <Chip
                label={selectedOrder.statusDisplayName}
                color={statusColors[selectedOrder.status as OrderStatus]}
                size="small"
                sx={{ ml: 2 }}
              />
            </DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                注文ID: {selectedOrder.orderId}
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>商品名</TableCell>
                      <TableCell align="right">単価</TableCell>
                      <TableCell align="right">数量</TableCell>
                      <TableCell align="right">小計</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedOrder.items.map((item, idx) => (
                      <TableRow key={idx}>
                        <TableCell>{item.menuItemName}</TableCell>
                        <TableCell align="right">
                          ¥{item.unitPrice.toLocaleString()}
                        </TableCell>
                        <TableCell align="right">{item.quantity}</TableCell>
                        <TableCell align="right">
                          ¥{item.subtotal.toLocaleString()}
                        </TableCell>
                      </TableRow>
                    ))}
                    <TableRow>
                      <TableCell colSpan={3} align="right">
                        <strong>合計（税抜）</strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>
                          ¥{selectedOrder.totalAmount.toLocaleString()}
                        </strong>
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell colSpan={3} align="right">
                        <strong>合計（税込）</strong>
                      </TableCell>
                      <TableCell align="right">
                        <Typography fontWeight="bold" color="primary">
                          ¥{selectedOrder.totalAmountWithTax.toLocaleString()}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDetailOpen(false)}>閉じる</Button>
              {nextStatusMap[selectedOrder.status as OrderStatus] && (
                <Button
                  variant="contained"
                  onClick={() =>
                    handleStatusUpdate(
                      selectedOrder.orderId,
                      nextStatusMap[selectedOrder.status as OrderStatus]!
                    )
                  }
                >
                  {nextStatusLabel[selectedOrder.status as OrderStatus]}
                </Button>
              )}
              {(selectedOrder.status === "PLACED" ||
                selectedOrder.status === "PREPARING") && (
                <Button
                  color="error"
                  onClick={() => handleCancel(selectedOrder.orderId)}
                >
                  キャンセル
                </Button>
              )}
            </DialogActions>
          </>
        )}
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
