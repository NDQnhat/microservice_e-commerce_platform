'use client';

import React from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, TrendingUp, Tag, ShieldCheck, Truck } from 'lucide-react';
import { ProductCard } from '@/components/product/ProductCard';
import { MOCK_PRODUCTS, MOCK_CATEGORIES } from '@/lib/mock-data';

export default function HomePage() {
  const bestSellers = MOCK_PRODUCTS.filter((p) => p.isBestSeller);
  const promotionalProducts = MOCK_PRODUCTS.filter((p) =>
    p.skus.some((s) => s.salePrice && s.salePrice < s.price)
  );
  const newArrivals = MOCK_PRODUCTS.filter((p) => p.isNewArrival);

  return (
    <div className="space-y-12 sm:space-y-16">
      {/* 1. Hero Editorial Banner */}
      <section className="relative rounded-3xl overflow-hidden bg-zinc-900 text-white min-h-[460px] sm:min-h-[520px] flex items-center shadow-lg">
        {/* Background photo overlay */}
        <div className="absolute inset-0 z-0">
          <img
            src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1800&q=85"
            alt="Hero editorial banner"
            className="w-full h-full object-cover object-center opacity-30 scale-105"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-zinc-950 via-zinc-950/80 to-transparent" />
        </div>

        {/* Content */}
        <div className="relative z-10 max-w-2xl px-6 sm:px-12 py-12 space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-xs font-semibold uppercase tracking-wider text-zinc-200">
            <Sparkles className="w-3.5 h-3.5 text-rose-400" />
            <span>Bộ sưu tập Xuân Hè 2026</span>
          </div>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight uppercase leading-none font-sans">
            Phong cách <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-white via-zinc-200 to-zinc-400">
              Tối giản & Đẳng cấp
            </span>
          </h1>

          <p className="text-zinc-300 text-sm sm:text-base leading-relaxed max-w-xl">
            Sự kết hợp hoàn hảo giữa vật liệu thượng hạng và thiết kế công thái học vượt thời gian. Mua sắm liền mạch, bảo đảm giữ hàng chuẩn xác với kiến trúc phân tán.
          </p>

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <Link
              href="/products"
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-white text-zinc-900 text-sm font-bold hover:bg-zinc-100 transition-all shadow-md group"
            >
              <span>Khám phá ngay</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link
              href="/products?categoryId=cat-fashion"
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-white/10 hover:bg-white/20 text-white text-sm font-semibold backdrop-blur-xs transition border border-white/20"
            >
              Thời trang & May mặc
            </Link>
          </div>
        </div>
      </section>

      {/* 2. Top Categories Visual Grid */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
              Danh mục nổi bật
            </h2>
            <p className="text-xs sm:text-sm text-zinc-500 mt-0.5">
              Khám phá các sản phẩm được phân loại theo tiêu chuẩn chất lượng cao
            </p>
          </div>
          <Link
            href="/products"
            className="text-xs sm:text-sm font-semibold text-zinc-900 hover:text-zinc-600 flex items-center gap-1 group"
          >
            <span>Tất cả</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
          {MOCK_CATEGORIES.map((cat) => (
            <Link
              key={cat.id}
              href={`/products?categoryId=${cat.id}`}
              className="group relative rounded-2xl overflow-hidden aspect-4/3 sm:aspect-square bg-zinc-100 border border-zinc-200/80 shadow-xs hover:shadow-md transition-all duration-300 block"
            >
              <img
                src={cat.imageUrl}
                alt={cat.name}
                className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-300"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-zinc-950/80 via-zinc-950/20 to-transparent" />
              <div className="absolute bottom-3 sm:bottom-4 left-3 sm:left-4 right-3 text-white">
                <h3 className="text-sm sm:text-base font-bold leading-tight drop-shadow-xs">
                  {cat.name}
                </h3>
                <span className="text-[11px] text-zinc-300 flex items-center gap-1 mt-1 group-hover:translate-x-1 transition-transform">
                  <span>Khám phá</span>
                  <ArrowRight className="w-3 h-3" />
                </span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 3. Hot Deals & Promotions (BR-013 Strike-through Price Demonstration) */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-rose-50 text-rose-600">
              <Tag className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
                Ưu đãi chớp nhoáng
              </h2>
              <p className="text-xs sm:text-sm text-zinc-500 mt-0.5">
                Giá khuyến mãi áp dụng có thời hạn với mức giá tiết kiệm đặc biệt
              </p>
            </div>
          </div>
          <Link
            href="/products"
            className="text-xs sm:text-sm font-semibold text-rose-600 hover:text-rose-700"
          >
            Xem tất cả ưu đãi &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {promotionalProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* 4. Best Sellers Section */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-zinc-100 text-zinc-900">
              <TrendingUp className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
                Sản phẩm bán chạy nhất
              </h2>
              <p className="text-xs sm:text-sm text-zinc-500 mt-0.5">
                Được khách hàng bình chọn và tin dùng nhiều nhất trong tháng
              </p>
            </div>
          </div>
          <Link
            href="/products"
            className="text-xs sm:text-sm font-semibold text-zinc-900 hover:text-zinc-600"
          >
            Xem toàn bộ &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {bestSellers.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* 5. New Arrivals Section */}
      {newArrivals.length > 0 && (
        <section className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-zinc-900">
                Mới lên kệ
              </h2>
              <p className="text-xs sm:text-sm text-zinc-500 mt-0.5">
                Các mẫu thiết kế và phụ kiện độc quyền vừa cập bến
              </p>
            </div>
            <Link
              href="/products"
              className="text-xs sm:text-sm font-semibold text-zinc-900 hover:text-zinc-600"
            >
              Xem tất cả &rarr;
            </Link>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {newArrivals.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
