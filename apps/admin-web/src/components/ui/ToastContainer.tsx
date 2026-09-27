import React from 'react';
import { useToastStore } from '@/store/toast-store';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X, Copy, Check } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToastStore();
  const [copiedId, setCopiedId] = React.useState<string | null>(null);

  if (toasts.length === 0) return null;

  const handleCopyCorr = (id: string, corrId: string) => {
    navigator.clipboard.writeText(corrId);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2.5 max-w-md w-full pointer-events-none">
      {toasts.map((toast) => {
        const isError = toast.type === 'error';
        const isSuccess = toast.type === 'success';
        const isWarning = toast.type === 'warning';

        return (
          <div
            key={toast.id}
            className={`pointer-events-auto rounded-xl border p-4 shadow-2xl backdrop-blur-md transition-all animate-in slide-in-from-top-2 duration-200 ${
              isError
                ? 'bg-slate-900/95 border-rose-500/40 text-slate-100'
                : isSuccess
                ? 'bg-slate-900/95 border-emerald-500/40 text-slate-100'
                : isWarning
                ? 'bg-slate-900/95 border-amber-500/40 text-slate-100'
                : 'bg-slate-900/95 border-slate-700 text-slate-100'
            }`}
          >
            <div className="flex items-start gap-3">
              <div className="mt-0.5 shrink-0">
                {isError && <AlertCircle className="h-5 w-5 text-rose-400" />}
                {isSuccess && <CheckCircle2 className="h-5 w-5 text-emerald-400" />}
                {isWarning && <AlertTriangle className="h-5 w-5 text-amber-400" />}
                {!isError && !isSuccess && !isWarning && <Info className="h-5 w-5 text-indigo-400" />}
              </div>

              <div className="flex-1 space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-semibold text-white">{toast.title}</p>
                  <button
                    onClick={() => removeToast(toast.id)}
                    className="text-slate-400 hover:text-white p-0.5 rounded transition"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>

                {toast.detail && (
                  <p className="text-xs text-slate-300 leading-relaxed">{toast.detail}</p>
                )}

                {(toast.code || toast.correlationId) && (
                  <div className="pt-2 flex flex-wrap items-center gap-2 text-[11px] font-mono">
                    {toast.code && (
                      <span className="bg-rose-500/20 text-rose-300 px-2 py-0.5 rounded font-semibold border border-rose-500/30">
                        {toast.code}
                      </span>
                    )}
                    {toast.correlationId && (
                      <button
                        onClick={() => handleCopyCorr(toast.id, toast.correlationId!)}
                        className="flex items-center gap-1 bg-slate-800 hover:bg-slate-700 text-slate-300 px-2 py-0.5 rounded border border-slate-700 transition"
                        title="Copy Correlation ID"
                      >
                        {copiedId === toast.id ? (
                          <>
                            <Check className="h-3 w-3 text-emerald-400" />
                            <span className="text-emerald-400">Copied</span>
                          </>
                        ) : (
                          <>
                            <Copy className="h-3 w-3" />
                            <span className="truncate max-w-[140px]">{toast.correlationId}</span>
                          </>
                        )}
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};
