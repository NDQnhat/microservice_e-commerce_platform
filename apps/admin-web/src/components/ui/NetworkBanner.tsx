import React, { useState, useEffect } from 'react';
import { WifiOff, Wifi } from 'lucide-react';

export const NetworkBanner: React.FC = () => {
  const [isOnline, setIsOnline] = useState<boolean>(() =>
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [showRestored, setShowRestored] = useState<boolean>(false);

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | null = null;

    const handleOnline = () => {
      setIsOnline(true);
      setShowRestored(true);
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        setShowRestored(false);
      }, 3000);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowRestored(false);
      if (timer) clearTimeout(timer);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      if (timer) clearTimeout(timer);
    };
  }, []);

  if (!isOnline) {
    return (
      <div
        role="alert"
        aria-live="assertive"
        className="w-full bg-rose-500/20 text-rose-300 border-b border-rose-500/30 px-4 py-2 text-xs font-semibold flex items-center justify-center gap-2 z-50 sticky top-0 backdrop-blur-md transition-all shadow-sm"
      >
        <WifiOff className="h-4 w-4 shrink-0 text-rose-400 animate-pulse" />
        <span>
          Mất kết nối Internet. Các thao tác ghi dữ liệu tạm thời bị khóa để tránh thất thoát dữ liệu.
        </span>
      </div>
    );
  }

  if (showRestored) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="w-full bg-emerald-500/20 text-emerald-300 border-b border-emerald-500/30 px-4 py-2 text-xs font-semibold flex items-center justify-center gap-2 z-50 sticky top-0 backdrop-blur-md transition-all shadow-sm"
      >
        <Wifi className="h-4 w-4 shrink-0 text-emerald-400" />
        <span>Đã khôi phục kết nối Internet. Hệ thống đã sẵn sàng đồng bộ dữ liệu.</span>
      </div>
    );
  }

  return null;
};
