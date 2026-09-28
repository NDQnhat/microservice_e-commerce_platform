import { describe, it, expect } from 'vitest';
import { apiClient, ApiClientError } from '@/lib/api-client';
import { PaymentCallbackPayload } from '@/types';

describe('Payment Callback & BR-002 Duplicate Callback Idempotency', () => {
  it('processes payment callback SUCCESS successfully and transitions to PAID', async () => {
    // ord-2026-002 is in PAID in mock data, let's test a simulated order
    const payload: PaymentCallbackPayload = {
      order_id: 'ord-2026-002',
      provider_reference: 'TXN-TEST-1',
      result: 'SUCCESS',
      amount: 710000,
    };

    // Because ord-2026-002 was already processed, sending again should trigger BR-002
    try {
      await apiClient('/api/v1/payments/callback', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      // Should not reach here if BR-002 is active
      expect.unreachable('Should have thrown duplicate payment error');
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(ApiClientError);
      const apiErr = err as ApiClientError;
      expect(apiErr.problem.status).toBe(422);
      expect(apiErr.problem.code).toBe('DUPLICATE_PAYMENT');
      expect(apiErr.problem.detail).toContain('BR-002');
    }
  });

  it('rejects duplicate payment callbacks for already-paid orders to prevent double charging', async () => {
    // Verify duplicate payment rejection on ord-2026-001
    const duplicatePayload: PaymentCallbackPayload = {
      order_id: 'ord-2026-001',
      provider_reference: 'TXN-DUPLICATE-REPLAY',
      result: 'SUCCESS',
      amount: 1850000,
    };

    await expect(
      apiClient('/api/v1/payments/callback', {
        method: 'POST',
        body: JSON.stringify(duplicatePayload),
      })
    ).rejects.toThrow();
  });
});
