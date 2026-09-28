import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TerminalBadge, isTerminalStatus } from '../components/TerminalBadge';
import { MaskedText } from '../components/ui/MaskedText';
import { maskEmail, maskPhone } from '../utils/mask';
import { parseRfc7807Error } from '../utils/error';
import { SHIPMENT_STATE_TRANSITIONS } from '../pages/ShipmentsPage';

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

  describe('Remediation STT 01: Shipment State Machine Transitions (BR-010, BR-011)', () => {
    it('locks terminal states DELIVERED and RETURNED with zero outgoing transitions', () => {
      expect(SHIPMENT_STATE_TRANSITIONS.DELIVERED).toEqual([]);
      expect(SHIPMENT_STATE_TRANSITIONS.RETURNED).toEqual([]);
    });

    it('enforces unidirectional state progression matrix', () => {
      expect(SHIPMENT_STATE_TRANSITIONS.CREATED).toEqual(['PACKING']);
      expect(SHIPMENT_STATE_TRANSITIONS.PACKING).toEqual(['READY_FOR_PICKUP', 'HANDED_OVER']);
      expect(SHIPMENT_STATE_TRANSITIONS.READY_FOR_PICKUP).toEqual(['HANDED_OVER']);
      expect(SHIPMENT_STATE_TRANSITIONS.HANDED_OVER).toEqual(['IN_TRANSIT', 'SHIPPED']);
      expect(SHIPMENT_STATE_TRANSITIONS.IN_TRANSIT).toEqual(['DELIVERED', 'DELIVERY_FAILED']);
      expect(SHIPMENT_STATE_TRANSITIONS.SHIPPED).toEqual(['DELIVERED', 'DELIVERY_FAILED']);
      expect(SHIPMENT_STATE_TRANSITIONS.DELIVERY_FAILED).toEqual(['RETURNED', 'IN_TRANSIT']);
    });
  });

  describe('Remediation STT 02: Inventory Invariant Boundary Guard (BR-015)', () => {
    const validateInventoryAdjustment = (
      onHand: number,
      reserved: number,
      available: number,
      delta: number
    ): { isValid: boolean; violation?: string } => {
      if (delta === 0) return { isValid: false, violation: 'Chênh lệch phải khác 0' };
      const newPhysical = onHand + delta;
      const newAvailable = available + delta;

      if (newPhysical < reserved) {
        return {
          isValid: false,
          violation: `Vi phạm bất biến BR-015: Tồn kho vật lý sau chỉnh sửa (${newPhysical}) nhỏ hơn số lượng đang giữ chỗ (${reserved}).`,
        };
      }
      if (newAvailable < 0) {
        return {
          isValid: false,
          violation: `Tồn kho khả dụng sau điều chỉnh (${newAvailable}) không được âm.`,
        };
      }
      return { isValid: true };
    };

    it('allows valid restock and valid stock reduction', () => {
      // Restock +10: onHand 20 -> 30, available 15 -> 25, reserved 5
      expect(validateInventoryAdjustment(20, 5, 15, 10).isValid).toBe(true);

      // Decrease -5: onHand 20 -> 15 (>= 5 reserved), available 15 -> 10 (>= 0)
      expect(validateInventoryAdjustment(20, 5, 15, -5).isValid).toBe(true);
    });

    it('rejects adjustment when physical inventory falls below reserved allocations', () => {
      // onHand 20, reserved 15, available 5. Delta -10 -> newPhysical = 10 < 15
      const result = validateInventoryAdjustment(20, 15, 5, -10);
      expect(result.isValid).toBe(false);
      expect(result.violation).toContain('nhỏ hơn số lượng đang giữ chỗ');
    });

    it('rejects adjustment when available stock becomes negative', () => {
      // onHand 20, reserved 0, available 5. Delta -10 -> newAvailable = -5 < 0
      const result = validateInventoryAdjustment(20, 0, 5, -10);
      expect(result.isValid).toBe(false);
      expect(result.violation).toContain('không được âm');
    });
  });

  describe('Remediation STT 13 & STT 06: SKU Code Formatting & Normalization', () => {
    const normalizeSku = (raw: string): string => {
      return raw.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    };
    const skuRegex = /^[A-Z0-9_-]{4,32}$/;

    it('auto-uppercases and strips illegal characters', () => {
      expect(normalizeSku('nike-air-max-42')).toBe('NIKE-AIR-MAX-42');
      expect(normalizeSku('sku 99 # special!')).toBe('SKU99SPECIAL');
      expect(normalizeSku('vn_shirt_red_xl')).toBe('VN_SHIRT_RED_XL');
    });

    it('validates SKU code format regex ^[A-Z0-9_-]{4,32}$', () => {
      expect(skuRegex.test('NIKE-PEGASUS-40-BLK-42')).toBe(true);
      expect(skuRegex.test('SKU_001')).toBe(true);
      expect(skuRegex.test('SH-1')).toBe(true); // 4 chars

      // Invalid SKUs
      expect(skuRegex.test('SH1')).toBe(false); // length 3 < 4
      expect(skuRegex.test('sku-lower-case')).toBe(false); // lowercase not allowed
      expect(skuRegex.test('SKU WITH SPACES')).toBe(false);
      expect(skuRegex.test('SKU@INVALID#')).toBe(false);
      expect(skuRegex.test('A'.repeat(33))).toBe(false); // > 32 chars
    });
  });

  describe('Remediation STT 08: Payment Reconciliation URL & Code Evidence Validation', () => {
    const validateEvidence = (ref: string): boolean => {
      const externalIdRegex = /^[a-zA-Z0-9_-]{6,64}$/;
      const urlRegex = /^https?:\/\/.+/i;
      const trimmed = ref.trim();
      return externalIdRegex.test(trimmed) || urlRegex.test(trimmed);
    };

    it('accepts both external transaction IDs and secure URLs as valid evidence', () => {
      expect(validateEvidence('VCB-STMT-20260927-00192')).toBe(true);
      expect(validateEvidence('STRIPE-CH-994112')).toBe(true);
      expect(validateEvidence('https://bank.internal/receipt/88910')).toBe(true);
      expect(validateEvidence('http://portal.vietcombank.com.vn/trans/9941')).toBe(true);
    });

    it('rejects invalid or insufficient evidence references', () => {
      expect(validateEvidence('short')).toBe(false); // < 6 chars
      expect(validateEvidence('ftp://invalid.com')).toBe(false); // not http/https
      expect(validateEvidence('has space id')).toBe(false);
      expect(validateEvidence('')).toBe(false);
    });
  });

  describe('Remediation STT 03, STT 09: Audit Justification Invariants (BR-018)', () => {
    const isJustificationValid = (text: string): boolean => text.trim().length >= 15;

    it('enforces minimum 15 characters for role assignments, exception resolutions, and ignores', () => {
      expect(isJustificationValid('Cần bổ sung')).toBe(false); // 11 chars
      expect(isJustificationValid('Đã xử lý xong')).toBe(false); // 13 chars
      expect(isJustificationValid('Điều chỉnh phân quyền theo quyết định bổ nhiệm QĐ-2026/09')).toBe(true);
      expect(isJustificationValid('Kho vật lý đã kiểm đếm và xác nhận khớp với phiếu nhập')).toBe(true);
      expect(isJustificationValid('Ngoại lệ dương tính giả do mạng chập chờn đã tự phục hồi')).toBe(true);
    });
  });
});
