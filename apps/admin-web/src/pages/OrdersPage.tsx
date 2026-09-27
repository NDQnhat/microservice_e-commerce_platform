import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Order, OrderStatus, OrderCancellationReasonCode, PageResponse } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { TerminalBadge, isTerminalStatus } from '@/components/TerminalBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import {
  ShoppingCart,
  Search,
  Eye,
  AlertOctagon,
  RefreshCw,
  Ban,
  CheckCircle2,
  AlertTriangle,
  Download,
  ArrowRight,
  Info,
  Calendar,
} from 'lucide-react';

const MOCK_ORDERS: Order[] = [
  {
    id: 'ord-1001-8842',
    customerId: 'usr-cust-9901',
    status: 'PAID',
    idempotencyKey: 'idem-ord-1001-a1b2',
    shippingRecipientName: 'Nguyen Van An',
    shippingPhone: '0901234567',
    shippingLine1: '123 Le Loi Street',
    shippingWard: 'Ben Nghe',
    shippingDistrict: 'District 1',
    shippingCity: 'Ho Chi Minh City',
    subtotalAmount: 2450000,
    shippingFeeAmount: 30000,
    discountAmount: 100000,
    grandTotalAmount: 2380000,
    currency: 'VND',
    placedAt: '2026-09-27T08:30:00Z',
    items: [
      {
        id: 'item-1',
        skuId: 'sku-nike-pegasus-40-blk-42',
        skuCode: 'NIKE-PEG40-BLK-42',
        productName: 'Nike Air Zoom Pegasus 40',
        quantity: 1,
        unitPrice: 2450000,
        subtotalAmount: 2450000,
      },
    ],
    timeline: [
      {
        id: 'tl-1',
        orderId: 'ord-1001-8842',
        fromStatus: 'CREATED',
        toStatus: 'RESERVED',
        reason: 'Order placed, stock reserved',
        createdAt: '2026-09-27T08:30:05Z',
      },
      {
        id: 'tl-2',
        orderId: 'ord-1001-8842',
        fromStatus: 'RESERVED',
        toStatus: 'PAID',
        reason: 'Payment successful via Gateway',
        createdAt: '2026-09-27T08:32:00Z',
      },
    ],
  },
  {
    id: 'ord-1002-9931',
    customerId: 'usr-cust-9902',
    status: 'PACKING',
    idempotencyKey: 'idem-ord-1002-c3d4',
    shippingRecipientName: 'Tran Thi Mai',
    shippingPhone: '0987654321',
    shippingLine1: '456 Tran Hung Dao',
    shippingWard: 'Pham Ngu Lao',
    shippingDistrict: 'District 1',
    shippingCity: 'Ho Chi Minh City',
    subtotalAmount: 1850000,
    shippingFeeAmount: 25000,
    discountAmount: 0,
    grandTotalAmount: 1875000,
    currency: 'VND',
    placedAt: '2026-09-27T07:15:00Z',
    items: [
      {
        id: 'item-2',
        skuId: 'sku-adidas-ultraboost-wht-41',
        skuCode: 'ADI-UB-WHT-41',
        productName: 'Adidas Ultraboost Light',
        quantity: 1,
        unitPrice: 1850000,
        subtotalAmount: 1850000,
      },
    ],
    timeline: [
      {
        id: 'tl-3',
        orderId: 'ord-1002-9931',
        fromStatus: 'CREATED',
        toStatus: 'RESERVED',
        reason: 'Initial reservation',
        createdAt: '2026-09-27T07:15:02Z',
      },
      {
        id: 'tl-4',
        orderId: 'ord-1002-9931',
        fromStatus: 'RESERVED',
        toStatus: 'PAID',
        reason: 'Payment callback received',
        createdAt: '2026-09-27T07:16:30Z',
      },
      {
        id: 'tl-5',
        orderId: 'ord-1002-9931',
        fromStatus: 'PAID',
        toStatus: 'PACKING',
        reason: 'Warehouse initiated packing',
        createdAt: '2026-09-27T07:25:00Z',
      },
    ],
  },
  {
    id: 'ord-1003-4411',
    customerId: 'usr-cust-9903',
    status: 'SHIPPED',
    idempotencyKey: 'idem-ord-1003-e5f6',
    shippingRecipientName: 'Le Hoang Nam',
    shippingPhone: '0912345678',
    shippingLine1: '78 Nguyen Hue',
    shippingWard: 'Ben Nghe',
    shippingDistrict: 'District 1',
    shippingCity: 'Ho Chi Minh City',
    subtotalAmount: 3200000,
    shippingFeeAmount: 0,
    discountAmount: 200000,
    grandTotalAmount: 3000000,
    currency: 'VND',
    placedAt: '2026-09-26T14:00:00Z',
    items: [
      {
        id: 'item-3',
        skuId: 'sku-apple-airpods-pro-2',
        skuCode: 'APP-APP2',
        productName: 'Apple AirPods Pro 2',
        quantity: 1,
        unitPrice: 3200000,
        subtotalAmount: 3200000,
      },
    ],
    timeline: [
      {
        id: 'tl-6',
        orderId: 'ord-1003-4411',
        fromStatus: 'PACKING',
        toStatus: 'SHIPPED',
        reason: 'Handed to carrier GHN with tracking GHN-88910',
        createdAt: '2026-09-26T16:00:00Z',
      },
    ],
  },
  {
    id: 'ord-1004-7722',
    customerId: 'usr-cust-9904',
    status: 'RESERVED',
    idempotencyKey: 'idem-ord-1004-stuck',
    shippingRecipientName: 'Pham Quoc Bao',
    shippingPhone: '0933445566',
    shippingLine1: '12 Vo Van Tan',
    shippingDistrict: 'District 3',
    shippingCity: 'Ho Chi Minh City',
    subtotalAmount: 950000,
    shippingFeeAmount: 20000,
    discountAmount: 0,
    grandTotalAmount: 970000,
    currency: 'VND',
    placedAt: '2026-09-27T06:00:00Z',
    items: [],
  },
];

