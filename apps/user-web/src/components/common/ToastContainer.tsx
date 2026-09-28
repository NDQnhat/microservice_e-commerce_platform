'use client';

import React from 'react';
import { useToastStore, ToastMessage } from '@/store/toast-store';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';
import { cn } from '@/lib/utils';

export function ToastContainer() {
  const { toasts, removeToast } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div
      role="region"
      aria-label="Thông báo hệ thống"
      className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-md w-full px-4 pointer-events-none"
    >
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={() => removeToast(toast.id)} />
      ))}
    </div>
  );
}

function ToastItem({ toast, onDismiss }: { toast: ToastMessage; onDismiss: () => void }) {
  const getIcon = () => {
    switch (toast.type) {
      case 'success':
        return <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />;
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />;
      case 'error':
        return <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />;
      default:
        return <Info className="w-5 h-5 text-sky-600 shrink-0" />;
    }
  };

  const getBorderColor = () => {
    switch (toast.type) {
      case 'success':
        return 'border-emerald-200 bg-white';
      case 'warning':
        return 'border-amber-200 bg-white';
      case 'error':
        return 'border-rose-200 bg-white';
      default:
        return 'border-zinc-200 bg-white';
    }
  };

  return (
    <div
      className={cn(
        'pointer-events-auto rounded-xl p-4 shadow-lg border transition-all duration-300 ease-out animate-in slide-in-from-bottom-3',
        getBorderColor()
      )}
    >
      <div className="flex items-start gap-3">
        {getIcon()}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2">
            <h4 className="text-sm font-semibold text-zinc-900 leading-tight">{toast.title}</h4>
            {toast.code && (
              <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded bg-zinc-100 text-zinc-600 border border-zinc-200">
                {toast.code}
              </span>
            )}
          </div>
          {toast.message && (
            <p className="mt-1 text-xs text-zinc-600 leading-relaxed break-words">{toast.message}</p>
          )}
          {toast.correlationId && (
            <p className="mt-1.5 text-[10px] text-zinc-400 font-mono">
              Correlation ID: {toast.correlationId}
            </p>
          )}
        </div>
        <button
          onClick={onDismiss}
          className="text-zinc-400 hover:text-zinc-700 transition p-1 rounded-md"
          aria-label="Đóng"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
