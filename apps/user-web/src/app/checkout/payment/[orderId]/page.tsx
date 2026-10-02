'use client';

import React, { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
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
  QrCode,
} from 'lucide-react';
import { apiClient, ApiClientError } from '@/lib/api-client';
import { Order, PaymentCallbackPayload, BusinessConfiguration } from '@/types';
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
  const searchParams = useSearchParams();
  const isVnpay = searchParams.get('gateway') === 'vnpay';

  const { user } = useUserStore();
  const { showSuccess, showError, showWarning } = useToastStore();

  const [isProcessing, setIsProcessing] = useState(false);
  const [duplicateTestSuccess, setDuplicateTestSuccess] = useState<string | null>(null);
  const [lateCallbackTestResult, setLateCallbackTestResult] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null); // Dynamic TTL from config (BR-003, ORD-T04)

  // Query order details
  const {
    data: order,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<Order>({
    queryKey: ['order-payment', resolvedParams.orderId],
    queryFn: () => {
      const customerId = user?.id || 'cust-demo-001';
      return apiClient<Order>(`/api/v1/customers/${customerId}/orders/${resolvedParams.orderId}`);
    },
    refetchInterval: 3000, // Poll order status
  });

  // FIX-M5: Dynamic TTL from Business Configuration
  useEffect(() => {
    async function loadTTL() {
      try {
        const config = await apiClient<any>('/api/v1/configurations');
        let ttlMinutes = 15;
        if (Array.isArray(config)) {
          const item = config.find((i: any) => {
            const k = (i.configKey || i.key || '').toLowerCase();
            return k.includes('reservation') || k.includes('timeout') || k.includes('ttl');
          });
          if (item && !isNaN(Number(item.configValue || item.value))) {
            ttlMinutes = Number(item.configValue || item.value);
          }
        } else if (config?.reservation_timeout_minutes) {
          ttlMinutes = config.reservation_timeout_minutes;
        }
        const ttlSeconds = (ttlMinutes ?? 15) * 60;
        setCountdown(ttlSeconds);
      } catch {
        setCountdown(15 * 60); // Fallback 15 phút nếu config API fail
      }
    }
    loadTTL();
  }, []);

  // STT 8: Countdown timer effect
  useEffect(() => {
    if (countdown === null || countdown <= 0) return;
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [countdown]);

  // STT 8: Automatically trigger EXPIRED status on backend when countdown hits 00:00
  useEffect(() => {
    if (countdown === 0 && order && order.status === 'RESERVED') {
      const customerId = user?.id || 'cust-demo-001';
      apiClient<Order>(`/api/v1/customers/${customerId}/orders/${order.id}/expire`, {
        method: 'POST',
      })
        .then(() => refetch())
        .catch(() => {});
    }
  }, [countdown, order, user?.id, refetch]);

  const formatCountdown = (seconds: number | null) => {
    if (seconds === null) return '--:--';
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
      provider_reference: (isVnpay ? 'VNPAY-' : 'TXN-') + generateUUID().slice(0, 12).toUpperCase(),
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

  // 3. Simulate Late/Stale Callback Test (FR-010 Step 5, FIX-C3)
  const handleSimulateLateCallback = async () => {
    if (!order) return;
    setIsProcessing(true);
    setLateCallbackTestResult(null);

    const payload: PaymentCallbackPayload = {
      order_id: order.id,
      provider_reference: 'TXN-LATE-' + generateUUID().slice(0, 8),
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
      if (err instanceof ApiClientError && err.problem.code === 'LATE_OR_STALE_PAYMENT_CALLBACK') {
        setLateCallbackTestResult(
          `[FR-010 LATE CALLBACK EXCEPTION RECORDED]: ${err.problem.detail}`
        );
        showWarning('Callback muộn được ghi nhận exception (FR-010)', err.problem.detail);
      } else {
        showError('Lỗi kiểm tra callback muộn', err);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  // FIX-M13: Error state render with 404 distinction and refetch
  if (isError) {
    const apiErr = error as ApiClientError | Error;
    const isNotFound = apiErr instanceof ApiClientError && apiErr.problem?.status === 404;
    return (
      <div className="max-w-md mx-auto py-16 text-center space-y-4">
        <h2 className="text-xl font-bold text-zinc-900">
          {isNotFound ? 'Không tìm thấy đơn hàng' : 'Lỗi tải thông tin thanh toán'}
        </h2>
        <p className="text-sm text-zinc-500">
          {isNotFound
            ? 'Đơn hàng này không tồn tại hoặc bạn không có quyền truy cập.'
            : 'Không thể kết nối đến máy chủ. Vui lòng tải lại trang.'}
        </p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 rounded-xl bg-zinc-900 text-white text-xs font-semibold hover:bg-zinc-800 transition"
        >
          Thử lại
        </button>
      </div>
    );
  }

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
  const isExpired = order.status === 'EXPIRED' || countdown === 0;
  const isFailed = order.status === 'PAYMENT_FAILED';
  const isPending = order.status === 'RESERVED' && !isExpired;

  return (
    <div className="max-w-3xl mx-auto py-6 space-y-8">
      {/* Header */}
      <div className="text-center space-y-2">
        <div className="flex items-center justify-center gap-2">
          <span className="text-[11px] font-mono uppercase tracking-widest text-zinc-500 bg-zinc-100 px-3 py-1 rounded-full border border-zinc-200">
            {isVnpay ? 'VNPay Gateway Simulation Console (Sandbox)' : 'Payment Gateway Simulation Console (Sandbox)'}
          </span>
          {isVnpay && (
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
              VNPay QR
            </span>
          )}
        </div>
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
            ) : isExpired ? (
              <AlertTriangle className="w-6 h-6 text-rose-600" />
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
                      : isExpired || isFailed
                      ? 'bg-rose-100 text-rose-800'
                      : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  {isExpired ? 'EXPIRED' : order.status}
                </span>
              </div>
              <p className="text-xs text-zinc-600 mt-0.5">
                {isPaid && 'Đơn hàng đã được xác nhận thanh toán thành công!'}
                {isPending && 'Đang giữ chỗ tồn kho (ACTIVE). Chờ callback từ cổng thanh toán...'}
                {isExpired && 'Đơn hàng đã hết hạn giữ chỗ (EXPIRED). Tồn kho đã giải phóng.'}
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

        {/* STT 8: Prominent Red Alert Banner on EXPIRED */}
        {isExpired && (
          <div className="p-4 rounded-xl bg-rose-50 border-2 border-rose-300 text-rose-900 space-y-1.5 animate-in fade-in">
            <div className="flex items-center gap-2 font-bold text-sm text-rose-700">
              <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0" />
              <span>Đơn hàng đã hết hạn giữ chỗ (EXPIRED)</span>
            </div>
            <p className="text-xs text-rose-800 leading-relaxed">
              Tồn kho đã được giải phóng theo quy tắc BR-003. Toàn bộ thao tác thanh toán cho đơn hàng này đã bị vô hiệu hóa để bảo đảm tính nhất quán dữ liệu.
            </p>
            <div className="pt-1">
              <Link
                href="/products"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-rose-700 hover:text-rose-900 underline"
              >
                <span>Quay lại trang sản phẩm để đặt đơn mới</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        )}

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

        {/* Late callback test result banner (FIX-C3) */}
        {lateCallbackTestResult && (
          <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              <span>Ghi nhận Exception Late Callback (FR-010)!</span>
            </p>
            <p className="leading-relaxed font-mono">{lateCallbackTestResult}</p>
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
              disabled={isProcessing || isPaid || isExpired}
              className="py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-xs transition disabled:opacity-40 disabled:cursor-not-allowed"
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
              disabled={isProcessing || isPaid || isFailed || isExpired}
              className="py-3 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-xs transition disabled:opacity-40 disabled:cursor-not-allowed"
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
              className="w-full py-2.5 px-4 rounded-xl border-2 border-indigo-600 text-indigo-700 hover:bg-indigo-50 text-xs font-bold flex items-center justify-center gap-2 transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Thử gửi lại Callback Thành công lần 2 (Kiểm tra chống trùng lặp BR-002)</span>
            </button>
            <p className="text-[10px] text-zinc-400 mt-1 text-center">
              * Quy tắc BR-002: Một đơn hàng chỉ được có duy nhất 1 giao dịch SUCCEEDED. Callback trùng lặp phải trả về mã lỗi 422 DUPLICATE_PAYMENT.
            </p>
          </div>

          {/* Test Late Callback Button (FIX-C3) */}
          {(isExpired || isFailed || order.status === 'CANCELLED') && (
            <div className="pt-2">
              <button
                onClick={handleSimulateLateCallback}
                disabled={isProcessing}
                className="w-full py-2.5 px-4 rounded-xl border-2 border-amber-600 text-amber-700 hover:bg-amber-50 text-xs font-bold flex items-center justify-center gap-2 transition disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {isProcessing ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Clock className="w-4 h-4" />
                )}
                <span>Test Late Callback (sau khi EXPIRED/CANCELLED)</span>
              </button>
              <p className="text-[10px] text-zinc-400 mt-1 text-center">
                * Quy tắc FR-010: Callback thành công gửi tới đơn hàng đã kết thúc (Terminal State) sẽ bị từ chối với mã lỗi 422 LATE_OR_STALE_PAYMENT_CALLBACK và ghi nhận exception.
              </p>
            </div>
          )}
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
