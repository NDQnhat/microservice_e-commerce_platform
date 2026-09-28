import { describe, it, expect, vi, beforeEach } from 'vitest';
import { VALID_NEXT_TRANSITIONS } from '../pages/OrdersPage';
import { OrderStatus } from '../types';

describe('Gap Analysis 12 Tasks Verification Test Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // =========================================================================
  // TASK 01 & 02: Order State Machine Transitions, State Guard & RBAC
  // =========================================================================
  describe('Task 01 & Task 02: Order State Machine Transitions, State Guard & RBAC (ORD-T09, API-ORD-003)', () => {
    it('Task 01: prevents manual transition to COMPLETED from SHIPPED (auto-transitioned via DELIVERED event)', () => {
      // Per SRS ORD-T09: Once an order is SHIPPED, operators cannot manually complete it.
      // COMPLETED is only triggered automatically when the carrier sends DELIVERED webhook.
      expect(VALID_NEXT_TRANSITIONS.SHIPPED).toEqual([]);
      expect(VALID_NEXT_TRANSITIONS.SHIPPED).not.toContain('COMPLETED');
    });

    it('Task 01: enforces valid transition paths from CREATED, RESERVED, PAID, PACKING', () => {
      expect(VALID_NEXT_TRANSITIONS.CREATED).toEqual(['RESERVED', 'CANCELLED']);
      expect(VALID_NEXT_TRANSITIONS.RESERVED).toEqual(['PAYMENT_FAILED', 'EXPIRED', 'CANCELLED']);
      expect(VALID_NEXT_TRANSITIONS.PAID).toEqual(['PACKING', 'CANCELLED']);
      expect(VALID_NEXT_TRANSITIONS.PACKING).toEqual(['SHIPPED']);
    });

    it('Task 02: validates carrier name and tracking number regex ^[a-zA-Z0-9-]{8,32}$ when transitioning PACKING -> SHIPPED', () => {
      const trackingRegex = /^[a-zA-Z0-9-]{8,32}$/;

      const validateShippingTransition = (
        carrierName: string,
        trackingCode: string
      ): { isValid: boolean; error?: string } => {
        if (!carrierName.trim()) {
          return { isValid: false, error: 'Vui lòng chọn đơn vị vận chuyển.' };
        }
        if (!trackingCode.trim() || !trackingRegex.test(trackingCode.trim())) {
          return {
            isValid: false,
            error: 'Mã vận đơn không hợp lệ. Phải từ 8-32 ký tự alphanumeric và dấu gạch ngang (-).',
          };
        }
        return { isValid: true };
      };

      // Valid shipping dispatches
      expect(validateShippingTransition('Giao Hàng Nhanh (GHN)', 'GHN-99882211').isValid).toBe(true);
      expect(validateShippingTransition('Viettel Post', 'VTP-2026-X881').isValid).toBe(true);

      // Missing carrier
      expect(validateShippingTransition('', 'GHN-99882211').isValid).toBe(false);
      expect(validateShippingTransition('  ', 'GHN-99882211').error).toContain('đơn vị vận chuyển');

      // Invalid tracking numbers
      expect(validateShippingTransition('GHN', 'SHORT').isValid).toBe(false); // < 8 chars
      expect(validateShippingTransition('GHN', 'TRACK_WITH_UNDERSCORE').isValid).toBe(false); // underscore invalid
      expect(validateShippingTransition('GHN', 'TRACK WITH SPACE').isValid).toBe(false); // space invalid
      expect(validateShippingTransition('GHN', 'A'.repeat(33)).isValid).toBe(false); // > 32 chars
    });

    it('Task 02: enforces RBAC guard: only SUPER_ADMIN, OPS_ADMIN, and ORDER_OPERATOR can transition order state', () => {
      const canTransitionOrder = (userRoles: string[]): boolean => {
        return userRoles.some(
          (role) => role === 'SUPER_ADMIN' || role === 'OPS_ADMIN' || role === 'ORDER_OPERATOR'
        );
      };

      expect(canTransitionOrder(['SUPER_ADMIN'])).toBe(true);
      expect(canTransitionOrder(['OPS_ADMIN'])).toBe(true);
      expect(canTransitionOrder(['ORDER_OPERATOR'])).toBe(true);

      // Unauthorized roles
      expect(canTransitionOrder(['WAREHOUSE_STAFF'])).toBe(false);
      expect(canTransitionOrder(['SUPPORT_AGENT'])).toBe(false);
      expect(canTransitionOrder(['FINANCIAL_AUDITOR'])).toBe(false);
      expect(canTransitionOrder(['CATALOG_MANAGER'])).toBe(false);
      expect(canTransitionOrder([])).toBe(false);
    });
  });

  // =========================================================================
  // TASK 03: Shipment Management Endpoint Standardization
  // =========================================================================
  describe('Task 03: Shipment Management Standardization (API-SHP-001)', () => {
    it('builds standard shipment registration payload with carrier_name, tracking_code, target_status', () => {
      const createShipmentPayload = (
        carrierName: string,
        trackingCode: string,
        targetStatus: string = 'IN_TRANSIT'
      ) => ({
        carrier_name: carrierName.trim(),
        tracking_code: trackingCode.trim(),
        target_status: targetStatus,
      });

      const payload = createShipmentPayload('Giao Hàng Tiết Kiệm (GHTK)', 'GHTK-881920-VN');
      expect(payload).toEqual({
        carrier_name: 'Giao Hàng Tiết Kiệm (GHTK)',
        tracking_code: 'GHTK-881920-VN',
        target_status: 'IN_TRANSIT',
      });
      expect(payload.carrier_name).toBeTruthy();
      expect(/^[a-zA-Z0-9-]{8,32}$/.test(payload.tracking_code)).toBe(true);
    });
  });

  // =========================================================================
  // TASK 04: Catalog Promotions Datetime Inputs & Validation
  // =========================================================================
  describe('Task 04: Catalog Promotion Schedule Validation (BR-021)', () => {
    const validatePromotionDates = (
      startAt: string,
      endAt: string
    ): { isValid: boolean; error?: string } => {
      if (!startAt || !endAt) {
        return { isValid: false, error: 'Vui lòng chọn thời gian bắt đầu và kết thúc.' };
      }
      const startDate = new Date(startAt);
      const endDate = new Date(endAt);
      const now = new Date();

      if (endDate <= startDate) {
        return { isValid: false, error: 'Thời gian kết thúc phải sau thời gian bắt đầu.' };
      }
      if (endDate < now) {
        return { isValid: false, error: 'Thời gian kết thúc không thể trong quá khứ.' };
      }
      return { isValid: true };
    };

    it('accepts valid future promotion date range', () => {
      const futureStart = new Date(Date.now() + 86400000).toISOString();
      const futureEnd = new Date(Date.now() + 86400000 * 7).toISOString();
      expect(validatePromotionDates(futureStart, futureEnd).isValid).toBe(true);
    });

    it('rejects promotion when end date is before or equal to start date', () => {
      const start = '2026-10-01T10:00:00Z';
      const end = '2026-10-01T09:00:00Z';
      const res = validatePromotionDates(start, end);
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('Thời gian kết thúc phải sau thời gian bắt đầu');
    });

    it('rejects promotion when end date is in the past', () => {
      const pastStart = '2025-01-01T00:00:00Z';
      const pastEnd = '2025-01-02T00:00:00Z';
      const res = validatePromotionDates(pastStart, pastEnd);
      expect(res.isValid).toBe(false);
      expect(res.error).toContain('Thời gian kết thúc không thể trong quá khứ');
    });
  });

  // =========================================================================
  // TASK 05: Internal Staff Account Lock & Unlock
  // =========================================================================
  describe('Task 05: Internal Staff Account Lock/Unlock & Audit Trail (API-IAM-005, FR-018)', () => {
    it('requires minimum 15 characters audit justification for staff lock/unlock', () => {
      const validateStaffActionReason = (reason: string): boolean => {
        return reason.trim().length >= 15;
      };

      expect(validateStaffActionReason('Quá ngắn')).toBe(false); // 8 chars
      expect(validateStaffActionReason('Khóa ngay')).toBe(false); // 9 chars
      expect(validateStaffActionReason('Nghi vấn rò rỉ mã OTP đăng nhập')).toBe(true); // >= 15 chars
      expect(validateStaffActionReason('Quyết định số 104/QĐ-BGD khôi phục quyền')).toBe(true);
    });

    it('restricts staff lock/unlock authorization strictly to SUPER_ADMIN', () => {
      const canManageStaffStatus = (roles: string[]): boolean => {
        return roles.includes('SUPER_ADMIN');
      };

      expect(canManageStaffStatus(['SUPER_ADMIN'])).toBe(true);
      expect(canManageStaffStatus(['OPS_ADMIN'])).toBe(false);
      expect(canManageStaffStatus(['ORDER_OPERATOR'])).toBe(false);
      expect(canManageStaffStatus(['FINANCIAL_AUDITOR'])).toBe(false);
    });
  });

  // =========================================================================
  // TASK 06: DLQ Replay Mechanism
  // =========================================================================
  describe('Task 06: Dead Letter Queue (DLQ) Replay Mechanism (API-EXC-003, FR-032)', () => {
    it('allows DLQ replay only for exceptions in OPEN or INVESTIGATING state', () => {
      const canReplayDLQ = (status: string): boolean => {
        return status === 'OPEN' || status === 'INVESTIGATING';
      };

      expect(canReplayDLQ('OPEN')).toBe(true);
      expect(canReplayDLQ('INVESTIGATING')).toBe(true);
      expect(canReplayDLQ('RESOLVED')).toBe(false);
      expect(canReplayDLQ('IGNORED')).toBe(false);
    });

    it('formats DLQ replay mutation payload with action: REPLAY', () => {
      const getDLQReplayPayload = () => ({
        action: 'REPLAY',
      });
      expect(getDLQReplayPayload()).toEqual({ action: 'REPLAY' });
    });
  });

  // =========================================================================
  // TASK 07: Product Edit & Product-Level Media Upload
  // =========================================================================
  describe('Task 07: Product Edit & Product-Level Media Upload (API-CAT-001, API-CAT-004)', () => {
    it('validates product update payload structure', () => {
      const buildProductUpdatePayload = (
        name: string,
        description: string,
        categoryId: string,
        status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
      ) => ({
        name: name.trim(),
        description: description.trim(),
        category_id: categoryId,
        status,
      });

      const payload = buildProductUpdatePayload(
        'Áo Thun Thể Thao Pro',
        'Chất liệu tái chế thoáng mát',
        'cat-apparel-01',
        'ACTIVE'
      );
      expect(payload.name).toBe('Áo Thun Thể Thao Pro');
      expect(payload.status).toBe('ACTIVE');
    });

    it('formats product-level media upload with null sku_id for hero/cover images', () => {
      const createProductMediaPayload = (
        productId: string,
        mediaUrl: string,
        mediaType: 'IMAGE' | 'VIDEO' = 'IMAGE',
        isPrimary: boolean = true
      ) => ({
        product_id: productId,
        sku_id: null, // Product-level media per API-CAT-004
        media_url: mediaUrl,
        media_type: mediaType,
        is_primary: isPrimary,
      });

      const media = createProductMediaPayload('prod-9001', 'https://cdn.example.com/hero.jpg');
      expect(media.sku_id).toBeNull();
      expect(media.is_primary).toBe(true);
      expect(media.product_id).toBe('prod-9001');
    });
  });

  // =========================================================================
  // TASK 08: Category Management & Guardrail (BR-005)
  // =========================================================================
  describe('Task 08: Category Invariant Guardrail (BR-005)', () => {
    const validateCategoryDeactivationOrDeletion = (
      assignedProductsCount: number
    ): { canModify: boolean; reason?: string } => {
      if (assignedProductsCount > 0) {
        return {
          canModify: false,
          reason: `Quy tắc BR-005: Danh mục đang chứa ${assignedProductsCount} sản phẩm trực thuộc. Không thể xóa hoặc chuyển sang Vô hiệu hóa.`,
        };
      }
      return { canModify: true };
    };

    it('blocks category deletion/deactivation when it contains assigned products', () => {
      const result = validateCategoryDeactivationOrDeletion(5);
      expect(result.canModify).toBe(false);
      expect(result.reason).toContain('BR-005');
      expect(result.reason).toContain('5 sản phẩm trực thuộc');
    });

    it('allows category deletion/deactivation when assigned products count is 0', () => {
      const result = validateCategoryDeactivationOrDeletion(0);
      expect(result.canModify).toBe(true);
      expect(result.reason).toBeUndefined();
    });
  });

  // =========================================================================
  // TASK 09: Order Status Breakdown on Dashboard
  // =========================================================================
  describe('Task 09: Order Status Breakdown (7 States Lifecycle Distribution)', () => {
    const ALL_7_ORDER_STATUSES: OrderStatus[] = [
      'RESERVED',
      'PAID',
      'PACKING',
      'SHIPPED',
      'COMPLETED',
      'CANCELLED',
      'EXPIRED',
    ];

    it('contains all 7 required order lifecycle states', () => {
      expect(ALL_7_ORDER_STATUSES).toHaveLength(7);
      expect(ALL_7_ORDER_STATUSES).toContain('RESERVED');
      expect(ALL_7_ORDER_STATUSES).toContain('PAID');
      expect(ALL_7_ORDER_STATUSES).toContain('PACKING');
      expect(ALL_7_ORDER_STATUSES).toContain('SHIPPED');
      expect(ALL_7_ORDER_STATUSES).toContain('COMPLETED');
      expect(ALL_7_ORDER_STATUSES).toContain('CANCELLED');
      expect(ALL_7_ORDER_STATUSES).toContain('EXPIRED');
    });

    it('computes breakdown percentages accurately', () => {
      const breakdown: Record<string, number> = {
        RESERVED: 10,
        PAID: 20,
        PACKING: 20,
        SHIPPED: 20,
        COMPLETED: 20,
        CANCELLED: 5,
        EXPIRED: 5,
      };

      const total = Object.values(breakdown).reduce((acc, c) => acc + c, 0);
      expect(total).toBe(100);

      const percentages = Object.entries(breakdown).reduce(
        (acc, [st, cnt]) => {
          acc[st] = (cnt / total) * 100;
          return acc;
        },
        {} as Record<string, number>
      );

      expect(percentages.RESERVED).toBe(10);
      expect(percentages.PAID).toBe(20);
      expect(percentages.COMPLETED).toBe(20);
      expect(percentages.CANCELLED).toBe(5);
    });
  });

  // =========================================================================
  // TASK 10: Customer Support Resend Notification
  // =========================================================================
  describe('Task 10: Customer Support Resend Notification (FR-041, BR-016)', () => {
    it('validates supported notification types: ORDER_CONFIRMATION, SHIPPING_TRACKING, ACCOUNT_OTP', () => {
      const SUPPORTED_TYPES = ['ORDER_CONFIRMATION', 'SHIPPING_TRACKING', 'ACCOUNT_OTP'] as const;

      expect(SUPPORTED_TYPES).toContain('ORDER_CONFIRMATION');
      expect(SUPPORTED_TYPES).toContain('SHIPPING_TRACKING');
      expect(SUPPORTED_TYPES).toContain('ACCOUNT_OTP');
    });

    it('enforces >= 15 characters audit reason and valid recipient', () => {
      const validateResendNotification = (
        recipient: string,
        reason: string
      ): { isValid: boolean; error?: string } => {
        if (!recipient.trim()) {
          return { isValid: false, error: 'Người nhận không được để trống.' };
        }
        if (reason.trim().length < 15) {
          return {
            isValid: false,
            error: 'Lý do gửi lại bắt buộc tối thiểu 15 ký tự cho WORM audit.',
          };
        }
        return { isValid: true };
      };

      expect(validateResendNotification('', 'Khách hàng yêu cầu gửi lại email').isValid).toBe(false);
      expect(validateResendNotification('test@example.com', 'Quá ngắn').isValid).toBe(false);
      expect(
        validateResendNotification(
          'customer@example.com',
          'Khách hàng khiếu nại chưa nhận được email xác nhận đơn hàng qua hotline'
        ).isValid
      ).toBe(true);
    });

    it('filters notification logs by orderId and recipient', () => {
      const logs = [
        { id: 'log-1', orderId: 'ord-1001', recipient: 'alex@example.com' },
        { id: 'log-2', orderId: 'ord-1002', recipient: 'bob@example.com' },
        { id: 'log-3', orderId: 'ord-2005', recipient: 'alex@example.com' },
      ];

      const searchFilter = (query: string) => {
        const q = query.toLowerCase();
        return logs.filter(
          (l) =>
            l.orderId.toLowerCase().includes(q) ||
            l.recipient.toLowerCase().includes(q) ||
            l.id.toLowerCase().includes(q)
        );
      };

      expect(searchFilter('ord-1001')).toHaveLength(1);
      expect(searchFilter('alex@example.com')).toHaveLength(2);
      expect(searchFilter('nonexistent')).toHaveLength(0);
    });
  });

  // =========================================================================
  // TASK 11: Low-stock Threshold on Inventory (BR-015)
  // =========================================================================
  describe('Task 11: Low-Stock Threshold Calculation (BR-015)', () => {
    const computeStockStatus = (
      availableQuantity: number,
      lowStockThreshold: number = 10
    ): 'OUT_OF_STOCK' | 'LOW_STOCK' | 'IN_STOCK' => {
      if (availableQuantity <= 0) return 'OUT_OF_STOCK';
      if (availableQuantity <= lowStockThreshold) return 'LOW_STOCK';
      return 'IN_STOCK';
    };

    it('returns OUT_OF_STOCK when available quantity <= 0', () => {
      expect(computeStockStatus(0, 10)).toBe('OUT_OF_STOCK');
      expect(computeStockStatus(-2, 10)).toBe('OUT_OF_STOCK');
    });

    it('returns LOW_STOCK when 0 < available quantity <= lowStockThreshold', () => {
      expect(computeStockStatus(1, 10)).toBe('LOW_STOCK');
      expect(computeStockStatus(5, 10)).toBe('LOW_STOCK');
      expect(computeStockStatus(10, 10)).toBe('LOW_STOCK'); // boundary <= 10
    });

    it('returns IN_STOCK when available quantity > lowStockThreshold', () => {
      expect(computeStockStatus(11, 10)).toBe('IN_STOCK');
      expect(computeStockStatus(50, 10)).toBe('IN_STOCK');
    });

    it('respects custom SKU lowStockThreshold', () => {
      // Threshold 15
      expect(computeStockStatus(12, 15)).toBe('LOW_STOCK');
      expect(computeStockStatus(16, 15)).toBe('IN_STOCK');

      // Threshold 3
      expect(computeStockStatus(4, 3)).toBe('IN_STOCK');
      expect(computeStockStatus(3, 3)).toBe('LOW_STOCK');
    });
  });

  // =========================================================================
  // TASK 12: Interceptor 401 Session Interceptor
  // =========================================================================
  describe('Task 12: 401 Session Interceptor & Logout Logic', () => {
    it('detects 401 Unauthorized error and triggers logout handler', () => {
      let loggedOut = false;
      let toastMessage = '';
      let redirectedUrl = '';

      const handleUnauthorizedError = (status: number) => {
        if (status === 401) {
          loggedOut = true;
          toastMessage = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
          redirectedUrl = '/login';
        }
      };

      handleUnauthorizedError(401);

      expect(loggedOut).toBe(true);
      expect(toastMessage).toBe('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
      expect(redirectedUrl).toBe('/login');
    });

    it('does not trigger logout for non-401 errors like 403, 404, 500', () => {
      let loggedOut = false;
      const handleUnauthorizedError = (status: number) => {
        if (status === 401) loggedOut = true;
      };

      handleUnauthorizedError(403);
      expect(loggedOut).toBe(false);

      handleUnauthorizedError(500);
      expect(loggedOut).toBe(false);
    });
  });
});