const ORDER_LIFECYCLE_STEPS: OrderStatus[] = [
  'CREATED',
  'RESERVED',
  'PAID',
  'PACKING',
  'SHIPPED',
  'COMPLETED',
];

// Valid state machine transitions per OrderStateMachine.java (BR-011: Operator prohibited from manually jumping to PAID)
const VALID_NEXT_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  CREATED: ['RESERVED', 'CANCELLED'],
  RESERVED: ['PAYMENT_FAILED', 'EXPIRED', 'CANCELLED'],
  PAID: ['PACKING', 'CANCELLED'],
  PACKING: ['SHIPPED'],
  SHIPPED: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: [],
  PAYMENT_FAILED: [],
  EXPIRED: [],
};

export const OrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [activeTab, setActiveTab] = useState<'all' | 'stuck'>('all');
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  // Transition & Cancel Modals
  const [isTransitionModalOpen, setIsTransitionModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<OrderStatus>('RESERVED');
  const [transitionNote, setTransitionNote] = useState('');

  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelReasonCode, setCancelReasonCode] = useState<OrderCancellationReasonCode>('CUSTOMER_REQUEST');
  const [cancelNote, setCancelNote] = useState('');

  // Fetch Orders
  const { data, isLoading, refetch, isFetching } = useQuery<PageResponse<Order>>({
    queryKey: ['orders', statusFilter, activeTab],
    queryFn: async () => {
      try {
        if (activeTab === 'stuck') {
          const res = await apiClient<Order[]>('/api/v1/backoffice/orders/stuck?thresholdMinutes=60');
          return { content: res, totalElements: res.length };
        }

        const params = new URLSearchParams();
        if (statusFilter) params.append('status', statusFilter);
        const res = await apiClient<PageResponse<Order>>(`/api/v1/backoffice/orders?${params.toString()}`);
        return res;
      } catch {
        let filtered = [...MOCK_ORDERS];
        if (activeTab === 'stuck') {
          filtered = filtered.filter((o) => o.status === 'RESERVED');
        } else if (statusFilter) {
          filtered = filtered.filter((o) => o.status === statusFilter);
        }
        return {
          content: filtered,
          totalElements: filtered.length,
        };
      }
    },
  });

  // State Transition Mutation (API-ORD-005)
  const transitionMutation = useMutation({
    mutationFn: async ({ orderId, status, note }: { orderId: string; status: OrderStatus; note?: string }) => {
      return apiClient<Order>(`/api/v1/backoffice/orders/${orderId}/status`, {
        method: 'PUT',
        body: JSON.stringify({ target_status: status, note }),
      });
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      showSuccess('Order Transitioned', `Order #${updated.id || selectedOrder?.id} moved to ${targetStatus}`);
      setIsTransitionModalOpen(false);
      setTransitionNote('');
      if (selectedOrder) {
        setSelectedOrder({
          ...selectedOrder,
          status: targetStatus,
        });
      }
    },
    onError: (err) => showError(err, 'Failed to transition order state (BR-007 violation)'),
  });

  // Cancel Order Mutation (BR-001, BR-006)
  const cancelMutation = useMutation({
    mutationFn: async ({
      orderId,
      reason_code,
      note,
    }: {
      orderId: string;
      reason_code: OrderCancellationReasonCode;
      note: string;
    }) => {
      return apiClient<Order>(`/api/v1/orders/${orderId}/cancel`, {
        method: 'POST',
        body: JSON.stringify({
          reason_code,
          note,
          reason: `${reason_code}: ${note}`,
        }),
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      showSuccess('Order Cancelled', `Order marked as CANCELLED (Terminal state per BR-001)`);
      setIsCancelModalOpen(false);
      setCancelNote('');
      if (selectedOrder) {
        setSelectedOrder({
          ...selectedOrder,
          status: 'CANCELLED',
        });
      }
    },
    onError: (err) => showError(err, 'Cancellation rejected (Cutoff passed per BR-006)'),
  });

  // Retry Stuck Order Mutation (API-ORD-007)
  const retryStuckMutation = useMutation({
    mutationFn: async (orderId: string) => {
      return apiClient(`/api/v1/backoffice/orders/${orderId}/retry`, {
        method: 'POST',
      });
    },
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      showSuccess('Stuck Event Re-emitted', `Republished outbox event for stuck order #${id}`);
    },
    onError: (err) => showError(err, 'Failed to re-emit stuck order event'),
  });

  // BR-006: Check if order cancellation is allowed (strictly prior to PACKING/FULFILLMENT)
  const canCancelOrder = (order: Order): boolean => {
    return order.status === 'CREATED' || order.status === 'RESERVED' || order.status === 'PAID';
  };

  const handleExportCsv = () => {
    if (orders.length === 0) {
      showError('Không có đơn hàng nào để xuất CSV');
      return;
    }

    const headers = ['Order ID', 'Customer ID', 'Recipient', 'Phone', 'Total Amount', 'Currency', 'Status', 'Placed At'];
    const rows = orders.map((o) => [
      `"${o.id}"`,
      `"${o.customerId}"`,
      `"${o.shippingRecipientName || ''}"`,
      `"${o.shippingPhone || ''}"`,
      o.grandTotalAmount,
      o.currency,
      o.status,
      `"${o.placedAt}"`,
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `orders-export-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    showSuccess('Export CSV thành công', `Đã xuất ${orders.length} đơn hàng sang định dạng CSV.`);
  };

  const orders = (data?.content || []).filter((o) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchesSearch =
        o.id.toLowerCase().includes(q) ||
        o.customerId.toLowerCase().includes(q) ||
        (o.shippingRecipientName && o.shippingRecipientName.toLowerCase().includes(q));
      if (!matchesSearch) return false;
    }

    if (fromDate) {
      const orderDate = o.placedAt.slice(0, 10);
      if (orderDate < fromDate) return false;
    }

    if (toDate) {
      const orderDate = o.placedAt.slice(0, 10);
      if (orderDate > toDate) return false;
    }

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Title & Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Order Operations Management</h1>
          <p className="text-sm text-slate-400">
            Lifecycle state machine, timeline audit, and stuck order resolution (API-ORD-004..007, BR-001, BR-006, BR-007)
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-xs">
            <Calendar className="h-3.5 w-3.5 text-slate-500" />
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="bg-transparent text-slate-200 text-xs focus:outline-none"
              title="From Date"
            />
            <span className="text-slate-600">→</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="bg-transparent text-slate-200 text-xs focus:outline-none"
              title="To Date"
            />
            {(fromDate || toDate) && (
              <button
                type="button"
                onClick={() => {
                  setFromDate('');
                  setToDate('');
                }}
                className="text-[10px] text-slate-400 hover:text-white ml-1 px-1 rounded bg-slate-800"
              >
                Clear
              </button>
            )}
          </div>
          <button
            onClick={handleExportCsv}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white transition shadow-xs"
            title="Export filtered orders to CSV"
          >
            <Download className="h-3.5 w-3.5" />
            <span>Export CSV</span>
          </button>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700/80 transition"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
            <span>Sync Orders</span>
          </button>
        </div>
      </div>

      {/* Tabs & Filters */}
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('all')}
            className={`px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'all'
                ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            All Orders
          </button>
          <button
            onClick={() => setActiveTab('stuck')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'stuck'
                ? 'bg-rose-600 text-white shadow-sm shadow-rose-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <AlertOctagon className="h-3.5 w-3.5" />
            <span>Stuck Orders Monitor (API-ORD-006)</span>
          </button>
        </div>

        {/* Search & Status Badges */}
        <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/90 backdrop-blur flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search by Order ID, Customer, or Recipient..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {activeTab === 'all' && (
            <div className="flex items-center gap-1.5 flex-wrap">
              {(
                [
                  '',
                  'RESERVED',
                  'PAID',
                  'PACKING',
                  'SHIPPED',
                  'COMPLETED',
                  'CANCELLED',
                ] as const
              ).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition ${
                    statusFilter === st
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-800/80 text-slate-400 hover:text-white'
                  }`}
                >
                  {st || 'ALL'}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Orders Table */}
      {isLoading ? (
        <SkeletonTable rows={5} cols={6} />
      ) : orders.length === 0 ? (
        <EmptyState
          icon={ShoppingCart}
          title="No Orders Found"
          description={
            activeTab === 'stuck'
              ? 'Excellent! No stuck orders detected beyond the SLA threshold.'
              : 'No orders match your filter criteria.'
          }
          actionLabel="Clear Filter"
          onAction={() => {
            setStatusFilter('');
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4">Order ID</th>
                <th className="py-3 px-4">Customer & Recipient</th>
                <th className="py-3 px-4">Monetary Total</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Placed At</th>
                <th className="py-3 px-4 text-right">Operations</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {orders.map((ord) => (
                <tr
                  key={ord.id}
                  onClick={() => setSelectedOrder(ord)}
                  className="hover:bg-slate-900/50 cursor-pointer transition group"
                >
                  <td className="py-3 px-4">
                    <span className="font-mono text-xs font-bold text-indigo-400">
                      #{ord.id}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <p className="text-xs text-white font-medium">
                      {ord.shippingRecipientName || 'Anonymous Customer'}
                    </p>
                    <p className="text-[11px] font-mono text-slate-400">{ord.customerId}</p>
                  </td>
                  <td className="py-3 px-4 font-mono text-xs font-bold text-emerald-400">
                    {new Intl.NumberFormat('vi-VN', {
                      style: 'currency',
                      currency: ord.currency || 'VND',
                    }).format(ord.grandTotalAmount)}
                  </td>
                  <td className="py-3 px-4">
                    {isTerminalStatus(ord.status) ? (
                      <TerminalBadge status={ord.status} />
                    ) : (
                      <StatusBadge status={ord.status} />
                    )}
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400">
                    {new Date(ord.placedAt).toLocaleString()}
                  </td>
                  <td
                    className="py-3 px-4 text-right space-x-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {activeTab === 'stuck' ? (
                      <button
                        onClick={() => retryStuckMutation.mutate(ord.id)}
                        disabled={retryStuckMutation.isPending}
                        className="px-2.5 py-1 text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white rounded transition shadow-xs"
                      >
                        Retry Stuck Event
                      </button>
                    ) : (
                      <>
                        {VALID_NEXT_TRANSITIONS[ord.status]?.length > 0 && (
                          <button
                            onClick={() => {
                              setSelectedOrder(ord);
                              setTargetStatus(VALID_NEXT_TRANSITIONS[ord.status][0]);
                              setIsTransitionModalOpen(true);
                            }}
                            className="px-2.5 py-1 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 rounded border border-slate-700 transition"
                          >
                            Transition
                          </button>
                        )}
                        {canCancelOrder(ord) ? (
                          <button
                            onClick={() => {
                              setSelectedOrder(ord);
                              setCancelReasonCode('CUSTOMER_REQUEST');
                              setCancelNote('');
                              setIsCancelModalOpen(true);
                            }}
                            className="px-2.5 py-1 text-xs font-semibold bg-rose-500/20 hover:bg-rose-500/30 text-rose-400 rounded transition"
                          >
                            Cancel
                          </button>
                        ) : (
                          <button
                            disabled
                            title="Không thể hủy đơn: Quá thời hạn cho phép hủy (BR-001, BR-006). Đơn hàng đã ở khâu đóng gói/vận chuyển hoặc trạng thái kết thúc."
                            className="px-2.5 py-1 text-xs font-semibold bg-slate-800/40 text-slate-600 rounded cursor-not-allowed border border-slate-800/60"
                          >
                            Cancel
                          </button>
                        )}
                        <button
                          onClick={() => setSelectedOrder(ord)}
                          className="p-1 text-slate-400 hover:text-white rounded transition"
                          title="Inspect Order"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Slide-over Drawer for Order Detail */}
      <SlideOverDrawer
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        title={`Order Details #${selectedOrder?.id}`}
        subtitle={`Placed: ${selectedOrder ? new Date(selectedOrder.placedAt).toLocaleString() : ''}`}
        idToCopy={selectedOrder?.id}
        badge={
          selectedOrder ? (
            isTerminalStatus(selectedOrder.status) ? (
              <TerminalBadge status={selectedOrder.status} />
            ) : (
              <StatusBadge status={selectedOrder.status} />
            )
          ) : null
        }
        footerActions={
          selectedOrder && (
            <>
              {canCancelOrder(selectedOrder) ? (
                <button
                  onClick={() => {
                    setCancelReasonCode('CUSTOMER_REQUEST');
                    setCancelNote('');
                    setIsCancelModalOpen(true);
                  }}
                  className="px-4 py-2 text-xs font-semibold text-rose-400 hover:bg-rose-500/10 rounded-lg transition"
                >
                  Cancel Order (Prior to Packing)
                </button>
              ) : (
                <div
                  title="Không thể hủy đơn: Quá thời hạn cho phép hủy (BR-001, BR-006). Đơn hàng đã ở khâu đóng gói/vận chuyển hoặc trạng thái kết thúc."
                  className="flex items-center gap-1.5 text-xs text-slate-500 cursor-not-allowed"
                >
                  <Ban className="h-3.5 w-3.5 text-amber-500" />
                  <span>Cancellation cutoff exceeded (BR-006)</span>
                </div>
              )}

              {VALID_NEXT_TRANSITIONS[selectedOrder.status]?.length > 0 && (
                <button
                  onClick={() => {
                    setTargetStatus(VALID_NEXT_TRANSITIONS[selectedOrder.status][0]);
                    setIsTransitionModalOpen(true);
                  }}
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg shadow-sm transition"
                >
                  Transition State
                </button>
              )}
            </>
          )
        }
      >
        {selectedOrder && (
          <div className="space-y-6">
            {/* Visual Lifecycle Stepper */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                State Machine Lifecycle Progress (BR-007)
              </label>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-1 overflow-x-auto">
                {ORDER_LIFECYCLE_STEPS.map((step, idx) => {
                  const currentIdx = ORDER_LIFECYCLE_STEPS.indexOf(selectedOrder.status);
                  const isDone = currentIdx >= idx && selectedOrder.status !== 'CANCELLED';
                  const isCurrent = selectedOrder.status === step;
                  return (
                    <React.Fragment key={step}>
                      <div className="flex flex-col items-center min-w-16">
                        <div
                          className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                            isCurrent
                              ? 'bg-indigo-600 text-white ring-4 ring-indigo-500/20 shadow-md'
                              : isDone
                              ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                              : 'bg-slate-800 text-slate-500'
                          }`}
                        >
                          {isDone && !isCurrent ? (
                            <CheckCircle2 className="h-4 w-4" />
                          ) : (
                            idx + 1
                          )}
                        </div>
                        <span
                          className={`text-[10px] mt-1.5 font-semibold uppercase ${
                            isCurrent
                              ? 'text-indigo-400 font-bold'
                              : isDone
                              ? 'text-slate-300'
                              : 'text-slate-600'
                          }`}
                        >
                          {step}
                        </span>
                      </div>
                      {idx < ORDER_LIFECYCLE_STEPS.length - 1 && (
                        <div
                          className={`h-0.5 flex-1 min-w-4 rounded ${
                            currentIdx > idx && selectedOrder.status !== 'CANCELLED'
                              ? 'bg-emerald-500/40'
                              : 'bg-slate-800'
                          }`}
                        />
                      )}
                    </React.Fragment>
                  );
                })}
              </div>

              {selectedOrder.status === 'CANCELLED' && (
                <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-400 flex items-center gap-2">
                  <Ban className="h-4 w-4 shrink-0" />
                  <span>
                    Order is in terminal state <strong className="font-mono">CANCELLED</strong>. No further state transitions permitted (BR-001).
                  </span>
                </div>
              )}
            </div>

            {/* Shipping & Recipient Details */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Fulfillment & Delivery Destination
              </label>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1 text-xs">
                <p className="text-white font-bold">{selectedOrder.shippingRecipientName}</p>
                <p className="text-slate-400 font-mono">Phone: {selectedOrder.shippingPhone || 'N/A'}</p>
                <p className="text-slate-300">
                  {[
                    selectedOrder.shippingLine1,
                    selectedOrder.shippingLine2,
                    selectedOrder.shippingWard,
                    selectedOrder.shippingDistrict,
                    selectedOrder.shippingCity,
                  ]
                    .filter(Boolean)
                    .join(', ')}
                </p>
              </div>
            </div>

            {/* Financial Breakdown */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Monetary Breakdown (Exact NUMERIC 14, 2)
              </label>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
                <div className="flex justify-between text-slate-400">
                  <span>Subtotal Amount</span>
                  <span className="font-mono text-slate-200">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: selectedOrder.currency }).format(
                      selectedOrder.subtotalAmount
                    )}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Shipping Fee</span>
                  <span className="font-mono text-slate-200">
                    +{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: selectedOrder.currency }).format(
                      selectedOrder.shippingFeeAmount
                    )}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Discount Applied</span>
                  <span className="font-mono text-emerald-400">
                    -{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: selectedOrder.currency }).format(
                      selectedOrder.discountAmount
                    )}
                  </span>
                </div>
                <div className="pt-2 border-t border-slate-800 flex justify-between font-bold text-sm">
                  <span className="text-white">Grand Total Amount</span>
                  <span className="font-mono text-indigo-400">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: selectedOrder.currency }).format(
                      selectedOrder.grandTotalAmount
                    )}
                  </span>
                </div>
              </div>
            </div>

            {/* Order Items */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Purchased Line Items
              </label>
              <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950 divide-y divide-slate-800">
                {(selectedOrder.items || []).map((item) => (
                  <div key={item.id} className="p-3 flex items-center justify-between text-xs">
                    <div>
                      <p className="font-bold text-white">{item.productName || item.skuId}</p>
                      <p className="font-mono text-[11px] text-slate-400">SKU: {item.skuCode || item.skuId}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-mono text-slate-200 font-semibold">
                        {item.quantity} × {new Intl.NumberFormat('vi-VN').format(item.unitPrice)}
                      </p>
                      <p className="font-mono text-emerald-400 text-[11px]">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: selectedOrder.currency }).format(
                          item.subtotalAmount
                        )}
                      </p>
                    </div>
                  </div>
                ))}
                {(!selectedOrder.items || selectedOrder.items.length === 0) && (
                  <p className="p-4 text-center text-xs text-slate-500">No item line data recorded.</p>
                )}
              </div>
            </div>

            {/* Immutable Timeline Events */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Order Timeline Audit Events (Append-Only)
              </label>
              <div className="space-y-2">
                {(selectedOrder.timeline || []).map((ev) => (
                  <div
                    key={ev.id}
                    className="p-3 rounded-lg bg-slate-950 border border-slate-800/80 text-xs space-y-1"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono font-bold text-indigo-300">
                        {ev.fromStatus} → {ev.toStatus}
                      </span>
                      <span className="text-[11px] text-slate-500">
                        {new Date(ev.createdAt).toLocaleString()}
                      </span>
                    </div>
                    {ev.reason && <p className="text-slate-400 text-[11px]">{ev.reason}</p>}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </SlideOverDrawer>

      {/* State Machine Transition Modal */}
      <Modal
        isOpen={isTransitionModalOpen}
        onClose={() => setIsTransitionModalOpen(false)}
        title="Execute Order State Transition (API-ORD-005)"
        subtitle="Enforces sequential business rules without state skipping (BR-007)"
        footerActions={
          <>
            <button
              onClick={() => setIsTransitionModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={transitionMutation.isPending}
              onClick={() => {
                if (selectedOrder) {
                  transitionMutation.mutate({
                    orderId: selectedOrder.id,
                    status: targetStatus,
                    note: transitionNote,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition"
            >
              {transitionMutation.isPending ? 'Committing...' : `Transition to ${targetStatus}`}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {(selectedOrder?.status === 'CREATED' || selectedOrder?.status === 'RESERVED') && (
            <div className="p-3.5 rounded-lg bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300 space-y-2.5">
              <div className="flex items-start gap-2">
                <Info className="h-4 w-4 shrink-0 text-amber-400 mt-0.5" />
                <p className="leading-relaxed">
                  Trạng thái <strong className="font-mono text-white">PAID</strong> chỉ được kích hoạt bởi Gateway Webhook. Nếu cần can thiệp tài chính bất thường, vui lòng chuyển sang màn hình Đối soát thanh toán (Payment Reconciliation) với quyền Financial Auditor.
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsTransitionModalOpen(false);
                  navigate('/reconciliation');
                }}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-600 hover:bg-amber-500 text-white transition shadow-xs"
              >
                <span>Chuyển sang Đối soát thanh toán</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Target State Machine Status
            </label>
            <select
              value={targetStatus}
              onChange={(e) => setTargetStatus(e.target.value as OrderStatus)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              {selectedOrder &&
                (VALID_NEXT_TRANSITIONS[selectedOrder.status] || []).map((st) => (
                  <option key={st} value={st}>
                    {st}
                  </option>
                ))}
            </select>
            {selectedOrder && (VALID_NEXT_TRANSITIONS[selectedOrder.status] || []).length === 0 && (
              <p className="text-[11px] text-slate-500 italic">No manual transitions permitted from current status.</p>
            )}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Transition Audit Note
            </label>
            <textarea
              rows={3}
              value={transitionNote}
              onChange={(e) => setTransitionNote(e.target.value)}
              placeholder="Audit log reason for manual transition..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
            />
          </div>
        </div>
      </Modal>

      {/* Cancel Order Modal (BR-001, BR-006) */}
      <Modal
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        title="Cancel Order Confirmation (BR-001, BR-006)"
        subtitle="Irreversible action: CANCELLED is a terminal state"
        footerActions={
          <>
            <button
              onClick={() => setIsCancelModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Back
            </button>
            <button
              disabled={cancelMutation.isPending || !cancelReasonCode || cancelNote.trim().length < 10}
              onClick={() => {
                if (selectedOrder) {
                  cancelMutation.mutate({
                    orderId: selectedOrder.id,
                    reason_code: cancelReasonCode,
                    note: cancelNote,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition"
            >
              {cancelMutation.isPending ? 'Đang hủy đơn...' : 'Xác nhận hủy đơn'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <AlertTriangle className="h-4 w-4" />
              Terminal Invariant Warning (BR-001, BR-006)
            </p>
            <p className="text-[11px] text-slate-400">
              Once cancelled, reserved stock will be automatically released back to inventory, and this order can never be transitioned again.
            </p>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Lý do hủy đơn (Reason Code - Bắt buộc)
            </label>
            <select
              value={cancelReasonCode}
              onChange={(e) => setCancelReasonCode(e.target.value as OrderCancellationReasonCode)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              <option value="CUSTOMER_REQUEST">CUSTOMER_REQUEST - Khách hàng yêu cầu hủy đơn</option>
              <option value="OUT_OF_STOCK">OUT_OF_STOCK - Hết hàng tồn kho</option>
              <option value="PAYMENT_TIMEOUT">PAYMENT_TIMEOUT - Quá hạn thời gian thanh toán</option>
              <option value="SUSPECTED_FRAUD">SUSPECTED_FRAUD - Nghi ngờ giao dịch gian lận</option>
              <option value="OPERATOR_OVERRIDE">OPERATOR_OVERRIDE - Can thiệp quản trị viên</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase">
                Ghi chú hủy đơn (Tối thiểu 10 ký tự)
              </label>
              <span
                className={`text-[11px] font-mono ${
                  cancelNote.trim().length >= 10 ? 'text-emerald-400 font-semibold' : 'text-rose-400'
                }`}
              >
                {cancelNote.trim().length} / 10 ký tự tối thiểu
              </span>
            </div>
            <textarea
              rows={3}
              value={cancelNote}
              onChange={(e) => setCancelNote(e.target.value)}
              placeholder="Nhập lý do chi tiết giải trình việc hủy đơn (tối thiểu 10 ký tự)..."
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
