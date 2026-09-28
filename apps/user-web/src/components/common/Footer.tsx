'use client';

import React from 'react';
import Link from 'next/link';
import { ShieldCheck, Truck, RotateCcw, Headphones } from 'lucide-react';

export function Footer() {
  return (
    <footer className="bg-white border-t border-zinc-200/80 mt-auto">
      {/* Brand value propositions */}
      <div className="border-b border-zinc-100 bg-zinc-50/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-zinc-900 text-white shrink-0">
                <Truck className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-zinc-900">Giao hàng Miễn phí</h4>
                <p className="mt-1 text-xs text-zinc-500 leading-relaxed">
                  Áp dụng toàn quốc cho đơn hàng đạt giá trị từ 500.000₫.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-zinc-900 text-white shrink-0">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-zinc-900">Đổi trả trong 30 ngày</h4>
                <p className="mt-1 text-xs text-zinc-500 leading-relaxed">
                  Bảo hành và hỗ trợ đổi trả nguyên hộp nếu không vừa ý.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-zinc-900 text-white shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-zinc-900">100% Chính hãng</h4>
                <p className="mt-1 text-xs text-zinc-500 leading-relaxed">
                  Cam kết chất lượng cao cấp, nguồn gốc vật liệu minh bạch.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-zinc-900 text-white shrink-0">
                <Headphones className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-zinc-900">Hỗ trợ 24/7</h4>
                <p className="mt-1 text-xs text-zinc-500 leading-relaxed">
                  Đội ngũ chăm sóc khách hàng luôn sẵn sàng đồng hành.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main footer navigation */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10">
          <div className="lg:col-span-2 space-y-4">
            <Link href="/" className="inline-block">
              <span className="text-2xl font-black tracking-tighter text-zinc-900 uppercase font-sans">
                ATELIER<span className="text-rose-600">.</span>
              </span>
            </Link>
            <p className="text-sm text-zinc-500 leading-relaxed max-w-sm">
              Định hình phong cách sống hiện đại qua những sản phẩm may mặc và đồ công nghệ tinh xảo, tối giản và bền vững theo thời gian.
            </p>
            <div className="text-xs text-zinc-400 font-mono space-y-1">
              <p>Hệ thống vi dịch vụ phân tán — Distributed E-Commerce</p>
              <p>Tuân thủ nghiêm ngặt tính bất biến đơn hàng & Idempotency</p>
            </div>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-zinc-900 uppercase tracking-wider">Danh mục</h5>
            <ul className="mt-4 space-y-2.5 text-sm text-zinc-600">
              <li>
                <Link href="/products?categoryId=cat-fashion" className="hover:text-zinc-900 transition">
                  Thời trang & May mặc
                </Link>
              </li>
              <li>
                <Link href="/products?categoryId=cat-tech" className="hover:text-zinc-900 transition">
                  Thiết bị & Công nghệ
                </Link>
              </li>
              <li>
                <Link href="/products?categoryId=cat-accessories" className="hover:text-zinc-900 transition">
                  Phụ kiện cao cấp
                </Link>
              </li>
              <li>
                <Link href="/products?categoryId=cat-lifestyle" className="hover:text-zinc-900 transition">
                  Không gian sống & Decor
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-zinc-900 uppercase tracking-wider">Khách hàng</h5>
            <ul className="mt-4 space-y-2.5 text-sm text-zinc-600">
              <li>
                <Link href="/account/orders" className="hover:text-zinc-900 transition">
                  Tra cứu đơn hàng
                </Link>
              </li>
              <li>
                <Link href="/account/addresses" className="hover:text-zinc-900 transition">
                  Sổ địa chỉ giao hàng
                </Link>
              </li>
              <li>
                <Link href="/cart" className="hover:text-zinc-900 transition">
                  Giỏ hàng của bạn
                </Link>
              </li>
              <li>
                <span className="text-zinc-400 cursor-not-allowed">Chính sách bảo mật</span>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-xs font-semibold text-zinc-900 uppercase tracking-wider">Nhận bản tin</h5>
            <p className="mt-4 text-xs text-zinc-500 leading-relaxed">
              Nhận thông báo sớm nhất về các đợt ra mắt sản phẩm giới hạn và chương trình ưu đãi đặc quyền.
            </p>
            <form onSubmit={(e) => e.preventDefault()} className="mt-4 space-y-2">
              <input
                type="email"
                placeholder="Email của bạn..."
                className="w-full bg-zinc-100 text-sm text-zinc-900 placeholder-zinc-400 px-3.5 py-2 rounded-xl border border-zinc-200 focus:border-zinc-900 focus:bg-white focus:outline-none transition"
              />
              <button
                type="submit"
                className="w-full bg-zinc-900 hover:bg-zinc-800 text-white text-xs font-semibold py-2.5 rounded-xl transition shadow-xs"
              >
                Đăng ký ngay
              </button>
            </form>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-zinc-100 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-zinc-400">
          <p>&copy; {new Date().getFullYear()} ATELIER E-Commerce Platform. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <span>Powered by Next.js 15 & Spring Boot Microservices</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
