import { ApiError } from '@/types';

function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'corr-' + Math.random().toString(36).substring(2, 15);
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
  
  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': correlationId,
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorProblem: ApiError;
    try {
      errorProblem = await response.json();
    } catch {
      errorProblem = {
        type: 'about:blank',
        title: response.statusText,
        status: response.status,
        detail: `HTTP error ${response.status}`,
        instance: endpoint,
        timestamp: new Date().toISOString(),
      };
    }
    throw new ApiClientError(errorProblem);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
