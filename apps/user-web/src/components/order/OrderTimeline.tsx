'use client';

import React from 'react';
import { OrderStatus, OrderTimelineEvent } from '@/types';
import {
  CheckCircle2,
  Clock,
  Package,
  Truck,
  CheckCheck,
  AlertCircle,
  XCircle,
  User,
  Cpu,
  Shield,
} from 'lucide-react';
import { formatDate } from '@/lib/utils';

interface OrderTimelineProps {
  status: OrderStatus;
  timeline?: OrderTimelineEvent[];
}

const ORDER_STEPS: { status: OrderStatus; label: string; icon: React.ElementType }[] = [
  { status: 'RESERVED', label: 'Đặt hàng & Giữ chỗ', icon: Clock },
  { status: 'PAID', label: 'Đã thanh toán', icon: CheckCircle2 },
  { status: 'PACKING', label: 'Đang đóng gói', icon: Package },
  { status: 'SHIPPED', label: 'Đang vận chuyển', icon: Truck },
  { status: 'COMPLETED', label: 'Giao hàng thành công', icon: CheckCheck },
];

export function OrderTimeline({ status, timeline = [] }: OrderTimelineProps) {
  const isCancelled = status === 'CANCELLED';
  const isFailed = status === 'PAYMENT_FAILED';
  const isExpired = status === 'EXPIRED';
  const isAbnormal = isCancelled || isFailed || isExpired;

  // Determine active step index
  const getStepIndex = (st: OrderStatus): number => {
    switch (st) {
      case 'RESERVED':
        return 0;
      case 'PAID':
        return 1;
      case 'PACKING':
        return 2;
      case 'SHIPPED':
        return 3;
      case 'COMPLETED':
        return 4;
      default:
        return -1;
    }
  };

  const currentStepIdx = getStepIndex(status);

  const getActorBadge = (actorType: string) => {
    switch (actorType) {
      case 'CUSTOMER':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-zinc-100 text-zinc-700">
            <User className="w-3 h-3" />
            Khách hàng
          </span>
        );
      case 'BACK_OFFICE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Shield className="w-3 h-3" />
            Nhân viên kho / Ops
          </span>
        );
      case 'SYSTEM':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <Cpu className="w-3 h-3" />
            Hệ thống tự động
          </span>
        );
    }
  };

  return (
    <div className="space-y-8">
      {/* 1. Visual Stepper Bar */}
      {!isAbnormal ? (
        <div className="relative py-4">
          <div className="flex items-center justify-between relative z-10">
            {ORDER_STEPS.map((step, idx) => {
              const Icon = step.icon;
              const isPast = idx < currentStepIdx;
              const isCurrent = idx === currentStepIdx;

              return (
                <div key={step.status} className="flex flex-col items-center text-center flex-1">
                  <div
                    className={`w-10 h-10 sm:w-12 sm:h-12 rounded-full flex items-center justify-center transition-all ${
                      isPast
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : isCurrent
                        ? 'bg-zinc-900 text-white ring-4 ring-zinc-200 shadow-md animate-pulse'
                        : 'bg-zinc-100 text-zinc-400 border border-zinc-200'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                  </div>
                  <span
                    className={`mt-2 text-[11px] sm:text-xs font-semibold max-w-[80px] sm:max-w-none leading-tight ${
                      isCurrent ? 'text-zinc-900 font-bold' : isPast ? 'text-emerald-700' : 'text-zinc-400'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Progress bar background line */}
          <div className="absolute top-9 sm:top-10 left-8 right-8 h-0.5 bg-zinc-200 -z-0">
            <div
              className="h-full bg-emerald-600 transition-all duration-500"
              style={{
                width: `${(Math.max(0, currentStepIdx) / (ORDER_STEPS.length - 1)) * 100}%`,
              }}
            />
          </div>
        </div>
      ) : (
        /* Abnormal Status Banner */
        <div className="p-4 rounded-xl border flex items-center gap-3 bg-rose-50 border-rose-200 text-rose-800">
          <XCircle className="w-6 h-6 text-rose-600 shrink-0" />
          <div>
            <h4 className="text-sm font-bold">
              {isCancelled && 'Đơn hàng đã được hủy'}
              {isFailed && 'Thanh toán không thành công'}
              {isExpired && 'Đơn hàng đã hết hạn giữ chỗ (Timeout)'}
            </h4>
            <p className="text-xs text-rose-700 mt-0.5">
              {isCancelled && 'Tồn kho đã được giải phóng tự động về kho hàng (BR-001/BR-003).'}
              {isFailed && 'Giao dịch thanh toán thất bại. Bạn có thể tiến hành đặt lại đơn hàng mới.'}
              {isExpired && 'Thời gian giữ chỗ quá hạn mà chưa nhận được xác nhận thanh toán.'}
            </p>
          </div>
        </div>
      )}

      {/* 2. Detailed Immutable Chronological Timeline Events */}
      <div className="space-y-4">
        <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-900 border-b border-zinc-200 pb-2">
          Nhật ký trạng thái chi tiết (Order Timeline Events)
        </h4>

        {timeline.length === 0 ? (
          <p className="text-xs text-zinc-400 italic">Chưa có nhật ký sự kiện nào được ghi nhận.</p>
        ) : (
          <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-zinc-200">
            {timeline.map((evt) => (
              <div key={evt.id} className="relative group">
                {/* Timeline node dot */}
                <div className="absolute -left-6 top-1 w-4 h-4 rounded-full bg-white border-2 border-zinc-900 group-hover:scale-110 transition-transform" />

                <div className="bg-zinc-50/70 hover:bg-zinc-50 border border-zinc-200/80 rounded-xl p-3.5 transition space-y-1.5 shadow-2xs">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-zinc-900">
                        {evt.fromStatus ? `${evt.fromStatus} → ${evt.toStatus}` : evt.toStatus}
                      </span>
                      {getActorBadge(evt.actorType)}
                    </div>
                    <span className="text-[11px] text-zinc-400 font-mono">
                      {formatDate(evt.occurredAt)}
                    </span>
                  </div>

                  {evt.note && <p className="text-xs text-zinc-600 leading-relaxed">{evt.note}</p>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
