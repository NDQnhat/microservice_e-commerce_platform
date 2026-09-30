'use client';

import React, { Suspense, useState, useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Product, Category, ProductFilterParams, PaginatedResult } from '@/types';
import { ProductCard } from '@/components/product/ProductCard';
import { FacetedFilters } from '@/components/product/FacetedFilters';
import { Pagination } from '@/components/common/Pagination';
import { ProductGridSkeleton } from '@/components/common/SkeletonLoader';
import { EmptyState } from '@/components/common/EmptyState';
import { SlidersHorizontal, ArrowUpDown, X } from 'lucide-react';

function ProductsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  // STT 5 & STT 14: Initialize filters completely from URL parameters
  const [filters, setFilters] = useState<ProductFilterParams>({
    categoryId: searchParams.get('categoryId') || undefined,
    search: searchParams.get('search') || searchParams.get('q') || undefined,
    sortBy: (searchParams.get('sortBy') as any) || undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
    color: searchParams.get('color') || undefined,
    size: searchParams.get('size') || undefined,
    inStockOnly: searchParams.get('inStockOnly') === 'true' || searchParams.get('inStockOnly') === '1',
    page: searchParams.get('page') ? Number(searchParams.get('page')) : 1,
    pageSize: searchParams.get('pageSize') ? Number(searchParams.get('pageSize')) : 12,
  });

  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);

  // Sync state if URL search query changes
  useEffect(() => {
    setFilters({
      categoryId: searchParams.get('categoryId') || undefined,
      search: searchParams.get('search') || searchParams.get('q') || undefined,
      sortBy: (searchParams.get('sortBy') as any) || undefined,
      minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
      maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
      color: searchParams.get('color') || undefined,
      size: searchParams.get('size') || undefined,
      inStockOnly: searchParams.get('inStockOnly') === 'true' || searchParams.get('inStockOnly') === '1',
      page: searchParams.get('page') ? Number(searchParams.get('page')) : 1,
      pageSize: searchParams.get('pageSize') ? Number(searchParams.get('pageSize')) : 12,
    });
  }, [searchParams]);

  // STT 14: Push filters and pagination changes to URL SearchParams
  const updateUrlWithFilters = (newFilters: ProductFilterParams) => {
    const params = new URLSearchParams();
    if (newFilters.search) params.set('search', newFilters.search);
    if (newFilters.categoryId) params.set('categoryId', newFilters.categoryId);
    if (newFilters.minPrice !== undefined && newFilters.minPrice > 0) {
      params.set('minPrice', newFilters.minPrice.toString());
    }
    if (newFilters.maxPrice !== undefined && newFilters.maxPrice < Infinity) {
      params.set('maxPrice', newFilters.maxPrice.toString());
    }
    if (newFilters.sortBy) params.set('sortBy', newFilters.sortBy);
    if (newFilters.color) params.set('color', newFilters.color);
    if (newFilters.size) params.set('size', newFilters.size);
    if (newFilters.inStockOnly) params.set('inStockOnly', 'true');
    if (newFilters.page && newFilters.page > 1) {
      params.set('page', newFilters.page.toString());
    }
    if (newFilters.pageSize && newFilters.pageSize !== 12) {
      params.set('pageSize', newFilters.pageSize.toString());
    }

    const queryStr = params.toString();
    router.push(queryStr ? `/products?${queryStr}` : '/products');
  };

  const handleFilterChange = (newFilters: ProductFilterParams) => {
    setFilters(newFilters);
    updateUrlWithFilters(newFilters);
  };

  const handlePageChange = (newPage: number) => {
    const updated = { ...filters, page: newPage };
    setFilters(updated);
    updateUrlWithFilters(updated);
    // Smooth scroll to top of products grid
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Query categories
  const { data: categories = [] } = useQuery<Category[]>({
    queryKey: ['categories'],
    queryFn: () => apiClient<Category[]>('/api/v1/categories'),
  });

  // Query products with active filters and server-side pagination
  const { data: productData, isLoading } = useQuery<PaginatedResult<Product>>({
    queryKey: ['products', filters],
    queryFn: () => {
      const params = new URLSearchParams();
      if (filters.search) params.set('search', filters.search);
      if (filters.categoryId) params.set('categoryId', filters.categoryId);
      if (filters.minPrice !== undefined && filters.minPrice > 0) {
        params.set('minPrice', filters.minPrice.toString());
      }
      if (filters.maxPrice !== undefined && filters.maxPrice < Infinity) {
        params.set('maxPrice', filters.maxPrice.toString());
      }
      if (filters.sortBy) params.set('sortBy', filters.sortBy);
      if (filters.color) params.set('color', filters.color);
      if (filters.size) params.set('size', filters.size);
      if (filters.inStockOnly) params.set('inStockOnly', 'true');
      params.set('page', (filters.page || 1).toString());
      params.set('pageSize', (filters.pageSize || 12).toString());

      return apiClient<PaginatedResult<Product>>(`/api/v1/products?${params.toString()}`);
    },
  });

  const products = productData?.items || [];
  const total = productData?.total || 0;
  const totalPages = productData?.totalPages || 1;
  const currentPage = productData?.page || 1;
  const pageSize = productData?.size || 12;

  const handleResetFilters = () => {
    const emptyFilters: ProductFilterParams = { page: 1, pageSize: 12 };
    setFilters(emptyFilters);
    updateUrlWithFilters(emptyFilters);
  };

  return (
    <div className="space-y-6">
      {/* Page Title & Breadcrumb header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-zinc-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900">
            {filters.search
              ? `Kết quả tìm kiếm: "${filters.search}"`
              : filters.categoryId
              ? categories.find((c) => c.id === filters.categoryId)?.name || 'Danh mục sản phẩm'
              : 'Tất cả sản phẩm'}
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 mt-1">
            Hiển thị <strong>{products.length}</strong> trên tổng số <strong>{total}</strong> sản phẩm chất lượng cao
          </p>
        </div>

        {/* Sort & Mobile filter trigger */}
        <div className="flex items-center gap-3 self-end sm:self-auto">
          {/* Mobile Filter Trigger */}
          <button
            onClick={() => setIsMobileFilterOpen(true)}
            className="lg:hidden flex items-center gap-1.5 px-3 py-2 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-800 bg-white hover:bg-zinc-50 transition"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Bộ lọc</span>
          </button>

          {/* Sort dropdown */}
          <div className="flex items-center gap-2">
            <label className="text-xs font-medium text-zinc-500 hidden sm:inline">Sắp xếp:</label>
            <div className="relative">
              <select
                value={filters.sortBy || ''}
                onChange={(e) =>
                  handleFilterChange({
                    ...filters,
                    sortBy: (e.target.value as any) || undefined,
                    page: 1,
                  })
                }
                className="appearance-none bg-white border border-zinc-200 rounded-xl px-3 py-2 pr-8 text-xs font-medium text-zinc-900 focus:outline-none focus:border-zinc-900 cursor-pointer shadow-xs"
              >
                <option value="">Mặc định (Nổi bật)</option>
                <option value="price-asc">Giá: Thấp đến Cao</option>
                <option value="price-desc">Giá: Cao đến Thấp</option>
                <option value="newest">Mới nhất</option>
                <option value="rating">Đánh giá cao nhất</option>
              </select>
              <ArrowUpDown className="w-3 h-3 text-zinc-400 absolute right-2.5 top-3 pointer-events-none" />
            </div>
          </div>
        </div>
      </div>

      {/* Main Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Desktop Sticky Sidebar Filter */}
        <div className="hidden lg:block lg:col-span-1">
          <div className="sticky top-28 bg-white p-5 rounded-2xl border border-zinc-200/80 shadow-xs">
            <FacetedFilters
              categories={categories}
              filters={filters}
              onFilterChange={handleFilterChange}
              onReset={handleResetFilters}
            />
          </div>
        </div>

        {/* Product Grid Area */}
        <div className="lg:col-span-3 space-y-8">
          {isLoading ? (
            <ProductGridSkeleton count={6} />
          ) : products.length === 0 ? (
            <div className="py-12">
              <EmptyState
                title="Không tìm thấy sản phẩm"
                description="Không có sản phẩm nào khớp với tiêu chí tìm kiếm hoặc bộ lọc hiện tại của bạn."
                actionText="Xóa tất cả bộ lọc"
                onAction={handleResetFilters}
              />
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {/* STT 5: Pagination component */}
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={total}
                pageSize={pageSize}
                onPageChange={handlePageChange}
                className="border-t border-zinc-200 pt-6"
              />
            </>
          )}
        </div>
      </div>

      {/* Mobile Filters Slide-over Modal */}
      {isMobileFilterOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs"
            onClick={() => setIsMobileFilterOpen(false)}
          />
          <div className="relative ml-auto w-full max-w-xs bg-white h-full p-6 shadow-2xl overflow-y-auto animate-in slide-in-from-right">
            <div className="flex items-center justify-between pb-4 border-b border-zinc-200">
              <h3 className="text-base font-bold text-zinc-900">Bộ lọc sản phẩm</h3>
              <button
                onClick={() => setIsMobileFilterOpen(false)}
                className="p-1 rounded-lg text-zinc-400 hover:text-zinc-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="pt-4">
              <FacetedFilters
                categories={categories}
                filters={filters}
                onFilterChange={(newF) => {
                  handleFilterChange(newF);
                }}
                onReset={handleResetFilters}
              />
            </div>
            <div className="pt-6 border-t border-zinc-200 mt-6">
              <button
                onClick={() => setIsMobileFilterOpen(false)}
                className="w-full py-2.5 rounded-xl bg-zinc-900 text-white text-xs font-semibold"
              >
                Xem kết quả ({total})
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function ProductsPage() {
  return (
    <Suspense fallback={<ProductGridSkeleton count={8} />}>
      <ProductsContent />
    </Suspense>
  );
}
