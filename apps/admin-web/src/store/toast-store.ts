import { create } from 'zustand';
import { ToastMessage, ApiError } from '@/types';
import { ApiClientError } from '@/lib/api-client';

interface ToastStore {
  toasts: ToastMessage[];
  addToast: (toast: Omit<ToastMessage, 'id'>) => string;
  removeToast: (id: string) => void;
  showSuccess: (title: string, detail?: string) => string;
  showError: (error: unknown, fallbackTitle?: string) => string;
  showWarning: (title: string, detail?: string) => string;
  clearAll: () => void;
}

export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],
  addToast: (toast) => {
    const id = 'toast-' + Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = { ...toast, id };
    set((state) => ({ toasts: [...state.toasts, newToast] }));

    const duration = toast.durationMs ?? 5000;
    if (duration > 0) {
      setTimeout(() => {
        get().removeToast(id);
      }, duration);
    }
    return id;
  },
  removeToast: (id) => {
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }));
  },
  showSuccess: (title, detail) => {
    return get().addToast({
      type: 'success',
      title,
      detail,
    });
  },
  showError: (error: unknown, fallbackTitle = 'Action Failed') => {
    let title = fallbackTitle;
    let detail: string | undefined;
    let code: string | undefined;
    let correlationId: string | undefined;

    if (error instanceof ApiClientError) {
      const p: ApiError = error.problem;
      title = p.title || fallbackTitle;
      detail = p.detail || error.message;
      code = p.code;
      correlationId = p.correlationId;
    } else if (error instanceof Error) {
      detail = error.message;
    } else if (typeof error === 'string') {
      detail = error;
    }

    return get().addToast({
      type: 'error',
      title,
      detail,
      code,
      correlationId,
      durationMs: 7000,
    });
  },
  showWarning: (title, detail) => {
    return get().addToast({
      type: 'warning',
      title,
      detail,
    });
  },
  clearAll: () => set({ toasts: [] }),
}));
