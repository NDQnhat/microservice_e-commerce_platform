'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { AlertOctagon, RotateCcw, Home } from 'lucide-react';

export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log exception to monitoring service or console
    console.error('Unhandled Client Exception captured by App Router Error Boundary:', error);
  }, [error]);

  return (
    <div className="min-h-[70vh] flex items-center justify-center py-16 px-4">
      <div className="max-w-md w-full text-center space-y-6 animate-in fade-in zoom-in-95 duration-300">
        {/* Error icon */}
        <div className="w-20 h-20 rounded-3xl bg-rose-50 flex items-center justify-center mx-auto text-rose-600 border border-rose-200/80 shadow-xs">
          <AlertOctagon className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-mono font-bold tracking-widest uppercase text-rose-600 bg-rose-50 px-3 py-1 rounded-full border border-rose-200">
            Hệ thống gián đoạn
          </span>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-zinc-900">
            Đã xảy ra sự cố ngoài ý muốn
          </h1>
          <p className="text-xs sm:text-sm text-zinc-500 leading-relaxed max-w-sm mx-auto">
            Hệ thống đã tự động ghi nhận lỗi giao diện. Vui lòng tải lại trang hoặc thử lại thao tác vừa thực hiện.
          </p>
        </div>

        {/* Error diagnostic info */}
        {error.message && (
          <div className="p-3 bg-zinc-50 rounded-xl border border-zinc-200/80 text-left font-mono text-[11px] text-zinc-600 overflow-x-auto">
            <p className="font-bold text-zinc-800">Thông báo lỗi:</p>
            <p className="mt-0.5 text-rose-600">{error.message}</p>
            {error.digest && (
              <p className="mt-1 text-zinc-400">Digest: {error.digest}</p>
            )}
          </div>
        )}

        {/* Action Controls */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <button
            onClick={() => reset()}
            className="flex-1 py-3 px-4 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white font-semibold text-xs sm:text-sm transition flex items-center justify-center gap-2 shadow-xs"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Thử lại (Reset)</span>
          </button>

          <Link
            href="/"
            className="flex-1 py-3 px-4 rounded-xl border border-zinc-200 text-zinc-800 font-semibold text-xs sm:text-sm hover:bg-zinc-50 transition flex items-center justify-center gap-2"
          >
            <Home className="w-4 h-4" />
            <span>Về Trang chủ</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
