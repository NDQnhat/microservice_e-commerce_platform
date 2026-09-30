'use client';

import React from 'react';
import { Category, ProductFilterParams } from '@/types';
import { Filter, RotateCcw, Check } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

interface FacetedFiltersProps {
  categories: Category[];
  filters: ProductFilterParams;
  onFilterChange: (newFilters: ProductFilterParams) => void;
  onReset: () => void;
  className?: string;
}

const COMMON_COLORS = [
  { name: 'Đen', value: 'Đen' },
  { name: 'Trắng', value: 'Trắng' },
  { name: 'Xám', value: 'Xám' },
  { name: 'Xanh Navy', value: 'Xanh' },
  { name: 'Bạc', value: 'Bạc' },
];

const COMMON_SIZES = ['S', 'M', 'L', 'XL'];

export function FacetedFilters({
  categories,
  filters,
  onFilterChange,
  onReset,
  className = '',
}: FacetedFiltersProps) {
  const handleCategorySelect = (catId?: string) => {
    onFilterChange({
      ...filters,
      categoryId: filters.categoryId === catId ? undefined : catId,
      page: 1,
    });
  };

  const [localMin, setLocalMin] = React.useState<string>(filters.minPrice ? String(filters.minPrice) : '');
  const [localMax, setLocalMax] = React.useState<string>(filters.maxPrice ? String(filters.maxPrice) : '');
  const [priceError, setPriceError] = React.useState<string | null>(null);

  React.useEffect(() => {
    setLocalMin(filters.minPrice ? String(filters.minPrice) : '');
    setLocalMax(filters.maxPrice ? String(filters.maxPrice) : '');
    setPriceError(null);
  }, [filters.minPrice, filters.maxPrice]);

  const handleApplyPrice = (minVal?: number, maxVal?: number) => {
    let min = minVal !== undefined ? minVal : localMin ? Number(localMin) : undefined;
    let max = maxVal !== undefined ? maxVal : localMax ? Number(localMax) : undefined;

    if (min !== undefined && max !== undefined && min > max) {
      // Auto-swap per STT 13
      const temp = min;
      min = max;
      max = temp;
      setLocalMin(String(min));
      setLocalMax(String(max));
      setPriceError('Giá "Từ" lớn hơn "Đến": Hệ thống đã tự động hoán đổi khoảng giá phù hợp.');
    } else {
      setPriceError(null);
    }

    onFilterChange({
      ...filters,
      minPrice: min,
      maxPrice: max,
      page: 1,
    });
  };

  const handlePriceChange = (min?: number, max?: number) => {
    setLocalMin(min !== undefined ? String(min) : '');
    setLocalMax(max !== undefined ? String(max) : '');
    handleApplyPrice(min, max);
  };

  const handleColorSelect = (color: string) => {
    onFilterChange({
      ...filters,
      color: filters.color === color ? undefined : color,
      page: 1,
    });
  };

  const handleSizeSelect = (size: string) => {
    onFilterChange({
      ...filters,
      size: filters.size === size ? undefined : size,
      page: 1,
    });
  };

  const hasActiveFilters = Boolean(
    filters.categoryId ||
      filters.minPrice ||
      filters.maxPrice ||
      filters.color ||
      filters.size ||
      filters.inStockOnly
  );

  return (
    <aside className={`space-y-6 ${className}`}>
      {/* Filter Header */}
      <div className="flex items-center justify-between pb-3 border-b border-zinc-200">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-zinc-900" />
          <h3 className="text-sm font-bold text-zinc-900 uppercase tracking-wide">Bộ lọc</h3>
        </div>
        {hasActiveFilters && (
          <button
            onClick={onReset}
            className="flex items-center gap-1 text-xs text-rose-600 hover:text-rose-700 font-medium transition"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Xóa lọc</span>
          </button>
        )}
      </div>

      {/* 1. Multi-level Categories */}
      <div className="space-y-3">
        <h4 className="text-xs font-bold text-zinc-900 uppercase tracking-wider">Danh mục</h4>
        <div className="space-y-1 text-sm">
          <button
            onClick={() => handleCategorySelect(undefined)}
            className={`w-full text-left px-2.5 py-1.5 rounded-lg transition text-xs ${
              !filters.categoryId
                ? 'font-bold text-zinc-900 bg-zinc-100'
                : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-50'
            }`}
          >
            Tất cả danh mục
          </button>
          {categories.map((cat) => {
            const isSelected = filters.categoryId === cat.id;
            return (
              <div key={cat.id} className="space-y-0.5">
                <button
                  onClick={() => handleCategorySelect(cat.id)}
                  className={`w-full text-left px-2.5 py-1.5 rounded-lg transition text-xs flex items-center justify-between ${
                    isSelected
                      ? 'font-bold text-zinc-900 bg-zinc-100'
                      : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-50'
                  }`}
                >
                  <span>{cat.name}</span>
                  {isSelected && <Check className="w-3.5 h-3.5 text-zinc-900" />}
                </button>

                {/* Subcategories (2-level hierarchy) */}
                {cat.subcategories && cat.subcategories.length > 0 && (
                  <div className="pl-4 space-y-0.5">
                    {cat.subcategories.map((sub) => {
                      const isSubSelected = filters.categoryId === sub.id;
                      return (
                        <button
                          key={sub.id}
                          onClick={() => handleCategorySelect(sub.id)}
                          className={`w-full text-left px-2 py-1 rounded text-[11px] transition flex items-center justify-between ${
                            isSubSelected
                              ? 'font-bold text-zinc-900 bg-zinc-100'
                              : 'text-zinc-500 hover:text-zinc-900 hover:bg-zinc-50'
                          }`}
                        >
                          <span>{sub.name}</span>
                          {isSubSelected && <Check className="w-3 h-3 text-zinc-900" />}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 2. Price Range (STT 13: Constraint & Auto-swap) */}
      <div className="space-y-3 pt-3 border-t border-zinc-100">
        <div className="flex items-center justify-between">
          <h4 className="text-xs font-bold text-zinc-900 uppercase tracking-wider">Khoảng giá</h4>
          {(localMin || localMax) && (
            <button
              onClick={() => handleApplyPrice()}
              className="text-[10px] font-bold text-zinc-900 hover:underline"
            >
              Áp dụng
            </button>
          )}
        </div>

        {priceError && (
          <p className="text-[10px] text-amber-700 bg-amber-50 p-2 rounded-lg leading-tight">
            {priceError}
          </p>
        )}

        <div className="space-y-2">
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <label className="text-[10px] text-zinc-500 block mb-1">Từ (₫)</label>
              <input
                type="number"
                placeholder="0"
                value={localMin}
                onChange={(e) => setLocalMin(e.target.value)}
                onBlur={() => handleApplyPrice()}
                className="w-full px-2.5 py-1.5 rounded-lg border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
              />
            </div>
            <div>
              <label className="text-[10px] text-zinc-500 block mb-1">Đến (₫)</label>
              <input
                type="number"
                placeholder="5,000,000"
                value={localMax}
                onChange={(e) => setLocalMax(e.target.value)}
                onBlur={() => handleApplyPrice()}
                className="w-full px-2.5 py-1.5 rounded-lg border border-zinc-200 text-xs text-zinc-900 focus:outline-none focus:border-zinc-900"
              />
            </div>
          </div>

          {/* Quick price presets */}
          <div className="flex flex-wrap gap-1.5 pt-1">
            <button
              onClick={() => handlePriceChange(undefined, 1000000)}
              className="text-[11px] px-2 py-0.5 rounded-md border border-zinc-200 text-zinc-600 hover:bg-zinc-100"
            >
              Dưới 1 triệu
            </button>
            <button
              onClick={() => handlePriceChange(1000000, 3000000)}
              className="text-[11px] px-2 py-0.5 rounded-md border border-zinc-200 text-zinc-600 hover:bg-zinc-100"
            >
              1tr - 3tr
            </button>
            <button
              onClick={() => handlePriceChange(3000000, undefined)}
              className="text-[11px] px-2 py-0.5 rounded-md border border-zinc-200 text-zinc-600 hover:bg-zinc-100"
            >
              Trên 3 triệu
            </button>
          </div>
        </div>
      </div>

      {/* 3. Color Swatches */}
      <div className="space-y-3 pt-3 border-t border-zinc-100">
        <h4 className="text-xs font-bold text-zinc-900 uppercase tracking-wider">Màu sắc</h4>
        <div className="flex flex-wrap gap-2">
          {COMMON_COLORS.map((col) => {
            const isSelected = filters.color === col.value;
            return (
              <button
                key={col.value}
                onClick={() => handleColorSelect(col.value)}
                className={`text-xs px-2.5 py-1 rounded-full border transition flex items-center gap-1 ${
                  isSelected
                    ? 'border-zinc-900 bg-zinc-900 text-white font-medium'
                    : 'border-zinc-200 text-zinc-700 hover:border-zinc-400 bg-white'
                }`}
              >
                <span>{col.name}</span>
                {isSelected && <Check className="w-3 h-3" />}
              </button>
            );
          })}
        </div>
      </div>

      {/* 4. Sizes */}
      <div className="space-y-3 pt-3 border-t border-zinc-100">
        <h4 className="text-xs font-bold text-zinc-900 uppercase tracking-wider">Kích cỡ</h4>
        <div className="flex flex-wrap gap-2">
          {COMMON_SIZES.map((size) => {
            const isSelected = filters.size === size;
            return (
              <button
                key={size}
                onClick={() => handleSizeSelect(size)}
                className={`w-9 h-9 text-xs rounded-lg border font-semibold transition flex items-center justify-center ${
                  isSelected
                    ? 'border-zinc-900 bg-zinc-900 text-white'
                    : 'border-zinc-200 text-zinc-700 hover:border-zinc-400 bg-white'
                }`}
              >
                {size}
              </button>
            );
          })}
        </div>
      </div>

      {/* 5. In Stock Only */}
      <div className="pt-3 border-t border-zinc-100">
        <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-zinc-800">
          <input
            type="checkbox"
            checked={Boolean(filters.inStockOnly)}
            onChange={(e) =>
              onFilterChange({
                ...filters,
                inStockOnly: e.target.checked,
                page: 1,
              })
            }
            className="w-4 h-4 rounded border-zinc-300 text-zinc-900 focus:ring-zinc-900"
          />
          <span>Chỉ hiện sản phẩm còn hàng</span>
        </label>
      </div>
    </aside>
  );
}
