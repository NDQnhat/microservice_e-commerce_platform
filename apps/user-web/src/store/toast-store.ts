import { create } from 'zustand';
import { ApiError } from '@/types';
import { generateUUID } from '@/lib/utils';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message?: string;
  code?: string;
  correlationId?: string;
  duration?: number;
}

interface ToastState {
  toasts: ToastMessage[];
  addToast: (toast: Omit<ToastMessage, 'id'>) => string;
  removeToast: (id: string) => void;
  showError: (title: string, error?: unknown) => void;
  showSuccess: (title: string, message?: string) => void;
  showInfo: (title: string, message?: string) => void;
  showWarning: (title: string, message?: string) => void;
}

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],

  addToast: (toast) => {
    const id = generateUUID();
    const duration = toast.duration ?? (toast.type === 'error' ? 6000 : 4000);
    const newToast: ToastMessage = { ...toast, id };

    set((state) => ({
      toasts: [...state.toasts, newToast],
    }));

    if (duration > 0) {
      setTimeout(() => {
        get().removeToast(id);
      }, duration);
    }

    return id;
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },

  showSuccess: (title, message) => {
    get().addToast({
      type: 'success',
      title,
      message,
    });
  },

  showInfo: (title, message) => {
    get().addToast({
      type: 'info',
      title,
      message,
    });
  },

  showWarning: (title, message) => {
    get().addToast({
      type: 'warning',
      title,
      message,
    });
  },

  showError: (title, error) => {
    let message = 'Đã có lỗi xảy ra. Vui lòng thử lại sau.';
    let code: string | undefined;
    let correlationId: string | undefined;

    if (error && typeof error === 'object') {
      const anyErr = error as any;
      if (anyErr.problem) {
        const problem: ApiError = anyErr.problem;
        message = problem.detail || problem.title || message;
        code = problem.code;
        correlationId = problem.correlationId;
      } else if (anyErr.detail || anyErr.title) {
        message = anyErr.detail || anyErr.title;
        code = anyErr.code;
        correlationId = anyErr.correlationId;
      } else if (anyErr.message) {
        message = anyErr.message;
      }
    }

    get().addToast({
      type: 'error',
      title,
      message,
      code,
      correlationId,
    });
  },
}));
