'use client';

import React from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Trash2, Plus, Minus } from 'lucide-react';
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

  const handleIncrement = () => {
    const res = updateQuantity(item.id, item.quantity + 1);
    if (!res.success && res.message) {
      showWarning('Giới hạn tồn kho', res.message);
    }
  };

  const handleDecrement = () => {
    updateQuantity(item.id, item.quantity - 1);
  };

  const formatAttributes = (attrs?: Record<string, string>) => {
    if (!attrs) return null;
    return Object.entries(attrs)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' | ');
  };

  return (
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
              onClick={() => removeItem(item.id)}
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
  );
}
