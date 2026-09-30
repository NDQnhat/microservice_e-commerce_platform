import React from 'react';
import Link from 'next/link';
import { Compass, Home, ArrowRight, Search } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center py-16 px-4">
      <div className="max-w-md w-full text-center space-y-6 animate-in fade-in zoom-in-95 duration-300">
        {/* 404 Accent Icon */}
        <div className="w-20 h-20 rounded-3xl bg-zinc-100 flex items-center justify-center mx-auto text-zinc-900 border border-zinc-200/80 shadow-xs">
          <Compass className="w-10 h-10 animate-pulse" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-mono font-bold tracking-widest uppercase text-rose-600 bg-rose-50 px-3 py-1 rounded-full border border-rose-200">
            Mã lỗi: 404 Not Found
          </span>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-zinc-900">
            Không tìm thấy trang
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 leading-relaxed max-w-sm mx-auto">
            Trang web hoặc sản phẩm bạn đang tìm kiếm có thể đã thay đổi đường dẫn hoặc tạm thời ngừng kinh doanh.
          </p>
        </div>

        {/* Quick Category Suggestions */}
        <div className="p-4 rounded-2xl bg-zinc-50 border border-zinc-200/60 text-left space-y-2.5">
          <p className="text-xs font-semibold text-zinc-700 flex items-center gap-1.5">
            <Search className="w-3.5 h-3.5 text-zinc-500" />
            <span>Gợi ý khám phá bộ sưu tập nổi bật:</span>
          </p>
          <div className="flex flex-wrap gap-1.5">
            <Link
              href="/products?categoryId=cat-fashion"
              className="text-xs px-2.5 py-1 rounded-lg bg-white border border-zinc-200 text-zinc-700 hover:border-zinc-900 hover:text-zinc-900 transition"
            >
              Thời trang & May mặc
            </Link>
            <Link
              href="/products?categoryId=cat-tech"
              className="text-xs px-2.5 py-1 rounded-lg bg-white border border-zinc-200 text-zinc-700 hover:border-zinc-900 hover:text-zinc-900 transition"
            >
              Thiết bị & Công nghệ
            </Link>
            <Link
              href="/products?categoryId=cat-accessories"
              className="text-xs px-2.5 py-1 rounded-lg bg-white border border-zinc-200 text-zinc-700 hover:border-zinc-900 hover:text-zinc-900 transition"
            >
              Phụ kiện cao cấp
            </Link>
          </div>
        </div>

        {/* Primary CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <Link
            href="/"
            className="flex-1 py-3 px-4 rounded-xl border border-zinc-200 text-zinc-800 font-semibold text-xs sm:text-sm hover:bg-zinc-50 transition flex items-center justify-center gap-2"
          >
            <Home className="w-4 h-4" />
            <span>Quay lại Trang chủ</span>
          </Link>

          <Link
            href="/products"
            className="flex-1 py-3 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white font-semibold text-xs sm:text-sm transition flex items-center justify-center gap-2 shadow-xs"
          >
            <span>Khám phá Sản phẩm</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    </div>
  );
}
