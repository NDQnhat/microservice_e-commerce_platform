'use client';

import React from 'react';
import Link from 'next/link';
import { ShoppingBag, ArrowRight, ArrowLeft, Truck, ShieldCheck, Trash2 } from 'lucide-react';
import { useCartStore, FREE_SHIPPING_THRESHOLD } from '@/store/cart-store';
import { CartItemRow } from '@/components/cart/CartItemRow';
import { EmptyState } from '@/components/common/EmptyState';
import { formatCurrency } from '@/lib/utils';

export default function CartPage() {
  const { items, itemCount, subtotal, clearCart } = useCartStore();

  const isFreeShipping = subtotal >= FREE_SHIPPING_THRESHOLD;
  const shippingFee = isFreeShipping || items.length === 0 ? 0 : 30000;
  const grandTotal = subtotal + shippingFee;
  const amountNeeded = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
  const freeShippingProgress = Math.min(100, Math.round((subtotal / FREE_SHIPPING_THRESHOLD) * 100));

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

        <button
          onClick={clearCart}
          className="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-rose-600 transition self-start sm:self-auto"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Xóa tất cả</span>
        </button>
      </div>

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

            {/* Checkout CTA */}
            <Link
              href="/checkout"
              className="w-full py-3.5 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-sm font-bold flex items-center justify-center gap-2 shadow-md transition group"
            >
              <span>Tiến hành Thanh toán</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>

            <div className="flex items-center justify-center gap-2 text-[11px] text-zinc-400 pt-1">
              <ShieldCheck className="w-3.5 h-3.5 text-zinc-500" />
              <span>Bảo mật giao dịch thanh toán chuẩn SSL 256-bit</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
