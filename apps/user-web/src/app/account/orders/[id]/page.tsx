'use client';

import React, { useState, use } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Order } from '@/types';
import { useUserStore } from '@/store/user-store';
import { OrderTimeline } from '@/components/order/OrderTimeline';
import { OrderCancelModal } from '@/components/order/OrderCancelModal';
import { OrderStatusBadge } from '@/components/order/OrderStatusBadge';
import { formatCurrency, formatDate } from '@/lib/utils';
import { Skeleton } from '@/components/common/SkeletonLoader';
import { EmptyState } from '@/components/common/EmptyState';
import {
  ChevronLeft,
  MapPin,
  CreditCard,
  Ban,
  AlertCircle,
  Copy,
  CheckCircle2,
  Lock,
} from 'lucide-react';
import { useToastStore } from '@/store/toast-store';

export default function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const { user, isAuthenticated } = useUserStore();
  const { showSuccess } = useToastStore();
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

  const {
    data: order,
    isLoading,
    refetch,
  } = useQuery<Order>({
    queryKey: ['order-detail', resolvedParams.id],
    queryFn: () => {
      const customerId = user?.id || 'cust-demo-001';
      return apiClient<Order>(`/api/v1/customers/${customerId}/orders/${resolvedParams.id}`);
    },
    enabled: Boolean(user),
  });

  if (!isAuthenticated || !user) {
    return (
      <div className="py-16 text-center">
        <EmptyState
          title="Yêu cầu đăng nhập"
          description="Vui lòng đăng nhập để xem thông tin chi tiết đơn hàng."
          actionText="Đăng nhập"
          actionHref="/auth/login"
        />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto py-8 space-y-6">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-64 w-full rounded-2xl" />
        <Skeleton className="h-48 w-full rounded-2xl" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="py-16">
        <EmptyState
          title="Không tìm thấy đơn hàng"
          description="Đơn hàng không tồn tại hoặc bạn không có quyền truy cập."
          actionText="Về danh sách đơn hàng"
          actionHref="/account/orders"
        />
      </div>
    );
  }

  // Business Rules BR-001, BR-006, BR-007:
  // Customer cancellation permitted ONLY prior to PACKING (i.e. in RESERVED or PAID)
  const canCancelOrder = order.status === 'RESERVED' || order.status === 'PAID';
  const isPastCutoff = ['PACKING', 'SHIPPED', 'COMPLETED'].includes(order.status);
  const isCancelled = order.status === 'CANCELLED';

  return (
    <div className="max-w-4xl mx-auto py-6 space-y-8">
      {/* Top back navigation */}
      <div className="flex items-center justify-between">
        <Link
          href="/account/orders"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-zinc-600 hover:text-zinc-900 transition"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Quay lại danh sách đơn hàng</span>
        </Link>

        {order.status === 'RESERVED' && (
          <Link
            href={`/checkout/payment/${order.id}`}
            className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold shadow-xs transition"
          >
            Thanh toán đơn hàng
          </Link>
        )}
      </div>

      {/* Order Main Header */}
      <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-lg sm:text-xl font-bold font-mono text-zinc-900">
              {order.orderNumber}
            </span>
            <button
              onClick={() => {
                navigator.clipboard.writeText(order.orderNumber);
                showSuccess('Đã sao chép mã đơn hàng');
              }}
              className="text-zinc-400 hover:text-zinc-700 p-1"
              aria-label="Sao chép"
            >
              <Copy className="w-3.5 h-3.5" />
            </button>
          </div>
          <p className="text-xs text-zinc-400 mt-1 font-mono">
            Thời gian tạo: {formatDate(order.createdAt)}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <OrderStatusBadge status={order.status} />

          {/* Cancel button or cutoff explanation */}
          {canCancelOrder ? (
            <button
              onClick={() => setIsCancelModalOpen(true)}
              className="px-3.5 py-1.5 rounded-xl border border-rose-200 text-rose-700 hover:bg-rose-50 text-xs font-semibold transition"
            >
              Hủy đơn hàng
            </button>
          ) : isPastCutoff ? (
            <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
              <Lock className="w-3.5 h-3.5" />
              <span>Đã khóa hủy đơn</span>
            </div>
          ) : null}
        </div>
      </div>

      {/* Explanatory cutoff callout if past packing */}
      {isPastCutoff && (
        <div className="flex items-start gap-3 p-4 rounded-xl bg-zinc-100 border border-zinc-200 text-xs text-zinc-600">
          <AlertCircle className="w-4 h-4 text-zinc-500 shrink-0 mt-0.5" />
          <p>
            <strong>Chính sách hủy đơn (BR-006 & BR-007):</strong> Đơn hàng đã chuyển sang trạng thái <strong>{order.status}</strong> (đang đóng gói hoặc vận chuyển) nên quý khách không thể tự hủy trên hệ thống. Nếu có thắc mắc, vui lòng liên hệ tổng đài CSKH để được trợ giúp.
          </p>
        </div>
      )}

      {/* 1. Visual Stepper & Chronological Order Timeline */}
      <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs">
        <OrderTimeline status={order.status} timeline={order.timeline} />
      </div>

      {/* 2. Grid: Shipping Address Snapshot (BR-017) & Monetary Breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Shipping Address Snapshot */}
        <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-3">
          <div className="flex items-center gap-2 text-zinc-900 border-b border-zinc-100 pb-2">
            <MapPin className="w-4 h-4" />
            <h3 className="text-sm font-bold">Địa chỉ giao hàng (Snapshotted)</h3>
          </div>
          <div className="text-xs text-zinc-700 space-y-1">
            <p className="font-semibold text-zinc-900">{order.shippingAddress.recipientName}</p>
            <p className="text-zinc-500">{order.shippingAddress.phone}</p>
            <p className="leading-relaxed">
              {order.shippingAddress.line1}, {order.shippingAddress.ward},{' '}
              {order.shippingAddress.district}, {order.shippingAddress.city}
            </p>
          </div>
          <div className="pt-2 text-[10px] text-zinc-400 font-mono border-t border-zinc-100">
            * Địa chỉ được lưu giữ bất biến tại thời điểm đặt hàng per BR-017.
          </div>
        </div>

        {/* Payment & Breakdown Summary */}
        <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-3">
          <div className="flex items-center gap-2 text-zinc-900 border-b border-zinc-100 pb-2">
            <CreditCard className="w-4 h-4" />
            <h3 className="text-sm font-bold">Thanh toán & Chi phí</h3>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between text-zinc-600">
              <span>Phương thức:</span>
              <span className="font-semibold text-zinc-900">{order.paymentMethod || 'MOCK_GATEWAY'}</span>
            </div>
            <div className="flex justify-between text-zinc-600">
              <span>Tạm tính:</span>
              <span className="font-semibold text-zinc-900">
                {formatCurrency(order.subtotalAmount)}
              </span>
            </div>
            <div className="flex justify-between text-zinc-600">
              <span>Phí vận chuyển:</span>
              <span>
                {order.shippingFeeAmount === 0 ? (
                  <span className="text-emerald-600 font-semibold uppercase text-[11px]">
                    Miễn phí
                  </span>
                ) : (
                  formatCurrency(order.shippingFeeAmount)
                )}
              </span>
            </div>
            <div className="pt-2 border-t border-zinc-100 flex justify-between items-baseline">
              <span className="text-xs font-bold text-zinc-900">Tổng thanh toán:</span>
              <span className="text-base font-black text-zinc-900">
                {formatCurrency(order.grandTotalAmount)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Snapshotted Order Items (BR-013, FR-028) */}
      <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-4">
        <h3 className="text-sm font-bold text-zinc-900 border-b border-zinc-100 pb-2">
          Chi tiết sản phẩm đơn hàng (Order Items Snapshot)
        </h3>

        <div className="divide-y divide-zinc-100">
          {order.items.map((item) => (
            <div key={item.id} className="py-3 flex items-center justify-between gap-4 text-xs">
              <div className="flex items-center gap-3">
                <div className="w-14 h-14 rounded-xl bg-zinc-100 border border-zinc-200/60 overflow-hidden shrink-0">
                  {item.productImageSnapshot ? (
                    <img
                      src={item.productImageSnapshot}
                      alt={item.productNameSnapshot}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-[10px] text-zinc-400">
                      No img
                    </div>
                  )}
                </div>
                <div>
                  <h4 className="font-semibold text-zinc-900">{item.productNameSnapshot}</h4>
                  <p className="text-[11px] text-zinc-500 font-mono">
                    SKU: {item.skuCodeSnapshot}
                  </p>
                  {item.attributeSnapshot && (
                    <p className="text-[11px] text-zinc-400">
                      {Object.entries(item.attributeSnapshot)
                        .map(([k, v]) => `${k}: ${v}`)
                        .join(' | ')}
                    </p>
                  )}
                </div>
              </div>

              <div className="text-right">
                <p className="font-bold text-zinc-900 text-sm">
                  {formatCurrency(item.lineTotal)}
                </p>
                <p className="text-[11px] text-zinc-400">
                  {formatCurrency(item.unitPriceSnapshot)} x {item.quantity}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Order Cancel Modal */}
      {isCancelModalOpen && (
        <OrderCancelModal
          order={order}
          isOpen={isCancelModalOpen}
          onClose={() => setIsCancelModalOpen(false)}
          onSuccess={() => refetch()}
        />
      )}
    </div>
  );
}
