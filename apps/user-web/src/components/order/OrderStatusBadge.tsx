import React from 'react';
import { OrderStatus } from '@/types';

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  const getBadgeStyle = () => {
    switch (status) {
      case 'RESERVED':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'PAID':
        return 'bg-sky-50 text-sky-700 border-sky-200';
      case 'PACKING':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'SHIPPED':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'COMPLETED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'CANCELLED':
      case 'PAYMENT_FAILED':
      case 'EXPIRED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-zinc-50 text-zinc-700 border-zinc-200';
    }
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${getBadgeStyle()}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current" />
      <span>{status}</span>
    </span>
  );
}
