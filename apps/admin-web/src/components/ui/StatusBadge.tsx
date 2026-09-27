import React from 'react';

interface StatusBadgeProps {
  status: string;
  variant?: 'emerald' | 'amber' | 'rose' | 'indigo' | 'slate' | 'cyan' | 'auto';
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  variant = 'auto',
  className = '',
}) => {
  let resolvedVariant = variant;

  if (resolvedVariant === 'auto') {
    const s = status.toUpperCase();
    if (
      s === 'PAID' ||
      s === 'RESOLVED' ||
      s === 'COMPLETED' ||
      s === 'DELIVERED' ||
      s === 'CONFIRMED' ||
      s === 'ACTIVE' ||
      s === 'SUCCESS' ||
      s === 'UP'
    ) {
      resolvedVariant = 'emerald';
    } else if (
      s === 'INVESTIGATING' ||
      s === 'PENDING' ||
      s === 'PACKING' ||
      s === 'RESERVED' ||
      s === 'LOW_STOCK'
    ) {
      resolvedVariant = 'amber';
    } else if (
      s === 'OPEN' ||
      s === 'FAILED' ||
      s === 'CANCELLED' ||
      s === 'PAYMENT_FAILED' ||
      s === 'DELIVERY_FAILED' ||
      s === 'CRITICAL' ||
      s === 'EXPIRED'
    ) {
      resolvedVariant = 'rose';
    } else if (s === 'SHIPPED' || s === 'CREATED') {
      resolvedVariant = 'indigo';
    } else if (s === 'RETURNED' || s === 'REFUNDED') {
      resolvedVariant = 'cyan';
    } else {
      resolvedVariant = 'slate';
    }
  }

  const variantStyles = {
    emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    amber: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    rose: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    indigo: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    cyan: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
    slate: 'bg-slate-700/30 text-slate-300 border-slate-700/50',
  };

  const dotStyles = {
    emerald: 'bg-emerald-400',
    amber: 'bg-amber-400',
    rose: 'bg-rose-400',
    indigo: 'bg-indigo-400',
    cyan: 'bg-cyan-400',
    slate: 'bg-slate-400',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${variantStyles[resolvedVariant]} ${className}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotStyles[resolvedVariant]} animate-pulse`} />
      {status}
    </span>
  );
};
