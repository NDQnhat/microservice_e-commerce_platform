'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { X, ShoppingBag, ArrowRight, Truck } from 'lucide-react';
import { useCartStore, FREE_SHIPPING_THRESHOLD } from '@/store/cart-store';
import { CartItemRow } from './CartItemRow';
import { formatCurrency } from '@/lib/utils';
import { EmptyState } from '@/components/common/EmptyState';

export function MiniCartDrawer() {
  const router = useRouter();
  const { items, itemCount, subtotal, isDrawerOpen, closeDrawer, freeShippingThreshold } = useCartStore();

  // Close on Escape key press
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeDrawer();
    };
    if (isDrawerOpen) {
      window.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isDrawerOpen, closeDrawer]);

  const threshold = freeShippingThreshold || 500000;
  const amountNeeded = Math.max(0, threshold - subtotal);
  const freeShippingProgress = Math.min(100, Math.round((subtotal / threshold) * 100));

  return (
    <>
      {/* Translucent backdrop */}
      <div
        className={`fixed inset-0 z-50 bg-black/50 backdrop-blur-xs transition-opacity duration-300 ${
          isDrawerOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
        onClick={closeDrawer}
        aria-hidden="true"
      />

      {/* Slide-over Drawer Panel */}
      <div
        className={`fixed inset-y-0 right-0 z-50 w-full max-w-md bg-white shadow-2xl flex flex-col transform transition-transform duration-300 ease-out ${
          isDrawerOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        {/* Drawer Header */}
        <div className="p-4 sm:p-5 border-b border-zinc-200/80 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-zinc-900" />
            <h3 className="text-base font-bold text-zinc-900">Giỏ hàng của bạn</h3>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-zinc-100 text-zinc-700">
              {itemCount}
            </span>
          </div>
          <button
            onClick={closeDrawer}
            className="p-1.5 text-zinc-400 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition"
            aria-label="Đóng ngăn giỏ hàng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Free Shipping Progress Bar */}
        <div className="bg-zinc-50 border-b border-zinc-200/80 p-3.5 px-4 sm:px-5">
          <div className="flex items-center gap-2 text-xs font-medium text-zinc-800 mb-1.5">
            <Truck className="w-4 h-4 text-zinc-900 shrink-0" />
            {amountNeeded === 0 ? (
              <span className="text-emerald-700 font-semibold">
                🎉 Đơn hàng của bạn đủ điều kiện MIỄN PHÍ vận chuyển!
              </span>
            ) : (
              <span>
                Mua thêm <strong className="text-zinc-900">{formatCurrency(amountNeeded)}</strong> để được Miễn phí ship
              </span>
            )}
          </div>
          <div className="w-full bg-zinc-200 rounded-full h-1.5 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                amountNeeded === 0 ? 'bg-emerald-600' : 'bg-zinc-900'
              }`}
              style={{ width: `${freeShippingProgress}%` }}
            />
          </div>
        </div>

        {/* Drawer Items Body */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5">
          {items.length === 0 ? (
            <div className="py-12">
              <EmptyState
                icon={<ShoppingBag className="w-8 h-8 text-zinc-400" />}
                title="Giỏ hàng đang trống"
                description="Bạn chưa chọn sản phẩm nào. Hãy khám phá ngay các bộ sưu tập mới nhất!"
                actionText="Khám phá sản phẩm"
                onAction={() => {
                  closeDrawer();
                  router.push('/products');
                }}
              />
            </div>
          ) : (
            <div className="divide-y divide-zinc-100">
              {items.map((item) => (
                <CartItemRow key={item.id} item={item} compact />
              ))}
            </div>
          )}
        </div>

        {/* Sticky Drawer Footer */}
        {items.length > 0 && (
          <div className="p-4 sm:p-5 border-t border-zinc-200/80 bg-white space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span className="text-zinc-500 font-medium">Tạm tính:</span>
              <span className="text-lg font-bold text-zinc-900">{formatCurrency(subtotal)}</span>
            </div>
            <p className="text-[11px] text-zinc-400">
              Phí vận chuyển và chiết khấu sẽ được tính chi tiết khi đặt hàng.
            </p>

            <div className="grid grid-cols-2 gap-2 pt-1">
              <Link
                href="/cart"
                onClick={closeDrawer}
                className="flex items-center justify-center py-2.5 px-4 rounded-xl border border-zinc-300 text-xs sm:text-sm font-semibold text-zinc-800 hover:bg-zinc-50 transition"
              >
                Xem giỏ hàng
              </Link>
              <Link
                href="/checkout"
                onClick={closeDrawer}
                className="flex items-center justify-center gap-1.5 py-2.5 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs sm:text-sm font-semibold transition shadow-sm group"
              >
                <span>Thanh toán</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
