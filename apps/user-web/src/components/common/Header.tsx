'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ShoppingBag, Search, User as UserIcon, Menu, X, MapPin, ChevronDown, LogOut, Package } from 'lucide-react';
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';

export function Header() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const { itemCount, openDrawer } = useCartStore();
  const { user, isAuthenticated, logout } = useUserStore();

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      router.push('/products');
    }
  };

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-zinc-200/80 transition-all">
      {/* Top Banner Notice */}
      <div className="bg-zinc-900 text-white text-xs py-2 px-4 text-center tracking-wide font-medium flex items-center justify-center gap-2">
        <span>Miễn phí vận chuyển toàn quốc cho đơn hàng từ 500.000₫</span>
        <span className="hidden md:inline text-zinc-400">|</span>
        <span className="hidden md:inline text-zinc-300">Đổi trả linh hoạt trong 30 ngày</span>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20 gap-4">
          {/* Mobile menu trigger */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden p-2 text-zinc-700 hover:text-zinc-900 rounded-lg hover:bg-zinc-100 transition"
            aria-label="Toggle menu"
          >
            {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>

          {/* Brand Logo */}
          <Link href="/" className="flex items-center gap-2 shrink-0 group">
            <span className="text-2xl font-black tracking-tighter text-zinc-900 uppercase font-sans">
              ATELIER<span className="text-rose-600">.</span>
            </span>
            <span className="text-[10px] font-mono tracking-widest text-zinc-500 uppercase hidden sm:inline-block border-l border-zinc-300 pl-2">
              Storefront
            </span>
          </Link>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center space-x-8 text-sm font-medium">
            <Link
              href="/"
              className="text-zinc-700 hover:text-zinc-900 transition-colors py-1 hover:border-b-2 hover:border-zinc-900"
            >
              Trang chủ
            </Link>
            <Link
              href="/products"
              className="text-zinc-700 hover:text-zinc-900 transition-colors py-1 hover:border-b-2 hover:border-zinc-900"
            >
              Sản phẩm
            </Link>
            <Link
              href="/products?categoryId=cat-fashion"
              className="text-zinc-700 hover:text-zinc-900 transition-colors py-1 hover:border-b-2 hover:border-zinc-900"
            >
              Thời trang
            </Link>
            <Link
              href="/products?categoryId=cat-tech"
              className="text-zinc-700 hover:text-zinc-900 transition-colors py-1 hover:border-b-2 hover:border-zinc-900"
            >
              Công nghệ
            </Link>
            <Link
              href="/products?categoryId=cat-accessories"
              className="text-zinc-700 hover:text-zinc-900 transition-colors py-1 hover:border-b-2 hover:border-zinc-900"
            >
              Phụ kiện
            </Link>
          </nav>

          {/* Search bar & Actions */}
          <div className="flex items-center gap-3 sm:gap-4 flex-1 max-w-md justify-end">
            <form onSubmit={handleSearchSubmit} className="relative hidden lg:block w-full max-w-xs">
              <input
                type="text"
                placeholder="Tìm kiếm sản phẩm..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-zinc-100 text-sm text-zinc-900 placeholder-zinc-500 pl-9 pr-4 py-2 rounded-full border border-transparent focus:border-zinc-300 focus:bg-white focus:outline-none transition-all"
              />
              <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
            </form>

            {/* Cart Trigger */}
            <button
              onClick={openDrawer}
              className="relative p-2.5 text-zinc-800 hover:text-zinc-900 rounded-full hover:bg-zinc-100 transition-colors group"
              aria-label={`Giỏ hàng (${itemCount} sản phẩm)`}
            >
              <ShoppingBag className="w-5 h-5 group-hover:scale-110 transition-transform" />
              {itemCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 min-w-[20px] h-5 px-1 bg-zinc-900 text-white text-[11px] font-bold rounded-full flex items-center justify-center animate-in zoom-in">
                  {itemCount}
                </span>
              )}
            </button>

            {/* User Account Dropdown */}
            <div className="relative">
              {isAuthenticated && user ? (
                <div className="relative">
                  <button
                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                    className="flex items-center gap-2 py-1.5 px-3 rounded-full hover:bg-zinc-100 border border-zinc-200 text-sm font-medium text-zinc-800 transition"
                  >
                    <div className="w-6 h-6 rounded-full bg-zinc-900 text-white flex items-center justify-center text-xs font-semibold">
                      {user.fullName.charAt(0)}
                    </div>
                    <span className="hidden sm:inline max-w-[120px] truncate">{user.fullName}</span>
                    <ChevronDown className="w-3.5 h-3.5 text-zinc-500" />
                  </button>

                  {isUserMenuOpen && (
                    <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-zinc-200/80 py-2 z-50 animate-in fade-in zoom-in-95">
                      <div className="px-4 py-2 border-b border-zinc-100">
                        <p className="text-xs text-zinc-500">Đăng nhập với</p>
                        <p className="text-sm font-semibold text-zinc-900 truncate">{user.email}</p>
                      </div>

                      <Link
                        href="/account/orders"
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-zinc-700 hover:bg-zinc-50 hover:text-zinc-900"
                      >
                        <Package className="w-4 h-4 text-zinc-500" />
                        Đơn hàng của tôi
                      </Link>

                      <Link
                        href="/account/addresses"
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-zinc-700 hover:bg-zinc-50 hover:text-zinc-900"
                      >
                        <MapPin className="w-4 h-4 text-zinc-500" />
                        Sổ địa chỉ giao hàng
                      </Link>

                      <div className="border-t border-zinc-100 mt-1 pt-1">
                        <button
                          onClick={() => {
                            setIsUserMenuOpen(false);
                            logout();
                          }}
                          className="flex items-center gap-2.5 w-full text-left px-4 py-2 text-sm text-rose-600 hover:bg-rose-50"
                        >
                          <LogOut className="w-4 h-4" />
                          Đăng xuất
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Link
                    href="/auth/login"
                    className="text-sm font-medium text-zinc-700 hover:text-zinc-900 px-3 py-1.5 rounded-lg hover:bg-zinc-100 transition"
                  >
                    Đăng nhập
                  </Link>
                  <Link
                    href="/auth/register"
                    className="hidden sm:inline-flex text-sm font-medium bg-zinc-900 hover:bg-zinc-800 text-white px-3.5 py-1.5 rounded-full transition shadow-xs"
                  >
                    Đăng ký
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Mobile search bar */}
        <div className="pb-3 lg:hidden">
          <form onSubmit={handleSearchSubmit} className="relative w-full">
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm, thương hiệu..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-100 text-sm text-zinc-900 placeholder-zinc-500 pl-9 pr-4 py-2 rounded-full border border-transparent focus:border-zinc-300 focus:bg-white focus:outline-none transition-all"
            />
            <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
          </form>
        </div>

        {/* Mobile menu collapsible */}
        {isMobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-zinc-200/80 space-y-2 animate-in slide-in-from-top-2">
            <Link
              href="/"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Trang chủ
            </Link>
            <Link
              href="/products"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Tất cả sản phẩm
            </Link>
            <Link
              href="/products?categoryId=cat-fashion"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Thời trang & May mặc
            </Link>
            <Link
              href="/products?categoryId=cat-tech"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Thiết bị & Công nghệ
            </Link>
            <Link
              href="/products?categoryId=cat-accessories"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Phụ kiện cao cấp
            </Link>
            <Link
              href="/account/orders"
              onClick={() => setIsMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-base font-medium text-zinc-800 hover:bg-zinc-100"
            >
              Đơn hàng của tôi
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
