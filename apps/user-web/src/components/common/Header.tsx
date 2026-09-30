'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ShoppingBag,
  Search,
  User as UserIcon,
  Menu,
  X,
  MapPin,
  ChevronDown,
  LogOut,
  Package,
  Bell,
  CheckCircle,
  Truck,
  AlertCircle,
  Clock,
  Sparkles,
} from 'lucide-react';
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { subscribeMockMode, isMockModeActive } from '@/lib/api-client';
import { formatCurrency } from '@/lib/utils';
import { OrderNotification } from '@/types';

// Mock recent order status notifications (FR-013)
const INITIAL_NOTIFICATIONS: OrderNotification[] = [
  {
    id: 'notif-1',
    orderId: 'ord-2026-001',
    orderNumber: 'ORD-20260928-8921',
    type: 'OrderShipped',
    title: 'Đơn hàng đang giao',
    message: 'Đơn hàng #ORD-20260928-8921 đã được giao cho đơn vị vận chuyển GHN.',
    timestamp: '10 phút trước',
    read: false,
  },
  {
    id: 'notif-2',
    orderId: 'ord-2026-002',
    orderNumber: 'ORD-20260930-1042',
    type: 'PaymentSucceeded',
    title: 'Thanh toán thành công',
    message: 'Giao dịch thanh toán 710.000₫ cho đơn #ORD-20260930-1042 đã được xác nhận (PAID).',
    timestamp: '1 giờ trước',
    read: false,
  },
  {
    id: 'notif-3',
    orderId: 'ord-2026-003',
    orderNumber: 'ORD-20260925-4102',
    type: 'OrderCreated',
    title: 'Khởi tạo đơn hàng',
    message: 'Đơn hàng #ORD-20260925-4102 đã được tạo thành công và giữ tồn kho (RESERVED).',
    timestamp: '1 ngày trước',
    read: true,
  },
];

export function Header() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMockMode, setIsMockMode] = useState(true);
  const [isMock, setIsMock] = useState(false);
  const [notifications, setNotifications] = useState<OrderNotification[]>(INITIAL_NOTIFICATIONS);

  const { itemCount, openDrawer, freeShippingThreshold } = useCartStore();
  const { user, isAuthenticated, logout } = useUserStore();

  // STT 16: Subscribe to mock mode state
  useEffect(() => {
    return subscribeMockMode((active) => {
      setIsMockMode(active);
      setIsMock(active);
    });
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      router.push('/products');
    }
  };

  const threshold = freeShippingThreshold || 500000;

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-zinc-200/80 transition-all">
      {/* FIX-M7: Mock Mode Offline Banner */}
      {isMock && (
        <div className="w-full bg-amber-500 text-white text-center text-[11px] font-semibold py-1 px-4">
          ⚡ Demo Mode — Đang chạy trên dữ liệu mô phỏng (Mock). Backend chưa kết nối.
        </div>
      )}
      {/* Top Banner Notice with STT 12 Dynamic Freeship and STT 16 Mock Indicator */}
      <div className="bg-zinc-900 text-white text-xs py-2 px-4 tracking-wide font-medium flex items-center justify-between">
        <div className="hidden sm:block w-36" />
        <div className="flex-1 text-center flex items-center justify-center gap-2">
          <span>Miễn phí vận chuyển toàn quốc cho đơn hàng từ {formatCurrency(threshold)}</span>
          <span className="hidden md:inline text-zinc-500">|</span>
          <span className="hidden md:inline text-zinc-300">Đổi trả linh hoạt trong 30 ngày</span>
        </div>

        {/* STT 16: Sandbox Mock Mode Subtle Indicator */}
        <div className="flex items-center gap-2">
          {isMockMode && (
            <span
              className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 text-[10px] font-mono border border-amber-500/30"
              title="Hệ thống đang hoạt động ở chế độ High-Fidelity Mock Handler (Sandbox Offline)"
            >
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
              <span>Sandbox Mock</span>
            </span>
          )}
        </div>
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
          <div className="flex items-center gap-2 sm:gap-3 flex-1 max-w-md justify-end">
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

            {/* STT 18: Notification Bell Dropdown */}
            <div className="relative">
              <button
                onClick={() => {
                  setIsNotificationOpen(!isNotificationOpen);
                  setIsUserMenuOpen(false);
                }}
                className="relative p-2.5 text-zinc-800 hover:text-zinc-900 rounded-full hover:bg-zinc-100 transition-colors"
                aria-label={`Thông báo đơn hàng (${unreadCount} chưa đọc)`}
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-600 rounded-full animate-pulse" />
                )}
              </button>

              {isNotificationOpen && (
                <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-xl border border-zinc-200/80 py-3 z-50 animate-in fade-in zoom-in-95">
                  <div className="px-4 pb-2.5 border-b border-zinc-100 flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Bell className="w-4 h-4 text-zinc-900" />
                      <h4 className="text-xs font-bold text-zinc-900 uppercase tracking-wide">
                        Thông báo đơn hàng (FR-013)
                      </h4>
                    </div>
                    {unreadCount > 0 && (
                      <button
                        onClick={markAllAsRead}
                        className="text-[10px] text-zinc-500 hover:text-zinc-900 font-medium"
                      >
                        Đánh dấu đã đọc
                      </button>
                    )}
                  </div>

                  <div className="max-h-80 overflow-y-auto divide-y divide-zinc-100">
                    {notifications.map((item) => (
                      <Link
                        key={item.id}
                        href={`/account/orders/${item.orderId}`}
                        onClick={() => setIsNotificationOpen(false)}
                        className={`p-3.5 block transition hover:bg-zinc-50 ${
                          !item.read ? 'bg-zinc-50/60' : ''
                        }`}
                      >
                        <div className="flex items-start gap-2.5">
                          <div className="mt-0.5 shrink-0">
                            {item.type === 'PaymentSucceeded' && (
                              <CheckCircle className="w-4 h-4 text-emerald-600" />
                            )}
                            {item.type === 'OrderShipped' && (
                              <Truck className="w-4 h-4 text-blue-600" />
                            )}
                            {item.type === 'OrderCreated' && (
                              <Clock className="w-4 h-4 text-amber-500" />
                            )}
                            {item.type === 'OrderCancelled' && (
                              <AlertCircle className="w-4 h-4 text-rose-500" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between gap-1">
                              <p className="text-xs font-bold text-zinc-900 truncate">
                                {item.title}
                              </p>
                              <span className="text-[10px] text-zinc-400 shrink-0">
                                {item.timestamp}
                              </span>
                            </div>
                            <p className="text-[11px] text-zinc-600 mt-0.5 line-clamp-2 leading-relaxed">
                              {item.message}
                            </p>
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>

                  <div className="pt-2 px-4 border-t border-zinc-100 text-center">
                    <Link
                      href="/account/orders"
                      onClick={() => setIsNotificationOpen(false)}
                      className="text-xs font-semibold text-zinc-900 hover:underline"
                    >
                      Xem toàn bộ lịch sử đơn hàng
                    </Link>
                  </div>
                </div>
              )}
            </div>

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
                    onClick={() => {
                      setIsUserMenuOpen(!isUserMenuOpen);
                      setIsNotificationOpen(false);
                    }}
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
