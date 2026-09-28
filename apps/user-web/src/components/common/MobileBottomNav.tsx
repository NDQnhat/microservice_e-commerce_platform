'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Compass, ShoppingBag, User } from 'lucide-react';
import { useCartStore } from '@/store/cart-store';
import { useUserStore } from '@/store/user-store';
import { cn } from '@/lib/utils';

export function MobileBottomNav() {
  const pathname = usePathname();
  const { itemCount, openDrawer } = useCartStore();
  const { isAuthenticated } = useUserStore();

  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-zinc-200 px-4 py-2 flex items-center justify-around shadow-lg">
      <Link
        href="/"
        className={cn(
          'flex flex-col items-center gap-1 text-xs font-medium py-1 px-3 rounded-lg transition',
          pathname === '/' ? 'text-zinc-900 font-bold' : 'text-zinc-500 hover:text-zinc-900'
        )}
      >
        <Home className="w-5 h-5" />
        <span>Trang chủ</span>
      </Link>

      <Link
        href="/products"
        className={cn(
          'flex flex-col items-center gap-1 text-xs font-medium py-1 px-3 rounded-lg transition',
          pathname.startsWith('/products') ? 'text-zinc-900 font-bold' : 'text-zinc-500 hover:text-zinc-900'
        )}
      >
        <Compass className="w-5 h-5" />
        <span>Khám phá</span>
      </Link>

      <button
        onClick={openDrawer}
        className="flex flex-col items-center gap-1 text-xs font-medium py-1 px-3 rounded-lg text-zinc-500 hover:text-zinc-900 transition relative"
      >
        <div className="relative">
          <ShoppingBag className="w-5 h-5" />
          {itemCount > 0 && (
            <span className="absolute -top-1 -right-2 min-w-[16px] h-4 px-1 bg-zinc-900 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
              {itemCount}
            </span>
          )}
        </div>
        <span>Giỏ hàng</span>
      </button>

      <Link
        href={isAuthenticated ? '/account/orders' : '/auth/login'}
        className={cn(
          'flex flex-col items-center gap-1 text-xs font-medium py-1 px-3 rounded-lg transition',
          pathname.startsWith('/account') || pathname.startsWith('/auth')
            ? 'text-zinc-900 font-bold'
            : 'text-zinc-500 hover:text-zinc-900'
        )}
      >
        <User className="w-5 h-5" />
        <span>{isAuthenticated ? 'Tài khoản' : 'Đăng nhập'}</span>
      </Link>
    </div>
  );
}
