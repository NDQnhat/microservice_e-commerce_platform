import { ApiError } from '@/types';
import { useAuthStore } from '@/store/auth-store';
import { useToastStore } from '@/store/toast-store';

function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'corr-admin-' + Math.random().toString(36).substring(2, 15);
}

export class ApiClientError extends Error {
  constructor(public readonly problem: ApiError) {
    super(problem.detail || problem.title || 'API Error');
    this.name = 'ApiClientError';
  }
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const correlationId = generateCorrelationId();
  const token = typeof window !== 'undefined' ? localStorage.getItem('admin_access_token') : null;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': correlationId,
    ...(options.headers as Record<string, string>),
  };

  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401) {
      useAuthStore.getState().logout();
      useToastStore.getState().showWarning(
        'Phiên làm việc đã hết hạn',
        'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.'
      );
      if (typeof window !== 'undefined' && window.location && window.location.pathname !== '/login') {
        try {
          window.location.href = '/login';
        } catch {
          // Fallback for jsdom / test environment
        }
      }
    }
    let errorProblem: ApiError;
    try {
      errorProblem = await response.json();
    } catch {
      errorProblem = {
        type: 'about:blank',
        title: response.statusText,
        status: response.status,
        detail: `HTTP error ${response.status}: ${response.statusText}`,
        instance: endpoint,
        timestamp: new Date().toISOString(),
      };
    }

    if (!errorProblem.correlationId) {
      errorProblem.correlationId = response.headers.get('x-correlation-id') || correlationId;
    }

    throw new ApiClientError(errorProblem);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
