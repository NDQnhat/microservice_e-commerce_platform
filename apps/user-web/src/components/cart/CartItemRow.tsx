'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Trash2, Plus, Minus, AlertCircle } from 'lucide-react';
import { CartItem } from '@/types';
import { useCartStore } from '@/store/cart-store';
import { useToastStore } from '@/store/toast-store';
import { formatCurrency } from '@/lib/utils';

interface CartItemRowProps {
  item: CartItem;
  compact?: boolean;
}

export function CartItemRow({ item, compact = false }: CartItemRowProps) {
  const { updateQuantity, removeItem } = useCartStore();
  const { showWarning } = useToastStore();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleIncrement = async () => {
    const res = await updateQuantity(item.id, item.quantity + 1);
    if (!res.success && res.message) {
      showWarning('Giới hạn tồn kho', res.message);
    }
  };

  const handleDecrement = async () => {
    if (item.quantity <= 1) {
      setShowDeleteConfirm(true);
    } else {
      await updateQuantity(item.id, item.quantity - 1);
    }
  };

  const handleConfirmDelete = async () => {
    setShowDeleteConfirm(false);
    await removeItem(item.id);
  };

  const formatAttributes = (attrs?: Record<string, string>) => {
    if (!attrs) return null;
    return Object.entries(attrs)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' | ');
  };

  return (
    <>
      <div className={`flex gap-3 sm:gap-4 py-3 sm:py-4 ${compact ? 'border-b border-zinc-100 last:border-b-0' : 'border-b border-zinc-200'}`}>
        {/* Product Thumbnail */}
        <div className="relative w-16 sm:w-20 h-16 sm:h-20 rounded-xl overflow-hidden bg-zinc-100 shrink-0 border border-zinc-200/60">
          {item.productImage ? (
            <img
              src={item.productImage}
              alt={item.productName}
              className="w-full h-full object-cover object-center"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-[10px] text-zinc-400">
              No image
            </div>
          )}
        </div>

        {/* Item info */}
        <div className="flex-1 min-w-0 flex flex-col justify-between">
          <div>
            <div className="flex items-start justify-between gap-2">
              <Link
                href={`/products/${item.productId}`}
                className="text-xs sm:text-sm font-semibold text-zinc-900 hover:text-zinc-600 line-clamp-1 transition"
              >
                {item.productName}
              </Link>
              <button
                onClick={() => setShowDeleteConfirm(true)}
                className="text-zinc-400 hover:text-rose-600 p-1 transition"
                aria-label="Xóa khỏi giỏ hàng"
              >
                <Trash2 className="w-3.5 sm:w-4 h-3.5 sm:h-4" />
              </button>
            </div>

            {item.attributes && (
              <p className="text-[11px] text-zinc-500 mt-0.5 line-clamp-1">
                {formatAttributes(item.attributes)}
              </p>
            )}

            {item.maxAvailableStock <= 3 && (
              <span className="inline-block mt-1 text-[10px] font-semibold text-rose-600 bg-rose-50 px-1.5 py-0.5 rounded">
                Chỉ còn {item.maxAvailableStock} sản phẩm
              </span>
            )}
          </div>

          {/* Pricing & Stepper */}
          <div className="flex items-center justify-between mt-2 pt-1">
            {/* Stepper */}
            <div className="flex items-center border border-zinc-200 rounded-lg bg-zinc-50/50 overflow-hidden">
              <button
                onClick={handleDecrement}
                className="p-1 sm:p-1.5 text-zinc-600 hover:text-zinc-900 hover:bg-zinc-200/60 transition"
                aria-label="Giảm số lượng"
              >
                <Minus className="w-3 h-3" />
              </button>
              <span className="w-7 sm:w-8 text-center text-xs font-semibold text-zinc-900 select-none">
                {item.quantity}
              </span>
              <button
                onClick={handleIncrement}
                className="p-1 sm:p-1.5 text-zinc-600 hover:text-zinc-900 hover:bg-zinc-200/60 transition"
                aria-label="Tăng số lượng"
              >
                <Plus className="w-3 h-3" />
              </button>
            </div>

            {/* Price display */}
            <div className="text-right">
              <div className="text-xs sm:text-sm font-bold text-zinc-900">
                {formatCurrency(item.totalPrice)}
              </div>
              {item.originalPrice && item.originalPrice > item.unitPrice && (
                <div className="text-[10px] text-zinc-400 line-through">
                  {formatCurrency(item.originalPrice * item.quantity)}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* STT 11: Confirmation Dialog for Removing Cart Item */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs"
            onClick={() => setShowDeleteConfirm(false)}
          />
          <div className="relative bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-zinc-200 z-10 space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3 text-zinc-900">
              <div className="w-10 h-10 rounded-full bg-rose-50 flex items-center justify-center text-rose-600 shrink-0">
                <Trash2 className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-zinc-900">Xóa sản phẩm</h4>
                <p className="text-xs text-zinc-500">Xác nhận thao tác xóa</p>
              </div>
            </div>
            <p className="text-xs text-zinc-600 leading-relaxed">
              Bạn có chắc chắn muốn xóa sản phẩm <strong>"{item.productName}"</strong> khỏi giỏ hàng?
            </p>
            <div className="flex gap-2.5 pt-1">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 py-2 px-3 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 transition"
              >
                Hủy bỏ
              </button>
              <button
                onClick={handleConfirmDelete}
                className="flex-1 py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold transition"
              >
                Xác nhận xóa
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
