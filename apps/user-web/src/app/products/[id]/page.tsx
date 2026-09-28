'use client';

import React, { useState, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Product, Sku } from '@/types';
import { ProductGallery } from '@/components/product/ProductGallery';
import { VariantSelector } from '@/components/product/VariantSelector';
import { ProductCard } from '@/components/product/ProductCard';
import { Skeleton } from '@/components/common/SkeletonLoader';
import { EmptyState } from '@/components/common/EmptyState';
import { useCartStore } from '@/store/cart-store';
import { useToastStore } from '@/store/toast-store';
import { MOCK_PRODUCTS } from '@/lib/mock-data';
import {
  ShoppingBag,
  Zap,
  ChevronRight,
  ShieldCheck,
  Truck,
  RotateCcw,
  Check,
} from 'lucide-react';

export default function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const router = useRouter();
  const { addItem } = useCartStore();
  const { showSuccess, showWarning } = useToastStore();

  const [activeTab, setActiveTab] = useState<'desc' | 'specs' | 'shipping'>('desc');
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);

  // Fetch product detail
  const {
    data: product,
    isLoading,
    error,
  } = useQuery<Product>({
    queryKey: ['product', resolvedParams.id],
    queryFn: () => apiClient<Product>(`/api/v1/products/${resolvedParams.id}`),
  });

  // Set default SKU once product is loaded
  React.useEffect(() => {
    if (product && product.skus.length > 0 && !selectedSku) {
      setSelectedSku(product.skus[0]);
    }
  }, [product, selectedSku]);

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 sm:gap-12 py-6">
        <Skeleton className="aspect-square w-full rounded-2xl" />
        <div className="space-y-4">
          <Skeleton className="h-4 w-1/4" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="py-12">
        <EmptyState
          title="Không tìm thấy sản phẩm"
          description="Sản phẩm bạn đang tìm kiếm không tồn tại hoặc đã ngừng kinh doanh."
          actionText="Quay lại danh mục sản phẩm"
          actionHref="/products"
        />
      </div>
    );
  }

  const currentSku = selectedSku || product.skus[0];
  const maxStock = currentSku?.inventory?.quantityAvailable ?? 10;
  const isOutOfStock = maxStock <= 0;

  const handleAddToCart = () => {
    if (isOutOfStock) return;
    setIsAdding(true);
    const res = addItem(product, currentSku, quantity);
    if (res.success) {
      showSuccess('Đã thêm vào giỏ hàng', `${product.name} (SL: ${quantity})`);
    } else if (res.message) {
      showWarning('Không thể thêm', res.message);
    }
    setTimeout(() => setIsAdding(false), 500);
  };

  const handleBuyNow = () => {
    if (isOutOfStock) return;
    const res = addItem(product, currentSku, quantity);
    if (res.success) {
      router.push('/checkout');
    } else if (res.message) {
      showWarning('Không thể mua ngay', res.message);
    }
  };

  // Related products from same category
  const relatedProducts = MOCK_PRODUCTS.filter(
    (p) => p.id !== product.id && p.category.id === product.category.id
  ).slice(0, 4);

  return (
    <div className="space-y-12 sm:space-y-16">
      {/* Breadcrumb Navigation */}
      <nav className="flex items-center gap-1.5 text-xs text-zinc-500 overflow-x-auto whitespace-nowrap pb-2">
        <Link href="/" className="hover:text-zinc-900 transition">
          Trang chủ
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
        <Link href="/products" className="hover:text-zinc-900 transition">
          Sản phẩm
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
        <Link
          href={`/products?categoryId=${product.category.id}`}
          className="hover:text-zinc-900 transition"
        >
          {product.category.name}
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
        <span className="text-zinc-900 font-medium truncate max-w-[200px]">{product.name}</span>
      </nav>

      {/* Main PDP Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 sm:gap-12 items-start">
        {/* Gallery */}
        <ProductGallery mediaUrls={product.mediaUrls} productName={product.name} />

        {/* Product Details & Variant Selector */}
        <div className="space-y-6 sm:space-y-8">
          <div>
            <span className="text-xs uppercase font-bold tracking-wider text-zinc-400">
              {product.category.name}
            </span>
            <h1 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight text-zinc-900 leading-snug">
              {product.name}
            </h1>
            <p className="mt-3 text-sm text-zinc-600 leading-relaxed">{product.description}</p>
          </div>

          {/* Variant Selector */}
          <VariantSelector
            product={product}
            selectedSku={currentSku}
            onSelectSku={setSelectedSku}
            quantity={quantity}
            onQuantityChange={setQuantity}
          />

          {/* Action CTAs */}
          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <button
              onClick={handleAddToCart}
              disabled={isOutOfStock || isAdding}
              className="flex-1 py-3.5 px-6 rounded-2xl border-2 border-zinc-900 text-zinc-900 hover:bg-zinc-100 font-bold text-sm flex items-center justify-center gap-2 transition disabled:opacity-40 disabled:hover:bg-transparent"
            >
              {isAdding ? (
                <>
                  <Check className="w-4 h-4 text-emerald-600" />
                  <span>Đã thêm</span>
                </>
              ) : (
                <>
                  <ShoppingBag className="w-4 h-4" />
                  <span>{isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}</span>
                </>
              )}
            </button>

            <button
              onClick={handleBuyNow}
              disabled={isOutOfStock}
              className="flex-1 py-3.5 px-6 rounded-2xl bg-zinc-900 hover:bg-zinc-800 text-white font-bold text-sm flex items-center justify-center gap-2 transition shadow-md disabled:opacity-40"
            >
              <Zap className="w-4 h-4" />
              <span>{isOutOfStock ? 'Hết hàng' : 'Mua ngay'}</span>
            </button>
          </div>

          {/* Trust Guarantees */}
          <div className="grid grid-cols-3 gap-3 pt-6 border-t border-zinc-200/80 text-center">
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zinc-50 border border-zinc-200/60">
              <Truck className="w-4 h-4 text-zinc-700" />
              <span className="text-[11px] font-semibold text-zinc-800">Miễn phí ship</span>
              <span className="text-[10px] text-zinc-400">Đơn từ 500k</span>
            </div>
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zinc-50 border border-zinc-200/60">
              <RotateCcw className="w-4 h-4 text-zinc-700" />
              <span className="text-[11px] font-semibold text-zinc-800">Đổi trả 30 ngày</span>
              <span className="text-[10px] text-zinc-400">Nguyên bao bì</span>
            </div>
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-zinc-50 border border-zinc-200/60">
              <ShieldCheck className="w-4 h-4 text-zinc-700" />
              <span className="text-[11px] font-semibold text-zinc-800">Chính hãng 100%</span>
              <span className="text-[10px] text-zinc-400">Bảo hành đầy đủ</span>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs Section: Description, Specs, Shipping */}
      <div className="pt-8 border-t border-zinc-200">
        <div className="flex gap-8 border-b border-zinc-200">
          <button
            onClick={() => setActiveTab('desc')}
            className={`pb-3 text-sm font-bold transition relative ${
              activeTab === 'desc'
                ? 'text-zinc-900 border-b-2 border-zinc-900'
                : 'text-zinc-400 hover:text-zinc-600'
            }`}
          >
            Mô tả sản phẩm
          </button>
          <button
            onClick={() => setActiveTab('specs')}
            className={`pb-3 text-sm font-bold transition relative ${
              activeTab === 'specs'
                ? 'text-zinc-900 border-b-2 border-zinc-900'
                : 'text-zinc-400 hover:text-zinc-600'
            }`}
          >
            Thông số kỹ thuật
          </button>
          <button
            onClick={() => setActiveTab('shipping')}
            className={`pb-3 text-sm font-bold transition relative ${
              activeTab === 'shipping'
                ? 'text-zinc-900 border-b-2 border-zinc-900'
                : 'text-zinc-400 hover:text-zinc-600'
            }`}
          >
            Vận chuyển & Đổi trả
          </button>
        </div>

        <div className="py-6 text-sm text-zinc-600 leading-relaxed max-w-3xl">
          {activeTab === 'desc' && (
            <div className="space-y-4">
              <p>{product.description}</p>
              <p>
                Sản phẩm được chế tác với công nghệ tiên tiến nhất nhằm mang lại độ bền cơ học cao, khả năng chống hao mòn theo thời gian và cảm giác sử dụng thoải mái tối đa cho người dùng hàng ngày.
              </p>
            </div>
          )}

          {activeTab === 'specs' && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 py-2 border-b border-zinc-100">
                <span className="font-semibold text-zinc-900">Mã sản phẩm:</span>
                <span className="font-mono text-zinc-600">{product.id}</span>
              </div>
              <div className="grid grid-cols-2 py-2 border-b border-zinc-100">
                <span className="font-semibold text-zinc-900">Mã SKU hiện tại:</span>
                <span className="font-mono text-zinc-600">{currentSku.skuCode}</span>
              </div>
              <div className="grid grid-cols-2 py-2 border-b border-zinc-100">
                <span className="font-semibold text-zinc-900">Danh mục:</span>
                <span>{product.category.name}</span>
              </div>
              <div className="grid grid-cols-2 py-2 border-b border-zinc-100">
                <span className="font-semibold text-zinc-900">Xuất xứ:</span>
                <span>Chính hãng Atelier / Tiêu chuẩn quốc tế</span>
              </div>
            </div>
          )}

          {activeTab === 'shipping' && (
            <div className="space-y-4">
              <p>
                <strong>Giao hàng hỏa tốc:</strong> Đơn hàng nội thành Hà Nội & TP. Hồ Chí Minh được xử lý đóng gói và bàn giao cho đơn vị vận chuyển trong vòng 2-4 giờ làm việc.
              </p>
              <p>
                <strong>Miễn phí vận chuyển:</strong> Áp dụng tự động cho toàn bộ đơn hàng đạt giá trị tạm tính từ 500.000₫. Phí vận chuyển tiêu chuẩn cho đơn dưới 500k là 30.000₫ toàn quốc.
              </p>
              <p>
                <strong>Chính sách đổi trả:</strong> Hỗ trợ đổi size, đổi màu hoặc hoàn tiền 100% trong vòng 30 ngày kể từ ngày nhận hàng nếu phát sinh lỗi từ nhà sản xuất.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Related Products Section */}
      {relatedProducts.length > 0 && (
        <div className="space-y-6 pt-6">
          <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
            Sản phẩm tương tự
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
            {relatedProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
