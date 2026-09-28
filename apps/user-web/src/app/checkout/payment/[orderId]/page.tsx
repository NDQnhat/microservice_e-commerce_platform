'use client';

import React, { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  CreditCard,
  CheckCircle2,
  XCircle,
  Clock,
  AlertTriangle,
  Loader2,
  ArrowRight,
  ShieldCheck,
  RefreshCw,
  Copy,
} from 'lucide-react';
import { apiClient, ApiClientError } from '@/lib/api-client';
import { Order, PaymentCallbackPayload } from '@/types';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { formatCurrency, generateUUID } from '@/lib/utils';
import { Skeleton } from '@/components/common/SkeletonLoader';

export default function PaymentGatewaySimulationPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const resolvedParams = use(params);
  const router = useRouter();
  const { user } = useUserStore();
  const { showSuccess, showError, showWarning } = useToastStore();

  const [isProcessing, setIsProcessing] = useState(false);
  const [duplicateTestSuccess, setDuplicateTestSuccess] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(900); // 15 mins reservation countdown

  // Query order details
  const {
    data: order,
    isLoading,
    refetch,
  } = useQuery<Order>({
    queryKey: ['order-payment', resolvedParams.orderId],
    queryFn: () => {
      const customerId = user?.id || 'cust-demo-001';
      return apiClient<Order>(`/api/v1/customers/${customerId}/orders/${resolvedParams.orderId}`);
    },
    refetchInterval: 3000, // Poll order status
  });

  // Countdown timer effect
  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatCountdown = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // 1. Simulate Callback (API-PAY-001)
  const handleSimulatePayment = async (result: 'SUCCESS' | 'FAILED') => {
    if (!order) return;
    setIsProcessing(true);
    setDuplicateTestSuccess(null);

    const payload: PaymentCallbackPayload = {
      order_id: order.id,
      provider_reference: 'TXN-' + generateUUID().slice(0, 12).toUpperCase(),
      result,
      amount: order.grandTotalAmount,
    };

    try {
      await apiClient('/api/v1/payments/callback', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      if (result === 'SUCCESS') {
        showSuccess('Thanh toán thành công', `Đơn hàng ${order.orderNumber} đã chuyển sang trạng thái PAID.`);
      } else {
        showWarning('Thanh toán thất bại', `Đơn hàng ${order.orderNumber} đã chuyển sang PAYMENT_FAILED.`);
      }
      await refetch();
    } catch (err: unknown) {
      if (err instanceof ApiClientError && err.problem.code === 'DUPLICATE_PAYMENT') {
        setDuplicateTestSuccess(err.problem.detail);
        showWarning('Phát hiện trùng lặp giao dịch (BR-002)', err.problem.detail);
      } else {
        showError('Lỗi xử lý callback', err);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  // 2. Simulate Duplicate Callback Test (BR-002)
  const handleSimulateDuplicateCallback = async () => {
    if (!order) return;
    setIsProcessing(true);
    setDuplicateTestSuccess(null);

    const payload: PaymentCallbackPayload = {
      order_id: order.id,
      provider_reference: 'TXN-DUPLICATE-' + generateUUID().slice(0, 8),
      result: 'SUCCESS',
      amount: order.grandTotalAmount,
    };

    try {
      await apiClient('/api/v1/payments/callback', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      showSuccess('Callback xử lý');
      await refetch();
    } catch (err: unknown) {
      if (err instanceof ApiClientError && err.problem.code === 'DUPLICATE_PAYMENT') {
        setDuplicateTestSuccess(
          `[BR-002 INVARIANT VERIFIED]: Backend từ chối xử lý lần 2! RFC 7807 Error Code: ${err.problem.code} - ${err.problem.detail}`
        );
        showWarning('Bảo đảm BR-002 hoạt động chuẩn xác', err.problem.detail);
      } else {
        showError('Lỗi kiểm tra', err);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto py-12 space-y-6">
        <Skeleton className="h-10 w-1/2 mx-auto" />
        <Skeleton className="h-64 w-full rounded-2xl" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-md mx-auto py-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-zinc-900">Không tìm thấy thông tin thanh toán</h2>
        <Link href="/products" className="inline-block py-2.5 px-6 rounded-xl bg-zinc-900 text-white text-xs font-semibold">
          Quay lại mua sắm
        </Link>
      </div>
    );
  }

  const isPaid = order.status === 'PAID';
  const isPending = order.status === 'RESERVED';
  const isFailed = order.status === 'PAYMENT_FAILED';

  return (
    <div className="max-w-3xl mx-auto py-6 space-y-8">
      {/* Header */}
      <div className="text-center space-y-2">
        <span className="text-[11px] font-mono uppercase tracking-widest text-zinc-500 bg-zinc-100 px-3 py-1 rounded-full border border-zinc-200">
          Payment Gateway Simulation Console (Sandbox)
        </span>
        <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-zinc-900">
          Xác thực Thanh toán Đơn hàng
        </h1>
        <p className="text-xs sm:text-sm text-zinc-500 max-w-md mx-auto">
          Mô phỏng quy trình trao đổi dữ liệu an toàn giữa Khách hàng, Cổng thanh toán (PGW) và Hệ thống Microservices.
        </p>
      </div>

      {/* Order Card Overview */}
      <div className="bg-white rounded-2xl border border-zinc-200/80 p-6 shadow-xs space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-zinc-100">
          <div>
            <span className="text-xs text-zinc-400">Mã đơn hàng:</span>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-base font-bold font-mono text-zinc-900">{order.orderNumber}</span>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(order.orderNumber);
                  showSuccess('Đã sao chép mã đơn hàng');
                }}
                className="text-zinc-400 hover:text-zinc-700"
                aria-label="Sao chép"
              >
                <Copy className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          <div className="text-right">
            <span className="text-xs text-zinc-400">Số tiền cần thanh toán:</span>
            <div className="text-xl font-black text-zinc-900 mt-0.5">
              {formatCurrency(order.grandTotalAmount)}
            </div>
          </div>
        </div>

        {/* Current Order Status Indicator */}
        <div className="flex items-center justify-between p-4 rounded-xl bg-zinc-50 border border-zinc-200/80">
          <div className="flex items-center gap-3">
            {isPaid ? (
              <CheckCircle2 className="w-6 h-6 text-emerald-600" />
            ) : isFailed ? (
              <XCircle className="w-6 h-6 text-rose-600" />
            ) : (
              <Clock className="w-6 h-6 text-amber-500 animate-spin" />
            )}
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs text-zinc-500 font-medium">Trạng thái hiện tại:</span>
                <span
                  className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                    isPaid
                      ? 'bg-emerald-100 text-emerald-800'
                      : isFailed
                      ? 'bg-rose-100 text-rose-800'
                      : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  {order.status}
                </span>
              </div>
              <p className="text-xs text-zinc-600 mt-0.5">
                {isPaid && 'Đơn hàng đã được xác nhận thanh toán thành công!'}
                {isPending && 'Đang giữ chỗ tồn kho (ACTIVE). Chờ callback từ cổng thanh toán...'}
                {isFailed && 'Giao dịch thất bại. Tồn kho đã được hoàn trả (RELEASED).'}
              </p>
            </div>
          </div>

          {isPending && (
            <div className="text-right hidden sm:block">
              <span className="text-[10px] text-zinc-400 uppercase">Thời gian giữ chỗ:</span>
              <p className="text-sm font-bold font-mono text-zinc-900">{formatCountdown(countdown)}</p>
            </div>
          )}
        </div>

        {/* Duplicate test result banner */}
        {duplicateTestSuccess && (
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>Xác thực tính Idempotent thành công!</span>
            </p>
            <p className="leading-relaxed font-mono">{duplicateTestSuccess}</p>
          </div>
        )}

        {/* Simulation Action Controls (Sandbox) */}
        <div className="space-y-3 pt-2">
          <h3 className="text-xs font-bold text-zinc-900 uppercase tracking-wide">
            Bảng điều khiển mô phỏng kết quả Callback (API-PAY-001):
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <button
              onClick={() => handleSimulatePayment('SUCCESS')}
              disabled={isProcessing || isPaid}
              className="py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-xs transition disabled:opacity-40"
            >
              {isProcessing ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <CheckCircle2 className="w-4 h-4" />
              )}
              <span>Mô phỏng Thanh toán Thành công (SUCCESS)</span>
            </button>

            <button
              onClick={() => handleSimulatePayment('FAILED')}
              disabled={isProcessing || isPaid || isFailed}
              className="py-3 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-xs transition disabled:opacity-40"
            >
              {isProcessing ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <XCircle className="w-4 h-4" />
              )}
              <span>Mô phỏng Thanh toán Thất bại (FAILED)</span>
            </button>
          </div>

          {/* Test BR-002 Button */}
          <div className="pt-2">
            <button
              onClick={handleSimulateDuplicateCallback}
              disabled={isProcessing || !isPaid}
              className="w-full py-2.5 px-4 rounded-xl border-2 border-indigo-600 text-indigo-700 hover:bg-indigo-50 text-xs font-bold flex items-center justify-center gap-2 transition disabled:opacity-40"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Thử gửi lại Callback Thành công lần 2 (Kiểm tra chống trùng lặp BR-002)</span>
            </button>
            <p className="text-[10px] text-zinc-400 mt-1 text-center">
              * Quy tắc BR-002: Một đơn hàng chỉ được có duy nhất 1 giao dịch SUCCEEDED. Callback trùng lặp phải trả về mã lỗi 422 DUPLICATE_PAYMENT.
            </p>
          </div>
        </div>

        {/* Navigation to Order Tracking */}
        <div className="pt-4 border-t border-zinc-100 flex flex-col sm:flex-row items-center justify-between gap-3">
          <Link
            href={`/account/orders/${order.id}`}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 py-2.5 px-5 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs font-bold transition shadow-xs"
          >
            <span>Xem tiến độ đơn hàng & Timeline</span>
            <ArrowRight className="w-4 h-4" />
          </Link>

          <Link
            href="/products"
            className="text-xs font-semibold text-zinc-500 hover:text-zinc-900 transition"
          >
            Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    </div>
  );
}
