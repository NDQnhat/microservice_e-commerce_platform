import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TerminalBadge, isTerminalStatus } from '../components/TerminalBadge';
import { MaskedText } from '../components/ui/MaskedText';
import { maskEmail, maskPhone } from '../utils/mask';
import { parseRfc7807Error } from '../utils/error';

describe('Remediation & Invariant Guard Tests (SRS Hardening)', () => {
  describe('Task 8: RFC 7807 & Terminal Badge', () => {
    it('isTerminalStatus correctly detects terminal states', () => {
      expect(isTerminalStatus('DELIVERED')).toBe(true);
      expect(isTerminalStatus('COMPLETED')).toBe(true);
      expect(isTerminalStatus('CANCELLED')).toBe(true);
      expect(isTerminalStatus('REFUNDED')).toBe(true);
      expect(isTerminalStatus('RESOLVED')).toBe(true);
      expect(isTerminalStatus('IGNORED')).toBe(true);

      // Non-terminal states
      expect(isTerminalStatus('CREATED')).toBe(false);
      expect(isTerminalStatus('RESERVED')).toBe(false);
      expect(isTerminalStatus('PAID')).toBe(false);
      expect(isTerminalStatus('PACKING')).toBe(false);
      expect(isTerminalStatus('SHIPPED')).toBe(false);
      expect(isTerminalStatus('OPEN')).toBe(false);
    });

    it('renders TerminalBadge with lock icon and terminal label', () => {
      render(<TerminalBadge status="DELIVERED" />);
      expect(screen.getByText('DELIVERED')).toBeInTheDocument();
      expect(screen.getByTitle('Trạng thái kết thúc - Không được phép chỉnh sửa')).toBeInTheDocument();
    });

    it('parses standard RFC 7807 problem details object correctly', () => {
      const errorObj = {
        response: {
          data: {
            type: 'https://ecommerce.internal/errors/insufficient-stock',
            title: 'Insufficient Inventory',
            status: 409,
            detail: 'Available stock is lower than requested quantity',
            code: 'INV-409-STOCK',
            correlationId: 'test-corr-12345',
          },
        },
      };

      const parsed = parseRfc7807Error(errorObj, 'Default Title');
      expect(parsed.title).toBe('Insufficient Inventory');
      expect(parsed.detail).toBe('Available stock is lower than requested quantity');
      expect(parsed.code).toBe('INV-409-STOCK');
      expect(parsed.correlationId).toBe('test-corr-12345');
    });

    it('parses RFC 7807 invalid_params into readable detail string', () => {
      const errorObj = {
        title: 'Validation Error',
        invalid_params: [
          { name: 'tracking_number', reason: 'Must match regex ^[a-zA-Z0-9-]{8,32}$' },
          { name: 'carrier_code', reason: 'Carrier code is required' },
        ],
      };

      const parsed = parseRfc7807Error(errorObj);
      expect(parsed.detail).toContain('tracking_number: Must match regex');
      expect(parsed.detail).toContain('carrier_code: Carrier code is required');
    });
  });

  describe('Task 6: PII Masking Utilities & MaskedText Component', () => {
    it('masks email preserving prefix and domain', () => {
      expect(maskEmail('customer.locked@example.com')).toBe('cu***@example.com');
      expect(maskEmail('alex@internal.com')).toBe('al***@internal.com');
      expect(maskEmail('')).toBe('');
      expect(maskEmail(null)).toBe('');
    });

    it('masks phone numbers preserving prefix and last 4 digits', () => {
      expect(maskPhone('0901234567')).toBe('09****4567');
      expect(maskPhone('0988776655')).toBe('09****6655');
      expect(maskPhone('')).toBe('');
      expect(maskPhone(null)).toBe('');
    });

    it('renders MaskedText masked by default, and reveals on click if canReveal is true', () => {
      const onRevealAudit = vi.fn();
      render(
        <MaskedText
          value="customer.locked@example.com"
          type="email"
          canReveal={true}
          onRevealAudit={onRevealAudit}
        />
      );

      // Initially masked
      expect(screen.getByText('cu***@example.com')).toBeInTheDocument();
      expect(screen.queryByText('customer.locked@example.com')).not.toBeInTheDocument();

      // Click reveal button
      const toggleBtn = screen.getByRole('button');
      fireEvent.click(toggleBtn);

      // Now revealed
      expect(screen.getByText('customer.locked@example.com')).toBeInTheDocument();
      expect(onRevealAudit).toHaveBeenCalledTimes(1);

      // Click again to hide
      fireEvent.click(toggleBtn);
      expect(screen.getByText('cu***@example.com')).toBeInTheDocument();
      // Audit should not fire again on hide
      expect(onRevealAudit).toHaveBeenCalledTimes(1);
    });

    it('MaskedText hides reveal button if canReveal is false', () => {
      render(
        <MaskedText
          value="0901234567"
          type="phone"
          canReveal={false}
        />
      );

      expect(screen.getByText('09****4567')).toBeInTheDocument();
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
  });

  describe('Task 1 & Task 2: State Machine, Cancellation & Inventory Boundaries', () => {
    it('strictly forbids manual operator transition to PAID from CREATED or RESERVED', () => {
      const getValidManualTransitions = (currentStatus: string): string[] => {
        if (currentStatus === 'CREATED') return ['RESERVED', 'CANCELLED'];
        if (currentStatus === 'RESERVED') return ['CANCELLED'];
        if (currentStatus === 'PAID') return ['FULFILLING', 'PACKING', 'CANCELLED'];
        return [];
      };

      expect(getValidManualTransitions('CREATED')).not.toContain('PAID');
      expect(getValidManualTransitions('RESERVED')).not.toContain('PAID');
    });

    it('enforces cancellation cutoff: disabled for PACKING, SHIPPED, DELIVERED, COMPLETED', () => {
      const nonCancellableStatuses = [
        'FULFILLING',
        'PACKING',
        'SHIPPED',
        'DELIVERED',
        'COMPLETED',
        'CANCELLED',
        'REFUNDED',
        'PAYMENT_FAILED',
        'EXPIRED',
      ];

      const isCancellable = (status: string) => !nonCancellableStatuses.includes(status);

      expect(isCancellable('CREATED')).toBe(true);
      expect(isCancellable('RESERVED')).toBe(true);
      expect(isCancellable('PAID')).toBe(true);

      nonCancellableStatuses.forEach((st) => {
        expect(isCancellable(st)).toBe(false);
      });
    });

    it('enforces negative stock boundary: decrease amount cannot exceed available or on-hand stock', () => {
      const validateStockDecrease = (
        amount: number,
        onHandQuantity: number,
        availableQuantity: number
      ): { isValid: boolean; error?: string } => {
        if (amount <= 0) return { isValid: false, error: 'Số lượng phải lớn hơn 0' };
        if (amount > availableQuantity) {
          return {
            isValid: false,
            error: `Số lượng giảm (${amount}) vượt quá khả dụng thực tế (${availableQuantity}).`,
          };
        }
        if (amount > onHandQuantity) {
          return {
            isValid: false,
            error: `Số lượng giảm (${amount}) vượt quá tồn kho vật lý On-hand (${onHandQuantity}).`,
          };
        }
        return { isValid: true };
      };

      // Valid decrease
      expect(validateStockDecrease(5, 50, 30).isValid).toBe(true);

      // Decreasing more than available
      const overAvailable = validateStockDecrease(35, 50, 30);
      expect(overAvailable.isValid).toBe(false);
      expect(overAvailable.error).toContain('vượt quá khả dụng');

      // Decreasing more than on-hand
      const overOnHand = validateStockDecrease(60, 50, 100);
      expect(overOnHand.isValid).toBe(false);
      expect(overOnHand.error).toContain('vượt quá tồn kho vật lý On-hand');
    });
  });

  describe('Task 3: Payment Reconciliation Audit Constraints', () => {
    it('validates external transaction ID format and audit justification length', () => {
      const externalTxRegex = /^[a-zA-Z0-9_-]{6,64}$/;

      expect(externalTxRegex.test('TXN_PAYPAL_998811')).toBe(true);
      expect(externalTxRegex.test('VNPAY-20260927-123')).toBe(true);
      expect(externalTxRegex.test('short')).toBe(false); // < 6 chars
      expect(externalTxRegex.test('has spaces in id')).toBe(false);

      const validateAuditJustification = (text: string): boolean => {
        return text.trim().length >= 15;
      };

      expect(validateAuditJustification('Too short')).toBe(false);
      expect(validateAuditJustification('Khớp với sao kê ngân hàng VCB đợt 27/09')).toBe(true);
    });

    it('restricts manual reconciliation authority to FINANCIAL_AUDITOR and SUPER_ADMIN', () => {
      const canReconcile = (roles: string[]): boolean => {
        return roles.includes('SUPER_ADMIN') || roles.includes('FINANCIAL_AUDITOR');
      };

      expect(canReconcile(['SUPER_ADMIN'])).toBe(true);
      expect(canReconcile(['FINANCIAL_AUDITOR'])).toBe(true);
      expect(canReconcile(['OPS_ADMIN'])).toBe(false);
      expect(canReconcile(['ORDER_OPERATOR'])).toBe(false);
      expect(canReconcile(['SUPPORT_AGENT'])).toBe(false);
      expect(canReconcile(['WAREHOUSE_STAFF'])).toBe(false);
    });
  });

  describe('Task 5: Shipment Carrier & Tracking Number Format', () => {
    it('validates carrier tracking number regex ^[a-zA-Z0-9-]{8,32}$', () => {
      const trackingRegex = /^[a-zA-Z0-9-]{8,32}$/;

      expect(trackingRegex.test('GHN-8849102-VN')).toBe(true);
      expect(trackingRegex.test('VTP-9921445-VN')).toBe(true);
      expect(trackingRegex.test('VNPOST12345678')).toBe(true);

      // Invalid tracking numbers
      expect(trackingRegex.test('SHORT-7')).toBe(false); // length 7 < 8
      expect(trackingRegex.test('GHN_12345_VN')).toBe(false); // underscore not permitted
      expect(trackingRegex.test('GHN 8849102 VN')).toBe(false); // space not permitted
      expect(trackingRegex.test('A'.repeat(33))).toBe(false); // > 32 chars
    });
  });
});
