'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ShoppingBag,
  ArrowRight,
  ArrowLeft,
  Truck,
  ShieldCheck,
  Trash2,
  AlertTriangle,
  Loader2,
} from 'lucide-react';
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { useToastStore } from '@/store/toast-store';
import { CartItemRow } from '@/components/cart/CartItemRow';
import { EmptyState } from '@/components/common/EmptyState';
import { apiClient } from '@/lib/api-client';
import { Product } from '@/types';
import { formatCurrency } from '@/lib/utils';

export default function CartPage() {
  const router = useRouter();
  const {
    items,
    itemCount,
    subtotal,
    clearCart,
    loadCart,
    freeShippingThreshold,
    standardShippingFee,
  } = useCartStore();
  const { showError, showWarning } = useToastStore();

  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [validationIssues, setValidationIssues] = useState<string[]>([]);

  // STT 12: Dynamic freeship and shipping fee configuration
  const threshold = freeShippingThreshold || 500000;
  const standardFee = standardShippingFee || 30000;
  const isFreeShipping = subtotal >= threshold;
  const shippingFee = isFreeShipping || items.length === 0 ? 0 : standardFee;
  const grandTotal = subtotal + shippingFee;
  const amountNeeded = Math.max(0, threshold - subtotal);
  const freeShippingProgress = Math.min(100, Math.round((subtotal / threshold) * 100));

  // STT 11: Confirm clear all
  const handleConfirmClear = async () => {
    setShowClearConfirm(false);
    await clearCart();
  };

  // FIX-H2: Pre-checkout Stock and Price Re-validation via Real-Time API Call
  const handleProceedToCheckout = async () => {
    setIsValidating(true);
    setValidationIssues([]);
    const issues: string[] = [];

    // Gọi API để lấy state cart mới nhất từ server (server sẽ validate stock)
    try {
      const userId = useUserStore.getState().user?.id || 'cust-demo-001';
      await loadCart(userId); // Refresh cart từ server
      const currentItems = useCartStore.getState().items; // Lấy items đã sync

      for (const item of currentItems) {
        // Gọi API lấy product detail để có stock mới nhất
        const product = await apiClient<Product>(`/api/v1/products/${item.productId}`);
        const sku = product?.skus?.find((s) => s.id === item.skuId);

        if (!sku) {
          issues.push(`Sản phẩm "${item.productName}" hiện không còn kinh doanh.`);
          continue;
        }
        const availableStock = sku.inventory?.quantityAvailable ?? 0;
        if (availableStock <= 0) {
          issues.push(`Sản phẩm "${item.productName}" hiện đã hết hàng.`);
        } else if (item.quantity > availableStock) {
          issues.push(`"${item.productName}": chỉ còn ${availableStock} cái (bạn chọn ${item.quantity}).`);
        }
        const currentPrice = sku.salePrice ?? sku.price;
        if (currentPrice !== item.unitPrice) {
          issues.push(`Giá "${item.productName}" thay đổi: ${formatCurrency(item.unitPrice)} → ${formatCurrency(currentPrice)}.`);
        }
      }
    } catch {
      // Nếu API lỗi, fallback sang error message nhưng không block UI
      issues.push('Không thể xác minh tồn kho thời gian thực. Vui lòng thử lại.');
    }

    setIsValidating(false);
    if (issues.length > 0) {
      setValidationIssues(issues);
      showWarning('Tồn kho hoặc giá có thay đổi', 'Vui lòng kiểm tra và cập nhật giỏ hàng.');
      return;
    }
    router.push('/checkout');
  };

  if (items.length === 0) {
    return (
      <div className="py-16">
        <EmptyState
          icon={<ShoppingBag className="w-10 h-10 text-zinc-400" />}
          title="Giỏ hàng của bạn đang trống"
          description="Hãy chọn cho mình những sản phẩm yêu thích và quay lại đây để hoàn tất đơn hàng nhé!"
          actionText="Khám phá sản phẩm ngay"
          actionHref="/products"
        />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-zinc-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900">
            Giỏ hàng của bạn
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 mt-1">
            Bạn đang có <strong>{itemCount}</strong> sản phẩm trong giỏ hàng
          </p>
        </div>

        {/* STT 11: Trigger Clear Cart Modal */}
        <button
          onClick={() => setShowClearConfirm(true)}
          className="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-rose-600 transition self-start sm:self-auto"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Xóa tất cả</span>
        </button>
      </div>

      {/* STT 4: Pre-validation Alert Banner if issues exist */}
      {validationIssues.length > 0 && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 space-y-2 animate-in fade-in">
          <div className="flex items-center gap-2 text-amber-900 font-bold text-sm">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
            <span>Phát hiện thay đổi tồn kho / giá trước khi thanh toán:</span>
          </div>
          <ul className="list-disc list-inside text-xs text-amber-800 space-y-1 pl-2">
            {validationIssues.map((issue, idx) => (
              <li key={idx}>{issue}</li>
            ))}
          </ul>
          <p className="text-xs text-amber-700 pt-1">
            Vui lòng điều chỉnh số lượng hoặc xóa sản phẩm không khả dụng để tiếp tục thanh toán.
          </p>
        </div>
      )}

      {/* Main Grid: Line Items + Order Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Line Items List */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-zinc-200/80 p-4 sm:p-6 shadow-xs divide-y divide-zinc-100">
          {items.map((item) => (
            <CartItemRow key={item.id} item={item} />
          ))}

          <div className="pt-4 flex items-center justify-between text-xs text-zinc-500">
            <Link href="/products" className="inline-flex items-center gap-1.5 hover:text-zinc-900 font-medium">
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Tiếp tục mua sắm</span>
            </Link>
          </div>
        </div>

        {/* Order Summary Sticky Card */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-white rounded-2xl border border-zinc-200/80 p-5 sm:p-6 shadow-xs space-y-5">
            <h3 className="text-base font-bold text-zinc-900 border-b border-zinc-100 pb-3">
              Tóm tắt đơn hàng
            </h3>

            {/* Free Shipping Progress */}
            <div className="bg-zinc-50 rounded-xl p-3.5 border border-zinc-200/60 space-y-2">
              <div className="flex items-center gap-2 text-xs font-medium text-zinc-800">
                <Truck className="w-4 h-4 text-zinc-900 shrink-0" />
                {isFreeShipping ? (
                  <span className="text-emerald-700 font-semibold">
                    🎉 Đủ điều kiện MIỄN PHÍ vận chuyển!
                  </span>
                ) : (
                  <span>
                    Mua thêm <strong>{formatCurrency(amountNeeded)}</strong> để được Miễn phí ship
                  </span>
                )}
              </div>
              <div className="w-full bg-zinc-200 rounded-full h-1.5 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    isFreeShipping ? 'bg-emerald-600' : 'bg-zinc-900'
                  }`}
                  style={{ width: `${freeShippingProgress}%` }}
                />
              </div>
            </div>

            {/* Financial breakdown */}
            <div className="space-y-2.5 text-xs sm:text-sm">
              <div className="flex justify-between text-zinc-600">
                <span>Tạm tính hàng hóa:</span>
                <span className="font-semibold text-zinc-900">{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between text-zinc-600">
                <span>Phí vận chuyển ước tính:</span>
                <span>
                  {shippingFee === 0 ? (
                    <span className="text-emerald-600 font-semibold uppercase text-xs">Miễn phí</span>
                  ) : (
                    formatCurrency(shippingFee)
                  )}
                </span>
              </div>
              <div className="flex justify-between text-zinc-600">
                <span>Chiết khấu khuyến mãi:</span>
                <span className="text-zinc-400">0₫</span>
              </div>

              <div className="pt-3 border-t border-zinc-200 flex justify-between items-baseline">
                <span className="text-sm font-bold text-zinc-900">Tổng thanh toán:</span>
                <span className="text-xl font-black text-zinc-900">{formatCurrency(grandTotal)}</span>
              </div>
            </div>

            {/* STT 4: Pre-checkout Validation on Click */}
            <button
              onClick={handleProceedToCheckout}
              disabled={isValidating}
              className="w-full py-3.5 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-md transition group disabled:opacity-50"
            >
              {isValidating ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang kiểm tra tồn kho...</span>
                </>
              ) : (
                <>
                  <span>Tiến hành Thanh toán</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </button>

            <div className="flex items-center justify-center gap-2 text-[11px] text-zinc-400 pt-1">
              <ShieldCheck className="w-3.5 h-3.5 text-zinc-500" />
              <span>Bảo mật giao dịch thanh toán chuẩn SSL 256-bit</span>
            </div>
          </div>
        </div>
      </div>

      {/* STT 11: Clear All Confirmation Modal */}
      {showClearConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs"
            onClick={() => setShowClearConfirm(false)}
          />
          <div className="relative bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-zinc-200 z-10 space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3 text-zinc-900">
              <div className="w-10 h-10 rounded-full bg-rose-50 flex items-center justify-center text-rose-600 shrink-0">
                <Trash2 className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-zinc-900">Xóa toàn bộ giỏ hàng</h4>
                <p className="text-xs text-zinc-500">Xác nhận làm trống giỏ hàng</p>
              </div>
            </div>
            <p className="text-xs text-zinc-600 leading-relaxed">
              Bạn có chắc chắn muốn xóa toàn bộ <strong>{itemCount} sản phẩm</strong> khỏi giỏ hàng? Hành động này không thể hoàn tác.
            </p>
            <div className="flex gap-2.5 pt-1">
              <button
                onClick={() => setShowClearConfirm(false)}
                className="flex-1 py-2 px-3 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 transition"
              >
                Giữ lại
              </button>
              <button
                onClick={handleConfirmClear}
                className="flex-1 py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition"
              >
                Xóa tất cả
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
