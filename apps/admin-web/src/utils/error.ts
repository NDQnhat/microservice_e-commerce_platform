import { ApiError } from '@/types';
import { ApiClientError } from '@/lib/api-client';

export interface ParsedRfc7807Error {
  type?: string;
  title: string;
  status?: number;
  detail: string;
  code?: string;
  correlationId?: string;
  invalidParams: Record<string, string>;
}

/**
 * RFC 7807 Problem Details Parser (FR-041, NFR-API-002)
 * Unpacks structured backend error contracts into user-friendly titles,
 * localized/descriptive details, and form field violation maps.
 */
export function parseRfc7807Error(error: unknown, fallbackTitle = 'Action Failed'): ParsedRfc7807Error {
  const result: ParsedRfc7807Error = {
    title: fallbackTitle,
    detail: 'An unexpected error occurred. Please verify your inputs.',
    invalidParams: {},
  };

  if (!error) {
    return result;
  }

  // 1. ApiClientError instance
  if (error instanceof ApiClientError) {
    const p: ApiError = error.problem;
    result.type = p.type;
    result.title = p.title || fallbackTitle;
    result.status = p.status;
    result.code = p.code;
    result.correlationId = p.correlationId;
    result.detail = p.detail || error.message || result.detail;

    const rawParams = p.invalid_params || p.invalidParams || [];
    for (const item of rawParams) {
      if (item && item.name) {
        result.invalidParams[item.name] = item.reason;
      }
    }

    return result;
  }

  // 2. Direct object matching RFC 7807 structure (or nested under response.data)
  if (typeof error === 'object' && error !== null) {
    let obj = error as Record<string, unknown>;

    // Handle Axios-like error objects
    if (obj.response && typeof obj.response === 'object' && obj.response !== null) {
      const resp = obj.response as Record<string, unknown>;
      if (resp.data && typeof resp.data === 'object' && resp.data !== null) {
        obj = resp.data as Record<string, unknown>;
      }
    }

    if (obj.title && typeof obj.title === 'string') {
      result.title = obj.title;
    }
    if (obj.type && typeof obj.type === 'string') {
      result.type = obj.type;
    }
    if (typeof obj.status === 'number') {
      result.status = obj.status;
    }
    if (obj.code && typeof obj.code === 'string') {
      result.code = obj.code;
    }
    if (obj.correlationId && typeof obj.correlationId === 'string') {
      result.correlationId = obj.correlationId;
    }
    if (obj.detail && typeof obj.detail === 'string') {
      result.detail = obj.detail;
    } else if (obj.message && typeof obj.message === 'string') {
      result.detail = obj.message;
    }

    const rawParams = (obj.invalid_params || obj.invalidParams) as Array<{ name: string; reason: string }> | undefined;
    if (Array.isArray(rawParams)) {
      for (const item of rawParams) {
        if (item && typeof item.name === 'string') {
          result.invalidParams[item.name] = item.reason || 'Invalid value';
        }
      }
      if (result.detail === 'An unexpected error occurred. Please verify your inputs.' && rawParams.length > 0) {
        result.detail = rawParams.map((p) => `${p.name}: ${p.reason}`).join('; ');
      }
    }

    return result;
  }

  // 3. Standard JS Error
  if (error instanceof Error) {
    result.detail = error.message;
    return result;
  }

  // 4. Primitive string
  if (typeof error === 'string') {
    result.detail = error;
    return result;
  }

  return result;
}
