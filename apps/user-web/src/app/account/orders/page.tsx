'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Order, OrderStatus } from '@/types';
import { useUserStore } from '@/store/user-store';
import { formatCurrency, formatDate } from '@/lib/utils';
import { EmptyState } from '@/components/common/EmptyState';
import { OrderCardSkeleton } from '@/components/common/SkeletonLoader';
import { Package, ChevronRight, Clock, CheckCircle2, Truck, XCircle, Search } from 'lucide-react';

import { OrderStatusBadge } from '@/components/order/OrderStatusBadge';

const STATUS_TABS: { label: string; value: OrderStatus | 'ALL' }[] = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Chờ thanh toán', value: 'RESERVED' },
  { label: 'Đã thanh toán', value: 'PAID' },
  { label: 'Đang đóng gói', value: 'PACKING' },
  { label: 'Đang giao hàng', value: 'SHIPPED' },
  { label: 'Hoàn tất', value: 'COMPLETED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
];

export default function CustomerOrdersPage() {
  const { user, isAuthenticated } = useUserStore();
  const [activeTab, setActiveTab] = useState<OrderStatus | 'ALL'>('ALL');
  const [searchNumber, setSearchNumber] = useState('');

  const { data: orders = [], isLoading } = useQuery<Order[]>({
    queryKey: ['customer-orders', user?.id],
    queryFn: () => {
      const customerId = user?.id || 'cust-demo-001';
      return apiClient<Order[]>(`/api/v1/customers/${customerId}/orders`);
    },
    enabled: Boolean(user),
  });

  if (!isAuthenticated || !user) {
    return (
      <div className="py-16 text-center">
        <EmptyState
          title="Yêu cầu đăng nhập"
          description="Vui lòng đăng nhập để kiểm tra lịch sử và hành trình đơn hàng của bạn."
          actionText="Đăng nhập ngay"
          actionHref="/auth/login?returnUrl=/account/orders"
        />
      </div>
    );
  }

  // Filter orders by active status tab and search
  const filteredOrders = orders.filter((o) => {
    const matchesTab = activeTab === 'ALL' || o.status === activeTab;
    const matchesSearch =
      !searchNumber.trim() ||
      o.orderNumber.toLowerCase().includes(searchNumber.toLowerCase());
    return matchesTab && matchesSearch;
  });

  return (
    <div className="space-y-6 sm:space-y-8">
      {/* Title */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900">
          Đơn hàng của tôi
        </h1>
        <p className="text-xs sm:text-sm text-zinc-500 mt-1">
          Theo dõi hành trình xử lý, trạng thái giữ hàng và timeline chi tiết
        </p>
      </div>

      {/* Tabs & Search Filter */}
      <div className="space-y-4">
        {/* Search */}
        <div className="relative max-w-sm">
          <input
            type="text"
            placeholder="Tìm theo mã đơn hàng (ORD-...)"
            value={searchNumber}
            onChange={(e) => setSearchNumber(e.target.value)}
            className="w-full bg-white border border-zinc-200 rounded-xl pl-9 pr-4 py-2 text-xs sm:text-sm text-zinc-900 placeholder-zinc-400 focus:outline-none focus:border-zinc-900 shadow-2xs"
          />
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
        </div>

        {/* Status Filter Tabs */}
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-none">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                activeTab === tab.value
                  ? 'bg-zinc-900 text-white shadow-xs'
                  : 'bg-white border border-zinc-200 text-zinc-600 hover:bg-zinc-50'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Order List */}
      {isLoading ? (
        <div className="space-y-4">
          <OrderCardSkeleton />
          <OrderCardSkeleton />
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="py-12">
          <EmptyState
            icon={<Package className="w-8 h-8 text-zinc-400" />}
            title="Không tìm thấy đơn hàng"
            description="Bạn hiện không có đơn hàng nào ở trạng thái này."
            actionText="Mua sắm ngay"
            actionHref="/products"
          />
        </div>
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs hover:shadow-md transition space-y-4"
            >
              {/* Card Header */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-100 pb-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-bold text-zinc-900">
                      {order.orderNumber}
                    </span>
                    <span className="text-zinc-300">•</span>
                    <span className="text-xs text-zinc-500 font-mono">
                      {formatDate(order.createdAt)}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-500 mt-1">
                    Giao tới: <strong>{order.shippingAddress.recipientName}</strong> (
                    {order.shippingAddress.district}, {order.shippingAddress.city})
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <OrderStatusBadge status={order.status} />
                  <Link
                    href={`/account/orders/${order.id}`}
                    className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-900 hover:bg-zinc-100 transition"
                    aria-label="Xem chi tiết đơn hàng"
                  >
                    <ChevronRight className="w-5 h-5" />
                  </Link>
                </div>
              </div>

              {/* Items Summary preview */}
              <div className="space-y-2">
                {order.items.slice(0, 2).map((item) => (
                  <div key={item.id} className="flex items-center gap-3 text-xs">
                    <div className="w-12 h-12 rounded-lg bg-zinc-100 border border-zinc-200/60 overflow-hidden shrink-0">
                      {item.productImageSnapshot ? (
                        <img
                          src={item.productImageSnapshot}
                          alt={item.productNameSnapshot}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-[9px] text-zinc-400">
                          Ảnh
                        </div>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-zinc-900 truncate">
                        {item.productNameSnapshot}
                      </p>
                      <p className="text-[11px] text-zinc-500">
                        Mã SKU: {item.skuCodeSnapshot} | SL: {item.quantity}
                      </p>
                    </div>
                    <span className="font-bold text-zinc-900 shrink-0">
                      {formatCurrency(item.lineTotal)}
                    </span>
                  </div>
                ))}

                {order.items.length > 2 && (
                  <p className="text-[11px] text-zinc-400 italic pt-1">
                    +{order.items.length - 2} sản phẩm khác...
                  </p>
                )}
              </div>

              {/* Card Footer */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-zinc-100">
                <div className="flex items-baseline gap-2">
                  <span className="text-xs text-zinc-500">Tổng thanh toán:</span>
                  <span className="text-base font-black text-zinc-900">
                    {formatCurrency(order.grandTotalAmount)}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {order.status === 'RESERVED' && (
                    <Link
                      href={`/checkout/payment/${order.id}`}
                      className="px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold transition shadow-xs"
                    >
                      Thanh toán ngay
                    </Link>
                  )}
                  <Link
                    href={`/account/orders/${order.id}`}
                    className="px-3.5 py-1.5 rounded-xl border border-zinc-200 text-zinc-800 text-xs font-semibold hover:bg-zinc-50 transition"
                  >
                    Xem chi tiết & Timeline
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
