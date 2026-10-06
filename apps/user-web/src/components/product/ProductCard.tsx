'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ShoppingBag, Heart, Star, Check } from 'lucide-react';
import { Product } from '@/types';
import { useCartStore } from '@/store/cart-store';
import { useToastStore } from '@/store/toast-store';
import { formatCurrency, calculateDiscountPercent } from '@/lib/utils';

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const { addItem } = useCartStore();
  const { showSuccess, showWarning } = useToastStore();

  // Primary SKU to display
  const primarySku = product.skus?.[0];
  const basePrice = primarySku?.price ?? 0;
  const salePrice = primarySku?.salePrice;
  const discountPercent = calculateDiscountPercent(basePrice, salePrice);
  const availableStock = primarySku?.inventory?.quantityAvailable ?? 10;
  const isOutOfStock = availableStock <= 0;
  const isLowStock = availableStock > 0 && availableStock <= 3;

  const handleQuickAdd = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (isOutOfStock) return;

    setIsAdding(true);
    try {
      const res = await addItem(product, primarySku, 1);
      if (res.success) {
        showSuccess('Đã thêm vào giỏ hàng', `${product.name} (${primarySku.skuCode})`);
      } else if (res.message) {
        showWarning('Không thể thêm', res.message);
      }
    } finally {
      setTimeout(() => setIsAdding(false), 500);
    }
  };

  const toggleWishlist = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsWishlisted(!isWishlisted);
  };

  return (
    <div className="group flex flex-col h-full bg-white rounded-2xl border border-zinc-200/80 overflow-hidden shadow-xs hover:shadow-md transition-all duration-300">
      {/* 1. Image Frame (1:1 square) */}
      <Link href={`/products/${product.id}`} className="relative aspect-square w-full overflow-hidden bg-zinc-100 block">
        <img
          src={product.mediaUrls?.[0] || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600'}
          alt={product.name}
          className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-300 ease-out"
        />

        {/* Floating Top Badges */}
        <div className="absolute top-2.5 left-2.5 flex flex-col gap-1 z-10">
          {discountPercent > 0 && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-600 text-white shadow-xs">
              -{discountPercent}%
            </span>
          )}
          {product.isBestSeller && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-zinc-900 text-white shadow-xs">
              Bán chạy
            </span>
          )}
          {isLowStock && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500 text-white shadow-xs">
              Chỉ còn {availableStock}
            </span>
          )}
          {isOutOfStock && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-zinc-800 text-white shadow-xs">
              Hết hàng
            </span>
          )}
        </div>

        {/* Wishlist Button */}
        <button
          onClick={toggleWishlist}
          className={`absolute top-2.5 right-2.5 p-2 rounded-full backdrop-blur-md transition-colors z-10 ${
            isWishlisted
              ? 'bg-rose-50 text-rose-600'
              : 'bg-white/80 text-zinc-600 hover:text-rose-600 hover:bg-white'
          }`}
          aria-label="Thêm vào danh sách yêu thích"
        >
          <Heart className={`w-4 h-4 ${isWishlisted ? 'fill-current' : ''}`} />
        </button>

        {/* Quick Add To Cart Floating Button (reveals on card hover) */}
        {!isOutOfStock && (
          <div className="absolute inset-x-3 bottom-3 translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-200 z-10">
            <button
              onClick={handleQuickAdd}
              disabled={isAdding}
              className="w-full py-2.5 px-3 rounded-xl bg-zinc-900/90 hover:bg-zinc-900 text-white text-xs font-semibold backdrop-blur-xs flex items-center justify-center gap-1.5 shadow-md active:scale-95 transition"
            >
              {isAdding ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Đã thêm</span>
                </>
              ) : (
                <>
                  <ShoppingBag className="w-3.5 h-3.5" />
                  <span>Thêm nhanh</span>
                </>
              )}
            </button>
          </div>
        )}
      </Link>

      {/* Card Content & Pricing */}
      <div className="p-3.5 sm:p-4 flex flex-col flex-1 justify-between gap-2">
        <div>
          <div className="flex items-center justify-between text-xs text-zinc-400 mb-1">
            <span className="uppercase font-medium tracking-wider text-[10px] text-zinc-500">
              {product.category?.name}
            </span>
            {product.rating && (
              <div className="flex items-center gap-1 text-zinc-700">
                <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                <span className="text-[11px] font-semibold">{product.rating}</span>
              </div>
            )}
          </div>

          <Link href={`/products/${product.id}`} className="block">
            <h3 className="text-xs sm:text-sm font-semibold text-zinc-900 group-hover:text-zinc-600 transition line-clamp-2 leading-snug">
              {product.name}
            </h3>
          </Link>
        </div>

        {/* Price Presentation */}
        <div className="pt-1 flex items-baseline gap-2">
          <span className="text-sm sm:text-base font-bold text-zinc-900">
            {formatCurrency(salePrice || basePrice)}
          </span>
          {salePrice && salePrice < basePrice && (
            <span className="text-xs text-zinc-400 line-through">
              {formatCurrency(basePrice)}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
