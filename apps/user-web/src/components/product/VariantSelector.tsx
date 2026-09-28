'use client';

import React from 'react';
import { Product, Sku } from '@/types';
import { Plus, Minus, AlertCircle, CheckCircle2 } from 'lucide-react';
import { formatCurrency, calculateDiscountPercent } from '@/lib/utils';

interface VariantSelectorProps {
  product: Product;
  selectedSku: Sku;
  onSelectSku: (sku: Sku) => void;
  quantity: number;
  onQuantityChange: (qty: number) => void;
}

export function VariantSelector({
  product,
  selectedSku,
  onSelectSku,
  quantity,
  onQuantityChange,
}: VariantSelectorProps) {
  const basePrice = selectedSku.price;
  const salePrice = selectedSku.salePrice;
  const discountPercent = calculateDiscountPercent(basePrice, salePrice);
  const availableStock = selectedSku.inventory?.quantityAvailable ?? 10;
  const isOutOfStock = availableStock <= 0;
  const isLowStock = availableStock > 0 && availableStock <= 3;

  // Extract all attribute keys (e.g. ['Color', 'Size'])
  const attributeKeys = Array.from(
    new Set(product.skus.flatMap((s) => Object.keys(s.attributes || {})))
  );

  const handleAttributeValueSelect = (attrKey: string, val: string) => {
    // Find matching SKU that has this attr value plus matches other currently selected attrs if possible
    const currentAttrs = { ...(selectedSku.attributes || {}), [attrKey]: val };
    const matchedSku =
      product.skus.find((s) =>
        Object.entries(currentAttrs).every(([k, v]) => s.attributes[k] === v)
      ) ||
      product.skus.find((s) => s.attributes[attrKey] === val) ||
      selectedSku;

    onSelectSku(matchedSku);
    // Reset quantity if exceeds new SKU stock
    const newStock = matchedSku.inventory?.quantityAvailable ?? 10;
    if (quantity > newStock && newStock > 0) {
      onQuantityChange(newStock);
    }
  };

  return (
    <div className="space-y-6">
      {/* Dynamic Price Display */}
      <div className="space-y-1">
        <div className="flex items-baseline gap-3">
          <span className="text-3xl font-extrabold text-zinc-900 tracking-tight">
            {formatCurrency(salePrice || basePrice)}
          </span>
          {salePrice && salePrice < basePrice && (
            <span className="text-lg text-zinc-400 line-through">
              {formatCurrency(basePrice)}
            </span>
          )}
          {discountPercent > 0 && (
            <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-rose-600 text-white">
              Tiết kiệm {discountPercent}%
            </span>
          )}
        </div>
        <p className="text-xs text-zinc-400 font-mono">Mã SKU: {selectedSku.skuCode}</p>
      </div>

      {/* Stock Urgency Status Alert */}
      <div>
        {isOutOfStock ? (
          <div className="flex items-center gap-2 p-3 rounded-xl bg-zinc-100 text-zinc-600 text-xs font-medium">
            <AlertCircle className="w-4 h-4 text-zinc-500 shrink-0" />
            <span>Tạm thời hết hàng cho biến thể này. Vui lòng chọn màu/size khác.</span>
          </div>
        ) : isLowStock ? (
          <div className="flex items-center gap-2 p-3 rounded-xl bg-rose-50 border border-rose-200/80 text-rose-700 text-xs font-semibold animate-pulse">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>Chỉ còn {availableStock} sản phẩm trong kho! Hãy nhanh tay đặt hàng.</span>
          </div>
        ) : (
          <div className="flex items-center gap-2 text-xs font-medium text-emerald-700">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>Còn hàng ({availableStock} sản phẩm sẵn sàng giao ngay)</span>
          </div>
        )}
      </div>

      {/* Attribute / Variant Selectors */}
      <div className="space-y-4 pt-2">
        {attributeKeys.map((key) => {
          // Unique values for this attribute key across product SKUs
          const values = Array.from(
            new Set(product.skus.map((s) => s.attributes[key]).filter(Boolean))
          );
          const currentVal = selectedSku.attributes?.[key];

          return (
            <div key={key} className="space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-zinc-900 uppercase tracking-wide">{key}:</span>
                <span className="text-zinc-600 font-medium">{currentVal}</span>
              </div>

              <div className="flex flex-wrap gap-2">
                {values.map((val) => {
                  const isSelected = currentVal === val;
                  return (
                    <button
                      key={val}
                      onClick={() => handleAttributeValueSelect(key, val)}
                      className={`text-xs px-3.5 py-2 rounded-xl border font-medium transition ${
                        isSelected
                          ? 'border-zinc-900 bg-zinc-900 text-white shadow-xs'
                          : 'border-zinc-200 bg-white text-zinc-800 hover:border-zinc-400'
                      }`}
                    >
                      {val}
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {/* Quantity Stepper (BR-004 Stock Limit) */}
      {!isOutOfStock && (
        <div className="pt-2 flex items-center gap-4">
          <span className="text-xs font-bold text-zinc-900 uppercase tracking-wide">Số lượng:</span>
          <div className="flex items-center border border-zinc-300 rounded-xl overflow-hidden bg-white">
            <button
              onClick={() => onQuantityChange(Math.max(1, quantity - 1))}
              disabled={quantity <= 1}
              className="p-2.5 text-zinc-600 hover:text-zinc-900 hover:bg-zinc-100 disabled:opacity-40 transition"
              aria-label="Giảm"
            >
              <Minus className="w-3.5 h-3.5" />
            </button>
            <span className="w-12 text-center text-sm font-bold text-zinc-900 select-none">
              {quantity}
            </span>
            <button
              onClick={() => onQuantityChange(Math.min(availableStock, quantity + 1))}
              disabled={quantity >= availableStock}
              className="p-2.5 text-zinc-600 hover:text-zinc-900 hover:bg-zinc-100 disabled:opacity-40 transition"
              aria-label="Tăng"
            >
              <Plus className="w-3.5 h-3.5" />
            </button>
          </div>
          {quantity >= availableStock && (
            <span className="text-xs text-amber-600 font-medium">Đã đạt mức tối đa khả dụng</span>
          )}
        </div>
      )}
    </div>
  );
}
