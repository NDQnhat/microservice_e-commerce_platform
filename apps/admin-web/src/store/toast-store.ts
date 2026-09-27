import { create } from 'zustand';
import { ToastMessage } from '@/types';
import { parseRfc7807Error } from '@/utils/error';

interface ToastStore {
  toasts: ToastMessage[];
  addToast: (toast: Omit<ToastMessage, 'id'>) => string;
  removeToast: (id: string) => void;
  showSuccess: (title: string, detail?: string) => string;
  showError: (error: unknown, fallbackTitle?: string) => string;
  showWarning: (title: string, detail?: string) => string;
  showInfo: (title: string, detail?: string) => string;
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
    const parsed = parseRfc7807Error(error, fallbackTitle);

    return get().addToast({
      type: 'error',
      title: parsed.title,
      detail: parsed.detail,
      code: parsed.code,
      correlationId: parsed.correlationId,
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
  showInfo: (title, detail) => {
    return get().addToast({
      type: 'info',
      title,
      detail,
    });
  },
  clearAll: () => set({ toasts: [] }),
}));
